package dev.sixdev.unrrifts.gui;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import dev.sixdev.unrrifts.core.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GuiListener implements Listener {

    private final UnrRiftsPlugin plugin;
    private final ConfigService cfg;
    private final LobbyGroupManager groups;

    private final Map<UUID, GuiSession> session = new HashMap<>();

    private static final String TITLE_ROOT = "§5unrRifts";
    private static final String TITLE_MAP = "§5unrRifts §8— §fMap";
    private static final String TITLE_WORLD = "§5unrRifts §8— §fWorldType";
    private static final String TITLE_MODE = "§5unrRifts §8— §fMode";
    private static final String TITLE_KIT = "§5unrRifts §8— §fKit";
    private static final String TITLE_QUEUE = "§5unrRifts §8— §fQueue";

    public GuiListener(UnrRiftsPlugin plugin, ConfigService cfg, LobbyGroupManager groups){
        this.plugin = plugin;
        this.cfg = cfg;
        this.groups = groups;
    }

    public void openRoot(Player p){
        session.putIfAbsent(p.getUniqueId(), new GuiSession());
        openMapChoice(p);
    }

    private void openMapChoice(Player p){
        GuiSession s = session.computeIfAbsent(p.getUniqueId(), k -> new GuiSession());
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAP);

        inv.setItem(11, item(Material.GRASS_BLOCK, "§aGenerated", List.of("§7Use the built-in generator")));
        inv.setItem(15, item(Material.MAP, "§bCustom Map", List.of("§7Use an admin-built map")));

        inv.setItem(26, item(Material.BARRIER, "§cClose", List.of("§7Close GUI")));
        p.openInventory(inv);
    }

    private void openWorldType(Player p){
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_WORLD);

        inv.setItem(10, item(Material.DEEPSLATE, "§fCAVE RIFT", List.of("§7Underground hub + radial rifts")));
        inv.setItem(12, item(Material.DEEPSLATE_BRICKS, "§fDUNGEON ROOMS", List.of("§7Room chains + boss + exfil")));
        inv.setItem(14, item(Material.STONE, "§fRAVINE WORLD", List.of("§7Large ravine-like traversal")));
        inv.setItem(16, item(Material.OAK_LOG, "§fWOODS WORLD", List.of("§7Forest setting with run structure")));

        inv.setItem(22, item(Material.ARROW, "§eNext", List.of("§7Choose mode")));
        inv.setItem(26, item(Material.BARRIER, "§cBack", List.of("§7Back")));
        p.openInventory(inv);
    }

    private void openCustomMap(Player p){
        Inventory inv = Bukkit.createInventory(null, 54, "§5unrRifts §8— §fCustom Maps");
        MapRegistry reg = cfg.maps();

        int slot = 0;
        for (String name : reg.names()){
            if (!reg.enabled(name)) continue;
            ItemStack it = item(Material.FILLED_MAP, "§b"+name, List.of("§7Custom map", "§7Click to select"));
            inv.setItem(slot++, it);
            if (slot >= 45) break;
        }

        inv.setItem(49, item(Material.ARROW, "§eNext", List.of("§7Choose mode")));
        inv.setItem(53, item(Material.BARRIER, "§cBack", List.of("§7Back")));
        p.openInventory(inv);
    }

    private void openMode(Player p){
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MODE);
        inv.setItem(11, item(Material.DIAMOND_SWORD, "§cPvP", List.of("§7Last man standing", "§7Exfil only for winner")));
        inv.setItem(15, item(Material.IRON_SWORD, "§aPvE", List.of("§7Boss objective", "§7Party can exfil")));
        inv.setItem(22, item(Material.ARROW, "§eNext", List.of("§7Choose kit")));
        inv.setItem(26, item(Material.BARRIER, "§cBack", List.of("§7Back")));
        p.openInventory(inv);
    }

    private void openKit(Player p){
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_KIT);

        int slot=0;
        for (KitDefinition k : cfg.kits().values()){
            ItemStack it = item(k.icon(), k.displayName(), List.of("§7Click to select", "§8ID: "+k.id()));
            inv.setItem(slot++, it);
            if (slot >= 45) break;
        }

        inv.setItem(49, item(Material.LIME_CONCRETE, "§aReady / Join Queue", List.of("§7Requires kit selection")));
        inv.setItem(53, item(Material.BARRIER, "§cBack", List.of("§7Back")));
        p.openInventory(inv);
    }

    private void openQueue(Player p){
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_QUEUE);

        GuiSession s = session.get(p.getUniqueId());
        if (s == null) s = new GuiSession();

        LobbyGroup g = groups.groupOf(p);
        String keyStr = s.mapChoiceType == MapChoiceType.CUSTOM ? ("Custom: "+s.customMap) : ("Generated: "+s.worldType.display());
        String kit = (s.kitId == null || s.kitId.isBlank()) ? "§cNot selected" : "§a"+s.kitId;

        List<String> lore = new ArrayList<>();
        lore.add("§7Map: §f"+keyStr);
        lore.add("§7Mode: §f"+s.mode.display());
        lore.add("§7Kit: "+kit);
        if (g != null){
            lore.add("§7Group: §f"+g.id().toString().substring(0,8));
            lore.add("§7Players: §f"+g.size()+"§7/§f"+cfg.maxPlayers());
            lore.add("§7Ready: §f"+g.kits().size()+"§7/§f"+g.size());
            if (g.countdown() != null){
                lore.add("§eCountdown: §f"+g.countdown().left()+"s");
            } else if (g.size() >= cfg.minPlayers() && g.allKitsSelected()){
                lore.add("§aStarting soon...");
            } else {
                lore.add("§cWaiting for players/kits...");
            }
        } else {
            lore.add("§cNot queued.");
        }

        inv.setItem(13, item(Material.NETHER_STAR, "§fStatus", lore));
        inv.setItem(11, item(Material.BOOK, "§eChange Kit", List.of("§7Open kit selection")));
        inv.setItem(15, item(Material.BARRIER, "§cLeave Queue", List.of("§7Leave your current group")));
        inv.setItem(26, item(Material.BARRIER, "§cClose", List.of("§7Close")));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getCurrentItem() == null) return;
        String title = e.getView().getTitle();
        if (!title.startsWith("§5unrRifts")) return;

        e.setCancelled(true);

        GuiSession s = session.computeIfAbsent(p.getUniqueId(), k -> new GuiSession());
        ItemStack it = e.getCurrentItem();
        Material m = it.getType();

        // MAP CHOICE
        if (title.equals(TITLE_MAP)){
            if (m == Material.GRASS_BLOCK){
                s.mapChoiceType = MapChoiceType.GENERATED;
                openWorldType(p);
                return;
            }
            if (m == Material.MAP || m == Material.FILLED_MAP){
                s.mapChoiceType = MapChoiceType.CUSTOM;
                openCustomMap(p);
                return;
            }
            if (m == Material.BARRIER){
                p.closeInventory();
                return;
            }
        }

        // WORLD TYPE
        if (title.equals(TITLE_WORLD)){
            if (m == Material.DEEPSLATE) s.worldType = WorldType.CAVE_RIFT;
            if (m == Material.DEEPSLATE_BRICKS) s.worldType = WorldType.DUNGEON_ROOMS;
            if (m == Material.STONE) s.worldType = WorldType.RAVINE_WORLD;
            if (m == Material.OAK_LOG) s.worldType = WorldType.WOODS_WORLD;

            if (m == Material.ARROW){
                openMode(p);
                return;
            }
            if (m == Material.BARRIER){
                openMapChoice(p);
                return;
            }
            // stay, allow choosing then next
            p.sendMessage("§5[unrRifts] §7WorldType: §f"+s.worldType.display());
            return;
        }

        // CUSTOM MAP LIST
        if (title.equals("§5unrRifts §8— §fCustom Maps")){
            if (m == Material.FILLED_MAP){
                String name = stripColor(it.getItemMeta() != null ? it.getItemMeta().getDisplayName() : "");
                s.customMap = name;
                p.sendMessage("§5[unrRifts] §7Custom map: §f"+name);
                return;
            }
            if (m == Material.ARROW){
                if (s.customMap == null || s.customMap.isBlank()){
                    p.sendMessage("§5[unrRifts] §cSelect a custom map first.");
                    return;
                }
                openMode(p);
                return;
            }
            if (m == Material.BARRIER){
                openMapChoice(p);
                return;
            }
        }

        // MODE
        if (title.equals(TITLE_MODE)){
            if (m == Material.DIAMOND_SWORD) s.mode = RunMode.PVP;
            if (m == Material.IRON_SWORD) s.mode = RunMode.PVE;

            if (m == Material.ARROW){
                openKit(p);
                return;
            }
            if (m == Material.BARRIER){
                if (s.mapChoiceType == MapChoiceType.CUSTOM) openCustomMap(p);
                else openWorldType(p);
                return;
            }
            p.sendMessage("§5[unrRifts] §7Mode: §f"+s.mode.display());
            return;
        }

        // KIT
        if (title.equals(TITLE_KIT)){
            if (m == Material.LIME_CONCRETE){
                if (s.kitId == null || s.kitId.isBlank()){
                    p.sendMessage("§5[unrRifts] §cSelect a kit first!");
                    return;
                }
                // join queue + set kit
                LobbyKey key = s.toLobbyKey();
                groups.join(p, key);
                groups.setKit(p, s.kitId);
                openQueue(p);
                return;
            }
            if (m == Material.BARRIER){
                openMode(p);
                return;
            }
            // selecting a kit by icon match
            String kitId = resolveKitIdByDisplayOrIcon(it);
            if (kitId != null){
                s.kitId = kitId;
                p.sendMessage("§5[unrRifts] §7Kit: §f"+cfg.kits().get(kitId).displayName());
            }
            return;
        }

        // QUEUE
        if (title.equals(TITLE_QUEUE)){
            if (m == Material.BOOK){
                openKit(p);
                return;
            }
            if (m == Material.BARRIER){
                // could be leave or close, check name
                String dn = it.getItemMeta() != null ? it.getItemMeta().getDisplayName() : "";
                if (dn.contains("Leave Queue")){
                    groups.leave(p);
                    p.sendMessage("§5[unrRifts] §7Left queue.");
                    openKit(p);
                    return;
                }
                p.closeInventory();
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        // no-op (we keep session)
    }

    private String resolveKitIdByDisplayOrIcon(ItemStack it){
        if (it == null) return null;
        // match by icon first
        for (KitDefinition k : cfg.kits().values()){
            if (k.icon() == it.getType()) return k.id();
        }
        // fallback by display
        String dn = it.getItemMeta() != null ? it.getItemMeta().getDisplayName() : "";
        dn = stripColor(dn);
        for (KitDefinition k : cfg.kits().values()){
            if (stripColor(k.displayName()).equalsIgnoreCase(dn)) return k.id();
        }
        return null;
    }

    private ItemStack item(Material m, String name, List<String> lore){
        ItemStack it = new ItemStack(m, 1);
        var meta = it.getItemMeta();
        if (meta != null){
            meta.setDisplayName(name);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String stripColor(String s){
        return s == null ? "" : s.replaceAll("§[0-9a-fk-or]", "");
    }
}
