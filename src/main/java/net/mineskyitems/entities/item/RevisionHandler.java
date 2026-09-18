package net.mineskyitems.entities.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class RevisionHandler {

    private static final NamespacedKey REVISION_KEY = new NamespacedKey("mineskyitems", "revision");

    public static int getRevision(final ItemStack stack) {
        return stack.getPersistentDataContainer().getOrDefault(REVISION_KEY, PersistentDataType.INTEGER, 0);
    }

    public static boolean shouldRevision(final ItemStack stack) {
        Item item = ItemHandler.getItemFromStack(stack);
        if(item != null) {
            final int revision = getRevision(stack);
            return item.getRevision() < revision;
        }
        return false;
    }

    public static void startRevision(final ItemStack stack, final Item item) {

    }

}
