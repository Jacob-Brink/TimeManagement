package Storage;

import Main.TimeManagement;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Storage {

    private static HashMap<UUID, TimePlayer> uuidToTimePlayer = new HashMap<UUID, TimePlayer>();
    private static MySQLTable.ColumnWrapper uuid, loginTime, millPlayed;
    private static MySQLTable mainTable;

    public static Connection getConnection() {
        try {
            return MySQLConnectionPool.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveState(boolean saveOnDisable) {

        String sql = "INSERT INTO " + mainTable.getName() + "(" + uuid.getName() + "," + loginTime.getName() + "," + millPlayed.getName() + ") VALUES";

        ArrayList<String> strings = new ArrayList<String>();
        for (TimePlayer timePlayer : uuidToTimePlayer.values()) {
            for (LoginData login : timePlayer.getNewLogins()) {
                TimeManagement.sendInfo("Storing new logins");
                strings.add("(\"" + timePlayer.getUUID().toString() + "\"," + login.getStart() + "," + login.getMill() + ")");
            }

            //add current session since onDisable is called before all players are forced to log out for whatever reason
            if (saveOnDisable && timePlayer.isActive()) {
                TimeManagement.sendInfo("Storing new logins on disable");
                TimeManagement.sendInfo(timePlayer.getCurrentSessionDuration() + " milliseconds of play");

                strings.add("(\"" + timePlayer.getUUID().toString() + "\"," + timePlayer.getCurrentSessionStart() + "," + timePlayer.getCurrentSessionDuration() + ")");
            }

            timePlayer.setNewToStored();
        }

        if (strings.size() == 0)
            return;

        boolean commas = true;
        int size = strings.size();
        int count = 0;
        for (String valueEntry : strings) {
            if (count == size-1) {
                commas = false;
            }
            sql += valueEntry + (commas ? "," : "");
            count++;
        }

        ResultSet results;

        TimeManagement.sendInfo("SQL: \"" + sql + "\"");

        final String finalSql = sql;
        final int entriesSaved = strings.size();

        if (!saveOnDisable) {
            Bukkit.getScheduler().runTaskAsynchronously(TimeManagement.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    try {
                        TimeManagement.sendInfo("Running insertion asynchronously");
                        PreparedStatement statement = getConnection().prepareStatement(finalSql);
                        statement.execute();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        TimeManagement.sendError("Failed to send data to server. " + entriesSaved + " entries were unrecorded.");
                    }
                }
            });
        } else {
            try {
                TimeManagement.sendInfo("Running insertion asynchronously");
                PreparedStatement statement = getConnection().prepareStatement(finalSql);
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                TimeManagement.sendError("Failed to send data to server. " + entriesSaved + " entries were unrecorded.");
            }
        }



    }

    public static TimePlayer getTimePlayer(UUID uuid) {
        return uuidToTimePlayer.get(uuid);
    }

    public static class DataAggregator extends MySQLTable.Aggregator {
        TimePlayer timePlayer = null;
        protected void setup() {
            registerColumn(uuid, new AggregatorCallbackFunction() {
                @Override
                public void call(ResultSet results) throws SQLException {
                    //store timePlayer if last timePlayer was not null
                    if (timePlayer != null) {
                        uuidToTimePlayer.put(timePlayer.getUUID(), timePlayer);
                    }

                    if (results != null) {
                        //make new timePlayer on change of uuid column
                        String uuidString = results.getString(uuid.getName());
                        UUID uuid = UUID.fromString(uuidString);
                        timePlayer = new TimePlayer(uuid);
                    }

                }
            });

            registerColumn(loginTime, new AggregatorCallbackFunction() {
                @Override
                public void call(ResultSet results) throws SQLException {
                    if (results != null) {
                        long login = results.getLong(loginTime.getName());
                        long playtime = results.getLong(millPlayed.getName());
                        timePlayer.addStoredLogin(new LoginData(login, playtime));
                    }
                }
            });
        }
    }

    public static void loggedIn(final UUID uuid) {
        if (!uuidToTimePlayer.containsKey(uuid)) {
            TimePlayer timePlayer = new TimePlayer(uuid);
            timePlayer.loggedIn();
            uuidToTimePlayer.put(uuid, timePlayer);
        } else {
            TimePlayer timePlayer = uuidToTimePlayer.get(uuid);
            timePlayer.loggedIn();
        }
    }

    public static void loggedOut(final UUID uuid) {
        TimePlayer timePlayer = uuidToTimePlayer.get(uuid);
        timePlayer.loggedOut();
    }


    private static void setupTables() {
        uuid = new MySQLTable.ColumnWrapper("UUID", "VARCHAR(36) NOT NULL", "");
        loginTime = new MySQLTable.ColumnWrapper("LOGIN", "BIGINT NOT NULL", "");
        millPlayed = new MySQLTable.ColumnWrapper("MILL", "BIGINT NOT NULL", "");

        mainTable = new MySQLTable("VERTX_TIME", uuid, loginTime, millPlayed);

        if (mainTable.create())
            TimeManagement.sendInfo("MySQL tables are setup!");
        else
           TimeManagement.sendError("MySQL tables not setup correctly... Please stop server and attend to this issue, since this plugin will not record data when not working");

    }

    public static void loadState() {
        setupTables();
        DataAggregator dataAggregator = new DataAggregator();
        mainTable.processData(dataAggregator, "SELECT * FROM " + mainTable.getName() + " ORDER BY " + uuid.getName() + " ASC");
    }
}
