package dev.sixdev.unrrifts.core;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GeneratedWorldBuilder {

    private final UnrRiftsPlugin plugin;
    private final ConfigService cfg;

    public GeneratedWorldBuilder(UnrRiftsPlugin plugin, ConfigService cfg){
        this.plugin = plugin;
        this.cfg = cfg;
    }

    public void build(RunInstance run, WorldType type){
        World w = run.world;
        if (w == null) return;

        int buildY = cfg.runBuildY();
        int players = Math.max(1, Math.min(6, run.group.size()));

        // center
        int cx = 0;
        int cz = 0;

        // base material palette
        Material wall = switch (type){
            case WOODS_WORLD -> Material.OAK_LOG;
            case RAVINE_WORLD -> Material.STONE;
            case DUNGEON_ROOMS -> Material.DEEPSLATE_BRICKS;
            default -> Material.DEEPSLATE;
        };
        Material floor = switch (type){
            case WOODS_WORLD -> Material.MOSS_BLOCK;
            case RAVINE_WORLD -> Material.TUFF;
            case DUNGEON_ROOMS -> Material.DEEPSLATE_TILES;
            default -> Material.STONE;
        };
        Material light = switch (type){
            case WOODS_WORLD -> Material.LANTERN;
            default -> Material.SOUL_LANTERN;
        };

        // clear a big area (lightweight)
        int r = 140;
        for (int x = -r; x <= r; x++){
            for (int z = -r; z <= r; z++){
                int y0 = buildY-12;
                int y1 = buildY+28;
                for (int y=y0; y<=y1; y++){
                    w.getBlockAt(cx+x,y,cz+z).setType(Material.AIR, false);
                }
            }
        }

        // create a "hub cave" (ellipsoid-ish) for cave/dungeon; for ravine/woods we still build a hub room
        Location hub = new Location(w, cx+0.5, buildY+1, cz+0.5, 0f, 0f);
        run.hub = hub;

        buildEllipsoid(w, cx, buildY+2, cz, 18, 10, 18, Material.AIR);
        // shell
        buildEllipsoidShell(w, cx, buildY+2, cz, 19, 11, 19, wall);
        fillFloor(w, cx, buildY, cz, 16, floor);

        // start rooms: one per player (1..6), attached to hub via small tunnel
        List<Location> startSpawns = new ArrayList<>();
        double angleStep = 360.0 / players;
        int startRoomDist = 24;
        for (int i=0;i<players;i++){
            double ang = Math.toRadians(i*angleStep);
            int rx = cx + (int)Math.round(Math.cos(ang) * startRoomDist);
            int rz = cz + (int)Math.round(Math.sin(ang) * startRoomDist);

            buildRoom(w, rx, buildY+2, rz, 9, 7, 9, wall, floor, light);
            carveTunnel(w, cx, buildY+2, cz, rx, buildY+2, rz, 3, wall);

            Location spawn = new Location(w, rx+0.5, buildY+1, rz+0.5, (float)(i*angleStep+180), 0f);
            startSpawns.add(spawn);
        }

        int idx=0;
        for (UUID u : run.group.players()){
            if (idx < startSpawns.size()) run.spawnByPlayer.put(u, startSpawns.get(idx));
            idx++;
        }

        // Build radial rift branches + ring connection
        int branches = Math.max(3, players);
        int branchLen = 70;
        List<Location> branchEnds = new ArrayList<>();
        double step = 360.0 / branches;

        for (int b=0;b<branches;b++){
            double ang = Math.toRadians(b*step);
            int bx = cx + (int)Math.round(Math.cos(ang)*branchLen);
            int bz = cz + (int)Math.round(Math.sin(ang)*branchLen);

            // carve branch tunnel and a mid-room and an end-room
            int midx = cx + (int)Math.round(Math.cos(ang)*(branchLen*0.5));
            int midz = cz + (int)Math.round(Math.sin(ang)*(branchLen*0.5));
            carveTunnel(w, cx, buildY+2, cz, midx, buildY+2, midz, 3, wall);
            buildRoom(w, midx, buildY+2, midz, 12, 8, 12, wall, floor, light);

            carveTunnel(w, midx, buildY+2, midz, bx, buildY+2, bz, 3, wall);
            buildRoom(w, bx, buildY+2, bz, 16, 10, 16, wall, floor, light);
            branchEnds.add(new Location(w, bx, buildY+2, bz));
        }

        // ring connection between branch ends
        for (int i=0;i<branchEnds.size();i++){
            Location a = branchEnds.get(i);
            Location b = branchEnds.get((i+1)%branchEnds.size());
            carveTunnel(w, a.getBlockX(), buildY+2, a.getBlockZ(), b.getBlockX(), buildY+2, b.getBlockZ(), 3, wall);
        }

        // Boss room at one branch end (farthest)
        Location bossCenter = branchEnds.get(0);
        buildRoom(w, bossCenter.getBlockX(), buildY+2, bossCenter.getBlockZ(), 22, 12, 22, wall, floor, Material.CRYING_OBSIDIAN);
        run.bossRoom = new Location(w, bossCenter.getBlockX()+0.5, buildY+1, bossCenter.getBlockZ()+0.5);

        // Exfil on opposite side
        Location ex = branchEnds.get(branchEnds.size()/2);
        buildExfilZone(w, ex.getBlockX(), buildY+1, ex.getBlockZ());
        run.exfil = new Location(w, ex.getBlockX()+0.5, buildY+1, ex.getBlockZ()+0.5);

        // Loot chests in mid and end rooms
        placeLoot(run, type, floor);

        // Set spawn location to hub
        w.setSpawnLocation(run.hub);

        // make sure it's playable (some ambient light)
        for (int x=-80;x<=80;x+=8){
            for (int z=-80;z<=80;z+=8){
                w.getBlockAt(cx+x, buildY+6, cz+z).setType(Material.GLOWSTONE, false);
            }
        }
    }

    private void placeLoot(RunInstance run, WorldType type, Material floor){
        LootConfig loot = cfg.loot();
        Random rnd = new Random();
        int buildY = cfg.runBuildY();
        World w = run.world;

        // pick some positions around the branches area
        int branches = Math.max(3, run.group.size());
        int branchLen = 70;
        double step = 360.0 / branches;

        for (int b=0;b<branches;b++){
            int chests = loot.chestsPerBranchMin() + rnd.nextInt(Math.max(1, loot.chestsPerBranchMax()-loot.chestsPerBranchMin()+1));
            for (int c=0;c<chests;c++){
                double ang = Math.toRadians(b*step);
                double dist = 30 + rnd.nextInt(40);
                int x = (int)Math.round(Math.cos(ang)*dist);
                int z = (int)Math.round(Math.sin(ang)*dist);
                int y = buildY+1;

                // place chest on floor
                Block base = w.getBlockAt(x, y-1, z);
                base.setType(floor, false);
                Block chestB = w.getBlockAt(x, y, z);
                chestB.setType(Material.CHEST, false);

                if (chestB.getState() instanceof Chest chest){
                    fillChest(chest.getBlockInventory(), loot);
                }
                // add some light near it
                w.getBlockAt(x, y+2, z).setType(Material.LANTERN, false);
            }
        }
    }

    private void fillChest(Inventory inv, LootConfig loot){
        Random rnd = new Random();
        List<LootConfig.Tier> tiers = loot.tiers();
        if (tiers.isEmpty()) return;

        int rolls = 2 + rnd.nextInt(3);
        for (int i=0;i<rolls;i++){
            LootConfig.Tier t = pickTier(tiers, rnd);
            if (t == null || t.items().isEmpty()) continue;
            String spec = t.items().get(rnd.nextInt(t.items().size()));
            ItemStack is = ItemParser.parse(spec);
            if (is != null) inv.addItem(is);
        }

        // heart upgrade as special item (consumable)
        if (rnd.nextDouble() < loot.heartUpgradeChance()){
            ItemStack heart = new ItemStack(Material.NETHER_STAR, 1);
            var meta = heart.getItemMeta();
            if (meta != null){
                meta.setDisplayName("§cHeart Upgrade");
                meta.setLore(List.of("§7Right-click to gain +1 heart", "§7Up to max "+cfg.maxHearts()));
                heart.setItemMeta(meta);
            }
            inv.addItem(heart);
        }
    }

    private LootConfig.Tier pickTier(List<LootConfig.Tier> tiers, Random rnd){
        int total = 0;
        for (var t : tiers) total += Math.max(1, t.weight());
        int roll = rnd.nextInt(Math.max(1,total));
        int cur = 0;
        for (var t : tiers){
            cur += Math.max(1, t.weight());
            if (roll < cur) return t;
        }
        return tiers.get(0);
    }

    private void buildExfilZone(World w, int cx, int y, int cz){
        int r = cfg.exfilRadius();
        for (int x=-r;x<=r;x++){
            for (int z=-r;z<=r;z++){
                if (x*x+z*z > r*r) continue;
                w.getBlockAt(cx+x,y-1,cz+z).setType(Material.QUARTZ_BLOCK,false);
                w.getBlockAt(cx+x,y,cz+z).setType(Material.AIR,false);
            }
        }
        // marker
        w.getBlockAt(cx, y, cz).setType(Material.BEACON, false);
        w.getBlockAt(cx, y+1, cz).setType(Material.GLASS, false);
        w.getBlockAt(cx, y+2, cz).setType(Material.SEA_LANTERN, false);
        // label-ish
        w.getBlockAt(cx+2, y, cz).setType(Material.EMERALD_BLOCK, false);
    }

    private void buildRoom(World w, int cx, int cy, int cz, int sx, int sy, int sz, Material wall, Material floor, Material light){
        int hx = sx/2;
        int hz = sz/2;
        for (int x=cx-hx; x<=cx+hx; x++){
            for (int y=cy-sy/2; y<=cy+sy/2; y++){
                for (int z=cz-hz; z<=cz+hz; z++){
                    boolean border = (x==cx-hx || x==cx+hx || y==cy-sy/2 || y==cy+sy/2 || z==cz-hz || z==cz+hz);
                    Block b = w.getBlockAt(x,y,z);
                    if (border) b.setType(wall,false);
                    else b.setType(Material.AIR,false);
                }
            }
        }
        // floor
        for (int x=cx-hx+1; x<=cx+hx-1; x++){
            for (int z=cz-hz+1; z<=cz+hz-1; z++){
                w.getBlockAt(x, cy-sy/2+1, z).setType(floor,false);
            }
        }
        // ceiling lights
        for (int x=cx-hx+2; x<=cx+hx-2; x+=4){
            for (int z=cz-hz+2; z<=cz+hz-2; z+=4){
                w.getBlockAt(x, cy+sy/2-1, z).setType(light,false);
            }
        }
        // some wall lights
        Random rnd = new Random();
        for (int i=0;i<6;i++){
            int x = cx - hx + 2 + rnd.nextInt(Math.max(1,sx-4));
            int z = cz - hz + 2 + rnd.nextInt(Math.max(1,sz-4));
            w.getBlockAt(x, cy, z).setType(Material.TORCH,false);
        }
    }

    private void carveTunnel(World w, int x1,int y1,int z1, int x2,int y2,int z2, int radius, Material shell){
        // simple Bresenham-like step
        int dx = x2-x1, dy=y2-y1, dz=z2-z1;
        int steps = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
        if (steps < 1) steps = 1;
        for (int i=0;i<=steps;i++){
            double t = i/(double)steps;
            int x = (int)Math.round(x1 + dx*t);
            int y = (int)Math.round(y1 + dy*t);
            int z = (int)Math.round(z1 + dz*t);

            for (int ox=-radius;ox<=radius;ox++){
                for (int oy=-radius;oy<=radius;oy++){
                    for (int oz=-radius;oz<=radius;oz++){
                        double d = ox*ox + oy*oy + oz*oz;
                        Block b = w.getBlockAt(x+ox,y+oy,z+oz);
                        if (d <= radius*radius){
                            b.setType(Material.AIR,false);
                        } else if (d <= (radius+1)*(radius+1)){
                            if (b.getType() == Material.AIR) b.setType(shell,false);
                        }
                    }
                }
            }
            // add occasional wall light at player level +5
            if (i % 10 == 0){
                w.getBlockAt(x, y+5, z).setType(Material.SOUL_TORCH,false);
            }
        }
    }

    private void buildEllipsoid(World w, int cx,int cy,int cz, int rx,int ry,int rz, Material fill){
        for (int x=cx-rx; x<=cx+rx; x++){
            for (int y=cy-ry; y<=cy+ry; y++){
                for (int z=cz-rz; z<=cz+rz; z++){
                    double nx=(x-cx)/(double)rx, ny=(y-cy)/(double)ry, nz=(z-cz)/(double)rz;
                    if (nx*nx+ny*ny+nz*nz <= 1.0){
                        w.getBlockAt(x,y,z).setType(fill,false);
                    }
                }
            }
        }
    }
    private void buildEllipsoidShell(World w, int cx,int cy,int cz, int rx,int ry,int rz, Material shell){
        for (int x=cx-rx; x<=cx+rx; x++){
            for (int y=cy-ry; y<=cy+ry; y++){
                for (int z=cz-rz; z<=cz+rz; z++){
                    double nx=(x-cx)/(double)rx, ny=(y-cy)/(double)ry, nz=(z-cz)/(double)rz;
                    double v = nx*nx+ny*ny+nz*nz;
                    if (v <= 1.0 && v >= 0.92){
                        w.getBlockAt(x,y,z).setType(shell,false);
                    }
                }
            }
        }
    }
    private void fillFloor(World w, int cx, int y, int cz, int r, Material floor){
        for (int x=cx-r; x<=cx+r; x++){
            for (int z=cz-r; z<=cz+r; z++){
                if ((x-cx)*(x-cx)+(z-cz)*(z-cz) <= r*r){
                    w.getBlockAt(x, y, z).setType(floor,false);
                }
            }
        }
    }
}
