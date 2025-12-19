package dev.sixdev.unrrifts.core;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardService {
    private final UnrRiftsPlugin plugin;
    private final ConfigService cfg;
    private final File file;
    private YamlConfiguration yaml;

    public LeaderboardService(UnrRiftsPlugin plugin, ConfigService cfg){
        this.plugin = plugin;
        this.cfg = cfg;
        this.file = new File(plugin.getDataFolder(), cfg.leaderboardFile());
        load();
    }

    public void load(){
        try {
            if (!file.exists()){
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (IOException ignored){}
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void save(){
        try {
            yaml.save(file);
        } catch (IOException e){
            plugin.getLogger().warning("Failed to save leaderboard: "+e.getMessage());
        }
    }

    public void recordWin(UUID player, String name, long timeMs){
        String key = "players."+player.toString();
        int wins = yaml.getInt(key+".wins", 0) + 1;
        long best = yaml.getLong(key+".bestTimeMs", 0L);
        if (best == 0L || timeMs < best) best = timeMs;

        yaml.set(key+".name", name);
        yaml.set(key+".wins", wins);
        yaml.set(key+".bestTimeMs", best);

        // global fastest
        long fastest = yaml.getLong("global.fastestTimeMs", 0L);
        if (fastest == 0L || timeMs < fastest){
            yaml.set("global.fastestTimeMs", timeMs);
            yaml.set("global.fastestName", name);
        }
        save();
    }

    // =============================
    // Global getters (für PAPI)
    // =============================
    public String getGlobalFastestName() {
        return yaml.getString("global.fastestName", "-");
    }

    public long getGlobalFastestTimeMs() {
        return yaml.getLong("global.fastestTimeMs", 0L);
    }

    // =============================
    // Player getters (für PAPI)
    // =============================
    public String getName(UUID uuid) {
        return yaml.getString("players." + uuid + ".name", "-");
    }

    public int getWins(UUID uuid, String fallbackName) {
        ensureName(uuid, fallbackName);
        return yaml.getInt("players." + uuid + ".wins", 0);
    }

    public long getBestTimeMs(UUID uuid, String fallbackName) {
        ensureName(uuid, fallbackName);
        return yaml.getLong("players." + uuid + ".bestTimeMs", 0L);
    }

    private void ensureName(UUID uuid, String name) {
        if (name == null || name.isBlank()) return;
        String path = "players." + uuid + ".name";
        if (!yaml.isSet(path)) {
            yaml.set(path, name);
            save();
        }
    }

    // =============================
    // Toplists (rank startet bei 1)
    // =============================
    public UUID getTopWinsUuid(int rank) {
        if (rank < 1) return null;
        List<UUID> top = getTopWins(rank);
        return top.size() >= rank ? top.get(rank - 1) : null;
    }

    public UUID getTopFastestUuid(int rank) {
        if (rank < 1) return null;
        List<UUID> top = getTopFastest(rank);
        return top.size() >= rank ? top.get(rank - 1) : null;
    }

    public List<UUID> getTopWins(int limit) {
        Map<UUID, Integer> winsByUuid = new HashMap<>();

        ConfigurationSection sec = yaml.getConfigurationSection("players");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    int wins = yaml.getInt("players." + key + ".wins", 0);
                    winsByUuid.put(id, wins);
                } catch (Exception ignored) {}
            }
        }

        return winsByUuid.entrySet().stream()
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<UUID> getTopFastest(int limit) {
        Map<UUID, Long> bestByUuid = new HashMap<>();

        ConfigurationSection sec = yaml.getConfigurationSection("players");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    long ms = yaml.getLong("players." + key + ".bestTimeMs", 0L);
                    if (ms > 0L) bestByUuid.put(id, ms);
                } catch (Exception ignored) {}
            }
        }

        return bestByUuid.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry::getValue)) // kleinste Zeit zuerst
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Deine alte Map kannst du behalten (z.B. fürs eigene GUI), ist aber für PAPI nicht nötig.
    public Map<String,Object> placeholders(){
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("unrrifts_fastest_time_ms", yaml.getLong("global.fastestTimeMs",0L));
        map.put("unrrifts_fastest_name", yaml.getString("global.fastestName",""));
        return map;
    }
}
