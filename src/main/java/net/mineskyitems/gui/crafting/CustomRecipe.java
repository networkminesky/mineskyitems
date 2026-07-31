package net.mineskyitems.gui.crafting;

import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CustomRecipe {
    private final String id;
    private final RecipeIngredient[] ingredients; // Matriz 3x3 (tamanho 9)
    private final RecipeIngredient resultIngredient;
    private final ItemStack vanillaResult;

    public CustomRecipe(String id, RecipeIngredient[] ingredients, RecipeIngredient resultIngredient, ItemStack vanillaResult) {
        this.id = id;
        this.ingredients = ingredients;
        this.resultIngredient = resultIngredient;
        this.vanillaResult = vanillaResult;
    }

    public String getId() {
        return id;
    }

    public RecipeIngredient[] getIngredients() {
        return ingredients;
    }

    public ItemStack createResult() {
        if (resultIngredient != null && resultIngredient.isCustom()) {
            Item customItem = ItemHandler.getItemById(resultIngredient.customId());
            if (customItem != null) {
                ItemStack stack = customItem.buildStack();
                if (resultIngredient.amount() > 1) {
                    stack.setAmount(resultIngredient.amount());
                }
                return stack;
            }
        }
        return vanillaResult != null ? vanillaResult.clone() : null;
    }

    public boolean matches(ItemStack[] matrix) {
        if (matrix == null || matrix.length < 9) return false;

        for (int i = 0; i < 9; i++) {
            RecipeIngredient req = ingredients[i];
            ItemStack current = matrix[i];

            boolean isEmptyCurrent = (current == null || current.getType() == Material.AIR);
            boolean isEmptyReq = (req == null);

            if (isEmptyReq && isEmptyCurrent) continue;
            if (isEmptyReq != isEmptyCurrent) return false;

            if (!req.matches(current)) {
                return false;
            }
        }
        return true;
    }
}