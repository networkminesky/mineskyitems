package net.minesky.commands;

import net.minesky.MineSkyItems;
import net.minesky.config.ItemConfig;
import net.minesky.gui.ItemBuilderMenu;
import net.minesky.handler.Item;
import net.minesky.handler.ItemHandler;
import net.minesky.handler.categories.Category;
import net.minesky.handler.categories.CategoryHandler;
import net.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;

public class ItemCommand implements TabExecutor {

    public static final List<String> subCommands = Arrays.asList("criar", "editar", "give", "get");

    void commandList(CommandSender s) {
        s.sendMessage(Utils.c(
                "&c/item criar <categoria> &8- &7Cria um novo item"+
                        "\n&c/item editar <nome> &8- &7Edita um item já criado a partir do nome"+
                        "\n&c/item give <player> <nome> &8- &7Pega uma cópia do item a partir do nome para um jogador"+
                        "\n&c/item get <nome> &8- &7Pega uma cópia do item a partir do nome"+
                        "\n&c/item achar [id, nome ou nada] &8- &7Procura um item pela parte do nome dele, ou pelo seu ID, ou pelo item em sua mão."
        ));
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("mineskyitems.item")) {
            s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
            return true;
        }

        if(args.length == 0) {
            commandList(s);
            return true;
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

                Category category = CategoryHandler.getCategoryById(prompt);

                if(category == null) {
                    s.sendMessage("Nenhuma categoria encontrada com esse Nome ou ID.");
                }

                ItemBuilderMenu.mainMenu(p);

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

        //return List.of();
        return ItemHandler.getItemsNames();
    }
}
