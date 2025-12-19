package dev.sixdev.unrrifts.core;

import org.bukkit.Location;

import java.util.*;

/** In-memory admin build session (not persisted until finalize). */
public final class MapBuildSession {

    public final UUID builder;
    public final String world;
    public final String mode; // PVE/PVP
    public final MapSource source;
    public int minPlayers = 1;
    public int maxPlayers = 6;
    public boolean allowBreak;

    // Redstone block boundary points (block coordinates)
    public final Set<String> boundary = new LinkedHashSet<>(); // "x,y,z"

    // Markers
    public final List<String> playerSpawns = new ArrayList<>();
    public final List<ManualMapData.MobSpawn> mobSpawns = new ArrayList<>();
    public String bossId = "";
    public String bossLoc = "";
    public final List<ManualMapData.LootSpawn> lootSpawns = new ArrayList<>();
    public final List<String> exfilSpots = new ArrayList<>();
    public final Map<String, String> events = new LinkedHashMap<>();

    public MapBuildSession(UUID builder, String world, String mode, MapSource source, boolean allowBreak) {
        this.builder = builder;
        this.world = world;
        this.mode = mode;
        this.source = source;
        this.allowBreak = allowBreak;
    }

    public int nextPlayerSlot() {
        return Math.min(6, playerSpawns.size() + 1);
    }

    public void addBoundary(Location l) {
        boundary.add(((int) Math.floor(l.getX())) + "," + ((int) Math.floor(l.getY())) + "," + ((int) Math.floor(l.getZ())));
    }
}