package net.mineskyitems.entities.item;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class AttributeOverrider {

    public static final record Overrider(String name, double absolute, double divider, double multiplier) {}

    private final Set<Overrider> overriders;

    public AttributeOverrider(final ConfigurationSection section) {
        this.overriders = new HashSet<>();
        if(section == null)
            return;

        for(String name : section.getKeys(false)) {
            ConfigurationSection overriderSection = section.getConfigurationSection(name);
            if(overriderSection == null)
                continue;

            this.overriders.add(new Overrider(
                    name,
                    overriderSection.getDouble("absolute", -1),
                    overriderSection.getDouble("divider", -1),
                    overriderSection.getDouble("multiplier", -1)
            ));
        }
    }

    public Set<Overrider> getOverriders() {
        return overriders;
    }
}
