package net.minesky.handler;

import net.minesky.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemBuilder {

    private Material material = Material.IRON_AXE;
    private String displayName;

    private int customModel = 0;
    private List<String> playerClass;

    private int itemLevel = -1;

    public ItemBuilder() {



    }

    public String generateId() {
        return displayName.toLowerCase().replace("/\s+/g", "_").replace("/[^w-]+/g", "");
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public int getItemLevel() {return itemLevel;}
    public int getCustomModel() {return customModel;}
    public Material getMaterial() {return material;}
    public List<String> getPlayerClass() {return playerClass;}
    public String getDisplayName() {return displayName;}

    public void setItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
    }

    public void setCustomModel(int customModel) {
        this.customModel = customModel;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPlayerClass(List<String> playerClass) {
        this.playerClass = playerClass;
    }

    public ItemStack getItemStack() {

        ItemStack it = new ItemStack(material);
        ItemMeta im = it.getItemMeta();

        im.setCustomModelData(getCustomModel());
        im.setDisplayName(Utils.c(getDisplayName()));

        it.setItemMeta(im);
        return it;

    }
}
