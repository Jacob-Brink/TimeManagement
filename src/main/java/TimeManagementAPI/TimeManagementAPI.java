package TimeManagementAPI;

import Storage.LoginData;

import java.math.BigInteger;
import java.util.Date;
import java.util.TreeSet;
import java.util.UUID;

public interface TimeManagementAPI {
    BigInteger getTotalPlayTimeMinutes(final UUID uuid);

    BigInteger getAverageMinutesPerGameSession(final UUID uuid);

    int getLoginNumbers(final UUID uuid);

    double getDaysSinceFirstLogin(final UUID uuid);

    long getLongestSession(final UUID uuid);

    Date getFirstLoginDate(UUID uuid);

    TreeSet<LoginData> getData(UUID uuid);

}
