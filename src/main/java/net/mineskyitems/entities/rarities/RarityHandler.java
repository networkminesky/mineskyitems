package net.mineskyitems.entities.rarities;

import net.mineskyitems.MineSkyItems;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public class RarityHandler {

    public static ArrayList<ItemRarity> rarities = new ArrayList<>();

    public static File file = new File(MineSkyItems.getInstance().getDataFolder(), "rarity.yml");
    public static YamlConfiguration configuration;

    public static ItemRarity getRarityById(String id) {
        return rarities.stream()
                .filter(element -> element.getId().equals(id))
                .findFirst().orElse(null);
    }

    public static ItemRarity calculateRarityByLevel(final int level) {
        return rarities.stream()
                .filter(rarity -> level <= rarity.getMaxLevel())
                .min(Comparator.comparingInt(ItemRarity::getMaxLevel))
                .orElse(null);
    }

    public static void setupRarities() {
        configuration = YamlConfiguration.loadConfiguration(file);
        if (!file.exists()) {
            try {
                MineSkyItems.l.info("Arquivo rarity.yml criado!");
                configuration.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        configuration = YamlConfiguration.loadConfiguration(file);

        for(String key : configuration.getKeys(false)) {

            ItemRarity rarity = new ItemRarity(key, configuration.getConfigurationSection(key));

            MineSkyItems.l.info("| Carregando raridade "+rarity.getId() + " com custom font?: "+rarity.hasCustomFont());

            rarities.add(rarity);

        }
    }

}
