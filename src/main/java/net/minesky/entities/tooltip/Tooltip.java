package net.minesky.entities.tooltip;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minesky.entities.item.Item;
import net.minesky.entities.item.ItemAttributes;
import net.minesky.utils.Utils;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Tooltip {

    private final String id;
    private final List<String> format;

    public Tooltip(String id, List<String> format) {
        this.id = id;
        this.format = format;
    }

    public String getId() {
        return id;
    }

    public List<Component> getFormattedLore(Item item) {
        ItemAttributes attributes = item.getItemAttributes();

        List<Component> definitiveLore = new ArrayList<>();

        for(String s : getRawFormat()) {
            if(s.isBlank() || s.isEmpty() || s.contains("%vazio%")) {
                definitiveLore.add(Component.text(" ").style(Style.style()));
                continue;
            }

            if(s.equalsIgnoreCase("%rarity%")) {
                Bukkit.getOnlinePlayers().iterator().next().sendMessage(item.getItemRarity().getFullComponent());
                definitiveLore.add(item.getItemRarity().getFullComponent());
                continue;
            }

            final String classes = item.getRequiredClasses().toString();

            if(item.getRequiredClasses().isEmpty() && s.contains("%classes%"))
                continue;

            s = s.replace("%classes%", classes.substring(1, classes.length() - 1));
            s = s.replace("%level%", item.getRequiredLevel()+"");

            s = s.replace("%damage%", attributes.getDamage()+"");
            s = s.replace("%speed%", attributes.getSpeed() + "");

            s = Utils.c(s);

            if(s.equalsIgnoreCase("%lore%")) {
                final List<String> lore = item.getMetadata().lore();
                if(!lore.isEmpty()) {
                    for(String loreLine : lore) {
                        definitiveLore.add(Component.text(loreLine)
                                .style(Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC)));
                    }
                }
                continue;
            }

            Component textComponent = LegacyComponentSerializer.legacySection().deserialize(s)
                    .style(Style.style());

            definitiveLore.add(textComponent);
        }

        /*definitiveLore = definitiveLore.stream()
                .map(a -> Utils.c("&7"+a))
                .collect(Collectors.toList());*/

        return definitiveLore;
    }

    public List<String> getRawFormat() {
        return format;
    }
}
