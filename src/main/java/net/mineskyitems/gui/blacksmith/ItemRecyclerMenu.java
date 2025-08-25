package net.mineskyitems.gui.blacksmith;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.ItemDustHandler;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static net.mineskyitems.gui.MenuUtils.modelButton;
import static net.mineskyitems.gui.MenuUtils.simpleButton;

public class ItemRecyclerMenu implements Listener {
    public static HashMap<Player, Inventory> inventories = new HashMap<>();
    private static final List<Integer> inputSlots = List.of(0, 1, 2, 9, 10, 11, 18, 19, 20);

    private static final List<Integer> destroyButtonSlots = List.of(13, 14);

    private static void reorganizeItems(Inventory inv) {
        destroyButtonSlots.forEach(slot -> inv.setItem(slot, modelButton(
                Material.PAPER, "Destruir", 2, "• Destrua seus itens antigos", " e receba pó de item.",
                " ",
                "&e➳ Clique esquerdo - Desmantelar o item"
        )));
    }

    private static final List<Integer> outputSlots = List.of(7, 8, 16, 17, 25, 26);
    private static void setOutputItems(Inventory inventory) {
        outputSlots.forEach(a -> {
            inventory.setItem(a, null);
        });

        int remaining = getTotalDust(getInputItems(inventory));

        int slotIndex = 0;

        while (remaining > 0 && slotIndex < outputSlots.size()) {
            int amount = Math.min(64, remaining); // max 64
            int slot = outputSlots.get(slotIndex);

            ItemStack dusts = ItemDustHandler.dustItem.buildStack().clone();
            dusts.setAmount(amount);
            inventory.setItem(slot, dusts);

            remaining -= amount;
            slotIndex++;
        }

        final int epic = getTotalEpicDust(getInputItems(inventory));
        if(epic > 0) {
            ItemStack epicDust = ItemDustHandler.epicDustItem.buildStack().clone();
            epicDust.setAmount(epic);
            ItemMeta im = epicDust.getItemMeta();
            List<Component> components = im.lore();
            components.add(Component.text(" * 50% de chance")
                    .color(NamedTextColor.GOLD));
            im.lore(components);
            epicDust.setItemMeta(im);

            try {
                inventory.setItem(outputSlots.get(slotIndex), epicDust);
            }catch (IndexOutOfBoundsException ignore) {}
        }
    }

    public static void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                "[{\"text\":\"VWX\",\"font\":\"guis\",\"color\":\"white\"},{\"text\":\"Destruindo itens\",\"font\":\"default\",\"color\":\"black\"}]");

        inventories.put(player, inv);

        reorganizeItems(inv);

        player.openInventory(inv);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if(!inventories.containsValue(e.getInventory())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if(!inventories.containsValue(e.getInventory())) return;

        final Player p = (Player)e.getPlayer();

        inventories.remove(p);

        for(int slot : inputSlots) {
            ItemStack itemStack = e.getInventory().getItem(slot);
            if(itemStack != null && !itemStack.getType().isAir()) {

                p.getInventory().addItem(itemStack);

            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        final Player p = (Player) e.getWhoClicked();
        final int slot = e.getSlot();
        final ClickType clickType = e.getClick();
        final Inventory inventory = e.getInventory();

        if(!inventories.containsValue(inventory))
            return;

        if(clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            e.setCancelled(true);
            return;
        }

        if(!inventory.equals(e.getClickedInventory()))
            return;

        if (!inputSlots.contains(slot)) {

            e.setCancelled(true);

        } else {

            ItemStack itemStack = e.getCurrentItem();

            if(itemStack == null)
                itemStack = e.getCursor();

            if(!Utils.isMineSkyItem(itemStack)) {
                p.sendMessage(Utils.c("&cPrecisa ser um item que pode ser destruido."));
                e.setCancelled(true);
                return;
            }

        }

        Bukkit.getScheduler().runTaskLater(MineSkyItems.getInstance(), a -> {
            setOutputItems(inventory);
        }, 2);

        p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1f);

        if(destroyButtonSlots.contains(slot)
        && clickType == ClickType.LEFT) {
            if(p.getInventory().firstEmpty() == -1) {
                p.sendMessage(Utils.c("&cVocê deve ter pelo menos um slot vazio no inventário!"));
                return;
            }

            List<ItemStack> itemStacks = getInputItems(inventory);
            if (itemStacks.isEmpty()) {
                p.sendMessage(Utils.c("&cColoque ao menos um item para destruir!"));
                return;
            }

            final int totalDust = getTotalDust(itemStacks);
            final int totalEpicDust = getTotalEpicDust(itemStacks);

            if((totalDust + totalEpicDust) >= 384) {
                p.sendMessage(Utils.c("&cVocê está tentando destruir muitos itens de uma só vez!"));
                return;
            }

            itemStacks.forEach(inventory::removeItem);

            p.closeInventory();

            ItemStack dusts = ItemDustHandler.dustItem.buildStack().clone();
            dusts.setAmount(totalDust);

            p.getInventory().addItem(dusts);

            p.sendMessage(Utils.c("&aVocê recebeu x" + totalDust + " pó de item ao destruir o seu item!"));

            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 0f);
            p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 2f);

            if(totalEpicDust > 0 && new Random().nextBoolean()) {
                ItemStack epicDusts = ItemDustHandler.epicDustItem.buildStack().clone();
                epicDusts.setAmount(totalEpicDust);

                p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 2f, 1.2f);
                p.getInventory().addItem(epicDusts);

                p.sendMessage(Utils.c("&dVocê recebeu x" + totalEpicDust + " pó de item épico ao destruir o seu item!"));
            }

            p.spawnParticle(Particle.WAX_OFF, p.getLocation(), 30, 0.5, 0.5, 0.5);
        }
    }

    private static int getTotalDust(List<ItemStack> itemStacks) {
        int totalDust = 0;
        for(ItemStack item : itemStacks) {
            Item msItem = ItemHandler.getItemFromStack(item);
            if(msItem == null) continue;

            int dust = ItemDustHandler.howManyDustsForItem(msItem);
            totalDust += dust;
        }
        return totalDust;
    }

    private static int getTotalEpicDust(List<ItemStack> itemStacks) {
        int totalDust = 0;
        for(ItemStack item : itemStacks) {
            Item msItem = ItemHandler.getItemFromStack(item);
            if(msItem == null) continue;
            if(msItem.getRequiredLevel() < 60) continue;

            int dust = ItemDustHandler.howManyEpicDustsForItem(msItem);
            totalDust += dust;
        }
        return totalDust;
    }

    private static List<ItemStack> getInputItems(Inventory inv) {
        return inputSlots.stream()
                .map(inv::getItem)
                .filter(item -> item != null && !item.getType().isAir())
                .collect(Collectors.toList());
    }
}
