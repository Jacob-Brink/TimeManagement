package Storage;

import Main.TimeManagement;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;

public class MySQLTable {

    private HashMap<String, ColumnWrapper> columns = new HashMap();

    public final ColumnWrapper getColumnByName(String name) {
        return columns.get(name);
    }

    public static class ColumnWrapper {
        private String name, creationString, type;
        private boolean canBeNull;
        private boolean isPrimaryKey;

        public ColumnWrapper(String name, String type, String extraParams) {
            this.name = name;
            this.canBeNull = true;
            this.creationString = name + " " + type + " " + extraParams;

            if (type.contains("VARCHAR")) {
                this.type = "string";
            } else if (type.contains("BIGINT")) {
                this.type = "bigint";
            } else if (type.contains("INT")) {
                this.type = "int";
            } else if (type.contains("BOOLEAN")) {
                this.type = "boolean";
            }

            if (extraParams.contains("NOT NULL"))
                this.canBeNull = false;

            this.isPrimaryKey = false;
            if (extraParams.contains("PRIMARY KEY"))
                this.isPrimaryKey = true;

        }

        public String getCurrentValue(final ResultSet resultSet) {
            try {
                switch(this.type) {
                    case "int":
                        return resultSet.getInt(getName()) + "";
                    case "bigint":
                        return resultSet.getLong(getName()) + "";
                    case "string":
                        return resultSet.getString(getName()) + "";
                    case "boolean":
                        return resultSet.getBoolean(getName()) + "";
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String getName() {
            return this.name;
        }

        public boolean isCanBeNull() {
            return this.canBeNull;
        }

        public boolean isPrimaryKey() {
            return this.isPrimaryKey;
        }

        public String getSetupString() {
            return this.creationString;
        }
    }
    private String tableName;

    /* isSetCorrectly
     * @precondition: assumes table exists
     * @params: none
     * @returns: true if table has columns with same names as those inputted
     */
    private boolean isSetCorrectly() {
        List<ColumnWrapper> list = new ArrayList<ColumnWrapper>(columns.values());
        Connection connection = null;
        try {
            connection = MySQLConnectionPool.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        for (ColumnWrapper column : list) {
            Bukkit.getLogger().info(" - " + column.getName());
            try {
                DatabaseMetaData md = connection.getMetaData();
                ResultSet rs = md.getColumns(null, null, getName(), column.getName());
                if (rs.next())
                    continue;
            } catch (SQLException e) {
                e.printStackTrace();
                Bukkit.getLogger().info("MySQLTable->isSetCorrectly: Something went wrong while checking columns.");
                return false;
            }
            Bukkit.getLogger().info("   --> No column found. Please either remove this table entirely or add this column to the table.");
            return false;
        }
        return true;
    }

    /* exists
     * @precondition: name is given
     * @params: none
     * @returns: true only if table exists
     */
    private boolean exists() {
        ResultSet tables = null;
        try {
            tables = MySQLConnectionPool.getConnection().getMetaData().getTables(null, null, getName(), null);
            return tables.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /* create
     * @precondition: none
     * @params: none
     * @returns: none
     * @desc: creates table if not created; if created, checks if setup right
     */
    public boolean create() {
        Bukkit.getLogger().info("Creating Table " + getName());
        if (exists()) {
            if (!isSetCorrectly()) {
                Bukkit.getLogger().info("Table with same name exists, but is not setup correctly. Please remove this table or change the name of the given table in the code. \nResolving by ignoring data");
                return false;
            }
            return true;
        }

        List<ColumnWrapper> list = new ArrayList<ColumnWrapper>(columns.values());
        int commas = columns.size()-1;
        //create table creation statement
        String sql = "";
        for (ColumnWrapper column : list) {
            String columnCreator = column.getSetupString();

            if (commas >= 1)
                sql += columnCreator + ",";
            else
                sql += columnCreator;

            commas--;
        }

        Bukkit.getLogger().info("Creating table: \"CREATE TABLE " + getName() + "(" + sql + ")\"");

        Statement statement = null;
        try {
            statement = MySQLConnectionPool.getConnection().createStatement();
            statement.executeUpdate("CREATE TABLE " + getName() + "(" + sql + ")");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public String getName() {
        return tableName;
    }

    public abstract static class Aggregator {

        public static class ValuePair {
            private String lastValue;
            private AggregatorCallbackFunction callback;

            public String getLastValue() {
                return lastValue;
            }

            public void setLastValue(String lastValue) {
                this.lastValue = lastValue;
            }

            public AggregatorCallbackFunction getCallback() {
                return callback;
            }

            public ValuePair(AggregatorCallbackFunction callback) {
                this.callback = callback;
                this.lastValue = null;
            }
        }

        protected LinkedHashMap<ColumnWrapper, ValuePair> columnsToLastValues = new LinkedHashMap();

        /* setup
         * desc: sets up columnToLastIDs
         */
        protected abstract void setup();

        public void registerColumn(ColumnWrapper columnWrapper, AggregatorCallbackFunction callback) {
            columnsToLastValues.put(columnWrapper, new ValuePair(callback));
        }

        /* isNewGroup
         * assumes: linkedHashMap was created in order from biggest groupings to smallest
         * returns: string of column that represents the "most encapsulating group" (i.e. the group containing sub groups) that has changed
         * returns: null if no change (but shouldn't happen if mysql table is setup right
         */
        public ValuePair isNewGroup(final ResultSet resultSet) {

            for (Map.Entry<ColumnWrapper, ValuePair> entry : this.columnsToLastValues.entrySet()) {
                ColumnWrapper column = entry.getKey();
                ValuePair valuePair = entry.getValue();
                String lastValue = valuePair.getLastValue();

                if (column.getCurrentValue(resultSet) != lastValue) {
                    return valuePair;
                }

            }
            return null;
        }

        /* updateLastValues
         * desc: updates group ids to current ids
         */
        public void updateLastValues(final ResultSet resultSet) {
            for (Map.Entry<ColumnWrapper, ValuePair> entry : this.columnsToLastValues.entrySet()) {
                ColumnWrapper column = entry.getKey();
                ValuePair valuePair = entry.getValue();
                valuePair.setLastValue(column.getCurrentValue(resultSet));
            }
        }

        private void processRow(final ResultSet results) {
            //handle end of rows
            if (results == null) {
                List<ValuePair> valuePairs = new ArrayList<ValuePair>(this.columnsToLastValues.values());
                try {
                    valuePairs.get(0).getCallback().call(null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return;
            }

            boolean processingOn = false;
            for (Map.Entry<ColumnWrapper, ValuePair> entry : this.columnsToLastValues.entrySet()) {
                ColumnWrapper column = entry.getKey();
                Aggregator.ValuePair valuePair = entry.getValue();

                try {
                    if (processingOn == false) {
                        if (!((column.getCurrentValue(results)).equalsIgnoreCase(valuePair.getLastValue()))) {
                            processingOn = true;
                            if (valuePair.getCallback() != null) {
                                valuePair.getCallback().call(results);
                            }
                        }
                    } else {
                        valuePair.getCallback().call(results);
                    }

                } catch (SQLException e) {
                    Bukkit.getLogger().info("If you see this, you may have an issue with your table or with the coder XD");
                    e.printStackTrace();
                }

            }

            //update last values to current values
            updateLastValues(results);

        }

        public Aggregator() {
            setup();
        }
    }

    public static void processData(Aggregator aggregator, String sql) {
        ResultSet results;
        try {
            PreparedStatement statement = MySQLConnectionPool.getConnection().prepareStatement(sql);
            results = statement.executeQuery();
            int rowCount = 0;
            while (results.next()) {
                rowCount++;
                aggregator.processRow(results);
            }
            aggregator.processRow(null);
            TimeManagement.sendInfo("Processed " + rowCount + " rows of data");

        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getLogger().info("Something went wrong when getting the statement");
        }

    }

    public MySQLTable(String tableName, ColumnWrapper ...columnWrappers) {
        this.tableName = tableName;
        for (ColumnWrapper column : columnWrappers) {
            if (this.columns.containsKey(column.getName())) {
                throw new IllegalArgumentException("Cannot have two or more columns with the same name");
            }
            this.columns.put(column.getName(), column);
        }
    }
}

