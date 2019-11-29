package Storage;

public class LoginData implements Comparable<LoginData> {
    private long start, mill;

    public LoginData(final long start, final long mill) {
        this.start = start;
        this.mill = mill;
    }

    public long getStart() {
        return this.start;
    }

    public long getMill() {
        return this.mill;
    }

    @Override
    public int compareTo(LoginData o) {
        return Long.compare(this.start, o.getStart());
    }
}
