package net.minesky.entities.item;

import net.minesky.MineSkyItems;
import net.minesky.entities.categories.Category;
import net.minesky.entities.categories.CategoryHandler;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemHandler {

    private static List<Item> getAllItems() {
        return CategoryHandler.categories.stream()
                .flatMap(category -> category.getAllItems().stream())
                .collect(Collectors.toList());
    }

    public static Item getItemByName(String name) {
        return getAllItems().stream()
                .filter(element -> element.getMetadata().displayName().toLowerCase().contains(ChatColor.stripColor(name).toLowerCase()))
                .findFirst().orElse(null);
    }
    public static Item getItemById(String id) {
        return getAllItems().stream()
                .filter(element -> element.getId().equals(id))
                .findFirst().orElse(null);
    }

    public static List<String> getItemsNames() {
        List<String> names = new ArrayList<>();
        getAllItems().forEach(item -> {

            names.add(ChatColor.stripColor(item.getMetadata().displayName()));

        });

        Collections.sort(names);

        return names;
    }

    public static void deleteItemEntry(Category category, String idEntry) {
        category.getConfig().set(idEntry, null);
        category.reloadFile();
    }

    public static Item getItemFromStack(ItemStack itemStack) {
        if(!itemStack.hasItemMeta())
            return null;

        try {

            String s = itemStack.getItemMeta().getPersistentDataContainer().get(
                    MineSkyItems.NAMESPACED_KEY, PersistentDataType.STRING);
            if (s == null) return null;

            return getItemById(s);

        } catch(Exception ignored) {}

        return null;
    }

}
