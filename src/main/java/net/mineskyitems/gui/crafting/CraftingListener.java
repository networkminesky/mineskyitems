package net.mineskyitems.gui.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class CraftingListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        boolean hasItems = false;
        boolean hasCustom = false;

        for (ItemStack stack : matrix) {
            if (stack != null && !stack.getType().isAir()) {
                hasItems = true;
                if (ItemHandler.getItemFromStack(stack) != null) {
                    hasCustom = true;
                }
            }
        }

        if (!hasItems) return;

        CustomRecipe matched = RecipeManager.matchRecipe(matrix);
        if (matched != null) {
            inv.setResult(matched.createResult());
        } else if (hasCustom) {
            inv.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        CustomRecipe recipe = RecipeManager.matchRecipe(matrix);
        if (recipe == null) {
            for (ItemStack is : matrix) {
                if (is != null && ItemHandler.getItemFromStack(is) != null) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingCreatorGUI gui)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 54) {
            boolean isGrid = false;
            for (int gridSlot : CraftingCreatorGUI.GRID_SLOTS) {
                if (gridSlot == slot) {
                    isGrid = true;
                    break;
                }
            }

            if (slot == CraftingCreatorGUI.RESULT_SLOT) {
                return; // Permite colocar o item de resultado
            }

            if (slot == CraftingCreatorGUI.SAVE_SLOT) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();

                RecipeIngredient[] ingredients = new RecipeIngredient[9];
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = gui.getInventory().getItem(CraftingCreatorGUI.GRID_SLOTS[i]);
                    ingredients[i] = RecipeIngredient.fromItemStack(stack);
                }

                ItemStack resultStack = gui.getInventory().getItem(CraftingCreatorGUI.RESULT_SLOT);
                if (resultStack == null || resultStack.getType().isAir()) {
                    player.sendMessage(Component.text("❌ Coloque um item no slot de resultado!", NamedTextColor.RED));
                    return;
                }

                RecipeIngredient resultIngredient = RecipeIngredient.fromItemStack(resultStack);
                CustomRecipe recipe = new CustomRecipe(
                        gui.getRecipeId(),
                        ingredients,
                        resultIngredient,
                        resultIngredient.isCustom() ? null : resultStack
                );

                RecipeManager.saveRecipe(recipe);
                player.sendMessage(Component.text("✔ Receita '" + gui.getRecipeId() + "' criada e salva!", NamedTextColor.GREEN));
                player.closeInventory();
                return;
            }

            if (!isGrid) {
                event.setCancelled(true);
            }
        }
    }
}