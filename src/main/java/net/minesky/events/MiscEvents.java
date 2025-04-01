package net.minesky.events;

import net.minesky.entities.item.ItemHandler;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class MiscEvents implements Listener {

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if(e.isCancelled()) return;

        final Item drop = e.getItemDrop();

        final net.minesky.entities.item.Item item = ItemHandler.getItemFromStack(drop.getItemStack());
        if(item == null) return;

        if(item.getItemRarity().shouldHaveGlowing())
            drop.setGlowing(true);

        drop.setCustomNameVisible(true);
        drop.customName(drop.getItemStack().displayName());

    }

}
