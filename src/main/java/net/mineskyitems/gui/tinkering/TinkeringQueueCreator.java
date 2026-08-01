package net.mineskyitems.gui.tinkering;

import net.kyori.adventure.text.Component;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.gui.tinkering.recipe.TinkeringManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TinkeringQueueCreator implements Listener {

    public static final Set<Inventory> queueInventories = Collections.synchronizedSet(new HashSet<>());
    public static final Map<UUID, QueueItem> activePlayerQueue = new HashMap<>();

    public static final Map<UUID, QueueItem> heldPlayerItems = new HashMap<>();

    public static final Set<String> activeProcessingPaths = Collections.synchronizedSet(new HashSet<>());

    private static final Set<Integer> INPUT_SLOTS = Set.of(
            0, 1, 2, 3, 4,
            9, 10, 11, 12, 13,
            18, 19, 20, 21, 22,
            27, 28, 29, 30, 31,
            36, 37, 38, 39, 40
    );

    private static final int RESULT_SLOT = 25;
    private static final int INFO_SLOT = 48;
    private static final int SAVE_SLOT = 49;

    public static class QueueItem {
        final String categoryKey;
        final String itemKey;
        final String path;
        final String suggestedMaterial;
        final boolean isUpgrade;

        public QueueItem(String categoryKey, String itemKey, String path, String suggestedMaterial, boolean isUpgrade) {
            this.categoryKey = categoryKey;
            this.itemKey = itemKey;
            this.path = path;
            this.suggestedMaterial = suggestedMaterial;
            this.isUpgrade = isUpgrade;
        }
    }

    public static boolean processNextItem(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores podem executar este comando.");
            return true;
        }

        QueueItem queueItem;

        if (heldPlayerItems.containsKey(player.getUniqueId())) {
            queueItem = heldPlayerItems.get(player.getUniqueId());
            player.sendMessage("§e[Fila Tinkering] Retomando a criação do seu item reservado: §a" + queueItem.itemKey);
        } else {
            queueItem = getNextAvailableItem();
            if (queueItem == null) {
                player.sendMessage("§a✔ Nenhum item pendente para conversão no converting.yml!");
                return true;
            }
            heldPlayerItems.put(player.getUniqueId(), queueItem);
            activeProcessingPaths.add(queueItem.path);
        }

        Item customItem = ItemHandler.getItem(queueItem.itemKey);
        if (customItem == null) {
            player.sendMessage("§c✘ O item '" + queueItem.itemKey + "' não foi encontrado no ItemHandler!");
            return true;
        }

        activePlayerQueue.put(player.getUniqueId(), queueItem);
        openQueueGUI(player, queueItem, customItem.buildStack());
        return true;
    }

    private static synchronized QueueItem getNextAvailableItem() {
        File convertingFile = new File(MineSkyItems.getInstance().getDataFolder(), "converting.yml");
        if (!convertingFile.exists()) return null;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(convertingFile);

        for (String categoryKey : config.getKeys(false)) {
            ConfigurationSection category = config.getConfigurationSection(categoryKey);
            if (category == null) continue;

            for (String itemKey : category.getKeys(false)) {
                ConfigurationSection itemSection = category.getConfigurationSection(itemKey);
                if (itemSection == null) continue;

                boolean done = itemSection.getBoolean("done", false);
                if (done) continue;

                List<String> types = itemSection.getStringList("type");
                boolean isCrafting = types.contains("crafting");
                boolean isUpgrade = types.contains("upgrade");

                if (!isCrafting && !isUpgrade) continue;

                String path = categoryKey + "." + itemKey;

                if (activeProcessingPaths.contains(path)) continue;

                String suggestedMaterial = itemSection.getString("suggested-material", "Nenhum");

                return new QueueItem(categoryKey, itemKey, path, suggestedMaterial, isUpgrade);
            }
        }
        return null;
    }

    private static void openQueueGUI(Player player, QueueItem queueItem, ItemStack resultStack) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("§8Fila: " + queueItem.itemKey));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < 54; i++) {
            if (!INPUT_SLOTS.contains(i) && i != RESULT_SLOT && i != SAVE_SLOT && i != INFO_SLOT) {
                inventory.setItem(i, filler);
            }
        }

        inventory.setItem(RESULT_SLOT, resultStack);

        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Component.text("§eℹ Informações da Conversão"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§fItem ID: §a" + queueItem.itemKey));
            lore.add(Component.text("§fCategoria: §7" + queueItem.categoryKey));
            lore.add(Component.text("§fMaterial Sugerido: §b" + queueItem.suggestedMaterial));
            if (queueItem.isUpgrade) {
                lore.add(Component.empty());
                lore.add(Component.text("§c⚠ ATENÇÃO: Este item é um UPGRADE!"));
                lore.add(Component.text("§cÉ obrigatório incluir o item base na grade de crafting!"));
            }
            infoMeta.lore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        inventory.setItem(INFO_SLOT, infoItem);

        ItemStack saveButton = new ItemStack(Material.LIME_DYE);
        ItemMeta saveMeta = saveButton.getItemMeta();
        if (saveMeta != null) {
            saveMeta.displayName(Component.text("§a✔ Salvar e Concluir no converting.yml"));
            saveButton.setItemMeta(saveMeta);
        }
        inventory.setItem(SAVE_SLOT, saveButton);

        queueInventories.add(inventory);
        player.openInventory(inventory);

        player.sendMessage(" ");
        player.sendMessage("§a[Fila Tinkering] §fCriando receita para: §e" + queueItem.itemKey);
        player.sendMessage("§aMaterial Sugerido: §b" + queueItem.suggestedMaterial);
        if (queueItem.isUpgrade) {
            player.sendMessage("§c⚠ ATENÇÃO: Este item é um UPGRADE de outro item!");
            player.sendMessage("§cVocê precisa obrigatoriamente colocar o item base junto do crafting!");
        }
        player.sendMessage(" ");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!queueInventories.contains(e.getInventory())) return;

        if (e.getClickedInventory() == e.getView().getTopInventory()) {
            int slot = e.getSlot();

            if (slot == SAVE_SLOT) {
                e.setCancelled(true);
                Player player = (Player) e.getWhoClicked();
                QueueItem queueItem = activePlayerQueue.get(player.getUniqueId());
                if (queueItem != null) {
                    saveQueueRecipe(player, e.getInventory(), queueItem);
                }
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
        if (!queueInventories.contains(inventory)) return;

        Player player = (Player) e.getPlayer();
        QueueItem queueItem = activePlayerQueue.get(player.getUniqueId());

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

        if (queueItem != null) {
            activePlayerQueue.remove(player.getUniqueId());
            player.sendMessage("§e[Fila Tinkering] O item '" + queueItem.itemKey + "' ficou reservado exclusivamente para você!");
            player.sendMessage("§7Quando pegar os materiais, use §f/mineskyitems criar-tinkerer §7para continuar.");
        }

        queueInventories.remove(inventory);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (heldPlayerItems.containsKey(uuid)) {
            QueueItem queueItem = heldPlayerItems.remove(uuid);
            if (queueItem != null) {
                activeProcessingPaths.remove(queueItem.path);
            }
        }
        activePlayerQueue.remove(uuid);
    }

    private void saveQueueRecipe(Player player, Inventory inventory, QueueItem queueItem) {
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

        File file = new File(craftingFolder, queueItem.itemKey + ".yml");
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

        for (Map.Entry<Character, Map<String, String>> entry : keysSection.entrySet()) {
            String path = "keys." + entry.getKey();
            config.set(path + ".type", entry.getValue().get("type"));
            config.set(path + ".id", entry.getValue().get("id"));
        }

        config.set("shape", shapeList);

        Item customResult = ItemHandler.getItemFromStack(resultStack);
        config.set("result.type", (customResult != null) ? "MINESKYITEM" : "VANILLA");
        config.set("result.id", (customResult != null) ? customResult.getId() : resultStack.getType().name());

        try {
            config.save(file);
        } catch (IOException e) {
            player.sendMessage("§c✘ Erro ao salvar a receita no arquivo YAML.");
            e.printStackTrace();
            return;
        }

        File convertingFile = new File(MineSkyItems.getInstance().getDataFolder(), "converting.yml");
        if (convertingFile.exists()) {
            YamlConfiguration convertingConfig = YamlConfiguration.loadConfiguration(convertingFile);
            convertingConfig.set(queueItem.path + ".done", true);
            try {
                convertingConfig.save(convertingFile);
            } catch (IOException e) {
                player.sendMessage("§c✘ Erro ao atualizar o converting.yml");
                e.printStackTrace();
                return;
            }
        }

        TinkeringManager.registerAllFromFile();

        activeProcessingPaths.remove(queueItem.path);
        heldPlayerItems.remove(player.getUniqueId());
        activePlayerQueue.remove(player.getUniqueId());

        player.sendMessage("§a✔ Receita de '" + queueItem.itemKey + "' salva e concluída no converting.yml!");
        player.closeInventory();
    }
}