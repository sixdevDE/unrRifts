package dev.sixdev.unrrifts.core;

import org.bukkit.entity.Player;

import java.util.*;

public class LobbyGroup {
    private final UUID id = UUID.randomUUID();
    private final LobbyKey key;

    private final List<UUID> players = new ArrayList<>();
    private final Map<UUID, String> selectedKit = new HashMap<>();

    private boolean running = false;
    private Countdown countdown;

    public LobbyGroup(LobbyKey key){
        this.key = key;
    }

    public UUID id(){ return id; }
    public LobbyKey key(){ return key; }

    public List<UUID> players(){ return Collections.unmodifiableList(players); }
    public int size(){ return players.size(); }
    public boolean running(){ return running; }
    public void setRunning(boolean v){ running = v; }

    public void add(Player p){
        if (!players.contains(p.getUniqueId())) players.add(p.getUniqueId());
    }

    public void remove(Player p){
        players.remove(p.getUniqueId());
        selectedKit.remove(p.getUniqueId());
    }

    public void setKit(Player p, String kitId){
        selectedKit.put(p.getUniqueId(), kitId);
    }

    public boolean hasKit(Player p){ return selectedKit.containsKey(p.getUniqueId()); }

    public Map<UUID,String> kits(){ return Collections.unmodifiableMap(selectedKit); }

    public boolean allKitsSelected(){
        return !players.isEmpty() && selectedKit.size() == players.size();
    }

    public Countdown countdown(){ return countdown; }
    public void setCountdown(Countdown c){ this.countdown = c; }

    public String kitOf(UUID player){ return selectedKit.get(player); }
}
