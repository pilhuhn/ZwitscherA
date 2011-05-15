package de.bsd.zwitscher.account;

/**
 * Singleton to hold the current account
 * @author Heiko W. Rupp
 */
public class AccountHolder {
    private static AccountHolder ourInstance = new AccountHolder();

    private Account account;

    public static AccountHolder getInstance() {
        return ourInstance;
    }

    private AccountHolder() {
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
