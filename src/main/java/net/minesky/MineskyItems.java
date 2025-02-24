package net.minesky;

import net.minesky.commands.CriarItemsCommand;
import net.minesky.config.ItemConfig;
import net.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class MineskyItems extends JavaPlugin {

    private static final Map<Player, ItemConfig> itemConfigMap = new HashMap<>();

    public static Map<Player, ItemConfig> getItemConfigMap() {
        return itemConfigMap;
    }

    @Override
    public void onEnable() {
        Utils.Logger(Level.INFO, "    __  ___    _                           __                      ____   __                           ");
        Utils.Logger(Level.INFO, "   /  |/  /   (_)   ____   ___    _____   / /__   __  __          /  _/  / /_  ___    ____ ___    _____");
        Utils.Logger(Level.INFO, "  / /|_/ /   / /   / __ \\ / _ \\  / ___/  / //_/  / / / /          / /   / __/ / _ \\  / __ `__ \\  / ___/");
        Utils.Logger(Level.INFO, " / /  / /   / /   / / / //  __/ (__  )  / ,<    / /_/ /         _/ /   / /_  /  __/ / / / / / / (__  ) ");
        Utils.Logger(Level.INFO, "/_/  /_/   /_/   /_/ /_/ \\___/ /____/  /_/|_|   \\__, /         /___/   \\__/  \\___/ /_/ /_/ /_/ /____/ ");
        Utils.Logger(Level.INFO, "                                               /____/");
        System();
    }

    @Override
    public void onDisable() {
        Utils.Logger(Level.INFO, "Goodbye!");
    }

    private void System() {
        PluginManager system = Bukkit.getServer().getPluginManager();
        system.registerEvents(new CriarItemsCommand(), this);
        this.getCommand("criaritem").setExecutor(new CriarItemsCommand());
    }

    public static MineskyItems get() {
        return (MineskyItems) Bukkit.getServer().getPluginManager().getPlugin("MineskyItems");
    }
}
