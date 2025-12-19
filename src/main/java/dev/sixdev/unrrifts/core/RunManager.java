package dev.sixdev.unrrifts.core;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RunManager {

    private final UnrRiftsPlugin plugin;
    private final ConfigService cfg;
    private final LeaderboardService leaderboard;

    // run world -> run instance
    private final Map<String, RunInstance> runs = new ConcurrentHashMap<>();

    // exfil timers per player
    private final Map<UUID, BukkitTask> exfilTasks = new ConcurrentHashMap<>();

    // bossbars per run
    private final Map<String, BossBar> bossBars = new ConcurrentHashMap<>();

    public RunManager(UnrRiftsPlugin plugin, ConfigService cfg, LeaderboardService leaderboard){
        this.plugin = plugin;
        this.cfg = cfg;
        this.leaderboard = leaderboard;
    }

    public RunInstance runByWorld(World w){
        if (w == null) return null;
        return runs.get(w.getName());
    }

    
public RunInstance runOf(org.bukkit.entity.Player p){
    if (p == null || p.getWorld() == null) return null;
    return runs.get(p.getWorld().getName());
}

public void startRun(LobbyGroup group){
        // create run world
        String worldName = "unrrift_"+System.currentTimeMillis()+"_"+group.id().toString().substring(0,8);
        RunInstance run = new RunInstance(group, worldName);

        plugin.getLogger().info("Starting run "+run.id+" for "+group.key()+" with "+group.size()+" players");

        if (group.key().mapChoiceType == MapChoiceType.CUSTOM){
            startCustomMap(run, group.key().customMap);
        } else {
            startGenerated(run, group.key().worldType);
        }
    }

    private void startGenerated(RunInstance run, WorldType type){
        WorldCreator wc = new WorldCreator(run.worldName);
        wc.environment(World.Environment.NORMAL);

        // For cave/dungeon we want underground-like feel. We'll still use normal, but we build at buildY.
        wc.type(WorldType2Flat(type));

        World world = wc.createWorld();
        if (world == null){
            plugin.getLogger().severe("Failed to create run world "+run.worldName);
            failRun(run, "World creation failed.");
            return;
        }

        // Configure world settings
        world.setAutoSave(false);
        world.setPVP(run.group.key().mode == RunMode.PVP);

        run.world = world;
        runs.put(world.getName(), run);

        // build structures async-ish then teleport
        Bukkit.getScheduler().runTask(plugin, () -> {
            GeneratedWorldBuilder builder = new GeneratedWorldBuilder(plugin, cfg);
            builder.build(run, type);

            beginRunTeleportAndKits(run);
            spawnBoss(run);
            startCompassTasks(run);
        });
    }

    private org.bukkit.WorldType WorldType2Flat(WorldType type){
        // Ravine/Woods can use NORMAL; Cave/Dungeon use FLAT to avoid mountain surface issues (we build underground anyway).
        return switch (type){
            case RAVINE_WORLD, WOODS_WORLD -> org.bukkit.WorldType.NORMAL;
            default -> org.bukkit.WorldType.FLAT;
        };
    }

    private void startCustomMap(RunInstance run, String mapName){
        MapRegistry reg = cfg.maps();
        if (!reg.exists(mapName) || !reg.enabled(mapName)){
            failRun(run, "Custom map not found or disabled: "+mapName);
            return;
        }
        String templateWorld = reg.templateWorld(mapName);
        if (templateWorld == null || templateWorld.isBlank()){
            failRun(run, "Template world not set for map: "+mapName);
            return;
        }

        // copy template world folder -> new world folder (same server root)
        try {
            File container = Bukkit.getWorldContainer();
            File src = new File(container, templateWorld);
            File dst = new File(container, run.worldName);
            if (!src.exists() || !src.isDirectory()){
                failRun(run, "Template world folder missing: "+templateWorld);
                return;
            }
            FileUtil.copyWorldFolder(src, dst);

            WorldCreator wc = new WorldCreator(run.worldName);
            World world = wc.createWorld();
            if (world == null){
                failRun(run, "Failed to load custom map world.");
                return;
            }
            world.setAutoSave(false);
            world.setPVP(run.group.key().mode == RunMode.PVP);

            run.world = world;
            run.mapName = mapName;
            run.manualMap = reg.manual(mapName);
            runs.put(world.getName(), run);

            // load locations
            run.hub = Util.stringToLoc(reg.hub(mapName));
            run.bossRoom = Util.stringToLoc(reg.boss(mapName));
            run.exfil = Util.stringToLoc(reg.exfil(mapName));

            // spawn points per slot
            int idx = 1;
            for (UUID u : run.group.players()){
                Location s = Util.stringToLoc(reg.spawn(mapName, idx));
                if (s == null){
                    // fallback to hub
                    s = run.hub != null ? run.hub : world.getSpawnLocation();
                }
                run.spawnByPlayer.put(u, s);
                idx++;
            }

            beginRunTeleportAndKits(run);
            if (reg.manual(mapName)) spawnManualContent(run, reg, mapName);
            spawnBoss(run);
            startCompassTasks(run);
        } catch (Exception e){
            failRun(run, "Failed to copy/load map: "+e.getMessage());
        }
    }

    private void beginRunTeleportAndKits(RunInstance run){
        // Set alive and teleport into run spawns
        for (UUID u : run.group.players()){
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;

            run.alive.add(u);

            // kits + hearts
            applyKitAndHearts(p, run.group);

            // teleport
            Location spawn = run.spawnByPlayer.get(u);
            if (spawn == null){
                spawn = run.hub != null ? run.hub : run.world.getSpawnLocation();
            }
            p.teleport(spawn);

            p.sendMessage("§5[unrRifts] §aRun started! Mode: §f"+run.group.key().mode.display());
        }
    }

    private void applyKitAndHearts(Player p, LobbyGroup group){
        p.getInventory().clear();
        String kitId = group.kitOf(p.getUniqueId());
        KitDefinition kit = cfg.kits().get(kitId);
        if (kit != null){
            for (ItemStack is : kit.buildItems()){
                // mark kit items so we can strip them on run end/leave
                markKitItem(is);
                p.getInventory().addItem(is);
            }
            p.sendMessage("§5[unrRifts] §7Kit selected: §f"+kit.displayName());
        }

        // hearts
        int minHearts = Math.max(1, cfg.minHearts());
        int maxHearts = Math.max(minHearts, cfg.maxHearts());
        double maxHealth = Math.min(maxHearts, 10) * 2.0; // vanilla cap for safety; config expects <=10
        double minHealth = Math.min(minHearts, 10) * 2.0;

        p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(maxHealth);
        p.setHealth(Math.min(p.getHealth(), maxHealth));
        if (p.getHealth() < minHealth) p.setHealth(minHealth);
        p.setFoodLevel(20);
        p.setSaturation(8f);
    }

    public void failRun(RunInstance run, String reason){
        plugin.getLogger().warning("Run failed: "+reason);
        // teleport everyone to lobby exit
        Location exit = Util.stringToLoc(cfg.lobbyExitStr());
        Location lobby = Util.stringToLoc(cfg.lobbySpawnStr());
        for (UUID u : run.group.players()){
            Player p = Bukkit.getPlayer(u);
            if (p != null){
                if (exit != null) p.teleport(exit);
                else if (lobby != null) p.teleport(lobby);
                p.sendMessage("§5[unrRifts] §cRun failed: "+reason);
            }
        }
        cleanupRunWorld(run);
    }

    public void handlePlayerDeath(Player p){
        RunInstance run = runByWorld(p.getWorld());
        if (run == null) return;

        run.alive.remove(p.getUniqueId());

        if (run.group.key().mode == RunMode.PVP){
            // if only one alive left -> allow exfil trigger when they reach exfil
            checkWinner(run);
        }
    }

    private void checkWinner(RunInstance run){
        if (run.group.key().mode != RunMode.PVP) return;
        if (run.alive.size() == 1){
            UUID winner = run.alive.iterator().next();
            Player p = Bukkit.getPlayer(winner);
            if (p != null){
                p.sendMessage("§5[unrRifts] §6You are the last one alive. Find Exfil!");
            }
        }
        if (run.alive.isEmpty()){
            endRun(run, null, false);
        }
    }

    public boolean canStartExfilTimer(RunInstance run, Player p){
        if (run == null) return false;
        if (run.exfil == null) return false;

        if (run.group.key().mode == RunMode.PVP){
            return run.alive.size() == 1 && run.alive.contains(p.getUniqueId());
        } else {
            return run.bossDefeated; // PvE gated by boss/objective
        }
    }

    public void tryStartExfil(Player p){
        RunInstance run = runByWorld(p.getWorld());
        if (run == null) return;
        if (!canStartExfilTimer(run, p)) return;
        if (exfilTasks.containsKey(p.getUniqueId())) return;

        int seconds = cfg.exfilTimerSeconds();
        p.sendMessage("§5[unrRifts] §aExfil started! Stay in zone for §e"+seconds+"§as.");

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int left = seconds;
            @Override public void run() {
                if (!p.isOnline()){
                    cancel();
                    return;
                }
                if (runByWorld(p.getWorld()) != run){
                    cancel();
                    return;
                }
                if (!isInExfil(run, p.getLocation())){
                    p.sendMessage("§5[unrRifts] §cExfil canceled (left zone).");
                    cancel();
                    return;
                }
                left--;
                if (left <= 0){
                    // exit
                    completeExfil(run, p);
                    cancel();
                } else if (left <= 5 || left % 5 == 0){
                    p.sendMessage("§5[unrRifts] §7Exfil in §e"+left+"§7s...");
                }
            }
            private void cancel(){
                BukkitTask t = exfilTasks.remove(p.getUniqueId());
                if (t != null) t.cancel();
            }
        }, 20L, 20L);

        exfilTasks.put(p.getUniqueId(), task);
    }

    public void cancelExfil(Player p){
        BukkitTask t = exfilTasks.remove(p.getUniqueId());
        if (t != null) t.cancel();
    }

    /**
     * Player-initiated leave ("/rift leave").
     * Removes the player from the current run (if any), strips kit items,
     * teleports to exit, and DOES NOT record leaderboard.
     */
    public void forceLeave(Player p){
        if (p == null || p.getWorld() == null) return;
        RunInstance run = runByWorld(p.getWorld());
        if (run == null) return;

        cancelExfil(p);
        // remove membership
        try { run.group.remove(p); } catch (Exception ignored) {}
        run.alive.remove(p.getUniqueId());

        // strip kit items (keep drops/loot)
        stripKitItems(p);

        Location exit = Util.stringToLoc(cfg.lobbyExitStr());
        Location lobby = Util.stringToLoc(cfg.lobbySpawnStr());
        if (exit != null) p.teleport(exit);
        else if (lobby != null) p.teleport(lobby);

        p.sendMessage("§5[unrRifts] §7You left the run.");

        if (run.alive.isEmpty()){
            endRun(run, null, false);
        }
    }

    // --- kit cleanup ---
    private final org.bukkit.NamespacedKey KIT_TAG = new org.bukkit.NamespacedKey(UnrRiftsPlugin.get(), "unrrifts_kit");

    private void markKitItem(org.bukkit.inventory.ItemStack is){
        if (is == null) return;
        var meta = is.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(KIT_TAG, org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
        is.setItemMeta(meta);
    }

    private void stripKitItems(Player p){
        if (p == null) return;
        var inv = p.getInventory();
        for (int i=0;i<inv.getSize();i++){
            var it = inv.getItem(i);
            if (it == null) continue;
            var meta = it.getItemMeta();
            if (meta == null) continue;
            Byte b = meta.getPersistentDataContainer().get(KIT_TAG, org.bukkit.persistence.PersistentDataType.BYTE);
            if (b != null && b == (byte)1){
                inv.setItem(i, null);
            }
        }
    }

    public boolean isInExfil(RunInstance run, Location loc){
        if (run.exfil == null || loc == null) return false;
        if (!loc.getWorld().equals(run.exfil.getWorld())) return false;
        return loc.distanceSquared(run.exfil) <= (cfg.exfilRadius() * cfg.exfilRadius());
    }

    private void completeExfil(RunInstance run, Player p){
        Location exit = Util.stringToLoc(cfg.lobbyExitStr());
        Location lobby = Util.stringToLoc(cfg.lobbySpawnStr());
        // strip kit items (keep drops/loot) before leaving the run
        stripKitItems(p);

        if (exit != null) p.teleport(exit);
        else if (lobby != null) p.teleport(lobby);

        p.sendMessage("§5[unrRifts] §aExfil successful!");
        run.alive.remove(p.getUniqueId());

        // record leaderboard only if boss token present and boss defeated (as spec)
        boolean hasToken = hasExfilToken(p);
        if (run.bossDefeated && hasToken){
            long timeMs = System.currentTimeMillis() - run.startTimeMs;
            leaderboard.recordWin(p.getUniqueId(), p.getName(), timeMs);
        }

        if (run.alive.isEmpty()){
            endRun(run, p, true);
        }
    }

    private boolean hasExfilToken(Player p){
        ItemStack token = cfg.exfilTokenItem();
        for (ItemStack is : p.getInventory().getContents()){
            if (is == null) continue;
            if (is.getType() == token.getType()){
                var meta = is.getItemMeta();
                var tmeta = token.getItemMeta();
                if (meta != null && tmeta != null && Objects.equals(meta.getDisplayName(), tmeta.getDisplayName())){
                    return true;
                }
            }
        }
        return false;
    }

    public void endRun(RunInstance run, Player winner, boolean natural){
        // remove bossbar
        BossBar bb = bossBars.remove(run.worldName);
        if (bb != null) bb.removeAll();

        // teleport remaining players out
        Location exit = Util.stringToLoc(cfg.lobbyExitStr());
        Location lobby = Util.stringToLoc(cfg.lobbySpawnStr());
        for (UUID u : new ArrayList<>(run.group.players())){
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            cancelExfil(p);
            if (p.getWorld().getName().equals(run.worldName)){
                stripKitItems(p);
                if (exit != null) p.teleport(exit);
                else if (lobby != null) p.teleport(lobby);
            }
            p.sendMessage("§5[unrRifts] §7Run ended.");
        }

        // schedule cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupRunWorld(run), cfg.runCleanupDelaySeconds() * 20L);
    }

    private void cleanupRunWorld(RunInstance run){
        // unload & delete folder
        try {
            World w = run.world;
            if (w != null){
                // kick any stragglers
                for (Player p : w.getPlayers()){
                    Location exit = Util.stringToLoc(cfg.lobbyExitStr());
                    Location lobby = Util.stringToLoc(cfg.lobbySpawnStr());
                    if (exit != null) p.teleport(exit);
                    else if (lobby != null) p.teleport(lobby);
                }
                Bukkit.unloadWorld(w, false);
            }
        } catch (Exception ignored){}

        runs.remove(run.worldName);

        try {
            File folder = new File(Bukkit.getWorldContainer(), run.worldName);
            FileUtil.deleteWorldFolder(folder);
        } catch (Exception e){
            plugin.getLogger().warning("Failed to delete run world folder: "+e.getMessage());
        }
    }

    
	private static String mapGetStr(java.util.Map<?,?> m, String key, String def){
	    Object v = m.get(key);
	    return v == null ? def : String.valueOf(v);
	}
	private static int mapGetInt(java.util.Map<?,?> m, String key, int def){
	    Object v = m.get(key);
	    if (v == null) return def;
	    try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignored){ return def; }
	}

	private void spawnManualContent(RunInstance run, MapRegistry reg, String mapName){
    try {
        // Loot spawns
        for (var o : reg.lootSpawns(mapName)){
            if (!(o instanceof java.util.Map<?,?> m)) continue;
	            String tier = mapGetStr(m, "tier", "T1");
	            String locS = mapGetStr(m, "loc", "");
            org.bukkit.Location l = Util.stringToLoc(locS);
            if (l == null) continue;
            l.setWorld(run.world);

            org.bukkit.block.Block b = run.world.getBlockAt(l);
            b.setType(org.bukkit.Material.CHEST, false);
            org.bukkit.block.BlockState st = b.getState();
            if (st instanceof org.bukkit.block.Chest chest){
                fillChest(chest.getBlockInventory(), tier);
            }
        }

        // Mob spawns
        for (var o : reg.mobSpawns(mapName)){
            if (!(o instanceof java.util.Map<?,?> m)) continue;
	            String typeS = mapGetStr(m, "type", "ZOMBIE");
	            int level = mapGetInt(m, "level", 1);
	            String locS = mapGetStr(m, "loc", "");
            org.bukkit.Location l = Util.stringToLoc(locS);
            if (l == null) continue;
            l.setWorld(run.world);

            org.bukkit.entity.EntityType type;
            try { type = org.bukkit.entity.EntityType.valueOf(Util.upper(typeS)); }
            catch (Exception ex){ type = org.bukkit.entity.EntityType.ZOMBIE; }

            org.bukkit.entity.Entity e = run.world.spawnEntity(l, type);
            e.setPersistent(true);
            // simple "level" => extra health if living
            if (e instanceof org.bukkit.entity.LivingEntity le){
                double base = le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null ?
                        le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue() : 20.0;
                double hp = Math.min(200.0, base + (level-1)*5.0);
                if (le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null){
                    le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(hp);
                }
                le.setHealth(Math.min(hp, le.getHealth()));
            }
        }

        // Exfil override (use first)
        java.util.List<String> ex = reg.exfils(mapName);
        if (ex != null && !ex.isEmpty()){
            org.bukkit.Location l = Util.stringToLoc(ex.get(0));
            if (l != null){
                l.setWorld(run.world);
                run.exfil = l;
            }
        }

        // Boss override via manual boss loc (already loaded), bossId currently only affects name if configured
        String bossId = reg.bossId(mapName);
        if (bossId != null && !bossId.isBlank()){
            run.customBossId = bossId;
        }
    } catch (Exception ex){
        plugin.getLogger().warning("Manual content spawn failed: "+ex.getMessage());
    }
}

private void fillChest(org.bukkit.inventory.Inventory inv, String tier){
    try {
        LootConfig lc = cfg.loot();
        java.util.List<LootConfig.Tier> tiers = lc.tiers();
        LootConfig.Tier chosen = null;
        for (LootConfig.Tier t : tiers){
            if (t.id().equalsIgnoreCase(tier)){
                chosen = t; break;
            }
        }
        if (chosen == null && !tiers.isEmpty()) chosen = tiers.get(0);
        if (chosen == null) return;

        java.util.Random rnd = new java.util.Random();
        int rolls = Math.max(1, cfg.getInt("loot.manual.rollsPerChest", 4));
        for (int i=0;i<rolls;i++){
            String itemS = chosen.items().get(rnd.nextInt(chosen.items().size()));
            org.bukkit.inventory.ItemStack it = ItemParser.parse(itemS);
            if (it == null) continue;
            inv.addItem(it);
        }
    } catch (Exception ignored){}
}

private void spawnBoss(RunInstance run){
        if (run.bossRoom == null) return;

        String bossId = run.customBossId;
EntityType type = cfg.bossType();
String name = cfg.bossName();
double health = cfg.bossHealth();
if (bossId != null && !bossId.isBlank()){
    String path = "bosses."+bossId+".";
    String t = plugin.getConfig().getString(path+"type", "");
    if (t != null && !t.isBlank()){
        try { type = EntityType.valueOf(Util.upper(t)); } catch (Exception ignored) {}
    }
    String n = plugin.getConfig().getString(path+"name", "");
    if (n != null && !n.isBlank()) name = n;
    health = plugin.getConfig().getDouble(path+"health", health);
}

LivingEntity boss = (LivingEntity) run.world.spawnEntity(run.bossRoom, type);
boss.setCustomName("§c"+name);

        boss.setCustomNameVisible(true);
        boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(health);
        boss.setHealth(health);
        boss.setPersistent(true);
        boss.setRemoveWhenFarAway(false);

        BossBar bar = Bukkit.createBossBar("§c"+name, BarColor.PURPLE, BarStyle.SEGMENTED_20);
        bar.setVisible(true);
        bossBars.put(run.worldName, bar);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            RunInstance r = runs.get(run.worldName);
            if (r == null) { bar.removeAll(); return; }
            if (!boss.isValid() || boss.isDead()){
                if (!r.bossDefeated){
                    r.bossDefeated = true;
                    bar.setProgress(0.0);
                    bar.setTitle("§aBoss defeated!");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> { bar.removeAll(); bossBars.remove(run.worldName); }, 20L * 8);

                    // give exfil token to alive players
                    if (cfg.bossDropsToken()){
                        for (UUID u : new HashSet<>(r.alive)){
                            Player p = Bukkit.getPlayer(u);
                            if (p != null && p.getWorld().getName().equals(r.worldName)){
                                p.getInventory().addItem(cfg.exfilTokenItem());
                                p.sendMessage("§5[unrRifts] §dYou received an Exfil Token!");
                            }
                        }
                    }
                }
                return;
            }
            double max = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
            double cur = boss.getHealth();
            double prog = Math.max(0.0, Math.min(1.0, cur / max));
            bar.setProgress(prog);

            // show bar to players in run world
            for (UUID u : r.group.players()){
                Player p = Bukkit.getPlayer(u);
                if (p != null && p.getWorld().getName().equals(r.worldName)) bar.addPlayer(p);
                else if (p != null) bar.removePlayer(p);
            }
        }, 20L, 20L);
    }

    private void startCompassTasks(RunInstance run){
        // exfil marker: set compass target
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            RunInstance r = runs.get(run.worldName);
            if (r == null) return;
            if (r.exfil == null) return;
            for (UUID u : r.group.players()){
                Player p = Bukkit.getPlayer(u);
                if (p == null) continue;
                if (!p.getWorld().getName().equals(r.worldName)) continue;
                p.setCompassTarget(r.exfil);
                // auto toggle info after boss
                if (r.bossDefeated){
                    p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent("§aEXFIL UNLOCKED §7→ Follow your compass"));
                }
            }
        }, 20L, 20L * 2);
    }

    public void shutdown(){
        for (RunInstance run : new ArrayList<>(runs.values())){
            endRun(run, null, false);
        }
        runs.clear();
        for (BukkitTask t : exfilTasks.values()) t.cancel();
        exfilTasks.clear();
    }
}
