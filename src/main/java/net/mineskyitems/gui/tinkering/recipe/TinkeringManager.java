package net.mineskyitems.gui.tinkering.recipe;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class TinkeringManager {

    private static final List<TinkeringRecipe> recipes = new ArrayList<>();

    public static List<TinkeringRecipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    public static int amount() {
        return recipes.size();
    }

    public static class ItemEntry {
        private final boolean isVanilla;
        private final String type;
        private final String id;

        public ItemEntry(final String type, final String id) {
            this.isVanilla = !type.equalsIgnoreCase("mineskyitem");
            this.type = type;

            if (this.isVanilla) {
                this.id = id.replace("minecraft:", "").toUpperCase().trim();
            } else {
                this.id = id.trim();
            }
        }

        public boolean isAir() {
            return id.equalsIgnoreCase("air") || type.equalsIgnoreCase("air") || id.isEmpty();
        }

        public boolean isVanilla() {
            return isVanilla;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }
    }

    public static void clearRecipes() {
        recipes.clear();
    }

    public static void registerRecipe(TinkeringRecipe recipe) {
        recipes.add(recipe);
    }

    public static void registerAllFromFile() {
        TinkeringManager.clearRecipes();

        File craftingFolder = new File(MineSkyItems.getInstance().getDataFolder(), "crafting");
        if (!craftingFolder.exists()) {
            craftingFolder.mkdirs();
        }

        File[] files = craftingFolder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.getName().endsWith(".yml")) continue;
            final String id = file.getName().replace(".yml", "").trim();

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection keys = config.getConfigurationSection("keys");
            if (keys == null) {
                MineSkyItems.l.warning("Keys is null em " + file.getName());
                continue;
            }

            final Map<Character, ItemEntry> ingredients = new HashMap<>();

            for (String key : keys.getKeys(false)) {
                ConfigurationSection section = keys.getConfigurationSection(key);
                if (section == null) continue;

                final String type = section.getString("type", "MINESKYITEM");
                final String itemId = section.getString("id", "");

                ingredients.put(section.getName().charAt(0), new ItemEntry(type, itemId));
            }

            List<String> shape = config.getStringList("shape");
            final ItemEntry result = new ItemEntry(config.getString("result.type", ""), config.getString("result.id", ""));

            TinkeringRecipe customRecipe = new TinkeringRecipe(id, shape.toArray(new String[0]), ingredients, result);
            TinkeringManager.registerRecipe(customRecipe);
        }
    }

    public static boolean deleteRecipe(String id) {
        File craftingFolder = new File(MineSkyItems.getInstance().getDataFolder(), "crafting");
        File file = new File(craftingFolder, id + ".yml");

        if (file.exists() && file.delete()) {
            registerAllFromFile();
            return true;
        }
        return false;
    }

    public static TinkeringRecipe getMatchingRecipe(ItemStack[][] grid) {
        ItemStack[][] croppedGrid = cropGrid(grid);

        for (TinkeringRecipe recipe : recipes) {
            if (matches(croppedGrid, recipe.getCroppedRecipeGrid())) {
                return recipe;
            }
        }
        return null;
    }

    public static ItemStack buildItemStackFromEntry(ItemEntry entry) {
        if (entry == null || entry.isAir()) return null;

        if (entry.isVanilla()) {
            try {
                Material mat = Material.valueOf(entry.getId());
                return new ItemStack(mat);
            } catch (Exception ex) {
                return null;
            }
        } else {
            Item item = ItemHandler.getItem(entry.getId());
            return (item != null) ? item.buildStack() : null;
        }
    }

    private static boolean matches(ItemStack[][] croppedGrid, ItemEntry[][] croppedRecipe) {
        if (croppedGrid.length != croppedRecipe.length || croppedGrid[0].length != croppedRecipe[0].length) {
            return false;
        }

        for (int r = 0; r < croppedGrid.length; r++) {
            for (int c = 0; c < croppedGrid[0].length; c++) {
                ItemStack gridItem = croppedGrid[r][c];
                ItemEntry recipeEntry = croppedRecipe[r][c];

                boolean gridEmpty = (gridItem == null || gridItem.getType().isAir());
                boolean recipeEmpty = (recipeEntry == null || recipeEntry.isAir());

                if (gridEmpty && recipeEmpty) continue;
                if (gridEmpty || recipeEmpty) return false;

                if (recipeEntry.isVanilla()) {
                    if (!gridItem.getType().name().equalsIgnoreCase(recipeEntry.getId())) {
                        return false;
                    }
                } else {
                    Item custom = ItemHandler.getItemFromStack(gridItem);
                    if (custom == null || !custom.getId().equalsIgnoreCase(recipeEntry.getId())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static ItemStack[][] cropGrid(ItemStack[][] grid) {
        int minRow = 5, maxRow = -1;
        int minCol = 5, maxCol = -1;

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                ItemStack item = grid[r][c];
                if (item != null && !item.getType().isAir()) {
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                }
            }
        }

        if (maxRow == -1) {
            return new ItemStack[0][0];
        }

        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;
        ItemStack[][] cropped = new ItemStack[height][width];

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                cropped[r][c] = grid[minRow + r][minCol + c];
            }
        }

        return cropped;
    }
}