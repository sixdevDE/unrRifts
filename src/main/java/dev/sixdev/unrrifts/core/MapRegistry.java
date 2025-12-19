package dev.sixdev.unrrifts.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class MapRegistry {

    private final JavaPlugin plugin;

    public MapRegistry(JavaPlugin plugin){
        this.plugin = plugin;
    }

    public Set<String> names(){
        ConfigurationSection reg = plugin.getConfig().getConfigurationSection("maps.registry");
        if (reg == null) return Collections.emptySet();
        return reg.getKeys(false);
    }

    public boolean exists(String name){
        return plugin.getConfig().isConfigurationSection("maps.registry."+name);
    }

    public boolean enabled(String name){
        return plugin.getConfig().getBoolean("maps.registry."+name+".enabled", false);
    }

    public String templateWorld(String name){
        return plugin.getConfig().getString("maps.registry."+name+".templateWorld", "");
    }

    public String hub(String name){ return plugin.getConfig().getString("maps.registry."+name+".hub", ""); }
    public String boss(String name){ return plugin.getConfig().getString("maps.registry."+name+".boss", ""); }
    public String exfil(String name){ return plugin.getConfig().getString("maps.registry."+name+".exfil", ""); }
    public String spawn(String name, int slot){ return plugin.getConfig().getString("maps.registry."+name+".spawns."+slot, ""); }

    public void create(String name, String templateWorld){
        plugin.getConfig().set("maps.registry."+name+".enabled", false);
        plugin.getConfig().set("maps.registry."+name+".templateWorld", templateWorld);
        plugin.getConfig().set("maps.registry."+name+".hub", "");
        plugin.getConfig().set("maps.registry."+name+".boss", "");
        plugin.getConfig().set("maps.registry."+name+".exfil", "");
        for (int i=1;i<=6;i++){
            plugin.getConfig().set("maps.registry."+name+".spawns."+i, "");
        }
        plugin.saveConfig();
    }

    public void setEnabled(String name, boolean enabled){
        plugin.getConfig().set("maps.registry."+name+".enabled", enabled);
        plugin.saveConfig();
    }

    public void setHub(String name, String loc){ plugin.getConfig().set("maps.registry."+name+".hub", loc); plugin.saveConfig(); }
    public void setBoss(String name, String loc){ plugin.getConfig().set("maps.registry."+name+".boss", loc); plugin.saveConfig(); }
    public void setExfil(String name, String loc){ plugin.getConfig().set("maps.registry."+name+".exfil", loc); plugin.saveConfig(); }
    public void setSpawn(String name, int slot, String loc){ plugin.getConfig().set("maps.registry."+name+".spawns."+slot, loc); plugin.saveConfig(); }

}
