package Main;

import Commands.TotalTimeCommand;
import Listeners.AFKStatusChangeListener;
import Listeners.LogInOutListener;
import Storage.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import TimeManagementAPI.TimeManagementAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TimeManagement extends JavaPlugin implements TimeManagementAPI {

    public final static String prefix = "" + ChatColor.GREEN + "VertX" + ChatColor.WHITE + " TimeSheet: " + ChatColor.GRAY;
    public final static String errorPrefix = "" + ChatColor.RED + "VertX" + ChatColor.WHITE + " TimeSheet(ERROR): " + ChatColor.GRAY;
    private static JavaPlugin plugin;
    private static ChangeHandler afkHandler, loginHandler, testHandler;
    public static boolean debug;

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static void sendError(String errorMessage) {
        Bukkit.getLogger().info(errorPrefix + errorMessage);
    }

    public static void sendInfo(String infoMessage) {
        Bukkit.getLogger().info(prefix + infoMessage);
    }

    public static void sendDebug(String infoMessage) {
        if (debug) {
            Bukkit.getLogger().info(prefix + infoMessage);
        };
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

    public static ChangeHandler getTestHandler() { return testHandler; }

    public static void setPlugin(JavaPlugin p) {
        plugin = p;
    }

    @Override
    public void onEnable() {
        plugin = this;

        createConfig();
        debug = getConfig().getBoolean("debugmode");
        afkHandler = new ChangeHandler("GO_AFK", "GO_NO_AFK", "AFK");
        loginHandler = new ChangeHandler("LOGIN", "LOGOUT", "LOGINS");
        testHandler = new ChangeHandler("TEST_IN", "TEST_OUT", "TEST_LOGIN");

        getServer().getPluginManager().registerEvents(new LogInOutListener(), this);
        getServer().getPluginManager().registerEvents(new AFKStatusChangeListener(), this);

        getCommand("totaltime").setExecutor(new TotalTimeCommand("totaltime", "time.total"));
        //getCommand("firstlogin").setExecutor(new FirstLoginCommand("firstlogin", "time.birth"));
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
        loginHandler.finish();
        afkHandler.finish();
        MySQLConnectionPool.close();

    }

    @Override
    public CompletableFuture<PublicDataContainer> getBasicInfo(UUID uuid) {
        return loginHandler.getBasicData(uuid);
    }

    @Override
    public CompletableFuture<ArrayList<LoginData>> getEachLogin(UUID uuid) {
        return loginHandler.getAllData(uuid);
    }

}
