package net.minesky.entities.tooltip;

import net.minesky.MineSkyItems;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TooltipHandler {

    public static ArrayList<Tooltip> tooltips = new ArrayList<>();

    public static File file = new File(MineSkyItems.getInstance().getDataFolder(), "tooltips.yml");
    public static YamlConfiguration configuration;

    public static Tooltip getTooltipById(String id) {
        return tooltips.stream()
                .filter(element -> element.getId().equals(id))
                .findFirst().orElse(null);
    }

    public static void setupTooltips() {
        configuration = YamlConfiguration.loadConfiguration(file);
        if (!file.exists()) {
            try {
                MineSkyItems.l.info("Arquivo tooltips.yml criado!");
                configuration.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        configuration = YamlConfiguration.loadConfiguration(file);

        for(String key : configuration.getKeys(false)) {

            Tooltip tooltip = new Tooltip(key, configuration.getStringList(key));

            MineSkyItems.l.info("| Carregando tooltip "+tooltip.getId());

            tooltips.add(tooltip);

        }
    }

}
