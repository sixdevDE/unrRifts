package dev.sixdev.unrrifts.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Stores manual maps (built inside a shared world) in plugins/unrRifts/maps.yml.
 *
 * This is intentionally independent from config.yml to keep admin-built content separate
 * and to make backups/migration easy.
 */
public final class ManualMapStore {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;
    private final Map<String, ManualMapData> cache = new LinkedHashMap<>();

    public ManualMapStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "maps.yml");
        load();
    }

    public void load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (IOException ignored) {}
        this.yaml = YamlConfiguration.loadConfiguration(file);
        rebuildCache();
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save maps.yml: " + e.getMessage());
        }
        rebuildCache();
    }

    private void rebuildCache() {
        cache.clear();
        for (String name : names()) {
            ManualMapData d = readMap(name);
            if (d != null) cache.put(name, d);
        }
    }

    /** Returns a cached view of all maps. Cache is rebuilt on load/save/writeMap. */
    public Collection<ManualMapData> all() {
        return Collections.unmodifiableCollection(cache.values());
    }

    /** Find the first map in the given world whose bbox contains (x,z). */
    public ManualMapData findAt(String world, int x, int z) {
        if (world == null) return null;
        for (ManualMapData d : cache.values()) {
            if (!world.equalsIgnoreCase(d.world)) continue;
            if (d.source == MapSource.WORLD) {
                return d; // dedicated world maps cover entire world
            }
            if (x >= Math.min(d.minX, d.maxX) && x <= Math.max(d.minX, d.maxX)
                    && z >= Math.min(d.minZ, d.maxZ) && z <= Math.max(d.minZ, d.maxZ)) {
                return d;
            }
        }
        return null;
    }

    public boolean exists(String name) {
        return yaml.isConfigurationSection("maps." + name);
    }

    public Set<String> names() {
        ConfigurationSection sec = yaml.getConfigurationSection("maps");
        if (sec == null) return Collections.emptySet();
        return sec.getKeys(false);
    }

    public void writeMap(ManualMapData data) {
        String base = "maps." + data.name;
        yaml.set(base + ".enabled", true);
        yaml.set(base + ".world", data.world);
        yaml.set(base + ".source", data.source.name());
        yaml.set(base + ".mode", data.mode);
        yaml.set(base + ".minPlayers", data.minPlayers);
        yaml.set(base + ".maxPlayers", data.maxPlayers);
        yaml.set(base + ".allowBreak", data.allowBreak);
        yaml.set(base + ".bbox.minX", data.minX);
        yaml.set(base + ".bbox.maxX", data.maxX);
        yaml.set(base + ".bbox.minZ", data.minZ);
        yaml.set(base + ".bbox.maxZ", data.maxZ);
        yaml.set(base + ".boundary", new ArrayList<>(data.boundary));
        yaml.set(base + ".spawns", new ArrayList<>(data.playerSpawns));
        yaml.set(base + ".exfil", new ArrayList<>(data.exfilSpots));

        // boss
        if (data.bossId != null && !data.bossId.isBlank() && data.bossSpawn != null && !data.bossSpawn.isBlank()) {
            yaml.set(base + ".boss.id", data.bossId);
            yaml.set(base + ".boss.loc", data.bossSpawn);
        } else {
            yaml.set(base + ".boss", null);
        }

        // loot spawns
        List<Map<String, Object>> loot = new ArrayList<>();
        for (ManualMapData.LootSpawn ls : data.lootSpawns) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tier", ls.tier());
            m.put("loc", ls.loc());
            loot.add(m);
        }
        yaml.set(base + ".loot", loot);

        // mob spawns
        List<Map<String, Object>> mobs = new ArrayList<>();
        for (ManualMapData.MobSpawn ms : data.mobSpawns) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", ms.entityType());
            m.put("level", ms.level());
            m.put("noBlockBreak", ms.noBlockBreak());
            m.put("loc", ms.loc());
            mobs.add(m);
        }
        yaml.set(base + ".mobs", mobs);

        // events
        Map<String, String> ev = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : data.events.entrySet()) {
            ev.put(e.getKey(), e.getValue());
        }
        yaml.set(base + ".events", ev);

        save();
    }

    public ManualMapData readMap(String name) {
        String base = "maps." + name;
        if (!yaml.isConfigurationSection(base)) return null;

        ManualMapData d = new ManualMapData();
        d.name = name;
        d.world = yaml.getString(base + ".world", "");
        String src = yaml.getString(base + ".source", "SHARED_REGION");
        try { d.source = MapSource.valueOf(src.toUpperCase(java.util.Locale.ROOT)); } catch (Exception ignored) { d.source = MapSource.SHARED_REGION; }
        d.mode = yaml.getString(base + ".mode", "PVE");
        d.minPlayers = yaml.getInt(base + ".minPlayers", 1);
        d.maxPlayers = yaml.getInt(base + ".maxPlayers", 6);
        d.allowBreak = yaml.getBoolean(base + ".allowBreak", true);
        d.minX = yaml.getInt(base + ".bbox.minX", 0);
        d.maxX = yaml.getInt(base + ".bbox.maxX", 0);
        d.minZ = yaml.getInt(base + ".bbox.minZ", 0);
        d.maxZ = yaml.getInt(base + ".bbox.maxZ", 0);

        d.boundary.addAll(yaml.getStringList(base + ".boundary"));
        d.playerSpawns.addAll(yaml.getStringList(base + ".spawns"));
        d.exfilSpots.addAll(yaml.getStringList(base + ".exfil"));

        d.bossId = yaml.getString(base + ".boss.id", "");
        d.bossSpawn = yaml.getString(base + ".boss.loc", "");

        List<Map<?, ?>> loot = yaml.getMapList(base + ".loot");
        for (Map<?, ?> m : loot) {
            Object tierObj = m.get("tier");
            Object locObj = m.get("loc");
            String tier = tierObj == null ? "" : String.valueOf(tierObj);
            String loc = locObj == null ? "" : String.valueOf(locObj);
            if (!loc.isBlank()) d.lootSpawns.add(new ManualMapData.LootSpawn(tier, loc));
        }

        List<Map<?, ?>> mobs = yaml.getMapList(base + ".mobs");
        for (Map<?, ?> m : mobs) {
            Object typeObj = m.get("type");
            Object levelObj = m.get("level");
            Object noBBObj = m.get("noBlockBreak");
            Object locObj = m.get("loc");

            String type = typeObj == null ? "" : String.valueOf(typeObj);

            int level = 0;
            try {
                if (levelObj != null) level = Integer.parseInt(String.valueOf(levelObj));
            } catch (Exception ignored) {}

            boolean noBB = false;
            try {
                if (noBBObj != null) noBB = Boolean.parseBoolean(String.valueOf(noBBObj));
            } catch (Exception ignored) {}

            String loc = locObj == null ? "" : String.valueOf(locObj);
            if (!loc.isBlank()) d.mobSpawns.add(new ManualMapData.MobSpawn(type, level, noBB, loc));
        }

        ConfigurationSection ev = yaml.getConfigurationSection(base + ".events");
        if (ev != null) {
            for (String k : ev.getKeys(false)) {
                String loc = ev.getString(k, "");
                if (loc != null && !loc.isBlank()) d.events.put(k, loc);
            }
        }

        return d;
    }
}