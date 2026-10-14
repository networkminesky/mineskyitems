package net.mineskyitems.events;

import io.papermc.paper.event.player.PlayerPickEntityEvent;
import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.entities.item.RevisionHandler;
import net.mineskyitems.entities.shields.ShieldManager;
import net.mineskyitems.utils.DyedColorHelper;
import net.mineskyitems.utils.InteractionType;
import net.mineskyitems.utils.Utils;
import org.bukkit.*;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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

    @EventHandler
    public void shieldDisable(PlayerShieldDisableEvent e) {
        final Player p = e.getPlayer();
        ItemStack shield = null;

        if (p.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
            shield = p.getInventory().getItemInOffHand();
        }
        else if (p.getInventory().getItemInMainHand().getType() == Material.SHIELD) {
            shield = p.getInventory().getItemInMainHand();
        }

        if(shield == null)
            return;

        Item item = ItemHandler.getItemFromStack(shield);
        if(item == null)
            return;

        if(item.getCategory().getShield() != null
                && item.getCategory().getShield().equalsIgnoreCase("tank")) {
            e.setCancelled(true);
            p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 0.8f, 0.9f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if(e.getCursor() == null)
            return;

        final Player player = (Player) e.getWhoClicked();
        ItemStack targetStack = e.getCurrentItem();

        if(targetStack == null)
            return;
        ItemMeta meta = targetStack.getItemMeta();
        if(targetStack.getType().isAir() || meta == null)
            return;

        Item customItem = ItemHandler.getItemFromStack(targetStack);

        final Item customCursor = ItemHandler.getItemFromStack(e.getCursor());
        if (customCursor != null && customCursor.getCategory().isSkin()) {
            final Item.SkinMetadata skinMetadata = customCursor.getSkinMetadata();
            if(skinMetadata == null)
                return;
            if(!meta.hasEquippable()) {
                if(!(targetStack.isSimilar(e.getCursor()))) {
                    player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.8f, 1);
                    player.sendMessage(Component.text("Este item não pode ter skins.").color(NamedTextColor.RED));
                }
                return;
            }

            final EquipmentSlot equipSlot = meta.getEquippable().getSlot();
            e.setCancelled(true);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if(pdc.has(ItemHandler.SKIN, PersistentDataType.STRING)) {
                player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.8f, 1);
                player.sendMessage(Component.text("Este item já possui uma skin aplicada. Remova skins usando um balde de água.").color(NamedTextColor.RED));
                return;
            }

            String item = "";
            String equippable = "";
            int model = -1;

            ItemStack cursor = e.getCursor();
            cursor.setAmount(cursor.getAmount() - 1);

            if(skinMetadata.DEFAULT() != null) {
                item = skinMetadata.DEFAULT().item();
                equippable = skinMetadata.DEFAULT().equippable();
                model = skinMetadata.DEFAULT().model();
            }

            if(equipSlot == EquipmentSlot.HEAD && skinMetadata.HEAD() != null) {
                model = skinMetadata.HEAD().model();
                item = skinMetadata.HEAD().item();
                equippable = skinMetadata.HEAD().equippable();
            }
            else if(equipSlot == EquipmentSlot.CHEST && skinMetadata.CHEST() != null) {
                model = skinMetadata.CHEST().model();
                item = skinMetadata.CHEST().item();
                equippable = skinMetadata.CHEST().equippable();
            }
            else if(equipSlot == EquipmentSlot.LEGS && skinMetadata.LEGS() != null) {
                model = skinMetadata.LEGS().model();
                item = skinMetadata.LEGS().item();
                equippable = skinMetadata.LEGS().equippable();
            }
            else if(equipSlot == EquipmentSlot.FEET && skinMetadata.FEET() != null) {
                model = skinMetadata.FEET().model();
                item = skinMetadata.FEET().item();
                equippable = skinMetadata.FEET().equippable();
            }

            if(model != -1) {
                meta.setCustomModelData(model);
            }

            EquippableComponent component = meta.getEquippable();
            if(!equippable.isEmpty()) {
                if(equippable.equals("null"))
                    component.setModel(null);
                else
                    component.setModel(NamespacedKey.minecraft(equippable));
            }
            meta.setEquippable(component);

            pdc.set(ItemHandler.SKIN, PersistentDataType.STRING, customCursor.getId());
            targetStack.setItemMeta(meta);

            if(!item.isEmpty()) {
                Material material = Material.matchMaterial(item);
                if(material != null) {
                    targetStack = targetStack.withType(material);
                }
            }

            if (customItem != null) {
                targetStack.lore(customItem.getCategory().getTooltip().getFormattedLore(customItem, targetStack));
            }

            final Color skinDyedColor = DyedColorHelper.resolveColor(cursor, customCursor);
            if (skinDyedColor != null) {
                DyedColorHelper.applyColor(targetStack, skinDyedColor, null);
            }

            e.setCurrentItem(targetStack);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.4f);
            player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.8f);

            return;
        }

        if (customItem != null
                && e.getCursor().getType() == Material.WATER_BUCKET
                && meta.getPersistentDataContainer().has(ItemHandler.SKIN, PersistentDataType.STRING)) {
            e.setCancelled(true);

            meta.getPersistentDataContainer().remove(ItemHandler.SKIN);
            targetStack.setItemMeta(meta);

            if (customItem.getDyedColor() != null) {
                DyedColorHelper.applyFromItemConfig(targetStack, customItem);
            } else {
                DyedColorHelper.clearColor(targetStack);
            }

            RevisionHandler.startRevision(targetStack, customItem);
            e.setCurrentItem(targetStack);
            e.setCursor(new ItemStack(Material.BUCKET));

            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.6f, 1.2f);
            return;
        }

        if (customItem != null && DyedColorHelper.isDyeMaterial(e.getCursor().getType())) {
            final DyeColor dyeColor = DyedColorHelper.getDyeColor(e.getCursor().getType());
            if (dyeColor != null && DyedColorHelper.canBeDyed(customItem, targetStack)) {
                e.setCancelled(true);

                ItemStack cursor = e.getCursor();
                cursor.setAmount(cursor.getAmount() - 1);

                DyedColorHelper.applyDye(targetStack, customItem, dyeColor);
                targetStack.lore(customItem.getCategory().getTooltip().getFormattedLore(customItem, targetStack));

                e.setCurrentItem(targetStack);
                player.playSound(player.getLocation(), Sound.ITEM_DYE_USE, 1.0f, 1.0f);
                return;
            }
        }

        if(customItem == null)
            return;

        // Totem Shield applier
        if (e.getCursor().getType() == Material.TOTEM_OF_UNDYING) {
            int maxTotems = customItem.getItemAttributes().getMaxTotems();
            if (maxTotems <= 0) {
                return;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            int currentTotems = pdc.getOrDefault(ShieldManager.TOTEM_KEY, PersistentDataType.INTEGER, 0);

            if (currentTotems >= maxTotems) {
                return;
            }

            e.setCancelled(true);

            ItemStack cursor = e.getCursor();
            cursor.setAmount(cursor.getAmount() - 1);

            currentTotems++;
            pdc.set(ShieldManager.TOTEM_KEY, PersistentDataType.INTEGER, currentTotems);
            targetStack.setItemMeta(meta);

            targetStack.lore(customItem.getCategory().getTooltip().getFormattedLore(customItem, targetStack));
            e.setCurrentItem(targetStack);

            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 1.4f);
            player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        final Player p = e.getPlayer();

        if (RevisionHandler.checkAndApply(p, e.getMainHandItem())) {
            e.setMainHandItem(e.getMainHandItem());
        }

        if (RevisionHandler.checkAndApply(p, e.getOffHandItem())) {
            e.setOffHandItem(e.getOffHandItem());
        }

        ItemStack mainHandStack = e.getOffHandItem();
        Item mainHandItem = ItemHandler.getItemFromStack(mainHandStack);

        ItemStack offHandStack = e.getMainHandItem();
        Item offHandItem = ItemHandler.getItemFromStack(offHandStack);

        if (offHandItem != null && offHandItem.getCategory().getShield() != null) {
            e.setCancelled(true);
            offHandItem.onInteraction(p, offHandStack, InteractionType.KEY_F, e, EquipmentSlot.OFF_HAND);
            p.getScheduler().run(MineSkyItems.getInstance(), t -> p.updateInventory(), null);
            return;
        }

        if (mainHandItem != null && mainHandItem.getCategory().getShield() != null) {
            e.setCancelled(true);
            mainHandItem.onInteraction(p, mainHandStack, InteractionType.KEY_F, e, EquipmentSlot.HAND);
            p.getScheduler().run(MineSkyItems.getInstance(), t -> p.updateInventory(), null);
            return;
        }

        if (mainHandItem != null) {
            mainHandItem.onInteraction(p, mainHandStack, InteractionType.KEY_F, e, EquipmentSlot.HAND);
            if (e.isCancelled()) {
                p.getScheduler().run(MineSkyItems.getInstance(), t -> p.updateInventory(), null);
                return;
            }
        }

        if (offHandItem != null) {
            offHandItem.onInteraction(p, offHandStack, InteractionType.KEY_F, e, EquipmentSlot.OFF_HAND);
            if (e.isCancelled()) {
                p.getScheduler().run(MineSkyItems.getInstance(), t -> p.updateInventory(), null);
            }
        }
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