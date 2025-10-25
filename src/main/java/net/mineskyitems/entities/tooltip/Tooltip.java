package net.mineskyitems.entities.tooltip;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemAttributes;
import net.mineskyitems.utils.Utils;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

    public List<Component> getFormattedLore(Item item, ItemStack stack) {
        ItemAttributes attributes = item.getItemAttributes();

        List<Component> definitiveLore = new ArrayList<>();

        final Component empty = Component.space().style(Style.style());

        int n = 0;
        for(String s : getRawFormat()) {
            if(s.contains("%empty%") || s.contains("%vazio%")) {
                try {
                    final int check = (n - 1);
                    if (!empty.equals(definitiveLore.get(check))) {
                        definitiveLore.add(empty);
                        n++;
                    }
                } catch (Exception ignore) {}
                continue;
            }

            if(s.equalsIgnoreCase("%rarity%")) {
                Component categoryName = Component.space().append(Component.text(item.getCategory().getName()))
                        .font(Style.DEFAULT_FONT)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                        .color(item.getItemRarity().getTextColor());

                Component full = item.getItemRarity().getFullComponent()
                                .append(categoryName);

                n++;
                definitiveLore.add(full);
                continue;
            }

            final String classes = item.getRequiredClasses().toString();

            if(item.getRequiredClasses().isEmpty() && s.contains("%classes%"))
                continue;

            s = s.replace("%classes%", classes.substring(1, classes.length() - 1));
            s = s.replace("%level%", item.getRequiredLevel()+"");

            final double damage = item.getCategory().getType().equalsIgnoreCase("ranged")
                    ? attributes.getArrowDamage() : attributes.getDamage();

            s = s.replace("%damage%", Utils.format(damage));
            s = s.replace("%speed%", Utils.format(attributes.getSpeed()));

            s = s.replace("%health%", "+"+Utils.format(attributes.getMaxHealth()));

            final int durability = item.getDurability(stack);
            final int maxDurability = item.getMaxDurability();
            final String durString = (maxDurability == durability) ? durability+"" : durability+"/"+maxDurability;

            s = s.replace("%durability%", durString);

            s = Utils.c(s);

            if(s.equalsIgnoreCase("%lore%")) {
                final List<String> lore = item.getMetadata().lore();
                if(!lore.isEmpty()) {
                    for(String loreLine : lore) {
                        definitiveLore.add(Component.text(loreLine)
                                .style(Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC)));
                        n++;
                    }
                }
                continue;
            }

            Component textComponent = LegacyComponentSerializer.legacySection().deserialize(s)
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);

            definitiveLore.add(textComponent);
            n++;
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
