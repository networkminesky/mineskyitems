package net.minesky.logics;

import net.minesky.MineSkyItems;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LevelCurvesLogic {

    // Level maximo que pode ser atingido,
    // hardcoded para evitar modificações desnecessárias.
    public static final int maxLevel = 100;

    public static File file = new File(MineSkyItems.getInstance().getDataFolder(), "default-curves.yml");
    public static YamlConfiguration configuration;

    public static void setupCurves() {
        if(!file.exists())
            saveDefaultFile();

        configuration = YamlConfiguration.loadConfiguration(file);

        cacheCurves();
    }

    private static String translateDots(String z) {
        return z.replace(".", "_").toUpperCase();
    }

    //   KEY (STRING or ATTRIBUTE) | DOUBLE CURVES
    protected static HashMap<String, List<Double>> curves = new HashMap<>();

    public static final String ITEM_DURABILITY_CURVE = "ITEM_DURABILITY";
    public static final String ANOTHER_ONE = "idk";

    private static void cacheCurves() {
        for(Attribute attribute : Attribute.values()) {
            final String translatedDots = translateDots(attribute.getKey().getKey());
            registerCurve(translatedDots);
        }

        registerCurve(ITEM_DURABILITY_CURVE);
    }

    private static void registerCurve(String key) {
        List<Double> doubles = new ArrayList<>();

        MineSkyItems.l.info("| Caching a curva de nivel de "+key);

        for(String s : configuration.getStringList(key)) {
            try {
                double d = Double.parseDouble(s);
                MineSkyItems.l.info("  | Curve: "+d);
                doubles.add(d);
            } catch(Exception ignored) {}
        }
        curves.put(key, doubles);
    }

    private static List<Double> getCurves(String key) {
        return curves.getOrDefault(key, List.of((double) 0));
    }

    public static double calculateValue(int level, Attribute attribute) {
        return calculateValue(level, translateDots(attribute.getKey().getKey()));
    }
    public static double calculateValue(int level, String key) {
        final List<Double> curves = getCurves(key);

        if (curves == null || curves.isEmpty()) {
            return 1.0;
        }

        int size = curves.size();
        if (size == 1) {
            return curves.get(0);
        }

        double step = (double) maxLevel / (size - 1);

        int lowerIndex = Math.min((int) (level / step), size - 2);
        int upperIndex = lowerIndex + 1;

        double lowerValue = curves.get(lowerIndex);
        double upperValue = curves.get(upperIndex);

        double posLower = lowerIndex * step;
        double factor = (level - posLower) / step;

        // Interpolação
        return lowerValue + factor * (upperValue - lowerValue);
    }

    public static void saveDefaultFile() {
        final String fileName = "default-curves.yml";
        InputStream in = MineSkyItems.getInstance().getResource(fileName);

        if (in == null) {
            return;
        } else {
            File outFile = new File(MineSkyItems.getInstance().getDataFolder(), fileName);

            try {
                if (!outFile.exists()) {
                    OutputStream out = new FileOutputStream(outFile);
                    byte[] buf = new byte[1024];

                    int len;
                    while((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    out.close();
                    in.close();
                }
            } catch (IOException ignored) {}
        }
    }


}
