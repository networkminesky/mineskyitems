package net.mineskyitems.gui.kits;

import net.kyori.adventure.text.Component;
import net.mineskyitems.entities.kits.KitHandler;
import net.mineskyitems.entities.kits.KitHandler.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class KitListGUI implements Listener {

    public static class KitListHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 9);
        }
    }

    public static void openGUI(Player player) {
        int size = KitHandler.getListGuiSize();
        Component title = KitHandler.parseComponent(KitHandler.getListGuiTitle());

        Inventory inv = Bukkit.createInventory(new KitListHolder(), size, title);

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);

        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, border);
            }
        }

        for (Kit kit : KitHandler.getAllKits()) {
            int slot = kit.getSlot();
            if (slot < 0 || slot >= size) continue;

            ItemStack icon = kit.getIcon().clone();
            ItemMeta meta = icon.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.displayName(KitHandler.parseComponent(kit.getName()));

            List<Component> lore = new ArrayList<>();
            lore.add(KitHandler.parseComponent("<dark_gray>/kit: " + kit.getId() + "</dark_gray>"));
            lore.add(Component.empty());
            lore.add(KitHandler.parseComponent("<gray>Tempo de espera: <gold>" + KitHandler.formatTime(kit.getCooldown() * 1000L) + "</gold></gray>"));
            lore.add(Component.empty());

            if (player.hasPermission("mineskyitems.kit." + kit.getId().toLowerCase())) {
                lore.add(KitHandler.parseComponent("<green>✔ Você tem acesso a este kit!</green>"));
            } else {
                lore.add(KitHandler.parseComponent("<red>✘ Você não tem permissão para este kit.</red>"));
            }

            lore.add(Component.empty());
            lore.add(KitHandler.parseComponent("<yellow>➤ Clique para visualizar o conteúdo!</yellow>"));

            meta.lore(lore);
            icon.setItemMeta(meta);

            inv.setItem(slot, icon);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.0f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof KitListHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) return;

        for (Kit kit : KitHandler.getAllKits()) {
            if (kit.getSlot() == rawSlot) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
                KitPreviewGUI.open(player, kit);
                return;
            }
        }
    }
}