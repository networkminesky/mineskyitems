package net.mineskyitems.entities.item;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.categories.Category;
import net.mineskyitems.entities.categories.CategoryHandler;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemHandler {

    public static NamespacedKey LEVEL_NAMESPACE = NamespacedKey.fromString("item_level");
    public static NamespacedKey CLASS_NAMESPACE = NamespacedKey.fromString("classes");

    public static int getStaticRequiredLevel(final ItemStack itemStack) {
        if(itemStack == null) return 0;
        if(!itemStack.hasItemMeta()) return 0;

        final ItemMeta itemMeta = itemStack.getItemMeta();
        if(!itemMeta.getPersistentDataContainer().has(MineSkyItems.NAMESPACED_KEY))
            return 0;

        return itemMeta.getPersistentDataContainer().getOrDefault(LEVEL_NAMESPACE, PersistentDataType.INTEGER, 0);
    }

    public static List<String> getStaticClasses(final ItemStack itemStack) {
        if(itemStack == null) return List.of();
        if(!itemStack.hasItemMeta()) return List.of();

        final ItemMeta itemMeta = itemStack.getItemMeta();
        if(!itemMeta.getPersistentDataContainer().has(MineSkyItems.NAMESPACED_KEY))
            return List.of();

        return itemMeta.getPersistentDataContainer().getOrDefault(CLASS_NAMESPACE,
                PersistentDataType.LIST.strings(), getItemFromStack(itemStack).getRequiredClasses());
    }

    public static List<Item> getAllItems() {
        return CategoryHandler.categories.stream()
                .flatMap(category -> category.getAllItems().stream())
                .collect(Collectors.toList());
    }

    public static Item getItemByName(String name) {
        return getAllItems().stream()
                .filter(element -> ChatColor.stripColor(element.getMetadata().displayName()).equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }
    public static Item getItemById(String id) {
        return getAllItems().stream()
                .filter(element -> element.getId().equalsIgnoreCase(id))
                .findFirst().orElse(null);
    }
    public static Item getItem(String nameOrId) {
        return getAllItems().stream()
                .filter(item -> nameOrId.equalsIgnoreCase(item.getId()) ||
                        nameOrId.equalsIgnoreCase(ChatColor.stripColor(item.getMetadata().displayName())))
                .findFirst().orElse(null);
    }

    public static List<String> getItemsNamesAndIds() {
        List<String> names = new ArrayList<>();
        getAllItems().forEach(item -> {
            names.add(ChatColor.stripColor(item.getMetadata().displayName()));
            names.add(item.getId());
        });

        Collections.sort(names);

        return names;
    }
    public static List<String> getItemsNames() {
        List<String> names = new ArrayList<>();
        getAllItems().forEach(item -> names.add(ChatColor.stripColor(item.getMetadata().displayName())));

        Collections.sort(names);

        return names;
    }

    public static void deleteItemEntry(Category category, String idEntry) {
        category.getConfig().set(idEntry, null);
        category.reloadFile();
    }

    public static void deleteItem(Item item) {
        final Category category = item.getCategory();

        category.getConfig().set(item.getId(), null);
        category.reloadFile();
        category.reloadCategory();
    }

    public static Item getItemFromStack(ItemStack itemStack) {
        if(itemStack == null)
            return null;
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
