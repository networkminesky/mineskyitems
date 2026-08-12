package net.mineskyitems.entities.categories;

import net.mineskyitems.entities.curves.CurveHandler;
import net.mineskyitems.entities.curves.ItemCurve;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.tooltip.Tooltip;
import net.mineskyitems.entities.tooltip.TooltipHandler;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Category {

    private final ConfigurationSection categoriesSection;
    private final String id;

    private final File file;
    private YamlConfiguration config;

    /**
     * MELEE for melee physical weapons
     * RANGED for ranged attack weapons (e.g. Bows, Crossbows
     * TOOL for tools
     */
    private final String type;
    /** nullable
     * PICKAXE, AXE, HOE, SHOVEL
     */
    private final @Nullable String tool;

    private final String name;
    private final Material defaultItem;

    private final ItemCurve curve;

    private final boolean noAttributes;
    private final boolean doNotStack;
    private final boolean dualHanded;
    private final boolean oneHanded;
    private final boolean disappearWhenBroken;
    private final boolean showAlmostBroken;
    private final boolean vanillaDurability;

    private final Tooltip tooltip;

    private final List<Item> itemList = new ArrayList<>();

    public Category(File file, ConfigurationSection categoriesSection, String id) {
        this.categoriesSection = categoriesSection;
        this.id = id;

        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);

        this.tooltip = TooltipHandler.getTooltipById(categoriesSection.getString("tooltip", "default"));

        this.type = categoriesSection.getString("type", "MELEE");
        this.tool = categoriesSection.getString("tool", null);

        this.vanillaDurability = categoriesSection.getBoolean("vanilla-durability", true);

        this.curve = CurveHandler.getById(categoriesSection.getString("curve", "default-curves.yml"));
        this.name = categoriesSection.getString("name", id.toLowerCase());
        this.defaultItem = Material.getMaterial(categoriesSection.getString("default-item", "STONE"));

        this.dualHanded = categoriesSection.getBoolean("dual-handed", false);
        this.oneHanded = categoriesSection.getBoolean("one-handed", false);

        this.noAttributes = categoriesSection.getBoolean("no-attributes", false);
        this.doNotStack = categoriesSection.getBoolean("do-not-stack", false);
        this.disappearWhenBroken = categoriesSection.getBoolean("disappear-when-broken", true);
        this.showAlmostBroken = categoriesSection.getBoolean("show-almost-broken", true);
    }

    public boolean isVanillaDurability() {
        return this.vanillaDurability;
    }

    public boolean shouldShowAlmostBroken() {
        return showAlmostBroken;
    }

    public ItemCurve getCurve() {
        return curve;
    }

    /**
     * Means that the player can put the item in the offhand slot (F) and attack with the right click.
     * @return
     */
    public boolean isDualHanded() {
        return dualHanded;
    }

    /**
     * Means that the player can only attack with this item if it they do not have another item in the offhand slot.
     * @return
     */
    public boolean isOneHanded() {
        return oneHanded;
    }

    public boolean isTool() {
        return this.tool != null;
    }

    @Nullable
    public String getTool() {
        return this.tool;
    }

    public String getType() {
        return type;
    }

    public boolean isDoNotStack() {
        return doNotStack;
    }

    public boolean isNoAttributes() {
        return noAttributes;
    }

    public boolean isDisappearWhenBroken() {
        return disappearWhenBroken;
    }

    public Tooltip getTooltip() {
        return tooltip;
    }

    public void removeItem(Item item) {
        itemList.remove(item);
    }

    public void playUseSounds(Player player, boolean global) {
        if(getCategoriesFileSection().contains("on-use")) {
            String sound = getCategoriesFileSection().getString("on-use.sound", "");
            float pitch = (float)getCategoriesFileSection().getDouble("on-use.pitch", 1);

            if(global)
                player.getWorld().playSound(player.getLocation(), sound, 0.8f, pitch);
            else
                player.playSound(player.getLocation(), sound, 0.8f, pitch);
        }
    }

    public void forceAddItem(Item item) {
        itemList.add(item);
    }

    public List<Item> getAllItems() {
        return itemList;
    }

    public void reloadFile() {
        final File file = getFile();
        try {
            config.save(file);
        } catch (IOException ex) {
            ex.fillInStackTrace();
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void registerItemsInsideCategory() {
        for(String itemId : getConfig().getKeys(false)) {
            final ConfigurationSection section = getConfig().getConfigurationSection(itemId);

            assert section != null;
            try {
                Item item = new Item(this, itemId, section);
                //MineSkyItems.l.info("  | Item carregado: "+item.getMetadata().displayName());

                itemList.add(item);
            } catch (Exception exception) {
                exception.fillInStackTrace();
            }
        }
        reloadFile();
    }

    public void reloadCategory() {
        //reloadFile();
        itemList.clear();
        registerItemsInsideCategory();
    }

    public Material getDefaultItem() {
        return defaultItem;
    }

    public String getName() {
        return name;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public File getFile() {
        return file;
    }

    public String getId() {
        return id;
    }

    public ConfigurationSection getCategoriesFileSection() {
        return categoriesSection;
    }
}
