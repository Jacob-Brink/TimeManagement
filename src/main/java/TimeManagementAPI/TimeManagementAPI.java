package TimeManagementAPI;

import Storage.DataWrapper;
import Storage.LoginData;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TimeManagementAPI {
    long getTotalTime(final UUID uuid) throws Exception;

    long getTotalAFKTime(final UUID uuid) throws Exception;

    long getLoginCount(final UUID uuid) throws Exception;

    long getFirstStart(final UUID uuid) throws Exception;

    CompletableFuture<ArrayList<LoginData>> getEachLogin(final UUID uuid);

}
