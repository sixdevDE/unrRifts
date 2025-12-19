package dev.sixdev.unrrifts.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class LootConfig {
    private final JavaPlugin plugin;

    public LootConfig(JavaPlugin plugin){
        this.plugin = plugin;
    }

    public int chestsPerBranchMin(){ return plugin.getConfig().getInt("loot.chestsPerBranchMin", 1); }
    public int chestsPerBranchMax(){ return plugin.getConfig().getInt("loot.chestsPerBranchMax", 3); }
    public double heartUpgradeChance(){ return plugin.getConfig().getDouble("loot.heartUpgradeChance", 0.10); }

    public List<Tier> tiers(){
        List<Tier> list = new ArrayList<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("loot.tiers");
        if (sec == null) return list;
        for (String k : sec.getKeys(false)){
            int weight = sec.getInt(k+".weight", 1);
            List<String> items = sec.getStringList(k+".items");
            list.add(new Tier(k, weight, items));
        }
        list.sort(Comparator.comparingInt(Tier::weight).reversed());
        return list;
    }

    public record Tier(String id, int weight, List<String> items){}
}
