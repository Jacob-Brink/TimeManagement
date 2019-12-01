package Commands;

import Main.TimeManagement;
import Storage.DataWrapper;
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
        DataWrapper data = TimeManagement.getLoginHandler().getDataWrapper(uuid);
        if (data == null) {
            player.sendMessage("Your data is still being loaded at this time. Please wait and try again");
            return;
        }
        long milliseconds = data.getRunningTotalTime(currentTime);
        long seconds = (milliseconds / 1000) % 60;
        long minutes = (milliseconds / 60000);
        player.sendMessage(ChatColor.BOLD + "Total Time: " + ChatColor.GRAY + "" + minutes + "m " + seconds + "s ");//todo
    }
}
