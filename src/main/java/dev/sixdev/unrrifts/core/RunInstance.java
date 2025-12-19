package dev.sixdev.unrrifts.core;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class RunInstance {
    public final UUID id = UUID.randomUUID();
    public final LobbyGroup group;
    public final String worldName;
    public final long startTimeMs = System.currentTimeMillis();

    public World world;

    public Location hub;
    public Location bossRoom;
    public Location exfil;

    public final Map<UUID, Location> spawnByPlayer = new HashMap<>();
    public final Set<UUID> alive = new HashSet<>();

    public boolean bossDefeated = false;

    public RunInstance(LobbyGroup group, String worldName){
        this.group = group;
        this.worldName = worldName;
    }
}
