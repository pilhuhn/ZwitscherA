package de.bsd.zwitscher.account;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import de.bsd.zwitscher.MainActivity;
import de.bsd.zwitscher.TweetDB;

import java.util.List;

/**
 * Listener for V11+ devices where the account switching is in the action bar.
 *
 * We need a separate class for the callback, as otherwise we would pull in the ActionBar class
 * into the TabWidget and would thus not work on Android 2.2
 *
 * @author Heiko W. Rupp
 */
public class AccountNavigationListener implements ActionBar.OnNavigationListener {

    private Activity context;
    private List<Account> accountList;
    private Account currentAccount;

    public AccountNavigationListener(Activity context, List<Account> accountList, Account currentAccount) {
        this.context = context;
        this.accountList = accountList;
        this.currentAccount = currentAccount;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {

        Account selectedAccount = accountList.get(itemPosition);
        if (!selectedAccount.equals(currentAccount)) {
            TweetDB tdb = TweetDB.getInstance(context);
            tdb.setDefaultAccount(selectedAccount.getId());
            Intent intent = new Intent(context, MainActivity.class);
            AccountHolder accountHolder = AccountHolder.getInstance(context);
            accountHolder.setAccount(selectedAccount);
            accountHolder.setSwitchingAccounts(true);
            context.startActivity(intent);
            context.finish();
        }


        return true;
    }

}
