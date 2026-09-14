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

            String[] grid = new String[9];
            Arrays.fill(grid, "AIR");
            for (int i = 0; i < Math.min(gridList.size(), 9); i++) {
                grid[i] = gridList.get(i);
            }

            ItemStack resultStack = null;
            String resStr = config.getString(path + ".result");

            if (resStr != null && !resStr.trim().isEmpty()) {
                resultStack = parseItemStack(resStr);
            }

            if (resultStack == null) {
                resultStack = config.getItemStack(path + ".result_vanilla");
            }

            if (resultStack != null) {
                registerBukkitRecipe(key, grid, resultStack);
            } else {
                MineSkyItems.l.warning("Receita '" + key + "' ignorada: resultado inválido ou nulo.");
            }
        }

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
            if (!isAirDescriptor(descriptor)) {
                int r = i / 3;
                int c = i % 3;
                minRow = Math.min(minRow, r);
                maxRow = Math.max(maxRow, r);
                minCol = Math.min(minCol, c);
                maxCol = Math.max(maxCol, c);
            }
        }

        if (maxRow == -1) {
            MineSkyItems.l.warning("Receita '" + recipeId + "' está vazia e não foi registrada.");
            return;
        }

        Map<String, Character> descriptorToChar = new HashMap<>();
        Map<Character, RecipeChoice> charToChoice = new HashMap<>();
        char currentChar = 'A';

        int height = maxRow - minRow + 1;
        String[] shape = new String[height];

        for (int r = minRow; r <= maxRow; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = minCol; c <= maxCol; c++) {
                int slot = r * 3 + c;
                String descriptor = (slot < grid.length) ? grid[slot] : "AIR";

                if (isAirDescriptor(descriptor)) {
                    sb.append(' ');
                } else {
                    String cleanDescriptor = descriptor.trim();
                    if (!descriptorToChar.containsKey(cleanDescriptor)) {
                        RecipeChoice choice = parseChoice(cleanDescriptor);
                        if (choice == null) {
                            MineSkyItems.l.severe("Erro na receita '" + recipeId + "': ingrediente inválido '" + descriptor + "'.");
                            return;
                        }
                        descriptorToChar.put(cleanDescriptor, currentChar);
                        charToChoice.put(currentChar, choice);
                        currentChar++;
                    }
                    sb.append(descriptorToChar.get(cleanDescriptor));
                }
            }
            shape[r - minRow] = sb.toString();
        }

        if(resultStack.isEmpty() || resultStack.getType().isAir())
            return;

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
        if (isAirDescriptor(descriptor)) return null;

        String clean = descriptor.trim();
        String[] parts = clean.split(":");

        if (parts[0].equalsIgnoreCase("CUSTOM")) {
            if (parts.length >= 2) {
                Item customItem = ItemHandler.getItemById(parts[1]);
                if (customItem != null) {
                    int amount = parts.length >= 3 ? parseAmountSafe(parts[2]) : 1;
                    ItemStack stack = customItem.buildStack();
                    stack.setAmount(amount);
                    return new RecipeChoice.ExactChoice(stack);
                }
            }
            return null;
        }

        Material mat = resolveMaterial(clean);
        if (mat != null && mat != Material.AIR) {
            return new RecipeChoice.MaterialChoice(mat);
        }

        return null;
    }

   public static ItemStack parseItemStack(String descriptor) {
        if (isAirDescriptor(descriptor)) {
            return new ItemStack(Material.AIR);
        }

        String clean = descriptor.trim();
        String[] parts = clean.split(":");

        if (parts[0].equalsIgnoreCase("CUSTOM")) {
            if (parts.length >= 2) {
                Item customItem = ItemHandler.getItemById(parts[1]);
                if (customItem != null) {
                    int amount = parts.length >= 3 ? parseAmountSafe(parts[2]) : 1;
                    ItemStack stack = customItem.buildStack();
                    stack.setAmount(amount);
                    return stack;
                }
            }
            return new ItemStack(Material.AIR);
        }

        Material mat = resolveMaterial(clean);
        if (mat != null && mat != Material.AIR) {
            int amount = 1;
            if (parts.length >= 3 && parts[0].equalsIgnoreCase("VANILLA")) {
                amount = parseAmountSafe(parts[2]);
            } else if (parts.length >= 2 && !parts[0].equalsIgnoreCase("VANILLA") && !clean.toLowerCase().startsWith("minecraft:")) {
                amount = parseAmountSafe(parts[1]);
            }
            return new ItemStack(mat, amount);
        }

        return new ItemStack(Material.AIR);
    }

    private static Material resolveMaterial(String descriptor) {
        String s = descriptor.trim();
        if (s.toLowerCase().startsWith("minecraft:")) {
            s = s.substring(10);
        }

        String[] parts = s.split(":");
        String matName;

        if (parts[0].equalsIgnoreCase("VANILLA") && parts.length >= 2) {
            matName = parts[1];
        } else {
            matName = parts[0];
        }

        Material mat = Material.matchMaterial(matName);
        if (mat == null) {
            mat = Material.matchMaterial(matName.toUpperCase());
        }
        return mat;
    }

    private static boolean isAirDescriptor(String descriptor) {
        if (descriptor == null) return true;
        String s = descriptor.trim().toUpperCase();
        return s.isEmpty() || s.equals("AIR") || s.equals("VANILLA:AIR") || s.startsWith("AIR:") || s.startsWith("VANILLA:AIR:");
    }

    private static int parseAmountSafe(String str) {
        try {
            return Math.max(1, Integer.parseInt(str.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
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