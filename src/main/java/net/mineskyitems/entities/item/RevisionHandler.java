package net.mineskyitems.entities.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.mineskyitems.MineSkyItems;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class RevisionHandler {

    public static final NamespacedKey REVISION_KEY = new NamespacedKey("mineskyitems", "revision");

    public static int getRevision(final ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return 0;
        }
        return stack.getItemMeta().getPersistentDataContainer().getOrDefault(REVISION_KEY, PersistentDataType.INTEGER, 0);
    }

    public static boolean shouldRevision(final ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        Item item = ItemHandler.getItemFromStack(stack);
        if (item != null) {
            final int revision = getRevision(stack);
            return item.getRevision() > revision;
        }
        return false;
    }

    public static boolean checkAndApply(final Player player, final ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        if (shouldRevision(stack)) {
            Item item = ItemHandler.getItemFromStack(stack);
            if (item != null) {
                startRevision(stack, item);
                return true;
            }
        }
        return false;
    }

    public static void startRevision(final ItemStack stack, final Item item) {
        if (stack == null || item == null) {
            return;
        }

        if (stack.getType() != item.getMetadata().material()) {
            stack.setType(item.getMetadata().material());
        }

        if (!item.getCategory().isNoAttributes()) {
            item.getItemAttributes().translateAndUpdate(stack);
        }

        ItemMeta im = stack.getItemMeta();
        PersistentDataContainer container = im.getPersistentDataContainer();

        container.set(ItemHandler.LEVEL_NAMESPACE, PersistentDataType.INTEGER, item.getRequiredLevel());
        container.set(ItemHandler.CLASS_NAMESPACE, PersistentDataType.LIST.strings(), item.getRequiredClasses());
        container.set(MineSkyItems.NAMESPACED_KEY, PersistentDataType.STRING, item.getId());
        container.set(REVISION_KEY, PersistentDataType.INTEGER, item.getRevision());

        if (item.getCategory().isDoNotStack()) {
            if (!container.has(NamespacedKey.minecraft("unique"), PersistentDataType.STRING)) {
                container.set(NamespacedKey.minecraft("unique"), PersistentDataType.STRING, UUID.randomUUID().toString());
            }
            im.setMaxStackSize(1);
        }

        if (item.getItemRarity().hasTooltip()) {
            im.setTooltipStyle(NamespacedKey.minecraft(item.getItemRarity().getTooltip()));
        } else {
            im.setTooltipStyle(null);
        }

        if (!item.getCategory().isVanillaDurability()) {
            im.setUnbreakable(true);
            im.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            int max = item.getMaxDurability();
            int current = container.getOrDefault(Item.ITEM_DURABILITY, PersistentDataType.INTEGER, max);
            container.set(Item.ITEM_DURABILITY, PersistentDataType.INTEGER, Math.min(current, max));
        }

        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        if (!im.hasCustomName()) {
            Component itemName = Component.text(item.getMetadata().displayName()).color(item.getItemRarity().getTextColor());
            im.itemName(itemName);
        }

        im.setCustomModelData(item.getMetadata().modelData());

        if (item.getCategory().isTool()) {
            ToolComponent toolComponent = im.getTool();
            toolComponent.getRules().clear();

            final float toolSpeed = item.getItemAttributes().getToolSpeed();
            final float defaultToolSpeed = item.getItemAttributes().getDefaultToolSpeed();
            toolComponent.setDefaultMiningSpeed(defaultToolSpeed);
            toolComponent.setDamagePerBlock(1);

            switch (item.getCategory().getTool()) {
                case "PICKAXE" -> toolComponent.addRule(Tag.MINEABLE_PICKAXE, toolSpeed, true);
                case "AXE" -> toolComponent.addRule(Tag.MINEABLE_AXE, toolSpeed, true);
                case "HOE" -> toolComponent.addRule(Tag.MINEABLE_HOE, toolSpeed, true);
                case "SHOVEL" -> toolComponent.addRule(Tag.MINEABLE_SHOVEL, toolSpeed, true);
                case "PAXEL" -> {
                    toolComponent.addRule(Tag.MINEABLE_PICKAXE, toolSpeed, true);
                    toolComponent.addRule(Tag.MINEABLE_AXE, toolSpeed, true);
                    toolComponent.addRule(Tag.MINEABLE_SHOVEL, toolSpeed, true);
                }
            }
            im.setTool(toolComponent);
        }

        if (item.getCategory().getType().equalsIgnoreCase("armor") && !item.isNoAutoArmor()) {
            EquippableComponent equippableComponent = im.getEquippable();
            equippableComponent.setModel(NamespacedKey.minecraft("part_" + item.getMetadata().modelData()));
            equippableComponent.setSlot(item.getMetadata().material().getEquipmentSlot());
            im.setEquippable(equippableComponent);
        }

        stack.setItemMeta(im);

        if (item.getCategory().isFood() && item.getFoodMetadata() != null) {
            boolean drink = item.getCategory().getFood().equalsIgnoreCase("drink");

            Consumable consum = Consumable.consumable()
                    .hasConsumeParticles(true)
                    .consumeSeconds(item.getFoodMetadata().consumeSeconds())
                    .animation(drink ? ItemUseAnimation.DRINK : ItemUseAnimation.EAT)
                    .sound(Key.key("minecraft", (drink) ? "entity.generic.drink" : "entity.generic.eat"))
                    .build();
            stack.setData(DataComponentTypes.CONSUMABLE, consum);

            FoodProperties foodProp = FoodProperties.food()
                    .nutrition(item.getFoodMetadata().nutrition())
                    .saturation(item.getFoodMetadata().saturation())
                    .canAlwaysEat(false)
                    .build();
            stack.setData(DataComponentTypes.FOOD, foodProp);
        }

        if (!item.getCategory().isNoAttributes()) {
            if (item.getCategory().isVanillaDurability()) {
                stack.setData(DataComponentTypes.MAX_STACK_SIZE, 1);

                int currentMax = stack.hasData(DataComponentTypes.MAX_DAMAGE)
                        ? stack.getData(DataComponentTypes.MAX_DAMAGE)
                        : stack.getType().getMaxDurability();
                int currentDamage = stack.hasData(DataComponentTypes.DAMAGE)
                        ? stack.getData(DataComponentTypes.DAMAGE)
                        : 0;

                int remaining = Math.max(0, currentMax - currentDamage);
                int newMax = item.getMaxDurability();
                int newDamage = Math.max(0, newMax - remaining);

                stack.setData(DataComponentTypes.MAX_DAMAGE, newMax);
                stack.setData(DataComponentTypes.DAMAGE, newDamage);
            } else {
                stack.resetData(DataComponentTypes.MAX_DAMAGE);
                stack.resetData(DataComponentTypes.DAMAGE);
            }
        }

        ItemMeta finalMeta = stack.getItemMeta();
        finalMeta.lore(item.getCategory().getTooltip().getFormattedLore(item, stack));
        stack.setItemMeta(finalMeta);
    }
}