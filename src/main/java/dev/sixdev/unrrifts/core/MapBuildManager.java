package dev.sixdev.unrrifts.core;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MapBuildManager {
    private final Map<UUID, MapBuildSession> sessions = new ConcurrentHashMap<>();

    public MapBuildSession get(Player p){ return sessions.get(p.getUniqueId()); }
    public boolean has(Player p){ return sessions.containsKey(p.getUniqueId()); }

    public MapBuildSession start(Player p, String mapName, RunMode mode, boolean allowBreak){
        MapBuildSession s = new MapBuildSession(p.getUniqueId(), mapName, p.getWorld().getName(), mode, allowBreak);
        sessions.put(p.getUniqueId(), s);
        return s;
    }

    public void stop(Player p){
        sessions.remove(p.getUniqueId());
    }
}
