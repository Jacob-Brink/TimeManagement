package Listeners;

import Main.TimeManagement;
import Storage.Storage;
import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class AFKStatusChangeListener implements Listener {
    @EventHandler
    public void onAFKChange(AfkStatusChangeEvent e) {
        String playerName = e.getAffected().getName();
        Player player = Bukkit.getPlayer(playerName);

        final UUID uuid = player.getUniqueId();

        boolean nowAFK = e.getValue();
        if (nowAFK) {
            TimeManagement.getAFKHandler().timeIn(uuid);
        } else {
            TimeManagement.getAFKHandler().timeOut(uuid);
        }

    }
}
