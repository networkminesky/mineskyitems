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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KitPreviewGUI implements Listener {

    public static class KitPreviewHolder implements InventoryHolder {
        private final Kit kit;

        public KitPreviewHolder(Kit kit) {
            this.kit = kit;
        }

        public Kit getKit() {
            return kit;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 9);
        }
    }

    public static void open(Player player, Kit kit) {
        int size = KitHandler.getPreviewGuiSize();
        String titleStr = KitHandler.getPreviewGuiTitle().replace("%kit_name%", kit.getName());
        Component title = KitHandler.parseComponent(titleStr);

        Inventory inv = Bukkit.createInventory(new KitPreviewHolder(kit), size, title);

        for (Map.Entry<Integer, ItemStack> entry : kit.getItems().entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < size - 9) {
                inv.setItem(slot, entry.getValue().clone());
            }
        }

        ItemStack separator = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta sepMeta = separator.getItemMeta();
        sepMeta.displayName(Component.text(" "));
        separator.setItemMeta(sepMeta);

        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, separator);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(KitHandler.parseComponent("<red><bold>« Voltar</bold></red>"));
        List<Component> backLore = new ArrayList<>();
        backLore.add(KitHandler.parseComponent("<gray>Clique para retornar ao menu de kits.</gray>"));
        backMeta.lore(backLore);
        back.setItemMeta(backMeta);
        inv.setItem(size - 9, back);

        ItemStack claim = new ItemStack(Material.NETHER_STAR);
        ItemMeta claimMeta = claim.getItemMeta();
        claimMeta.displayName(KitHandler.parseComponent("<green><bold>✔ RESGATAR KIT</bold></green>"));

        List<Component> claimLore = new ArrayList<>();
        claimLore.add(KitHandler.parseComponent("<gray>Kit: </gray>" + kit.getName()));
        claimLore.add(KitHandler.parseComponent("<gray>Cooldown: <gold>" + KitHandler.formatTime(kit.getCooldown() * 1000L) + "</gold></gray>"));
        claimLore.add(Component.empty());

        if (player.hasPermission("mineskyitems.kit." + kit.getId().toLowerCase())) {
            claimLore.add(KitHandler.parseComponent("<yellow>➤ Clique aqui para resgatar este kit!</yellow>"));
        } else {
            claimLore.add(KitHandler.parseComponent("<red>✘ Você não possui permissão para pegar este kit.</red>"));
        }

        claimMeta.lore(claimLore);
        claim.setItemMeta(claimMeta);
        inv.setItem(size - 5, claim);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof KitPreviewHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        int size = event.getInventory().getSize();

        if (rawSlot == size - 9) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
            KitListGUI.openGUI(player);
        } else if (rawSlot == size - 5) {
            KitHandler.claimKit(player, holder.getKit());
        }
    }
}