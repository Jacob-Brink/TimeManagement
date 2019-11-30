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
    public void onLogin(AfkStatusChangeEvent e) {
        String playerName = e.getAffected().getName();
        Player player = Bukkit.getPlayer(playerName);
        Storage.changeAFKStatus(player.getUniqueId(), e.getValue());

    }
}
