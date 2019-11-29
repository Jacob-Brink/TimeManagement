package Storage;

import Main.TimeManagement;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.UUID;

public class TimePlayer {
    private TreeSet<LoginData> logins, newLogins;
    private UUID uuid;
    private long currentLogIn;
    private boolean active;

    public TimePlayer(final UUID uuid) {
        this.newLogins = new TreeSet<LoginData>();
        this.logins = new TreeSet<LoginData>();
        this.uuid = uuid;
        this.active = false;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public void addStoredLogin(LoginData loginData) {
        this.logins.add(loginData);
    }

    public TreeSet<LoginData> getNewLogins() {
        return this.newLogins;
    }

    public TreeSet<LoginData> getAllLogins() {
        TreeSet<LoginData> totalLoginData = new TreeSet<LoginData>();
        totalLoginData.addAll(logins);
        totalLoginData.addAll(newLogins);
        return totalLoginData;
    }

    public void setNewToStored() {
        this.logins.addAll(this.newLogins);
        this.newLogins = new TreeSet<LoginData>();
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void loggedIn() {
        currentLogIn = getCurrentTime();
        this.active = true;
    }

    public void loggedOut() {
        this.newLogins.add(new LoginData(currentLogIn, getCurrentTime() - currentLogIn));
        this.active = false;
    }

    public long getCurrentSessionDuration() {
        return (getCurrentTime() - currentLogIn);
    }

    public long getCurrentSessionStart() {
        return currentLogIn;
    }

    public boolean isActive() {
        return this.active;
    }
}
