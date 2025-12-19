package dev.sixdev.unrrifts.cmd;

import dev.sixdev.unrrifts.core.*;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Drop-in manual map builder commands.
 *
 * Flow:
 *   /unrmapstartbuild <pve|pvp>
 *   place REDSTONE_BLOCKS as boundary markers (optional but recommended)
 *   /unrpspawn (repeat 1..6)
 *   /unrmspawn <entityType> [level] [--no_block_break]
 *   /unrbspawn <bossId>
 *   /unrlspawn [tier]
 *   /unrexfil
 *   /unrevent <name>
 *   /unrmapsetbreak <true|false>
 *   /unrmapfinalize <name>
 */
public final class MapBuildCommands implements CommandExecutor, TabCompleter {

    private final MapBuildManager build;

    public MapBuildCommands(MapBuildManager build) {
        this.build = build;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        String cmd = command.getName().toLowerCase(Locale.ROOT);
        UUID id = p.getUniqueId();
        MapBuildSession s = build.session(id);

        switch (cmd) {
            case "unrsharedworld" -> {
                if (args.length < 2 || !args[0].equalsIgnoreCase("set")) {
                    p.sendMessage("§8[unrRifts] §7Usage: §e/unrsharedworld set <world>");
                    return true;
                }
                String w = args[1].trim();
                if (w.isBlank()) {
                    p.sendMessage("§8[unrRifts] §cInvalid world name.");
                    return true;
                }
                if (Bukkit.getWorld(w) == null) {
                    p.sendMessage("§8[unrRifts] §cWorld not loaded/found: §e" + w);
                    return true;
                }
                build.plugin().getConfig().set("maps.sharedWorld", w);
                build.plugin().saveConfig();
                p.sendMessage("§8[unrRifts] §aShared map world set to: §e" + w);
                return true;
            }
            case "unrmapimportworld" -> {
                if (args.length < 5) {
                    p.sendMessage("§8[unrRifts] §7Usage: §e/unrmapimportworld <name> <world> <pve|pvp> <minPlayers> <maxPlayers> [break:true|false]");
                    return true;
                }
                String name = args[0].trim();
                String world = args[1].trim();
                String mode = args[2].trim().toUpperCase(Locale.ROOT);
                int minP, maxP;
                try { minP = Integer.parseInt(args[3]); maxP = Integer.parseInt(args[4]); } catch (Exception e) {
                    p.sendMessage("§8[unrRifts] §cminPlayers/maxPlayers must be numbers.");
                    return true;
                }
                if (minP < 1) minP = 1;
                if (maxP > 6) maxP = 6;
                if (maxP < minP) maxP = minP;
                if (!mode.equals("PVE") && !mode.equals("PVP")) {
                    p.sendMessage("§8[unrRifts] §cMode must be pve or pvp.");
                    return true;
                }
                if (name.isBlank()) {
                    p.sendMessage("§8[unrRifts] §cInvalid name.");
                    return true;
                }
                if (build.store().exists(name)) {
                    p.sendMessage("§8[unrRifts] §cA map with that name already exists: §e" + name);
                    return true;
                }
                if (Bukkit.getWorld(world) == null) {
                    p.sendMessage("§8[unrRifts] §cWorld not loaded/found: §e" + world);
                    return true;
                }
                boolean allowBreak = true;
                if (args.length >= 6) {
                    String v = args[5].trim().toLowerCase(Locale.ROOT);
                    if (v.equals("true") || v.equals("yes") || v.equals("1")) allowBreak = true;
                    else if (v.equals("false") || v.equals("no") || v.equals("0")) allowBreak = false;
                }
                ManualMapData d = new ManualMapData();
                d.name = name;
                d.world = world;
                d.source = MapSource.WORLD;
                d.mode = mode;
                d.minPlayers = minP;
                d.maxPlayers = maxP;
                d.allowBreak = allowBreak;
                build.store().writeMap(d);
                p.sendMessage("§8[unrRifts] §aWorld map registered: §e" + name + " §7(world §e" + world + "§7).");
                p.sendMessage("§8[unrRifts] §7Now go to that world and run §e/unrmapstartbuild " + (mode.equals("PVP") ? "pvp" : "pve") + "§7, set spawns/markers, then §e/unrmapfinalize " + name + " --force");
                return true;
            }
            case "unrmapstartbuild" -> {
                if (s != null) {
                    p.sendMessage("§8[unrRifts] §cYou already have an active build session. Use /unrmapfinalize <name> or just start over by restarting the session.");
                    return true;
                }
                if (args.length < 1) {
                    p.sendMessage("§8[unrRifts] §7Usage: §e/unrmapstartbuild <pve|pvp> [break:true|false]");
                    return true;
                }
                String mode = Util.upper(args[0]);
                if (!mode.equals("PVE") && !mode.equals("PVP")) {
                    p.sendMessage("§8[unrRifts] §cMode must be PVE or PVP.");
                    return true;
                }
                boolean allowBreak = true;
                if (args.length >= 2) {
                    String v = args[1].trim().toLowerCase(Locale.ROOT);
                    if (v.equals("false") || v.equals("no") || v.equals("0")) allowBreak = false;
                    if (v.equals("true") || v.equals("yes") || v.equals("1")) allowBreak = true;
                }

                boolean ok = build.start(id, p.getWorld().getName(), mode, allowBreak);
                if (!ok) {
                    p.sendMessage("§8[unrRifts] §cCould not start build session.");
                    return true;
                }
                p.sendMessage("§8[unrRifts] §aBuild session started in world §e" + p.getWorld().getName() + "§a (" + mode + ").");
                p.sendMessage("§8[unrRifts] §7Block breaking inside this map during runs: " + (allowBreak ? "§aENABLED" : "§cDISABLED") + "§7. You can change it with §e/unrmapsetbreak <true|false>§7.");
                p.sendMessage("§8[unrRifts] §7Place §cREDSTONE_BLOCK§7s to mark your map boundary. Then use /unrpspawn, /unrmspawn, /unrbspawn, /unrlspawn, /unrexfil, /unrevent.");
                return true;
            }

            case "unrmapsetbreak" -> {
                if (s == null) {
                    p.sendMessage("§8[unrRifts] §cNo active build session. Use /unrmapstartbuild first.");
                    return true;
                }
                if (args.length < 1) {
                    p.sendMessage("§8[unrRifts] §7Usage: §e/unrmapsetbreak <true|false>");
                    return true;
                }
                String v = args[0].trim().toLowerCase(Locale.ROOT);
                boolean allow;
                if (v.equals("true") || v.equals("yes") || v.equals("1")) allow = true;
                else if (v.equals("false") || v.equals("no") || v.equals("0")) allow = false;
                else {
                    p.sendMessage("§8[unrRifts] §cValue must be true/false.");
                    return true;
                }
                s.allowBreak = allow;
                p.sendMessage("§8[unrRifts] §aallowBreak set to: " + (allow ? "§atrue" : "§cfalse"));
                return true;
            }

            case "unrmapfinalize" -> {
                if (s == null) {
                    p.sendMessage("§8[unrRifts] §cNo active build session. Use /unrmapstartbuild first.");
                    return true;
                }
                if (args.length < 1) {
                    p.sendMessage("§8[unrRifts] §7Usage: §e/unrmapfinalize <name> [--force]");
                    return true;
                }
                String name = args[0].trim();
                if (name.isBlank()) {
                    p.sendMessage("§8[unrRifts] §cInvalid name.");
                    return true;
                }

                boolean force = false;
                for (int i = 1; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("--force")) {
                        force = true;
                        break;
                    }
                }

                if (build.store().exists(name) && !force) {
                    p.sendMessage("§8[unrRifts] §cMap already exists: §e" + name + " §7(use §e/unrmapfinalize " + name + " --force§7 to overwrite)");
                    return true;
                }

                if (s.playerSpawns.isEmpty()) {
                    p.sendMessage("§8[unrRifts] §cYou must set at least 1 player spawn using /unrpspawn.");
                    return true;
                }

                boolean buildBarrier = true;
                ManualMapData data = build.finalize(id, name, buildBarrier);
                if (data == null) {
                    p.sendMessage("§8[unrRifts] §cFailed to finalize map.");
                    return true;
                }

                p.sendMessage("§8[unrRifts] §aMap saved: §e" + name + "§a (" + data.mode + ")");
                p.sendMessage("§8[unrRifts] §7Spawns: §e" + data.playerSpawns.size()
                        + "§7, Mobs: §e" + data.mobSpawns.size()
                        + "§7, Loot: §e" + data.lootSpawns.size()
                        + "§7, Exfil: §e" + data.exfilSpots.size());
                p.sendMessage("§8[unrRifts] §7BBox: §e[" + data.minX + "," + data.minZ + "] -> [" + data.maxX + "," + data.maxZ + "]§7 | Boundary points: §e" + data.boundary.size());
                return true;
            }

            case "unrpspawn" -> {
                if (s == null) {
                    p.sendMessage("§8[unrRifts] §cNo active build session. Use /unrmapstartbuild first.");
                    return true;
                }
                if (s.playerSpawns.size() >= 6) {
                    p.sendMessage("§8[unrRifts] §cYou already set 6 player spawns.");
                    return true;
                }
                int slot = s.nextPlayerSlot();
                s.playerSpawns.add(Util.locToString(p.getLocation()));
                p.sendMessage("§8[unrRifts] §aPlayer spawn set for slot §e" + slot + "§a.");
                return true;
            }

            case "unrmspawn" -> {
                if (s == null) {
                    p.sendMessage("§8[unrRifts] §cNo active build session. Use /unrmapstartbuild first.");
                    return true;
                }
                if (args.length < 1) {
                    p.sendMessage("§8[unrRifts] §7Usage: §e/unrmspawn <entityType> [level] [--no_block_break]");
                    return true;
                }
                String entityType = Util.upper(args[0]);
                int level = 0;
                boolean noBB = false;
                for (int i = 1; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("--no_block_break")) noBB = true;
                    else {
                        try { level = Integer.parseInt(args[i]); } catch (Exception ignored) {}
                    }
                }
                s.mobSpawns.add(new ManualMapData.MobSpawn(entityType, level, noBB, Util.locToString(p.getLocation())));
                p.sendMessage("§8[unrRifts] §aMob spawn added: §e" + entityType + "§a level §e" + level + "§a" + (noBB ? " §7(no_block_break)" : ""));
                return true;
            }

            case "unrbspawn" -> {
                if (s == null) {
                    p.sendMessage("§8[unrRifts] §cNo active build session. Use /unrmapstartbuild first.");
                    return true;
                }
                if (args.length < 1) {
                    p.sendMessage("§8[unrRifts] §7Usage: §e/unrbspawn <bossId>");
                    return true;
                }
                s.bossId = args[0].trim();
                s.bossLoc = Util.locToString(p.getLocation());
                p.sendMessage("§8[unrRifts] §aBoss spawn set: §e" + s.bossId);
                return true;
            }

            case "unrlspawn" -> {
                if (s == null) {
                    p.sendMessage("§8[unrRifts] §cNo active build session. Use /unrmapstartbuild first.");
                    return true;
                }
                String tier = args.length >= 1 ? args[0].trim() : "T1";
                if (tier.isBlank()) tier = "T1";
                s.lootSpawns.add(new ManualMapData.LootSpawn(tier, Util.locToString(p.getLocation())));
                p.sendMessage("§8[unrRifts] §aLoot spawn added (" + tier + ").");
                return true;
            }

            case "unrexfil" -> {
                if (s == null) {
                    p.sendMessage("§8[unrRifts] §cNo active build session. Use /unrmapstartbuild first.");
                    return true;
                }
                s.exfilSpots.add(Util.locToString(p.getLocation()));
                p.sendMessage("§8[unrRifts] §aExfil spot added. Total: §e" + s.exfilSpots.size());
                return true;
            }

            case "unrevent" -> {
                if (s == null) {
                    p.sendMessage("§8[unrRifts] §cNo active build session. Use /unrmapstartbuild first.");
                    return true;
                }
                if (args.length < 1) {
                    p.sendMessage("§8[unrRifts] §7Usage: §e/unrevent <name>");
                    return true;
                }
                String evName = args[0].trim();
                if (evName.isBlank()) {
                    p.sendMessage("§8[unrRifts] §cInvalid event name.");
                    return true;
                }
                s.events.put(evName, Util.locToString(p.getLocation()));
                p.sendMessage("§8[unrRifts] §aEvent trigger saved: §e" + evName);
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        if (cmd.equals("unrsharedworld")) {
            if (args.length == 1) return Arrays.asList("set");
            if (args.length == 2) {
                return Bukkit.getWorlds().stream().map(w -> w.getName()).toList();
            }
            return Collections.emptyList();
        }

        if (cmd.equals("unrmapimportworld")) {
            if (args.length == 1) return Collections.emptyList();
            if (args.length == 2) return Bukkit.getWorlds().stream().map(w -> w.getName()).toList();
            if (args.length == 3) return Arrays.asList("pve", "pvp");
            if (args.length == 4 || args.length == 5) return Arrays.asList("1","2","3","4","5","6");
            if (args.length == 6) return Arrays.asList("true","false");
            return Collections.emptyList();
        }

        if (cmd.equals("unrmapstartbuild")) {
            if (args.length == 1) return Arrays.asList("pve", "pvp");
            if (args.length == 2) return Arrays.asList("true", "false");
            return Collections.emptyList();
        }

        if (cmd.equals("unrmapsetbreak")) {
            if (args.length == 1) return Arrays.asList("true", "false");
            return Collections.emptyList();
        }

        if (cmd.equals("unrmspawn")) {
            if (args.length == 1) {
                return Arrays.asList("ZOMBIE", "SKELETON", "SPIDER", "CREEPER", "HUSK", "DROWNED", "PILLAGER");
            }
            if (args.length >= 2) {
                return Arrays.asList("1", "2", "3", "5", "10", "--no_block_break");
            }
        }

        if (cmd.equals("unrbspawn")) {
            if (args.length == 1) {
                return Arrays.asList("rift_boss", "warden", "custom");
            }
        }

        return Collections.emptyList();
    }
}