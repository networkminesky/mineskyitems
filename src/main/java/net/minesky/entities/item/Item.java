package net.minesky.entities.item;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.utils.MythicUtil;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.minesky.MineSkyItems;
import net.minesky.entities.categories.Category;
import net.minesky.entities.rarities.ItemRarity;
import net.minesky.entities.rarities.RarityHandler;
import net.minesky.utils.InteractionType;
import net.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Item {

    private final ConfigurationSection itemSection;

    private final List<ItemSkill> itemSkills;
    private final ItemMetadata metadata;

    private final String id;

    private final Category category;

    private final int levelRequirement;
    private final List<String> requiredClasses;

    private ItemAttributes itemAttributes;
    private ItemRarity itemRarity;

    public ConfigurationSection getConfig() {
        return itemSection;
    }

    public Item(Category category, String id, ConfigurationSection itemSection) {
        this.category = category;
        this.id = id;

        this.itemSection = itemSection;

        final ConfigurationSection metadataSec = itemSection.getConfigurationSection("metadata");
        List<String> lore = new ArrayList<>();
        if(metadataSec.contains("lore"))
            lore = metadataSec.getStringList("lore");

        this.metadata = new ItemMetadata(
                Material.getMaterial(metadataSec.getString("material", getCategory().getDefaultItem().name())),
                metadataSec.getString("displayname", "Nome inválido"),
                metadataSec.getInt("model", -1),
                lore
        );

        this.requiredClasses = itemSection.getStringList("required-class");
        this.levelRequirement = itemSection.getInt("required-level", 0);

        ConfigurationSection skillsSection = itemSection.getConfigurationSection("skills");

        if(skillsSection == null)
            this.itemSkills = new ArrayList<>();
        else
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
                            skill.getInt("cooldown", 0),
                            skill.getString("mythic-id", "")
                    );
                })
                .collect(Collectors.toList());

        this.itemAttributes = new ItemAttributes(this);

        this.itemRarity = RarityHandler.calculateRarityByLevel(levelRequirement);
    }

    public ItemRarity getItemRarity() {
        return itemRarity;
    }

    public ItemAttributes getItemAttributes() {
        return itemAttributes;
    }

    public void onInteraction(Player player, ItemStack itemStack, InteractionType interactionType) {

        PlayerData playerData = MineSkyItems.mmocoreAPI.getPlayerData(player);

        if(!hasClassRequirement(playerData.getProfess().getName()))
            return;

        if(!hasLevelRequirement(playerData.getLevel()))
            return;

        getItemSkills().forEach(itemSkill -> {
            if(itemSkill.interactionType == interactionType) {
                // check cooldown dps

                List<Entity> targets = new ArrayList();
                Entity casterEntity = player;
                Location origin = player.getLocation();
                LivingEntity target = MythicUtil.getTargetedEntity(player);
                targets.add(target);

                String spell = itemSkill.mythicSkillId();

                MythicBukkit.inst().getAPIHelper().castSkill(casterEntity, spell, casterEntity, origin, targets, null, 1.0F);

            }
        });
    }

    public boolean hasLevelRequirement(int level) {
        return level >= this.levelRequirement;
    }
    public boolean hasClassRequirement(String className) {
        if(requiredClasses.isEmpty()) return true;
        return requiredClasses.contains(className);
    }

    public int getRequiredLevel() {
        return this.levelRequirement;
    }
    public List<String> getRequiredClasses() {
        return this.requiredClasses;
    }

    public Category getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    public ItemMetadata getMetadata() {
        return metadata;
    }

    public List<ItemSkill> getItemSkills() {
        return itemSkills;
    }

    public ItemStack buildStack() {
        // Setar os atributos aqui
        ItemStack itemStack = getItemAttributes().translateAndUpdate(new ItemStack(metadata.material()));

        Bukkit.broadcastMessage("speed: "+getItemAttributes().getSpeed() + " | damage: "+getItemAttributes().getDamage());

        ItemMeta im = itemStack.getItemMeta();

        im.getPersistentDataContainer().set(MineSkyItems.NAMESPACED_KEY, PersistentDataType.STRING, getId());

        im.setDisplayName(Utils.c("&#cfe6de"+metadata.displayName()));

        im.lore(getCategory().getTooltip().getFormattedLore(this));

        im.setCustomModelData(metadata.modelData());

        itemStack.setItemMeta(im);

        return itemStack;
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
