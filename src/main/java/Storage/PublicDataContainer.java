package Storage;

public class PublicDataContainer {

    public long getStartTime() {
        return startTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getLoginCount() {
        return loginCount;
    }

    private long startTime, totalTime, loginCount;

    public PublicDataContainer(final long startTime, final long totalTime, final long loginCount) {
        this.startTime = startTime;
        this.totalTime = totalTime;
        this.loginCount = loginCount;
    }



}
