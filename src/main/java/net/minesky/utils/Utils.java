package net.minesky.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.block.Action;

import java.util.logging.Level;

public class Utils {

    public static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static InteractionType convertInteractionType(Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)
            return InteractionType.RIGHT_CLICK;

        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR)
            return InteractionType.LEFT_CLICK;

        return InteractionType.RIGHT_CLICK;
    }

}
