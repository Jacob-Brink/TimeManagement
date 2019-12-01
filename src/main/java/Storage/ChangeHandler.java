package Storage;

import Main.TimeManagement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ChangeHandler {

    private MySQLTable timeTable;
    private MySQLTable.ColumnWrapper uuidColumn, inColumn, outColumn, autoIncrementColumn;

    private MySQLTable accumulatorTable;
    private MySQLTable.ColumnWrapper totalDoingColumn, startCountColumn, firstStartColumn, accUUIDColumn;

    private BlockingQueue<SequentialRunnable> blockingQueue;
    private ConcurrentHashMap<UUID, DataWrapper> players = new ConcurrentHashMap<UUID, DataWrapper>();

    public ChangeHandler(String timeIn, String timeOut, String timeType) {
        this.blockingQueue = new LinkedBlockingQueue<SequentialRunnable>();
        setupTables(timeIn, timeOut, timeType);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(TimeManagement.getPlugin(), new Runnable() {
            @Override
            public void run() {
                    for (DataWrapper data : players.values()){
                        if (data.isDoing()) {
                            update(data.getUUID());
                        }
                    }
            }
        }, 0L, 30*20);

        AsyncInsert asyncInsert = new AsyncInsert(this.blockingQueue);
        Thread thread = new Thread(asyncInsert);
        thread.start();
    }

    public DataWrapper getDataWrapper(final UUID uuid) {
        return players.get(uuid);
    }

    private boolean setupTables(String timeIn, String timeOut, String timeType) {
        autoIncrementColumn = new MySQLTable.ColumnWrapper("ID", "INT NOT NULL AUTO_INCREMENT", "PRIMARY KEY");
        uuidColumn = new MySQLTable.ColumnWrapper("UUID", "VARCHAR(36) NOT NULL", "");
        inColumn = new MySQLTable.ColumnWrapper(timeIn, "BIGINT NOT NULL", "");
        outColumn = new MySQLTable.ColumnWrapper(timeOut, "BIGINT", "");
        timeTable = new MySQLTable("VERTX_" + timeType, autoIncrementColumn, uuidColumn, inColumn, outColumn);

        if(!createTable(timeTable))
            return false;

        accUUIDColumn = new MySQLTable.ColumnWrapper("UUID", "VARCHAR(46) NOT NULL", "PRIMARY KEY");
        totalDoingColumn = new MySQLTable.ColumnWrapper("TOTAL", "BIGINT", "");
        startCountColumn = new MySQLTable.ColumnWrapper("START_NUM", "BIGINT NOT NULL", "");
        firstStartColumn = new MySQLTable.ColumnWrapper("FIRST_START", "BIGINT NOT NULL", "");
        accumulatorTable = new MySQLTable("VERTX_" + timeType + "_QUICK_ACCESS", accUUIDColumn, totalDoingColumn, startCountColumn, firstStartColumn);

        if (!createTable(accumulatorTable))
            return false;

        return true;
    }

    private boolean createTable(MySQLTable mySQLTable) {
        if (mySQLTable.create()) {
            return true;
        } else {
            TimeManagement.sendError("Could not create table " + timeTable.getName());
            TimeManagement.disable();
            return false;
        }
    }

    public static long currentTime() {
        return System.currentTimeMillis();
    }

    public void timeIn(Player player) {
        TimeManagement.sendError(timeTable.getName());

        final String uuid = player.getUniqueId().toString();
        final UUID playerUUID = player.getUniqueId();
        final long timeStamp = currentTime();

        this.blockingQueue.add(new SequentialRunnable() {
            @Override
            public boolean run() {
                try (Connection connection = MySQLConnectionPool.getConnection()) {
                    PreparedStatement stmt = connection.prepareStatement("INSERT INTO " + timeTable.getName() + " (" + uuidColumn.getName() + ", " + inColumn.getName() +") VALUES(?, ?)");
                    stmt.setString(1, uuid);
                    stmt.setLong(2, timeStamp);
                    stmt.execute();

                    PreparedStatement accStatement = connection.prepareStatement("INSERT INTO " + accumulatorTable.getName() +
                            "(" + accUUIDColumn.getName() + ", " + startCountColumn.getName() + ", " + firstStartColumn.getName() + ")" +
                            " VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE " + startCountColumn.getName() + "=" + startCountColumn.getName() +
                            "+1, " + firstStartColumn.getName() + " =" + firstStartColumn.getName()+"");

                    accStatement.setString(1, uuid);
                    accStatement.setLong(2, 1);
                    accStatement.setLong(3, timeStamp);

                    accStatement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    //todo
                }
                return true;
            }
        });

        DataWrapper data = players.get(player.getUniqueId());
        if (data != null) {
            data.setDoing(true);
        }

        Bukkit.getScheduler().runTaskAsynchronously(TimeManagement.getPlugin(), () -> {
            try (Connection connection = MySQLConnectionPool.getConnection()) {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + accumulatorTable.getName() + " WHERE " + accUUIDColumn.getName() + "=?");
                stmt.setString(1, uuid);
                ResultSet results = stmt.executeQuery();

                long total, startCount, firstStart;
                if (results.next()) {
                    startCount = results.getLong(startCountColumn.getName());
                    firstStart = results.getLong(firstStartColumn.getName());
                    total = 0;
                    try {
                        total = results.getLong(totalDoingColumn.getName());
                    } catch (SQLException e) {
                        //todo: new player handling
                    }
                    DataWrapper dataWrapper = new DataWrapper(playerUUID, firstStart, total, startCount, timeStamp);
                    dataWrapper.setDoing(true);
                    players.put(playerUUID, dataWrapper);

                } else {
                    //todo: handle new player
                }

            } catch (SQLException e) {
                e.printStackTrace();
                //todo
            }
        });

    }

    public void update(UUID uuid) {
        final String uuidString = uuid.toString();
        final long time = currentTime();
        DataWrapper data = getDataWrapper(uuid);
        //assuming total time is total time before last start
        final long newTime = data.getRunningTotalTime();

        Bukkit.getScheduler().runTaskAsynchronously(TimeManagement.getPlugin(), () -> {
            try (Connection connection = MySQLConnectionPool.getConnection()) {

                //1. update total time in accumulator
                PreparedStatement acc = connection.prepareStatement("UPDATE " + accumulatorTable.getName() + " SET " + totalDoingColumn.getName() + "=? WHERE " + uuidColumn.getName() + "=?");
                acc.setLong(1, newTime); //todo: figure out how to get running total
                acc.setString(2, uuidString);
                acc.execute();

                TimeManagement.sendInfo("YEP IT WORKS :(");

            } catch (SQLException e) {
                e.printStackTrace();
                //todo
            }

        });
    }

    public void timeOut(Player player) {
        final String uuidString = player.getUniqueId().toString();
        final long time = currentTime();

        update(player.getUniqueId());

        blockingQueue.add(new SequentialRunnable() {
            @Override
            public boolean run() {
                try (Connection connection = MySQLConnectionPool.getConnection()) {
                    //1. update last login time stamp
                    PreparedStatement update = connection.prepareStatement("UPDATE " + timeTable.getName() + " SET " + outColumn.getName() + "=? " +
                            "WHERE " + uuidColumn.getName() + "=? ORDER BY " + autoIncrementColumn.getName() + " DESC LIMIT 1");
                    update.setLong(1, time);
                    update.setString(2, uuidString);
                    update.execute();

                } catch (SQLException e) {
                    e.printStackTrace();
                    //todo
                }
                return true;
            }
        });
        DataWrapper data = players.get(player.getUniqueId());
        if (data != null) {
            data.setDoing(false);
        }
    }

}
