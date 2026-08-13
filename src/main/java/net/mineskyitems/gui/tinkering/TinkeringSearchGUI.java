package net.mineskyitems.gui.tinkering;

import net.kyori.adventure.text.Component;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.categories.CategoryHandler;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.gui.tinkering.recipe.TinkeringManager;
import net.mineskyitems.gui.tinkering.recipe.TinkeringRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TinkeringSearchGUI implements Listener {

    public static final Set<Inventory> searchInventories = Collections.synchronizedSet(new HashSet<>());
    public static final Map<UUID, SearchState> playerStates = new HashMap<>();
    public static final Map<UUID, SearchState> awaitingSearchPrompt = new HashMap<>();

    public enum SortingMode {
        DEFAULT("Padrão"),
        LEVEL_HIGHEST("Level (Maior > Menor)"),
        LEVEL_LOWEST("Level (Menor > Maior)"),
        CATEGORY("Por Categoria"),
        MATERIALS("Materiais Atuais");

        private final String displayName;

        SortingMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public SortingMode next() {
            SortingMode[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }

        public SortingMode previous() {
            SortingMode[] vals = values();
            return vals[(ordinal() - 1 + vals.length) % vals.length];
        }
    }

    public static class SearchState {
        String query;
        SortingMode sortingMode = SortingMode.MATERIALS;
        int categoryIndex = 0;
        int page = 0;

        public SearchState(String query, SortingMode sortingMode, int categoryIndex, int page) {
            this.query = query;
            this.sortingMode = sortingMode;
            this.categoryIndex = categoryIndex;
            this.page = page;
        }
    }

    public static void openGUI(Player player, SearchState state) {
        if (state == null) {
            state = new SearchState(null, SortingMode.DEFAULT, 0, 0);
        }
        playerStates.put(player.getUniqueId(), state);

        String titleText = (state.query != null && !state.query.trim().isEmpty())
                ?
                "[{\"text\":\"VbX\",\"font\":\"guis\",\"color\":\"white\"},{\"text\":\""+state.query+"\",\"font\":\"default\",\"color\":\"black\"}]"
                :
                "[{\"text\":\"VaX\",\"font\":\"guis\",\"color\":\"white\"},{\"text\":\"\",\"font\":\"default\",\"color\":\"black\"}]";
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(titleText));

        renderTopBar(inventory, state);
        renderItemList(player, inventory, state);

        searchInventories.add(inventory);
        player.openInventory(inventory);
    }

    private static void renderTopBar(Inventory inventory, SearchState state) {
        ItemStack searchBtn = new ItemStack(Material.PAPER);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        if (searchMeta != null) {
            searchMeta.displayName(Component.text("§e🔍 Botão de Pesquisar"));
            searchMeta.setCustomModelData(2);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Clique para digitar um termo no chat."));
            lore.add(Component.text("§fTermo Atual: §a" + (state.query != null ? state.query : "§7[Nenhum]")));
            searchMeta.lore(lore);
            searchBtn.setItemMeta(searchMeta);
        }

        for (int i = 0; i <= 4; i++) {
            inventory.setItem(i, searchBtn);
        }

        ItemStack sortBtn = new ItemStack(Material.HOPPER);
        ItemMeta sortMeta = sortBtn.getItemMeta();
        if (sortMeta != null) {
            sortMeta.displayName(Component.text("§b⚡ Ordenação"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Modos de ordenação disponíveis:"));

            for (SortingMode mode : SortingMode.values()) {
                if (mode == state.sortingMode) {
                    lore.add(Component.text(" §a➤ " + mode.getDisplayName()));
                } else {
                    lore.add(Component.text(" §8  " + mode.getDisplayName()));
                }
            }

            if (state.sortingMode == SortingMode.CATEGORY) {
                List<String> categories = CategoryHandler.getCategoriesNames();
                String catName = (!categories.isEmpty() && state.categoryIndex < categories.size())
                        ? categories.get(state.categoryIndex) : "Todas";
                lore.add(Component.text(""));
                lore.add(Component.text("§fCategoria Selecionada: §d" + catName));
                lore.add(Component.text("§f(Shift + Clique para alternar categoria)"));
            }

            lore.add(Component.text(""));
            lore.add(Component.text("§7[Clique Esquerdo: Próximo]"));
            lore.add(Component.text("§7[Clique Direito: Anterior]"));

            sortMeta.lore(lore);
            sortBtn.setItemMeta(sortMeta);
        }
        inventory.setItem(5, sortBtn);

        ItemStack prevBtn = new ItemStack(Material.PAPER);
        ItemMeta prevMeta = prevBtn.getItemMeta();
        if (prevMeta != null) {
            prevMeta.setCustomModelData(27);
            prevMeta.displayName(Component.text("§a◄ Página Anterior"));
            prevBtn.setItemMeta(prevMeta);
        }
        inventory.setItem(6, prevBtn);

        ItemStack backBtn = new ItemStack(Material.PAPER);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setCustomModelData(22);
            backMeta.displayName(Component.text("§c❌ Voltar para a Bancada"));
            backBtn.setItemMeta(backMeta);
        }
        inventory.setItem(7, backBtn);

        ItemStack nextBtn = new ItemStack(Material.PAPER);
        ItemMeta nextMeta = nextBtn.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setCustomModelData(24);
            nextMeta.displayName(Component.text("§aPróxima Página ►"));
            nextBtn.setItemMeta(nextMeta);
        }
        inventory.setItem(8, nextBtn);
    }

    public static List<RecipeDisplayEntry> getFilteredEntries(Player player, SearchState state) {
        List<TinkeringRecipe> allRecipes = TinkeringManager.getRecipes();
        List<RecipeDisplayEntry> entries = new ArrayList<>();

        for (TinkeringRecipe recipe : allRecipes) {
            TinkeringManager.ItemEntry result = recipe.getResult();
            if (result == null || result.isAir()) continue;

            int level = 0;
            String displayName = "";
            String categoryName = "Outros";

            if (result.isVanilla()) {
                displayName = result.getId();
                categoryName = "Vanilla";
            } else {
                Item item = ItemHandler.getItem(result.getId());
                if (item != null) {
                    level = item.getRequiredLevel();
                    displayName = (item.getMetadata() != null && item.getMetadata().displayName() != null)
                            ? item.getMetadata().displayName() : item.getId();
                    if (item.getCategory() != null) {
                        categoryName = item.getCategory().getName();
                    }
                }
            }

            if (state.query != null && !state.query.trim().isEmpty()) {
                String q = state.query.toLowerCase().trim();
                boolean matches = displayName.toLowerCase().contains(q)
                        || result.getId().toLowerCase().contains(q)
                        || categoryName.toLowerCase().contains(q);

                if (!matches) continue;
            }

            if (state.sortingMode == SortingMode.CATEGORY) {
                List<String> categories = CategoryHandler.getCategoriesNames();
                if (!categories.isEmpty() && state.categoryIndex < categories.size()) {
                    String selectedCat = categories.get(state.categoryIndex);
                    if (!categoryName.equalsIgnoreCase(selectedCat)) {
                        continue;
                    }
                }
            }

            int matScore = 0;
            if (state.sortingMode == SortingMode.MATERIALS) {
                matScore = calculateInventoryMaterialScore(player, recipe);
            }

            entries.add(new RecipeDisplayEntry(recipe, result, level, displayName, categoryName, matScore));
        }

        if (state.sortingMode == SortingMode.LEVEL_HIGHEST) {
            entries.sort((a, b) -> Integer.compare(b.level, a.level));
        } else if (state.sortingMode == SortingMode.LEVEL_LOWEST) {
            entries.sort((a, b) -> Integer.compare(a.level, b.level));
        } else if (state.sortingMode == SortingMode.MATERIALS) {
            entries.sort((a, b) -> Integer.compare(b.materialScore, a.materialScore));
        }

        return entries;
    }

    private static void renderItemList(Player player, Inventory inventory, SearchState state) {
        List<RecipeDisplayEntry> entries = getFilteredEntries(player, state);

        int pageSize = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / pageSize));
        if (state.page >= totalPages) state.page = totalPages - 1;
        if (state.page < 0) state.page = 0;

        int startIndex = state.page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, entries.size());

        int slotIndex = 9;
        for (int i = startIndex; i < endIndex; i++) {
            RecipeDisplayEntry entry = entries.get(i);
            ItemStack stack = TinkeringManager.buildItemStackFromEntry(entry.resultEntry);
            if (stack != null) {
                inventory.setItem(slotIndex, stack);
            }
            slotIndex++;
        }

        for (int i = slotIndex; i <= 53; i++) {
            inventory.setItem(i, null);
        }
    }

    private static int calculateInventoryMaterialScore(Player player, TinkeringRecipe recipe) {
        int score = 0;
        ItemStack[] invContents = player.getInventory().getContents();
        TinkeringManager.ItemEntry[][] grid = recipe.getCroppedRecipeGrid();

        for (TinkeringManager.ItemEntry[] row : grid) {
            for (TinkeringManager.ItemEntry entry : row) {
                if (entry == null || entry.isAir()) continue;

                for (ItemStack item : invContents) {
                    if (item == null || item.getType().isAir()) continue;

                    if (entry.isVanilla()) {
                        if (item.getType().name().equalsIgnoreCase(entry.getId())) {
                            score += item.getAmount();
                            break;
                        }
                    } else {
                        Item custom = ItemHandler.getItemFromStack(item);
                        if (custom != null && custom.getId().equalsIgnoreCase(entry.getId())) {
                            score += item.getAmount();
                            break;
                        }
                    }
                }
            }
        }
        return score;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!searchInventories.contains(e.getInventory())) return;

        e.setCancelled(true);

        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        Player player = (Player) e.getWhoClicked();
        SearchState state = playerStates.get(player.getUniqueId());
        if (state == null) state = new SearchState(null, SortingMode.DEFAULT, 0, 0);

        int slot = e.getSlot();

        if (slot >= 0 && slot <= 4) {
            awaitingSearchPrompt.put(player.getUniqueId(), state);
            player.closeInventory();
            player.sendMessage(" ");
            player.sendMessage("§aDigite no chat o termo da pesquisa (ou 'cancelar'):");
            player.sendMessage(" ");
            return;
        }

        if (slot == 5) {
            if (e.isShiftClick() && state.sortingMode == SortingMode.CATEGORY) {
                List<String> categories = CategoryHandler.getCategoriesNames();
                if (!categories.isEmpty()) {
                    state.categoryIndex = (state.categoryIndex + 1) % categories.size();
                }
            } else if (e.isRightClick()) {
                state.sortingMode = state.sortingMode.previous();
                state.categoryIndex = 0;
            } else {
                state.sortingMode = state.sortingMode.next();
                state.categoryIndex = 0;
            }
            openGUI(player, state);
            return;
        }

        if (slot == 6) {
            if (state.page > 0) {
                state.page--;
                openGUI(player, state);
            }
            return;
        }

        if (slot == 7) {
            player.closeInventory();
            TinkeringGUI.openGUI(player, TinkeringGUI.tinkeringBlocks.get(player.getUniqueId()));
            return;
        }

        if (slot == 8) {
            state.page++;
            openGUI(player, state);
            return;
        }

        if (slot >= 9 && slot <= 53) {
            int indexOnPage = slot - 9;
            int pageSize = 45;
            int targetIndex = (state.page * pageSize) + indexOnPage;

            List<RecipeDisplayEntry> currentEntries = getFilteredEntries(player, state);
            if (targetIndex >= 0 && targetIndex < currentEntries.size()) {
                RecipeDisplayEntry selected = currentEntries.get(targetIndex);
                player.closeInventory();
                TinkeringRecipePreviewGUI.openGUI(player, selected.recipe, state);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        searchInventories.remove(e.getInventory());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (awaitingSearchPrompt.containsKey(uuid)) {
            e.setCancelled(true);
            SearchState state = awaitingSearchPrompt.remove(uuid);

            String msg = e.getMessage().trim();
            if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel")) {
                state.query = null;
                player.sendMessage("§cPesquisa cancelada.");
            } else {
                state.query = msg;
                player.sendMessage("§aPesquisando por: §f" + msg);
            }

            player.getScheduler().run(MineSkyItems.getInstance(), (task) -> openGUI(player, state), null);
        }
    }

    public static class RecipeDisplayEntry {
        public final TinkeringRecipe recipe;
        public final TinkeringManager.ItemEntry resultEntry;
        public final int level;
        public final String displayName;
        public final String categoryName;
        public final int materialScore;

        RecipeDisplayEntry(TinkeringRecipe recipe, TinkeringManager.ItemEntry resultEntry, int level, String displayName, String categoryName, int materialScore) {
            this.recipe = recipe;
            this.resultEntry = resultEntry;
            this.level = level;
            this.displayName = displayName;
            this.categoryName = categoryName;
            this.materialScore = materialScore;
        }
    }
}