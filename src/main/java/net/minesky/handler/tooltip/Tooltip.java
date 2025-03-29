package net.minesky.handler.tooltip;

import net.minesky.handler.Item;
import net.minesky.handler.ItemAttributes;
import net.minesky.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
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

    public List<String> getFormattedLore(Item item) {
        final ItemAttributes attributes = item.getItemAttributes();

        List<String> definitiveLore = new ArrayList<>();

        for(String s : getRawFormat()) {
            if(s.isBlank() || s.isEmpty()) {
                definitiveLore.add(s);
                continue;
            }
            final String classes = item.getRequiredClasses().toString();

            s = s.replace("%rarity%", "&f"+item.getRarity());

            s = s.replace("%classes%", classes.substring(1, classes.length() - 1));
            s = s.replace("%level%", item.getRequiredLevel()+"");

            s = s.replace("%damage%", attributes.getDamage()+"");
            s = s.replace("%speed%", attributes.getSpeed() + "");

            if(s.equalsIgnoreCase("%lore%")) {
                final List<String> lore = item.getMetadata().lore();
                if(!lore.isEmpty())
                    definitiveLore.addAll(lore);
                continue;
            }

            definitiveLore.add(s);
        }

        definitiveLore = definitiveLore.stream()
                .map(a -> Utils.c("&7"+a))
                .collect(Collectors.toList());

        return definitiveLore;
    }

    public List<String> getRawFormat() {
        return format;
    }
}
