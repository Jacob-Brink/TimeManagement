package Commands;

import Main.Calculations;
import Main.TimeManagement;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.UUID;

public class TotalTimeCommand extends CommandAsset {

    public TotalTimeCommand(String commandName, String permission) {
        super(commandName, permission, AllowedUserType.PLAYER);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        long minutesInHour = 60;

        BigInteger totalMinutes = Calculations.getTotalPlayTimeMinutes(uuid);
        BigInteger totalHours = totalMinutes.divide(BigInteger.valueOf(minutesInHour));
        totalMinutes = totalMinutes.mod(BigInteger.valueOf(minutesInHour));

        player.sendMessage(ChatColor.BOLD + "Total Time: " + ChatColor.GRAY + "" + totalHours.toString() + "h " + totalMinutes.toString() + "m");
    }
}
