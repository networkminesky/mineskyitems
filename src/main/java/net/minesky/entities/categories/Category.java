package net.minesky.entities.categories;

import net.minesky.entities.item.Item;
import net.minesky.entities.tooltip.Tooltip;
import net.minesky.entities.tooltip.TooltipHandler;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Category {

    private final ConfigurationSection categoriesSection;
    private final String id;

    private final File file;
    private YamlConfiguration config;

    // Type can be: "MELEE" for melee physical weapons - "RANGED" for ranged attack weapons (e.g. Bows, Crossbows, ...)
    private final String type;

    private final String name;
    private final Material defaultItem;

    private final boolean noAttributes;

    private final Tooltip tooltip;

    private final List<Item> itemList = new ArrayList<>();

    public Category(File file, ConfigurationSection categoriesSection, String id) {
        this.categoriesSection = categoriesSection;
        this.id = id;

        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);

        this.tooltip = TooltipHandler.getTooltipById(categoriesSection.getString("tooltip", "default"));

        this.type = categoriesSection.getString("type", "MELEE");
        this.name = categoriesSection.getString("name", id.toLowerCase());
        this.defaultItem = Material.getMaterial(categoriesSection.getString("default-item", "STONE"));

        this.noAttributes = categoriesSection.getBoolean("no-attributes", false);
    }

    public String getType() {
        return type;
    }

    public boolean isNoAttributes() {
        return noAttributes;
    }

    public Tooltip getTooltip() {
        return tooltip;
    }

    public void removeItem(Item item) {
        itemList.remove(item);
    }

    public void playUseSounds(Player player) {
        if(getCategoriesFileSection().contains("on-use")) {
            String sound = getCategoriesFileSection().getString("on-use.sound", "");
            float pitch = (float)getCategoriesFileSection().getDouble("on-use.pitch", 1);

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
