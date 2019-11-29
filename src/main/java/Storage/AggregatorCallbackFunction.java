package Storage;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AggregatorCallbackFunction {
    public abstract void call(final ResultSet results) throws SQLException;
}
