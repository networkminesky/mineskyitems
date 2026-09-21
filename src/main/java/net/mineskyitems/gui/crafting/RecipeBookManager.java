package net.mineskyitems.gui.crafting;

import net.mineskyitems.MineSkyItems;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeBookManager implements Listener {

    private static final Set<NamespacedKey> allCustomRecipeKeys = ConcurrentHashMap.newKeySet();

    public static void clearKeys() {
        allCustomRecipeKeys.clear();
    }

    public static void registerKey(NamespacedKey key) {
        if (key != null) {
            allCustomRecipeKeys.add(key);
        }
    }

    public static void registerKeys(Collection<NamespacedKey> keys) {
        if (keys != null) {
            allCustomRecipeKeys.addAll(keys);
        }
    }

    public static void unregisterKey(NamespacedKey key) {
        if (key != null) {
            allCustomRecipeKeys.remove(key);
        }
    }

    public static Set<NamespacedKey> getAllCustomRecipeKeys() {
        return Collections.unmodifiableSet(allCustomRecipeKeys);
    }

    public static void unlockAllForPlayer(Player player) {
        if (allCustomRecipeKeys.isEmpty() || !player.isOnline()) return;

        player.getScheduler().run(MineSkyItems.getInstance(), task -> {
            if (!player.isOnline()) return;
            player.discoverRecipes(allCustomRecipeKeys);
        }, null);
    }

    public static void broadcastNewRecipe(NamespacedKey key) {
        if (key == null) return;
        registerKey(key);

        Collection<NamespacedKey> single = Collections.singleton(key);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.getScheduler().run(MineSkyItems.getInstance(), task -> {
                if (online.isOnline()) {
                    online.discoverRecipes(single);
                }
            }, null);
        }
    }

    public static void broadcastAllToOnlinePlayers() {
        if (allCustomRecipeKeys.isEmpty()) return;

        for (Player online : Bukkit.getOnlinePlayers()) {
            unlockAllForPlayer(online);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.getScheduler().runDelayed(MineSkyItems.getInstance(), task -> {
            if (player.isOnline()) {
                unlockAllForPlayer(player);
            }
        }, null, 10L);
    }
}