package net.mineskyitems.events;

import io.papermc.paper.event.player.PlayerPickEntityEvent;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.entities.item.RevisionHandler;
import net.mineskyitems.utils.InteractionType;
import net.mineskyitems.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class InteractionEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemHeld(PlayerItemHeldEvent e) {
        final Player p = e.getPlayer();
        ItemStack stack = p.getInventory().getItem(e.getNewSlot());
        if (RevisionHandler.checkAndApply(p, stack)) {
            p.getInventory().setItem(e.getNewSlot(), stack);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent e) {
        final Player p = e.getPlayer();

        if (!e.hasItem() || e.getItem() == null)
            return;

        ItemStack itemStack = e.getItem();

        if (RevisionHandler.checkAndApply(p, itemStack)) {
            if (e.getHand() == EquipmentSlot.HAND) {
                p.getInventory().setItemInMainHand(itemStack);
            } else if (e.getHand() == EquipmentSlot.OFF_HAND) {
                p.getInventory().setItemInOffHand(itemStack);
            }
        }

        Item item = ItemHandler.getItemFromStack(itemStack);

        if (item != null)
            item.onInteraction(p, itemStack, Utils.convertInteractionType(e.getAction()), e, e.getHand());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        final Player p = e.getPlayer();

        if (e.getMainHandItem() != null && RevisionHandler.checkAndApply(p, e.getMainHandItem())) {
            e.setMainHandItem(e.getMainHandItem());
        }

        if (e.getOffHandItem() != null && RevisionHandler.checkAndApply(p, e.getOffHandItem())) {
            e.setOffHandItem(e.getOffHandItem());
        }

        if (e.getOffHandItem() == null)
            return;

        ItemStack itemStack = e.getOffHandItem();
        Item item = ItemHandler.getItemFromStack(itemStack);

        if (item != null)
            item.onInteraction(p, itemStack, InteractionType.KEY_F, e, EquipmentSlot.OFF_HAND);
    }

    @EventHandler
    public void onPickup(PlayerPickEntityEvent e) {
        if(!e.getPlayer().hasPermission("mineskyitems.itemeditor"))
            return;
        if(e.getPlayer().getGameMode() != GameMode.CREATIVE)
            return;

        final int slot = e.getTargetSlot();

        final Player p = e.getPlayer();
        if(p.isSneaking())
            return;

        p.getScheduler().runDelayed(MineSkyItems.getInstance(), (task) -> {
            final ItemStack stack = p.getInventory().getItem(slot);

            Item item = ItemHandler.getItemFromStack(stack);
            if(item != null) {
                p.playSound(p, Sound.ENTITY_ITEM_PICKUP, 1, 1);
                p.sendTitle("...", "§7Atualizando item", 5, 0, 3);

                p.getInventory().setItem(slot, item.buildStack());
            }
        }, null, 1);
    }

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
        if(e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)
            return;
        if(!(e.getDamager() instanceof Player damager))
            return;
        if(e.isCancelled())
            return;

        ItemStack stack = damager.getInventory().getItemInMainHand();
        Item item = ItemHandler.getItemFromStack(stack);

        if(item == null)
            return;
    }
}