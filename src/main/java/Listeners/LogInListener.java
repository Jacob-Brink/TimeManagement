package Listeners;

import Main.TimeManagement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import Storage.Storage;

public class LogInListener implements Listener {
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        Storage.loggedIn(player.getUniqueId());
    }
}
