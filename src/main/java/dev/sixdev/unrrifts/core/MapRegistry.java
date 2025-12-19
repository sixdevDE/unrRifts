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

    /** Updates the template world for an existing registry entry. */
    public void setTemplateWorld(String name, String templateWorld){
        plugin.getConfig().set("maps.registry."+name+".templateWorld", templateWorld);
        plugin.saveConfig();
    }



// --- Manual map fields (stored under maps.registry.<name>.manual.*) ---
private String base(String name){ return "maps.registry."+name; }
private String man(String name, String key){ return base(name)+".manual."+key; }

public boolean manual(String name){ return plugin.getConfig().getBoolean(man(name,"enabled"), false); }
public void setManual(String name, boolean enabled){ plugin.getConfig().set(man(name,"enabled"), enabled); plugin.saveConfig(); }

public boolean allowBreak(String name){ return plugin.getConfig().getBoolean(man(name,"allowBreak"), true); }
public void setAllowBreak(String name, boolean allow){ plugin.getConfig().set(man(name,"allowBreak"), allow); plugin.saveConfig(); }

public String mode(String name){ return plugin.getConfig().getString(man(name,"mode"), "PVE"); }
public void setMode(String name, String mode){ plugin.getConfig().set(man(name,"mode"), mode); plugin.saveConfig(); }

public java.util.List<String> boundary(String name){ return plugin.getConfig().getStringList(man(name,"boundary")); }
public void setBoundary(String name, java.util.List<String> list){ plugin.getConfig().set(man(name,"boundary"), list); plugin.saveConfig(); }

public java.util.List<java.util.Map<?,?>> mobSpawns(String name){
    return plugin.getConfig().getMapList(man(name,"mobSpawns"));
}
public void setMobSpawns(String name, java.util.List<java.util.Map<String,Object>> list){
    plugin.getConfig().set(man(name,"mobSpawns"), list); plugin.saveConfig();
}

public java.util.List<java.util.Map<?,?>> lootSpawns(String name){
    return plugin.getConfig().getMapList(man(name,"lootSpawns"));
}
public void setLootSpawns(String name, java.util.List<java.util.Map<String,Object>> list){
    plugin.getConfig().set(man(name,"lootSpawns"), list); plugin.saveConfig();
}

public java.util.List<String> exfils(String name){ return plugin.getConfig().getStringList(man(name,"exfils")); }
public void setExfils(String name, java.util.List<String> list){ plugin.getConfig().set(man(name,"exfils"), list); plugin.saveConfig(); }

public java.util.Map<String,Object> events(String name){
    java.util.Map<String,Object> m = plugin.getConfig().getConfigurationSection(man(name,"events")) == null ? new java.util.LinkedHashMap<>() :
            plugin.getConfig().getConfigurationSection(man(name,"events")).getValues(false);
    return m;
}
public void setEvent(String name, String eventName, String loc){
    plugin.getConfig().set(man(name,"events."+eventName), loc); plugin.saveConfig();
}

public String bossId(String name){ return plugin.getConfig().getString(man(name,"boss.id"), ""); }
public String bossLoc(String name){ return plugin.getConfig().getString(man(name,"boss.loc"), ""); }
public void setBoss(String name, String bossId, String loc){
    plugin.getConfig().set(man(name,"boss.id"), bossId);
    plugin.getConfig().set(man(name,"boss.loc"), loc);
    plugin.saveConfig();
}

}
