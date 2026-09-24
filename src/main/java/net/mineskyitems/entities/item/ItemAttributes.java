package net.mineskyitems.entities.item;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.curves.CurveHandler;
import net.mineskyitems.entities.curves.ItemCurve;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class ItemAttributes {

    private final Item item;
    private final ConfigurationSection section;

    private double damage = 1.0;
    private double skillDamage = 0.0;
    private double arrowDamage = 1.0;

    private double speed = 1.0;

    private double maxHealth = 0;
    private double attackRange = 1;

    private double armor = 0;
    private double armorToughness = 0;

    private double knockbackResistance = 0.0;

    private float toolSpeed = 1f;
    private float defaultToolSpeed = 1f;

    private double attackKnockback = 1.0;

    public static final AttributeModifier.Operation defaultOperation = AttributeModifier.Operation.ADD_NUMBER;

    public ItemAttributes(Item item) {
        this.item = item;

        if(item.getConfig().contains("attributes"))
            this.section = item.getConfig().getConfigurationSection("attributes");
        else
            this.section = item.getConfig();

        calculateBasedOnLevel();
    }

    private void calculateBasedOnLevel() {
        final ItemCurve curve = getItem().getCategory().getCurve();
        final int level = item.getRequiredLevel();

        this.damage = curve.calculateValue(this.item, level, Attribute.ATTACK_DAMAGE);
        this.skillDamage = curve.calculateValue(this.item, level, CurveHandler.SKILL_DAMAGE_CURVE);

        this.speed = curve.calculateValue(this.item, level, Attribute.ATTACK_SPEED);

        this.maxHealth = curve.calculateValue(this.item, level, Attribute.MAX_HEALTH);
        this.attackRange = curve.calculateValue(this.item, level, Attribute.ENTITY_INTERACTION_RANGE);

        this.armor = curve.calculateValue(this.item, level, Attribute.ARMOR);
        this.armorToughness = curve.calculateValue(this.item, level, Attribute.ARMOR_TOUGHNESS);

        this.knockbackResistance = (float)curve.calculateValue(this.item, level, Attribute.KNOCKBACK_RESISTANCE);

        this.toolSpeed = (float)curve.calculateValue(this.item, level, CurveHandler.TOOL_SPEED);
        this.defaultToolSpeed = (float)curve.calculateValue(this.item, level, CurveHandler.DEFAULT_TOOL_SPEED);

        this.attackKnockback = curve.calculateValue(this.item, level, Attribute.ATTACK_KNOCKBACK);

        this.arrowDamage = curve.calculateValue(this.item, level, CurveHandler.ARROW_DAMAGE_CURVE);
    }

    public ConfigurationSection getAttributesSection() {
        return section;
    }

    public float getDefaultToolSpeed() {
        return defaultToolSpeed;
    }

    public float getToolSpeed() {
        return toolSpeed;
    }

    public double getSpeed() {
        return this.speed;
    }

    public double getDamage() {
        return this.damage;
    }

    public double getSkillDamage() {
        return this.skillDamage;
    }

    public double getArrowDamage() {
        return this.arrowDamage;
    }

    public double getMaxHealth() {
        return this.maxHealth;
    }

    public double getAttackRange() {
        return this.attackRange;
    }

    public double getArmorToughness() {
        return this.armorToughness;
    }

    public double getArmor() {
        return this.armor;
    }

    public double getKnockbackResistance() {
        return knockbackResistance;
    }

    public double getAttackKnockback() {
        return this.attackKnockback;
    }

    public Item getItem() {
        return item;
    }

    private NamespacedKey randomKey() {
        return new NamespacedKey(MineSkyItems.getInstance(), UUID.randomUUID().toString());
    }

    public ItemStack translateAndUpdate(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }

        itemMeta.setAttributeModifiers(null);

        EquipmentSlot slot = getItem().getMetadata().material().getEquipmentSlot();
        EquipmentSlotGroup group = slot != null ? slot.getGroup() : EquipmentSlotGroup.HAND;

        if (this.damage != 0.0) {
            itemMeta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                    new AttributeModifier(randomKey(), this.damage - 1, defaultOperation, EquipmentSlotGroup.HAND));
        }

        if (this.speed != 0.0) {
            itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED,
                    new AttributeModifier(randomKey(), this.speed - 4, defaultOperation, EquipmentSlotGroup.HAND));
        }

        if (this.maxHealth != 0.0) {
            itemMeta.addAttributeModifier(Attribute.MAX_HEALTH,
                    new AttributeModifier(randomKey(), this.maxHealth, defaultOperation, group));
        }

        if (this.armor != 0.0) {
            itemMeta.addAttributeModifier(Attribute.ARMOR,
                    new AttributeModifier(randomKey(), this.armor, defaultOperation, group));
        }

        if (this.armorToughness != 0.0) {
            itemMeta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS,
                    new AttributeModifier(randomKey(), this.armorToughness, defaultOperation, group));
        }

        if (this.knockbackResistance != 0.0) {
            itemMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(randomKey(), this.knockbackResistance, defaultOperation, group));
        }

        if (this.attackRange != 0.0) {
            itemMeta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(randomKey(), this.attackRange, defaultOperation, EquipmentSlotGroup.HAND));
        }

        if (this.attackKnockback != 1.0) {
            itemMeta.addAttributeModifier(Attribute.ATTACK_KNOCKBACK,
                    new AttributeModifier(randomKey(), this.attackKnockback, defaultOperation, EquipmentSlotGroup.HAND));
        }

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}