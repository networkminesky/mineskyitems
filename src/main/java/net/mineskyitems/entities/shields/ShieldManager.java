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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private static final Map<UUID, BoomerangSession> hitboxToSession = new ConcurrentHashMap<>();

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
            String hitboxUuidStr = sec.getString("hitbox");

            if (displayUuidStr != null) {
                Entity entity = Bukkit.getEntity(UUID.fromString(displayUuidStr));
                if (entity != null) entity.remove();
            }

            if (hitboxUuidStr != null) {
                Entity entity = Bukkit.getEntity(UUID.fromString(hitboxUuidStr));
                if (entity != null) entity.remove();
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

    private static boolean canHurt(Player damager, LivingEntity target, double damage) {
        if (target instanceof ArmorStand) return false;
        if (damager.equals(target)) return false;
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(damager, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
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

        player.swingHand(hand);
    }

    private static void executeRush(Player player, ItemStack itemStack, Item item, EquipmentSlot hand, Cancellable event) {
        if (event != null) event.setCancelled(true);

        CooldownManager.createItemCooldown(player, item, 6 * 20);
        //player.setCooldown(itemStack, 6 * 20);
        item.damageItem(player, itemStack, 2, null);

        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(itemStack);
        } else {
            player.getInventory().setItemInMainHand(itemStack);
        }

        rushingPlayers.add(player.getUniqueId());

        double yaw = Math.toRadians(player.getLocation().getYaw());
        Vector dir = new Vector(-Math.sin(yaw), 0.0, Math.cos(yaw)).normalize().multiply(2.4).setY(0.28);
        player.setVelocity(dir);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.9f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);

        final int durationTicks = 16;
        final double damage = Math.max(2.0, item.getItemAttributes().getSkillDamage() * 1.5);

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

                    if (!canHurt(player, target, damage)) {
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

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_STUNNED, 1, 0.8f);

        ItemStack thrownStack = itemStack.clone();
        thrownStack.setAmount(1);

        if (itemStack.getAmount() > 1) {
            itemStack.setAmount(itemStack.getAmount() - 1);
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(itemStack);
            } else {
                player.getInventory().setItemInMainHand(itemStack);
            }
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
                    new AxisAngle4f((float) Math.toRadians(270), 1, 0, 0),
                    new Vector3f(1.1f, 1.1f, 1.1f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
            ent.setTeleportDuration(2);
            ent.setInterpolationDuration(2);
            ent.setInterpolationDelay(0);
        });

        Interaction hitbox = world.spawn(spawnLoc.clone().subtract(0, 2.25, 0), Interaction.class, ent -> {
            ent.setInteractionWidth(4.5f);
            ent.setInteractionHeight(4.5f);
            ent.setResponsive(true);
            ent.addScoreboardTag(BOOMERANG_TAG);
        });

        BoomerangSession session = new BoomerangSession(player, item, thrownStack, hand, display, hitbox);
        activeBoomerangs.put(display.getUniqueId(), session);
        hitboxToSession.put(hitbox.getUniqueId(), session);
        playerToBoomerang.put(player.getUniqueId(), display.getUniqueId());

        session.start();
    }

    public static class BoomerangSession {
        private final UUID playerId;
        private final Item customItem;
        private final ItemStack itemStack;
        private final EquipmentSlot originalSlot;
        private final ItemDisplay display;
        private final Interaction hitbox;
        private final UUID displayId;
        private final UUID hitboxId;
        private final String storageKey;

        private Vector direction;
        private double speed = 1.35;
        private boolean returning = false;
        private int bounces = 0;
        private int ticksLived = 0;
        private int returnTicks = 0;
        private int maceDeflections = 0;
        private long lastDeflectTime = 0;
        private ScheduledTask task;
        private float currentYaw = 0;
        private double currentDamage;
        private UUID lastDeflector = null;
        private final Set<UUID> hitEntities = new HashSet<>();

        public BoomerangSession(Player player, Item customItem, ItemStack itemStack, EquipmentSlot originalSlot, ItemDisplay display, Interaction hitbox) {
            this.playerId = player.getUniqueId();
            this.customItem = customItem;
            this.itemStack = itemStack;
            this.originalSlot = originalSlot;
            this.display = display;
            this.hitbox = hitbox;
            this.displayId = display.getUniqueId();
            this.hitboxId = hitbox.getUniqueId();
            this.storageKey = UUID.randomUUID().toString();

            this.currentDamage = Math.max(1.0, customItem.getItemAttributes().getSkillDamage());
            this.direction = player.getEyeLocation().getDirection().normalize().multiply(this.speed);
            saveToConfig();
        }

        private void saveToConfig() {
            storageConfig.set(storageKey + ".player", playerId.toString());
            storageConfig.set(storageKey + ".slot", originalSlot.name());
            storageConfig.set(storageKey + ".item", itemStack);
            storageConfig.set(storageKey + ".display", displayId.toString());
            storageConfig.set(storageKey + ".hitbox", hitboxId.toString());
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

        public void deflectWithMace(Player deflector) {
            long now = System.currentTimeMillis();
            if (now - this.lastDeflectTime < 200) {
                return;
            }
            if (this.maceDeflections >= 20) {
                return;
            }

            this.lastDeflectTime = now;
            this.maceDeflections++;
            this.currentDamage *= 1.25;
            this.speed = Math.min(2.5, this.speed * 1.04);
            this.direction = deflector.getEyeLocation().getDirection().normalize().multiply(this.speed);
            this.lastDeflector = deflector.getUniqueId();
            this.returning = false;
            this.returnTicks = 0;
            this.ticksLived = 0;
            this.bounces = 0;
            this.hitEntities.clear();

            Location loc = display.getLocation();
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0, Color.WHITE);
            loc.getWorld().playSound(loc, Sound.ITEM_MACE_SMASH_GROUND, 1.2f, 1.0f);
            loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.2f, 1.6f);
            loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.7f, 1.8f);
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

            for (BoomerangSession other : activeBoomerangs.values()) {
                if (other == this) continue;
                if (this.displayId.compareTo(other.displayId) >= 0) continue;

                Location otherLoc = other.display.getLocation();
                if (!otherLoc.getWorld().equals(currentLoc.getWorld())) continue;

                if (currentLoc.distanceSquared(otherLoc) <= 3.2 * 3.2) {
                    Vector diff = currentLoc.toVector().subtract(otherLoc.toVector()).normalize();
                    if (diff.lengthSquared() < 0.001) {
                        diff = new Vector(0, 1, 0);
                    }

                    this.direction = diff.clone().multiply(this.speed);
                    other.direction = diff.clone().multiply(-1).multiply(other.speed);

                    this.bounces++;
                    other.bounces++;

                    Location mid = currentLoc.clone().add(otherLoc).multiply(0.5);
                    mid.getWorld().spawnParticle(Particle.SONIC_BOOM, mid, 1, 0, 0, 0, 0);
                    mid.getWorld().spawnParticle(Particle.SWEEP_ATTACK, mid, 10, 0.8, 0.8, 0.8, 0.1);
                    mid.getWorld().playSound(mid, Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 1.2f);
                    mid.getWorld().playSound(mid, Sound.ITEM_SHIELD_BLOCK, 3.0f, 0.6f);

                    Player damager = lastDeflector != null ? Bukkit.getPlayer(lastDeflector) : player;
                    if (damager == null || !damager.isOnline()) damager = player;

                    for (Entity entity : mid.getWorld().getNearbyEntities(mid, 6.0, 4.0, 6.0)) {
                        if (entity instanceof LivingEntity living && !(entity instanceof ArmorStand)) {
                            if (!entity.equals(damager) && canHurt(damager, living, 1.0)) {
                                Vector push = living.getLocation().toVector().subtract(mid.toVector());
                                if (push.lengthSquared() < 0.001) push = new Vector(0, 1, 0);
                                push.normalize().multiply(2.2).setY(0.6);
                                living.setVelocity(push);
                                living.damage(Math.max(2.0, currentDamage * 0.5), damager);
                            }
                        }
                    }
                    break;
                }
            }

            currentYaw += 50f;
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.toRadians(currentYaw), 0, 1, 0),
                    new Vector3f(1.1f, 1.1f, 1.1f),
                    new AxisAngle4f((float) Math.toRadians(270), 1, 0, 0)
            ));
            display.setInterpolationDuration(1);
            display.setInterpolationDelay(0);

            if (!returning) {
                if (ticksLived >= 48 || currentLoc.distance(player.getEyeLocation()) >= 38.0) {
                    returning = true;
                }

                RayTraceResult blockHit = currentLoc.getWorld().rayTraceBlocks(
                        currentLoc, direction, direction.length(), FluidCollisionMode.NEVER, true);

                if (blockHit != null && blockHit.getHitBlockFace() != null) {
                    BlockFace face = blockHit.getHitBlockFace();
                    Vector normal = face.getDirection();
                    direction = direction.subtract(normal.multiply(2 * direction.dot(normal))).multiply(0.9);

                    currentLoc.getWorld().playSound(currentLoc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.4f);
                    currentLoc.getWorld().spawnParticle(Particle.CRIT, blockHit.getHitPosition().toLocation(currentLoc.getWorld()), 6, 0.1, 0.1, 0.1, 0.1);

                    bounces++;
                    if (bounces >= 5) {
                        returning = true;
                    }
                }

                Player currentDamager = lastDeflector != null ? Bukkit.getPlayer(lastDeflector) : player;
                if (currentDamager == null || !currentDamager.isOnline()) {
                    currentDamager = player;
                }

                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.8, 1.8, 1.8)) {
                    if (entity.equals(currentDamager) || !(entity instanceof LivingEntity target) || entity instanceof ArmorStand || hitEntities.contains(target.getUniqueId())) {
                        continue;
                    }

                    if (!canHurt(currentDamager, target, currentDamage)) {
                        continue;
                    }

                    target.damage(currentDamage, currentDamager);
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.9f);
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);

                    hitEntities.add(target.getUniqueId());
                    currentDamage = Math.max(1.0, currentDamage * 0.75);
                    bounces++;

                    LivingEntity nextTarget = null;
                    double closestDist = Double.MAX_VALUE;
                    for (Entity nearby : currentLoc.getWorld().getNearbyEntities(currentLoc, 8.0, 5.0, 8.0)) {
                        if (nearby.equals(currentDamager) || nearby.equals(target) || !(nearby instanceof LivingEntity living) || nearby instanceof ArmorStand || hitEntities.contains(nearby.getUniqueId())) {
                            continue;
                        }
                        if (!canHurt(currentDamager, living, currentDamage)) {
                            continue;
                        }
                        double d = currentLoc.distanceSquared(nearby.getLocation());
                        if (d < closestDist) {
                            closestDist = d;
                            nextTarget = living;
                        }
                    }

                    if (nextTarget != null) {
                        direction = nextTarget.getEyeLocation().toVector().subtract(currentLoc.toVector()).normalize().multiply(this.speed);
                    } else {
                        Vector away = currentLoc.toVector().subtract(target.getLocation().toVector()).setY(0.15).normalize();
                        direction = away.multiply(this.speed);
                    }

                    if (bounces >= 5) {
                        returning = true;
                    }
                    break;
                }
            } else {
                returnTicks++;

                Location targetLoc = player.getEyeLocation();
                Vector toPlayer = targetLoc.toVector().subtract(currentLoc.toVector());
                double dist = toPlayer.length();

                if (dist <= 1.2 || returnTicks >= 70 || dist >= 60.0) {
                    finishReturn(player);
                    runningTask.cancel();
                    return;
                }

                direction = toPlayer.normalize().multiply(this.speed);
            }

            Location nextLoc = currentLoc.clone().add(direction);
            display.teleportAsync(nextLoc);
            hitbox.teleportAsync(nextLoc.clone().subtract(0, 2.25, 0));

            currentLoc.getWorld().spawnParticle(Particle.DUST, nextLoc, 1,
                    new Particle.DustOptions(Color.fromRGB(206, 245, 66), 0.7f));
        }

        private void finishReturn(Player player) {
            cleanupSession();

            player.getScheduler().run(MineSkyItems.getInstance(), t -> {
                returnItemToPlayer(player, itemStack, originalSlot);
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 0.5f);
                CooldownManager.createItemCooldown(player, customItem, 6 * 20);
                player.setCooldown(itemStack, 6 * 20);
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
            hitbox.remove();
        }

        private void cleanupSession() {
            activeBoomerangs.remove(displayId);
            hitboxToSession.remove(hitboxId);
            playerToBoomerang.remove(playerId);
            removeFromConfig();
            display.remove();
            hitbox.remove();
        }

        public boolean isReturning() {
            return returning;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public ItemDisplay getDisplay() {
            return display;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHitboxDamage(EntityDamageByEntityEvent e) {
        BoomerangSession session = hitboxToSession.get(e.getEntity().getUniqueId());
        if (session == null) {
            return;
        }

        e.setCancelled(true);

        if (!(e.getDamager() instanceof Player damager)) {
            return;
        }

        ItemStack handItem = damager.getInventory().getItemInMainHand();
        if (handItem.getType() != Material.MACE) {
            return;
        }

        session.deflectWithMace(damager);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMaceSwing(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = e.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() != Material.MACE) {
            return;
        }

        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection().normalize();

        for (BoomerangSession session : activeBoomerangs.values()) {
            Location bLoc = session.display.getLocation();
            if (!bLoc.getWorld().equals(player.getWorld())) continue;

            Vector toB = bLoc.toVector().subtract(eye.toVector());
            double dist = toB.length();
            if (dist <= 5.5) {
                double dot = look.dot(toB.normalize());
                if (dot > 0.35) {
                    session.deflectWithMace(player);
                    break;
                }
            }
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