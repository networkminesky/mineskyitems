package net.mineskyitems.gui.rotatingshop.armors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.categories.Category;
import net.mineskyitems.entities.categories.CategoryHandler;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.gui.rotatingshop.RotatingItemsGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class RotatingArmorsGUI {

    //            CLASS NAME| INVENTORY
    public static Map<String, Inventory> inventoryMap = new HashMap<>();

    private static List<Item> getLevelSortedItems(final String categoryName, final String className) {
        return CategoryHandler.getCategoryById(categoryName).getAllItems().stream()
                .filter(it -> it.getRequiredClasses().contains(className))
                .filter(it -> it.getRequiredLevel() > 1) // excluindo a armor inicial
                .sorted(Comparator.comparingInt(Item::getRequiredLevel))
                .toList();
    }

    public static void openShop(final Player player, final String playerClass) {
        if(!inventoryMap.containsKey(playerClass)) {
            player.sendMessage(Component.text("Nenhuma loja encontrada.").color(NamedTextColor.RED));
            return;
        }

        player.openInventory(inventoryMap.get(playerClass));
    }

    public static final int ARMOR_AMOUNT = 12; // excluindo a armor inicial
    public static final int SPECIAL_ARMOR = 13;

    public static void createNewShop(final String playerClass) {
        Inventory inv = Bukkit.createInventory(null, 54, "[{\"text\":\"VIX\",\"font\":\"guis\",\"color\":\"white\"},{\"text\":\"Armaduras de "+playerClass+"\",\"font\":\"default\",\"color\":\"black\"}]");

        final List<Item> helmetList = getLevelSortedItems("capacetes", playerClass);
        final List<Item> chestplateList = getLevelSortedItems("peitorais", playerClass);
        final List<Item> leggingsList = getLevelSortedItems("calcas", playerClass);
        final List<Item> bootsList = getLevelSortedItems("botas", playerClass);

        // 0 a 13
        int currentRow = 0;
        int columnOffset = 0;
        for(int i = 0; i < ARMOR_AMOUNT; i++) {
            ItemStack helmet = format(helmetList.get(i));
            ItemStack chest = format(chestplateList.get(i));
            ItemStack legs = format(leggingsList.get(i));
            ItemStack boots = format(bootsList.get(i));

            inv.setItem(currentRow + columnOffset, helmet);
            inv.setItem(currentRow + columnOffset + 1, chest);
            inv.setItem(currentRow + columnOffset + 2, legs);
            inv.setItem(currentRow + columnOffset + 3, boots);

            currentRow = currentRow + 9;

            if(currentRow >= 54) {
                columnOffset = 5;
                currentRow = 0;
            }
        }

        ItemStack divider = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta im = divider.getItemMeta();
        im.setHideTooltip(true);
        im.displayName(Component.empty());

        divider.setItemMeta(im);
        for(int i = 0; i < 6; i++) {
            final int slot = 4 + (i * 9);
            inv.setItem(slot, divider);
        }

        inventoryMap.put(playerClass, inv);
    }

    public static int exponentialValue(int level) {
        int multiplier = level >= 86 ? 15 : 9;

        return (int) (multiplier * Math.pow(level, 1.5));
    }

    public static ItemStack format(Item item) {
        final int level = item.getRequiredLevel();

        final int price = exponentialValue(level)/2;

        ItemStack itemStack = item.buildStack();
        ItemMeta itemMeta = itemStack.getItemMeta();

        //final TextColor color = TextColor.color(3, 252, 232);
        final TextColor color = TextColor.color(252, 177, 3);

        List<Component> priceLore = new ArrayList<>(Arrays.asList(
                Component.text("Preço: $"+price)
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true)
                        .color(color),
                Component.text("Clique para comprar essa peça.")
                        .decoration(TextDecoration.ITALIC, false)
                        .color(color),
                Component.text(" ")
        ));

        final List<Component> itemLore = itemMeta.lore();
        if(itemLore != null) {
            priceLore.addAll(itemLore);
        }

        itemMeta.getPersistentDataContainer().set(RotatingItemsGUI.PRICE_NAMESPACE, PersistentDataType.INTEGER, price);

        itemMeta.lore(priceLore);

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

}
