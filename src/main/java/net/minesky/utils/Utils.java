package net.minesky.utils;

import net.minesky.MineSkyItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String c(String s) {
        return hex(s);
    }

    public static InteractionType convertInteractionType(Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)
            return InteractionType.RIGHT_CLICK;

        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR)
            return InteractionType.LEFT_CLICK;

        return InteractionType.RIGHT_CLICK;
    }

    private static String createHex(String hexString) {
        hexString = hexString.replace("&", "");
        return net.md_5.bungee.api.ChatColor.of(hexString).toString();
    }

    public static String hex(String message) {
        Pattern hexPattern = Pattern.compile("&#[A-Fa-f0-9]{6}");
        Matcher matcher = hexPattern.matcher(message);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, createHex(matcher.group()));
        }

        matcher.appendTail(result);
        message = result.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void awaitChatInput(Player player, ChatInputCallback callback) {
        player.sendTitle("§5§lDigite no chat", "§7Digite 'sair' para voltar!", 5, 60, 20);

        player.closeInventory();

        Listener listener = new Listener() {
            @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
            public void onChat(AsyncPlayerChatEvent e) {

                Bukkit.getScheduler().runTask(MineSkyItems.getInstance(), () -> {
                    if(!e.getPlayer().equals(player))
                        return;

                    final String msg = e.getMessage();

                    switch(msg.toLowerCase()) {
                        case "cancel":
                        case "cancelar":
                        case "sair": {
                            callback.onCancel();

                            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO,1, 1);
                            e.getPlayer().sendMessage("Cancelando e retornando ao menu...");

                            AsyncPlayerChatEvent.getHandlerList().unregister(this);
                            return;
                        }
                    }

                    e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_YES,1, 1);

                    AsyncPlayerChatEvent.getHandlerList().unregister(this);
                    callback.onInput(c(msg));
                });

            }
        };
        Bukkit.getServer().getPluginManager().registerEvents(listener, MineSkyItems.getInstance());
    }

    public static final String PURPLE_COLOR = net.md_5.bungee.api.ChatColor.of("#7147da")+"";

}
