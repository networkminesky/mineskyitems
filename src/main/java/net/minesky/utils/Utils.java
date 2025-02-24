package net.minesky.utils;

import org.bukkit.Bukkit;

import java.util.logging.Level;

public class Utils {
    public static void Logger(Level level, String message) {
        Bukkit.getLogger().log(level, "[MineSkyItems] " + message);
    }
}
