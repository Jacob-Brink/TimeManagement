package Storage;

import Main.TimeManagement;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLConnectionPool {

    private static String host, username, database, password;
    private static int port;

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    static {
        try {
            FileConfiguration config = TimeManagement.getPlugin().getConfig();
            host = config.getString("host");
            port = config.getInt("port");
            database = config.getString("database");
            username = config.getString("username");
            password = config.getString("password");
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().info("" + ChatColor.RED + "Config does not contain mysql configuration");
        }

        if (host == null) {
            Bukkit.getLogger().info("host is null");
        }
        if (database == null) {
            Bukkit.getLogger().info("database is null");
        }
        if (username == null) {
            Bukkit.getLogger().info("username is null");
        }
        if (password == null) {
            Bukkit.getLogger().info("database");
        }

        TimeManagement.sendInfo("Connecting to HOST: " + host + " on PORT: " + port + " FOR DATABASE " + database);
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.jdbc.Driver");

        config.addDataSourceProperty("autoReconnect", true);
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);

        //config.setMaximumPoolSize(20);
        config.setConnectionTimeout(3000);


        ds = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static void close() {
        try {
            ds.close();
        } catch (Exception e) {
            TimeManagement.sendError("A SQLException was caught" + e);
        }
    }
}