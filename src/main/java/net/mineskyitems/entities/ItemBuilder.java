package net.mineskyitems.entities;

import net.kyori.adventure.text.Component;
import net.mineskyitems.entities.categories.Category;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.entities.item.ItemSkill;
import net.mineskyitems.entities.item.ObtainingMethod;
import net.mineskyitems.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemBuilder {

    private Category category;
    private String id;
    private Material material;
    private String displayName;
    private int customModel;
    private List<String> lore;
    private int itemLevel;
    private List<String> playerClass;
    private List<ItemSkill> itemSkills;
    private Item.FoodMetadata foodMetadata;
    private int revision;
    private String forceRarity;
    private boolean noAutoArmor;
    private Set<ObtainingMethod> obtainingMethods;
    private ConfigurationSection rawSection;

    public ItemBuilder(Category category) {
        this.category = category;
        this.material = category.getDefaultItem();
        this.displayName = "Novo Item";
        this.customModel = 0;
        this.lore = new ArrayList<>();
        this.itemLevel = 1;
        this.playerClass = new ArrayList<>();
        this.itemSkills = new ArrayList<>();
        this.revision = 0;
        this.noAutoArmor = false;
        this.obtainingMethods = new HashSet<>();
        this.forceRarity = null;

        if (category.isFood()) {
            this.foodMetadata = new Item.FoodMetadata(4, 2.0f, 1.6f);
        } else {
            this.foodMetadata = null;
        }

        this.id = generateId();
    }

    public ItemBuilder(Item item) {
        this.category = item.getCategory();
        this.id = item.getId();
        this.material = item.getMetadata().material();
        this.displayName = item.getMetadata().displayName();
        this.customModel = item.getMetadata().modelData();
        this.lore = new ArrayList<>(item.getMetadata().lore());
        this.itemLevel = item.getRequiredLevel();
        this.playerClass = new ArrayList<>(item.getRequiredClasses());
        this.itemSkills = new ArrayList<>(item.getItemSkills());
        this.revision = item.getRevision();
        this.noAutoArmor = item.isNoAutoArmor();
        this.obtainingMethods = new HashSet<>(item.getObtainingMethods());
        this.rawSection = item.getConfig();

        if (item.getConfig().contains("force-rarity")) {
            this.forceRarity = item.getConfig().getString("force-rarity");
        } else {
            this.forceRarity = null;
        }

        if (item.getFoodMetadata() != null) {
            this.foodMetadata = new Item.FoodMetadata(
                    item.getFoodMetadata().nutrition(),
                    item.getFoodMetadata().saturation(),
                    item.getFoodMetadata().consumeSeconds()
            );
        } else if (category.isFood()) {
            this.foodMetadata = new Item.FoodMetadata(4, 2.0f, 1.6f);
        } else {
            this.foodMetadata = null;
        }
    }

    public boolean isStub() {
        return displayName == null || displayName.trim().isEmpty() || material == null || material.isAir();
    }

    public String generateId() {
        if (this.id != null && !this.id.trim().isEmpty()) {
            return this.id;
        }
        if (this.displayName == null || this.displayName.trim().isEmpty()) {
            return "item_" + UUID.randomUUID().toString().substring(0, 6);
        }
        String stripped = ChatColor.stripColor(Utils.c(this.displayName)).toLowerCase().trim();
        String normalized = stripped.replace(" ", "_").replaceAll("[^a-z0-9_\\-]", "");
        if (normalized.isEmpty()) {
            return "item_" + UUID.randomUUID().toString().substring(0, 6);
        }
        return normalized;
    }

    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(material != null ? material : category.getDefaultItem());
        ItemMeta im = stack.getItemMeta();
        if (im != null) {
            String name = displayName != null && !displayName.isEmpty() ? displayName : "Novo Item";
            im.displayName(Component.text(Utils.c(name)));
            im.setCustomModelData(customModel > 0 ? customModel : 0);

            List<Component> componentLore = new ArrayList<>();
            if (lore != null) {
                for (String line : lore) {
                    componentLore.add(Component.text(Utils.c(line)));
                }
            }
            im.lore(componentLore);
            stack.setItemMeta(im);
        }
        return stack;
    }

    public Item build() {
        String finalId = generateId();
        this.id = finalId;

        YamlConfiguration config = category.getConfig();
        ConfigurationSection section = config.getConfigurationSection(finalId);
        if (section == null) {
            section = config.createSection(finalId);
        }

        ConfigurationSection metadataSec = section.getConfigurationSection("metadata");
        if (metadataSec == null) {
            metadataSec = section.createSection("metadata");
        }

        metadataSec.set("material", material != null ? material.name() : category.getDefaultItem().name());
        metadataSec.set("displayname", displayName != null ? displayName : "Item");
        metadataSec.set("model", customModel);
        metadataSec.set("lore", lore != null ? lore : new ArrayList<>());

        section.set("required-level", itemLevel);
        section.set("classes", playerClass != null ? playerClass : new ArrayList<>());
        section.set("revision", revision);
        section.set("no-auto-armor", noAutoArmor);

        if (forceRarity != null && !forceRarity.isEmpty()) {
            section.set("force-rarity", forceRarity);
        } else {
            section.set("force-rarity", null);
        }

        if (foodMetadata != null || category.isFood()) {
            Item.FoodMetadata fm = foodMetadata != null ? foodMetadata : new Item.FoodMetadata(4, 2.0f, 1.6f);
            this.foodMetadata = fm;

            ConfigurationSection foodSec = section.getConfigurationSection("food");
            if (foodSec == null) {
                foodSec = section.createSection("food");
            }
            foodSec.set("nutrition", fm.nutrition());
            foodSec.set("saturation", (double) fm.saturation());
            foodSec.set("consume-seconds", (double) fm.consumeSeconds());
        }

        if (itemSkills != null && !itemSkills.isEmpty()) {
            ConfigurationSection skillsSec = section.createSection("skills");
            for (ItemSkill skill : itemSkills) {
                ConfigurationSection sSec = skillsSec.createSection(skill.getId());
                sSec.set("interaction-type", skill.getInteractionType().name());
                sSec.set("cooldown", skill.getCooldown());
                sSec.set("mythic-id", skill.getMythicSkillId());
            }
        } else {
            section.set("skills", null);
        }

        if (obtainingMethods != null && !obtainingMethods.isEmpty()) {
            List<String> list = obtainingMethods.stream().map(ObtainingMethod::name).toList();
            section.set("obtaining-methods", list);
        }

        if (rawSection != null) {
            if (rawSection.contains("attribute-overrider") && !section.contains("attribute-overrider")) {
                section.set("attribute-overrider", rawSection.get("attribute-overrider"));
            }
        }

        category.saveFile();
        category.reloadCategory();

        Item built = ItemHandler.getItemById(finalId);
        if (built != null) {
            return built;
        }

        return new Item(category, finalId, section);
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getCustomModel() {
        return customModel;
    }

    public void setCustomModel(int customModel) {
        this.customModel = customModel;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public int getItemLevel() {
        return itemLevel;
    }

    public void setItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
    }

    public List<String> getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(List<String> playerClass) {
        this.playerClass = playerClass;
    }

    public List<ItemSkill> getItemSkills() {
        return itemSkills;
    }

    public void setItemSkills(List<ItemSkill> itemSkills) {
        this.itemSkills = itemSkills;
    }

    public Item.FoodMetadata getFoodMetadata() {
        return foodMetadata;
    }

    public void setFoodMetadata(Item.FoodMetadata foodMetadata) {
        this.foodMetadata = foodMetadata;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public String getForceRarity() {
        return forceRarity;
    }

    public void setForceRarity(String forceRarity) {
        this.forceRarity = forceRarity;
    }

    public boolean isNoAutoArmor() {
        return noAutoArmor;
    }

    public void setNoAutoArmor(boolean noAutoArmor) {
        this.noAutoArmor = noAutoArmor;
    }

    public Set<ObtainingMethod> getObtainingMethods() {
        return obtainingMethods;
    }

    public void setObtainingMethods(Set<ObtainingMethod> obtainingMethods) {
        this.obtainingMethods = obtainingMethods;
    }
}