package net.mineskyitems.gui.crafting;

import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Keyed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class CraftingListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        if (recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equalsIgnoreCase("mineskyitems")) {
            return;
        }

        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && ItemHandler.getItemFromStack(item) != null) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}