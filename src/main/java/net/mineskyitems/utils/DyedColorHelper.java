package net.mineskyitems.utils;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import net.mineskyitems.entities.item.Item;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class DyedColorHelper {

    private static final Map<Material, DyeColor> MATERIAL_TO_DYE = new EnumMap<>(Material.class);
    private static final Map<DyeColor, Material> DYE_TO_MATERIAL = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor dyeColor : DyeColor.values()) {
            try {
                Material material = Material.valueOf(dyeColor.name() + "_DYE");
                MATERIAL_TO_DYE.put(material, dyeColor);
                DYE_TO_MATERIAL.put(dyeColor, material);
            } catch (IllegalArgumentException ignored) {
                // Caso alguma cor futura não tenha material correspondente
            }
        }
    }

    private DyedColorHelper() {}

    public static boolean isDyeMaterial(@NotNull Material material) {
        return MATERIAL_TO_DYE.containsKey(material);
    }

    public static @Nullable DyeColor getDyeColor(@NotNull Material material) {
        return MATERIAL_TO_DYE.get(material);
    }

    /**
     * Equivalente ao que o getItemMaterial tentava fazer (obter o Material a partir da DyeColor).
     */
    public static @Nullable Material getDyeMaterial(@NotNull DyeColor dyeColor) {
        return DYE_TO_MATERIAL.get(dyeColor);
    }

    public static boolean canBeDyed(@NotNull Item item, @NotNull ItemStack stack) {
        return item.getDyedColor() != null || stack.hasData(DataComponentTypes.DYED_COLOR);
    }

    public static @Nullable Color resolveColor(@NotNull ItemStack stack, @Nullable Item item) {
        if (stack.hasData(DataComponentTypes.DYED_COLOR)) {
            return stack.getData(DataComponentTypes.DYED_COLOR).color();
        }
        return colorFromItemConfig(item);
    }

    public static @Nullable Color colorFromItemConfig(@Nullable Item item) {
        if (item == null || item.getDyedColor() == null) {
            return null;
        }
        final Item.RGBEntry entry = item.getDyedColor();
        return Color.fromRGB(entry.r(), entry.g(), entry.b());
    }

    public static void applyColor(@NotNull ItemStack stack, @NotNull Color color, @Nullable DyeColor dye) {
        stack.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor()
                .color(color)
                .build());
        stack.setData(DataComponentTypes.DYE, dye != null ? dye : DyeColor.WHITE);
    }

    public static void applyFromItemConfig(@NotNull ItemStack stack, @NotNull Item item) {
        final Color color = colorFromItemConfig(item);
        if (color != null) {
            applyColor(stack, color, null);
        }
    }

    public static void clearColor(@NotNull ItemStack stack) {
        stack.resetData(DataComponentTypes.DYED_COLOR);
        stack.resetData(DataComponentTypes.DYE);
    }

    public static void applyDye(@NotNull ItemStack stack, @NotNull Item item, @NotNull DyeColor dye) {
        Color current = resolveColor(stack, item);
        if (current == null) {
            current = Color.fromRGB(0xFFFFFF);
        }

        final Color dyeRgb = dye.getColor();
        final Color mixed = Color.fromRGB(
                Math.min(255, current.getRed() + dyeRgb.getRed()),
                Math.min(255, current.getGreen() + dyeRgb.getGreen()),
                Math.min(255, current.getBlue() + dyeRgb.getBlue())
        );
        applyColor(stack, mixed, dye);
    }
}