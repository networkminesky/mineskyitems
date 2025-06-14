package net.minesky;

import net.Indyuce.mmocore.api.MMOCoreAPI;
import net.minesky.commands.ItemCommand;
import net.minesky.entities.ItemDustHandler;
import net.minesky.entities.rarities.RarityHandler;
import net.minesky.events.InteractionEvents;
import net.minesky.events.MiscEvents;
import net.minesky.gui.ItemBuilderMenu;
import net.minesky.entities.categories.CategoryHandler;
import net.minesky.entities.tooltip.TooltipHandler;
import net.minesky.gui.ItemSkillsMenu;
import net.minesky.gui.blacksmith.ItemRecyclerMenu;
import net.minesky.gui.blacksmith.ItemRepairMenu;
import net.minesky.logics.LevelCurvesLogic;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class MineSkyItems extends JavaPlugin {

    public static YamlConfiguration defaultCurveConfig;

    public static NamespacedKey NAMESPACED_KEY = NamespacedKey.fromString("mineskyitems");

    public static MMOCoreAPI mmocoreAPI;

    public static boolean MMOCORE_HOOK = false;

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

    private void System() {
        LevelCurvesLogic.setupCurves();

        l.info("Carregando rarities...");
        RarityHandler.setupRarities();

        l.info("Carregando tooltips...");
        TooltipHandler.setupTooltips();

        l.info("Carregando categorias...");
        CategoryHandler.setupCategories();

        l.info("Carregando itens hardcoded...");
        ItemDustHandler.registerDusts();

        if(Bukkit.getPluginManager().getPlugin("MMOCore") != null) {
            mmocoreAPI = new MMOCoreAPI(this);
            MMOCORE_HOOK = true;
        }

        Bukkit.getPluginManager().registerEvents(new InteractionEvents(), this);
        Bukkit.getPluginManager().registerEvents(new MiscEvents(), this);

        Bukkit.getPluginManager().registerEvents(new ItemBuilderMenu(), this);
        Bukkit.getPluginManager().registerEvents(new ItemSkillsMenu(), this);
        Bukkit.getPluginManager().registerEvents(new ItemRecyclerMenu(), this);
        Bukkit.getPluginManager().registerEvents(new ItemRepairMenu(), this);

        this.getCommand("item").setExecutor(new ItemCommand());
    }

    public static MineSkyItems getInstance() {
        return MineSkyItems.getPlugin(MineSkyItems.class);
    }
}
