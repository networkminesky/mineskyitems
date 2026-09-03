package net.mineskyitems.entities.curves;

import net.mineskyitems.entities.item.AttributeOverrider;
import net.mineskyitems.entities.item.Item;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemCurve {

    private final String id;

    private final File file;
    private final YamlConfiguration configuration;

    //   KEY (STRING or ATTRIBUTE) | DOUBLE CURVES
    private final HashMap<String, List<Double>> curves = new HashMap<>();

    public String getId() {
        return this.id;
    }

    public HashMap<String, List<Double>> getAllCurves() {
        return curves;
    }

    public double calculateValue(final Item item, int level, Attribute attribute) {
        return calculateValue(item, level, CurveHandler.translateDots(attribute.getKey().getKey()));
    }
    public double calculateValue(final Item item, int level, String key) {
        final List<Double> curves = getCurve(key);

        if (curves == null || curves.isEmpty()) {
            return 0.0;
        }

        int size = curves.size();
        if (size == 1) {
            return curves.get(0);
        }

        double step = (double) CurveHandler.maxLevel / (size - 1);

        int lowerIndex = Math.min((int) (level / step), size - 2);
        int upperIndex = lowerIndex + 1;

        double lowerValue = curves.get(lowerIndex);
        double upperValue = curves.get(upperIndex);

        double posLower = lowerIndex * step;
        double factor = (level - posLower) / step;

        // Interpolação
        double result = lowerValue + factor * (upperValue - lowerValue);
        if(!item.hasAttributeOverrider())
            return result;

        AttributeOverrider.Overrider overrider = item.getAttributeOverrider().getOverriders().stream()
                .filter(e -> e.name().equalsIgnoreCase(key))
                .findFirst().orElse(null);
        if(overrider == null)
            return result;

        if(overrider.absolute() != -1) {
            result = overrider.absolute();
        } else {
            if(overrider.divider() != -1) {
                result = result/overrider.divider();
            }
            if(overrider.multiplier() != -1) {
                result = result*overrider.multiplier();
            }
        }

        return result;
    }

    public ItemCurve(final String id) {
        this.id = id;

        // id com yml
        this.file = new File(CurveHandler.folder, id);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.configuration = YamlConfiguration.loadConfiguration(file);


        for(Attribute attribute : Attribute.values()) {
            final String translatedDots = CurveHandler.translateDots(attribute.getKey().getKey());
            registerCurve(translatedDots);
        }

        // Specific Plugin Curves
        registerCurve(CurveHandler.ITEM_DURABILITY_CURVE);
        registerCurve(CurveHandler.SKILL_DAMAGE_CURVE);
        registerCurve(CurveHandler.ARROW_DAMAGE_CURVE);

        registerCurve(CurveHandler.TOOL_SPEED);
        registerCurve(CurveHandler.DEFAULT_TOOL_SPEED);
    }

    public List<Double> getCurve(String key) {
        return curves.getOrDefault(key, List.of((double) 0));
    }

    public void registerCurve(String key) {
        List<Double> doubles = new ArrayList<>();
        for(String s : configuration.getStringList(key)) {
            try {
                double d = Double.parseDouble(s);
                doubles.add(d);
            } catch(Exception ignored) {}
        }
        curves.put(key, doubles);
    }

}
