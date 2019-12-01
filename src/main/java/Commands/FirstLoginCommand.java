package Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FirstLoginCommand extends CommandAsset {

    public FirstLoginCommand(String commandName, String permission) {
        super(commandName, permission, AllowedUserType.PLAYER);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        /*SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        player.sendMessage(ChatColor.BOLD + "First Login Date: " + ChatColor.GRAY + sdf.format(Calculations.getFirstLoginDate(player.getUniqueId())));*/
    }

}
