package net.mineskyitems.gui.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CraftingCreatorGUI implements InventoryHolder {

    public static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int RESULT_SLOT = 24;
    public static final int SAVE_SLOT = 49;

    private final String recipeId;
    private final Inventory inventory;

    public CraftingCreatorGUI(String recipeId) {
        this.recipeId = recipeId;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Criar Receita: " + recipeId, NamedTextColor.DARK_GRAY));
        setupGUI();
    }

    private void setupGUI() {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        for (int slot : GRID_SLOTS) {
            inventory.setItem(slot, null);
        }
        inventory.setItem(RESULT_SLOT, null);

        inventory.setItem(23, createItem(Material.ARROW, Component.text("➜ Resultado", NamedTextColor.YELLOW, TextDecoration.BOLD)));

        inventory.setItem(SAVE_SLOT, createItem(
                Material.LIME_STAINED_GLASS_PANE,
                Component.text("✔ Salvar Receita", NamedTextColor.GREEN, TextDecoration.BOLD),
                List.of(Component.text("Clique para registrar a receita!", NamedTextColor.GRAY))
        ));
    }

    private ItemStack createItem(Material mat, Component title) {
        return createItem(mat, title, null);
    }

    private ItemStack createItem(Material mat, Component title, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(title.decoration(TextDecoration.ITALIC, false));
            if (lore != null) meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getRecipeId() {
        return recipeId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}