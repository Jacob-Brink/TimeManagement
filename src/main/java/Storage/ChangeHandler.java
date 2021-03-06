package Storage;

import Main.TimeManagement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;

public class ChangeHandler {

    private MySQLTable timeTable;
    private MySQLTable.ColumnWrapper uuidColumn, inColumn, outColumn, autoIncrementColumn;

    private MySQLTable accumulatorTable;
    private MySQLTable.ColumnWrapper totalDoingColumn, startCountColumn, firstStartColumn, accUUIDColumn;

    private BlockingQueue<SequentialRunnable> blockingQueue;
    private ConcurrentHashMap<UUID, CompletableFuture<DataWrapper>> players = new ConcurrentHashMap<UUID, CompletableFuture<DataWrapper>>();
    private Thread thread;

    public ChangeHandler(String timeIn, String timeOut, String timeType) {
        this.blockingQueue = new LinkedBlockingQueue<SequentialRunnable>();
        setupTables(timeIn, timeOut, timeType);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(TimeManagement.getPlugin(), new Runnable() {
            @Override
            public void run() {
                updateActive(true);
            }
        }, 0L, 10 * 20);

        AsyncInsert asyncInsert = new AsyncInsert(this.blockingQueue);
        thread = new Thread(asyncInsert);
        thread.start();
    }

    public void finish() {
        this.blockingQueue.add(new SequentialRunnable() {
            @Override
            public boolean run() {
                return false;
            }
        });
        updateActive(false);

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void updateActive(boolean async) {
        final long time = currentTime();

        for (CompletableFuture<DataWrapper> dataFuture : players.values()) {
            //only update if completed future was created, otherwise do not wait
            if (dataFuture.isDone()) {
                try {

                    DataWrapper data = dataFuture.get();

                    //skip update command if data is not immediately available
                    if (data != null && data.isDoing()) {

                        final long newRunningTotal = data.getRunningTotalTime(time);

                        TimeManagement.sendInfo("UPDATING TIME TO " + newRunningTotal);

                        if (async) {
                            this.blockingQueue.add(new SequentialRunnable() {
                                @Override
                                public boolean run() {
                                    update(data.getUUID(), time, newRunningTotal);
                                    return true;
                                }
                            });
                        } else {
                            update(data.getUUID(), time, newRunningTotal);
                        }

                    }

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    TimeManagement.sendError("ERROR");
                }

            }

        }
    }

    public DataWrapper getDataWrapper(final UUID uuid) {
        CompletableFuture<DataWrapper> future = players.get(uuid);
        if (future != null) {
            try {
                return players.get(uuid).get(0, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException | NullPointerException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean setupTables(String timeIn, String timeOut, String timeType) {
        autoIncrementColumn = new MySQLTable.ColumnWrapper("ID", "INT NOT NULL AUTO_INCREMENT", "PRIMARY KEY");
        uuidColumn = new MySQLTable.ColumnWrapper("UUID", "VARCHAR(36) NOT NULL", "");
        inColumn = new MySQLTable.ColumnWrapper(timeIn, "BIGINT NOT NULL", "");
        outColumn = new MySQLTable.ColumnWrapper(timeOut, "BIGINT", "");
        timeTable = new MySQLTable("VERTX_" + timeType, autoIncrementColumn, uuidColumn, inColumn, outColumn);

        if (!createTable(timeTable))
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

    public void timeIn(final UUID playerUUID) {

        final String uuid = playerUUID.toString();
        final long timeStamp = currentTime();

        this.blockingQueue.add(new SequentialRunnable() {
            @Override
            public boolean run() {
                try (Connection connection = MySQLConnectionPool.getConnection()) {
                    TimeManagement.sendInfo("Inserting login time");
                    //1. insert new login on login sheet
                    PreparedStatement stmt = connection.prepareStatement("INSERT INTO " + timeTable.getName() + " (" + uuidColumn.getName() + ", " + inColumn.getName() + ") VALUES(?, ?)");
                    stmt.setString(1, uuid);
                    stmt.setLong(2, timeStamp);
                    stmt.execute();

                    TimeManagement.sendInfo("Updating accumulator table");
                    //2. either insert if new or update accumulator row for columns with start count and first start
                    PreparedStatement accStatement = connection.prepareStatement("INSERT INTO " + accumulatorTable.getName() +
                            "(" + accUUIDColumn.getName() + ", " + startCountColumn.getName() + ", " + firstStartColumn.getName() + ")" +
                            " VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE " + startCountColumn.getName() + "=" + startCountColumn.getName() +
                            "+1, " + firstStartColumn.getName() + " =" + firstStartColumn.getName() + "");

                    accStatement.setString(1, uuid);
                    accStatement.setLong(2, 1);
                    accStatement.setLong(3, timeStamp);

                    accStatement.execute();

                    CompletableFuture<DataWrapper> dataFuture = CompletableFuture.supplyAsync(() -> {
                        DataWrapper dataWrapper = null;
                        try (Connection connectionInside = MySQLConnectionPool.getConnection()) {
                            TimeManagement.sendDebug("Loading player from database");
                            PreparedStatement statement = connectionInside.prepareStatement("SELECT * FROM " + accumulatorTable.getName() + " WHERE " + accUUIDColumn.getName() + "=?");
                            statement.setString(1, uuid);
                            ResultSet results = statement.executeQuery();

                            long total, startCount, firstStart;
                            if (results.next()) {
                                startCount = results.getLong(startCountColumn.getName());
                                firstStart = results.getLong(firstStartColumn.getName());
                                try {
                                    total = results.getLong(totalDoingColumn.getName());
                                } catch (SQLException e) {
                                    total = 0;
                                }
                                // = new DataWrapper(playerUUID, firstStart, total, startCount, timeStamp);
                                dataWrapper = new DataWrapper(playerUUID, firstStart, total, startCount, timeStamp);

                            } else {
                                dataWrapper = new DataWrapper(playerUUID, timeStamp, 0, 1, timeStamp);
                            }
                            dataWrapper.setDoing(true);

                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        return dataWrapper;
                    });

                    players.put(playerUUID, dataFuture);

                } catch (SQLException e) {
                    e.printStackTrace();
                    TimeManagement.sendError("could not load or store player data. \n SERIOUS ERROR! SOME PLAYER DATA MAY NOT BE COLLECTED. PLEASE STOP SERVER AND FIX ISSUE");
                }
                return true;
            }
        });

        if (!players.contains(playerUUID)) {
            CompletableFuture<DataWrapper> dataFuture = CompletableFuture.supplyAsync(() -> {
                DataWrapper dataWrapper;
                try (Connection connection = MySQLConnectionPool.getConnection()) {
                    TimeManagement.sendInfo("Loading player from database");
                    PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + accumulatorTable.getName() + " WHERE " + accUUIDColumn.getName() + "=?");
                    stmt.setString(1, uuid);
                    ResultSet results = stmt.executeQuery();

                    long total, startCount, firstStart;
                    if (results.next()) {
                        startCount = results.getLong(startCountColumn.getName());
                        firstStart = results.getLong(firstStartColumn.getName());
                        try {
                            total = results.getLong(totalDoingColumn.getName());
                        } catch (SQLException e) {
                            total = 0;
                        }
                        // = new DataWrapper(playerUUID, firstStart, total, startCount, timeStamp);
                        dataWrapper = new DataWrapper(playerUUID, firstStart, total, startCount, timeStamp);

                    } else {
                        dataWrapper = new DataWrapper(playerUUID, timeStamp, 0, 1, timeStamp);
                    }
                    dataWrapper.setDoing(true);
                    return dataWrapper;

                } catch (SQLException e) {
                    e.printStackTrace();
                    TimeManagement.sendError("could not load player. SERIOUS ERROR! SOME PLAYER DATA MAY NOT BE COLLECTED. PLEASE STOP SERVER AND FIX ISSUE");
                }

                return null;

            });
            players.put(playerUUID, dataFuture);
        }


    }

    private void update(final UUID uuid, final long time, final long newTime) {

        final String uuidString = uuid.toString();

        try (Connection connection = MySQLConnectionPool.getConnection()) {

            //1. update total time in accumulator
            PreparedStatement acc = connection.prepareStatement("UPDATE " + accumulatorTable.getName() + " SET " + totalDoingColumn.getName() + "=? WHERE " + uuidColumn.getName() + "=?");
            acc.setLong(1, newTime);
            acc.setString(2, uuidString);
            acc.execute();

            //2. update last login time stamp
            PreparedStatement updateSQL = connection.prepareStatement("UPDATE " + timeTable.getName() + " SET " + outColumn.getName() + "=? " +
                    "WHERE " + uuidColumn.getName() + "=? ORDER BY " + autoIncrementColumn.getName() + " DESC LIMIT 1");
            updateSQL.setLong(1, time);
            updateSQL.setString(2, uuidString);
            updateSQL.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void timeOut(final UUID uuid) {

        final long time = currentTime();
        //todo: figure out what the heck is going on lol `/
        //todo: wait for players to get populated with completed future `/
        CompletableFuture<DataWrapper> data = players.get(uuid);

        //1. when dataWrap is loaded, set doing to false
        // the reason this is not in the blocking queue, is that this should be done as soon as possible
        // and the blocking queue may take time to process this timeout command
        data.thenCompose((dataWrap) ->
                CompletableFuture.supplyAsync(() -> {
                    if (dataWrap != null)
                        TimeManagement.sendDebug("Setting " + uuid + " \"doing\" to false");
                    dataWrap.setDoing(false);
                    return null;
                })
        );

        //2. update player
        this.blockingQueue.add(new SequentialRunnable() {
            @Override
            public boolean run() {
                try {
                    DataWrapper dataWrapper = data.get();
                    if (dataWrapper != null) {
                        //todo: this assumes that this happen after timeIn retreives data and stores it in the datawrapper
                        //todo: make this work in the case that it doesn't happen like that `/
                        //actually it is fine, since this is put in a queue, and we assume people log in before logging out, this will always happen afterwards
                        //and we also assume that on every login, a datawrapper is made in a completable future
                        //even if the completable future is not finished we wait for it in the queue
                        TimeManagement.sendDebug("updating player with uuid " + uuid);
                        update(uuid, time, dataWrapper.getRunningTotalTime(time));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

    }

    public CompletableFuture<ArrayList<LoginData>> getAllData(final UUID uuid) {
        return new CompletableFuture<>().supplyAsync(() -> {
            ArrayList<LoginData> logins = new ArrayList<LoginData>();
            try (Connection connection = MySQLConnectionPool.getConnection()) {
                String sql = "SELECT * FROM " + timeTable.getName() + " WHERE UUID=? ORDER BY " + accumulatorTable.getName() + " ASC ";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, uuid.toString());
                long lostCount = 0;
                ResultSet results = statement.executeQuery();
                while (results.next()) {
                    try {
                        long start = results.getLong(inColumn.getName());
                        long end = results.getLong(outColumn.getName());
                        LoginData login = new LoginData(start, end);
                        logins.add(login);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        lostCount++;
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return logins;
        });
    }

    public CompletableFuture<PublicDataContainer> getBasicData(final UUID uuid) {
        DataWrapper data = getDataWrapper(uuid);
        if (data != null) {
            long totalTime = data.getTotalTime();
            if (data.isDoing())
                totalTime = data.getRunningTotalTime(System.currentTimeMillis());
            return CompletableFuture.completedFuture(new PublicDataContainer(data.getFirstStart(), totalTime, data.getStartCount()));
        } else {
            return CompletableFuture.supplyAsync(() -> {
                try (Connection connection = MySQLConnectionPool.getConnection()) {
                    String sql = "SELECT * FROM " + accumulatorTable.getName() + " WHERE UUID=?";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, uuid.toString());
                    ResultSet results = statement.executeQuery();
                    if (results.next()) {
                        return new PublicDataContainer(results.getLong(firstStartColumn.getName()), results.getLong(totalDoingColumn.getName()), results.getLong(startCountColumn.getName()));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            });
        }
    }


}
