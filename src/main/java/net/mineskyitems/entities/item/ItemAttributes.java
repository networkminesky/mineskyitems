package net.mineskyitems.entities.item;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.curves.CurveHandler;
import net.mineskyitems.entities.curves.ItemCurve;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.w3c.dom.Attr;

import java.util.UUID;

public class ItemAttributes {

    private final Item item;
    private final ConfigurationSection section;

    private double damage = 1.0;
    private double arrowDamage = 1.0;
    private double speed = 1.0;

    private double maxHealth = 0;
    private double attackRange = 1;

    private double attackKnockback = 1;

    public ItemAttributes(Item item) {
        this.item = item;

        if(item.getConfig().contains("attributes"))
            this.section = item.getConfig().getConfigurationSection("attributes");
        else
            this.section = item.getConfig();

        calculateBasedOnLevel();
        //this.damage = section.getDouble("damage", 1.0);
        //this.speed = section.getDouble("speed", 1.0);
    }

    private void calculateBasedOnLevel() {
        final ItemCurve curve = getItem().getCategory().getCurve();

        this.damage = curve.calculateValue(item.getRequiredLevel(), Attribute.ATTACK_DAMAGE);
        this.speed = curve.calculateValue(item.getRequiredLevel(), Attribute.ATTACK_SPEED);

        this.maxHealth = curve.calculateValue(item.getRequiredLevel(), Attribute.MAX_HEALTH);
        this.attackRange = curve.calculateValue(item.getRequiredLevel(), Attribute.ENTITY_INTERACTION_RANGE);

        this.attackKnockback = curve.calculateValue(item.getRequiredLevel(), Attribute.ATTACK_KNOCKBACK);

        this.arrowDamage = curve.calculateValue(item.getRequiredLevel(), CurveHandler.ARROW_DAMAGE_CURVE);
    }

    public ConfigurationSection getAttributesSection() {
        return section;
    }

    public double getArrowDamage() { return this.arrowDamage; }

    public double getSpeed() {return this.speed;}
    public double getDamage() {return this.damage;}

    public double getMaxHealth() {return this.maxHealth;}
    public double getAttackRange() {return this.attackRange;}

    public double getAttackKnockback() {return this.attackKnockback;}

    public Item getItem() {
        return item;
    }

    public static final NamespacedKey namespace = new NamespacedKey(MineSkyItems.getInstance(), "attr");
    public static final AttributeModifier.Operation defaultOperation = AttributeModifier.Operation.ADD_NUMBER;

    public ItemStack translateAndUpdate(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();

        // Damage
        if(this.damage != 0.0) {
            itemMeta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                    new AttributeModifier(namespace, this.damage -1, defaultOperation, EquipmentSlotGroup.HAND));
        }

        // Speed
        // this.getSpeed()-4
        if(this.speed != 0.0) {
            itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED,
                    new AttributeModifier(namespace, this.speed -4, defaultOperation, EquipmentSlotGroup.HAND));
        }

        // Max Health
        if(this.maxHealth != 0.0) {
            itemMeta.addAttributeModifier(Attribute.MAX_HEALTH,
                    new AttributeModifier(new NamespacedKey(MineSkyItems.getInstance(), UUID.randomUUID().toString()), this.maxHealth, defaultOperation,
                            getItem().getMetadata().material().getEquipmentSlot().getGroup()));
        }

        // Attack Range
        if(this.attackRange != 0.0) {
            itemMeta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(namespace, this.attackRange, defaultOperation, EquipmentSlotGroup.HAND));
        }

        // Attack Knockback
        if(this.attackKnockback != 0.0) {
            itemMeta.addAttributeModifier(Attribute.ATTACK_KNOCKBACK,
                    new AttributeModifier(namespace, this.attackKnockback, defaultOperation, EquipmentSlotGroup.HAND));
        }

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }
}
