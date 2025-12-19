package dev.sixdev.unrrifts.core;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyGroupManager {

    private final UnrRiftsPlugin plugin;
    private final ConfigService cfg;
    private final RunManager runManager;

    // key -> groups
    private final Map<LobbyKey, List<LobbyGroup>> groups = new ConcurrentHashMap<>();
    // player -> group
    private final Map<UUID, LobbyGroup> playerGroup = new ConcurrentHashMap<>();

    public LobbyGroupManager(UnrRiftsPlugin plugin, ConfigService cfg, RunManager runManager){
        this.plugin = plugin;
        this.cfg = cfg;
        this.runManager = runManager;
    }

    public synchronized LobbyGroup join(Player p, LobbyKey key){
        leave(p);

        List<LobbyGroup> list = groups.computeIfAbsent(key, k -> new ArrayList<>());
        // pick first non-running with space
        LobbyGroup target = null;
        for (LobbyGroup g : list){
            if (!g.running() && g.size() < cfg.maxPlayers()){
                target = g; break;
            }
        }
        if (target == null){
            target = new LobbyGroup(key);
            list.add(target);
        }
        target.add(p);
        playerGroup.put(p.getUniqueId(), target);

        reevaluate(target);
        return target;
    }

    public synchronized void leave(Player p){
        LobbyGroup g = playerGroup.remove(p.getUniqueId());
        if (g != null){
            g.remove(p);
            stopCountdown(g);
            if (g.size() == 0 && !g.running()){
                // remove empty group
                List<LobbyGroup> list = groups.get(g.key());
                if (list != null){
                    list.remove(g);
                    if (list.isEmpty()) groups.remove(g.key());
                }
            } else {
                reevaluate(g);
            }
        }
    }

    public LobbyGroup groupOf(Player p){
        return playerGroup.get(p.getUniqueId());
    }

    public synchronized void setKit(Player p, String kitId){
        LobbyGroup g = groupOf(p);
        if (g == null) return;
        g.setKit(p, kitId);
        reevaluate(g);
    }

    public synchronized void reevaluate(LobbyGroup g){
        if (g.running()) return;

        int minPlayers = cfg.minPlayers();
        if (g.size() >= minPlayers && g.allKitsSelected()){
            if (g.countdown() == null){
                Countdown cd = new Countdown(plugin, cfg.countdownSeconds(),
                        () -> broadcastGroup(g, "§7Run starts in §e"+g.countdown().left()+"§7s..."),
                        () -> startRun(g)
                );
                g.setCountdown(cd);
                cd.start();
                broadcastGroup(g, "§aAll players ready. Countdown started!");
            }
        } else {
            stopCountdown(g);
        }
    }

    private void stopCountdown(LobbyGroup g){
        if (g.countdown() != null){
            g.countdown().cancel();
            g.setCountdown(null);
            broadcastGroup(g, "§cCountdown stopped (waiting for players/kits).");
        }
    }

    private void broadcastGroup(LobbyGroup g, String msg){
        for (UUID u : g.players()){
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage("§5[unrRifts] §r"+msg);
        }
    }

    private void startRun(LobbyGroup g){
        if (g.running()) return;
        // final check
        if (g.size() < cfg.minPlayers()) { stopCountdown(g); return; }
        if (!g.allKitsSelected()) { stopCountdown(g); return; }

        g.setRunning(true);
        g.setCountdown(null);

        runManager.startRun(g);
    }

    public Map<LobbyKey, List<LobbyGroup>> snapshot(){
        Map<LobbyKey,List<LobbyGroup>> snap = new LinkedHashMap<>();
        for (var e : groups.entrySet()){
            snap.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return snap;
    }

    public void shutdown(){
        // leave all players safely
        for (UUID u : new ArrayList<>(playerGroup.keySet())){
            Player p = Bukkit.getPlayer(u);
            if (p != null) leave(p);
        }
        groups.clear();
        playerGroup.clear();
    }
}
