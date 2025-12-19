package dev.sixdev.unrrifts.cmd;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import dev.sixdev.unrrifts.core.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class MapBuildCommands implements CommandExecutor {

    private final UnrRiftsPlugin plugin;
    private final ConfigService cfg;
    private final MapBuildManager builds;

    public MapBuildCommands(UnrRiftsPlugin plugin, ConfigService cfg, MapBuildManager builds){
        this.plugin = plugin;
        this.cfg = cfg;
        this.builds = builds;
    }

    private boolean admin(CommandSender s){
        return (s instanceof Player) && s.hasPermission("unrrifts.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player p)){
            sender.sendMessage("Players only.");
            return true;
        }
        if (!p.hasPermission("unrrifts.admin")){
            p.sendMessage("§cNo permission.");
            return true;
        }
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        return switch (cmd){
            case "unrmapstartbuild" -> cmdStart(p, args);
            case "unrmapfinalize" -> cmdFinalize(p, args);
            case "unrmapsetbreak" -> cmdSetBreak(p, args);
            case "unrpspawn" -> cmdPlayerSpawn(p);
            case "unrmspawn" -> cmdMobSpawn(p, args);
            case "unrlspawn" -> cmdLootSpawn(p, args);
            case "unrbspawn" -> cmdBossSpawn(p, args);
            case "unrexfil" -> cmdExfil(p);
            case "unrevent" -> cmdEvent(p, args);
            default -> { p.sendMessage("§cUnknown."); yield true; }
        };
    }

    private boolean cmdStart(Player p, String[] args){
        if (args.length < 2){
            p.sendMessage("§cUsage: /unrmapstartbuild <name> <pve|pvp> [break:true|false]");
            return true;
        }
        String name = args[0].trim();
        RunMode mode = "PVP".equalsIgnoreCase(args[1]) ? RunMode.PVP : RunMode.PVE;
        boolean allowBreak = true;
        if (args.length >= 3) allowBreak = Boolean.parseBoolean(args[2]);
        MapBuildSession s = builds.start(p, name, mode, allowBreak);
        p.sendMessage("§aBuild session started for §f"+name+"§a in world §f"+s.worldName+"§a (mode "+mode.display()+", break="+allowBreak+").");
        p.sendMessage("§7Place §cREDSTONE_BLOCK§7s as border markers while building.");
        return true;
    }

    private boolean cmdSetBreak(Player p, String[] args){
        MapBuildSession s = builds.get(p);
        if (s == null){ p.sendMessage("§cNo active build session."); return true; }
        if (args.length != 1){ p.sendMessage("§cUsage: /unrmapsetbreak <true|false>"); return true; }
        s.allowBreak = Boolean.parseBoolean(args[0]);
        p.sendMessage("§aallowBreak set to "+s.allowBreak);
        return true;
    }

    private boolean cmdPlayerSpawn(Player p){
        MapBuildSession s = builds.get(p);
        if (s == null){ p.sendMessage("§cNo active build session."); return true; }
        int slot = s.nextPlayerSlot;
        if (slot > 6){ p.sendMessage("§cAlready set 6 player spawns."); return true; }
        s.playerSpawns.put(slot, Util.locToString(p.getLocation()));
        s.nextPlayerSlot++;
        p.sendMessage("§aSet player spawn slot §f"+slot+"§a.");
        return true;
    }

    private boolean cmdMobSpawn(Player p, String[] args){
        MapBuildSession s = builds.get(p);
        if (s == null){ p.sendMessage("§cNo active build session."); return true; }
        if (args.length < 1){
            p.sendMessage("§cUsage: /unrmspawn <entityType> [level] [--no_block_break]");
            return true;
        }
        String type = Util.upper(args[0]);
        int level = 1;
        boolean noBreak = false;
        for (int i=1;i<args.length;i++){
            if (args[i].equalsIgnoreCase("--no_block_break")) noBreak = true;
            else {
                try { level = Integer.parseInt(args[i]); } catch (Exception ignored){}
            }
        }
        s.mobSpawns.add(new MapBuildSession.MobSpawn(type, level, noBreak, Util.locToString(p.getLocation())));
        p.sendMessage("§aAdded mob spawn: §f"+type+"§a (lvl "+level+", noBreak="+noBreak+")");
        return true;
    }

    private boolean cmdLootSpawn(Player p, String[] args){
        MapBuildSession s = builds.get(p);
        if (s == null){ p.sendMessage("§cNo active build session."); return true; }
        String tier = args.length >= 1 ? args[0] : "T1";
        s.lootSpawns.add(new MapBuildSession.LootSpawn(tier, Util.locToString(p.getLocation())));
        p.sendMessage("§aAdded loot spawn tier §f"+tier+"§a.");
        return true;
    }

    private boolean cmdBossSpawn(Player p, String[] args){
        MapBuildSession s = builds.get(p);
        if (s == null){ p.sendMessage("§cNo active build session."); return true; }
        if (args.length < 1){
            p.sendMessage("§cUsage: /unrbspawn <bossId>");
            return true;
        }
        s.bossId = args[0];
        s.bossLoc = Util.locToString(p.getLocation());
        p.sendMessage("§aBoss spawn set: §f"+s.bossId+"§a.");
        return true;
    }

    private boolean cmdExfil(Player p){
        MapBuildSession s = builds.get(p);
        if (s == null){ p.sendMessage("§cNo active build session."); return true; }
        s.exfils.add(Util.locToString(p.getLocation()));
        p.sendMessage("§aAdded exfil spot.");
        return true;
    }

    private boolean cmdEvent(Player p, String[] args){
        MapBuildSession s = builds.get(p);
        if (s == null){ p.sendMessage("§cNo active build session."); return true; }
        if (args.length < 1){
            p.sendMessage("§cUsage: /unrevent <name>");
            return true;
        }
        String name = args[0];
        s.events.put(name, Util.locToString(p.getLocation()));
        p.sendMessage("§aSet event trigger §f"+name+"§a.");
        return true;
    }

    private boolean cmdFinalize(Player p, String[] args){
        MapBuildSession s = builds.get(p);
        if (s == null){ p.sendMessage("§cNo active build session."); return true; }
        if (args.length < 1){
            p.sendMessage("§cUsage: /unrmapfinalize <name>");
            return true;
        }
        String name = args[0].trim();
        if (!name.equalsIgnoreCase(s.mapName)){
            p.sendMessage("§cThis session is for map §f"+s.mapName+"§c.");
            return true;
        }

        MapRegistry reg = cfg.maps();
        // ensure base entry exists
        if (!reg.exists(name)){
            reg.create(name, s.worldName);
        } else {
            reg.setTemplateWorld(name, s.worldName);
        }
        reg.setEnabled(name, true);
        reg.setManual(name, true);
        reg.setMode(name, s.mode.name());
        reg.setAllowBreak(name, s.allowBreak);

        // store spawns
        for (var e : s.playerSpawns.entrySet()){
            reg.setSpawn(name, e.getKey(), e.getValue());
        }
        // boss/exfil
        if (s.bossId != null && s.bossLoc != null) { reg.setBoss(name, s.bossLoc); reg.setBoss(name, s.bossId, s.bossLoc); }
        if (!s.exfils.isEmpty()) { reg.setExfil(name, s.exfils.get(0)); reg.setExfils(name, new ArrayList<>(s.exfils)); }
        for (var e : s.events.entrySet()){
            reg.setEvent(name, e.getKey(), e.getValue());
        }

        // store mob/loot lists
        List<Map<String,Object>> mobList = new ArrayList<>();
        for (var ms : s.mobSpawns){
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("type", ms.entityType());
            m.put("level", ms.level());
            m.put("noBlockBreak", ms.noBlockBreak());
            m.put("loc", ms.loc());
            mobList.add(m);
        }
        reg.setMobSpawns(name, mobList);

        List<Map<String,Object>> lootList = new ArrayList<>();
        for (var ls : s.lootSpawns){
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("tier", ls.tier());
            m.put("loc", ls.loc());
            lootList.add(m);
        }
        reg.setLootSpawns(name, lootList);

        // boundary: if none recorded, scan around player for redstone blocks in radius
        List<MapBuildSession.BlockPos> pts = new ArrayList<>(s.boundary);
        if (pts.isEmpty()){
            pts.addAll(scanRedstoneBorder(p.getWorld(), p.getLocation(), cfg.getInt("maps.manual.borderScanRadius", 128)));
        }
        if (pts.size() < 4){
            p.sendMessage("§cNo border blocks recorded/found. Place REDSTONE_BLOCKs while building (or near you).");
            builds.stop(p);
            return true;
        }

        // order points by angle around centroid (stable ring)
        double cx = pts.stream().mapToDouble(MapBuildSession.BlockPos::x).average().orElse(0);
        double cz = pts.stream().mapToDouble(MapBuildSession.BlockPos::z).average().orElse(0);
        pts.sort(Comparator.comparingDouble(a -> Math.atan2(a.z()-cz, a.x()-cx)));

        // save boundary as strings
        List<String> boundStr = pts.stream().map(b -> b.x()+","+b.y()+","+b.z()).collect(Collectors.toList());
        reg.setBoundary(name, boundStr);

        // build barrier wall connected point-to-point, full height
        int minY = p.getWorld().getMinHeight();
        int maxY = p.getWorld().getMaxHeight()-1;
        int placed = buildBarrierWall(p.getWorld(), pts, minY, maxY);
        p.sendMessage("§aFinalized map §f"+name+"§a. Barrier blocks placed: §f"+placed+"§a.");
        builds.stop(p);
        return true;
    }

    private List<MapBuildSession.BlockPos> scanRedstoneBorder(World w, Location center, int radius){
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int r2 = radius*radius;
        List<MapBuildSession.BlockPos> out = new ArrayList<>();
        int minX = cx-radius, maxX = cx+radius;
        int minY = w.getMinHeight(), maxY = w.getMaxHeight()-1;
        int minZ = cz-radius, maxZ = cz+radius;
        // Scan only a thin band around player's Y +- 8 for speed
        int y0 = Math.max(minY, cy-8);
        int y1 = Math.min(maxY, cy+8);
        for (int x=minX;x<=maxX;x++){
            for (int z=minZ;z<=maxZ;z++){
                int dx=x-cx, dz=z-cz;
                if (dx*dx+dz*dz>r2) continue;
                for (int y=y0;y<=y1;y++){
                    if (w.getBlockAt(x,y,z).getType()==Material.REDSTONE_BLOCK){
                        out.add(new MapBuildSession.BlockPos(x,y,z));
                    }
                }
            }
        }
        return out;
    }

    private int buildBarrierWall(World w, List<MapBuildSession.BlockPos> pts, int minY, int maxY){
        int placed = 0;
        for (int i=0;i<pts.size();i++){
            MapBuildSession.BlockPos a = pts.get(i);
            MapBuildSession.BlockPos b = pts.get((i+1)%pts.size());
            placed += drawSegment(w, a.x(), a.z(), b.x(), b.z(), minY, maxY);
        }
        return placed;
    }

    private int drawSegment(World w, int x0, int z0, int x1, int z1, int minY, int maxY){
        int placed = 0;
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx - dz;
        int x = x0;
        int z = z0;
        while (true){
            for (int y=minY;y<=maxY;y++){
                Block b = w.getBlockAt(x,y,z);
                if (b.getType() != Material.BARRIER){
                    b.setType(Material.BARRIER, false);
                    placed++;
                }
            }
            if (x == x1 && z == z1) break;
            int e2 = 2*err;
            if (e2 > -dz){ err -= dz; x += sx; }
            if (e2 < dx){ err += dx; z += sz; }
        }
        return placed;
    }
}
