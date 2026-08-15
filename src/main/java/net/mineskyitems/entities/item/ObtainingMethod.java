package net.mineskyitems.entities.item;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ObtainingMethod {
    LOOT("Loot"),
    CRAFTING("Crafting"),
    MOB_DROP("Drop de Mobs"),
    UPGRADE("Upgrade de Item"),
    UNKNOWN("Desconhecido"),
    UNOBTAINABLE("Impossível");

    private final String name;

    ObtainingMethod(String name) {
        this.name = name;
    };

    public String getName() {
        return this.name;
    }

    public static @NotNull ObtainingMethod fromValueOrUnknown(String obtainingMethod) {
        if (obtainingMethod == null) {
            return UNKNOWN;
        }
        try {
            return ObtainingMethod.valueOf(obtainingMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public static @Nullable ObtainingMethod fromValue(String obtainingMethod) {
        if (obtainingMethod == null) {
            return null;
        }
        try {
            return ObtainingMethod.valueOf(obtainingMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
