package net.minesky.logics;

import net.minesky.MineSkyItems;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.w3c.dom.Attr;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class LevelCurves {

    // Level maximo que pode ser atingido,
    // hardcoded para evitar modificações desnecessárias.
    public static final int maxLevel = 100;

    public static File file = new File(MineSkyItems.getInstance().getDataFolder(), "default-curve.yml");
    public static YamlConfiguration configuration;

    public static void setupCurves() {
        if(!file.exists())
            saveDefaultFile();

        configuration = YamlConfiguration.loadConfiguration(file);

        cacheCurves();
    }

    protected static HashMap<Attribute, List<Double>> curves = new HashMap<>();
    private static void cacheCurves() {
        for(Attribute attribute : Attribute.values()) {
            List<Double> doubles = new ArrayList<>();
            for(String s : configuration.getStringList(attribute.getKey().getKey())) {
                try {
                    double d = Double.parseDouble(s);
                    doubles.add(d);
                } catch(Exception ignored) {}
            }
            curves.put(attribute, doubles);
        }
    }

    public static List<Double> getCurves(Attribute attribute) {
        return curves.getOrDefault(attribute, List.of((double) 0));
    }

    public static double calculateValue(int level, Attribute attribute) {
        final List<Double> curves = getCurves(attribute);

        int size = curves.size();
        double step = (double) maxLevel / (size - 1); // Define os intervalos entre cada valor

        int lowerIndex = Math.min((int) (level / step), size - 2); // Índice do menor valor dentro do range
        int upperIndex = lowerIndex + 1; // Próximo índice

        double lowerValue = curves.get(lowerIndex);
        double upperValue = curves.get(upperIndex);

        double factor = (level % step) / step; // Posição relativa entre os dois pontos

        return lowerValue + factor * (upperValue - lowerValue);
    }

    public static void saveDefaultFile() {
        final String fileName = "default-curve.yml";
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
