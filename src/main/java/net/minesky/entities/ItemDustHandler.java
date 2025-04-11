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
        final int level = item.getRequiredLevel();

        double calculatedValue = Math.floor(((double) level / 25) * 1);
        int result = (int)Math.round(calculatedValue);

        //result = result + new Random().nextInt(-2, 2);

        return result;
    }

    public static int howManyDustsForItem(Item item) {
        final int level = item.getRequiredLevel();

        double calculatedValue = Math.floor(((double) level / 5) * 1);
        int result = (int)Math.round(calculatedValue);

        //result = result + new Random().nextInt(-2, 2);

        return result;
    }

}
