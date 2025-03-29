package net.minesky.gui;

import net.minesky.handler.ItemBuilder;
import net.minesky.handler.categories.Category;
import net.minesky.utils.ChatInputCallback;
import net.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilderMenu implements Listener {

    public static HashMap<Player, ItemBuilder> builderHashMap = new HashMap<>();
    public static HashMap<Player, Inventory> inventories = new HashMap<>();

    private static ItemStack simpleButton(Material m, String name, String... lore) {
        return simpleButton(m, name, 1, lore);
    }
    private static ItemStack simpleButton(Material m, String name, int count, String... lore) {
        ItemStack it = new ItemStack(m, count);
        ItemMeta im = it.getItemMeta();

        im.setDisplayName("§6§l"+name);

        im.setLore(Arrays.stream(lore)
                .map(a -> Utils.c("&7"+a))
                .collect(Collectors.toList()));

        it.setItemMeta(im);
        return it;
    }

    private static void reorganizeItems(ItemBuilder builder, Inventory inv) {
        inv.setItem(4, builder.build().buildStack());

        inv.setItem(10, simpleButton(
                Material.PLAYER_HEAD, "Classe necessária", "• Define classe(s) obrigatória(s)", " para usar esse item.",
                " ",
                "&6Classe: &e"+builder.getPlayerClass(),
                " ",
                "&e➳ Clique esquerdo - Definir classe(s)",
                "&e➳ Clique direito - Remover classe(s)")
        );

        inv.setItem(12, simpleButton(
                Material.EXPERIENCE_BOTTLE, "Level do item", (builder.getItemLevel() <= 0 ? 1 : builder.getItemLevel()),"• Define um nível (level) para esse item", "• Jogadores terão de ter o mesmo", " nível ou", " superior para usá-lo.",
                " ",
                "&6Level atual: &e"+(builder.getItemLevel() == -1 ? "Não" : builder.getItemLevel()),
                " ",
                "&e➳ Clique esquerdo - Definir nível",
                "&e➳ Clique direito - Remover nível")
        );

        inv.setItem(14, simpleButton(
                Material.NAME_TAG, "Nome","Define um nome de exibição", "para o seu item",
                " ",
                "&6Nome: &e"+( builder.getDisplayName().isEmpty() ? "Sem nome" : builder.getDisplayName()),
                " ",
                "&e➳ Clique esquerdo - Definir nome",
                "&e➳ Clique direito - Remover nome")
        );

        inv.setItem(16, simpleButton(
                Material.MAGMA_CREAM, "Modelo do item", (builder.getCustomModel() <= 0 ? 1 : builder.getCustomModel()),"• Todos os itens possuem um", " número para definir seu modelo.",
                " ",
                "&6Modelo: &e"+( builder.getCustomModel() == 0 ? "Sem modelo" : builder.getCustomModel()),
                " ",
                "&e➳ Clique esquerdo - Definir modelo",
                "&e➳ Clique direito - Remover modelo")
        );
    }

    public static void openMainMenu(Player player, ItemBuilder builder) {
        Inventory inv = Bukkit.createInventory(null, 27, "Customização de item");

        builderHashMap.put(player, builder);
        inventories.put(player, inv);

        reorganizeItems(builder, inv);

        player.openInventory(inv);
    }

    private static void reopenInventory(Player player) {
        Inventory inv = inventories.get(player);
        ItemBuilder builder = builderHashMap.get(player);
        if(inv == null || builder == null)
            return;

        reorganizeItems(builder, inv);

        player.closeInventory();
        player.openInventory(inv);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if(inventories.containsValue(e.getInventory()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        final Player p = (Player) e.getWhoClicked();
        final int slot = e.getSlot();
        final ClickType clickType = e.getClick();

        if(!inventories.containsValue(e.getInventory()))
            return;

        e.setCancelled(true);

        ItemBuilder builder = builderHashMap.get(p);
        assert builder != null;

        switch(slot) {
            // Give item / Change material data
            case 4 -> {
                switch(clickType) {
                    case RIGHT:
                    case LEFT: {
                        ItemStack stack = builder.build().buildStack();
                        p.getInventory().addItem(stack);
                        break;
                    }
                    case SHIFT_RIGHT: {
                        Utils.awaitChatInput(p, new ChatInputCallback() {
                            @Override
                            public void onInput(String response) {
                                Material material = Material.getMaterial(response.toUpperCase().trim());

                                if(material == null) {
                                    material = builder.getCategory().getDefaultItem();
                                }

                                builder.setMaterial(material);
                                reopenInventory(p);
                            }

                            @Override
                            public void onCancel() {
                                reopenInventory(p);
                            }
                        });
                        break;
                    }
                }
            }

            // Classe necessaria
            case 10 -> {
                switch(clickType) {
                    case RIGHT -> builder.setPlayerClass(new ArrayList<>());
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            List<String> classes = builder.getPlayerClass();

                            classes.add(response);

                            builder.setPlayerClass(classes);
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            // Level necessario
            case 12 -> {
                switch(clickType) {
                    case RIGHT -> builder.setItemLevel(-1);
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            int level = -1;
                            try {
                                level = Integer.parseInt(response);
                            } catch (Exception ex) {
                                p.sendMessage("Insira um numero válido.");
                                reopenInventory(p);
                                return;
                            }

                            builder.setItemLevel(level);
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            // Modificar nome do item
            case 14 -> {
                switch(clickType) {
                    case RIGHT -> {
                        return;
                    }
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            builder.setDisplayName(response);
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            // Modificar modelo do item
            case 16 -> {
                switch(clickType) {
                    case RIGHT -> builder.setCustomModel(0);
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            int model = -1;
                            try {
                                model = Integer.parseInt(response);
                            } catch (Exception ex) {
                                p.sendMessage("Insira um numero válido.");
                                reopenInventory(p);
                                return;
                            }

                            builder.setCustomModel(model);
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }
        }

        builder.build();

        switch(clickType) {
            case RIGHT -> {
                p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 0);
                reopenInventory(p);
            }
            case LEFT -> p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1);
        }

    }

}
