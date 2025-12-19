package dev.sixdev.unrrifts.core;

import java.util.*;

/** Data model for a manually built map stored in maps.yml. */
public final class ManualMapData {

    public String name = "";
    public String world = "";
    public MapSource source = MapSource.SHARED_REGION;

    public int minPlayers = 1;
    public int maxPlayers = 6;
    public String mode = "PVE"; // PVE | PVP
    public boolean allowBreak = true;

    // Bounding box in the shared world (derived from boundary redstone blocks)
    public int minX, maxX, minZ, maxZ;

    // Boundary points (strings) and various spawn markers
    public final Set<String> boundary = new LinkedHashSet<>(); // "x,y,z" (yaw/pitch not needed)
    public final List<String> playerSpawns = new ArrayList<>(); // Util.locToString
    public final List<MobSpawn> mobSpawns = new ArrayList<>();
    public String bossId = "";
    public String bossSpawn = ""; // Util.locToString
    public final List<LootSpawn> lootSpawns = new ArrayList<>();
    public final List<String> exfilSpots = new ArrayList<>();
    public final Map<String, String> events = new LinkedHashMap<>(); // eventName -> Util.locToString

    public record MobSpawn(String entityType, int level, boolean noBlockBreak, String loc) {}
    public record LootSpawn(String tier, String loc) {}
}