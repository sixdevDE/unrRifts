package dev.sixdev.unrrifts.core;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;

/**
 * Manages admin build sessions for manual maps (all maps can live in one shared world).
 */
public final class MapBuildManager {

    private final UnrRiftsPlugin plugin;
    private final ManualMapStore store;
    private final Map<UUID, MapBuildSession> sessions = new HashMap<>();

    public MapBuildManager(UnrRiftsPlugin plugin) {
        this.plugin = plugin;
        this.store = new ManualMapStore(plugin);
    }


    public UnrRiftsPlugin plugin() { return plugin; }

    public ManualMapStore store() { return store; }

    public MapBuildSession session(UUID admin) { return sessions.get(admin); }

    public boolean start(UUID admin, String world, String mode, boolean allowBreak) {
        if (sessions.containsKey(admin)) return false;
        MapSource source = MapSource.WORLD;
        String shared = plugin.cfg().sharedWorld();
        if (shared != null && !shared.isBlank() && shared.equalsIgnoreCase(world)) {
            source = MapSource.SHARED_REGION;
        }
        sessions.put(admin, new MapBuildSession(admin, world, mode, source, allowBreak));
        return true;
    }

    public void cancel(UUID admin) { sessions.remove(admin); }

    public ManualMapData finalize(UUID admin, String name, boolean buildBarrierFromBoundary) {
        MapBuildSession s = sessions.remove(admin);
        if (s == null) return null;

        ManualMapData d = new ManualMapData();
        d.name = name;
        d.world = s.world;
        d.source = s.source;
        d.mode = s.mode;
        d.minPlayers = s.minPlayers;
        d.maxPlayers = s.maxPlayers;
        d.allowBreak = s.allowBreak;
        d.boundary.addAll(s.boundary);
        d.playerSpawns.addAll(s.playerSpawns);
        d.mobSpawns.addAll(s.mobSpawns);
        d.bossId = s.bossId;
        d.bossSpawn = s.bossLoc;
        d.lootSpawns.addAll(s.lootSpawns);
        d.exfilSpots.addAll(s.exfilSpots);
        d.events.putAll(s.events);

        // bounding box from boundary points (shared-region maps only)
        if (s.source == MapSource.SHARED_REGION) {
            int[] bb = computeBBox(s.boundary);
            d.minX = bb[0]; d.maxX = bb[1]; d.minZ = bb[2]; d.maxZ = bb[3];
        } else {
            d.minX = 0; d.maxX = 0; d.minZ = 0; d.maxZ = 0;
        }

        // persist first
        store.writeMap(d);

        // optional: build barrier columns (air-only) on boundary points
        if (buildBarrierFromBoundary && d.source == MapSource.SHARED_REGION) {
            buildBarrierColumns(d);
        }

        return d;
    }

    private int[] computeBBox(Set<String> boundary) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (String s : boundary) {
            String[] p = s.split(",");
            if (p.length < 3) continue;
            int x = Integer.parseInt(p[0]);
            int z = Integer.parseInt(p[2]);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        if (minX == Integer.MAX_VALUE) {
            // fallback: 0-size box
            minX = maxX = minZ = maxZ = 0;
        }
        return new int[]{minX, maxX, minZ, maxZ};
    }

    private void buildBarrierColumns(ManualMapData d) {
        World w = Bukkit.getWorld(d.world);
        if (w == null) return;

        int minY = w.getMinHeight();
        int maxY = w.getMaxHeight() - 1;

        // To avoid world grief, we only place barriers into air-like blocks.
        for (String s : d.boundary) {
            String[] p = s.split(",");
            if (p.length < 3) continue;
            int x, y, z;
            try {
                x = Integer.parseInt(p[0]);
                y = Integer.parseInt(p[1]);
                z = Integer.parseInt(p[2]);
            } catch (Exception ignored) { continue; }

            // start above the boundary marker block to keep the floor intact
            int startY = Math.min(maxY, Math.max(minY, y + 1));

            for (int yy = startY; yy <= maxY; yy++) {
                var b = w.getBlockAt(x, yy, z);
                Material t = b.getType();
                if (t == Material.AIR || t == Material.CAVE_AIR || t == Material.VOID_AIR) {
                    b.setType(Material.BARRIER, false);
                }
            }
        }
    }
}