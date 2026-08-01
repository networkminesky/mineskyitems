package net.mineskyitems.gui.tinkering;

import net.kyori.adventure.text.Component;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.gui.tinkering.recipe.TinkeringManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TinkeringCreatorGUI implements Listener {

    public static final Set<Inventory> creatorInventories = Collections.synchronizedSet(new HashSet<>());
    public static final Map<UUID, String> activeCreators = new HashMap<>();

    private static final Set<Integer> INPUT_SLOTS = Set.of(
            0, 1, 2, 3, 4,
            9, 10, 11, 12, 13,
            18, 19, 20, 21, 22,
            27, 28, 29, 30, 31,
            36, 37, 38, 39, 40
    );

    private static final int RESULT_SLOT = 25;
    private static final int SAVE_SLOT = 49;

    public static void openCreatorGUI(Player player, String recipeId) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("§8Criando Crafting: " + recipeId));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < 54; i++) {
            if (!INPUT_SLOTS.contains(i) && i != RESULT_SLOT && i != SAVE_SLOT) {
                inventory.setItem(i, filler);
            }
        }

        // salvar
        ItemStack saveButton = new ItemStack(Material.LIME_DYE);
        ItemMeta saveMeta = saveButton.getItemMeta();
        if (saveMeta != null) {
            saveMeta.displayName(Component.text("§a✔ Salvar Receita"));
            saveButton.setItemMeta(saveMeta);
        }
        inventory.setItem(SAVE_SLOT, saveButton);

        creatorInventories.add(inventory);
        activeCreators.put(player.getUniqueId(), recipeId);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!creatorInventories.contains(e.getInventory())) return;

        if (e.getClickedInventory() == e.getView().getTopInventory()) {
            int slot = e.getSlot();

            if (slot == SAVE_SLOT) {
                e.setCancelled(true);
                Player player = (Player) e.getWhoClicked();
                String id = activeCreators.get(player.getUniqueId());
                saveRecipe(player, e.getInventory(), id);
                return;
            }

            if (!INPUT_SLOTS.contains(slot) && slot != RESULT_SLOT) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inventory = e.getInventory();
        if (!creatorInventories.contains(inventory)) return;

        Player player = (Player) e.getPlayer();

        // devolve os itens no input e no result para o jogador
        List<Integer> slotsToReturn = new ArrayList<>(INPUT_SLOTS);
        slotsToReturn.add(RESULT_SLOT);

        for (int slot : slotsToReturn) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                inventory.setItem(slot, null);
            }
        }

        activeCreators.remove(player.getUniqueId());
        creatorInventories.remove(inventory);
    }

    private void saveRecipe(Player player, Inventory inventory, String id) {
        ItemStack resultStack = inventory.getItem(RESULT_SLOT);
        if (resultStack == null || resultStack.getType().isAir()) {
            player.sendMessage("§c✘ Insira um item de resultado no slot " + RESULT_SLOT + "!");
            return;
        }

        int count = 0;
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) count++;
        }

        if (count == 0) {
            player.sendMessage("§c✘ Insira ao menos um ingrediente na grade 5x5!");
            return;
        }

        File craftingFolder = new File(MineSkyItems.getInstance().getDataFolder(), "crafting");
        if (!craftingFolder.exists()) {
            craftingFolder.mkdirs();
        }

        File file = new File(craftingFolder, id + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        char[] keys = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789".toCharArray();
        int keyIndex = 0;

        Map<String, Character> ingredientKeyMap = new HashMap<>();
        Map<Character, Map<String, String>> keysSection = new HashMap<>();

        int[] rows = {0, 9, 18, 27, 36};
        List<String> shapeList = new ArrayList<>();

        for (int r = 0; r < 5; r++) {
            StringBuilder rowBuilder = new StringBuilder();
            int startSlot = rows[r];

            for (int c = 0; c < 5; c++) {
                int slot = startSlot + c;
                ItemStack item = inventory.getItem(slot);

                if (item == null || item.getType().isAir()) {
                    rowBuilder.append(" ");
                } else {
                    Item custom = ItemHandler.getItemFromStack(item);
                    String type = (custom != null) ? "MINESKYITEM" : "VANILLA";
                    String itemId = (custom != null) ? custom.getId() : item.getType().name();

                    String lookupKey = type + ":" + itemId;

                    if (!ingredientKeyMap.containsKey(lookupKey)) {
                        char assignedChar = keys[keyIndex++];
                        ingredientKeyMap.put(lookupKey, assignedChar);

                        Map<String, String> keyData = new HashMap<>();
                        keyData.put("type", type);
                        keyData.put("id", itemId);
                        keysSection.put(assignedChar, keyData);
                    }

                    rowBuilder.append(ingredientKeyMap.get(lookupKey));
                }
            }
            shapeList.add(rowBuilder.toString());
        }

        // salva keys
        for (Map.Entry<Character, Map<String, String>> entry : keysSection.entrySet()) {
            String path = "keys." + entry.getKey();
            config.set(path + ".type", entry.getValue().get("type"));
            config.set(path + ".id", entry.getValue().get("id"));
        }

        // salva o shaper
        config.set("shape", shapeList);

        // salva o resultado
        Item customResult = ItemHandler.getItemFromStack(resultStack);
        config.set("result.type", (customResult != null) ? "MINESKYITEM" : "VANILLA");
        config.set("result.id", (customResult != null) ? customResult.getId() : resultStack.getType().name());

        try {
            config.save(file);
            TinkeringManager.registerAllFromFile();
            player.sendMessage("§a✔ Receita '" + id + "' criada e salva com sucesso!");
            player.closeInventory();
        } catch (IOException e) {
            player.sendMessage("§c✘ Ocorreu um erro ao salvar o arquivo da receita.");
            e.printStackTrace();
        }
    }
}