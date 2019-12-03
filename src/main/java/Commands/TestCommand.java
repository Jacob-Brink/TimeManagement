package Commands;

import Main.TimeManagement;
import Storage.ChangeHandler;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class TestCommand extends CommandAsset {

    public TestCommand(String commandName, String permission) {
        super(commandName, permission, AllowedUserType.ANY);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ChangeHandler handler = TimeManagement.getTestHandler();
        sender.sendMessage(TimeManagement.prefix + "Starting Test");
        sender.sendMessage(TimeManagement.prefix + "Testing Start Event");

        UUID uuidOne = new UUID(3, 4);
        UUID uuidTwo = new UUID(3, 4);

        sender.sendMessage(TimeManagement.prefix + "Testing ordered concurrent events.");
        sender.sendMessage(TimeManagement.prefix + "UUID " + uuidOne.toString() + " logs in before " + uuidTwo.toString());

        handler.timeIn(uuidOne);
        handler.timeIn(uuidTwo);

        sender.sendMessage(TimeManagement.prefix + "UUID " + uuidTwo.toString() + " logs out before " + uuidOne.toString());
        //todo: add delay
        handler.timeOut(uuidTwo);
        handler.timeOut(uuidOne);

        sender.sendMessage(TimeManagement.prefix + "Testing /totaltime command when player data is never received");




    }
}
