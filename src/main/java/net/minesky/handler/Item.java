package net.minesky.handler;

import net.minesky.utils.InteractionType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Item {

    private final YamlConfiguration config;
    private final File file;

    private final List<ItemSkill> itemSkills;
    private final ItemMetadata metadata;

    private final String category;

    public final String getId() {
        return this.file.getName().split("\\.")[0].trim();
    }

    public Item(String category, File file) {
        this.category = category;

        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);

        final ConfigurationSection metadataSec = config.getConfigurationSection("metadata");
        this.metadata = new ItemMetadata(
                Material.valueOf(metadataSec.getString("material", "IRON_AXE")),
                metadataSec.getString("displayname", "Nome inválido"),
                metadataSec.getInt("model", -1),
                metadataSec.getStringList("lore")
        );

        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        this.itemSkills = skillsSection.getKeys(false).stream()
                .map(key -> {
                    ConfigurationSection skill = skillsSection.getConfigurationSection(key);

                    InteractionType interactionType = Arrays.stream(InteractionType.values())
                            .filter(a -> a.name().equalsIgnoreCase(skill.getString("interaction-type", "RIGHT_CLICK")))
                            .findFirst()
                            .orElse(InteractionType.RIGHT_CLICK);

                    return new ItemSkill(
                            key,
                            interactionType,
                            skill.getInt("cooldown"),
                            skill.getString("mythic-id", "")
                    );
                })
                .collect(Collectors.toList());

    }

    public String getCategory() {
        return category;
    }

    public ItemMetadata getMetadata() {
        return metadata;
    }

    public YamlConfiguration getConfig() {
        return config;
    }
    public File getFile() {
        return file;
    }

    public List<ItemSkill> getItemSkills() {
        return itemSkills;
    }

    public record ItemMetadata(Material material,
                               String displayName,
                               int modelData,
                               List<String> lore) {}

    public record ItemSkill(String id,
                            InteractionType interactionType,
                            float cooldown,
                            String mythicSkillId) {}
}
