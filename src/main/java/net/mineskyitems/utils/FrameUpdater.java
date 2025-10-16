package net.mineskyitems.utils;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
Aparentemente o NMS/Spigot/Paper não possuem eventos quando
jogadores clicam com botao do meio em um item frame

(vulgo, copiar item do item frame)

então é preciso fazer um workaround meio bruto para fazer essa mecânica

 */
public class FrameUpdater {

    public static Map<UUID, Material> lastItem = new ConcurrentHashMap<>();

    public static void runnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Bukkit.getOnlinePlayers()) {
                    if(!(player.hasPermission("mineskyitems.itemeditor"))) continue;
                    if(player.getGameMode() != GameMode.CREATIVE) continue;

                    final ItemStack mainHand = player.getInventory().getItemInMainHand();

                    // Pegou um item novo na mão
                    if(!lastItem.getOrDefault(player.getUniqueId(), Material.AIR)
                            .equals(mainHand.getType())) {
                        final RayTraceResult result = player.rayTraceEntities(5);

                        if(result != null && result.getHitEntity() != null) {
                            if(result.getHitEntity().getType() == EntityType.ITEM_FRAME
                                || result.getHitEntity().getType() == EntityType.GLOW_ITEM_FRAME) {
                                ItemFrame frame = (ItemFrame) result.getHitEntity();

                                final String frameItemId = Utils.getItemIdFromStack(frame.getItem());
                                final String handItemId = Utils.getItemIdFromStack(mainHand);

                                if(!frameItemId.isBlank() && !handItemId.isBlank()) {
                                    // everything true
                                    if(handItemId.equalsIgnoreCase(frameItemId)) {
                                        Item item = ItemHandler.getItemById(frameItemId);

                                        if(item != null) {
                                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                                            player.sendTitle("...", "§7Atualizando item", 5, 0, 2);

                                            player.getInventory().setItemInMainHand(item.buildStack());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    lastItem.put(player.getUniqueId(), mainHand.getType());
                }
            }
        }.runTaskTimerAsynchronously(MineSkyItems.getInstance(), 60, 2);
    }

}
