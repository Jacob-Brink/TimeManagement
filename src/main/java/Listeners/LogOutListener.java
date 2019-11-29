package Listeners;

import Storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LogOutListener implements Listener {
    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Storage.loggedOut(player.getUniqueId());
    }
}