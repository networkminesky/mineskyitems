package net.mineskyitems.hook;

import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.core.drops.Drop;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class MythicHook implements Listener {

    @EventHandler
    public void onMythicDropLoad(MythicDropLoadEvent event) {
        if (event.getDropName().equalsIgnoreCase("mineskyitem")) {
            event.register(new MineSkyDrop(event.getConfig()));
        }
    }

    public static class MineSkyDrop extends Drop implements IItemDrop {
        private final String itemId;
        private final List<String> categories;
        private int minLevel = -1;
        private int maxLevel = -1;

        public MineSkyDrop(MythicLineConfig config) {
            super(config.getLine(), config);
            this.itemId = config.getString(new String[]{"id", "i", "item"}, "");

            String catString = config.getString(new String[]{"categories", "category", "cat", "c"}, "");
            if (!catString.isEmpty()) {
                this.categories = Arrays.stream(catString.replace("[", "").replace("]", "").replace("\"", "").split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
            } else {
                this.categories = Collections.emptyList();
            }

            String levelString = config.getString(new String[]{"level", "lvl", "l"}, "");
            parseLevelRange(levelString);
        }

        private void parseLevelRange(String levelStr) {
            if (levelStr == null || levelStr.trim().isEmpty()) {
                return;
            }
            try {
                levelStr = levelStr.trim();
                if (levelStr.contains("-")) {
                    String[] parts = levelStr.split("-");
                    this.minLevel = Integer.parseInt(parts[0].trim());
                    this.maxLevel = Integer.parseInt(parts[1].trim());
                } else if (levelStr.contains("..")) {
                    String[] parts = levelStr.split("\\.\\.");
                    this.minLevel = Integer.parseInt(parts[0].trim());
                    this.maxLevel = Integer.parseInt(parts[1].trim());
                } else {
                    this.minLevel = Integer.parseInt(levelStr);
                    this.maxLevel = this.minLevel;
                }

                if (this.minLevel > this.maxLevel) {
                    int temp = this.minLevel;
                    this.minLevel = this.maxLevel;
                    this.maxLevel = temp;
                }
            } catch (NumberFormatException e) {
                this.minLevel = -1;
                this.maxLevel = -1;
            }
        }

        @Override
        public BukkitItemStack getDrop(DropMetadata metadata, double amount) {
            if (!this.itemId.isEmpty()) {
                Item item = ItemHandler.getItem(this.itemId);
                return adapt(item, amount);
            }

            List<Item> items = ItemHandler.getAllItems().stream()
                    .filter(e -> {
                        if (this.categories.isEmpty()) return true;
                        if (e.getCategory() == null || e.getCategory().getId() == null) return false;
                        return this.categories.contains(e.getCategory().getId().toLowerCase());
                    })
                    .filter(e -> {
                        if (this.minLevel == -1 && this.maxLevel == -1) return true;
                        int reqLevel = e.getRequiredLevel();
                        return reqLevel >= this.minLevel && reqLevel <= this.maxLevel;
                    })
                    .toList();

            if (items.isEmpty()) {
                return null;
            }

            Item randomItem = items.get(ThreadLocalRandom.current().nextInt(items.size()));
            return adapt(randomItem, amount);
        }

        public BukkitItemStack adapt(Item item, double amount) {
            if (item == null) {
                return null;
            }

            ItemStack bukkitItem = item.buildStack();
            int finalAmount = (int) Math.max(1, amount);
            bukkitItem.setAmount(finalAmount);

            return BukkitAdapter.adapt(bukkitItem);
        }
    }
}