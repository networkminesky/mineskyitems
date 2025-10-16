package net.mineskyitems.gui.blacksmith;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.ItemDustHandler;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static net.mineskyitems.gui.MenuUtils.simpleButton;

public class ItemRepairMenu implements Listener {

    public static HashMap<Player, Inventory> inventories = new HashMap<>();

    private static final int itemInputSlot = 9;
    private static final List<Integer> dustInputSlots = List.of(12, 13, 14);

    private static final int outputSlot = 17;

    private static final List<Integer> repairButtonSlots = List.of(15, 16);

    private static void reorganizeItems(Inventory inv) {
        final int remaining = remainingDustsForItem(inv, ItemHandler.getItemFromStack(inv.getItem(itemInputSlot)));

        String[] str = (remaining > 0 ?
                new String[]{" &fAinda faltam colocar mais ", " &fx" + remaining + " pós para reparar o item!"}
                : new String[]{" &a✔ O seu item já pode", " &aser reparado!"});
        if(remaining < 0) {
            str = new String[] { " &cVocê colocou pós demais, indique", " &ca quantidade correta para", " &creparar o seu item." };
        }

        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta im = button.getItemMeta();

        im.setCustomModelData(2);
        im.setDisplayName("§6§lReparar");

        List<String> lore = new ArrayList<>();
        lore.addAll(Arrays.asList("• Clique aqui para reparar o seu", " item utilizando pó de item.", " "));
        lore.addAll(List.of(str));
        lore.addAll(Arrays.asList(" ", "&e➳ Clique esquerdo - Reparar item"));

        im.setLore(lore.stream()
                .map(a -> Utils.c("&7"+a))
                .collect(Collectors.toList()));

        button.setItemMeta(im);

        repairButtonSlots.forEach(slot -> inv.setItem(slot, button));
    }

    public static final Key KEY = Key.key(Key.MINECRAFT_NAMESPACE, "guis");

    public static void openMainMenu(Player player) {
        // raw implementation because fuck you kyori
        // thank you mineskycore
        Inventory inv = Bukkit.createInventory(null, 27,
        "[{\"text\":\"VYX\",\"font\":\"guis\",\"color\":\"white\"},{\"text\":\"Reparando itens\",\"font\":\"default\",\"color\":\"black\"}]");

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

        ItemStack itemStack = e.getInventory().getItem(itemInputSlot);
        if(itemStack != null && !itemStack.getType().isAir()) {
            p.getInventory().addItem(itemStack);
        }

        dustInputSlots.forEach(a -> {
            ItemStack stack = e.getInventory().getItem(a);
            if(stack != null && !stack.getType().isAir()) {
                p.getInventory().addItem(stack);
            }
        });
    }

    private static int remainingDustsForItem(Inventory inventory, Item item) {
        if(item == null) return 0;

        int result = ItemDustHandler.howManyDustsForItem(item) - howManyDustsInputted(inventory);

        return result;
    }

    private static List<ItemStack> getInputtedDusts(Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        dustInputSlots.forEach(dust -> {
            ItemStack it = inventory.getItem(dust);
            if(it != null && !it.getType().isAir())
                items.add(it);
        });
        return items;
    }

    private static int howManyDustsInputted(Inventory inventory) {
        int n = 0;
        for(ItemStack stack : getInputtedDusts(inventory)) {
            n += stack.getAmount();
        }
        return n;
    }

    private static ItemStack tooManyDusts = simpleButton(Material.BARRIER, "§c§lPós demais!", "• Você colocou pós demais, indique", " a quantidade correta para", " reparar o seu item.");
    private static void setOutputItem(Player player, Inventory inventory) {
        Item item = ItemHandler.getItemFromStack(inventory.getItem(itemInputSlot));
        if(item == null) return;

        reorganizeItems(inventory);

        final int remaining = remainingDustsForItem(inventory, item);

        if(remaining == 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1, 0);
            inventory.setItem(outputSlot, item.buildStack());
        } else
            inventory.setItem(outputSlot, null);

        if(remaining < 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0);
            inventory.setItem(outputSlot, tooManyDusts);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        final Player p = (Player) e.getWhoClicked();
        final int slot = e.getSlot();
        final ClickType clickType = e.getClick();
        final Inventory inventory = e.getInventory();

        // idiot-proof
        if(inventory.getType() == InventoryType.ANVIL) {
            AnvilInventory inv = (AnvilInventory)e.getInventory();
            if(Utils.isMineSkyItem(inv.getFirstItem())
                    && (inv.getSecondItem() != null || (e.getCurrentItem() != null && slot == 1))) {
                p.closeInventory();
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                p.sendMessage(Utils.c("&cTentando reparar seu item? Para reparar, você deve utilizar um forjador ou um ferreiro, você pode os encontrar logo na entrada de Edragon (spawn)."));

                p.updateInventory();
            }
            return;
        }

        if (!inventories.containsValue(inventory))
            return;

        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            e.setCancelled(true);
            return;
        }

        if (!inventory.equals(e.getClickedInventory()))
            return;

        if(dustInputSlots.contains(slot) || slot == itemInputSlot) {
            ItemStack itemStack = e.getCurrentItem();

            if (itemStack == null)
                itemStack = e.getCursor();

            Item item = ItemHandler.getItemFromStack(itemStack);
            if (item == null) {
                p.sendMessage(Utils.c("&cPrecisa ser um item que pode ser reparado ou um pó."));
                e.setCancelled(true);
                return;
            }

            if (dustInputSlots.contains(slot)) {
                if (!item.getCategory().getId().equalsIgnoreCase("dusts")) {
                    e.setCancelled(true);
                    return;
                }
            }
        } else
            e.setCancelled(true);

        Bukkit.getScheduler().runTaskLater(MineSkyItems.getInstance(), a -> {
            setOutputItem(p, inventory);
        }, 2);

        p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1f);

        if(clickType != ClickType.LEFT)
            return;

        // Clicou no botão para reparar
        if(repairButtonSlots.contains(slot)) {
            if(p.getInventory().firstEmpty() == -1) {
                p.sendMessage(Utils.c("&cVocê deve ter pelo menos um slot vazio no inventário!"));
                return;
            }

            final ItemStack stack = inventory.getItem(itemInputSlot);
            final Item item = ItemHandler.getItemFromStack(stack);

            if(stack == null) {
                p.sendMessage(Utils.c("&cVocê deve colocar um item para ser reparado no slot vazio!"));
                return;
            }
            if(item == null) {
                p.sendMessage(Utils.c("&cO seu item a ser reparado não é válido!"));
                return;
            }

            final int remainingDusts = remainingDustsForItem(inventory, item);
            if(remainingDusts > 0) {
                p.sendMessage(Utils.c("&cAinda faltam x"+remainingDusts+" pó de item para reparar o seu item."));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                return;
            }
            if(remainingDusts < 0) {
                p.sendMessage(Utils.c("&cVocê colocou pós demais, você deve colocar apenas x"+ItemDustHandler.howManyDustsForItem(item)));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                return;
            }

            getInputtedDusts(inventory).forEach(inventory::removeItem);

            inventory.setItem(itemInputSlot, null);

            ItemStack newItem = item.buildStack();
            for(Enchantment ench : stack.getEnchantments().keySet()) {
                final int level = stack.getEnchantmentLevel(ench);
                newItem.addEnchantment(ench, level);
            }

            p.getInventory().addItem(newItem);

            p.closeInventory();

            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 2f);
        }
    }
}
