package net.mineskyitems.hook;

import net.minesky.mineskygameplay.advancements.AdvancementsAPI;
import net.mineskyitems.entities.item.Item;
import org.bukkit.entity.Player;

public class AdvancementHook {

    public static void onCraft(final Player player, Item item) {
        if(player == null || item == null) return;

        final String id = item.getId();

        switch(id.toLowerCase()) {
            case "obsidiana_refinada" -> {
                AdvancementsAPI.get().grantAsync(player, "minesky:mineracao/forja_dura");
            }
            case "bloco_de_onix" -> {
                AdvancementsAPI.get().grantAsync(player, "minesky:mineracao/profundezas_ocultas");
            }
            case "meteorita" -> {
                AdvancementsAPI.get().grantAsync(player, "minesky:mineracao/pelos_cosmos");
            }
        }
    }

}
