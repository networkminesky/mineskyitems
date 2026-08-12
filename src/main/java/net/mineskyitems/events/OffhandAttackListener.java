package net.mineskyitems.events;

import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OffhandAttackListener implements Listener {

    private final Map<UUID, Long> offHandCooldowns = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.OFF_HAND) return;

        final Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        final Player p = e.getPlayer();
        final ItemStack stack = e.getItem();
        if (stack == null) return;

        Item customItem = ItemHandler.getItemFromStack(stack);
        if (customItem == null) return;

        if(!customItem.getCategory().isDualHanded())
            return;

        e.setCancelled(true);

        long now = System.currentTimeMillis();
        double attackSpeed = getAttackSpeed(stack);
        long cooldownMs = (long) (1000.0 / attackSpeed);
        long lastAttack = offHandCooldowns.getOrDefault(p.getUniqueId(), 0L);

        if (now - lastAttack < cooldownMs) {
            return;
        }

        double range = getInteractionRange(p, stack);
        RayTraceResult rayTrace = p.getWorld().rayTraceEntities(
                p.getEyeLocation(),
                p.getEyeLocation().getDirection(),
                range,
                0.3,
                entity -> entity instanceof LivingEntity && !entity.equals(p)
        );

        p.swingOffHand();

        if(rayTrace == null)
            return;

        if(rayTrace.getHitBlock() != null)
            return;

        if (rayTrace.getHitEntity() instanceof LivingEntity target) {
            offHandCooldowns.put(p.getUniqueId(), now);

            double damage = getAttackDamage(stack);

            target.damage(damage, p);

            customItem.forceDamageItem(p, stack, 1);

            if (p.getFallDistance() > 0.0F && !p.isOnGround() && !p.isClimbing() && !p.isInWater()) {
                p.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 12, 0.2, 0.2, 0.2, 0.1);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
            } else {
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.0f);
            }
        }
    }

    private double getInteractionRange(Player player, ItemStack stack) {
        double baseRange = player.getGameMode() == GameMode.CREATIVE ? 4.5 : 3.0;
        Attribute rangeAttr = getAttributeSafely("ENTITY_INTERACTION_RANGE", "PLAYER_ENTITY_INTERACTION_RANGE");
        if (rangeAttr != null) {
            AttributeInstance attrInstance = player.getAttribute(rangeAttr);
            if (attrInstance != null) {
                return attrInstance.getValue();
            }
        }
        return baseRange;
    }

    private double getAttackDamage(ItemStack stack) {
        double baseDamage = 1.0;
        ItemMeta meta = stack.getItemMeta();

        Attribute damageAttr = getAttributeSafely("ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE");
        if (meta != null && damageAttr != null && meta.hasAttributeModifiers()) {
            Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(damageAttr);
            if (modifiers != null && !modifiers.isEmpty()) {
                for (AttributeModifier mod : modifiers) {
                    if (mod.getSlot() == null || mod.getSlot() == EquipmentSlot.OFF_HAND || mod.getSlot() == EquipmentSlot.HAND) {
                        baseDamage += mod.getAmount();
                    }
                }
            }
        }
        return baseDamage;
    }

    private double getAttackSpeed(ItemStack stack) {
        double baseSpeed = 4.0;
        ItemMeta meta = stack.getItemMeta();

        Attribute speedAttr = getAttributeSafely("ATTACK_SPEED", "GENERIC_ATTACK_SPEED");
        if (meta != null && speedAttr != null && meta.hasAttributeModifiers()) {
            Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(speedAttr);
            if (modifiers != null && !modifiers.isEmpty()) {
                for (AttributeModifier mod : modifiers) {
                    if (mod.getSlot() == null || mod.getSlot() == EquipmentSlot.OFF_HAND || mod.getSlot() == EquipmentSlot.HAND) {
                        baseSpeed += mod.getAmount();
                    }
                }
            }
        }
        return baseSpeed;
    }

    private Attribute getAttributeSafely(String... names) {
        for (String name : names) {
            try {
                return Attribute.valueOf(name);
            } catch (IllegalArgumentException | NoClassDefFoundError ignored) {}
        }
        return null;
    }
}