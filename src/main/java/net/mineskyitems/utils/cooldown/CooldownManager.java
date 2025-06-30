package net.mineskyitems.utils.cooldown;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemSkill;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private static final Map<UUID, Map<ItemSkill, Long>> cooldowns = new HashMap<>();
    private static final Map<UUID, Map<Item, Long>> itemCooldowns = new HashMap<>();

    public static void createCooldown(Player player, ItemSkill item, float cooldown) {
        long cooldownInTicks = (long)(cooldown * 20);

        UUID uuid = player.getUniqueId();
        cooldowns.putIfAbsent(uuid, new HashMap<>());

        long cooldownEnd = System.currentTimeMillis() + (cooldownInTicks * 50); // mili
        cooldowns.get(uuid).put(item, cooldownEnd);

        // Agendando a remoção automática do cooldown
        Bukkit.getScheduler().runTaskLaterAsynchronously(MineSkyItems.getInstance(), () -> {
            Map<ItemSkill, Long> playerCooldowns = cooldowns.get(uuid);
            if (playerCooldowns != null) {
                playerCooldowns.remove(item);
                if (playerCooldowns.isEmpty()) {
                    cooldowns.remove(uuid);
                }
            }
        }, cooldownInTicks);
    }

    public static void createItemCooldown(Player player, Item item, float cooldown) {
        long cooldownInTicks = (long)(cooldown * 20);

        UUID uuid = player.getUniqueId();
        itemCooldowns.putIfAbsent(uuid, new HashMap<>());

        long cooldownEnd = System.currentTimeMillis() + (cooldownInTicks * 50); // mili
        itemCooldowns.get(uuid).put(item, cooldownEnd);

        // Agendando a remoção automática do cooldown
        Bukkit.getScheduler().runTaskLaterAsynchronously(MineSkyItems.getInstance(), () -> {
            Map<Item, Long> playerCooldowns = itemCooldowns.get(uuid);
            if (playerCooldowns != null) {
                playerCooldowns.remove(item);
                if (playerCooldowns.isEmpty()) {
                    itemCooldowns.remove(uuid);
                }
            }
        }, cooldownInTicks);
    }

    public static boolean inCooldown(Player player, ItemSkill item) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) return false;

        Long cooldownEnd = cooldowns.get(uuid).get(item);
        if (cooldownEnd == null || cooldownEnd < System.currentTimeMillis()) {
            cooldowns.get(uuid).remove(item);
            return false;
        }
        return true;
    }
    public static boolean inItemCooldown(Player player, Item item) {
        UUID uuid = player.getUniqueId();
        if (!itemCooldowns.containsKey(uuid)) return false;

        Long cooldownEnd = itemCooldowns.get(uuid).get(item);
        if (cooldownEnd == null || cooldownEnd < System.currentTimeMillis()) {
            itemCooldowns.get(uuid).remove(item);
            return false;
        }
        return true;
    }


    public static long getRemainingCooldown(Player player, ItemSkill item) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) return 0L;

        Long cooldownEnd = cooldowns.get(uuid).get(item);
        if (cooldownEnd == null || cooldownEnd < System.currentTimeMillis()) {
            cooldowns.get(uuid).remove(item);
            return 0L;
        }
        return (cooldownEnd - System.currentTimeMillis()) / 1000;
    }

}
