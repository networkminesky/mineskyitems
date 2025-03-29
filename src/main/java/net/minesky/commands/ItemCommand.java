package net.minesky.commands;

import net.minesky.MineSkyItems;
import net.minesky.gui.ItemBuilderMenu;
import net.minesky.handler.Item;
import net.minesky.handler.ItemBuilder;
import net.minesky.handler.ItemHandler;
import net.minesky.handler.categories.Category;
import net.minesky.handler.categories.CategoryHandler;
import net.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ItemCommand implements TabExecutor {

    public static final List<String> subCommands = Arrays.asList("criar", "editar", "give", "get", "reload", "achar");

    void commandList(CommandSender s) {
        s.sendMessage(Utils.PURPLE_COLOR+Utils.c("&lMineSkyItems v"+MineSkyItems.getInstance().getDescription().getVersion()));
        s.sendMessage(Utils.c(
                Utils.PURPLE_COLOR+"/item criar <categoria> &8- &7Cria um novo item\n"+
                        Utils.PURPLE_COLOR+"/item editar <nome> &8- &7Edita um item já criado a partir do nome\n"+
                        Utils.PURPLE_COLOR+"/item give <player> <nome> &8- &7Pega uma cópia do item a partir do nome para um jogador\n"+
                        Utils.PURPLE_COLOR+"/item get <nome> &8- &7Pega uma cópia do item a partir do nome\n"+
                        Utils.PURPLE_COLOR+"/item reload &8- &7Recarregar o plugin (não recomendado)\n"+
                        Utils.PURPLE_COLOR+"/item achar [id, nome ou nada] &8- &7Procura um item pela parte do nome dele, ou pelo seu ID, ou pelo item em sua mão."
        ));
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("mineskyitems.item")) {
            s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
            return true;
        }

        if(s instanceof Player p) {
            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1);
        }

        if(args.length == 0) {
            commandList(s);
            return true;
        }

        if(args[0].equalsIgnoreCase("reload")) {
            s.sendMessage("Recarregando...");
            MineSkyItems.reload();
        }

        // give <player> <nome>
        if(args.length >= 3) {
            if(args[0].equalsIgnoreCase("give")) {
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
            s.sendMessage("Apenas jogadores in-game podem utilizar esse comando.");
            return true;
        }

        // criar, achar, get, editar
        if(args.length >= 2) {
            final String prompt = args[1];

            if(args[0].equalsIgnoreCase("get")) {
                String itemName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                giveItemByName((Player) s, itemName);

                return true;
            }

            if(args[0].equalsIgnoreCase("criar")) {

                Category category = CategoryHandler.getCategory(prompt);

                if(category == null) {
                    s.sendMessage("Nenhuma categoria encontrada com esse Nome ou ID.");
                    return true;
                }

                s.sendMessage("Criando um novo item na categoria "+category.getName());
                ItemBuilderMenu.openMainMenu(p, new ItemBuilder(category));

            }

            if(args[0].equalsIgnoreCase("editar")) {

            }

        }

        return false;
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
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        if(s instanceof Player p) {
            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 1);
        }

        if(args.length <= 1) {
            return subCommands;
        }

        if(args[0].equalsIgnoreCase("criar")) {
            return CategoryHandler.getCategoriesString();
        }

        if(args[0].equalsIgnoreCase("get")) {
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

            String[] args2 = Arrays.copyOfRange(args, 2, args.length);

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

            // Adiciona o elemento à lista filtrada se contiver a substring desejada
            if (!elemento.toLowerCase().contains(input.toLowerCase())) {
                iterator.remove();
            } else {
                if (length >= 2) {
                    iterator.remove();
                    elementosAdicionais.add(elemento.toLowerCase().replace(input.toLowerCase(), "").trim());
                }
            }
        }

        // Adiciona os elementos adicionais à lista original após concluir a iteração
        list.addAll(elementosAdicionais);
    }

}
