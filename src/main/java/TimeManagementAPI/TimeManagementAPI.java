package TimeManagementAPI;

import Storage.DataWrapper;
import Storage.LoginData;
import Storage.PublicDataContainer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TimeManagementAPI {

    CompletableFuture<PublicDataContainer> getBasicInfo(final UUID uuid);

    CompletableFuture<ArrayList<LoginData>> getEachLogin(final UUID uuid);

}
