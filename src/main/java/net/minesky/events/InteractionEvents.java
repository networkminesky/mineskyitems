package net.minesky.events;

import net.minesky.entities.item.Item;
import net.minesky.entities.item.ItemHandler;
import net.minesky.utils.InteractionType;
import net.minesky.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class InteractionEvents implements Listener {

    // Right and Left click
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent e) {
        final Player p = e.getPlayer();

        if (!e.hasItem() || e.getItem() == null)
            return;

        ItemStack itemStack = e.getItem();
        Item item = ItemHandler.getItemFromStack(itemStack);

        if (item != null)
            item.onInteraction(p, itemStack, Utils.convertInteractionType(e.getAction()), e, e.getHand());
    }

    // Key F
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        final Player p = e.getPlayer();

        if (e.getOffHandItem() == null)
            return;

        ItemStack itemStack = e.getOffHandItem();
        Item item = ItemHandler.getItemFromStack(itemStack);

        if (item != null)
            item.onInteraction(p, itemStack, InteractionType.KEY_F, e, EquipmentSlot.OFF_HAND);
    }

    // Key Q
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        final Player p = e.getPlayer();

        ItemStack itemStack = e.getItemDrop().getItemStack();
        Item item = ItemHandler.getItemFromStack(itemStack);

        if (item != null)
            item.onInteraction(p, itemStack, Utils.convertInteractionType(ClickType.DROP), e, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if(e.isCancelled()) return;
        final Player p = e.getPlayer();

        ItemStack stack = p.getInventory().getItemInMainHand();
        Item item = ItemHandler.getItemFromStack(stack);
        if(item != null) {
            item.onItemUse(p, stack, e);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if(!(e.getDamager() instanceof Player damager))
            return;

        damager.sendTitle(Utils.format(e.getDamage()), e.getFinalDamage()+"", 5, 10, 10);

        ItemStack stack = damager.getInventory().getItemInMainHand();
        Item item = ItemHandler.getItemFromStack(stack);
        if(item != null) {
            item.onItemUse(damager, stack, e);
        }
    }

}
