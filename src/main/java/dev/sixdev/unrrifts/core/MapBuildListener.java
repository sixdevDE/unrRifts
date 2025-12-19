package dev.sixdev.unrrifts.core;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * While an admin is in a build session, redstone blocks placed/removed are treated as map boundary markers.
 */
public final class MapBuildListener implements Listener {

    private final MapBuildManager build;

    public MapBuildListener(MapBuildManager build) {
        this.build = build;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        var s = build.session(e.getPlayer().getUniqueId());
        if (s == null) return;
        if (e.getBlockPlaced().getType() != Material.REDSTONE_BLOCK) return;
        s.addBoundary(e.getBlockPlaced().getLocation());
        e.getPlayer().sendMessage("§8[unrRifts] §7Boundary marker added (§cREDSTONE_BLOCK§7). Total: §e" + s.boundary.size());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        var s = build.session(e.getPlayer().getUniqueId());
        if (s == null) return;
        if (e.getBlock().getType() != Material.REDSTONE_BLOCK) return;
        String key = e.getBlock().getX() + "," + e.getBlock().getY() + "," + e.getBlock().getZ();
        if (s.boundary.remove(key)) {
            e.getPlayer().sendMessage("§8[unrRifts] §7Boundary marker removed. Total: §e" + s.boundary.size());
        }
    }
}
