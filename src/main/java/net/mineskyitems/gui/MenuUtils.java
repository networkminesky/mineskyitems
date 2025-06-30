package net.mineskyitems.gui;

import net.mineskyitems.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MenuUtils {

    public static ItemStack simpleButton(Material m, String name, String... lore) {
        return simpleButton(m, name, 1, lore);
    }

    public static ItemStack simpleButton(Material m, String name, int count, String... lore) {
        ItemStack it = new ItemStack(m, count);
        ItemMeta im = it.getItemMeta();

        im.setDisplayName("§6§l"+name);

        im.setLore(Arrays.stream(lore)
                .map(a -> Utils.c("&7"+a))
                .collect(Collectors.toList()));

        it.setItemMeta(im);
        return it;
    }

    public static ItemStack modelButton(Material m, String name, int model, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();

        im.setCustomModelData(model);

        im.setDisplayName("§6§l"+name);

        im.setLore(Arrays.stream(lore)
                .map(a -> Utils.c("&7"+a))
                .collect(Collectors.toList()));

        it.setItemMeta(im);
        return it;
    }



}
