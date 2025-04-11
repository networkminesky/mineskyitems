package net.minesky.api;

import net.minesky.MineSkyItems;
import net.minesky.entities.categories.CategoryHandler;
import net.minesky.entities.item.Item;
import net.minesky.entities.ItemBuilder;
import net.minesky.entities.item.ItemSkill;
import net.minesky.utils.InteractionType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class MineSkyItemsAPI {

    public static boolean isMineSkyItem(ItemStack itemStack) {
        if(itemStack == null)
            return false;
        if(!itemStack.hasItemMeta())
            return false;
        try {
            return itemStack.getItemMeta().getPersistentDataContainer().has(MineSkyItems.NAMESPACED_KEY, PersistentDataType.STRING);
        } catch(Exception ignored) {}
        return false;
    }

    public void test() {

        ItemBuilder builder = new ItemBuilder(CategoryHandler.categories.iterator().next())
                .displayName("Item teste")
                .lore("Item incrivel", "e muito foda!")
                .itemSkills(
                        List.of(new ItemSkill("1", InteractionType.LEFT_CLICK, 20,"mythicskill"))
                )
                .customModel(20);

        Item item = builder.build();

        forceInteraction(null, item);

    }

    public void forceInteraction(Player player, Item item) {
        item.onInteraction(player, null, InteractionType.RIGHT_CLICK, null);
    }

}
