package net.mineskyitems.gui.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CraftingCreatorGUI implements InventoryHolder, Listener {

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

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingCreatorGUI gui)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 54) {
            boolean isGrid = false;
            for (int gridSlot : GRID_SLOTS) {
                if (gridSlot == slot) {
                    isGrid = true;
                    break;
                }
            }

            if (slot == RESULT_SLOT) return;

            if (slot == SAVE_SLOT) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();

                String[] descriptors = new String[9];
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = gui.getInventory().getItem(GRID_SLOTS[i]);
                    if (stack == null || stack.getType().isAir()) {
                        descriptors[i] = "AIR";
                    } else {
                        Item customItem = ItemHandler.getItemFromStack(stack);
                        if (customItem != null) {
                            descriptors[i] = "CUSTOM:" + customItem.getId();
                        } else {
                            descriptors[i] = "VANILLA:" + stack.getType().name();
                        }
                    }
                }

                ItemStack resultStack = gui.getInventory().getItem(RESULT_SLOT);
                if (resultStack == null || resultStack.getType().isAir()) {
                    player.sendMessage(Component.text("❌ Coloque um item no slot de resultado!", NamedTextColor.RED));
                    return;
                }

                CraftingManager.saveRecipe(gui.getRecipeId(), descriptors, resultStack);
                player.sendMessage(Component.text("✔ Receita '" + gui.getRecipeId() + "' criada e ativada nativamente!", NamedTextColor.GREEN));
                player.closeInventory();
                return;
            }

            if (!isGrid) {
                event.setCancelled(true);
            }
        }
    }
}