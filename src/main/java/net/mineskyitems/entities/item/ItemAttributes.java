package net.mineskyitems.entities.item;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.curves.ItemCurve;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemAttributes {

    private final Item item;
    private final ConfigurationSection section;

    private double damage = 1.0;
    private double speed = 1.0;

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

        double damage = curve.calculateValue(item.getRequiredLevel(), Attribute.ATTACK_DAMAGE);
        double speed = curve.calculateValue(item.getRequiredLevel(), Attribute.ATTACK_SPEED);

        this.damage = damage;
        this.speed = speed;
    }

    public ConfigurationSection getAttributesSection() {
        return section;
    }

    public double getSpeed() {return this.speed;}
    public double getDamage() {return this.damage;}

    public Item getItem() {
        return item;
    }

    public static final NamespacedKey namespace = new NamespacedKey(MineSkyItems.getInstance(), "attr");
    public static final AttributeModifier.Operation defaultOperation = AttributeModifier.Operation.ADD_NUMBER;

    public ItemStack translateAndUpdate(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();

        // Damage
        itemMeta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                new AttributeModifier(namespace, this.getDamage() -1, defaultOperation, EquipmentSlotGroup.HAND));

        // Speed
        // this.getSpeed()-4
        itemMeta.addAttributeModifier(Attribute.ATTACK_SPEED,
                new AttributeModifier(namespace, this.getSpeed() -4, defaultOperation, EquipmentSlotGroup.HAND));

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }
}
