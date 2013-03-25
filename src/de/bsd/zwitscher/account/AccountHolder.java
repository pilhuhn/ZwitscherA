package de.bsd.zwitscher.account;

import android.content.Context;
import de.bsd.zwitscher.TweetDB;

import java.util.HashSet;
import java.util.Set;

/**
 * Singleton to hold the current account
 * @author Heiko W. Rupp
 */
public class AccountHolder {
    private static AccountHolder ourInstance = new AccountHolder();

    private Account account;
    private Set<String> userNames = new HashSet<String>();
    private Set<String> hashTags = new HashSet<String>();
    private boolean switchingAccounts = false;

    public static AccountHolder getInstance(Context context) {
        if (ourInstance.getAccount()==null) {
            TweetDB tweetDB = TweetDB.getInstance(context);
            ourInstance.account = tweetDB.getDefaultAccount();
        }
        return ourInstance;
    }

    private AccountHolder() {
    }

    public boolean isSwitchingAccounts() {
        return switchingAccounts;
    }

    public void setSwitchingAccounts(boolean switchingAccounts) {
        this.switchingAccounts = switchingAccounts;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Set<String> getUserNames() {
        return userNames;
    }

    public void addUserName(String name) {
        if (!name.startsWith("@")) {
            userNames.add("@"+name);
        } else {
            userNames.add(name);
        }

    }

    public Set<String> getHashTags() {
        return hashTags;
    }

    public void addHashTag(String name) {
        if (!name.startsWith("#")) {
            hashTags.add("#"+name);
        } else {
            hashTags.add(name);
        }

    }

}
