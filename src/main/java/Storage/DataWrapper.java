package Storage;

import Main.TimeManagement;

import java.util.UUID;

public class DataWrapper{

    private boolean isDoing;
    private long firstStart, totalTime, startCount;
    private long runningTotal;
    private long lastStart;
    private UUID uuid;

    public DataWrapper(UUID uuid, long firstStart, long totalTime, long startCount, long lastStart) {
        this.firstStart = firstStart;
        this.totalTime = totalTime;
        this.startCount = startCount;
        this.lastStart = lastStart;
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public long currentTime() {
        return System.currentTimeMillis();
    }

    public boolean isDoing() {
        return isDoing;
    }

    public void setDoing(boolean doing) {
        isDoing = doing;
    }

    public long getFirstStart() {
        return firstStart;
    }

    public void setFirstStart(long firstStart) {
        this.firstStart = firstStart;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getLastStart() {
        return lastStart;
    }

    public void setLastStart(long lastStart) {
        this.lastStart = lastStart;
    }

    public long getStartCount() {
        return startCount;
    }

    public void incrementStartCount(long startCount) {
        this.startCount = startCount;
    }

    public long getRunningTotalTime(final long currentTime) {
        //TimeManagement.sendInfo("Total: " + totalTime + ", CurrentTime: " + currentTime() + "lastStart: " + lastStart);
        return totalTime + currentTime - lastStart;
    }
}
