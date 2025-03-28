package net.minesky.handler;

import net.minesky.handler.categories.Category;
import net.minesky.handler.categories.CategoryHandler;
import net.minesky.utils.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private Material material = Material.IRON_AXE;
    private String displayName = UUID.randomUUID().toString();
    private List<String> lore;

    private List<Item.ItemSkill> itemSkills = new ArrayList<>();

    private int customModel = 0;
    private List<String> playerClass = new ArrayList<>();;

    private int itemLevel = -1;

    private final Category category;

    public ItemBuilder(Category category) {
        this.category = category;
    }

    public String generateId() {
        return displayName.toLowerCase().replace("/\s+/g", "_").replace("/[^w-]+/g", "");
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Category getCategory() {return category;}
    public int getItemLevel() {return itemLevel;}
    public int getCustomModel() {return customModel;}
    public Material getMaterial() {return material;}
    public List<String> getPlayerClass() {return playerClass;}
    public String getDisplayName() {return displayName;}
    public List<String> getLore() {return lore;}
    public List<Item.ItemSkill> getItemSkills() {return itemSkills;}

    public void setItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
    }

    public void setCustomModel(int customModel) {
        this.customModel = customModel;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPlayerClass(List<String> playerClass) {
        this.playerClass = playerClass;
    }
    public void setItemSkills(List<Item.ItemSkill> itemSkills) {
        this.itemSkills = itemSkills;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }
    public void setLore(String... lore) {this.lore = List.of(lore);}

    public ItemBuilder itemLevel(int itemLevel) {
        this.setItemLevel(itemLevel);
        return this;
    }
    public ItemBuilder customModel(int customModel) {
        this.setCustomModel(customModel);
        return this;
    }
    public ItemBuilder material(Material material) {
        this.setMaterial(material);
        return this;
    }
    public ItemBuilder requiredClasses(List<String> classes) {
        this.setPlayerClass(classes);
        return this;
    }
    public ItemBuilder displayName(String displayName) {
        this.setDisplayName(displayName);
        return this;
    }
    public ItemBuilder lore(String... lore) {
        this.setLore(lore);
        return this;
    }
    public ItemBuilder lore(List<String> lore) {
        this.setLore(lore);
        return this;
    }
    public ItemBuilder itemSkills(List<Item.ItemSkill> skills) {
        this.setItemSkills(skills);
        return this;
    }

    public ItemStack getItemStack() {

        ItemStack it = new ItemStack(material);
        ItemMeta im = it.getItemMeta();

        im.setCustomModelData(getCustomModel());
        im.setDisplayName(Utils.c(getDisplayName()));

        it.setItemMeta(im);
        return it;

    }

    private ConfigurationSection setInsideCategory() {
        YamlConfiguration config = getCategory().getConfig();
        final String id = generateId();

        // Remove a configuração do item anterior antes de setar novamente.
        config.set(id, null);

        // Metadata
        config.set(id+".metadata.material", getMaterial().toString());
        config.set(id+".metadata.displayname", getDisplayName());
        config.set(id+".metadata.model", getCustomModel());
        config.set(id+".metadata.lore", getLore());

        // Skills
        int n = 1;
        for(Item.ItemSkill skill : getItemSkills()) {
            final String path = id+".skills."+ n;
            config.set(path+".interaction-type", skill.interactionType().name());
            config.set(path+".cooldown", skill.cooldown());
            config.set(path+".mythic-id", skill.mythicSkillId());
            n++;
        }

        // Other
        config.set(id+".required-level", getItemLevel());
        config.set(id+".required-class", getPlayerClass());

        return config.getConfigurationSection(id);
    }

    public Item build() {
        final String generatedId = generateId();

        ConfigurationSection section = setInsideCategory();

        Item item = new Item(category, generatedId, section);

        category.reloadCategory();

        return item;
    }
}
