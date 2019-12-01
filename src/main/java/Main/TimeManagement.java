package Main;

import Commands.FirstLoginCommand;
import Commands.TotalTimeCommand;
import Listeners.AFKStatusChangeListener;
import Listeners.LogInOutListener;
import Storage.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

public class TimeManagement extends JavaPlugin {//} implements TimeManagementAPI {

    private final static String prefix = "" + ChatColor.GREEN + "VertX" + ChatColor.WHITE + " TimeSheet: " + ChatColor.GRAY;
    private final static String errorPrefix = "" + ChatColor.RED + "VertX" + ChatColor.WHITE + " TimeSheet(ERROR): " + ChatColor.GRAY;
    private static JavaPlugin plugin;
    private static ChangeHandler afkHandler, loginHandler;

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static void sendError(String errorMessage) {
        Bukkit.getLogger().info(errorPrefix + errorMessage);
    }

    public static void sendInfo(String infoMessage) {
        Bukkit.getLogger().info(prefix + infoMessage);
    }

    public static void disable() {
        TimeManagement.sendError("Ran into an error that will break plugin. Disabling...");
        Bukkit.getPluginManager().disablePlugin(getPlugin());
    }

    public static ChangeHandler getAFKHandler() {
        return afkHandler;
    }

    public static ChangeHandler getLoginHandler() {
        return loginHandler;
    }

    @Override
    public void onEnable() {
        plugin = this;

        createConfig();
        afkHandler = new ChangeHandler("GO_AFK", "GO_NO_AFK", "AFK");
        loginHandler = new ChangeHandler("LOGIN", "LOGOUT", "LOGINS");


        getServer().getPluginManager().registerEvents(new LogInOutListener(), this);
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
        //Storage.saveState(true);
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
