package dev.sixdev.unrrifts.core;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;

public class MapBuildListener implements Listener {
    private final MapBuildManager builds;
    public MapBuildListener(MapBuildManager builds){ this.builds = builds; }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e){
        if (e.getBlockPlaced().getType() != Material.REDSTONE_BLOCK) return;
        Player p = e.getPlayer();
        MapBuildSession s = builds.get(p);
        if (s == null) return;
        s.addBoundary(e.getBlockPlaced());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e){
        if (e.getBlock().getType() != Material.REDSTONE_BLOCK) return;
        Player p = e.getPlayer();
        MapBuildSession s = builds.get(p);
        if (s == null) return;
        s.removeBoundary(e.getBlock());
    }
}
