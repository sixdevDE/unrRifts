package dev.sixdev.unrrifts.core;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class ItemParser {
    private ItemParser(){}

    // "MATERIAL" or "MATERIAL:amount" or "MATERIAL:min-max"
    public static ItemStack parse(String spec){
        if (spec == null || spec.isBlank()) return null;
        String[] p = spec.split(":");
        String matS = p[0].trim().toUpperCase(Locale.ROOT);
        Material m = Material.matchMaterial(matS);
        if (m == null) return null;

        int amt = 1;
        if (p.length >= 2){
            String a = p[1].trim();
            if (a.contains("-")){
                String[] r = a.split("-");
                int min = Integer.parseInt(r[0].trim());
                int max = Integer.parseInt(r[1].trim());
                if (max < min) { int t = min; min=max; max=t; }
                amt = min + (int)Math.floor(Math.random() * (max - min + 1));
            } else {
                amt = Integer.parseInt(a);
            }
        }
        if (amt < 1) amt = 1;
        return new ItemStack(m, amt);
    }
}
