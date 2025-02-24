package net.minesky.config;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ItemConfig {
    private String name;
    private int model;
    private Material material;
    private String classe;
    private int level;
    private List<String> lore;

    public ItemConfig() {
        this.name = "DisplayName";
        this.model = 0;
        this.material = Material.IRON_AXE;
        this.level = 1;
        this.classe = "Nenhuma";
        this.lore = new ArrayList<>();
    }

    public String getName() {
        return name;
    }
    public int getModel() {
        return model;
    }
    public String getMaterialName() {
        return this.material.toString();
    }
    public Material getMaterial() {
        return this.material;
    }
    public int getLevel() {
        return level;
    }
    public String getClasse() {
        return classe;
    }
    public List<String> getLore() {
        return this.lore;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setModel(int model) {
        this.model = model;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setLevel(int level) {
        this.level = level;
    }
    public void setClasse(String classe) {
        this.classe = classe;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }
}
