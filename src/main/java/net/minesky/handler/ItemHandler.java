package net.minesky.handler;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemHandler {

    public static ArrayList<Item> items = new ArrayList<>();

    public static Item getItemByName(String name) {
        return items.stream()
                .filter(element -> element.getMetadata().displayName().toLowerCase().contains(name.toLowerCase()))
                .findFirst().orElse(null);
    }
    public static Item getItemById(String id) {
        return items.stream()
                .filter(element -> element.getId().equals(id))
                .findFirst().orElse(null);
    }

    public static List<String> getItemsNames() {
        List<String> names = new ArrayList<>();
        items.forEach(item -> {

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
