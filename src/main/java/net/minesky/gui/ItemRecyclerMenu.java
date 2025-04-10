package net.minesky.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minesky.entities.ItemBuilder;
import net.minesky.entities.item.Item;
import net.minesky.entities.item.ItemHandler;
import net.minesky.utils.ChatInputCallback;
import net.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ItemRecyclerMenu implements Listener {
    public static HashMap<Player, Inventory> inventories = new HashMap<>();
    private final List<Integer> inputSlots = List.of(1, 2, 3, 10, 11, 12, 19, 20, 21);

    public static ItemStack simpleButton(Material m, String name, String... lore) {
        return simpleButton(m, name, 1, lore);
    }
    public static ItemStack simpleButton(Material m, String name, int count, String... lore) {
        ItemStack it = new ItemStack(m, count);
        ItemMeta im = it.getItemMeta();

        im.setDisplayName("§6§l"+name);

        im.setLore(Arrays.stream(lore)
                .map(a -> Utils.c("&7"+a))
                .collect(Collectors.toList()));

        it.setItemMeta(im);
        return it;
    }

    private static void reorganizeItems(Inventory inv) {
        inv.setItem(14, simpleButton(
                Material.MAGMA_CREAM, "Quebrar", "• Quebre seus itens", " antigos e ganhe Dust.",
                " ",
                "&e➳ Clique esquerdo - Quebrar o item",
                "&e➳ Clique direito - Cancelar e fehcar menu")
        );
    }

    public static void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Reciclagem de item");

        inventories.put(player, inv);

        reorganizeItems(inv);

        player.openInventory(inv);
    }

    public static void reopenInventory(Player player) {
        Inventory inv = inventories.get(player);
        if(inv == null)
            return;

        reorganizeItems(inv);

        player.closeInventory();
        player.openInventory(inv);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if(!inventories.containsValue(e.getInventory())) return;

        for (int slot : e.getRawSlots()) {
            if (slot >= 0 && slot < 27 && !List.of(1, 2, 3, 10, 11, 12, 19, 20, 21).contains(slot)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        final Player p = (Player) e.getWhoClicked();
        final int slot = e.getSlot();
        final ClickType clickType = e.getClick();

        if(!inventories.containsValue(e.getInventory()))
            return;

        e.setCancelled(true);

        if (slot >= 0 && slot < 27 && List.of(1, 2, 3, 10, 11, 12, 19, 20, 21).contains(slot)) {
            e.setCancelled(false);
        }

        switch(slot) {
            case 14 -> {
                switch(clickType) {
                    case RIGHT -> {
                        p.closeInventory();
                        inventories.remove(p);
                    }
                    case LEFT -> {
                        // BASICÃO :)
                        List<ItemStack> items = getInputItems(e.getInventory());

                        if (items.isEmpty()) {
                            p.sendMessage(Utils.c("&cColoque ao menos um item para reciclar!"));
                            return;
                        }

                        int totalDust = 0;
                        for (ItemStack item : items) {
                            int dust = getRecycleValue(item);
                            totalDust += dust;

                            e.getInventory().remove(item);
                        }

                        p.sendMessage(Utils.c("&aVocê recebeu " + totalDust + " de Dust!"));
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    }
                }
            }
        }

        switch(clickType) {
            case RIGHT -> {
                p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 0);
                reopenInventory(p);
            }
            case LEFT -> p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1);
        }

    }

    private List<ItemStack> getInputItems(Inventory inv) {
        return inputSlots.stream()
                .map(inv::getItem)
                .filter(item -> item != null && item.getType() != Material.AIR)
                .collect(Collectors.toList());
    }

    private int getRecycleValue(ItemStack stack) {
        Item item = ItemHandler.getItemFromStack(stack);
        int level = item.getRequiredLevel();
        //BALANCIAR
        switch (item.getItemRarity().getName()) {
            case "Comum" -> {
                return 1 * level;
            }
            case "Incomum" -> {
                return 2 * level;
            }
            case "Raro" -> {
                return 3 * level;
            }
            case "Épico" -> {
                return 4 * level;
            }
            case "Lendário" -> {
                return  5 * level;
            }
            case "Especial" -> {
                return  6 * level;
            }
        }
        return 1;
    }


}
