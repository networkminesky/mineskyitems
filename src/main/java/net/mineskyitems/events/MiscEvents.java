package net.mineskyitems.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class MiscEvents implements Listener {

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if(e.isCancelled()) return;

        final Item drop = e.getItemDrop();

        final net.mineskyitems.entities.item.Item item = ItemHandler.getItemFromStack(drop.getItemStack());
        if(item == null) return;

        if(item.getItemRarity().shouldHaveGlowing())
            drop.setGlowing(true);

        drop.setCustomNameVisible(true);

        // paper por algum motivo adiciona [] mesmo nao sendo um array (????)
        String semColchetes = LegacyComponentSerializer.legacySection().serialize(drop.getItemStack().displayName())
                .replace("[", "").replace("]", "");
        Component novoNome = LegacyComponentSerializer.legacySection().deserialize(semColchetes);

        drop.customName(novoNome);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        final Projectile projectile = e.getEntity();
        final Entity entity = e.getHitEntity();

        if(entity == null
        || !projectile.getPersistentDataContainer().has(MineSkyItems.NAMESPACED_KEY,
                PersistentDataType.DOUBLE))
            return;

        double damage = projectile.getPersistentDataContainer()
                .getOrDefault(MineSkyItems.NAMESPACED_KEY, PersistentDataType.DOUBLE, 1.0);

        if(entity instanceof Damageable damageable) {
            damageable.damage(damage, projectile);
            e.setCancelled(true);
            projectile.remove();
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if(e.getEntity().getType() != EntityType.PLAYER) return;

        final Player player = (Player) e.getEntity();
        final EntityDamageEvent.DamageCause damageCause = e.getCause();

        if(player.getEquipment() == null)
            return;

        Arrays.stream(player.getEquipment().getArmorContents()).forEach(stack -> {
            net.mineskyitems.entities.item.Item item = ItemHandler.getItemFromStack(stack);

            if(item != null) {
                item.damageItem(player, stack, 1, e);
            }
        });
    }
}
