package net.mineskyitems.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mineskyitems.entities.ItemBuilder;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.utils.ChatInputCallback;
import net.mineskyitems.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static net.mineskyitems.gui.MenuUtils.simpleButton;

public class ItemBuilderMenu implements Listener {

    public static HashMap<Player, ItemBuilder> builderHashMap = new HashMap<>();
    public static HashMap<Player, Inventory> inventories = new HashMap<>();

    private static void reorganizeItems(ItemBuilder builder, Inventory inv) {
        ItemStack item = builder.build().buildStack();
        ItemMeta im = item.getItemMeta();
        List<Component> lore = im.lore();

        lore.add(Component.text("-                        -")
                .color(NamedTextColor.GRAY).decorate(TextDecoration.STRIKETHROUGH));
        lore.add(Component.text("➳ Clique direito ou esquerdo - Pegar item")
                .color(NamedTextColor.YELLOW));
        lore.add(Component.text("➳ Clique direito + shift - Trocar item vanilla base")
                .color(NamedTextColor.YELLOW));

        im.lore(lore);
        item.setItemMeta(im);

        inv.setItem(4, item);

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

        String[] str = new String[] {"A","B","C"};

        ItemStack loreItem = new ItemStack(Material.PAPER);
        ItemMeta loreMeta = loreItem.getItemMeta();

        loreMeta.setDisplayName(Utils.c("&6&lDescrição do item"));

        List<String> lo = new ArrayList<>();
        lo.addAll(Arrays.asList("&7• Não é necessariamente obrigatório.", "&7• A descrição são textos visíveis no item.", " ", "&6Descrição:"));
        lo.addAll(builder.getLore());
        lo.add(" ");
        lo.addAll(Arrays.asList("&e➳ Clique esquerdo - Adicionar nova linha", "&e➳ Clique direito - Remover última linha"));

        lo = lo.stream()
                .map(a -> Utils.c("&7&o"+a))
                .collect(Collectors.toList());

        loreMeta.setLore(lo);
        loreItem.setItemMeta(loreMeta);

        inv.setItem(21, loreItem);

        inv.setItem(23, simpleButton(
                Material.BLAZE_POWDER, "Skills (poderes/magias)","• Você também pode adicionar skills", " (poderes) para os items.",
                " ",
                "&6Skills: &e"+( builder.getItemSkills().isEmpty() ? "Nenhuma skill" : builder.getItemSkills().stream()
                        .map(a -> "["+a.getMythicSkillId()+"] - "+a.getInteractionType().name()+", ")
                        .collect(Collectors.joining())),
                " ",
                "&e➳ Clique esquerdo - Adicionar nova skill",
                "&e➳ Clique direito - Remover última skill")
        );

    }

    public static void openMainMenu(Player player, ItemBuilder builder) {
        Inventory inv = Bukkit.createInventory(null, 27, "Customização de item");

        builderHashMap.put(player, builder);
        inventories.put(player, inv);

        reorganizeItems(builder, inv);

        player.openInventory(inv);
    }

    public static void reopenInventory(Player player) {
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

                            if(level < 0) {
                                p.sendMessage("O nível mínimo não deve ser negativo.");
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
                final String oldId = builder.generateId();
                switch(clickType) {
                    case RIGHT -> {
                        return;
                    }
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            builder.setDisplayName(response);
                            reopenInventory(p);

                            ItemHandler.deleteItemEntry(builder.getCategory(), oldId);
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

            // Lore do item
            case 21 -> {
                switch(clickType) {
                    case RIGHT -> builder.getLore().removeLast();
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            builder.getLore().add(response);
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            // Skills
            case 23 -> {
                switch(clickType) {
                    case RIGHT -> {

                        builder.getItemSkills().removeLast();

                    }
                    case LEFT -> {

                        ItemSkillsMenu.openInventory(p.getPlayer(), builder);

                    }
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
