package net.mineskyitems.utils;

import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.gui.rotatingshop.RotatingItemsGUI;
import net.mineskyitems.gui.rotatingshop.armors.RotatingArmorsGUI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RotatingShop {

    /*
    TODO: Precisa ser substituido por um metodo melhor futuramente
     */
    public static final int time = MineSkyItems.config.getInt("rotating-shop.time", 48);

    public static void initializeShop() {
        if(!MineSkyItems.MMOCORE_HOOK) {
            MineSkyItems.l.warning("To use the rotating shop, MMOCore must be enabled on this server.");
            return;
        }

        Set<String> classes = new HashSet<>();

        for (Item item : ItemHandler.getAllItems()) {
            classes.addAll(item.getRequiredClasses());
        }

        for(String classe : classes) {
            MineSkyItems.l.info("[SHOP] Carregando lojas para a classe "+classe);

            RotatingItemsGUI.createNewShop(classe);
            RotatingArmorsGUI.createNewShop(classe);
        }
    }

}
