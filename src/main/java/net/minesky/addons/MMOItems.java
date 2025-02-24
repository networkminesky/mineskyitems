package net.minesky.addons;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class MMOItems {
    private static boolean isMMOItemsLoaded() {
        Plugin mmoItemsPlugin = Bukkit.getPluginManager().getPlugin("MMOItems");
        return mmoItemsPlugin != null && mmoItemsPlugin.isEnabled();
    }
}
