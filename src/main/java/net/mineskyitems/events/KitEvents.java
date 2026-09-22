package net.mineskyitems.events;

import net.mineskyitems.entities.kits.KitHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class KitEvents implements Listener {
    public static String initialKit = "";

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();

        if(!p.hasPlayedBefore()) {
            KitHandler.Kit kit = KitHandler.getKit(initialKit);
            if(kit != null) {
                KitHandler.claimKit(p, kit);
            }
        }
    }
}