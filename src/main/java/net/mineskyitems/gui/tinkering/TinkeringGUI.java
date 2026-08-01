package net.mineskyitems.gui.tinkering;

import net.kyori.adventure.text.Component;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.gui.tinkering.recipe.TinkeringManager;
import net.mineskyitems.gui.tinkering.recipe.TinkeringRecipe;
import org.bukkit.*;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TinkeringGUI implements Listener {

    public static final Map<UUID, Block> tinkeringBlocks = new HashMap<>();
    public static final Set<Inventory> inventories = Collections.synchronizedSet(new HashSet<>());

    private static final Set<Integer> INPUT_SLOTS = Set.of(
            0, 1, 2, 3, 4,
            9, 10, 11, 12, 13,
            18, 19, 20, 21, 22,
            27, 28, 29, 30, 31,
            36, 37, 38, 39, 40
    );

    private static final int RESULT_SLOT = 25;
    private static final Set<Integer> SEARCH_BUTTON_SLOTS = Set.of(51, 52, 53); // slots do botão de procurar

    public static void openGUI(Player player, Block origin) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                "[{\"text\":\"VZX\",\"font\":\"guis\",\"color\":\"white\"},{\"text\":\"Inventando\",\"font\":\"default\",\"color\":\"black\"}]");

        ItemStack filler = getFillerItem();

        for (int i = 0; i < 54; i++) {
            if (!INPUT_SLOTS.contains(i) && i != RESULT_SLOT && !SEARCH_BUTTON_SLOTS.contains(i)) {
                inventory.setItem(i, filler);
            }
        }

        // botão de busca
        ItemStack searchBtn = new ItemStack(Material.PAPER);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setCustomModelData(2);
            searchMeta.displayName(Component.text("§a🔍 Buscar e Listar Receitas"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Clique para procurar itens, ordenar por"));
            lore.add(Component.text("§7level, categoria ou materiais do inventário."));
            searchMeta.lore(lore);
            searchBtn.setItemMeta(searchMeta);
        }
        for(int i : SEARCH_BUTTON_SLOTS) {
            inventory.setItem(i, searchBtn);
        }

        inventories.add(inventory);
        player.openInventory(inventory);

        if (origin != null) {
            tinkeringBlocks.put(player.getUniqueId(), origin);
        }
    }

    private static ItemStack getFillerItem() {
        ItemStack filler = new ItemStack(Material.PAPER);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(true);
            meta.setCustomModelData(2);
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }
        return filler;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inventory = e.getInventory();
        if (!inventories.contains(inventory)) {
            return;
        }

        Player player = (Player) e.getPlayer();

        for (int slot : INPUT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                inventory.setItem(slot, null);
            }
        }

        inventory.setItem(RESULT_SLOT, null);
        inventories.remove(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!inventories.contains(e.getInventory()))
            return;

        final int slot = e.getSlot();

        if (e.getClickedInventory() == e.getView().getTopInventory()) {
            if (SEARCH_BUTTON_SLOTS.contains(slot)) {
                e.setCancelled(true);
                Player player = (Player) e.getWhoClicked();
                TinkeringSearchGUI.openGUI(player, null);
                return;
            }

            if (!INPUT_SLOTS.contains(slot) && slot != RESULT_SLOT) {
                e.setCancelled(true);
                return;
            }
        }

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

        if (slot == RESULT_SLOT && e.getClickedInventory() == e.getView().getTopInventory()) {
            e.setCancelled(true);
            ItemStack result = e.getCurrentItem();
            if (result == null || result.getType().isAir()) {
                return;
            }

            if (tinkeringBlocks.containsKey(e.getWhoClicked().getUniqueId())) {
                final Block block = tinkeringBlocks.get(e.getWhoClicked().getUniqueId());
                final Location location = block.getLocation().clone().add(0.5, 0.75, 0.5);
                AtomicInteger n = new AtomicInteger(0);
                Bukkit.getRegionScheduler().runAtFixedRate(MineSkyItems.getInstance(), location, (task) -> {
                    if (n.getAndIncrement() >= 10) {
                        task.cancel();
                        return;
                    }
                    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 2, 2);
                    block.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, location, 0, 0, 1, 0, 0.05);
                }, 1, 5);
            }

            Player player = (Player) e.getWhoClicked();
            player.playSound(player, Sound.ENTITY_VILLAGER_WORK_TOOLSMITH, 1, 0.8f);
            if (e.isShiftClick()) {
                craftShift(player, e.getInventory());
            } else {
                craftNormal(player, e.getInventory());
            }
            return;
        }

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

    public static void scheduleUpdate(Inventory inventory) {
        if (inventory.getViewers().isEmpty()) return;

        Player player = (Player) inventory.getViewers().getFirst();
        player.getScheduler().run(MineSkyItems.getInstance(), (task) -> updateCrafting(inventory), null);
    }

    private static void updateCrafting(Inventory inventory) {
        ItemStack[][] grid = getGridMatrix(inventory);
        TinkeringRecipe matchedRecipe = TinkeringManager.getMatchingRecipe(grid);

        if (matchedRecipe != null) {
            inventory.setItem(RESULT_SLOT, TinkeringManager.buildItemStackFromEntry(matchedRecipe.getResult()));
        } else {
            inventory.setItem(RESULT_SLOT, null);
        }
    }

    private static ItemStack[][] getGridMatrix(Inventory inventory) {
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

        while (result != null && !result.getType().isAir()) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result.clone());
            if (!leftover.isEmpty()) {
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