package net.mineskyitems.hook;

import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.core.drops.Drop;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class MythicHook implements Listener {

    @EventHandler
    public void onMythicDropLoad(MythicDropLoadEvent event) {
        if (event.getDropName().equalsIgnoreCase("mineskyitem")) {
            event.register(new MineSkyDrop(event.getConfig()));
        }
    }

    public static class MineSkyDrop extends Drop implements IItemDrop {
        private final String itemId;

        public MineSkyDrop(MythicLineConfig config) {
            super(config.getLine(), config);
            this.itemId = config.getString(new String[]{"id", "i"}, "espada");
        }

        @Override
        public BukkitItemStack getDrop(DropMetadata metadata, double amount) {
            Item item = ItemHandler.getItem(this.itemId);
            return (item == null) ? null : BukkitAdapter.adapt(item.buildStack());
        }
    }
}