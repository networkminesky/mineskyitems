package net.minesky.scripts;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemFrameGenerator {

    public static void generate(Location location, Material material, final int initialStartingModel) {

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

    private static void createItem(Location location, Material material, int modelData) {
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
