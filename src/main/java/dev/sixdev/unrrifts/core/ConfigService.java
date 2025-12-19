package dev.sixdev.unrrifts.core;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ConfigService {

    private final UnrRiftsPlugin plugin;

    public ConfigService(UnrRiftsPlugin plugin){
        this.plugin = plugin;
    }

    public void reload(){
        plugin.reloadConfig();
    }

    public String lobbyWorld(){ return plugin.getConfig().getString("lobby.world", "lobby"); }
    public int maxPlayers(){ return plugin.getConfig().getInt("queue.maxPlayersPerLobby", 6); }
    public int minPlayers(){ return plugin.getConfig().getInt("queue.minPlayers", 1); }
    public int countdownSeconds(){ return plugin.getConfig().getInt("queue.countdownSeconds", 15); }

    public int runBuildY(){ return plugin.getConfig().getInt("run.buildY", 40); }
    public int runCleanupDelaySeconds(){ return plugin.getConfig().getInt("run.cleanupDelaySeconds", 10); }

    public int exfilRadius(){ return plugin.getConfig().getInt("run.exfil.radius", 4); }
    public int exfilTimerSeconds(){ return plugin.getConfig().getInt("run.exfil.timerSeconds", 10); }

    public int maxHearts(){ return plugin.getConfig().getInt("run.maxHearts", 10); }
    public int minHearts(){ return plugin.getConfig().getInt("run.minHearts", 3); }

    public EntityType bossType(){
        return EntityType.valueOf(plugin.getConfig().getString("boss.type", "WITHER_SKELETON").toUpperCase(Locale.ROOT));
    }
    public String bossName(){ return plugin.getConfig().getString("boss.name", "Rift Warden"); }
    public double bossHealth(){ return plugin.getConfig().getDouble("boss.health", 250.0); }

    public boolean bossDropsToken(){ return plugin.getConfig().getBoolean("boss.dropExfilToken", true); }

    public ItemStack exfilTokenItem(){
        String mat = plugin.getConfig().getString("boss.tokenItem.material", "AMETHYST_SHARD");
        Material m = Material.matchMaterial(mat);
        if (m == null) m = Material.AMETHYST_SHARD;
        ItemStack is = new ItemStack(m, 1);
        var meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfig().getString("boss.tokenItem.name", "§dExfil Token"));
            meta.setLore(plugin.getConfig().getStringList("boss.tokenItem.lore"));
            is.setItemMeta(meta);
        }
        return is;
    }

    public Map<String, KitDefinition> kits(){
        Map<String, KitDefinition> out = new LinkedHashMap<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("kits");
        if (sec == null) return out;
        for (String k : sec.getKeys(false)){
            String display = sec.getString(k+".display", k);
            Material icon = Material.matchMaterial(sec.getString(k+".icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;
            List<String> items = sec.getStringList(k+".items");
            out.put(k, new KitDefinition(k, display, icon, items));
        }
        return out;
    }

    public LootConfig loot(){
        return new LootConfig(plugin);
    }

    public MapRegistry maps(){
        return new MapRegistry(plugin);
    }

    public String leaderboardFile(){
        return plugin.getConfig().getString("leaderboard.file", "leaderboard.yml");
    }

    public String lobbySpawnStr(){ return plugin.getConfig().getString("lobby.spawn",""); }
    public String lobbyExitStr(){ return plugin.getConfig().getString("lobby.exit",""); }

    public void setLobbySpawn(String locStr){
        plugin.getConfig().set("lobby.spawn", locStr);
        plugin.saveConfig();
    }
    public void setExit(String locStr){
        plugin.getConfig().set("lobby.exit", locStr);
        plugin.saveConfig();
    }


// generic access helpers
public int getInt(String path, int def){ return plugin.getConfig().getInt(path, def); }
public double getDouble(String path, double def){ return plugin.getConfig().getDouble(path, def); }
public String getString(String path, String def){ return plugin.getConfig().getString(path, def); }

}
