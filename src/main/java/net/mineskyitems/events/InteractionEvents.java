package net.mineskyitems.events;

import io.papermc.paper.event.player.PlayerPickEntityEvent;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.utils.InteractionType;
import net.mineskyitems.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
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
        if(e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)
            return;
        if(!(e.getDamager() instanceof Player damager))
            return;
        if(e.isCancelled())
            return;

        //final int playerLevel = PlayerData.get(damager).getLevel();

        ItemStack stack = damager.getInventory().getItemInMainHand();
        Item item = ItemHandler.getItemFromStack(stack);

        if(item == null)
            return;

        //if(item.getCategory().getType().equalsIgnoreCase("melee")) {
        //    item.onItemUse(damager, stack, e);
        //}
    }
}
