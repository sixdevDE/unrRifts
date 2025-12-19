package dev.sixdev.unrrifts.core;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public record KitDefinition(
        String id,
        String displayName,
        Material icon,
        List<String> itemSpecs
) {
    public List<ItemStack> buildItems(){
        List<ItemStack> out = new ArrayList<>();
        for (String spec : itemSpecs){
            ItemStack is = ItemParser.parse(spec);
            if (is != null) out.add(is);
        }
        return out;
    }
}
