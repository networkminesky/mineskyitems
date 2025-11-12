package net.mineskyitems.utils;

import net.Indyuce.mmocore.api.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class Runnables {

    public static void equipmentChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Bukkit.getOnlinePlayers()) {
                    if(player.hasPermission("mineskyitems.bypassrequirements"))
                        continue;

                    final PlayerInventory inventory = player.getInventory();
                    final PlayerData data = PlayerData.get(player);

                    final int level = data.getLevel();
                    final String className = data.getProfess().getName();

                    checkArmorPiece(level, className, player, inventory.getHelmet(), EquipmentSlot.HEAD);
                    checkArmorPiece(level, className, player, inventory.getChestplate(), EquipmentSlot.CHEST);
                    checkArmorPiece(level, className, player, inventory.getLeggings(), EquipmentSlot.LEGS);
                    checkArmorPiece(level, className, player, inventory.getBoots(), EquipmentSlot.FEET);
                }
            }
        }.runTaskTimerAsynchronously(MineSkyItems.getInstance(), 20, 5);
    }

    public static void checkArmorPiece(final int playerLevel, final String className, final Player player,
                                       final ItemStack itemStack, final EquipmentSlot slot) {
        if(itemStack == null || itemStack.getType().isAir())
            return;
        if(!itemStack.hasItemMeta() || itemStack.getItemMeta() == null)
            return;
        if(!itemStack.getItemMeta().getPersistentDataContainer().has(MineSkyItems.NAMESPACED_KEY))
            return;

        final int level = ItemHandler.getStaticRequiredLevel(itemStack);
        final List<String> classes = ItemHandler.getStaticClasses(itemStack);

        if(level > playerLevel
        || !classes.contains(className)) {
            player.getInventory().setItem(slot, null);

            if(player.getInventory().firstEmpty() == -1) {
                player.getWorld().spawn(player.getLocation(), Item.class, item -> {
                    item.setItemStack(itemStack);
                    item.setPickupDelay(60);
                });
            } else {
                player.getInventory().addItem(itemStack);
            }

            player.sendMessage(Component.text("Você não pode equipar esse item, " +
                            "ele possui um nível muito superior ao seu, ou não é adequado a sua classe.")
                    .color(NamedTextColor.RED)
            );
        }
    }
}
