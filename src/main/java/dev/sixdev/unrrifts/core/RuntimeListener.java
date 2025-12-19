package dev.sixdev.unrrifts.core;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class RuntimeListener implements Listener {

    private final UnrRiftsPlugin plugin;
    private final ConfigService cfg;
    private final LobbyGroupManager groups;
    private final RunManager runs;

    public RuntimeListener(UnrRiftsPlugin plugin, ConfigService cfg, LobbyGroupManager groups, RunManager runs){
        this.plugin = plugin;
        this.cfg = cfg;
        this.groups = groups;
        this.runs = runs;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        groups.leave(e.getPlayer());
        runs.cancelExfil(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e){
        groups.leave(e.getPlayer());
        runs.cancelExfil(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent e){
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        RunInstance run = runs.runByWorld(victim.getWorld());
        if (run == null) return;

        // only allow pvp if mode is PVP
        if (run.group.key().mode != RunMode.PVP){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        runs.handlePlayerDeath(e.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e){
        // if inside a run, send to lobby exit
        Player p = e.getPlayer();
        RunInstance run = runs.runByWorld(p.getWorld());
        if (run == null) return;

        Location exit = Util.stringToLoc(cfg.lobbyExitStr());
        Location lobby = Util.stringToLoc(cfg.lobbySpawnStr());
        if (exit != null) e.setRespawnLocation(exit);
        else if (lobby != null) e.setRespawnLocation(lobby);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent e){
        if (e.getTo() == null) return;
        Player p = e.getPlayer();
        RunInstance run = runs.runByWorld(p.getWorld());
        if (run == null) return;

        if (runs.isInExfil(run, e.getTo())){
            runs.tryStartExfil(p);
        } else {
            runs.cancelExfil(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e){
        if (e.getItem() == null) return;
        ItemStack is = e.getItem();
        if (is.getType() != Material.NETHER_STAR) return;
        var meta = is.getItemMeta();
        if (meta == null) return;
        if (!Objects.equals(meta.getDisplayName(), "§cHeart Upgrade")) return;

        Player p = e.getPlayer();
        // apply +1 heart up to max
        double max = p.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        double cfgMax = Math.min(cfg.maxHearts(), 10) * 2.0;
        if (max >= cfgMax){
            p.sendMessage("§5[unrRifts] §cAlready at max hearts.");
            e.setCancelled(true);
            return;
        }
        p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Math.min(cfgMax, max + 2.0));
        p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getBaseValue(), p.getHealth()+2.0));
        p.sendMessage("§5[unrRifts] §a+1 Heart!");
        // consume one
        is.setAmount(is.getAmount()-1);
        e.setCancelled(true);
    }


@EventHandler(ignoreCancelled = true)
public void onBlockBreak(BlockBreakEvent e){
    Player p = e.getPlayer();
	    RunInstance run = runs.runOf(p);
    if (run == null) return;
    if (!run.manualMap) return;
    MapRegistry reg = cfg.maps();
    boolean allow = reg.allowBreak(run.mapName);
    if (!allow && !p.hasPermission("unrrifts.admin")){
        e.setCancelled(true);
    }
}

@EventHandler(ignoreCancelled = true)
public void onBlockPlace(BlockPlaceEvent e){
    Player p = e.getPlayer();
	    RunInstance run = runs.runOf(p);
    if (run == null) return;
    if (!run.manualMap) return;
    MapRegistry reg = cfg.maps();
    boolean allow = reg.allowBreak(run.mapName);
    if (!allow && !p.hasPermission("unrrifts.admin")){
        e.setCancelled(true);
    }
}

}
