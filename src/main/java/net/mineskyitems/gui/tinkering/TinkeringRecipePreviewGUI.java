package net.mineskyitems.gui.tinkering;

import net.kyori.adventure.text.Component;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.gui.tinkering.recipe.TinkeringManager;
import net.mineskyitems.gui.tinkering.recipe.TinkeringRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TinkeringRecipePreviewGUI implements Listener {

    public static final Set<Inventory> previewInventories = Collections.synchronizedSet(new HashSet<>());
    public static final Map<UUID, PreviewSession> activeSessions = new HashMap<>();

    private static final Set<Integer> INPUT_SLOTS = Set.of(
            0, 1, 2, 3, 4,
            9, 10, 11, 12, 13,
            18, 19, 20, 21, 22,
            27, 28, 29, 30, 31,
            36, 37, 38, 39, 40
    );

    private static final int RESULT_SLOT = 25;
    private static final int BACK_SLOT = 48;
    private static final int FILL_SLOT = 49;

    public static class PreviewSession {
        final Deque<TinkeringRecipe> recipeHistory = new ArrayDeque<>();
        final TinkeringSearchGUI.SearchState previousSearchState;

        public PreviewSession(TinkeringRecipe initialRecipe, TinkeringSearchGUI.SearchState previousSearchState) {
            this.recipeHistory.push(initialRecipe);
            this.previousSearchState = previousSearchState;
        }

        public TinkeringRecipe getCurrentRecipe() {
            return recipeHistory.peek();
        }
    }

    public static void openGUI(Player player, TinkeringRecipe recipe, TinkeringSearchGUI.SearchState previousSearchState) {
        PreviewSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            session = new PreviewSession(recipe, previousSearchState);
            activeSessions.put(player.getUniqueId(), session);
        } else if (session.getCurrentRecipe() != recipe) {
            session.recipeHistory.push(recipe);
        }

        openGUIInternal(player, session);
    }

    private static void openGUIInternal(Player player, PreviewSession session) {
        TinkeringRecipe recipe = session.getCurrentRecipe();

        Inventory inventory = Bukkit.createInventory(null, 54,
                "[{\"text\":\"VZX\",\"font\":\"guis\",\"color\":\"white\"},{\"text\":\"Pré-visualização\",\"font\":\"default\",\"color\":\"black\"}]");

        ItemStack filler = new ItemStack(Material.PAPER);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(true);
            meta.setCustomModelData(2);
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < 54; i++) {
            if (!INPUT_SLOTS.contains(i) && i != RESULT_SLOT && i != BACK_SLOT && i != FILL_SLOT) {
                inventory.setItem(i, filler);
            }
        }

        // Renderiza o Grid 5x5 do Crafting
        TinkeringManager.ItemEntry[][] grid = recipe.getCroppedRecipeGrid();
        int[] rows = {0, 9, 18, 27, 36};

        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                TinkeringManager.ItemEntry entry = grid[r][c];
                if (entry != null && !entry.isAir()) {
                    int slot = rows[r] + c;
                    ItemStack stack = TinkeringManager.buildItemStackFromEntry(entry);
                    if (stack != null) {
                        // Verifica se este ingrediente tem uma receita própria (Estilo JEI)
                        TinkeringRecipe subRecipe = TinkeringManager.getRecipeByResultItem(stack);
                        if (subRecipe != null) {
                            ItemMeta m = stack.getItemMeta();
                            if (m != null) {
                                List<Component> lore = m.hasLore() ? m.lore() : new ArrayList<>();
                                if (lore == null) lore = new ArrayList<>();
                                lore.add(Component.empty());
                                lore.add(Component.text("§e🔍 Clique para ver a receita deste item"));
                                m.lore(lore);
                                stack.setItemMeta(m);
                            }
                        }
                        inventory.setItem(slot, stack);
                    }
                }
            }
        }

        // Renderiza o item resultante
        ItemStack resultStack = TinkeringManager.buildItemStackFromEntry(recipe.getResult());
        if (resultStack != null) {
            TinkeringRecipe subRecipe = TinkeringManager.getRecipeByResultItem(resultStack);
            if (subRecipe != null && subRecipe != recipe) {
                ItemMeta m = resultStack.getItemMeta();
                if (m != null) {
                    List<Component> lore = m.hasLore() ? m.lore() : new ArrayList<>();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add(Component.empty());
                    lore.add(Component.text("§e🔍 Clique para ver a receita deste item"));
                    m.lore(lore);
                    resultStack.setItemMeta(m);
                }
            }
            inventory.setItem(RESULT_SLOT, resultStack);
        }

        // Botão Voltar (Slot 48)
        ItemStack backBtn = new ItemStack(Material.PAPER);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setCustomModelData(27);
            backMeta.displayName(Component.text("§c❌ Voltar"));
            backBtn.setItemMeta(backMeta);
        }
        inventory.setItem(BACK_SLOT, backBtn);

        // Botão Preencher Crafting (Slot 49)
        ItemStack fillBtn = new ItemStack(Material.ANVIL);
        ItemMeta fillMeta = fillBtn.getItemMeta();
        if (fillMeta != null) {
            fillMeta.displayName(Component.text("§a🔨 Preencher Crafting"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Pega os materiais disponíveis no seu inventário"));
            lore.add(Component.text("§7e posiciona automaticamente na mesa de tinkering."));
            fillMeta.lore(lore);
            fillBtn.setItemMeta(fillMeta);
        }
        inventory.setItem(FILL_SLOT, fillBtn);

        previewInventories.add(inventory);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!previewInventories.contains(e.getInventory())) return;

        e.setCancelled(true);

        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        Player player = (Player) e.getWhoClicked();
        PreviewSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        int slot = e.getSlot();

        // Slot 48: Voltar no Histórico ou Voltar para Pesquisa
        if (slot == BACK_SLOT) {
            session.recipeHistory.pop(); // Remove a receita atual

            if (!session.recipeHistory.isEmpty()) {
                // Abre a receita anterior no histórico
                openGUIInternal(player, session);
            } else {
                // Se o histórico esvaziou, volta para a tela de pesquisa
                activeSessions.remove(player.getUniqueId());
                player.closeInventory();
                TinkeringSearchGUI.openGUI(player, session.previousSearchState);
            }
            return;
        }

        // Slot 49: Preencher Crafting (Corrigido para não duplicar/resetar GUI)
        if (slot == FILL_SLOT) {
            TinkeringRecipe currentRecipe = session.getCurrentRecipe();
            activeSessions.remove(player.getUniqueId());
            player.closeInventory();

            Block origin = TinkeringGUI.tinkeringBlocks.get(player.getUniqueId());
            player.getScheduler().run(MineSkyItems.getInstance(), task -> {
                TinkeringGUI.openGUI(player, origin);
                Inventory topInv = player.getOpenInventory().getTopInventory();
                autoFillRecipe(player, topInv, currentRecipe);
            }, null);
            return;
        }

        // Clique em Itens no Grid 5x5 ou no Resultado (Navegação Recursiva JEI)
        if (INPUT_SLOTS.contains(slot) || slot == RESULT_SLOT) {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                TinkeringRecipe subRecipe = TinkeringManager.getRecipeByResultItem(clickedItem);
                if (subRecipe != null && subRecipe != session.getCurrentRecipe()) {
                    session.recipeHistory.push(subRecipe);
                    openGUIInternal(player, session);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        previewInventories.remove(e.getInventory());
    }

    private static void autoFillRecipe(Player player, Inventory tinkeringInv, TinkeringRecipe recipe) {
        TinkeringManager.ItemEntry[][] recipeGrid = recipe.getCroppedRecipeGrid();
        int[] rows = {0, 9, 18, 27, 36};

        int filledCount = 0;
        int requiredCount = 0;

        for (int r = 0; r < recipeGrid.length; r++) {
            for (int c = 0; c < recipeGrid[0].length; c++) {
                TinkeringManager.ItemEntry entry = recipeGrid[r][c];
                if (entry == null || entry.isAir()) continue;

                requiredCount++;
                int guiSlot = rows[r] + c;

                ItemStack[] playerContents = player.getInventory().getContents();
                for (int i = 0; i < playerContents.length; i++) {
                    ItemStack invItem = playerContents[i];
                    if (isMatchingEntry(invItem, entry)) {
                        ItemStack placedItem = invItem.clone();
                        placedItem.setAmount(1);

                        if (invItem.getAmount() > 1) {
                            invItem.setAmount(invItem.getAmount() - 1);
                        } else {
                            player.getInventory().setItem(i, null);
                        }

                        tinkeringInv.setItem(guiSlot, placedItem);
                        filledCount++;
                        break;
                    }
                }
            }
        }

        // Atualiza os cálculos de resultado na mesa de Tinkering aberta
        TinkeringGUI.scheduleUpdate(tinkeringInv);

        if (filledCount == requiredCount) {
            player.sendMessage("§a✔ Crafting totalmente preenchido!");
        } else if (filledCount > 0) {
            player.sendMessage("§e⚠ Crafting parcialmente preenchido (" + filledCount + "/" + requiredCount + " itens encontrados).");
        } else {
            player.sendMessage("§c✘ Nenhum dos materiais necessários foi encontrado no seu inventário.");
        }
    }

    private static boolean isMatchingEntry(ItemStack item, TinkeringManager.ItemEntry entry) {
        if (item == null || item.getType().isAir()) return false;
        if (entry == null || entry.isAir()) return false;

        if (entry.isVanilla()) {
            return item.getType().name().equalsIgnoreCase(entry.getId());
        } else {
            Item custom = ItemHandler.getItemFromStack(item);
            return custom != null && custom.getId().equalsIgnoreCase(entry.getId());
        }
    }
}