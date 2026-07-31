package net.mineskyitems;

import net.milkbowl.vault.economy.Economy;
import net.mineskyitems.commands.ItemCommand;
import net.mineskyitems.entities.ItemDustHandler;
import net.mineskyitems.entities.curves.CurveHandler;
import net.mineskyitems.entities.rarities.RarityHandler;
import net.mineskyitems.events.InteractionEvents;
import net.mineskyitems.events.MiscEvents;
import net.mineskyitems.events.OffhandAttackListener;
import net.mineskyitems.gui.crafting.CraftingListener;
import net.mineskyitems.gui.crafting.RecipeManager;
import net.mineskyitems.gui.editor.ItemBuilderMenu;
import net.mineskyitems.entities.categories.CategoryHandler;
import net.mineskyitems.entities.tooltip.TooltipHandler;
import net.mineskyitems.gui.editor.ItemSkillsMenu;
import net.mineskyitems.gui.blacksmith.ItemRecyclerMenu;
import net.mineskyitems.gui.blacksmith.ItemRepairMenu;
import net.mineskyitems.gui.rotatingshop.RotatingItemsGUI;
import net.mineskyitems.gui.tinkering.*;
import net.mineskyitems.gui.tinkering.recipe.TinkeringManager;
import net.mineskyitems.utils.FrameUpdater;
import net.mineskyitems.utils.RotatingShop;
import net.mineskyitems.utils.Runnables;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class MineSkyItems extends JavaPlugin {

    public static YamlConfiguration defaultCurveConfig;

    public static NamespacedKey NAMESPACED_KEY = NamespacedKey.fromString("mineskyitems");

    public static Economy economy = null;

    public static boolean MMOCORE_HOOK = false;

    public static Logger l;

    public static FileConfiguration config;

    @Override
    public void onEnable() {
        l = this.getLogger();

        this.saveDefaultConfig();

        config = this.getConfig();

        l.info("    __  ___    _                           __                      ____   __                           ");
        l.info("   /  |/  /   (_)   ____   ___    _____   / /__   __  __          /  _/  / /_  ___    ____ ___    _____");
        l.info("  / /|_/ /   / /   / __ \\ / _ \\  / ___/  / //_/  / / / /          / /   / __/ / _ \\  / __ `__ \\  / ___/");
        l.info(" / /  / /   / /   / / / //  __/ (__  )  / ,<    / /_/ /         _/ /   / /_  /  __/ / / / / / / (__  ) ");
        l.info("/_/  /_/   /_/   /_/ /_/ \\___/ /____/  /_/|_|   \\__, /         /___/   \\__/  \\___/ /_/ /_/ /_/ /____/ ");
        l.info("                                               /____/");

        System();
    }

    private void System() {
        l.info("Carregando item curves...");
        CurveHandler.setupCurves();

        l.info("Carregando rarities...");
        RarityHandler.setupRarities();

        l.info("Carregando tooltips...");
        TooltipHandler.setupTooltips();

        l.info("Carregando categorias...");
        CategoryHandler.setupCategories();

        l.info("Carregando itens hardcoded...");
        ItemDustHandler.registerDusts();

        FrameUpdater.runnable();

        Runnables.equipmentChecker();

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            l.info("Economy do Vault linkado!");
            economy = rsp.getProvider();
        }

        if(config.getBoolean("offhand-attack", false)) {
            Bukkit.getPluginManager().registerEvents(new OffhandAttackListener(), this);
        }

        Bukkit.getPluginManager().registerEvents(new InteractionEvents(), this);
        Bukkit.getPluginManager().registerEvents(new MiscEvents(), this);

        Bukkit.getPluginManager().registerEvents(new CraftingListener(), this);

        Bukkit.getPluginManager().registerEvents(new TinkeringCreatorGUI(), this);
        Bukkit.getPluginManager().registerEvents(new TinkeringSearchGUI(), this);
        Bukkit.getPluginManager().registerEvents(new TinkeringRecipePreviewGUI(), this);
        Bukkit.getPluginManager().registerEvents(new TinkeringGUI(), this);

        getServer().getPluginManager().registerEvents(new TinkeringQueueCreator(), this);

        Bukkit.getPluginManager().registerEvents(new ItemBuilderMenu(), this);
        Bukkit.getPluginManager().registerEvents(new ItemSkillsMenu(), this);
        Bukkit.getPluginManager().registerEvents(new ItemRecyclerMenu(), this);
        Bukkit.getPluginManager().registerEvents(new ItemRepairMenu(), this);

        Bukkit.getPluginManager().registerEvents(new RotatingItemsGUI(), this);

        Bukkit.getGlobalRegionScheduler().runDelayed(this, (_) -> RotatingShop.initializeShop(), 20);

        l.info("Carregando recipes custom...");
        RecipeManager.loadRecipes();

        l.info("Carregando recipes 5x5...");
        TinkeringManager.registerAllFromFile();
        l.info("Novos craftings registrados: "+ TinkeringManager.amount()+"!");

        this.getCommand("item").setExecutor(new ItemCommand());
    }

    public static MineSkyItems getInstance() {
        return MineSkyItems.getPlugin(MineSkyItems.class);
    }
}
