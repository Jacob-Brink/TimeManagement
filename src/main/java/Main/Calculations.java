package Main;

import Storage.Storage;
import Storage.TimePlayer;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.TreeSet;
import java.util.UUID;
import Storage.LoginData;
import org.bukkit.Bukkit;

public class Calculations {
    public static BigInteger getTotalPlayTimeMinutes(final UUID uuid) {
        TimePlayer timePlayer = Storage.getTimePlayer(uuid);
        if (timePlayer != null) {
            BigInteger sum = BigInteger.valueOf(0L);
            for (LoginData login : timePlayer.getAllLogins()) {
                sum = sum.add(BigInteger.valueOf(login.getMill()));
            }
            sum = sum.add(BigInteger.valueOf(timePlayer.getCurrentSessionDuration()));
            sum = sum.divide(BigInteger.valueOf(60000L));
            return sum;
        } else {
            throw new InvalidParameterException("Given uuid does not exist in our records.");
        }
    }

    public static BigInteger getAverageMinutesPerGameSession(UUID uuid) {
        return getTotalPlayTimeMinutes(uuid).divide(BigInteger.valueOf(getLoginNumbers(uuid)));
    }

    public static int getLoginNumbers(UUID uuid) {
        TimePlayer timePlayer = Storage.getTimePlayer(uuid);
        if (timePlayer != null) {
            return timePlayer.getAllLogins().size();
        } else {
            throw new InvalidParameterException("Given uuid does not exist in our records.");
        }
    }

    public static double getDaysSinceFirstLogin(UUID uuid) {
        TimePlayer timePlayer = Storage.getTimePlayer(uuid);
        if (timePlayer != null) {
            TreeSet<LoginData> loginData = timePlayer.getAllLogins();
            if (loginData.first() != null) {
                return (System.currentTimeMillis()-loginData.first().getStart()) / (1000*60*60*24);
            };
            return -1;
        } else {
            throw new InvalidParameterException("Given uuid does not exist in our records.");
        }
    }

    public static long getLongestSession(UUID uuid) {
        TimePlayer timePlayer = Storage.getTimePlayer(uuid);
        if (timePlayer != null) {
            long max = 0;
            for (LoginData loginData : timePlayer.getAllLogins()) {
                if (loginData.getMill() > max)
                    max = loginData.getMill();
            }
            return max;
        } else {
            throw new InvalidParameterException("Given uuid does not exist in our records.");
        }
    }

    public static Date getFirstLoginDate(UUID uuid) {
        TimePlayer timePlayer = Storage.getTimePlayer(uuid);
        if (timePlayer != null) {
            TreeSet<LoginData> loginData = timePlayer.getAllLogins();
            long firstLoginEpoch;
            if (loginData.size() > 0) {
                LoginData firstLogin = loginData.first();
                firstLoginEpoch = firstLogin.getStart();
            } else {
                firstLoginEpoch = timePlayer.getCurrentSessionStart();
            }

            return new Date(firstLoginEpoch);

        } else {
            throw new InvalidParameterException("Given uuid does not exist in our records.");
        }
    }
}
