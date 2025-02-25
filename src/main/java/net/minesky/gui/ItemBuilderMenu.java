package net.minesky.gui;

import net.minesky.utils.ItemBuilder;
import net.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ItemBuilderMenu implements Listener {

    public static HashMap<Player, ItemBuilder> builderHashMap = new HashMap<>();

    private static ItemStack simpleButton(Material m, String name, String... lore) {
        simpleButton(m, name, 1, lore);
    }
    private static ItemStack simpleButton(Material m, String name, int count, String... lore) {
        ItemStack it = new ItemStack(m, count);
        ItemMeta im = it.getItemMeta();

        im.setDisplayName(Utils.c("&6"+name));
        im.setLore(Arrays.asList(lore));

        it.setItemMeta(im);
        return it;
    }
    public static Inventory mainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Customização de item");

        ItemBuilder builder = builderHashMap.get(player);

        inv.setItem(3, builder.getItemStack());

        inv.setItem(5, simpleButton(
                Material.DRAGON_BREATH, "Modificar item base", "Clique aqui para", "modificar o item base.")
        );

        inv.setItem(10, simpleButton(
                Material.PLAYER_HEAD, "Classe necessária", "Define classe(s) obrigatória(s)", "para usar esse item.",
                " ",
                "&6Classe: &e"+builder.getPlayerClass(),
                " ",
                "&eClique esquerdo - Definir classe(s)",
                "&eClique direito - Remover classe(s)")
        );

        inv.setItem(12, simpleButton(
                Material.EXPERIENCE_BOTTLE, "Level do item", builder.getItemLevel(),"Define um nível (level)", "para esse item", "Jogadores terão de ter", "O mesmo nível ou", "superior para usá-lo.",
                " ",
                "&6Level atual: &e"+(builder.getItemLevel() == -1 ? "Não" : builder.getItemLevel()),
                " ",
                "&eClique esquerdo - Definir nível",
                "&eClique direito - Remover nível")
        );

        inv.setItem(14, simpleButton(
                Material.NAME_TAG, "Nome", builder.getItemLevel(),"Define um nome de exibição", "para o seu item",
                " ",
                "&6Nome: &e"+( builder.getDisplayName().isEmpty() ? "Sem nome" : builder.getDisplayName()),
                " ",
                "&eClique esquerdo - Definir nível",
                "&eClique direito - Remover nível")
        );

        inv.setItem(16, simpleButton(
                Material.MAGMA_CREAM, "Modelo do item", builder.getItemLevel(),"Todos os itens possuem", "um número para definir", "o seu modelo.",
                " ",
                "&6Modelo: &e"+( builder.getCustomModel() == 0 ? "Sem modelo" : builder.getCustomModel()),
                " ",
                "&eClique esquerdo - Definir nível",
                "&eClique direito - Remover nível")
        );

        return inv;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {

    }

}
