package Storage;

import Main.TimeManagement;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.UUID;

public class TimePlayer {
    private UUID uuid;
    private LoginListDataWrapper loginListDataWrapper;
    private long lastAFKChangeTime;
    private long firstLoginDate, totalTimeAFK, totalTime, loginCount;
    private boolean isAFK;

    public class LoginListDataWrapper {
        private TreeSet<LoginData> newLogins;
        private final TreeSet<LoginData> logins;
        private long currentLogInStart;
        private boolean online;

        public LoginListDataWrapper (final TreeSet<LoginData> loadedLoginData) {
            this.logins = new TreeSet<LoginData>(loadedLoginData);
            this.newLogins = new TreeSet<LoginData>();
            this.online = false;
        }

        protected void loggedIn() {
            currentLogInStart = getCurrentTime();
            this.online = true;
        }

        protected void loggedOut() {
            this.newLogins.add(new LoginData(currentLogInStart, getCurrentTime() - currentLogInStart));
            this.online = false;
        }

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

    }

    public TimePlayer(final UUID uuid) {
        this.uuid = uuid;
        this.loginListDataWrapper = new LoginListDataWrapper(new TreeSet<LoginData>());
        this.firstLoginDate = getCurrentTime();
        this.totalTimeAFK = 0;
        this.totalTime = 0;
        this.loginCount = 0;
        this.isAFK = false;
    }

    public TimePlayer(final UUID uuid, TreeSet<LoginData> loginData, long firstLoginDate, long totalTimeAFK, long totalTime, long loginCount) {
        this.uuid = uuid;
        this.loginListDataWrapper = new LoginListDataWrapper(loginData);
        this.firstLoginDate = firstLoginDate;
        this.totalTimeAFK = totalTimeAFK;
        this.totalTime = totalTime;
        this.loginCount = loginCount;
        this.isAFK = false;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public LoginListDataWrapper getLoginListWrapper() {
        return this.loginListDataWrapper;
    }

    public void loggedIn() {
        loginListDataWrapper.loggedIn();
        lastAFKChangeTime = getCurrentTime();
        loginCount++;
        this.isAFK = true;
    }

    public void loggedOut() {
        loginListDataWrapper.loggedOut();
        //if logging out as afk, add last duration
        if (isAFK) {
            this.totalTimeAFK += lastAFKDuration();
        }
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
