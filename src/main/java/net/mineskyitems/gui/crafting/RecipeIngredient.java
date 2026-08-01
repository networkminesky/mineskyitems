package net.mineskyitems.gui.crafting;

import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public record RecipeIngredient(String customId, Material material, int amount) {

    public RecipeIngredient(String customId, Material material, int amount) {
        this.customId = customId;
        this.material = material;
        this.amount = Math.max(1, amount);
    }

    public boolean isCustom() {
        return customId != null && !customId.isEmpty();
    }

    public static RecipeIngredient fromItemStack(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }

        Item customItem = ItemHandler.getItemFromStack(stack);
        if (customItem != null) {
            return new RecipeIngredient(customItem.getId(), stack.getType(), stack.getAmount());
        }
        return new RecipeIngredient(null, stack.getType(), stack.getAmount());
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }

        if (stack.getAmount() < this.amount) {
            return false;
        }

        if (isCustom()) {
            Item customItem = ItemHandler.getItemFromStack(stack);
            return customItem != null && this.customId.equals(customItem.getId());
        } else {
            // se for vanilla, checa se é item customizado reutilizando o mesmo Material
            Item customItem = ItemHandler.getItemFromStack(stack);
            return customItem == null && stack.getType() == this.material;
        }
    }

    public String serialize() {
        if (isCustom()) {
            return "CUSTOM:" + customId + ":" + amount;
        }
        return "VANILLA:" + material.name() + ":" + amount;
    }

    public static RecipeIngredient deserialize(String input) {
        if (input == null || input.isEmpty() || input.equalsIgnoreCase("AIR")) {
            return null;
        }
        String[] parts = input.split(":");
        if (parts[0].equalsIgnoreCase("CUSTOM")) {
            return new RecipeIngredient(parts[1], null, Integer.parseInt(parts[2]));
        } else if (parts[0].equalsIgnoreCase("VANILLA")) {
            return new RecipeIngredient(null, Material.valueOf(parts[1]), Integer.parseInt(parts[2]));
        }
        return null;
    }
}