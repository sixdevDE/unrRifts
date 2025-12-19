package dev.sixdev.unrrifts.core;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Protects manually built maps inside a shared world.
 *
 * Rules:
 * - Admins with an active build session can always break/place.
 * - Inside a manual map region:
 *     - If player is NOT in a run: deny (prevents grief in the shared world)
 *     - If player IS in a run: allow only if map.allowBreak == true
 */
public final class MapRegionProtectionListener implements Listener {

    private final MapBuildManager build;
    private final RunManager runs;

    public MapRegionProtectionListener(MapBuildManager build, RunManager runs) {
        this.build = build;
        this.runs = runs;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!shouldDeny(p, e.getBlock().getLocation())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!shouldDeny(p, e.getBlock().getLocation())) return;
        e.setCancelled(true);
    }

    private boolean shouldDeny(Player p, Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        // Allow admins who are currently in build mode (active build session)
        MapBuildSession session = build.session(p.getUniqueId());
        if (session != null && loc.getWorld().getName().equalsIgnoreCase(session.world)) {
            return false;
        }

        // Find manual map at this location (bbox check)
        ManualMapData map = build.store().findAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
        if (map == null) return false; // not inside a registered map region

        // Players in creative should not accidentally grief the shared world.
        // (Admins can always start a build session if they want to edit.)
        if (p.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        // Must be in a run to modify anything inside the shared-map regions.
        RunInstance run = runs.runByWorld(loc.getWorld());
        boolean inRun = run != null && run.group.players().contains(p.getUniqueId());

        if (!inRun) {
            p.sendActionBar("§cThis map region is protected. (Not in a run)");
            return true;
        }

        // In a run: respect allowBreak
        if (!map.allowBreak) {
            p.sendActionBar("§cBlock breaking/placing is disabled in this map.");
            return true;
        }

        return false;
    }
}
