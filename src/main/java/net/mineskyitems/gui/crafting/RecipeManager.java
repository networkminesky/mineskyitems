package net.mineskyitems.gui.crafting;

import net.mineskyitems.MineSkyItems;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeManager {

    private static final Map<String, CustomRecipe> recipes = new ConcurrentHashMap<>();
    private static final File file = new File(MineSkyItems.getInstance().getDataFolder(), "recipes.yml");
    private static YamlConfiguration config;

    public static void loadRecipes() {
        recipes.clear();
        if (!file.exists()) {
            MineSkyItems.getInstance().saveResource("recipes.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("recipes")) return;

        for (String key : config.getConfigurationSection("recipes").getKeys(false)) {
            String path = "recipes." + key;
            List<String> ingList = config.getStringList(path + ".grid");
            RecipeIngredient[] ingredients = new RecipeIngredient[9];
            for (int i = 0; i < 9 && i < ingList.size(); i++) {
                ingredients[i] = RecipeIngredient.deserialize(ingList.get(i));
            }

            String resStr = config.getString(path + ".result");
            RecipeIngredient result = RecipeIngredient.deserialize(resStr);
            ItemStack vanillaResult = config.getItemStack(path + ".result_vanilla");

            CustomRecipe recipe = new CustomRecipe(key, ingredients, result, vanillaResult);
            recipes.put(key.toLowerCase(), recipe);
        }
        MineSkyItems.l.info("Carregadas " + recipes.size() + " receitas customizadas com sucesso.");
    }

    public static void saveRecipe(CustomRecipe recipe) {
        recipes.put(recipe.getId().toLowerCase(), recipe);
        String path = "recipes." + recipe.getId();

        List<String> ingList = new ArrayList<>();
        for (RecipeIngredient ing : recipe.getIngredients()) {
            ingList.add(ing != null ? ing.serialize() : "AIR");
        }
        config.set(path + ".grid", ingList);

        ItemStack resultStack = recipe.createResult();
        var customResult = net.mineskyitems.entities.item.ItemHandler.getItemFromStack(resultStack);

        if (customResult != null) {
            config.set(path + ".result", "CUSTOM:" + customResult.getId() + ":" + resultStack.getAmount());
            config.set(path + ".result_vanilla", null);
        } else {
            config.set(path + ".result", "VANILLA:" + resultStack.getType().name() + ":" + resultStack.getAmount());
            config.set(path + ".result_vanilla", resultStack);
        }

        saveAsync();
    }

    public static void deleteRecipe(String id) {
        recipes.remove(id.toLowerCase());
        config.set("recipes." + id, null);
        saveAsync();
    }

    private static void saveAsync() {
        // Folia Async Scheduler (Thread-Safe File IO)
        Bukkit.getServer().getAsyncScheduler().runNow(MineSkyItems.getInstance(), task -> {
            try {
                config.save(file);
            } catch (IOException e) {
                MineSkyItems.l.severe("Erro ao salvar recipes.yml: " + e.getMessage());
            }
        });
    }

    public static CustomRecipe matchRecipe(ItemStack[] matrix) {
        if (matrix == null) return null;
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.matches(matrix)) {
                return recipe;
            }
        }
        return null;
    }

    public static Map<String, CustomRecipe> getRecipes() {
        return recipes;
    }
}