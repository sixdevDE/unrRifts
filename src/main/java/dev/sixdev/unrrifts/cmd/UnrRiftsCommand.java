package dev.sixdev.unrrifts.cmd;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import dev.sixdev.unrrifts.core.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class UnrRiftsCommand implements CommandExecutor, TabCompleter {

    private final UnrRiftsPlugin plugin;
    private final ConfigService cfg;
    private final LobbyGroupManager groups;
    private final RunManager runs;

    public UnrRiftsCommand(UnrRiftsPlugin plugin, ConfigService cfg, LobbyGroupManager groups, RunManager runs){
        this.plugin = plugin;
        this.cfg = cfg;
        this.groups = groups;
        this.runs = runs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")){
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")){
            cfg.reload();
            sender.sendMessage("§5[unrRifts] §aReloaded config.");
            return true;
        }

        if (!(sender instanceof Player p)){
            sender.sendMessage("Player required for this command.");
            return true;
        }

        if (sub.equals("setlobby")){
            Location loc = p.getLocation();
            cfg.setLobbySpawn(Util.locToString(loc));
            // auto set lobby world if blank
            plugin.getConfig().set("lobby.world", loc.getWorld().getName());
            plugin.saveConfig();
            sender.sendMessage("§5[unrRifts] §aLobby spawn set to current location.");
            return true;
        }

        if (sub.equals("setexit")){
            Location loc = p.getLocation();
            cfg.setExit(Util.locToString(loc));
            sender.sendMessage("§5[unrRifts] §aExit location set to current location.");
            return true;
        }

        if (sub.equals("debug")){
            sender.sendMessage("§5[unrRifts] §fActive LobbyGroups:");
            for (var e : groups.snapshot().entrySet()){
                sender.sendMessage(" §7- §f"+e.getKey()+" §7groups=§f"+e.getValue().size());
                for (LobbyGroup g : e.getValue()){
                    sender.sendMessage("   §8• §f"+g.id().toString().substring(0,8)+" §7players=§f"+g.size()+" §7ready=§f"+g.kits().size()+" §7running=§f"+g.running());
                }
            }
            sender.sendMessage("§5[unrRifts] §fRun worlds loaded: §f"+Bukkit.getWorlds().size());
            return true;
        }

        if (sub.equals("map")){
            return handleMap(sender, args);
        }

        sender.sendMessage("§5[unrRifts] §cUnknown subcommand. Use /unrrifts help");
        return true;
    }

    private boolean handleMap(CommandSender sender, String[] args){
        if (args.length < 2){
            sender.sendMessage("§5[unrRifts] §cUsage: /unrrifts map <create|sethub|setboss|setexfil|setspawn|enable|list> ...");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        MapRegistry reg = cfg.maps();

        if (action.equals("list")){
            sender.sendMessage("§5[unrRifts] §fMaps:");
            for (String name : reg.names()){
                sender.sendMessage(" §7- §f"+name+" §7enabled=§f"+reg.enabled(name)+" §7template=§f"+reg.templateWorld(name));
            }
            return true;
        }

        if (action.equals("create")){
            if (args.length < 4){
                sender.sendMessage("§5[unrRifts] §cUsage: /unrrifts map create <name> <world>");
                return true;
            }
            String name = args[2];
            String world = args[3];

            // verify template world exists
            File src = new File(Bukkit.getWorldContainer(), world);
            if (!src.exists() || !src.isDirectory()){
                sender.sendMessage("§5[unrRifts] §cWorld folder not found in server root: "+world);
                return true;
            }

            reg.create(name, world);
            sender.sendMessage("§5[unrRifts] §aMap created: §f"+name+" §7(templateWorld="+world+")");
            sender.sendMessage("§5[unrRifts] §7Now set: /unrrifts map sethub "+name+"  | setboss | setexfil | setspawn <slot>");
            return true;
        }

        if (!(sender instanceof Player p)){
            sender.sendMessage("Player required.");
            return true;
        }

        if (args.length < 3){
            sender.sendMessage("§5[unrRifts] §cMissing map name.");
            return true;
        }
        String name = args[2];
        if (!reg.exists(name)){
            sender.sendMessage("§5[unrRifts] §cMap not found: "+name);
            return true;
        }

        if (action.equals("sethub")){
            reg.setHub(name, Util.locToString(p.getLocation()));
            sender.sendMessage("§5[unrRifts] §aHub set for map "+name);
            return true;
        }
        if (action.equals("setboss")){
            reg.setBoss(name, Util.locToString(p.getLocation()));
            sender.sendMessage("§5[unrRifts] §aBoss location set for map "+name);
            return true;
        }
        if (action.equals("setexfil")){
            reg.setExfil(name, Util.locToString(p.getLocation()));
            sender.sendMessage("§5[unrRifts] §aExfil location set for map "+name);
            return true;
        }
        if (action.equals("setspawn")){
            if (args.length < 4){
                sender.sendMessage("§5[unrRifts] §cUsage: /unrrifts map setspawn <name> <slot 1-6>");
                return true;
            }
            int slot;
            try { slot = Integer.parseInt(args[3]); } catch (Exception e){ slot = 0; }
            if (slot < 1 || slot > 6){
                sender.sendMessage("§5[unrRifts] §cSlot must be 1..6");
                return true;
            }
            reg.setSpawn(name, slot, Util.locToString(p.getLocation()));
            sender.sendMessage("§5[unrRifts] §aSpawn "+slot+" set for map "+name);
            return true;
        }
        if (action.equals("enable")){
            if (args.length < 4){
                sender.sendMessage("§5[unrRifts] §cUsage: /unrrifts map enable <name> true|false");
                return true;
            }
            boolean en = Boolean.parseBoolean(args[3]);
            reg.setEnabled(name, en);
            sender.sendMessage("§5[unrRifts] §aMap "+name+" enabled="+en);
            return true;
        }

        sender.sendMessage("§5[unrRifts] §cUnknown map action.");
        return true;
    }

    private void sendHelp(CommandSender sender){
        sender.sendMessage("§5§lunrRifts §7— Help");
        sender.sendMessage("§fRequires: §eMultiverse-Core §7(lobby world must be created/imported via MV)");
        sender.sendMessage("");
        sender.sendMessage("§fPlayer:");
        sender.sendMessage(" §e/rift §7Open GUI to queue for runs (WorldType/Mode/Kit).");
        sender.sendMessage("");
        sender.sendMessage("§fAdmin:");
        sender.sendMessage(" §e/unrrifts help §7Show this help");
        sender.sendMessage(" §e/unrrifts setlobby §7Set lobby.spawn (and lobby.world)");
        sender.sendMessage(" §e/unrrifts setexit §7Set exit location (teleport target after run/leave)");
        sender.sendMessage(" §e/unrrifts reload §7Reload config");
        sender.sendMessage(" §e/unrrifts debug §7Show active LobbyGroups");
        sender.sendMessage("");
        sender.sendMessage("§fCustom Maps:");
        sender.sendMessage(" §e/unrrifts map create <name> <worldFolder> §7Register a template world");
        sender.sendMessage(" §e/unrrifts map sethub <name> §7Set hub location (stand at hub)");
        sender.sendMessage(" §e/unrrifts map setboss <name> §7Set boss location");
        sender.sendMessage(" §e/unrrifts map setexfil <name> §7Set exfil location");
        sender.sendMessage(" §e/unrrifts map setspawn <name> <1-6> §7Set startroom spawn slots");
        sender.sendMessage(" §e/unrrifts map enable <name> true|false §7Enable/disable map");
        sender.sendMessage(" §e/unrrifts map list §7List maps");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1){
            return filter(List.of("help","setlobby","setexit","reload","debug","map"), args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("map")){
            if (args.length == 2){
                return filter(List.of("create","sethub","setboss","setexfil","setspawn","enable","list"), args[1]);
            }
            MapRegistry reg = cfg.maps();
            if (args.length == 3 && !args[1].equalsIgnoreCase("create")){
                return filter(new ArrayList<>(reg.names()), args[2]);
            }
            if (args[1].equalsIgnoreCase("enable") && args.length == 4){
                return filter(List.of("true","false"), args[3]);
            }
            if (args[1].equalsIgnoreCase("setspawn") && args.length == 4){
                return filter(List.of("1","2","3","4","5","6"), args[3]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix){
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return list.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p)).collect(Collectors.toList());
    }
}
