package Listeners;

import Main.TimeManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import Storage.Storage;

import java.util.UUID;

public class LogInOutListener implements Listener {
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        TimeManagement.getLoginHandler().timeIn(uuid);
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        TimeManagement.getLoginHandler().timeOut(uuid);
        if (TimeManagement.getAFKHandler().getDataWrapper(uuid) == null) {
            //todo: this ignores the case in which a player logs in goes afk, but the dataWrapper is unavailable because of a long delay,
            return;
        }
        if (TimeManagement.getAFKHandler().getDataWrapper(player.getUniqueId()).isDoing()) {
            TimeManagement.getAFKHandler().timeOut(uuid);
        }
    }
}
