package Commands;

import Main.TimeManagement;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TotalTimeCommand extends CommandAsset {

    public TotalTimeCommand(String commandName, String permission) {
        super(commandName, permission, AllowedUserType.PLAYER);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        long currentTime = System.currentTimeMillis();
        long milliseconds = TimeManagement.getLoginHandler().getDataWrapper(uuid).getRunningTotalTime(currentTime);
        long seconds = (milliseconds / 1000) % 60;
        long minutes = (milliseconds / 60000);
        player.sendMessage(ChatColor.BOLD + "Total Time: " + ChatColor.GRAY + "" + minutes + "m " + seconds + "s ");//todo
    }
}
