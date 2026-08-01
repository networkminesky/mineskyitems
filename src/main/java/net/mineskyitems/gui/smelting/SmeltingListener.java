package net.mineskyitems.gui.smelting;

import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Keyed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class SmeltingListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        // Se for receita de queima customizada registrada pelo nosso plugin, permite
        if (recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equalsIgnoreCase("mineskyitems")) {
            return;
        }

        // Se for receita vanilla, não permite queimar itens customizados nela
        ItemStack source = event.getSource();
        if (source != null && ItemHandler.getItemFromStack(source) != null) {
            event.setCancelled(true);
        }
    }
}