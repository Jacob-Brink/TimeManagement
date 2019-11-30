package Storage;

import Main.TimeManagement;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.UUID;
import Storage.TimePlayer;
import Storage.LoginData;

public class Storage {

    private static HashMap<UUID, TimePlayer> uuidToTimePlayer = new HashMap<UUID, TimePlayer>();
    private static LoginDataAggregator loginDataAggregator;
    private static MySQLTable.ColumnWrapper uuidColumn, loginTime, millPlayed;
    private static MySQLTable mainTable;
    private static MySQLTable loginListTable;
    private static MySQLTable.ColumnWrapper totalPlayTimeColumn, firstLoginTimeColumn, totalPlayTimeAFKColumn, loginCountColumn;

    public static Connection getConnection() {
        try {
            return MySQLConnectionPool.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String fillInCommas(String sql, ArrayList<String> strings) {
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
        return sql;
    }

    public static void saveState(boolean saveOnDisable) {

        TimeManagement.sendInfo("SAVING");
        //1. insert login data to login table
        String sql = "INSERT INTO " + loginListTable.getName() + "(" + uuidColumn.getName() + "," + loginTime.getName() + "," + millPlayed.getName() + ") VALUES";

        ArrayList<String> strings = new ArrayList<String>();
        for (TimePlayer timePlayer : uuidToTimePlayer.values()) {
            TimeManagement.sendInfo("Player does exist");
            for (LoginData login : timePlayer.getNewLoginData()) {
                strings.add("(\"" + timePlayer.getUUID().toString() + "\"," + login.getStart() + "," + login.getMill() + ")");
            }

            if (timePlayer.isOnline()) {
                LoginData currentLoginData = timePlayer.getCurrentLoginData();
                strings.add("(\"" + timePlayer.getUUID().toString() + "\"," + currentLoginData.getStart() + "," + currentLoginData.getMill() + ")");
            }

            timePlayer.setNewToStored();
        }

        if (strings.size() == 0)
            return;

        sql = fillInCommas(sql, strings);

        TimeManagement.sendInfo("SQL: \"" + sql + "\"");

        if (!saveOnDisable) {
            runSQLInsertion(sql);
        } else {
            try {
                PreparedStatement statement = getConnection().prepareStatement(sql);
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                TimeManagement.sendError("Failed to send data to server.");
            }
        }

        //2. insert main data to main table
        String mainSQL = "INSERT INTO " + mainTable.getName() + "(" + uuidColumn.getName() + ", " + totalPlayTimeColumn.getName() + "," + totalPlayTimeAFKColumn.getName() + ", " + firstLoginTimeColumn.getName() + ", " + loginCountColumn.getName() + ") VALUES";
        ArrayList<String> stringValues = new ArrayList<>();
        for (TimePlayer timePlayer : uuidToTimePlayer.values()) {
            if (timePlayer.changed() || timePlayer.isOnline()) {
                String uuid = timePlayer.getUUID().toString();
                long totalPlayTime = timePlayer.getTotalPlayTime();
                long totalPlayTimeAFK = timePlayer.getTotalAFKTime();
                long firstLogInTime = timePlayer.getFirstLoginTime();
                long totalLoginCount = timePlayer.getLoginCount();

                stringValues.add("(\"" + uuid + "\", " + totalPlayTime + ", " + totalPlayTimeAFK + ", " + firstLogInTime + ", " + totalLoginCount + ")");
            }
        }

        mainSQL = fillInCommas(mainSQL, stringValues);
        TimeManagement.sendInfo("MAIN SQL INSERTION: " + mainSQL);
        if (!saveOnDisable) {
            runSQLInsertion(mainSQL);
        } else {
            try {
                PreparedStatement statement = getConnection().prepareStatement(mainSQL);
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                TimeManagement.sendError("Failed to send data to server.");
            }
        }

    }

    public static void runSQLInsertion(final String sql) {
        Bukkit.getScheduler().runTaskAsynchronously(TimeManagement.getPlugin(), new Runnable() {
            @Override
            public void run() {
                try {
                    TimeManagement.sendInfo("Running insertion asynchronously " + sql);
                    PreparedStatement statement = getConnection().prepareStatement(sql);
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    TimeManagement.sendError("Failed to send data to server...");
                }
            }
        });
    }

    public static TimePlayer getTimePlayer(UUID uuid) {
        return uuidToTimePlayer.get(uuid);
    }

    public static void changeAFKStatus(UUID uuid, boolean afk) {
        TimePlayer timePlayer = uuidToTimePlayer.get(uuid);
        timePlayer.setAFK(afk);
    }

    public static class LoginDataAggregator extends MySQLTable.Aggregator {
        private HashMap<UUID, TreeSet<LoginData>> timePlayersLoginData = new HashMap<UUID, TreeSet<LoginData>>();
        private UUID uuid = null;
        private TreeSet<LoginData> userLoginDataAccumulator = new TreeSet<LoginData>();

        protected void setup() {
            registerColumn(uuidColumn, new AggregatorCallbackFunction() {
                @Override
                public void call(ResultSet results) throws SQLException {
                    //store timePlayer if last timePlayer was not null
                    if (uuid != null) {
                        timePlayersLoginData.put(uuid, userLoginDataAccumulator);
                        userLoginDataAccumulator = new TreeSet<LoginData>();
                    }

                    if (results != null) {
                        //make new timePlayer on change of uuid column
                        String uuidString = results.getString(uuidColumn.getName());
                        uuid = UUID.fromString(uuidString);
                    }

                }
            });

            registerColumn(loginTime, new AggregatorCallbackFunction() {
                @Override
                public void call(ResultSet results) throws SQLException {
                    if (results != null) {
                        long login = results.getLong(loginTime.getName());
                        long playtime = results.getLong(millPlayed.getName());
                        userLoginDataAccumulator.add(new LoginData(login, playtime));
                    }
                }
            });
        }

        public TreeSet<LoginData> getLoginDataSet(final UUID uuid) {
            return timePlayersLoginData.get(uuid);
        }
    }

    public static class DataAggregator extends MySQLTable.Aggregator {

        @Override
        protected void setup() {
            registerColumn(uuidColumn, new AggregatorCallbackFunction() {
                @Override
                public void call(ResultSet results) throws SQLException {
                    if (results != null) {
                        String uuidString = results.getString(uuidColumn.getName());
                        UUID uuid = UUID.fromString(uuidString);
                        long totalPlayTime = results.getLong(totalPlayTimeColumn.getName());
                        long totalPlayTimeAFK = results.getLong(totalPlayTimeAFKColumn.getName());
                        long firstLoginTime = results.getLong(firstLoginTimeColumn.getName());
                        long loginCount = results.getLong(loginCountColumn.getName());

                        TimePlayer timePlayer = new TimePlayer(uuid, loginDataAggregator.getLoginDataSet(uuid), firstLoginTime, totalPlayTimeAFK, totalPlayTime, loginCount);
                        uuidToTimePlayer.put(uuid, timePlayer);

                    }
                };
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
        uuidColumn = new MySQLTable.ColumnWrapper("UUID", "VARCHAR(36) NOT NULL", "");
        loginTime = new MySQLTable.ColumnWrapper("LOGIN", "BIGINT NOT NULL", "");
        millPlayed = new MySQLTable.ColumnWrapper("MILL", "BIGINT NOT NULL", "");

        totalPlayTimeColumn = new MySQLTable.ColumnWrapper("TOTAL_PLAY_TIME", "BIGINT NOT NULL", "");
        totalPlayTimeAFKColumn = new MySQLTable.ColumnWrapper("TOTAL_NON_AFK_PLAY_TIME", "BIGINT NOT NULL", "");
        firstLoginTimeColumn = new MySQLTable.ColumnWrapper("FIRST_LOGIN_TIME", "BIGINT NOT NULL", "");
        loginCountColumn = new MySQLTable.ColumnWrapper("LOGIN_COUNT", "BIGINT NOT NULL", "");

        loginListTable = new MySQLTable("VERTX_TIME_LOGIN_LIST", uuidColumn, loginTime, millPlayed);

        if (loginListTable.create())
            TimeManagement.sendInfo("MySQL loginList table is setup!");
        else
           TimeManagement.sendError("MySQL table with name \"" + loginListTable.getName() + "\" is not setup correctly... Please stop server and attend to this issue");

        mainTable = new MySQLTable("VERTX_TIME_MAIN", uuidColumn, totalPlayTimeColumn, totalPlayTimeAFKColumn, firstLoginTimeColumn, loginCountColumn);

        if (mainTable.create())
            TimeManagement.sendInfo("MySQL table \"" + mainTable.getName() + "\" are setup!");
        else
            TimeManagement.sendError("MySQL table with name \"" + mainTable.getName() + "\" is not setup correctly... Please stop server and attend to this issue");

    }

    public static void loadState() {
        setupTables();

        TimeManagement.sendInfo(loginListTable.getName());
        LoginDataAggregator loginDataAggregator = new LoginDataAggregator();
        loginListTable.processData(loginDataAggregator, "SELECT * FROM " + loginListTable.getName() + " ORDER BY " + uuidColumn.getName() + " ASC");

        TimeManagement.sendInfo(mainTable.getName());
        DataAggregator mainDataAggregator = new DataAggregator();
        mainTable.processData(mainDataAggregator, "SELECT * FROM " + mainTable.getName());
    }

}

