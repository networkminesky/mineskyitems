package net.minesky.entities.rarities;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public class ItemRarity {

    private final String name;
    private final boolean glowing;

    private final int maxLevel;

    private final boolean customFont;

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

        this.maxLevel = config.getInt("max-level", 15);
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
                    .style(getColor());
    }

    public String getChatColor() {
        return ChatColor.of(color)+"";
    }

    public Style getColor() {
        return Style.style(TextColor.fromHexString(color));
    }

    public static final Key KEY = Key.key(Key.MINECRAFT_NAMESPACE, "rarity");

    public Component getFontComponent() {
        return Component.text(getConfig().getString("custom-font.char", "Z"))
                .color(NamedTextColor.WHITE)
                .font(KEY);
    }

}
