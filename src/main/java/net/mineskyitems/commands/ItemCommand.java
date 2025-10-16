package net.mineskyitems.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.events.DummyEvent;
import net.mineskyitems.gui.ItemBuilderMenu;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.ItemBuilder;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.entities.categories.Category;
import net.mineskyitems.entities.categories.CategoryHandler;
import net.mineskyitems.gui.blacksmith.ItemRecyclerMenu;
import net.mineskyitems.gui.blacksmith.ItemRepairMenu;
import net.mineskyitems.scripts.ArmorStandScript;
import net.mineskyitems.scripts.ItemFrameGenerator;
import net.mineskyitems.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemCommand implements TabExecutor {

    public static final List<String> subCommands = Arrays.asList("criar", "script", "get-all", "category", "editar", "give", "get", "reload", "achar", "deletar", "danificar", "menu");
    public static final List<String> menu_subCommands = Arrays.asList("reparar", "destruir");
    public static final List<String> scripts = Arrays.asList("empty", "category", "single", "armor");

    void commandList(CommandSender s) {
        s.sendMessage(Utils.PURPLE_COLOR+Utils.c("&lMineSkyItems v"+MineSkyItems.getInstance().getDescription().getVersion()));
        s.sendMessage(Utils.c(
                Utils.PURPLE_COLOR+"/item criar <categoria> &8- &7Cria um novo item\n"+
                        Utils.PURPLE_COLOR+"/item editar <nome> &8- &7Edita um item já criado a partir do nome\n"+
                        Utils.PURPLE_COLOR+"/item give <player> <nome> &8- &7Pega uma cópia do item a partir do nome para um jogador\n"+
                        Utils.PURPLE_COLOR+"/item deletar <nome> &8- &7Deleta um item existente\n"+
                        Utils.PURPLE_COLOR+"/item get <nome> &8- &7Pega uma cópia do item a partir do nome\n"+
                        Utils.PURPLE_COLOR+"/item get-all <categoria> &8- &7Pega uma cópia de todos os itens de uma categoria\n"+
                        Utils.PURPLE_COLOR+"/item category <categoria> &8- &7Lista a categoria com seus devidos itens\n"+
                        Utils.PURPLE_COLOR+"/item get <nome> &8- &7Pega uma cópia do item a partir do nome\n"+
                        Utils.PURPLE_COLOR+"/item menu <menu> &8- &7Abre um menu de item, ex: menu de destruir itens para virar pó\n"+
                        Utils.PURPLE_COLOR+"/item reload &8- &7Recarregar o plugin (não recomendado)\n"+
                        Utils.PURPLE_COLOR+"/item danificar <dano> &8- &7Danifica o item de sua mão na quantidade informada\n"+
                        Utils.PURPLE_COLOR+"/item achar [id, nome ou nada] &8- &7Procura um item pela parte do nome dele, ou pelo seu ID, ou pelo item em sua mão."
        ));
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if(s instanceof Player p) {
            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1);
        }

        if(args.length == 0) {
            if(!s.hasPermission("mineskyitems.command.help"))
                s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
            else commandList(s);
            return true;
        }

        if (!s.hasPermission("mineskyitems.command."+args[0].toLowerCase())) {
            s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
            return true;
        }

        if(args[0].equalsIgnoreCase("reload")) {
            s.sendMessage("§7Recarregando arquivos...");
            CategoryHandler.categories.forEach(category -> {
                category.reloadFile();
                category.reloadCategory();
            });
            s.sendMessage("§aCategorias recarregadas! "+ItemHandler.getItemsNames().size()+" itens ativos.");
            return true;
        }

        // give <player> <nome>
        if(args.length >= 3) {
            if(args[0].equalsIgnoreCase("give")) {
                if(!s.hasPermission("mineskyitems.command.give")) {
                    s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
                    return true;
                }

                Player player = Bukkit.getPlayer(args[1]);
                if(player == null) {
                    s.sendMessage(Utils.c("&cNinguém encontrado com esse nick. Você pode usar /item get (nome) para dar o item a você mesmo."));
                    return true;
                }

                String itemName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                giveItemByName(player, itemName);

                return true;
            }
        }

        if(!(s instanceof Player p)) {
            s.sendMessage("§cApenas jogadores in-game podem utilizar esse comando.");
            return true;
        }

        if(args[0].equalsIgnoreCase("achar")) {
            if(!s.hasPermission("mineskyitems.command.achar")) {
                s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
                return true;
            }

            Item item;

            // Comando possui input
            if(args.length >= 2) {
                String itemSearch = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();

                item = ItemHandler.getItem(itemSearch);
                if(item != null) {
                    itemInfo(p, item);
                    return true;
                }

                p.sendMessage("§7Nenhum item encontrado basedo em seu input.");
            }

            p.sendMessage("§aVerificando item em sua mão primária...");
            item = ItemHandler.getItemFromStack(p.getInventory().getItemInMainHand());
            if(item == null) {
                p.sendMessage("§cNenhum item encontrado nem em sua mão e nem no prompt do item. Verifique com um Admin ou Desenvolvedor do plugin.");
                return true;
            }

            itemInfo(p, item);
            return true;
        }

        // criar, achar, get, editar, deletar, menu, script
        if(args.length >= 2) {
            final String prompt = args[1];

            if(!s.hasPermission("mineskyitems.command."+args[0])) {
                s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
                return true;
            }

            if(args[0].equalsIgnoreCase("category")) {
                final Category category = CategoryHandler.getCategoryByName(args[1]);

                if (category == null) {
                    s.sendMessage("§cEssa categoria não existe!");
                    return true;
                }

                s.sendMessage("§6Nome: §f"+category.getName());
                s.sendMessage("§6ID: §f"+category.getId());
                s.sendMessage("§6Tipo: §f"+category.getType());
                s.sendMessage("§fListando todos os §e"+category.getAllItems().size()+" §fitens...");
                category.getAllItems().stream().sorted(Comparator.comparingInt(Item::getRequiredLevel))
                        .forEach(item -> {
                    s.sendMessage("§6• §e"+item.getMetadata().displayName()+"§f- Modelo: §a"+item.getMetadata().modelData()+"§f, Level: §d"+item.getRequiredLevel());
                });

                return true;
            }

            if(args[0].equalsIgnoreCase("get-all")) {
                final Category category = CategoryHandler.getCategoryByName(args[1]);

                if (category == null) {
                    s.sendMessage("§cEssa categoria não existe!");
                    return true;
                }

                category.getAllItems().stream().sorted(Comparator.comparingInt(Item::getRequiredLevel))
                        .forEach(item -> {
                    p.getInventory().addItem(item.buildStack());
                });

                return true;
            }

            if(args[0].equalsIgnoreCase("get")) {
                String itemName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                giveItemByName((Player) s, itemName);

                return true;
            }

            if(args[0].equalsIgnoreCase("menu")) {
                if(prompt.equalsIgnoreCase("destruir")) {

                    ItemRecyclerMenu.openMainMenu(p);

                } else {

                    ItemRepairMenu.openMainMenu(p);

                }

                return true;
            }

            if(args[0].equalsIgnoreCase("deletar")) {
                String itemName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                Item item = ItemHandler.getItemByName(itemName);
                if(item != null) {
                    ItemHandler.deleteItem(item);

                    s.sendMessage("§aItem deletado com sucesso!");
                    return true;
                }

                s.sendMessage("§cNenhum item existe com esse nome.");

                return true;
            }

            if(args[0].equalsIgnoreCase("script")) {
                final String scriptArgs = args.length > 2 ? args[2] : "";
                final String script = args[1].toLowerCase();

                switch(script) {
                    case "empty" -> {
                        int startingFrom = 0;
                        if(args.length >= 4) {
                            startingFrom = Integer.parseInt(args[3]);
                        }

                        ItemFrameGenerator.generateEmpty(p.getLocation(), Material.getMaterial(scriptArgs), startingFrom);
                    }
                    case "single" -> {
                        int model = 0;
                        if(args.length >= 4) {
                            model = Integer.parseInt(args[3]);
                        }

                        ItemFrameGenerator.createItem(p.getLocation(), Material.getMaterial(scriptArgs), model);
                    }
                    case "category" -> ItemFrameGenerator.generateCategory(p.getLocation(), CategoryHandler.getCategory(scriptArgs));
                    case "armor" -> ArmorStandScript.generate(p.getLocation(), scriptArgs);

                    default -> p.sendMessage("Script não encontrado, scripts existentes: "+scripts);
                }

                return true;
            }

            if(args[0].equalsIgnoreCase("danificar")) {
                int damage;

                try {
                    damage = Integer.parseInt(prompt);
                } catch (Exception ex) {
                    p.sendMessage("§cInforme um número válido de quantidade de dano para este item.");
                    return true;
                }

                Item item = ItemHandler.getItemFromStack(p.getInventory().getItemInMainHand());
                if(item == null) {
                    p.sendMessage("§cVocê deve segurar um item válido em sua mão para alterar a durabilidade dele.");
                    return true;
                }

                item.damageItem(p, p.getInventory().getItemInMainHand(), damage, new DummyEvent());
                return true;
            }

            if(args[0].equalsIgnoreCase("criar")) {
                Category category = CategoryHandler.getCategory(prompt);

                if(category == null) {
                    s.sendMessage("Nenhuma categoria encontrada com esse Nome ou ID.");
                    return true;
                }

                s.sendMessage("§aCriando um novo item na categoria "+category.getName());
                ItemBuilderMenu.openMainMenu(p, new ItemBuilder(category));

                return true;
            }

            if(args[0].equalsIgnoreCase("editar")) {
                String itemName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                Item item = ItemHandler.getItemByName(itemName);
                if(item != null) {

                    ItemBuilderMenu.openMainMenu(p, new ItemBuilder(item));
                    return true;

                }

                s.sendMessage("§cNenhum item existe com esse nome.");
                return true;
            }

        }

        return false;
    }

    private static void itemInfo(Player p, Item item) {
        p.sendMessage("§6§lItem encontrado!");
        p.sendMessage("§6Nome: §e"+item.getMetadata().displayName());
        p.sendMessage("§6ID: §e"+item.getId());
        p.sendMessage("§6Level: §e"+item.getRequiredLevel());
        p.sendMessage("§6Categoria: §e"+item.getCategory().getName());

        Component component = Component.text("Clique aqui para editar esse item.")
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/item editar "+ChatColor.stripColor(item.getMetadata().displayName())));

        p.sendMessage(component);
    }

    private void giveItemByName(Player player, String name) {
        Item item = ItemHandler.getItemByName(name);
        if(item == null) {
            player.sendMessage(Utils.c("&cNenhum item encontrado com esse nome."));
            return;
        }

        player.getInventory().addItem(item.buildStack());
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] args) {
        if(s instanceof Player p) {
            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 1);
        }

        if(args.length <= 1) {
            return subCommands;
        }

        if(args[0].equalsIgnoreCase("danificar")
        || args[0].equalsIgnoreCase("reload")) {
            return null;
        }

        if(args[0].equalsIgnoreCase("script")) {
            if(args.length == 2) {
                return scripts;
            } else if(args[1].equalsIgnoreCase("category")) {
                return CategoryHandler.getCategoriesNames();
            }
        }

        if(args[0].equalsIgnoreCase("menu")) {
            return menu_subCommands;
        }

        if(args[0].equalsIgnoreCase("achar")) {
            String[] args2 = Arrays.copyOfRange(args, 1, args.length);

            List<String> e = new ArrayList<>(ItemHandler.getItemsNamesAndIds());
            String input = String.join(" ", args2);

            reorganizeTabComplete(e, input, args2.length);

            return e;
        }

        if(args[0].equalsIgnoreCase("criar")
        || args[0].equalsIgnoreCase("get-all")
        || args[0].equalsIgnoreCase("category")) {
            String[] args2 = Arrays.copyOfRange(args, 1, args.length);

            List<String> e = new ArrayList<>(CategoryHandler.getCategoriesNames());
            String input = String.join(" ", args2);

            reorganizeTabComplete(e, input, args2.length);

            return e;
        }

        if(args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("deletar")) {
            String[] args2 = Arrays.copyOfRange(args, 1, args.length);

            List<String> e = new ArrayList<>(ItemHandler.getItemsNames());
            String input = String.join(" ", args2);

            reorganizeTabComplete(e, input, args2.length);

            return e;
        }

        if(args[0].equalsIgnoreCase("give")) {
            if(args.length == 2) {
                return null;
            }

            String[] args2 = Arrays.copyOfRange(args, 3, args.length);

            List<String> e = new ArrayList<>(ItemHandler.getItemsNames());
            String input = String.join(" ", args2);

            reorganizeTabComplete(e, input, args2.length);

            return e;
        }

        //return List.of();
        return ItemHandler.getItemsNames();
    }

    private static void reorganizeTabComplete(List<String> list, String input, int length) {
        Iterator<String> iterator = list.iterator();
        List<String> elementosAdicionais = new ArrayList<>();

        while (iterator.hasNext()) {
            String elemento = iterator.next();
            String[] elementoArgs = elemento.split(" ");

            if (!elemento.toLowerCase().contains(input.toLowerCase())) {
                iterator.remove();
            } else {
                if (length >= 2) {
                    iterator.remove();
                    elementosAdicionais.add(elemento.toLowerCase().replace(input.toLowerCase(), "").trim());
                }
            }
        }

        list.addAll(elementosAdicionais);
    }

}
