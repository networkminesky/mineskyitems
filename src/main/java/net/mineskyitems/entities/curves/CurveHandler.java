package net.mineskyitems.entities.curves;

import net.mineskyitems.MineSkyItems;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CurveHandler {

    // Level maximo que pode ser atingido,
    // hardcoded para evitar modificações desnecessárias.
    public static final int maxLevel = 100;

    public static final String ITEM_DURABILITY_CURVE = "ITEM_DURABILITY";
    public static final String ARROW_DAMAGE_CURVE = "ARROW_DAMAGE";

    public static Set<ItemCurve> curves = new HashSet<>();

    public static File folder = new File(MineSkyItems.getInstance().getDataFolder(), "curves");

    public static void setupCurves() {
        folder.mkdirs();

        ItemCurve defaultCurve = new ItemCurve("default-curves.yml");
        curves.add(defaultCurve);

        for(File file : Objects.requireNonNull(folder.listFiles())) {
            final String id = file.getName();

            MineSkyItems.l.info("| Carregando curva "+id);

            ItemCurve curve = new ItemCurve(id);

            MineSkyItems.l.info("   | Carregado com sucesso! Total curves: "+curve.getAllCurves().size());

            curves.add(curve);
        }
    }

    @Nullable
    public static ItemCurve getById(String id) {
        return curves.stream().filter(c -> c.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public static String translateDots(String z) {
        return z.replace(".", "_").toUpperCase();
    }
}
