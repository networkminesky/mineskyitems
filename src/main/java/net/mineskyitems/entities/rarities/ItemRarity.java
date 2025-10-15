package net.mineskyitems.entities.rarities;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public class ItemRarity {

    private final String name;
    private final boolean glowing;

    private final int maxLevel;

    private final boolean customFont;

    private String tooltip;

    private final ConfigurationSection config;

    private final String id;
    private final String color;

    public ItemRarity(String id, ConfigurationSection config) {
        this.id = id;
        this.config = config;

        this.name = config.getString("name", id);
        this.glowing = config.getBoolean("glowing", false);
        this.customFont = config.contains("custom-font", false);
        this.color = config.getString("color", "#ffffff");

        this.tooltip = config.getString("tooltip", null);

        this.maxLevel = config.getInt("max-level", 15);
    }

    @Nullable
    public String getTooltip() {
        return tooltip;
    }

    public boolean hasTooltip() {
        return tooltip != null && !tooltip.isBlank();
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public String getName() {
        return name;
    }

    public boolean hasCustomFont() {
        return customFont;
    }

    public boolean shouldHaveGlowing() {
        return glowing;
    }

    public ConfigurationSection getConfig() {return config;}
    public String getId() {return id;}

    public Component getFullComponent() {
        if(customFont)
            return getFontComponent();
        else
            return Component.text(getName())
                    .color(getTextColor());
    }

    public String getChatColor() {
        return ChatColor.of(color)+"";
    }

    public TextColor getTextColor() {
        return TextColor.fromHexString(color);
    }

    public static final Key KEY = Key.key(Key.MINECRAFT_NAMESPACE, "rarity");

    public Component getFontComponent() {
        return Component.text(getConfig().getString("custom-font.char", "Z"))
                .color(NamedTextColor.WHITE)
                .font(KEY)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

}
