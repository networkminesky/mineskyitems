package net.mineskyitems.gui;

import net.mineskyitems.entities.ItemBuilder;
import net.mineskyitems.entities.item.ItemSkill;
import net.mineskyitems.utils.ChatInputCallback;
import net.mineskyitems.utils.InteractionType;
import net.mineskyitems.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;

import static net.mineskyitems.gui.MenuUtils.simpleButton;

public class ItemSkillsMenu implements Listener {

    public static HashMap<Player, Inventory> skillsInventories = new HashMap<>();

    //                   PLAYER  | SKILL
    private static HashMap<Player, ItemSkill> skills = new HashMap<>();

    public static void closeInventory(Player player) {
        skills.remove(player);
        skillsInventories.remove(player);
        player.closeInventory();
    }

    public static void reopenInventory(Player player) {
        Inventory inv = skillsInventories.get(player);
        ItemSkill skill = skills.get(player);
        if(inv == null || skill == null)
            return;

        reorganizeItems(skill, inv);

        player.openInventory(inv);
    }

    private static void reorganizeItems(ItemSkill skill, Inventory inv) {
        inv.setItem(4, simpleButton(Material.BARRIER, "Voltar", "• Clique para voltar ou cancelar, caso você", " tenha editado uma skill, clique", " que a skill será salvada automaticamente."));

        inv.setItem(11, simpleButton(
                Material.ENDER_EYE, "Nome da skill mythic", "• Aqui você deve setar o nome da", " skill do plugin MythicMobs.",
                " ",
                "&6Atual: &e"+ skill.getMythicSkillId(),
                " ",
                "&e➳ Clique - Alterar skill mythic"
        ));

        inv.setItem(13, simpleButton(
                Material.PAPER, "Gatilho (trigger)", "• Qual ação o jogador deve executar", " segurando o item para usar a Skill?",
                " ",
                "&6Atual: &e"+ skill.getInteractionType().name(),
                " ",
                "&e➳ Clique - Alterar a ação necessária"
        ));

        inv.setItem(15, simpleButton(
                Material.CLOCK, "Cooldown", "• Qual o intervalo entre o uso dessa skill?", "• Cooldown em segundos.",
                " ",
                "&6Atual: &e"+ skill.getCooldown()+" segundos",
                " ",
                "&e➳ Clique - Alterar a ação necessária"
        ));
    }

    public static void openInventory(Player player, ItemBuilder builder) {
        Inventory inv = Bukkit.createInventory(null, 27, "Editando skills de "+builder.getDisplayName());

        final String skillid = String.valueOf(builder.getItemSkills().size() + 1);
        ItemSkill skill = new ItemSkill(skillid, InteractionType.LEFT_CLICK, 3, "nenhum");

        skills.put(player, skill);

        reorganizeItems(skill, inv);

        skillsInventories.put(player, inv);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if(skillsInventories.containsValue(e.getInventory())) {

        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if(skillsInventories.containsValue(e.getInventory()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        final Player p = (Player) e.getWhoClicked();
        final int slot = e.getSlot();
        final ClickType clickType = e.getClick();

        if(!skillsInventories.containsValue(e.getInventory()))
            return;

        e.setCancelled(true);

        switch (slot) {
            // Voltar
            case 4 -> {
                ItemBuilder builder = ItemBuilderMenu.builderHashMap.get(p);
                ItemSkill skill = skills.get(p);

                if(!skill.getMythicSkillId().equalsIgnoreCase("nenhum")) {
                    List<ItemSkill> skillList = builder.getItemSkills();
                    skillList.add(skill);

                    builder.setItemSkills(skillList);
                }

                closeInventory(p);
                ItemBuilderMenu.reopenInventory(p);
            }

            // Nome de skills mythic
            case 11 -> Utils.awaitChatInput(p, new ChatInputCallback() {
                @Override
                public void onInput(String response) {
                    skills.get(p).setMythicSkillId(response);
                    reopenInventory(p);
                }

                @Override
                public void onCancel() {
                    reopenInventory(p);
                }
            });

            // Skill interaction type
            case 13 -> {
                InteractionType type = Utils.convertInteractionType(clickType);
                skills.get(p).setInteractionType(type);

                reopenInventory(p);
                p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1);

                return;
            }

            // Skill cooldown
            case 15 -> Utils.awaitChatInput(p, new ChatInputCallback() {
                @Override
                public void onInput(String response) {
                    float value = 5;
                    try {
                        value = Float.valueOf(response);
                    } catch (Exception ex) {
                        p.sendMessage("Insira um número valido em segundos.");
                        reopenInventory(p);
                        return;
                    }

                    skills.get(p).setCooldown(value);
                    reopenInventory(p);
                }

                @Override
                public void onCancel() {
                    reopenInventory(p);
                }
            });
        }

        switch(clickType) {
            case LEFT -> p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1);
            default -> {
                p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 0);
                reopenInventory(p);
            }
        }
    }

}
