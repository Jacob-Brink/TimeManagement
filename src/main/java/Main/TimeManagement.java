package Main;

import Commands.FirstLoginCommand;
import Commands.TotalTimeCommand;
import Listeners.AFKStatusChangeListener;
import Listeners.LogInListener;
import Listeners.LogOutListener;
import Storage.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import TimeManagementAPI.TimeManagementAPI;

import java.io.File;
import java.math.BigInteger;
import java.util.Date;
import java.util.TreeSet;
import java.util.UUID;

public class TimeManagement extends JavaPlugin {//} implements TimeManagementAPI {

    private final static String prefix = "" + ChatColor.GREEN + "VertX" + ChatColor.WHITE + " TimeSheet: " + ChatColor.GRAY;
    private final static String errorPrefix = "" + ChatColor.RED + "VertX" + ChatColor.WHITE + " TimeSheet(ERROR): " + ChatColor.GRAY;
    private static JavaPlugin plugin;
    private int frequency;

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static void sendError(String errorMessage) {
        Bukkit.getLogger().info(errorPrefix + errorMessage);
    }

    public static void sendInfo(String infoMessage) {
        Bukkit.getLogger().info(prefix + infoMessage);
    }


    @Override
    public void onEnable() {
        plugin = this;

        createConfig();

        frequency = getConfig().getInt("frequency");

        final int ticks = 20*60*frequency;

        new BukkitRunnable() {

            boolean firstTime = true;

            public void run() {
                if (firstTime) {
                    firstTime = false;
                } else {
                    sendInfo("Saving state...");
                    Storage.saveState(false);
                }
            }

        }.runTaskTimer(this, 0l, ticks);


        Storage.loadState();

        for(Player player : Bukkit.getOnlinePlayers()) {
            Storage.getTimePlayer(player.getUniqueId()).loggedIn();
        }

        getServer().getPluginManager().registerEvents(new LogInListener(), this);
        getServer().getPluginManager().registerEvents(new LogOutListener(), this);
        getServer().getPluginManager().registerEvents(new AFKStatusChangeListener(), this);

        getCommand("totaltime").setExecutor(new TotalTimeCommand("totaltime", "time.total"));
        getCommand("firstlogin").setExecutor(new FirstLoginCommand("firstlogin", "time.birth"));


    }

    private void createConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                getLogger().info("config.yml not found, creating!");
                saveDefaultConfig();
            } else {
                getLogger().info("config.yml found, loading!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDisable() {
        Storage.saveState(true);
        MySQLConnectionPool.close();
    }
/*
    @Override
    public BigInteger getTotalPlayTimeMinutes(UUID uuid) {
        return Calculations.getTotalPlayTimeMinutes(uuid);
    }

    @Override
    public BigInteger getAverageMinutesPerGameSession(UUID uuid) {
        return Calculations.getAverageMinutesPerGameSession(uuid);
    }

    @Override
    public int getLoginNumbers(UUID uuid) {
        return Calculations.getLoginNumbers(uuid);
    }

    @Override
    public double getDaysSinceFirstLogin(UUID uuid) {
        return Calculations.getDaysSinceFirstLogin(uuid);
    }

    @Override
    public long getLongestSession(UUID uuid) {
        return Calculations.getLongestSession(uuid);
    }

    @Override
    public Date getFirstLoginDate(UUID uuid) {
        return Calculations.getFirstLoginDate(uuid);
    }

    @Override
    public TreeSet<LoginData> getData(UUID uuid) {
        TreeSet<LoginData> loginData = Storage.getTimePlayer(uuid).getAllLogins();
        return loginData;
    }*/
}
