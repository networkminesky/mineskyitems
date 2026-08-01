package net.mineskyitems.gui.crafting;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CraftingManager {

    private static final File file = new File(MineSkyItems.getInstance().getDataFolder(), "recipes.yml");
    private static final Set<NamespacedKey> registeredKeys = new HashSet<>();
    private static YamlConfiguration config;

    public static void loadRecipes() {
        // unregister old recipes
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();

        if (!file.exists()) {
            MineSkyItems.getInstance().saveResource("recipes.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("recipes")) return;

        for (String key : config.getConfigurationSection("recipes").getKeys(false)) {
            String path = "recipes." + key;
            List<String> gridList = config.getStringList(path + ".grid");
            String[] grid = gridList.toArray(new String[0]);

            ItemStack resultStack = null;
            String resStr = config.getString(path + ".result");

            if (resStr != null) {
                String[] parts = resStr.split(":", 3);
                if (parts[0].equalsIgnoreCase("CUSTOM")) {
                    Item customItem = ItemHandler.getItemById(parts[1]);
                    if (customItem != null) {
                        resultStack = customItem.buildStack();
                        resultStack.setAmount(Integer.parseInt(parts[2]));
                    }
                } else if (parts[0].equalsIgnoreCase("VANILLA")) {
                    Material mat = Material.matchMaterial(parts[1]);
                    if (mat != null) {
                        resultStack = new ItemStack(mat, Integer.parseInt(parts[2]));
                    }
                }
            }

            if (resultStack == null) {
                resultStack = config.getItemStack(path + ".result_vanilla");
            }

            if (resultStack != null && grid.length >= 9) {
                registerBukkitRecipe(key, grid, resultStack);
            }
        }

        // update recipes
        Bukkit.updateRecipes();
        MineSkyItems.l.info("Carregadas " + registeredKeys.size() + " receitas nativas com sucesso.");
    }

    public static void saveRecipe(String id, String[] gridDescriptors, ItemStack resultStack) {
        String path = "recipes." + id.toLowerCase();
        config.set(path + ".grid", Arrays.asList(gridDescriptors));

        Item customResult = ItemHandler.getItemFromStack(resultStack);
        if (customResult != null) {
            config.set(path + ".result", "CUSTOM:" + customResult.getId() + ":" + resultStack.getAmount());
            config.set(path + ".result_vanilla", null);
        } else {
            config.set(path + ".result", "VANILLA:" + resultStack.getType().name() + ":" + resultStack.getAmount());
            config.set(path + ".result_vanilla", resultStack);
        }

        // register recipes
        registerBukkitRecipe(id.toLowerCase(), gridDescriptors, resultStack);
        Bukkit.updateRecipes();

        saveAsync();
    }

    public static void deleteRecipe(String id) {
        String keyName = id.toLowerCase();
        NamespacedKey key = new NamespacedKey(MineSkyItems.getInstance(), "craft_" + keyName);
        Bukkit.removeRecipe(key);
        registeredKeys.remove(key);

        config.set("recipes." + keyName, null);
        Bukkit.updateRecipes();

        saveAsync();
    }

    private static void registerBukkitRecipe(String recipeId, String[] grid, ItemStack resultStack) {
        NamespacedKey key = new NamespacedKey(MineSkyItems.getInstance(), "craft_" + recipeId.toLowerCase());
        Bukkit.removeRecipe(key);

        int minRow = 3, maxRow = -1, minCol = 3, maxCol = -1;
        for (int i = 0; i < 9; i++) {
            String descriptor = grid[i];
            if (descriptor != null && !descriptor.equalsIgnoreCase("AIR") && !descriptor.isEmpty()) {
                int r = i / 3;
                int c = i % 3;
                minRow = Math.min(minRow, r);
                maxRow = Math.max(maxRow, r);
                minCol = Math.min(minCol, c);
                maxCol = Math.max(maxCol, c);
            }
        }

        if (maxRow == -1) return; // empty

        Map<String, Character> descriptorToChar = new HashMap<>();
        Map<Character, RecipeChoice> charToChoice = new HashMap<>();
        char currentChar = 'A';

        int height = maxRow - minRow + 1;
        String[] shape = new String[height];

        for (int r = minRow; r <= maxRow; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = minCol; c <= maxCol; c++) {
                int slot = r * 3 + c;
                String descriptor = grid[slot];

                if (descriptor == null || descriptor.equalsIgnoreCase("AIR") || descriptor.isEmpty()) {
                    sb.append(' ');
                } else {
                    if (!descriptorToChar.containsKey(descriptor)) {
                        RecipeChoice choice = parseChoice(descriptor);
                        if (choice == null) return;
                        descriptorToChar.put(descriptor, currentChar);
                        charToChoice.put(currentChar, choice);
                        currentChar++;
                    }
                    sb.append(descriptorToChar.get(descriptor));
                }
            }
            shape[r - minRow] = sb.toString();
        }

        ShapedRecipe recipe = new ShapedRecipe(key, resultStack);
        recipe.shape(shape);
        for (Map.Entry<Character, RecipeChoice> entry : charToChoice.entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }

        try {
            Bukkit.addRecipe(recipe);
            registeredKeys.add(key);
        } catch (Exception e) {
            MineSkyItems.l.severe("Erro ao registrar receita '" + recipeId + "': " + e.getMessage());
        }
    }

    private static RecipeChoice parseChoice(String descriptor) {
        String[] parts = descriptor.split(":", 3);
        if (parts.length < 2) return null;

        final int amount = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;

        if (parts[0].equalsIgnoreCase("CUSTOM")) {
            Item customItem = ItemHandler.getItemById(parts[1]);
            if (customItem != null) {
                ItemStack stack = customItem.buildStack();
                stack.setAmount(amount);
                return new RecipeChoice.ExactChoice(stack);
            }
        } else if (parts[0].equalsIgnoreCase("VANILLA")) {
            Material mat = Material.matchMaterial(parts[1]);
            if (mat != null) {
                return new RecipeChoice.MaterialChoice(mat);
            }
        }
        return null;
    }

    private static void saveAsync() {
        Bukkit.getServer().getAsyncScheduler().runNow(MineSkyItems.getInstance(), task -> {
            try {
                config.save(file);
            } catch (IOException e) {
                MineSkyItems.l.severe("Erro ao salvar recipes.yml: " + e.getMessage());
            }
        });
    }

    public static Map<String, Object> getRecipes() {
        return config.contains("recipes") ? config.getConfigurationSection("recipes").getValues(false) : Collections.emptyMap();
    }
}