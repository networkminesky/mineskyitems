package net.minesky.events;

import net.minesky.entities.item.Item;
import net.minesky.entities.item.ItemHandler;
import net.minesky.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InteractionEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent e) {
        final Player p = e.getPlayer();

        if (!e.hasItem() || e.getItem() == null)
            return;

        ItemStack itemStack = e.getItem();
        Item item = ItemHandler.getItemFromStack(itemStack);

        if (item != null)
            item.onInteraction(p, itemStack, Utils.convertInteractionType(e.getAction()));
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if(!(e.getDamager() instanceof Player damager))
            return;

        damager.sendTitle(e.getDamage()+"", e.getFinalDamage()+"", 5, 10, 10);
    }

}
