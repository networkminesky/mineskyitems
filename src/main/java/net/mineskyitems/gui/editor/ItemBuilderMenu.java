package net.mineskyitems.gui.editor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.ItemBuilder;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.utils.ChatInputCallback;
import net.mineskyitems.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        ItemStack item = builder.getItemStack();
        ItemMeta im = item.getItemMeta();
        List<Component> lore = im.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

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
                "&6Classe: &e" + (builder.getPlayerClass().isEmpty() ? "Nenhuma" : String.join(", ", builder.getPlayerClass())),
                " ",
                "&e➳ Clique esquerdo - Adicionar classe",
                "&e➳ Clique direito - Limpar classe(s)")
        );

        inv.setItem(12, simpleButton(
                Material.EXPERIENCE_BOTTLE, "Level do item", (builder.getItemLevel() <= 0 ? 1 : builder.getItemLevel()), "• Define um nível mínimo", " para jogadores usarem este item.",
                " ",
                "&6Level atual: &e" + (builder.getItemLevel() <= 0 ? "Nenhum" : builder.getItemLevel()),
                " ",
                "&e➳ Clique esquerdo - Definir nível",
                "&e➳ Clique direito - Remover nível")
        );

        inv.setItem(14, simpleButton(
                Material.NAME_TAG, "Nome", "• Define o nome de exibição", "  do seu item.",
                " ",
                "&6Nome: &e" + (builder.getDisplayName().isEmpty() ? "Sem nome" : builder.getDisplayName()),
                " ",
                "&e➳ Clique esquerdo - Alterar nome",
                "&e➳ Clique direito - Remover formatação")
        );

        inv.setItem(16, simpleButton(
                Material.MAGMA_CREAM, "Modelo do item", (builder.getCustomModel() <= 0 ? 1 : builder.getCustomModel()), "• Número identificador do", " modelo customizado (ResourcePack).",
                " ",
                "&6Modelo atual: &e" + (builder.getCustomModel() <= 0 ? "Padrão (0)" : builder.getCustomModel()),
                " ",
                "&e➳ Clique esquerdo - Definir modelo",
                "&e➳ Clique direito - Remover modelo")
        );

        inv.setItem(18, simpleButton(
                Material.EMERALD, "Raridade do item", "• Permite forçar uma raridade fixa.", " ",
                "&6Raridade: &e" + (builder.getForceRarity() == null ? "Automática (por nível)" : builder.getForceRarity()),
                " ",
                "&e➳ Clique esquerdo - Definir raridade",
                "&e➳ Clique direito - Modo automático")
        );

        ItemStack loreItem = new ItemStack(Material.PAPER);
        ItemMeta loreMeta = loreItem.getItemMeta();
        if (loreMeta != null) {
            loreMeta.displayName(Component.text(Utils.c("&6&lDescrição do item")));

            List<String> lo = new ArrayList<>();
            lo.addAll(Arrays.asList("&7• Textos adicionais visíveis no item.", " ", "&6Descrição atual:"));
            if (builder.getLore().isEmpty()) {
                lo.add("&8(Nenhuma linha definida)");
            } else {
                lo.addAll(builder.getLore());
            }
            lo.add(" ");
            lo.addAll(Arrays.asList(
                    "&e➳ Clique esquerdo - Adicionar nova linha",
                    "&e➳ Clique direito - Remover última linha",
                    "&e➳ Shift + Direito - Limpar tudo"
            ));

            lo = lo.stream().map(Utils::c).collect(Collectors.toList());
            loreMeta.setLore(lo);
            loreItem.setItemMeta(loreMeta);
        }
        inv.setItem(20, loreItem);

        if (builder.getCategory().isFood() || builder.getFoodMetadata() != null) {
            Item.FoodMetadata fm = builder.getFoodMetadata();
            int nutrition = fm != null ? fm.nutrition() : 0;
            float saturation = fm != null ? fm.saturation() : 0.0f;
            float seconds = fm != null ? fm.consumeSeconds() : 1.6f;

            inv.setItem(22, simpleButton(
                    Material.COOKED_BEEF, "Propriedades de Alimento", "• Configurações de consumo e regeneração.", " ",
                    "&6Nutrição: &e" + nutrition + " pernis",
                    "&6Saturação: &e" + saturation,
                    "&6Tempo de consumo: &e" + seconds + "s",
                    " ",
                    "&e➳ Clique esquerdo - Alterar Nutrição",
                    "&e➳ Shift + Clique esquerdo - Alterar Saturação",
                    "&e➳ Clique direito - Alterar Tempo de Consumo")
            );
        } else {
            inv.setItem(22, simpleButton(
                    Material.APPLE, "Tornar Alimento/Bebida", "• Esse item pertence a uma categoria comum.", "  Clique para transformá-lo em alimento.",
                    " ",
                    "&e➳ Clique esquerdo - Ativar propriedades de comida")
            );
        }

        inv.setItem(24, simpleButton(
                Material.BLAZE_POWDER, "Skills (Magias/Poderes)", "• Habilidades ativadas por interação.",
                " ",
                "&6Skills ativas: &e" + (builder.getItemSkills().isEmpty() ? "Nenhuma" : builder.getItemSkills().stream()
                        .map(a -> "[" + a.getMythicSkillId() + "]")
                        .collect(Collectors.joining(", "))),
                " ",
                "&e➳ Clique esquerdo - Abrir menu de skills",
                "&e➳ Clique direito - Remover última skill")
        );

        inv.setItem(26, simpleButton(
                Material.CLOCK, "Revisão do Item", (builder.getRevision() <= 0 ? 1 : builder.getRevision()), "• Controla atualizações automáticas", "  nos itens dos jogadores.",
                " ",
                "&6Revisão atual: &a" + builder.getRevision(),
                " ",
                "&e➳ Clique esquerdo - Incrementar (+1)",
                "&e➳ Shift + Clique esquerdo - Digitar número",
                "&e➳ Clique direito - Zerar revisão")
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
        if (inv == null || builder == null)
            return;

        reorganizeItems(builder, inv);

        player.getScheduler().run(MineSkyItems.getInstance(), (task) -> {
            player.openInventory(inv);
        }, null);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (inventories.containsValue(e.getInventory()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        final Player p = (Player) e.getWhoClicked();
        final int slot = e.getSlot();
        final ClickType clickType = e.getClick();

        if (!inventories.containsValue(e.getInventory()))
            return;

        e.setCancelled(true);

        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;

        ItemBuilder builder = builderHashMap.get(p);
        if (builder == null)
            return;

        switch (slot) {
            case 4 -> {
                switch (clickType) {
                    case RIGHT, LEFT -> {
                        if (builder.isStub()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                            return;
                        }
                        ItemStack stack = builder.build().buildStack();
                        p.getInventory().addItem(stack);
                    }
                    case SHIFT_RIGHT -> {
                        Utils.awaitChatInput(p, new ChatInputCallback() {
                            @Override
                            public void onInput(String response) {
                                Material material = Material.getMaterial(response.toUpperCase().trim());
                                if (material == null) {
                                    material = builder.getCategory().getDefaultItem();
                                }
                                builder.setMaterial(material);
                                builder.build();
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

            case 10 -> {
                switch (clickType) {
                    case RIGHT -> {
                        builder.setPlayerClass(new ArrayList<>());
                        builder.build();
                        reopenInventory(p);
                    }
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            List<String> classes = builder.getPlayerClass();
                            classes.add(response.trim());
                            builder.setPlayerClass(classes);
                            builder.build();
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            case 12 -> {
                switch (clickType) {
                    case RIGHT -> {
                        builder.setItemLevel(0);
                        builder.build();
                        reopenInventory(p);
                    }
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            int level;
                            try {
                                level = Integer.parseInt(response.trim());
                            } catch (Exception ex) {
                                p.sendMessage("§cInsira um número válido.");
                                reopenInventory(p);
                                return;
                            }
                            if (level < 0) {
                                p.sendMessage("§cO nível mínimo não pode ser negativo.");
                                reopenInventory(p);
                                return;
                            }
                            builder.setItemLevel(level);
                            builder.build();
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            case 14 -> {
                final String oldId = builder.generateId();
                switch (clickType) {
                    case RIGHT -> {
                        builder.setDisplayName(ChatColor.stripColor(builder.getDisplayName()));
                        builder.build();
                        reopenInventory(p);
                    }
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            builder.setDisplayName(Utils.c(response));
                            ItemHandler.deleteItemEntry(builder.getCategory(), oldId);
                            builder.setId(null);
                            builder.build();
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            case 16 -> {
                switch (clickType) {
                    case RIGHT -> {
                        builder.setCustomModel(0);
                        builder.build();
                        reopenInventory(p);
                    }
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            int model;
                            try {
                                model = Integer.parseInt(response.trim());
                            } catch (Exception ex) {
                                p.sendMessage("§cInsira um número válido.");
                                reopenInventory(p);
                                return;
                            }
                            builder.setCustomModel(model);
                            builder.build();
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            case 18 -> {
                switch (clickType) {
                    case RIGHT -> {
                        builder.setForceRarity(null);
                        builder.build();
                        reopenInventory(p);
                    }
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            builder.setForceRarity(response.trim().toLowerCase());
                            builder.build();
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            case 20 -> {
                if (clickType == ClickType.SHIFT_RIGHT) {
                    builder.getLore().clear();
                    builder.build();
                    reopenInventory(p);
                    return;
                }
                switch (clickType) {
                    case RIGHT -> {
                        if (!builder.getLore().isEmpty()) {
                            builder.getLore().removeLast();
                            builder.build();
                            reopenInventory(p);
                        }
                    }
                    case LEFT -> Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            builder.getLore().add(Utils.c(response));
                            builder.build();
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            case 22 -> {
                if (builder.getFoodMetadata() == null && !builder.getCategory().isFood()) {
                    builder.setFoodMetadata(new Item.FoodMetadata(4, 2.0f, 1.6f));
                    builder.build();
                    reopenInventory(p);
                    return;
                }

                Item.FoodMetadata current = builder.getFoodMetadata() != null ? builder.getFoodMetadata() : new Item.FoodMetadata(4, 2.0f, 1.6f);

                if (clickType == ClickType.SHIFT_LEFT) {
                    Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            try {
                                float sat = Float.parseFloat(response.trim());
                                builder.setFoodMetadata(new Item.FoodMetadata(current.nutrition(), sat, current.consumeSeconds()));
                                builder.build();
                            } catch (Exception ex) {
                                p.sendMessage("§cValor de saturação inválido.");
                            }
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                } else if (clickType == ClickType.LEFT) {
                    Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            try {
                                int nut = Integer.parseInt(response.trim());
                                builder.setFoodMetadata(new Item.FoodMetadata(nut, current.saturation(), current.consumeSeconds()));
                                builder.build();
                            } catch (Exception ex) {
                                p.sendMessage("§cValor de nutrição inválido.");
                            }
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                } else if (clickType == ClickType.RIGHT) {
                    Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            try {
                                float sec = Float.parseFloat(response.trim());
                                builder.setFoodMetadata(new Item.FoodMetadata(current.nutrition(), current.saturation(), sec));
                                builder.build();
                            } catch (Exception ex) {
                                p.sendMessage("§cTempo de consumo inválido.");
                            }
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                }
            }

            case 24 -> {
                switch (clickType) {
                    case RIGHT -> {
                        if (!builder.getItemSkills().isEmpty()) {
                            builder.getItemSkills().removeLast();
                            builder.build();
                            reopenInventory(p);
                        }
                    }
                    case LEFT -> ItemSkillsMenu.openInventory(p, builder);
                }
            }

            case 26 -> {
                if (clickType == ClickType.SHIFT_LEFT) {
                    Utils.awaitChatInput(p, new ChatInputCallback() {
                        @Override
                        public void onInput(String response) {
                            try {
                                int rev = Integer.parseInt(response.trim());
                                builder.setRevision(Math.max(0, rev));
                                builder.build();
                            } catch (Exception ex) {
                                p.sendMessage("§cNúmero de revisão inválido.");
                            }
                            reopenInventory(p);
                        }

                        @Override
                        public void onCancel() {
                            reopenInventory(p);
                        }
                    });
                    return;
                }
                switch (clickType) {
                    case RIGHT -> {
                        builder.setRevision(0);
                        builder.build();
                        reopenInventory(p);
                    }
                    case LEFT -> {
                        builder.setRevision(builder.getRevision() + 1);
                        builder.build();
                        reopenInventory(p);
                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
                    }
                }
            }
        }

        switch (clickType) {
            case RIGHT -> p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 0);
            case LEFT -> p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1);
        }
    }
}