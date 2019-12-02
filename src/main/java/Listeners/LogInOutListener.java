package Listeners;

import Main.TimeManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import Storage.Storage;

public class LogInOutListener implements Listener {
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        TimeManagement.getLoginHandler().timeIn(player);
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TimeManagement.getLoginHandler().timeOut(player);
        if (TimeManagement.getAFKHandler().getDataWrapper(player.getUniqueId()).isDoing()) {
            TimeManagement.getAFKHandler().timeOut(player);
        }
    }
}
