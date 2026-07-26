package net.mineskyitems.gui.tinkering;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.gui.tinkering.recipe.RecipeManager;
import net.mineskyitems.gui.tinkering.recipe.TinkeringRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class TinkeringGUI implements Listener {

    public static final Set<Inventory> inventories = Collections.synchronizedSet(new HashSet<>());

    private static final Set<Integer> INPUT_SLOTS = Set.of(
            0, 1, 2, 3, 4,
            9, 10, 11, 12, 13,
            18, 19, 20, 21, 22,
            27, 28, 29, 30, 31,
            36, 37, 38, 39, 40
    );

    // Slot destinado para o resultado do craft (Ex: Linha 3, Coluna 7)
    private static final int RESULT_SLOT = 25;

    public static void openGUI(Player player, Block origin) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                Component.text("VZ").font(Key.key("guis")));

        // Preenche espaços vazios com vidro cinza (opcional para ficar visualmente limpo)
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < 54; i++) {
            if (!INPUT_SLOTS.contains(i) && i != RESULT_SLOT) {
                inventory.setItem(i, filler);
            }
        }

        inventories.add(inventory);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        inventories.remove(e.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!inventories.contains(e.getInventory()))
            return;

        int slot = e.getSlot();

        // Evita ações de movimentação em slots de decoração/bloqueados
        if (e.getClickedInventory() == e.getView().getTopInventory()) {
            if (!INPUT_SLOTS.contains(slot) && slot != RESULT_SLOT) {
                e.setCancelled(true);
                return;
            }
        }

        // Lógica de Shift-Click do inventário do player para a GUI
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (e.getClickedInventory() == e.getView().getBottomInventory()) {
                e.setCancelled(true);
                ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    if (mergeIntoInputs(e.getInventory(), clickedItem)) {
                        e.setCurrentItem(null);
                    }
                    scheduleUpdate(e.getInventory());
                }
                return;
            }
        }

        // Eventos no slot de resultado (Crafting)
        if (slot == RESULT_SLOT && e.getClickedInventory() == e.getView().getTopInventory()) {
            e.setCancelled(true); // Tratamos o evento manualmente por segurança
            ItemStack result = e.getCurrentItem();
            if (result == null || result.getType().isAir()) {
                return;
            }

            Player player = (Player) e.getWhoClicked();
            if (e.isShiftClick()) {
                craftShift(player, e.getInventory());
            } else {
                craftNormal(player, e.getInventory());
            }
            return;
        }

        // Se clicou no grid de input, atualiza o resultado após a alteração terminar de processar
        scheduleUpdate(e.getInventory());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (inventories.contains(e.getInventory())) {
            for (int slot : e.getRawSlots()) {
                if (slot < e.getInventory().getSize() && !INPUT_SLOTS.contains(slot)) {
                    e.setCancelled(true);
                    return;
                }
            }
            scheduleUpdate(e.getInventory());
        }
    }

    private void scheduleUpdate(Inventory inventory) {
        // Agendamos para o próximo tick para que os itens já tenham sido posicionados no inventário
        Bukkit.getScheduler().runTask(MineSkyItems.getInstance(), () -> updateCrafting(inventory));
    }

    private void updateCrafting(Inventory inventory) {
        ItemStack[][] grid = getGridMatrix(inventory);
        TinkeringRecipe matchedRecipe = RecipeManager.getMatchingRecipe(grid);

        if (matchedRecipe != null) {
            final RecipeManager.ItemEntry entry = matchedRecipe.getResult();

            ItemStack itemStack = null;

            if(entry.isVanilla()) {
                try {
                    Material mat = Material.valueOf(entry.getId());
                    itemStack = new ItemStack(mat);
                } catch (Exception ex) {}
            } else {
                Item item = ItemHandler.getItem(entry.getId());
                if(item != null) {
                    itemStack = item.buildStack();
                }
            }

            inventory.setItem(RESULT_SLOT, itemStack);
        } else {
            inventory.setItem(RESULT_SLOT, null);
        }
    }

    private ItemStack[][] getGridMatrix(Inventory inventory) {
        ItemStack[][] grid = new ItemStack[5][5];
        int[] rows = {0, 9, 18, 27, 36};
        for (int r = 0; r < 5; r++) {
            int startSlot = rows[r];
            for (int c = 0; c < 5; c++) {
                grid[r][c] = inventory.getItem(startSlot + c);
            }
        }
        return grid;
    }

    private void craftNormal(Player player, Inventory inventory) {
        ItemStack result = inventory.getItem(RESULT_SLOT);
        if (result == null || result.getType().isAir()) return;

        ItemStack cursor = player.getItemOnCursor();
        if (cursor.getType().isAir()) {
            player.setItemOnCursor(result.clone());
            deductIngredients(inventory);
            updateCrafting(inventory);
        } else if (cursor.isSimilar(result)) {
            int newAmount = cursor.getAmount() + result.getAmount();
            if (newAmount <= cursor.getMaxStackSize()) {
                cursor.setAmount(newAmount);
                player.setItemOnCursor(cursor);
                deductIngredients(inventory);
                updateCrafting(inventory);
            }
        }
    }

    private void craftShift(Player player, Inventory inventory) {
        ItemStack result = inventory.getItem(RESULT_SLOT);
        if (result == null || result.getType().isAir()) return;

        // Loop para craftar o máximo de itens possíveis enquanto houver espaço e ingredientes
        while (result != null && !result.getType().isAir()) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result.clone());
            if (!leftover.isEmpty()) {
                // Inventário do player está cheio
                break;
            }
            deductIngredients(inventory);
            updateCrafting(inventory);
            result = inventory.getItem(RESULT_SLOT);
        }
    }

    private void deductIngredients(Inventory inventory) {
        int[] rows = {0, 9, 18, 27, 36};
        for (int r = 0; r < 5; r++) {
            int startSlot = rows[r];
            for (int c = 0; c < 5; c++) {
                int slot = startSlot + c;
                ItemStack item = inventory.getItem(slot);
                if (item != null && !item.getType().isAir()) {
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        inventory.setItem(slot, null);
                    }
                }
            }
        }
    }

    private boolean mergeIntoInputs(Inventory inventory, ItemStack item) {
        for (int slot : INPUT_SLOTS) {
            ItemStack current = inventory.getItem(slot);
            if (current != null && current.isSimilar(item)) {
                int maxStack = current.getMaxStackSize();
                int currentAmount = current.getAmount();
                if (currentAmount < maxStack) {
                    int add = Math.min(item.getAmount(), maxStack - currentAmount);
                    current.setAmount(currentAmount + add);
                    item.setAmount(item.getAmount() - add);
                    if (item.getAmount() <= 0) {
                        return true;
                    }
                }
            }
        }

        for (int slot : INPUT_SLOTS) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType().isAir()) {
                inventory.setItem(slot, item.clone());
                item.setAmount(0);
                return true;
            }
        }
        return false;
    }
}