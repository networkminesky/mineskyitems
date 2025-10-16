package net.mineskyitems.scripts;

import com.google.common.primitives.Floats;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.components.EquippableComponent;

public class ArmorStandScript {

    public static void generate(final Location location, final String limit) {
        int amount;
        try {
            amount = Integer.parseInt(limit);
        } catch (Exception ex) {
            amount = 80;
        }

        location.setYaw(-90);
        location.setPitch(0);

        for(int i = 0; i < amount; i++) {
            final int id = i;

            Location loc = location.clone().subtract(0,0,i);

            ItemStack helmet = item(id, EquipmentSlot.HEAD, Material.IRON_HELMET);
            ItemStack chestplate = item(id, EquipmentSlot.CHEST, Material.IRON_CHESTPLATE);
            ItemStack leggings = item(id, EquipmentSlot.LEGS, Material.IRON_LEGGINGS);
            ItemStack boots = item(id, EquipmentSlot.FEET, Material.IRON_BOOTS);

            location.getWorld().spawn(loc, ArmorStand.class, armor -> {
                armor.setCustomNameVisible(true);
                armor.customName(Component.text(id));

                EntityEquipment equipment = armor.getEquipment();

                equipment.setHelmet(helmet);
                equipment.setChestplate(chestplate);
                equipment.setLeggings(leggings);
                equipment.setBoots(boots);
            });
        }
    }

    private static ItemStack item(final int id, final EquipmentSlot slot, final Material material) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();

        CustomModelDataComponent modelDataComponent = itemMeta.getCustomModelDataComponent();
        EquippableComponent equippableComponent = itemMeta.getEquippable();

        modelDataComponent.setFloats(Floats.asList(id));

        equippableComponent.setModel(NamespacedKey.minecraft("part_"+id));
        equippableComponent.setSlot(slot);

        itemMeta.itemName(Component.text("Modelo: "+id));

        itemMeta.setEquippable(equippableComponent);
        itemMeta.setCustomModelDataComponent(modelDataComponent);

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

}
