package net.mineskyitems.entities.item;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.utils.MythicUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.categories.Category;
import net.mineskyitems.entities.curves.CurveHandler;
import net.mineskyitems.entities.curves.ItemCurve;
import net.mineskyitems.entities.rarities.ItemRarity;
import net.mineskyitems.entities.rarities.RarityHandler;
import net.mineskyitems.utils.InteractionType;
import net.mineskyitems.utils.Utils;
import net.mineskyitems.utils.cooldown.CooldownManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.crypto.Data;
import java.util.*;
import java.util.stream.Collectors;

public class Item {

    private final ConfigurationSection itemSection;

    private final List<ItemSkill> itemSkills;
    private final ItemMetadata metadata;

    private final String id;

    private final Category category;

    private final int levelRequirement;
    private final List<String> requiredClasses;

    private final ItemAttributes itemAttributes;
    private final ItemRarity itemRarity;

    private final boolean noAutoArmor;

    private final Set<ObtainingMethod> obtainingMethods;

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

        this.noAutoArmor = itemSection.getBoolean("no-auto-armor", false);

        this.metadata = new ItemMetadata(
                Material.getMaterial(metadataSec.getString("material", getCategory().getDefaultItem().name())),
                metadataSec.getString("displayname", "Nome inválido"),
                metadataSec.getInt("model", -1),
                lore
        );

        this.obtainingMethods = new HashSet<>();

        if(itemSection.contains("obtaining-methods")) {
            if(itemSection.isString("obtaining-methods"))
                this.obtainingMethods.add(ObtainingMethod.fromValueOrUnknown(itemSection.getString("obtaining-methods", "UNKNONW")));
            else if(itemSection.isList("obtaining-methods")) {
                for(String s : itemSection.getStringList("obtaining-methods")) {
                    ObtainingMethod method = ObtainingMethod.fromValue(s);
                    if(method != null) this.obtainingMethods.add(method);
                }
            }
        }

        this.requiredClasses = List.of();
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

    public boolean isNoAutoArmor() {
        return this.noAutoArmor;
    }

    public ItemRarity getItemRarity() {
        return itemRarity;
    }

    public ItemAttributes getItemAttributes() {
        return itemAttributes;
    }

    public int getMaxDurability() {
        return (int)Math.round(getCategory().getCurve().calculateValue(getRequiredLevel(), CurveHandler.ITEM_DURABILITY_CURVE));
    }

    public Set<ObtainingMethod> getObtainingMethods() {
        return obtainingMethods;
    }

    public boolean canBeObtained(final ObtainingMethod method) {
        return this.obtainingMethods.contains(method);
    }

    public int getDurability(ItemStack itemStack) {
        if(getCategory().isVanillaDurability()) {
            int max = itemStack.hasData(DataComponentTypes.MAX_DAMAGE)
                    ? itemStack.getData(DataComponentTypes.MAX_DAMAGE)
                    : itemStack.getType().getMaxDurability();

            int damage = itemStack.hasData(DataComponentTypes.DAMAGE)
                    ? itemStack.getData(DataComponentTypes.DAMAGE)
                    : 0;

            return Math.max(0, max - damage);
        }

        return itemStack.getItemMeta().getPersistentDataContainer()
                .getOrDefault(ITEM_DURABILITY, PersistentDataType.INTEGER, 0);
    }

    public boolean isItemBroken(ItemStack itemStack) {
        return getDurability(itemStack) <= 0;
    }

    private void noDurability(Player player, final ItemStack itemStack) {
        player.sendMessage("§cSeu item está quebrado, você deve repará-lo urgentemente em um ferreiro ou forjador.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1.2f);
    }

    public void removeHealthIfBroken(final Player player, final ItemStack itemStack) {
        ItemMeta im = itemStack.getItemMeta();

        if(isItemBroken(itemStack)) {
            im.removeAttributeModifier(Attribute.MAX_HEALTH);
            /*for(Attribute attribute : im.getAttributeModifiers().keySet()) {
                if(attribute == Attribute.MAX_HEALTH) {
                    im.removeAttributeModifier(attribute);
                }
            }*/

            itemStack.setItemMeta(im);
        } else if(im.getAttributeModifiers(Attribute.MAX_HEALTH) == null) {
            //Bukkit.broadcastMessage("botano coiso");
            getItemAttributes().translateAndUpdate(itemStack);
        }
    }

    public void forceDamageItem(Player player, ItemStack itemStack, int amount) {
        if(getCategory().isVanillaDurability()
                && itemStack.hasData(DataComponentTypes.DAMAGE)) {
            final int current = itemStack.getData(DataComponentTypes.DAMAGE);
            final int max = getMaxDurability();
            final int result = (current + amount);

            itemStack.setData(DataComponentTypes.MAX_DAMAGE, getMaxDurability());
            itemStack.setData(DataComponentTypes.DAMAGE, result);

            if(result >= max) {
                itemStack.damage(1, player);
                return;
            }
        }

        int result = (getDurability(itemStack) - amount);
        updateItemOnDamage(itemStack, result);
    }

    public void damageItem(Player player, ItemStack itemStack, int amount, @Nullable Cancellable event) {
        if(this.getCategory().isNoAttributes())
            return;

        final int result = (getDurability(itemStack) - amount);

        if(result < 0) {
            if(event != null) event.setCancelled(true);

            noDurability(player, itemStack);
            return;
        } else if(result <= 30) {
            if(getCategory().shouldShowAlmostBroken())
                player.sendActionBar(
                        Component.text("O item "+getMetadata().displayName+" está quebrando! Possui mais "+result+" usos.")
                                .color(NamedTextColor.RED)
                );
        }

        if(result == 0) {
            if(getCategory().isDisappearWhenBroken()) {
                player.getInventory().removeItem(itemStack);
                return;
            }
        }

        forceDamageItem(player, itemStack, result);
    }

    public void updateItemOnDamage(ItemStack itemStack, final int result) {
        ItemMeta im = itemStack.getItemMeta();
        if(!getCategory().isVanillaDurability()) {
            im.getPersistentDataContainer().set(ITEM_DURABILITY, PersistentDataType.INTEGER, result);
            itemStack.setItemMeta(im);
        }

        im.lore(getCategory().getTooltip().getFormattedLore(this, itemStack));
        itemStack.setItemMeta(im);
    }

    public void updateItemOnDamage(ItemStack itemStack) {
        updateItemOnDamage(itemStack, (getDurability(itemStack)));
    }

    public static NamespacedKey ITEM_DURABILITY = NamespacedKey.fromString("item-durability");
    public void onItemUse(Player player, ItemStack itemStack, Cancellable event) {
        // Som de uso do item
        getCategory().playUseSounds(player, false);

        naturalItemDamage(player, itemStack, event);
    }

    public void naturalItemDamage(Player player, ItemStack itemStack, Cancellable event) {
        // Reduzir durabilidade do item
        if(player.getGameMode() == GameMode.CREATIVE)
            return;
        if(this.getCategory().isVanillaDurability()) {
            player.getScheduler().runDelayed(MineSkyItems.getInstance(), (task) -> {
                updateItemOnDamage(itemStack, -1);
            }, null, 1);
            return;
        }

        // Checando encantamento de durabilidade
        if(itemStack.getEnchantments().containsKey(Enchantment.UNBREAKING)) {
            int level = itemStack.getEnchantmentLevel(Enchantment.UNBREAKING);

            double chance = 100.0 / (level + 1); // Fórmula vanilla de durabilidade do Minecraft
            double roll = Math.random() * 100.0;

            if (roll < chance)
                damageItem(player, itemStack, 1, event);

            return;
        }

        damageItem(player, itemStack, 1, event);
    }

    private final double FIXED_VELOCITY_MULTIPLIER = 2.8;

    public void onInteraction(Player player, ItemStack itemStack, InteractionType interactionType,
                              Cancellable event, @Nullable EquipmentSlot hand) {
        if(MineSkyItems.MMOCORE_HOOK) {
            /*
            PlayerData playerData = MineSkyItems.mmocoreAPI.getPlayerData(player);
            if (!player.hasPermission("mineskyitems.bypass.class-requirement") &&
                    !hasClassRequirement(playerData.getProfess().getName())) {
                //event.setCancelled(true);
                player.sendMessage("§cSua classe não possui conhecimento de como usar esse item.");
                return;
            }

            if (!player.hasPermission("mineskyitems.bypass.level-requirement") &&
                    !hasLevelRequirement(playerData.getLevel())) {
                //event.setCancelled(true);
                player.sendMessage("§cVocê ainda não possui o nível apropriado para usar esse item.");
                return;
            }*/
        }

        // Ranged system logic
        if(getCategory().getType().equalsIgnoreCase("ranged")
                && interactionType == InteractionType.RIGHT_CLICK) {
            double damage = getItemAttributes().getArrowDamage();
            final double cooldownInSeconds = getItemAttributes().getSpeed();

            ItemStack stack = Utils.getFirstArrowItem(player);
            if(stack == null) return;

            if(CooldownManager.inItemCooldown(player, this))
                return;

            if (isItemBroken(itemStack)) {
                noDurability(player, itemStack);
                return;
            }

            if(itemStack.containsEnchantment(Enchantment.POWER)) {
                int level = stack.getEnchantmentLevel(Enchantment.POWER);
                damage = (damage*0.25) * (level+1);
            }

            if(cooldownInSeconds != 0.0) {
                //player.setCooldown(itemStack, (int)(20/cooldownInSeconds));
                CooldownManager.createItemCooldown(player, this, (float) (20/cooldownInSeconds));
            }

            if(!itemStack.containsEnchantment(Enchantment.INFINITY)
                    && player.getGameMode() != GameMode.CREATIVE)
                stack.setAmount(stack.getAmount()-1);

            Vector baseDirection = player.getLocation().getDirection();

            Class<? extends AbstractArrow> arrowClass = (stack.getType() == Material.SPECTRAL_ARROW)
                    ? SpectralArrow.class : Arrow.class;

            boolean multishot = itemStack.containsEnchantment(Enchantment.MULTISHOT);
            int shots = multishot ? (3 + itemStack.getEnchantmentLevel(Enchantment.MULTISHOT)) : 1;

            double multdamage = damage;

            for (int i = 0; i < shots; i++) {
                Vector shotDirection = baseDirection.clone();

                if (multishot) {
                    double angle = 0;
                    if (i == 0) {
                        angle = -10;
                        multdamage = damage/2;
                    } else if (i == 2) {
                        angle = 10;
                        multdamage = damage/2;
                    } else multdamage = damage;
                    shotDirection.rotateAroundY(Math.toRadians(angle));
                }

                final double finalDamage = multdamage;
                player.getWorld().spawn(player.getEyeLocation(), arrowClass, arr -> {
                    arr.setShooter(player);
                    arr.setVelocity(shotDirection.multiply(FIXED_VELOCITY_MULTIPLIER));
                    arr.setDamage(0);
                    arr.getPersistentDataContainer().set(MineSkyItems.NAMESPACED_KEY, PersistentDataType.DOUBLE, finalDamage);
                    //arr.setKnockbackStrength(itemStack.getEnchantmentLevel(Enchantment.PUNCH) * 3);

                    if (stack.getType() == Material.TIPPED_ARROW
                            && arr instanceof Arrow) {
                        PotionMeta potionMeta = (PotionMeta) stack.getItemMeta();
                        ((Arrow) arr).setBasePotionType(potionMeta.getBasePotionType());
                        potionMeta.getCustomEffects().forEach(potionEffect -> {
                            ((Arrow) arr).addCustomEffect(potionEffect, true);
                        });
                    }

                    if (itemStack.containsEnchantment(Enchantment.FLAME)) {
                        arr.setFireTicks(999999);
                    }
                });
            }

            if(hand != null)
                player.swingHand(hand);

            naturalItemDamage(player, itemStack, event);

            player.getWorld().spawnParticle(Particle.CLOUD, player.getEyeLocation(), 0, 0, 0, 0, 0);
            getCategory().playUseSounds(player, true);

            return;
        }

        if(interactionType == InteractionType.RIGHT_CLICK) {
            Item offhand = ItemHandler.getItemFromStack(player.getInventory().getItemInOffHand());
            if(offhand != null && offhand.getCategory().isDualHanded())
                return;
        }

        getItemSkills().stream()
                .filter(skill -> skill.getInteractionType() == interactionType)
                .findFirst()
                .ifPresent(skill -> {
                    if(isItemBroken(itemStack)) {
                        noDurability(player, itemStack);
                        event.setCancelled(true);
                        return;
                    }

                    // Key feedback
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.3f, 1.2f);
                    event.setCancelled(true);

                    if (CooldownManager.inCooldown(player, skill)) {
                        final String message =
                                "Habilidade em recarga, aguarde mais "+CooldownManager.getRemainingCooldown(player, skill)+" segundo(s)!";
                        //if(MineSkyItems.MMOCORE_HOOK) {
                        //    MineSkyItems.mmocoreAPI.getPlayerData(player).displayActionBar("§c"+message);
                        //} else {
                            player.sendActionBar(Component.text(message).color(NamedTextColor.RED));
                        //}
                        return;
                    }
                    CooldownManager.createCooldown(player, skill, skill.getCooldown());

                    if(!getCategory().isVanillaDurability())
                        damageItem(player, itemStack, 1, null);
                    else
                        forceDamageItem(player, itemStack, 1);

                    List<Entity> targets = new ArrayList();
                    Entity casterEntity = player;
                    Location origin = player.getLocation();
                    LivingEntity target = MythicUtil.getTargetedEntity(player);
                    targets.add(target);

                    String spell = skill.getMythicSkillId();

                    MythicBukkit.inst().getAPIHelper()
                            .castSkill(casterEntity, spell, casterEntity, origin, targets, null, 1.0f, metadata -> {
                                metadata.getVariables().putFloat("mineskyitem-damage", (float)getItemAttributes().getSkillDamage());
                            });
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
        ItemStack itemStack = new ItemStack(metadata.material());

        if(!getCategory().isNoAttributes())
            itemStack = getItemAttributes().translateAndUpdate(itemStack);

        ItemMeta im = itemStack.getItemMeta();

        // Persistent Data Container
        PersistentDataContainer container = im.getPersistentDataContainer();
        container.set(ItemHandler.LEVEL_NAMESPACE, PersistentDataType.INTEGER, this.levelRequirement);
        container.set(ItemHandler.CLASS_NAMESPACE, PersistentDataType.LIST.strings(), this.requiredClasses);
        container.set(MineSkyItems.NAMESPACED_KEY, PersistentDataType.STRING, getId());

        if(getCategory().isDoNotStack()) {
            container.set(NamespacedKey.minecraft("unique"), PersistentDataType.STRING, UUID.randomUUID().toString());
            im.setMaxStackSize(1);
        }

        fixVanillaDurability(itemStack);

        if(getItemRarity().hasTooltip()) {
            im.setTooltipStyle(NamespacedKey.minecraft(getItemRarity().getTooltip()));
        }

        if(!getCategory().isVanillaDurability()) {
            im.setUnbreakable(true);
            im.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            container.set(ITEM_DURABILITY, PersistentDataType.INTEGER, getMaxDurability());
        }

        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        // Tools
        if(getCategory().isTool()) {
            ToolComponent toolComponent = im.getTool();
            toolComponent.getRules().clear();

            final float toolSpeed = getItemAttributes().getToolSpeed();
            final float defaultToolSpeed = getItemAttributes().getDefaultToolSpeed();
            toolComponent.setDefaultMiningSpeed(defaultToolSpeed);
            toolComponent.setDamagePerBlock(1);

            switch(getCategory().getTool()) {
                case "PICKAXE" -> toolComponent.addRule(Tag.MINEABLE_PICKAXE, toolSpeed, true);
                case "AXE" -> toolComponent.addRule(Tag.MINEABLE_AXE, toolSpeed, true);
                case "HOE" -> toolComponent.addRule(Tag.MINEABLE_HOE, toolSpeed, true);
                case "SHOVEL" -> toolComponent.addRule(Tag.MINEABLE_SHOVEL, toolSpeed, true);
            }
            im.setTool(toolComponent);
        }

        Component itemName = Component.text(metadata.displayName()).color(getItemRarity().getTextColor());
        im.itemName(itemName);
        im.lore(getCategory().getTooltip().getFormattedLore(this, itemStack));
        im.setCustomModelData(metadata.modelData());

        // Armor
        if(getCategory().getType().equalsIgnoreCase("armor") && !isNoAutoArmor()) {
            EquippableComponent equippableComponent = im.getEquippable();
            equippableComponent.setModel(NamespacedKey.minecraft("part_" + metadata.modelData));
            equippableComponent.setSlot(metadata.material().getEquipmentSlot());
            im.setEquippable(equippableComponent);
        }

        itemStack.setItemMeta(im);

        fixVanillaDurability(itemStack);

        return itemStack;
    }

    public void fixVanillaDurability(ItemStack itemStack) {
        if(getCategory().isNoAttributes())
            return;

        if(getCategory().isVanillaDurability()) {
            // OBRIGATÓRIO: Definir MAX_STACK_SIZE para 1
            itemStack.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
            itemStack.setData(DataComponentTypes.MAX_DAMAGE, getMaxDurability());
            itemStack.setData(DataComponentTypes.DAMAGE, 0);
        } else {
            itemStack.resetData(DataComponentTypes.MAX_DAMAGE);
            itemStack.resetData(DataComponentTypes.DAMAGE);
        }
    }

    public record ItemMetadata(Material material,
                               String displayName,
                               int modelData,
                               List<String> lore) {}
}
