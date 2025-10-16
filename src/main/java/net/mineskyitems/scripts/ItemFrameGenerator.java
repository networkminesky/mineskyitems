package net.mineskyitems.scripts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mineskyitems.entities.categories.Category;
import net.mineskyitems.entities.item.Item;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ItemFrameGenerator {

    public static void generateCategory(final Location location, Category category) {
        int actualColumn = 0;
        int actualRow = 0;

        List<Item> items = new ArrayList<>(category.getAllItems());
        items.sort(Comparator.comparingInt(Item::getRequiredLevel));

        for(Item item : items) {
            if(actualRow >= 4) {
                actualColumn++;
                actualRow = 0;
            }

            final ItemStack stack = item.buildStack();
            Component name = stack.effectiveName()
                    .append(Component.text(" - Lvl: "+item.getRequiredLevel()+"," +
                            " Modelo: "+item.getMetadata().modelData()).color(NamedTextColor.YELLOW));

            ItemMeta im = stack.getItemMeta();
            im.displayName(name);

            stack.setItemMeta(im);

            Location loc = location.clone().add(actualRow, 0, -actualColumn);

            location.getWorld().spawn(loc, ItemFrame.class, a -> {
                a.setFixed(true);
                a.setRotation(Rotation.NONE);
                a.setFacingDirection(BlockFace.UP);
                a.setItem(stack);
            });

            actualRow++;
        }
    }

    public static void generateEmpty(Location location, Material material, final int initialStartingModel) {

        // Valores fixos
        final int columnAmount = 4;
        final int rowAmount = 33;

        final int forward_rowValue = -1; // No Axis Z
        final int forward_columnValue = +1;

        // Valores dinamicos
        final int amountOfItems = ( rowAmount * columnAmount ); // 80

        int currentItem = 0;

        // Loop
        for(int i = 0; i <= rowAmount; i++) {
            for(int actual = 0; actual <= ( columnAmount - 1 ); actual++) {
                createItem(location.clone().add(actual, 0, -i), material, (initialStartingModel + currentItem));

                currentItem++;
            }
        }

    }

    public static void createItem(Location location, Material material, int modelData) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta im = itemStack.getItemMeta();

        im.setDisplayName("Modelo: "+modelData);
        im.setCustomModelData(modelData);

        itemStack.setItemMeta(im);

        location.getWorld().spawn(location, ItemFrame.class, a -> {
            a.setFixed(true);
            a.setRotation(Rotation.NONE);
            a.setFacingDirection(BlockFace.UP);
            a.setItem(itemStack);
        });
    }

}
