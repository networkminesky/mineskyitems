package net.minesky.entities.item;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.utils.MythicUtil;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minesky.MineSkyItems;
import net.minesky.entities.categories.Category;
import net.minesky.entities.rarities.ItemRarity;
import net.minesky.entities.rarities.RarityHandler;
import net.minesky.logics.LevelCurvesLogic;
import net.minesky.utils.InteractionType;
import net.minesky.utils.Utils;
import net.minesky.utils.cooldown.CooldownManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.awt.*;
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

        if(getConfig().contains("force-rarity"))
            this.itemRarity = RarityHandler.getRarityById(getConfig().getString("force-rarity", "common"));
        else
            this.itemRarity = RarityHandler.calculateRarityByLevel(levelRequirement);
    }

    public ItemRarity getItemRarity() {
        return itemRarity;
    }

    public ItemAttributes getItemAttributes() {
        return itemAttributes;
    }

    public int getMaxDurability() {
        return (int)Math.round(LevelCurvesLogic.calculateValue(getRequiredLevel(), LevelCurvesLogic.ITEM_DURABILITY_CURVE));
    }
    public int getDurability(ItemStack itemStack) {
        PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();

        if(!container.has(ITEM_DURABILITY, PersistentDataType.INTEGER))
            return 0;

        return container.get(ITEM_DURABILITY, PersistentDataType.INTEGER);
    }

    public boolean isItemBroken(ItemStack itemStack) {
        return getDurability(itemStack) <= 0;
    }

    private void noDurability(Player player) {
        player.sendMessage("§cSeu item está quebrado, você deve repará-lo urgentemente em um ferreiro ou forjador.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1.2f);
    }

    public void damageItem(Player player, ItemStack itemStack, int amount, Cancellable event) {
        int result = (getDurability(itemStack) - amount);

        if(result < 0) {
            event.setCancelled(true);
            noDurability(player);
            return;
        }

        ItemMeta im = itemStack.getItemMeta();
        im.getPersistentDataContainer().set(ITEM_DURABILITY, PersistentDataType.INTEGER, result);
        itemStack.setItemMeta(im);

        im.lore(getCategory().getTooltip().getFormattedLore(this, itemStack));
        itemStack.setItemMeta(im);
    }

    public static NamespacedKey ITEM_DURABILITY = NamespacedKey.fromString("item-durability");
    public void onItemUse(Player player, ItemStack itemStack, Cancellable event) {
        // Som de uso do item
        getCategory().playUseSounds(player);

        // Reduzir durabilidade do item
        if(player.getGameMode() == GameMode.CREATIVE)
            return;

        // Checando encantamento de durabilidade
        if(itemStack.getEnchantments().containsKey(Enchantment.DURABILITY)) {
            int level = itemStack.getEnchantmentLevel(Enchantment.DURABILITY);

            double chance = 100.0 / (level + 1); // Fórmula vanilla de durabilidade do Minecraft
            double roll = Math.random() * 100.0;

            if (roll < chance)
                damageItem(player, itemStack, 1, event);

            return;
        }

        damageItem(player, itemStack, 1, event);
    }

    public void onInteraction(Player player, ItemStack itemStack, InteractionType interactionType, Cancellable event) {

        if(MineSkyItems.MMOCORE_HOOK) {
            PlayerData playerData = MineSkyItems.mmocoreAPI.getPlayerData(player);
            if (!player.hasPermission("mineskyitems.bypass.class-requirement") &&
                    !hasClassRequirement(playerData.getProfess().getName())) {
                player.sendMessage("§cSua classe não possui conhecimento de como usar esse item.");
                return;
            }

            if (!player.hasPermission("mineskyitems.bypass.level-requirement") &&
                    !hasLevelRequirement(playerData.getLevel())) {
                player.sendMessage("§cVocê ainda não possui o nível apropriado para usar esse item.");
                return;
            }
        }

        getItemSkills().stream()
                .filter(skill -> skill.getInteractionType() == interactionType)
                .findFirst()
                .ifPresent(skill -> {
                    if(isItemBroken(itemStack)) {
                        noDurability(player);
                        event.setCancelled(true);
                        return;
                    }

                    // Key feedback
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    event.setCancelled(true);

                    if (CooldownManager.inCooldown(player, skill)) {
                        player.sendMessage("§cEssa magia está em cooldown, aguarde mais "+CooldownManager.getRemainingCooldown(player, skill)+" segundo(s)!");
                        return;
                    }
                    CooldownManager.createCooldown(player, skill, skill.getCooldown());

                    List<Entity> targets = new ArrayList();
                    Entity casterEntity = player;
                    Location origin = player.getLocation();
                    LivingEntity target = MythicUtil.getTargetedEntity(player);
                    targets.add(target);

                    String spell = skill.getMythicSkillId();

                    MythicBukkit.inst().getAPIHelper().castSkill(casterEntity, spell, casterEntity, origin, targets, null, 1.0F);
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
        ItemStack itemStack = new ItemStack(metadata.material());

        if(!getCategory().isNoAttributes())
            itemStack = getItemAttributes().translateAndUpdate(new ItemStack(metadata.material()));

        ItemMeta im = itemStack.getItemMeta();

        im.setUnbreakable(true);
        im.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer container = im.getPersistentDataContainer();
        container.set(MineSkyItems.NAMESPACED_KEY, PersistentDataType.STRING, getId());
        container.set(ITEM_DURABILITY, PersistentDataType.INTEGER, getMaxDurability());
        itemStack.setItemMeta(im);

        Component itemName = Component.text(metadata.displayName())
                .color(getItemRarity().getTextColor())
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        im.displayName(itemName);

        im.lore(getCategory().getTooltip().getFormattedLore(this, itemStack));

        im.setCustomModelData(metadata.modelData());

        itemStack.setItemMeta(im);

        return itemStack;
    }

    public record ItemMetadata(Material material,
                               String displayName,
                               int modelData,
                               List<String> lore) {}
}
