package net.minesky;

import net.Indyuce.mmocore.api.MMOCoreAPI;
import net.minesky.commands.ItemCommand;
import net.minesky.events.InteractionEvents;
import net.minesky.gui.ItemBuilderMenu;
import net.minesky.handler.categories.CategoryHandler;
import net.minesky.handler.tooltip.TooltipHandler;
import net.minesky.logics.LevelCurves;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.logging.Logger;

public final class MineSkyItems extends JavaPlugin {

    public static YamlConfiguration defaultCurveConfig;

    public static MMOCoreAPI mmocoreAPI;

    public static Logger l;

    @Override
    public void onEnable() {
        l = this.getLogger();

        l.info("    __  ___    _                           __                      ____   __                           ");
        l.info("   /  |/  /   (_)   ____   ___    _____   / /__   __  __          /  _/  / /_  ___    ____ ___    _____");
        l.info("  / /|_/ /   / /   / __ \\ / _ \\  / ___/  / //_/  / / / /          / /   / __/ / _ \\  / __ `__ \\  / ___/");
        l.info(" / /  / /   / /   / / / //  __/ (__  )  / ,<    / /_/ /         _/ /   / /_  /  __/ / / / / / / (__  ) ");
        l.info("/_/  /_/   /_/   /_/ /_/ \\___/ /____/  /_/|_|   \\__, /         /___/   \\__/  \\___/ /_/ /_/ /_/ /____/ ");
        l.info("                                               /____/");

        System();
    }

    // Método visando que o plugin está em um ambiente de desenvolvimento.
    public static void reload() {

        getInstance().getPluginLoader().disablePlugin(getInstance());
        getInstance().getPluginLoader().enablePlugin(getInstance());

    }

    private void System() {
        LevelCurves.setupCurves();

        l.info("Carregando tooltips...");
        TooltipHandler.setupTooltips();

        l.info("Carregando categorias...");
        CategoryHandler.setupCategories();

        if(Bukkit.getPluginManager().getPlugin("MMOCore") != null) {
            mmocoreAPI = new MMOCoreAPI(this);
        }

        Bukkit.getPluginManager().registerEvents(new ItemBuilderMenu(), this);

        Bukkit.getPluginManager().registerEvents(new InteractionEvents(), this);

        this.getCommand("item").setExecutor(new ItemCommand());
    }

    public static MineSkyItems getInstance() {
        return MineSkyItems.getPlugin(MineSkyItems.class);
    }
}
