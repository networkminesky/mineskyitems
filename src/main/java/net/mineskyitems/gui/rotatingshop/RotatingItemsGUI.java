package net.mineskyitems.gui.rotatingshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.gui.rotatingshop.armors.RotatingArmorsGUI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class RotatingItemsGUI implements Listener {

    public static final NamespacedKey PRICE_NAMESPACE = new NamespacedKey("mineskyitems", "price");

    //            CLASS NAME| INVENTORY
    public static Map<String, Inventory> inventoryMap = new HashMap<>();

    public static void openShop(final Player player, final String playerClass) {
        if(!inventoryMap.containsKey(playerClass)) {
            player.sendMessage(Component.text("Nenhuma loja encontrada.").color(NamedTextColor.RED));
            return;
        }

        player.openInventory(inventoryMap.get(playerClass));
    }

    public static boolean isNotArmor(Item item) {
        return !item.getCategory().getType().equalsIgnoreCase("armor");
    }

    public static void createNewShop(final String playerClass) {
        Inventory inv = Bukkit.createInventory(null, 54, "[{\"text\":\"VJX\",\"font\":\"guis\",\"color\":\"white\"},{\"text\":\"Loja do "+playerClass+"\",\"font\":\"default\",\"color\":\"black\"}]");

        final List<Item> itemList = new ArrayList<>(
                ItemHandler.getAllItems().stream()
                        .filter(it -> it.getRequiredClasses().contains(playerClass))
                        .filter(RotatingItemsGUI::isNotArmor)
                        .toList()
        );

        List<Item> lessThanLevel15 = new ArrayList<>(
                itemList.stream()
                        .filter(it -> it.getRequiredLevel() <= 20)
                        .filter(RotatingItemsGUI::isNotArmor)
                        .toList()
        );
        Collections.shuffle(lessThanLevel15);

        List<Item> lessThanLevel85 = new ArrayList<>(
                itemList.stream()
                        .filter(it -> it.getRequiredLevel() > 20 && it.getRequiredLevel() <= 85)
                        .filter(RotatingItemsGUI::isNotArmor)
                        .toList()
        );
        Collections.shuffle(lessThanLevel85);

        List<Item> legendaries = new ArrayList<>(
                itemList.stream()
                        .filter(it -> it.getRequiredLevel() >= 86)
                        .filter(RotatingItemsGUI::isNotArmor)
                        .toList()
        );
        Collections.shuffle(legendaries);

        int hotbar_level15 = 10;
        for(Item it : lessThanLevel15) {
            if(hotbar_level15 >= 17)
                break;
            inv.setItem(hotbar_level15, format(it));
            hotbar_level15++;
        }

        int hotbar_level85 = 19;
        for(Item it : lessThanLevel85) {
            if(hotbar_level85 == 26)
                hotbar_level85 = 28;
            if(hotbar_level85 == 35)
                break;
            inv.setItem(hotbar_level85, format(it));
            hotbar_level85++;
        }

        int hotbar_legendaries = 39;
        for(Item it : legendaries) {
            if(hotbar_legendaries == 42)
                break;
            inv.setItem(hotbar_legendaries, format(it));
            hotbar_legendaries++;
        }

        inventoryMap.put(playerClass, inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if(!inventoryMap.containsValue(e.getInventory())
        && !RotatingArmorsGUI.inventoryMap.containsValue(e.getInventory()))
            return;

        e.setCancelled(true);

        ItemStack itemStack = e.getCurrentItem();

        if(itemStack == null || !itemStack.hasItemMeta())
            return;

        final Item item = ItemHandler.getItemFromStack(itemStack);
        if(item == null) return;

        ItemMeta itemMeta = itemStack.getItemMeta();

        int price = itemMeta.getPersistentDataContainer().getOrDefault(PRICE_NAMESPACE, PersistentDataType.INTEGER, -1);
        if(price == -1) return;

        if(e.getWhoClicked().getInventory().firstEmpty() == -1) {
            e.getWhoClicked().sendMessage(Component.text("Você precisa ter um espaço livre no inventário.")
                    .color(NamedTextColor.RED));
            return;
        }
        final Player player = (Player)e.getWhoClicked();

        if(!MineSkyItems.economy.has(player, price)) {
            player.sendMessage(Component.text("Você não possui skyes o suficiente para comprar esse item.")
                    .color(NamedTextColor.RED));
            return;
        }

        MineSkyItems.economy.withdrawPlayer(player, price);

        player.playSound(player.getLocation(), "entity.villager.celebrate", 1, 1);
        player.getInventory().addItem(item.buildStack());

        player.sendMessage(Component.text("Você comprou o item '"+item.getMetadata().displayName()+"' com sucesso por "+price+" skyes!")
                .color(NamedTextColor.GREEN)
        );
    }

    public static int exponentialValue(int level) {
        int multiplier = level >= 86 ? 15 : 9;

        return (int) (multiplier * Math.pow(level, 1.5));
    }

    public static ItemStack format(Item item) {
        final int level = item.getRequiredLevel();

        final int price = exponentialValue(level);

        ItemStack itemStack = item.buildStack();
        ItemMeta itemMeta = itemStack.getItemMeta();

        //final TextColor color = TextColor.color(3, 252, 232);
        final TextColor color = TextColor.color(252, 177, 3);

        List<Component> priceLore = new ArrayList<>(Arrays.asList(
                Component.text("Preço: $"+price)
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true)
                        .color(color),
                Component.text("Clique para comprar esse item.")
                        .decoration(TextDecoration.ITALIC, false)
                        .color(color),
                Component.text(" ")
        ));

        final List<Component> itemLore = itemMeta.lore();
        if(itemLore != null) {
            priceLore.addAll(itemLore);
        }

        itemMeta.getPersistentDataContainer().set(PRICE_NAMESPACE, PersistentDataType.INTEGER, price);

        itemMeta.lore(priceLore);

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }
}
