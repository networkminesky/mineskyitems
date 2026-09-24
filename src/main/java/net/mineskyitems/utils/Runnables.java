package net.mineskyitems.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.entities.item.RevisionHandler;
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
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(MineSkyItems.getInstance(), (task) -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
                if(!player.hasPermission("mineskyitems.dynamic-revision"))
                    continue;

                player.getScheduler().run(MineSkyItems.getInstance(), (playerTask) -> {
                    try {
                        for (int i = 0; i < 40; i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack == null || stack.getType().isAir())
                                continue;

                            if (RevisionHandler.checkAndApply(player, stack)) {
                                player.getInventory().setItem(i, stack);
                            }
                        }
                    } catch(Exception ex) {
                        MineSkyItems.l.severe("Um erro ocorreu ao revisar itens dinamicamente no inventário do jogador "+player.getName());
                    }
                }, null);
            }
        }, 20, 10 * 20);
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
        || (!classes.isEmpty() && !classes.contains(className))) {
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
