package net.mineskyitems.entities.kits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.api.database.UpdatedData;
import net.minesky.api.database.ValueType;
import net.minesky.core.databridge.callbacks.ErrorType;
import net.minesky.core.databridge.callbacks.FindValueCallback;
import net.minesky.core.databridge.callbacks.SetOneCallback;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class KitHandler {

    private static final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private static File kitsFile;
    private static YamlConfiguration kitsConfig;

    private static String listGuiTitle = "<#FFAA00><bold>Kits do Servidor</bold>";
    private static int listGuiSize = 45;
    private static String previewGuiTitle = "<#FFAA00>Visualizar: <#FFEAA7>%kit_name%";
    private static int previewGuiSize = 54;

    public static void load() {
        kits.clear();
        kitsFile = new File(MineSkyItems.getInstance().getDataFolder(), "kits.yml");

        if (!kitsFile.exists()) {
            MineSkyItems.getInstance().getDataFolder().mkdirs();
            try {
                kitsFile.createNewFile();
                kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
                kitsConfig.set("gui.list.title", listGuiTitle);
                kitsConfig.set("gui.list.size", listGuiSize);
                kitsConfig.set("gui.preview.title", previewGuiTitle);
                kitsConfig.set("gui.preview.size", previewGuiSize);
                kitsConfig.createSection("kits");
                kitsConfig.save(kitsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
        }

        listGuiTitle = kitsConfig.getString("gui.list.title", listGuiTitle);
        listGuiSize = kitsConfig.getInt("gui.list.size", listGuiSize);
        previewGuiTitle = kitsConfig.getString("gui.preview.title", previewGuiTitle);
        previewGuiSize = kitsConfig.getInt("gui.preview.size", previewGuiSize);

        ConfigurationSection section = kitsConfig.getConfigurationSection("kits");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection sec = section.getConfigurationSection(id);
            if (sec == null) continue;

            String name = sec.getString("name", id);
            int slot = sec.getInt("slot", 0);
            int cooldown = sec.getInt("cooldown", 0);

            Material mat = Material.matchMaterial(sec.getString("icon.material", "CHEST"));
            if (mat == null) mat = Material.CHEST;
            ItemStack icon = new ItemStack(mat);
            int cmd = sec.getInt("icon.custom-model-data", 0);
            if (cmd > 0) {
                ItemMeta meta = icon.getItemMeta();
                meta.setCustomModelData(cmd);
                icon.setItemMeta(meta);
            }

            Map<Integer, ItemStack> items = new HashMap<>();
            ConfigurationSection itemsSec = sec.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String key : itemsSec.getKeys(false)) {
                    try {
                        int itemSlot = Integer.parseInt(key);
                        int amount = itemsSec.getInt(key + ".amount", 1);
                        String strItem = itemsSec.getString(key + ".item");

                        if (strItem != null && strItem.toLowerCase().startsWith("mineskyitem:")) {
                            String customId = strItem.substring(strItem.indexOf(":") + 1).trim();
                            Item custom = ItemHandler.getItemById(customId);
                            if (custom != null) {
                                ItemStack stack = custom.buildStack();
                                stack.setAmount(amount);
                                items.put(itemSlot, stack);
                                continue;
                            }
                        }

                        ItemStack vanilla = itemsSec.getItemStack(key + ".item");
                        if (vanilla != null) {
                            vanilla.setAmount(amount);
                            items.put(itemSlot, vanilla);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            Kit kit = new Kit(id, name, slot, cooldown, icon, items);
            kits.put(id.toLowerCase(), kit);
        }
    }

    public static void reload() {
        load();
    }

    public static void saveKit(Kit kit) {
        kits.put(kit.getId().toLowerCase(), kit);

        String path = "kits." + kit.getId();
        kitsConfig.set(path + ".name", kit.getName());
        kitsConfig.set(path + ".slot", kit.getSlot());
        kitsConfig.set(path + ".cooldown", kit.getCooldown());
        kitsConfig.set(path + ".icon.material", kit.getIcon().getType().name());

        if (kit.getIcon().hasItemMeta() && kit.getIcon().getItemMeta().hasCustomModelData()) {
            kitsConfig.set(path + ".icon.custom-model-data", kit.getIcon().getItemMeta().getCustomModelData());
        } else {
            kitsConfig.set(path + ".icon.custom-model-data", 0);
        }

        kitsConfig.set(path + ".items", null);
        for (Map.Entry<Integer, ItemStack> entry : kit.getItems().entrySet()) {
            int slot = entry.getKey();
            ItemStack stack = entry.getValue();
            if (stack == null || stack.getType().isAir()) continue;

            String itemPath = path + ".items." + slot;
            Item custom = ItemHandler.getItemFromStack(stack);
            if (custom != null) {
                kitsConfig.set(itemPath + ".item", "mineskyitem: " + custom.getId());
                kitsConfig.set(itemPath + ".amount", stack.getAmount());
            } else {
                kitsConfig.set(itemPath + ".item", stack);
                kitsConfig.set(itemPath + ".amount", stack.getAmount());
            }
        }

        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteKit(String id) {
        Kit removed = kits.remove(id.toLowerCase());
        if (removed == null && !kitsConfig.contains("kits." + id)) {
            return false;
        }

        kitsConfig.set("kits." + id, null);
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static Kit getKit(String id) {
        return kits.get(id.toLowerCase());
    }

    public static Collection<Kit> getAllKits() {
        return kits.values();
    }

    public static String getListGuiTitle() {
        return listGuiTitle;
    }

    public static int getListGuiSize() {
        return listGuiSize;
    }

    public static String getPreviewGuiTitle() {
        return previewGuiTitle;
    }

    public static int getPreviewGuiSize() {
        return previewGuiSize;
    }

    public static void claimKit(Player player, Kit kit) {
        if (!player.hasPermission("mineskyitems.kit." + kit.getId().toLowerCase())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(parseComponent("<red>✘ Você não tem permissão para resgatar o kit <gold>" + kit.getName() + "</gold>!</red>"));
            return;
        }

        getKitCooldownAsync(player, kit.getId(), lastClaim -> {
            long now = System.currentTimeMillis();
            long cooldownMillis = kit.getCooldown() * 1000L;
            long diff = (lastClaim + cooldownMillis) - now;

            if (diff > 0 && !player.hasPermission("mineskyitems.bypass.cooldown")) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(parseComponent("<red>✘ Aguarde mais <gold>" + formatTime(diff) + "</gold> para resgatar este kit novamente!</red>"));
                return;
            }

            List<ItemStack> toGive = new ArrayList<>();
            for (ItemStack item : kit.getItems().values()) {
                if (item != null && !item.getType().isAir()) {
                    toGive.add(item.clone());
                }
            }

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(toGive.toArray(new ItemStack[0]));
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                player.sendMessage(parseComponent("<yellow>⚠ Alguns itens foram dropados no chão por falta de espaço no inventário!</yellow>"));
            }

            setKitCooldownAsync(player, kit.getId(), now);

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.sendMessage(parseComponent("<green>✔ Você resgatou o kit </green>" + kit.getName() + "<green> com sucesso!</green>"));
            player.closeInventory();
        });
    }

    public static void getKitCooldownAsync(Player player, String kitId, Consumer<Long> callback) {
        try {
            PlayerDatabase.getPlayerSpecificDataAsync(
                    player.getUniqueId().toString(),
                    ValueType.LONG,
                    "kits." + kitId.toLowerCase(),
                    new FindValueCallback() {
                        @Override
                        public void onQueryDone(Document doc, Object value, boolean found) {
                            long lastClaim = 0L;
                            if (found && value instanceof Number n) {
                                lastClaim = n.longValue();
                            }
                            final long res = lastClaim;
                            player.getScheduler().run(MineSkyItems.getInstance(), task -> callback.accept(res), null);
                        }

                        @Override
                        public void onQueryError(ErrorType type) {
                            player.getScheduler().run(MineSkyItems.getInstance(), task -> callback.accept(0L), null);
                        }
                    }
            );
        } catch (Throwable t) {
            player.getScheduler().run(MineSkyItems.getInstance(), task -> callback.accept(0L), null);
        }
    }

    public static void setKitCooldownAsync(Player player, String kitId, long timestamp) {
        try {
            PlayerDatabase.setPlayerData(player.getUniqueId().toString(),
                    new UpdatedData("kits." + kitId.toLowerCase(), timestamp), new SetOneCallback() {
                @Override
                public void onSetDone() {}
                @Override
                public void onSetError(ErrorType type) {}
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static Component parseComponent(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        text = text.replaceAll("&#([a-fA-F0-9]{6})", "<#$1>");
        text = text.replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
        try {
            return MiniMessage.miniMessage().deserialize(text).decoration(TextDecoration.ITALIC, false);
        } catch (Exception ex) {
            return Component.text(text).decoration(TextDecoration.ITALIC, false);
        }
    }

    public static String formatTime(long millis) {
        long seconds = Math.max(0, millis / 1000);
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(secs).append("s");
        return sb.toString().trim();
    }

    public static class Kit {
        private final String id;
        private String name;
        private int slot;
        private int cooldown;
        private ItemStack icon;
        private final Map<Integer, ItemStack> items;

        public Kit(String id, String name, int slot, int cooldown, ItemStack icon, Map<Integer, ItemStack> items) {
            this.id = id;
            this.name = name;
            this.slot = slot;
            this.cooldown = cooldown;
            this.icon = icon;
            this.items = items;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSlot() {
            return slot;
        }

        public void setSlot(int slot) {
            this.slot = slot;
        }

        public int getCooldown() {
            return cooldown;
        }

        public void setCooldown(int cooldown) {
            this.cooldown = cooldown;
        }

        public ItemStack getIcon() {
            return icon;
        }

        public void setIcon(ItemStack icon) {
            this.icon = icon;
        }

        public Map<Integer, ItemStack> getItems() {
            return items;
        }
    }
}