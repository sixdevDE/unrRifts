package dev.sixdev.unrrifts.core;

import java.util.*;

/**
 * Build session while an admin is defining a manual map.
 * This class is intentionally lightweight: it only tracks markers and boundary points.
 */
public class MapBuildSession {
    public final UUID builder;
    public final String mapName;

    /** Back-compat name used in older code. */
    public final String worldName;

    /** Field expected by MapRegionProtectionListener. */
    public final String world;

    public final RunMode mode;
    public boolean allowBreak;

    public int nextPlayerSlot = 1;

    public final List<BlockPos> boundary = new ArrayList<>();
    public final Map<Integer, String> playerSpawns = new LinkedHashMap<>();
    public final List<MobSpawn> mobSpawns = new ArrayList<>();
    public final List<LootSpawn> lootSpawns = new ArrayList<>();
    public String bossId;
    public String bossLoc;
    public final List<String> exfils = new ArrayList<>();
    public final Map<String, String> events = new LinkedHashMap<>();

    public MapBuildSession(UUID builder, String mapName, String worldName, RunMode mode, boolean allowBreak) {
        this.builder = builder;
        this.mapName = mapName;
        this.worldName = worldName;
        this.world = worldName;
        this.mode = mode;
        this.allowBreak = allowBreak;
    }

    public void addBoundary(org.bukkit.block.Block b){
        boundary.add(new BlockPos(b.getX(), b.getY(), b.getZ()));
    }
    public void removeBoundary(org.bukkit.block.Block b){
        boundary.removeIf(p -> p.x==b.getX() && p.y==b.getY() && p.z==b.getZ());
    }

    public record BlockPos(int x,int y,int z){}
    public record MobSpawn(String entityType, int level, boolean noBlockBreak, String loc){}
    public record LootSpawn(String tier, String loc){}
}
