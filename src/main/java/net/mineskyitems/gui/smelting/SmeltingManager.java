package net.mineskyitems.gui.smelting;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SmeltingManager {

    private static final File file = new File(MineSkyItems.getInstance().getDataFolder(), "smelting.yml");
    private static final Set<NamespacedKey> registeredKeys = new HashSet<>();
    private static YamlConfiguration config;

    public static void loadRecipes() {
        boolean isStartup = true;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().toLowerCase().contains("plugman")) {
                isStartup = false;
                break;
            }
        }

        if (isStartup) {
            for (NamespacedKey key : registeredKeys) {
                Bukkit.removeRecipe(key);
            }
        }
        registeredKeys.clear();

        if (!file.exists()) {
            MineSkyItems.getInstance().saveResource("smelting.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("smelting")) return;

        for (String key : config.getConfigurationSection("smelting").getKeys(false)) {
            String path = "smelting." + key;

            String inputDescriptor = config.getString(path + ".input");
            String resStr = config.getString(path + ".result");

            boolean furnace = config.getBoolean(path + ".furnace", true);
            boolean blastFurnace = config.getBoolean(path + ".blast_furnace", true);
            boolean smoker = config.getBoolean(path + ".smoker", false);
            boolean campfire = config.getBoolean(path + ".campfire", false);

            int cookingTime = config.getInt(path + ".cooking_time", 200);
            float experience = (float) config.getDouble(path + ".experience", 0.1);

            ItemStack resultStack = parseResultStack(resStr, config.getItemStack(path + ".result_vanilla"));

            if (inputDescriptor != null && resultStack != null) {
                // Registra no Bukkit APENAS no startup[cite: 2]
                if (isStartup) {
                    registerBukkitSmelting(key, inputDescriptor, resultStack, furnace, blastFurnace, smoker, campfire, cookingTime, experience);
                }
            } else {
                MineSkyItems.l.warning("Receita de smelting '" + key + "' ignorada: input ou resultado inválidos.");
            }
        }

        if (isStartup) {
            Bukkit.updateRecipes();
            MineSkyItems.l.info("Carregadas " + registeredKeys.size() + " receitas de queima (smelting) com sucesso.");
        } else {
            MineSkyItems.l.info("Plugman detectado: receitas de smelting carregadas apenas na memoria.");
        }
    }

    public static void saveRecipe(String id, String inputDescriptor, ItemStack resultStack,
                                  boolean furnace, boolean blastFurnace, boolean smoker, boolean campfire,
                                  int cookingTime, float experience) {
        String path = "smelting." + id.toLowerCase();

        config.set(path + ".input", inputDescriptor);
        config.set(path + ".furnace", furnace);
        config.set(path + ".blast_furnace", blastFurnace);
        config.set(path + ".smoker", smoker);
        config.set(path + ".campfire", campfire);
        config.set(path + ".cooking_time", cookingTime);
        config.set(path + ".experience", experience);

        Item customResult = ItemHandler.getItemFromStack(resultStack);
        if (customResult != null) {
            config.set(path + ".result", "CUSTOM:" + customResult.getId() + ":" + resultStack.getAmount());
            config.set(path + ".result_vanilla", null);
        } else {
            config.set(path + ".result", "VANILLA:" + resultStack.getType().name() + ":" + resultStack.getAmount());
            config.set(path + ".result_vanilla", resultStack);
        }

        registerBukkitSmelting(id.toLowerCase(), inputDescriptor, resultStack, furnace, blastFurnace, smoker, campfire, cookingTime, experience);
        Bukkit.updateRecipes();

        saveAsync();
    }

    public static void deleteRecipe(String id) {
        String baseKey = id.toLowerCase();
        removeIfRegistered("smelt_f_" + baseKey);
        removeIfRegistered("smelt_b_" + baseKey);
        removeIfRegistered("smelt_s_" + baseKey);
        removeIfRegistered("smelt_c_" + baseKey);

        config.set("smelting." + baseKey, null);
        Bukkit.updateRecipes();

        saveAsync();
    }

    private static void registerBukkitSmelting(String recipeId, String inputDescriptor, ItemStack resultStack,
                                               boolean furnace, boolean blastFurnace, boolean smoker, boolean campfire,
                                               int cookingTime, float experience) {
        RecipeChoice choice = parseChoice(inputDescriptor);
        if (choice == null || resultStack == null) {
            MineSkyItems.l.severe("Erro ao registrar smelting '" + recipeId + "': ingrediente '" + inputDescriptor + "' inválido.");
            return;
        }

        String baseKey = recipeId.toLowerCase();

        if (furnace) {
            NamespacedKey key = NamespacedKey.fromString("msirecipes:smelt_f_" + baseKey);
            Bukkit.removeRecipe(key);
            try {
                FurnaceRecipe recipe = new FurnaceRecipe(key, resultStack, choice, experience, cookingTime);
                Bukkit.addRecipe(recipe);
                registeredKeys.add(key);
            } catch (Exception e) {
                MineSkyItems.l.severe("Erro ao registrar fornalha para '" + recipeId + "': " + e.getMessage());
            }
        }

        if (blastFurnace) {
            NamespacedKey key = NamespacedKey.fromString("msirecipes:smelt_b_" + baseKey);
            Bukkit.removeRecipe(key);
            try {
                BlastingRecipe recipe = new BlastingRecipe(key, resultStack, choice, experience, Math.max(1, cookingTime / 2));
                Bukkit.addRecipe(recipe);
                registeredKeys.add(key);
            } catch (Exception e) {
                MineSkyItems.l.severe("Erro ao registrar alto-forno para '" + recipeId + "': " + e.getMessage());
            }
        }

        if (smoker) {
            NamespacedKey key = NamespacedKey.fromString("msirecipes:smelt_s_" + baseKey);
            Bukkit.removeRecipe(key);
            try {
                SmokingRecipe recipe = new SmokingRecipe(key, resultStack, choice, experience, Math.max(1, cookingTime / 2));
                Bukkit.addRecipe(recipe);
                registeredKeys.add(key);
            } catch (Exception e) {
                MineSkyItems.l.severe("Erro ao registrar defumador para '" + recipeId + "': " + e.getMessage());
            }
        }

        if (campfire) {
            NamespacedKey key = NamespacedKey.fromString("msirecipes:smelt_c_" + baseKey);
            Bukkit.removeRecipe(key);
            try {
                CampfireRecipe recipe = new CampfireRecipe(key, resultStack, choice, experience, cookingTime * 2);
                Bukkit.addRecipe(recipe);
                registeredKeys.add(key);
            } catch (Exception e) {
                MineSkyItems.l.severe("Erro ao registrar fogueira para '" + recipeId + "': " + e.getMessage());
            }
        }
    }

    private static RecipeChoice parseChoice(String descriptor) {
        if (descriptor == null) return null;
        String clean = descriptor.trim();
        if (clean.isEmpty()) return null;

        String[] parts = clean.split(":");

        if (parts[0].equalsIgnoreCase("CUSTOM") || parts[0].equalsIgnoreCase("MINESKYITEM")) {
            if (parts.length >= 2) {
                Item customItem = ItemHandler.getItemById(parts[1].trim());
                if (customItem != null) {
                    ItemStack stack = customItem.buildStack();
                    stack.setAmount(1);
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

    public static ItemStack parseResultStack(String resStr, ItemStack fallbackVanilla) {
        if (resStr == null || resStr.trim().isEmpty()) {
            return fallbackVanilla;
        }

        String clean = resStr.trim();
        String[] parts = clean.split(":");

        if (parts[0].equalsIgnoreCase("CUSTOM") || parts[0].equalsIgnoreCase("MINESKYITEM")) {
            if (parts.length >= 2) {
                Item customItem = ItemHandler.getItemById(parts[1].trim());
                if (customItem != null) {
                    ItemStack stack = customItem.buildStack();
                    int amount = parts.length >= 3 ? parseAmountSafe(parts[2]) : 1;
                    stack.setAmount(amount);
                    return stack;
                }
            }
            return fallbackVanilla;
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

        return fallbackVanilla;
    }

    private static Material resolveMaterial(String descriptor) {
        String s = descriptor.trim();
        if (s.toLowerCase().startsWith("minecraft:")) {
            s = s.substring(10);
        }

        String[] parts = s.split(":");
        String matName;

        if (parts[0].equalsIgnoreCase("VANILLA") && parts.length >= 2) {
            matName = parts[1].trim();
        } else {
            matName = parts[0].trim();
        }

        Material mat = Material.matchMaterial(matName);
        if (mat == null) {
            mat = Material.matchMaterial(matName.toUpperCase());
        }
        return mat;
    }

    private static int parseAmountSafe(String str) {
        try {
            return Math.max(1, Integer.parseInt(str.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static void removeIfRegistered(String keyString) {
        NamespacedKey key = NamespacedKey.fromString("msirecipes:" + keyString);
        Bukkit.removeRecipe(key);
        registeredKeys.remove(key);
    }

    private static void saveAsync() {
        Bukkit.getServer().getAsyncScheduler().runNow(MineSkyItems.getInstance(), task -> {
            try {
                config.save(file);
            } catch (IOException e) {
                MineSkyItems.l.severe("Erro ao salvar smelting.yml: " + e.getMessage());
            }
        });
    }

    public static Map<String, Object> getRecipes() {
        return config.contains("smelting") ? config.getConfigurationSection("smelting").getValues(false) : Collections.emptyMap();
    }
}