package Storage;

public class LoginData {

    private long login, logout;

    public LoginData(final long login, final long logout) {
        this.login = login;
        this.logout = logout;
    }

    public long getLogin() {
        return this.login;
    }

    public long getLogout() {
        return this.logout;
    }

}
