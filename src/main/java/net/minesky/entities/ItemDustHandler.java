package net.minesky.entities;

import net.minesky.entities.categories.Category;
import net.minesky.entities.categories.CategoryHandler;
import net.minesky.entities.item.Item;
import net.minesky.entities.item.ItemHandler;

import java.util.Random;

public class ItemDustHandler {

    public static Item dustItem;
    public static Item epicDustItem;

    public static void registerDusts() {
        // Hardcoded Category
        Category category = CategoryHandler.getCategoryById("dusts");

        if(category == null) {
            //category = new Category()
        }

        dustItem = ItemHandler.getItemById("dust");
        epicDustItem = ItemHandler.getItemById("epicdust");
    }

    public static int howManyEpicDustsForItem(Item item) {
        if(item == null) return 0;

        return howManyDustsForItem(item) / 5;
    }

    public static int howManyDustsForItem(Item item) {
        if(item == null) return 0;

        final int level = item.getRequiredLevel();

        return (int) Math.round(1 + Math.pow(level - 1, 1.2) * 0.2384);
    }

}
