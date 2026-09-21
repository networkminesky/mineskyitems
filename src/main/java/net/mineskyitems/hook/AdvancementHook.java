package net.mineskyitems.hook;

import net.minesky.gameplay.api.MineSkyAPI;
import net.minesky.gameplay.api.advancements.AdvancementsAPI;
import net.mineskyitems.entities.item.Item;
import org.bukkit.entity.Player;

public class AdvancementHook {

    public static void onCraft(final Player player, Item item) {
        if(player == null || item == null) return;

        final String id = item.getId();

        AdvancementsAPI api = MineSkyAPI.getAdvancements();
        if(api == null)
            return;

        switch(id.toLowerCase()) {
            case "obsidiana_refinada" -> {
                api.grantAsync(player, "minesky:mineracao/forja_dura");
            }
            case "bloco_de_onix" -> {
                api.grantAsync(player, "minesky:mineracao/profundezas_ocultas");
            }
            case "meteorita" -> {
                api.grantAsync(player, "minesky:mineracao/pelos_cosmos");
            }
        }
    }

}
