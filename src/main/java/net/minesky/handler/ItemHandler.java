package net.minesky.handler;

import net.minesky.handler.categories.CategoryHandler;
import org.bukkit.NamespacedKey;
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
                .filter(element -> element.getMetadata().displayName().toLowerCase().contains(name.toLowerCase()))
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

            names.add(item.getMetadata().displayName());

        });

        Collections.sort(names);

        return names;
    }

    public static Item getItemFromStack(ItemStack itemStack) {
        if(!itemStack.hasItemMeta())
            return null;

        try {

            String s = itemStack.getItemMeta().getPersistentDataContainer().get(
                    NamespacedKey.fromString("mineskyitems"), PersistentDataType.STRING);
            if (s == null) return null;

            return getItemById(s);

        } catch(Exception ignored) {}

        return null;
    }

}
