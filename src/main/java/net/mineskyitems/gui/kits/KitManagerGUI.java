package net.mineskyitems.gui.kits;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.kits.KitHandler;
import net.mineskyitems.entities.kits.KitHandler.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KitManagerGUI implements Listener {

    public static final Map<UUID, EditSession> activeSessions = new ConcurrentHashMap<>();
    public static final Map<UUID, EditSession> awaitingNameInput = new ConcurrentHashMap<>();

    public static class KitManagerHolder implements InventoryHolder {
        private final EditSession session;

        public KitManagerHolder(EditSession session) {
            this.session = session;
        }

        public EditSession getSession() {
            return session;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 54);
        }
    }

    public static class EditSession {
        private final String id;
        private String displayName;
        private int cooldown;
        private int slot;
        private ItemStack icon;
        private final Map<Integer, ItemStack> items = new HashMap<>();

        public EditSession(String id, String displayName, int cooldown, int slot, ItemStack icon) {
            this.id = id;
            this.displayName = displayName;
            this.cooldown = cooldown;
            this.slot = slot;
            this.icon = icon;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public int getCooldown() { return cooldown; }
        public void setCooldown(int cooldown) { this.cooldown = cooldown; }
        public int getSlot() { return slot; }
        public void setSlot(int slot) { this.slot = slot; }
        public ItemStack getIcon() { return icon; }
        public void setIcon(ItemStack icon) { this.icon = icon; }
        public Map<Integer, ItemStack> getItems() { return items; }
    }

    public static void openCreate(Player player, String id) {
        EditSession session = new EditSession(
                id.toLowerCase(),
                "<#FFAA00>Kit " + id,
                86400,
                10,
                new ItemStack(Material.CHEST)
        );
        activeSessions.put(player.getUniqueId(), session);
        openInventory(player, session);
    }

    public static void openEdit(Player player, Kit kit) {
        EditSession session = new EditSession(
                kit.getId(),
                kit.getName(),
                kit.getCooldown(),
                kit.getSlot(),
                kit.getIcon().clone()
        );
        for (Map.Entry<Integer, ItemStack> entry : kit.getItems().entrySet()) {
            if (entry.getValue() != null) {
                session.getItems().put(entry.getKey(), entry.getValue().clone());
            }
        }
        activeSessions.put(player.getUniqueId(), session);
        openInventory(player, session);
    }

    public static void openInventory(Player player, EditSession session) {
        Inventory inv = Bukkit.createInventory(new KitManagerHolder(session), 54, KitHandler.parseComponent("<#FF8800>Editor de Kit: <#FFEAA7>" + session.getId()));

        for (Map.Entry<Integer, ItemStack> entry : session.getItems().entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < 36) {
                inv.setItem(entry.getKey(), entry.getValue());
            }
        }

        ItemStack sep = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta sepM = sep.getItemMeta();
        sepM.displayName(Component.text(" "));
        sep.setItemMeta(sepM);

        for (int i = 36; i < 45; i++) {
            inv.setItem(i, sep);
        }
        inv.setItem(46, sep);
        inv.setItem(48, sep);
        inv.setItem(50, sep);
        inv.setItem(52, sep);

        updateControlPanel(inv, session);
        player.openInventory(inv);
    }

    private static void updateControlPanel(Inventory inv, EditSession session) {
        ItemStack iconItem = session.getIcon().clone();
        ItemMeta iconMeta = iconItem.getItemMeta();
        iconMeta.displayName(KitHandler.parseComponent("<gold><bold>Ícone do Kit</bold></gold>"));
        List<Component> iconLore = new ArrayList<>();
        iconLore.add(KitHandler.parseComponent("<gray>Material: <yellow>" + session.getIcon().getType().name() + "</yellow></gray>"));
        iconLore.add(Component.empty());
        iconLore.add(KitHandler.parseComponent("<yellow>➤ Clique com um item no cursor para alterar!</yellow>"));
        iconMeta.lore(iconLore);
        iconItem.setItemMeta(iconMeta);
        inv.setItem(45, iconItem);

        ItemStack nameItem = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = nameItem.getItemMeta();
        nameMeta.displayName(KitHandler.parseComponent("<yellow><bold>Definir Nome Público</bold></yellow>"));
        List<Component> nameLore = new ArrayList<>();
        nameLore.add(KitHandler.parseComponent("<gray>Atual: </gray>" + session.getDisplayName()));
        nameLore.add(Component.empty());
        nameLore.add(KitHandler.parseComponent("<yellow>➤ Clique para digitar um novo nome no chat!</yellow>"));
        nameMeta.lore(nameLore);
        nameItem.setItemMeta(nameMeta);
        inv.setItem(47, nameItem);

        ItemStack clockItem = new ItemStack(Material.CLOCK);
        ItemMeta clockMeta = clockItem.getItemMeta();
        clockMeta.displayName(KitHandler.parseComponent("<aqua><bold>Tempo de Cooldown</bold></aqua>"));
        List<Component> clockLore = new ArrayList<>();
        clockLore.add(KitHandler.parseComponent("<gray>Atual: <gold>" + session.getCooldown() + "s</gold> (<yellow>" + KitHandler.formatTime(session.getCooldown() * 1000L) + "</yellow>)</gray>"));
        clockLore.add(Component.empty());
        clockLore.add(KitHandler.parseComponent("<green>Botão Esquerdo: +60s</green>"));
        clockLore.add(KitHandler.parseComponent("<red>Botão Direito: -60s</red>"));
        clockLore.add(KitHandler.parseComponent("<green>Shift + Esquerdo: +3600s (1h)</green>"));
        clockLore.add(KitHandler.parseComponent("<red>Shift + Direito: -3600s (1h)</red>"));
        clockMeta.lore(clockLore);
        clockItem.setItemMeta(clockMeta);
        inv.setItem(49, clockItem);

        ItemStack slotItem = new ItemStack(Material.COMPASS);
        ItemMeta slotMeta = slotItem.getItemMeta();
        slotMeta.displayName(KitHandler.parseComponent("<light_purple><bold>Posição no Menu</bold></light_purple>"));
        List<Component> slotLore = new ArrayList<>();
        slotLore.add(KitHandler.parseComponent("<gray>Slot atual: <gold>" + session.getSlot() + "</gold></gray>"));
        slotLore.add(Component.empty());
        slotLore.add(KitHandler.parseComponent("<green>Botão Esquerdo: +1</green>"));
        slotLore.add(KitHandler.parseComponent("<red>Botão Direito: -1</red>"));
        slotMeta.lore(slotLore);
        slotItem.setItemMeta(slotMeta);
        inv.setItem(51, slotItem);

        ItemStack saveItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta saveMeta = saveItem.getItemMeta();
        saveMeta.displayName(KitHandler.parseComponent("<green><bold>✔ SALVAR KIT</bold></green>"));
        List<Component> saveLore = new ArrayList<>();
        saveLore.add(KitHandler.parseComponent("<gray>Clique para salvar todas as alterações</gray>"));
        saveLore.add(KitHandler.parseComponent("<gray>no arquivo kits.yml e atualizar os dados.</gray>"));
        saveMeta.lore(saveLore);
        saveItem.setItemMeta(saveMeta);
        inv.setItem(53, saveItem);
    }

    private static void syncItemsFromInventory(Inventory inv, EditSession session) {
        session.getItems().clear();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                session.getItems().put(i, item.clone());
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof KitManagerHolder holder)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) return;

        Player player = (Player) event.getWhoClicked();
        EditSession session = holder.getSession();

        if (rawSlot >= 36 && rawSlot < 54) {
            event.setCancelled(true);

            if (rawSlot == 45) {
                ItemStack cursor = event.getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    ItemStack newIcon = cursor.clone();
                    newIcon.setAmount(1);
                    session.setIcon(newIcon);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
                    updateControlPanel(event.getInventory(), session);
                }
            } else if (rawSlot == 47) {
                syncItemsFromInventory(event.getInventory(), session);
                awaitingNameInput.put(player.getUniqueId(), session);
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                player.sendMessage(KitHandler.parseComponent("<yellow>Digite o novo nome público para o kit no chat (suporta cores & ou <#HEX>):</yellow>"));
                player.sendMessage(KitHandler.parseComponent("<gray>Digite <red>cancelar</red> para desistir.</gray>"));
            } else if (rawSlot == 49) {
                int delta = 60;
                if (event.isShiftClick()) delta = 3600;
                if (event.isRightClick()) delta = -delta;

                session.setCooldown(Math.max(0, session.getCooldown() + delta));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.3f);
                updateControlPanel(event.getInventory(), session);
            } else if (rawSlot == 51) {
                int delta = event.isRightClick() ? -1 : 1;
                session.setSlot(Math.max(0, Math.min(53, session.getSlot() + delta)));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.3f);
                updateControlPanel(event.getInventory(), session);
            } else if (rawSlot == 53) {
                syncItemsFromInventory(event.getInventory(), session);

                Kit kit = new Kit(
                        session.getId(),
                        session.getDisplayName(),
                        session.getSlot(),
                        session.getCooldown(),
                        session.getIcon(),
                        session.getItems()
                );

                KitHandler.saveKit(kit);
                activeSessions.remove(player.getUniqueId());

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.sendMessage(KitHandler.parseComponent("<green>✔ Kit <gold>" + session.getId() + "</gold> salvo com sucesso no kits.yml!</green>"));
                player.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        EditSession session = awaitingNameInput.remove(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        player.getScheduler().run(MineSkyItems.getInstance(), task -> {
            if (!input.equalsIgnoreCase("cancelar")) {
                session.setDisplayName(input);
                player.sendMessage(KitHandler.parseComponent("<green>✔ Nome atualizado para: </green>" + input));
            } else {
                player.sendMessage(KitHandler.parseComponent("<red>✘ Alteração de nome cancelada.</red>"));
            }
            openInventory(player, session);
        }, null);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof KitManagerHolder holder)) return;

        Player player = (Player) event.getPlayer();
        if (!awaitingNameInput.containsKey(player.getUniqueId())) {
            activeSessions.remove(player.getUniqueId());
        }
    }
}