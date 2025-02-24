package net.minesky.commands;

import net.minesky.config.ItemConfig;
import net.minesky.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;

public class CriarItemsCommand implements CommandExecutor, Listener {

    private final Map<Player, ItemConfig> itemConfigMap = new HashMap<>();
    @Override
    public boolean onCommand(CommandSender s, Command command, String lbl, String[] args) {
        if (!s.hasPermission("mineskyitems.criador")) {
            s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
            return true;
        }
        if (s instanceof Player) {
            Player player = (Player) s;
            abrirMenu(player);
            return true;
        }
        Utils.Logger(Level.SEVERE, "Vaza daqui o console >:C");
        return true;
    }

    private void abrirMenu(Player player) {
        ItemConfig config = new ItemConfig();
        itemConfigMap.put(player, config);

        Inventory menu = Bukkit.createInventory(null, 27, "Configurar itens");

        updateMenu(player, menu);
        player.openInventory(menu);
    }

    public void updateMenu(Player player, Inventory menu) {
        ItemConfig config = itemConfigMap.get(player);

        // Botão de Nome
        ItemStack nameItem = new ItemStack(Material.PAPER);
        ItemMeta nameMeta = nameItem.getItemMeta();
        nameMeta.setDisplayName("§7Nome: §a" + config.getName());
        nameMeta.setLore(Collections.singletonList("§eClique para alterar o nome."));
        nameItem.setItemMeta(nameMeta);
        menu.setItem(10, nameItem);

        // Botão de material
        ItemStack materialItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta materialMeta = materialItem.getItemMeta();
        materialMeta.setDisplayName("§7Material: §a" + config.getMaterialName());
        materialMeta.setLore(Collections.singletonList("§eClique para alternar."));
        materialItem.setItemMeta(materialMeta);
        menu.setItem(12, materialItem);

        // Botão de Model
        ItemStack modelItem = new ItemStack(Material.PAINTING);
        ItemMeta modelMeta = modelItem.getItemMeta();
        modelMeta.setDisplayName("§7Model (CustomModelData): §a" + config.getModel());
        modelMeta.setLore(Collections.singletonList("§eClique para alternar."));
        modelItem.setItemMeta(modelMeta);
        menu.setItem(13, modelItem);

        // Botão de Level
        ItemStack levelItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta levelMeta = levelItem.getItemMeta();
        levelMeta.setDisplayName("§7Level: §a" + config.getLevel());
        levelMeta.setLore(Collections.singletonList("§eClique para alternar."));
        levelItem.setItemMeta(levelMeta);
        menu.setItem(14, levelItem);

        // Botão de classe
        ItemStack classeItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta classeMeta = classeItem.getItemMeta();
        classeMeta.setDisplayName("§7Classe: §a" + config.getClasse());
        classeMeta.setLore(Collections.singletonList("§eClique para alternar."));
        classeItem.setItemMeta(classeMeta);
        menu.setItem(16, classeItem);

        // Botão de lore
        ItemStack loreItem = new ItemStack(Material.BIRCH_SIGN);
        ItemMeta loreMeta = loreItem.getItemMeta();
        loreMeta.setDisplayName("§7Lore");
        List<String> lore = new ArrayList<>();
        lore.add("§7Lore do item:");
        lore.addAll(config.getLore());
        lore.add("§eClique para alternar.");
        loreMeta.setLore(lore);
        loreItem.setItemMeta(loreMeta);
        menu.setItem(22, loreItem);

        // ITEM
        ItemStack item = new ItemStack(config.getMaterial());
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(config.getName());
        itemMeta.setCustomModelData(config.getModel());
        itemMeta.setLore(config.getLore());
        item.setItemMeta(itemMeta);
        menu.setItem(4, item);

        // Botão de Salvar
        ItemStack saveItem = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta saveMeta = saveItem.getItemMeta();
        saveMeta.setDisplayName("§aSalvar Configurações");
        saveItem.setItemMeta(saveMeta);
        menu.setItem(2, saveItem);

        // Botão de cancelar
        ItemStack cancelItem = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName("§cCancelar");
        cancelItem.setItemMeta(cancelMeta);
        menu.setItem(6, cancelItem);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory menu = event.getClickedInventory();

        if (menu == null || !event.getView().getTitle().equals("Configurar itens")) return;

        event.setCancelled(true);
        //IMPLEMENTAR DADOS MO PREGUIÇA
    }
}
