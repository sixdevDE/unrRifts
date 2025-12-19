package dev.sixdev.unrrifts.core;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of which admins are currently building a manual map.
 *
 * NOTE: Some listeners expect helper methods session(UUID) and store().
 * These are provided for compatibility.
 */
public class MapBuildManager {
    private final Map<UUID, MapBuildSession> sessions = new ConcurrentHashMap<>();
    private ManualMapStore store;

    public MapBuildManager() {}

    public MapBuildManager(ManualMapStore store) {
        this.store = store;
    }

    /** Optional wiring if you construct the manager before the store exists. */
    public void setStore(ManualMapStore store) {
        this.store = store;
    }

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

    // --- Compatibility helpers (expected by MapRegionProtectionListener) ---

    public MapBuildSession session(UUID playerId) {
        return sessions.get(playerId);
    }

    public ManualMapStore store() {
        return store;
    }
}
