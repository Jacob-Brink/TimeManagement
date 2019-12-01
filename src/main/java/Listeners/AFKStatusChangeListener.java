package Listeners;

import Main.TimeManagement;
import Storage.Storage;
import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AFKStatusChangeListener implements Listener {
    @EventHandler
    public void onAFKChange(AfkStatusChangeEvent e) {
        String playerName = e.getAffected().getName();
        Player player = Bukkit.getPlayer(playerName);

        boolean nowAFK = e.getValue();
        if (nowAFK) {
            TimeManagement.getAFKHandler().timeIn(player);
        } else {
            TimeManagement.getAFKHandler().timeOut(player);
        }

    }
}
