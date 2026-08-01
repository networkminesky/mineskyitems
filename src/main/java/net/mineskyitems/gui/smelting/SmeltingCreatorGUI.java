package net.mineskyitems.gui.smelting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SmeltingCreatorGUI implements InventoryHolder, Listener {

    public static final int INPUT_SLOT = 19;
    public static final int RESULT_SLOT = 25;
    public static final int SAVE_SLOT = 49;

    public static final int FURNACE_SLOT = 38;
    public static final int BLAST_SLOT = 39;
    public static final int SMOKER_SLOT = 40;
    public static final int CAMPFIRE_SLOT = 41;

    public static final int TIME_DEC_SLOT = 30;
    public static final int TIME_DISP_SLOT = 31;
    public static final int TIME_INC_SLOT = 32;

    public static final int XP_DEC_SLOT = 12;
    public static final int XP_DISP_SLOT = 13;
    public static final int XP_INC_SLOT = 14;

    private final String recipeId;
    private final Inventory inventory;

    private boolean furnace = true;
    private boolean blastFurnace = true;
    private boolean smoker = false;
    private boolean campfire = false;

    private int cookingTime = 200; // Ticks (10 seg)
    private float experience = 0.1f;

    public SmeltingCreatorGUI(String recipeId) {
        this.recipeId = recipeId;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Criar Queima: " + recipeId, NamedTextColor.DARK_GRAY));
        setupGUI();
    }

    private void setupGUI() {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        inventory.setItem(INPUT_SLOT, null);
        inventory.setItem(RESULT_SLOT, null);

        inventory.setItem(22, createItem(Material.FLINT_AND_STEEL, Component.text("🔥 Queima", NamedTextColor.GOLD, TextDecoration.BOLD)));

        // Ajuste de XP
        inventory.setItem(XP_DEC_SLOT, createItem(Material.RED_STAINED_GLASS_PANE, Component.text("-0.1 XP", NamedTextColor.RED)));
        inventory.setItem(XP_INC_SLOT, createItem(Material.LIME_STAINED_GLASS_PANE, Component.text("+0.1 XP", NamedTextColor.GREEN)));

        // Ajuste de Tempo
        inventory.setItem(TIME_DEC_SLOT, createItem(Material.RED_STAINED_GLASS_PANE, Component.text("-1s Tempo", NamedTextColor.RED)));
        inventory.setItem(TIME_INC_SLOT, createItem(Material.LIME_STAINED_GLASS_PANE, Component.text("+1s Tempo", NamedTextColor.GREEN)));

        // Botão Salvar
        inventory.setItem(SAVE_SLOT, createItem(
                Material.LIME_STAINED_GLASS_PANE,
                Component.text("✔ Salvar Receita de Queima", NamedTextColor.GREEN, TextDecoration.BOLD),
                List.of(Component.text("Clique para ativar no servidor!", NamedTextColor.GRAY))
        ));

        updateDisplays();
        updateToggles();
    }

    private void updateDisplays() {
        inventory.setItem(XP_DISP_SLOT, createItem(
                Material.EXPERIENCE_BOTTLE,
                Component.text("XP: " + String.format("%.1f", experience), NamedTextColor.YELLOW, TextDecoration.BOLD)
        ));

        inventory.setItem(TIME_DISP_SLOT, createItem(
                Material.CLOCK,
                Component.text("Tempo: " + cookingTime + " ticks (" + (cookingTime / 20) + "s)", NamedTextColor.AQUA, TextDecoration.BOLD)
        ));
    }

    private void updateToggles() {
        inventory.setItem(FURNACE_SLOT, createToggleItem(Material.FURNACE, "Fornalha Normal", furnace));
        inventory.setItem(BLAST_SLOT, createToggleItem(Material.BLAST_FURNACE, "Alto-Forno", blastFurnace));
        inventory.setItem(SMOKER_SLOT, createToggleItem(Material.SMOKER, "Defumador", smoker));
        inventory.setItem(CAMPFIRE_SLOT, createToggleItem(Material.CAMPFIRE, "Fogueira", campfire));
    }

    private ItemStack createToggleItem(Material mat, String name, boolean active) {
        Component title = active
                ? Component.text("✔ " + name + " (Ativado)", NamedTextColor.GREEN, TextDecoration.BOLD)
                : Component.text("❌ " + name + " (Desativado)", NamedTextColor.RED);
        return createItem(mat, title);
    }

    private ItemStack createItem(Material mat, Component title) {
        return createItem(mat, title, null);
    }

    private ItemStack createItem(Material mat, Component title, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(title.decoration(TextDecoration.ITALIC, false));
            if (lore != null) meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getRecipeId() {
        return recipeId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SmeltingCreatorGUI gui)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 54) {
            if (slot == INPUT_SLOT || slot == RESULT_SLOT) return; // Permite alterar itens de entrada/saída

            event.setCancelled(true);

            // Ajustes de XP
            if (slot == XP_DEC_SLOT) {
                gui.experience = Math.max(0.0f, gui.experience - 0.1f);
                gui.updateDisplays();
            } else if (slot == XP_INC_SLOT) {
                gui.experience = Math.min(50.0f, gui.experience + 0.1f);
                gui.updateDisplays();
            }

            // Ajustes de Tempo
            else if (slot == TIME_DEC_SLOT) {
                gui.cookingTime = Math.max(20, gui.cookingTime - 20);
                gui.updateDisplays();
            } else if (slot == TIME_INC_SLOT) {
                gui.cookingTime = Math.min(1200, gui.cookingTime + 20);
                gui.updateDisplays();
            }

            // Toggles
            else if (slot == FURNACE_SLOT) { gui.furnace = !gui.furnace; gui.updateToggles(); }
            else if (slot == BLAST_SLOT) { gui.blastFurnace = !gui.blastFurnace; gui.updateToggles(); }
            else if (slot == SMOKER_SLOT) { gui.smoker = !gui.smoker; gui.updateToggles(); }
            else if (slot == CAMPFIRE_SLOT) { gui.campfire = !gui.campfire; gui.updateToggles(); }

            // Salvar
            else if (slot == SAVE_SLOT) {
                Player player = (Player) event.getWhoClicked();

                ItemStack inputStack = gui.getInventory().getItem(INPUT_SLOT);
                if (inputStack == null || inputStack.getType().isAir()) {
                    player.sendMessage(Component.text("❌ Coloque um item no slot de entrada (esquerda)!", NamedTextColor.RED));
                    return;
                }

                ItemStack resultStack = gui.getInventory().getItem(RESULT_SLOT);
                if (resultStack == null || resultStack.getType().isAir()) {
                    player.sendMessage(Component.text("❌ Coloque um item no slot de resultado (direita)!", NamedTextColor.RED));
                    return;
                }

                Item customInput = ItemHandler.getItemFromStack(inputStack);
                String inputDescriptor = (customInput != null) ? "CUSTOM:" + customInput.getId() : "VANILLA:" + inputStack.getType().name();

                SmeltingManager.saveRecipe(
                        gui.getRecipeId(), inputDescriptor, resultStack,
                        gui.furnace, gui.blastFurnace, gui.smoker, gui.campfire,
                        gui.cookingTime, gui.experience
                );

                player.sendMessage(Component.text("✔ Receita de queima '" + gui.getRecipeId() + "' criada com sucesso!", NamedTextColor.GREEN));
                player.closeInventory();
            }
        }
    }
}