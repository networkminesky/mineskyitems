package net.minesky.handler.categories;

import net.minesky.handler.Item;
import net.minesky.handler.ItemHandler;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Category {

    private final ConfigurationSection categoriesSection;
    private final String id;

    private final File file;
    private YamlConfiguration config;

    private final String name;
    private final Material defaultItem;

    private final List<Item> itemList;

    public Category(File file, ConfigurationSection categoriesSection, String id) {
        this.categoriesSection = categoriesSection;
        this.id = id;

        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);

        this.name = categoriesSection.getString("name", id.toLowerCase());
        this.defaultItem = Material.valueOf(categoriesSection.getString("default-item", "STONE"));

        this.itemList = new ArrayList<>();
    }

    public void removeItem(Item item) {
        itemList.remove(item);
    }

    public List<Item> getAllItems() {
        return itemList;
    }

    public void addItem(Item item) {
        itemList.add(item);
        ItemHandler.items.add(item);
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
