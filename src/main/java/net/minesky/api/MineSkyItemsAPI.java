package net.minesky.api;

import net.minesky.handler.Item;
import net.minesky.handler.ItemBuilder;
import net.minesky.handler.categories.CategoryHandler;
import net.minesky.utils.InteractionType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class MineSkyItemsAPI {

    public void test() {

        ItemBuilder builder = new ItemBuilder(null)
                .displayName("Item teste")
                .lore("Item incrivel", "e muito foda!")
                .itemSkills(
                        List.of(new Item.ItemSkill("1", InteractionType.LEFT_CLICK, 20,"mythicskill"))
                )
                .customModel(20);

        Item item = builder.build();

        forceInteraction(null, item);

    }

    public void forceInteraction(Player player, Item item) {
        item.onInteraction(player, null, InteractionType.RIGHT_CLICK);
    }

}
