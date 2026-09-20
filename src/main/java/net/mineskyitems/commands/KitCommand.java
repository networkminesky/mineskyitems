package net.mineskyitems.commands;

import net.mineskyitems.gui.kits.KitListGUI;
import net.mineskyitems.entities.kits.KitHandler;
import net.mineskyitems.entities.kits.KitHandler.Kit;
import net.mineskyitems.gui.kits.KitPreviewGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KitCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores in-game podem abrir menus de kit.");
            return true;
        }

        if (args.length == 0) {
            KitListGUI.openGUI(player);
            return true;
        }

        String kitId = args[0].toLowerCase();
        Kit kit = KitHandler.getKit(kitId);
        if (kit == null) {
            player.sendMessage(KitHandler.parseComponent("<red>Nenhum kit encontrado com o ID: " + kitId + "</red>"));
            return true;
        }

        KitPreviewGUI.open(player, kit);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return KitHandler.getAllKits().stream()
                    .map(Kit::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}