package net.mineskyitems.entities.shields;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.utils.cooldown.CooldownManager;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShieldManager implements Listener {

    public static final NamespacedKey TOTEM_KEY = new NamespacedKey(MineSkyItems.getInstance(), "shield_totems");
    public static final String BOOMERANG_TAG = "minesky_shield_boomerang";

    private static final Set<UUID> rushingPlayers = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, BoomerangSession> activeBoomerangs = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerToBoomerang = new ConcurrentHashMap<>();

    private static File storageFile;
    private static YamlConfiguration storageConfig;

    public static void init() {
        storageFile = new File(MineSkyItems.getInstance().getDataFolder(), "active_boomerangs.yml");
        if (!storageFile.exists()) {
            try {
                storageFile.getParentFile().mkdirs();
                storageFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        storageConfig = YamlConfiguration.loadConfiguration(storageFile);

        restoreStoredBoomerangs();
    }

    public static void shutdown() {
        for (BoomerangSession session : new ArrayList<>(activeBoomerangs.values())) {
            session.cancelAndSave();
        }
        saveStorageFile();
    }

    private static void saveStorageFile() {
        try {
            storageConfig.save(storageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void restoreStoredBoomerangs() {
        for (String key : storageConfig.getKeys(false)) {
            ConfigurationSection sec = storageConfig.getConfigurationSection(key);
            if (sec == null) continue;

            UUID playerUuid = UUID.fromString(sec.getString("player"));
            EquipmentSlot slot = EquipmentSlot.valueOf(sec.getString("slot", "OFF_HAND"));
            ItemStack item = sec.getItemStack("item");
            String displayUuidStr = sec.getString("display");

            if (displayUuidStr != null) {
                UUID displayUuid = UUID.fromString(displayUuidStr);
                Entity entity = Bukkit.getEntity(displayUuid);
                if (entity != null) {
                    entity.remove();
                }
            }

            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline() && item != null) {
                player.getScheduler().run(MineSkyItems.getInstance(), t -> {
                    returnItemToPlayer(player, item, slot);
                }, null);
            } else if (item != null) {
                String worldName = sec.getString("world");
                double x = sec.getDouble("x");
                double y = sec.getDouble("y");
                double z = sec.getDouble("z");
                World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                if (world != null) {
                    world.dropItemNaturally(new Location(world, x, y, z), item);
                }
            }

            storageConfig.set(key, null);
        }
        saveStorageFile();
    }

    public static void handleShieldAbility(Player player, ItemStack itemStack, Item item, EquipmentSlot hand, Cancellable event) {
        String shieldType = item.getCategory().getShield();
        if (shieldType == null) return;

        if (CooldownManager.inItemCooldown(player, item)) {
            return;
        }

        if (item.isItemBroken(itemStack)) {
            return;
        }

        switch (shieldType.toLowerCase()) {
            case "rush" -> executeRush(player, itemStack, item, hand, event);
            case "boomerang" -> executeBoomerang(player, itemStack, item, hand, event);
        }
    }

    private static void executeRush(Player player, ItemStack itemStack, Item item, EquipmentSlot hand, Cancellable event) {
        if (event != null) event.setCancelled(true);

        CooldownManager.createItemCooldown(player, item, 6 * 20);
        item.damageItem(player, itemStack, 2, null);

        rushingPlayers.add(player.getUniqueId());

        Vector dir = player.getLocation().getDirection().setY(0.15).normalize().multiply(1.85);
        player.setVelocity(dir);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.9f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);

        final int durationTicks = 16;
        final double damage = Math.max(2.0, item.getItemAttributes().getDamage() * 1.5);

        player.getScheduler().runAtFixedRate(MineSkyItems.getInstance(), new java.util.function.Consumer<ScheduledTask>() {
            int ticks = 0;

            @Override
            public void accept(ScheduledTask task) {
                if (!player.isOnline() || player.isDead() || ticks++ >= durationTicks) {
                    rushingPlayers.remove(player.getUniqueId());
                    task.cancel();
                    return;
                }

                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.5, 0), 3, 0.2, 0.2, 0.2, 0.05);

                for (Entity entity : player.getWorld().getNearbyEntities(loc, 2.2, 2.0, 2.2)) {
                    if (entity.equals(player) || !(entity instanceof LivingEntity target) || entity instanceof ArmorStand) {
                        continue;
                    }

                    Vector push = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.4).setY(0.4);
                    target.setVelocity(push);
                    target.damage(damage, player);

                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.2f);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.2);
                }
            }
        }, null, 1, 1);
    }

    private static void executeBoomerang(Player player, ItemStack itemStack, Item item, EquipmentSlot hand, Cancellable event) {
        if (event != null) event.setCancelled(true);

        if (playerToBoomerang.containsKey(player.getUniqueId())) {
            return;
        }

        ItemStack thrownStack = itemStack.clone();
        thrownStack.setAmount(1);

        if (itemStack.getAmount() > 1) {
            itemStack.setAmount(itemStack.getAmount() - 1);
        } else {
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }

        item.damageItem(player, thrownStack, 1, null);

        Location spawnLoc = player.getEyeLocation();
        World world = player.getWorld();

        ItemDisplay display = world.spawn(spawnLoc, ItemDisplay.class, ent -> {
            ent.setItemStack(thrownStack);
            ent.addScoreboardTag(BOOMERANG_TAG);
            ent.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0),
                    new Vector3f(1.1f, 1.1f, 1.1f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
            ent.setInterpolationDuration(1);
            ent.setInterpolationDelay(0);
        });

        BoomerangSession session = new BoomerangSession(player, item, thrownStack, hand, display);
        activeBoomerangs.put(display.getUniqueId(), session);
        playerToBoomerang.put(player.getUniqueId(), display.getUniqueId());

        session.start();
    }

    public static class BoomerangSession {
        private final UUID playerId;
        private final Item customItem;
        private final ItemStack itemStack;
        private final EquipmentSlot originalSlot;
        private final ItemDisplay display;
        private final UUID displayId;
        private final String storageKey;

        private Vector direction;
        private boolean returning = false;
        private int bounces = 0;
        private int ticksLived = 0;
        private ScheduledTask task;
        private float currentYaw = 0;

        public BoomerangSession(Player player, Item customItem, ItemStack itemStack, EquipmentSlot originalSlot, ItemDisplay display) {
            this.playerId = player.getUniqueId();
            this.customItem = customItem;
            this.itemStack = itemStack;
            this.originalSlot = originalSlot;
            this.display = display;
            this.displayId = display.getUniqueId();
            this.storageKey = UUID.randomUUID().toString();

            this.direction = player.getEyeLocation().getDirection().normalize().multiply(1.25);
            saveToConfig();
        }

        private void saveToConfig() {
            storageConfig.set(storageKey + ".player", playerId.toString());
            storageConfig.set(storageKey + ".slot", originalSlot.name());
            storageConfig.set(storageKey + ".item", itemStack);
            storageConfig.set(storageKey + ".display", displayId.toString());
            Location loc = display.getLocation();
            storageConfig.set(storageKey + ".world", loc.getWorld().getName());
            storageConfig.set(storageKey + ".x", loc.getX());
            storageConfig.set(storageKey + ".y", loc.getY());
            storageConfig.set(storageKey + ".z", loc.getZ());
            saveStorageFile();
        }

        private void removeFromConfig() {
            storageConfig.set(storageKey, null);
            saveStorageFile();
        }

        public void start() {
            this.task = display.getScheduler().runAtFixedRate(MineSkyItems.getInstance(), t -> tick(t), null, 1, 1);
        }

        private void tick(ScheduledTask runningTask) {
            ticksLived++;

            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                dropAndCleanup();
                runningTask.cancel();
                return;
            }

            Location currentLoc = display.getLocation();

            currentYaw += 45f;
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.toRadians(currentYaw), 0, 1, 0),
                    new Vector3f(1.1f, 1.1f, 1.1f),
                    new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0)
            ));
            display.setInterpolationDuration(1);
            display.setInterpolationDelay(0);

            if (!returning) {
                if (ticksLived >= 26 || currentLoc.distance(player.getEyeLocation()) >= 20.0) {
                    returning = true;
                }

                RayTraceResult blockHit = currentLoc.getWorld().rayTraceBlocks(
                        currentLoc, direction, direction.length(), FluidCollisionMode.NEVER, true);

                if (blockHit != null && blockHit.getHitBlockFace() != null) {
                    BlockFace face = blockHit.getHitBlockFace();
                    Vector normal = face.getDirection();
                    direction = direction.subtract(normal.multiply(2 * direction.dot(normal))).multiply(0.85);

                    currentLoc.getWorld().playSound(currentLoc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.4f);
                    currentLoc.getWorld().spawnParticle(Particle.CRIT, blockHit.getHitPosition().toLocation(currentLoc.getWorld()), 6, 0.1, 0.1, 0.1, 0.1);

                    bounces++;
                    if (bounces >= 3) {
                        returning = true;
                    }
                }

                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.4, 1.4, 1.4)) {
                    if (entity.equals(player) || !(entity instanceof LivingEntity target) || entity instanceof ArmorStand) {
                        continue;
                    }

                    target.damage(customItem.getItemAttributes().getDamage(), player);
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.9f);
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                    returning = true;
                    break;
                }
            } else {
                Location targetLoc = player.getEyeLocation();
                Vector toPlayer = targetLoc.toVector().subtract(currentLoc.toVector());
                double dist = toPlayer.length();

                if (dist <= 1.35) {
                    finishReturn(player, false);
                    runningTask.cancel();
                    return;
                }

                direction = toPlayer.normalize().multiply(1.3);
            }

            Location nextLoc = currentLoc.clone().add(direction);
            display.teleportAsync(nextLoc);

            currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 1,
                    new Particle.DustOptions(Color.fromRGB(206, 245, 66), 0.7f));
        }

        public void catchManual(Player player) {
            if (task != null) task.cancel();
            finishReturn(player, true);
        }

        private void finishReturn(Player player, boolean manualCatch) {
            cleanupSession();

            player.getScheduler().run(MineSkyItems.getInstance(), t -> {
                returnItemToPlayer(player, itemStack, originalSlot);

                if (manualCatch) {
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.2f);
                    player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation().add(0, 0.3, 0), 2, 0.2, 0.2, 0.2, 0.05);
                } else {
                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 0.5f);
                    CooldownManager.createItemCooldown(player, customItem, 6 * 20);
                }
            }, null);
        }

        private void dropAndCleanup() {
            cleanupSession();
            Location loc = display.getLocation();
            loc.getWorld().dropItemNaturally(loc, itemStack);
        }

        public void cancelAndSave() {
            if (task != null) task.cancel();
            Location loc = display.getLocation();
            storageConfig.set(storageKey + ".world", loc.getWorld().getName());
            storageConfig.set(storageKey + ".x", loc.getX());
            storageConfig.set(storageKey + ".y", loc.getY());
            storageConfig.set(storageKey + ".z", loc.getZ());
            display.remove();
        }

        private void cleanupSession() {
            activeBoomerangs.remove(displayId);
            playerToBoomerang.remove(playerId);
            removeFromConfig();
            display.remove();
        }

        public boolean isReturning() {
            return returning;
        }

        public UUID getPlayerId() {
            return playerId;
        }
    }

    private static void returnItemToPlayer(Player player, ItemStack item, EquipmentSlot slot) {
        if (slot == EquipmentSlot.OFF_HAND && (player.getInventory().getItemInOffHand().getType() == Material.AIR)) {
            player.getInventory().setItemInOffHand(item);
            return;
        }

        if (slot == EquipmentSlot.HAND && (player.getInventory().getItemInMainHand().getType() == Material.AIR)) {
            player.getInventory().setItemInMainHand(item);
            return;
        }

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getCursor() == null || e.getCursor().getType() != Material.TOTEM_OF_UNDYING) {
            return;
        }

        ItemStack targetStack = e.getCurrentItem();
        if (targetStack == null || targetStack.getType() == Material.AIR) {
            return;
        }

        Item customItem = ItemHandler.getItemFromStack(targetStack);
        if (customItem == null) {
            return;
        }

        int maxTotems = customItem.getItemAttributes().getMaxTotems();
        if (maxTotems <= 0) {
            return;
        }

        ItemMeta meta = targetStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int currentTotems = pdc.getOrDefault(TOTEM_KEY, PersistentDataType.INTEGER, 0);

        if (currentTotems >= maxTotems) {
            return;
        }

        e.setCancelled(true);

        ItemStack cursor = e.getCursor();
        cursor.setAmount(cursor.getAmount() - 1);

        currentTotems++;
        pdc.set(TOTEM_KEY, PersistentDataType.INTEGER, currentTotems);
        targetStack.setItemMeta(meta);

        targetStack.lore(customItem.getCategory().getTooltip().getFormattedLore(customItem, targetStack));

        Player player = (Player) e.getWhoClicked();
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 1.4f);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityResurrect(EntityResurrectEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack shieldStack = null;
        Item customShield = null;

        ItemStack off = player.getInventory().getItemInOffHand();
        Item offItem = ItemHandler.getItemFromStack(off);
        if (offItem != null && off.hasItemMeta()) {
            int totems = off.getItemMeta().getPersistentDataContainer().getOrDefault(TOTEM_KEY, PersistentDataType.INTEGER, 0);
            if (totems > 0) {
                shieldStack = off;
                customShield = offItem;
            }
        }

        if (shieldStack == null) {
            ItemStack main = player.getInventory().getItemInMainHand();
            Item mainItem = ItemHandler.getItemFromStack(main);
            if (mainItem != null && main.hasItemMeta()) {
                int totems = main.getItemMeta().getPersistentDataContainer().getOrDefault(TOTEM_KEY, PersistentDataType.INTEGER, 0);
                if (totems > 0) {
                    shieldStack = main;
                    customShield = mainItem;
                }
            }
        }

        if (shieldStack == null) {
            return;
        }

        e.setCancelled(false);

        ItemMeta meta = shieldStack.getItemMeta();
        int current = meta.getPersistentDataContainer().getOrDefault(TOTEM_KEY, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(TOTEM_KEY, PersistentDataType.INTEGER, Math.max(0, current - 1));
        shieldStack.setItemMeta(meta);
        shieldStack.lore(customShield.getCategory().getTooltip().getFormattedLore(customShield, shieldStack));

        player.setHealth(1.0);
        player.clearActivePotionEffects();
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));

        player.playEffect(EntityEffect.TOTEM_RESURRECT);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRushingDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }

        if (rushingPlayers.contains(player.getUniqueId())) {
            e.setCancelled(true);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.1f);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof ItemDisplay display) {
            BoomerangSession session = activeBoomerangs.get(display.getUniqueId());
            if (session != null && session.getPlayerId().equals(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                session.catchManual(e.getPlayer());
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        UUID boomerangId = playerToBoomerang.get(player.getUniqueId());
        if (boomerangId == null) {
            return;
        }

        BoomerangSession session = activeBoomerangs.get(boomerangId);
        if (session == null || !session.isReturning()) {
            return;
        }

        Entity displayEntity = Bukkit.getEntity(boomerangId);
        if (displayEntity == null) {
            return;
        }

        if (player.getEyeLocation().distance(displayEntity.getLocation()) <= 5.0) {
            e.setCancelled(true);
            session.catchManual(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        rushingPlayers.remove(e.getPlayer().getUniqueId());

        UUID boomerangId = playerToBoomerang.get(e.getPlayer().getUniqueId());
        if (boomerangId != null) {
            BoomerangSession session = activeBoomerangs.get(boomerangId);
            if (session != null) {
                session.dropAndCleanup();
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        rushingPlayers.remove(e.getEntity().getUniqueId());

        UUID boomerangId = playerToBoomerang.get(e.getEntity().getUniqueId());
        if (boomerangId != null) {
            BoomerangSession session = activeBoomerangs.get(boomerangId);
            if (session != null) {
                session.dropAndCleanup();
            }
        }
    }
}