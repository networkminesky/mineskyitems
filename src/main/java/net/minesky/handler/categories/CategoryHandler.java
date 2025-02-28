package net.minesky.handler.categories;

import net.minesky.MineSkyItems;
import net.minesky.handler.Item;
import net.minesky.handler.ItemHandler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CategoryHandler {

    public static ArrayList<Category> categories = new ArrayList<>();

    public static File itemsFolder = new File(MineSkyItems.getInstance().getDataFolder(), "items");
    public static File file = new File(MineSkyItems.getInstance().getDataFolder(), "categories.yml");
    public static YamlConfiguration configuration;

    public static Category getCategoryById(String id) {
        return categories.stream()
                .filter(element -> element.getId().equals(id))
                .findFirst().orElse(null);
    }

    public static void setupCategories() {
        configuration = YamlConfiguration.loadConfiguration(file);
        if (!file.exists()) {
            try {
                MineSkyItems.l.info("Arquivo categories.yml criado!");
                configuration.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        itemsFolder.mkdirs();

        configuration = YamlConfiguration.loadConfiguration(file);

        for(String key : configuration.getKeys(false)) {
            final ConfigurationSection section = configuration.getConfigurationSection(key);
            File configfile = new File(itemsFolder, key+".yml");

            if(!configfile.exists()) {
                try {
                    configfile.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            assert section != null;

            Category category = new Category(configfile, section, key);

            MineSkyItems.l.info("| Carregando categoria "+category.getName());

            registerItemsInsideCategory(category);

            categories.add(category);
        }
    }

    private static void registerItemsInsideCategory(Category category) {

        for(String itemId : category.getConfig().getKeys(false)) {
            final ConfigurationSection section = category.getConfig().getConfigurationSection(itemId);

            assert section != null;
            try {
                Item item = new Item(category, itemId, section);

                MineSkyItems.l.info("  | Item carregado: "+item.getMetadata().displayName());

                ItemHandler.items.add(item);
                category.addItem(item);
            } catch (Exception exception) {
                exception.fillInStackTrace();
            }
        }

    }

}
