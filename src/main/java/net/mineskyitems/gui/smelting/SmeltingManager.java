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
        // Desregistra receitas de fornalhas antigas
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
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
                registerBukkitSmelting(key, inputDescriptor, resultStack, furnace, blastFurnace, smoker, campfire, cookingTime, experience);
            }
        }

        Bukkit.updateRecipes();
        MineSkyItems.l.info("Carregadas " + registeredKeys.size() + " receitas de queima (smelting) com sucesso.");
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
        if (choice == null || resultStack == null) return;

        String baseKey = recipeId.toLowerCase();

        if (furnace) {
            NamespacedKey key = new NamespacedKey(MineSkyItems.getInstance(), "smelt_f_" + baseKey);
            Bukkit.removeRecipe(key);
            try {
                FurnaceRecipe recipe = new FurnaceRecipe(key, resultStack, choice, experience, cookingTime);
                Bukkit.addRecipe(recipe);
                registeredKeys.add(key);
            } catch (Exception ignored) {}
        }

        if (blastFurnace) {
            NamespacedKey key = new NamespacedKey(MineSkyItems.getInstance(), "smelt_b_" + baseKey);
            Bukkit.removeRecipe(key);
            try {
                BlastingRecipe recipe = new BlastingRecipe(key, resultStack, choice, experience, Math.max(1, cookingTime / 2));
                Bukkit.addRecipe(recipe);
                registeredKeys.add(key);
            } catch (Exception ignored) {}
        }

        if (smoker) {
            NamespacedKey key = new NamespacedKey(MineSkyItems.getInstance(), "smelt_s_" + baseKey);
            Bukkit.removeRecipe(key);
            try {
                SmokingRecipe recipe = new SmokingRecipe(key, resultStack, choice, experience, Math.max(1, cookingTime / 2));
                Bukkit.addRecipe(recipe);
                registeredKeys.add(key);
            } catch (Exception ignored) {}
        }

        if (campfire) {
            NamespacedKey key = new NamespacedKey(MineSkyItems.getInstance(), "smelt_c_" + baseKey);
            Bukkit.removeRecipe(key);
            try {
                CampfireRecipe recipe = new CampfireRecipe(key, resultStack, choice, experience, cookingTime * 2);
                Bukkit.addRecipe(recipe);
                registeredKeys.add(key);
            } catch (Exception ignored) {}
        }
    }

    private static RecipeChoice parseChoice(String descriptor) {
        String[] parts = descriptor.split(":", 2);
        if (parts.length < 2) return null;

        if (parts[0].equalsIgnoreCase("CUSTOM")) {
            Item customItem = ItemHandler.getItemById(parts[1]);
            if (customItem != null) {
                ItemStack stack = customItem.buildStack();
                stack.setAmount(1);
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

    private static ItemStack parseResultStack(String resStr, ItemStack fallbackVanilla) {
        if (resStr != null) {
            String[] parts = resStr.split(":", 3);
            if (parts[0].equalsIgnoreCase("CUSTOM")) {
                Item customItem = ItemHandler.getItemById(parts[1]);
                if (customItem != null) {
                    ItemStack stack = customItem.buildStack();
                    stack.setAmount(Integer.parseInt(parts[2]));
                    return stack;
                }
            } else if (parts[0].equalsIgnoreCase("VANILLA")) {
                Material mat = Material.matchMaterial(parts[1]);
                if (mat != null) {
                    return new ItemStack(mat, Integer.parseInt(parts[2]));
                }
            }
        }
        return fallbackVanilla;
    }

    private static void removeIfRegistered(String keyString) {
        NamespacedKey key = new NamespacedKey(MineSkyItems.getInstance(), keyString);
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