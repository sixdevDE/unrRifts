package dev.sixdev.unrrifts.core;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/** Runtime state for a single active run. */
public class RunInstance {
    public final UUID id = UUID.randomUUID();
    public final LobbyGroup group;
    public final String worldName;

    /** Selected map name (generated or manual/custom). */
    public String mapName = null;

    /** True if this run is based on manual/custom map markers. */
    public boolean manualMap = false;

    public final long startTimeMs = System.currentTimeMillis();

    public World world;

    /** Hub/entry point inside run world (manual maps) */
    public Location hub;

    /** Boss room spawn location */
    public Location bossRoom;

    /** Exfil location */
    public Location exfil;

    /** Player spawn slot per player UUID (manual/custom) */
    public final Map<UUID, Location> spawnByPlayer = new HashMap<>();

    /** Alive players in this run (used for PvP + dormant wake logic) */
    public final Set<UUID> alive = new HashSet<>();

    public boolean bossDefeated = false;

    /** Optional boss id override from manual map marker. */
    public String customBossId = null;

    /** Dormant mobs (including boss) that should wake within 10 blocks. */
    public final Set<UUID> dormantMobs = new HashSet<>();

    /** Repeating task that wakes dormant mobs. */
    public BukkitTask dormantWakeTask = null;

    public RunInstance(LobbyGroup group, String worldName){
        this.group = group;
        this.worldName = worldName;
    }
}
