package Storage;

import Main.TimeManagement;

import java.util.TreeSet;
import java.util.UUID;

public class TimePlayer {
    private UUID uuid;
    private long lastAFKChangeTime;
    private long firstLoginDate, totalTimeAFK, totalTime, loginCount;
    private boolean isAFK;
    private boolean changed;
    private boolean loggedIn;

    private TreeSet<LoginData> newLogins;
    private TreeSet<LoginData> logins;
    private long currentLogInStart;
    private boolean online;

    public TreeSet<LoginData> getNewLoginData() {
        return this.newLogins;
    }

    public LoginData getCurrentLoginData() {
        return new LoginData(currentLogInStart, (getCurrentTime() - currentLogInStart));
    }

    public TreeSet<LoginData> getAllLoginData() {
        TreeSet<LoginData> storedLoginDataCopy = new TreeSet<LoginData>(this.logins);
        storedLoginDataCopy.addAll(this.newLogins);
        return storedLoginDataCopy;
    }

    public boolean isOnline() {
        return this.online;
    }

    public void setNewToStored() {
        this.newLogins = new TreeSet<LoginData>();
    }

    public boolean changed() {
        return this.changed;
    }

    public long getTotalPlayTime() {
        return this.totalTime;
    }

    public long getTotalAFKTime() {
        return this.totalTimeAFK;
    }

    public long getFirstLoginTime() {
        return this.firstLoginDate;
    }

    public long getLoginCount() {
        return this.loginCount;
    }

    public TimePlayer(final UUID uuid) {
        this.uuid = uuid;
        this.firstLoginDate = getCurrentTime();
        this.totalTimeAFK = 0;
        this.totalTime = 0;
        this.loginCount = 0;
        this.isAFK = false;
        this.changed = false;
        this.logins = new TreeSet<LoginData>();
        this.newLogins = new TreeSet<LoginData>();
        this.online = false;
        this.loggedIn = false;
    }

    public TimePlayer(final UUID uuid, TreeSet<LoginData> loadedLoginData, long firstLoginDate, long totalTimeAFK, long totalTime, long loginCount) {
        this.uuid = uuid;
        this.firstLoginDate = firstLoginDate;
        this.totalTimeAFK = totalTimeAFK;
        this.totalTime = totalTime;
        this.loginCount = loginCount;
        this.isAFK = false;
        this.changed = false;
        this.logins = new TreeSet<LoginData>(loadedLoginData);
        this.newLogins = new TreeSet<LoginData>();
        this.online = false;
        this.loggedIn = false;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void loggedIn() {
        currentLogInStart = getCurrentTime();
        this.online = true;
        lastAFKChangeTime = getCurrentTime();
        loginCount++;
        this.isAFK = false;
        this.changed = true;
        TimeManagement.sendInfo("LOGGED IN!");
    }

    public void loggedOut() {
        this.newLogins.add(new LoginData(currentLogInStart, getCurrentTime() - currentLogInStart));
        this.online = false;
        totalTime += (getCurrentTime() - currentLogInStart);
        //if logging out as afk, add last duration
        if (isAFK) {
            this.totalTimeAFK += lastAFKDuration();
        }
        this.changed = true;
        TimeManagement.sendInfo("LOGGED OUT!");
    }

    public void resetChangedStatus() {
        this.changed = false;
    }

    public void resetLoggedInOnceStatus() {
        this.loggedIn = false;
    }

    private long lastAFKDuration() {
        return getCurrentTime()-this.lastAFKChangeTime;
    }

    public void setAFK(boolean afk) {
        isAFK = afk;
        if (afk) {
            //when it switches to being afk, set start of lastAFKChangeTime to currentTime
            this.lastAFKChangeTime = getCurrentTime();
        } else {
            //when it switches to not afk, then it must have been the case that it was afk for last time period
            //add this to totalTimeAFK
            this.totalTimeAFK += lastAFKDuration();
        }
    }


}
