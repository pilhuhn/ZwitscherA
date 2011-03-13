package de.bsd.zwitscher.account;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TabWidget;
import de.bsd.zwitscher.TweetDB;

/**
 * Handle accounts related stuff like switching and deleting.
 */
public class AccountStuffActivity extends Activity {

    private List<Account> accounts;
    Spinner spinner;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.account_stuff);

        spinner = (Spinner) findViewById(R.id.select_account_spinner);
        getAccounts();
        List<String> data = getStringsForAccounts();
        int checked=0;
        for (int i = 0; i < accounts.size() ; i++) {
            if (accounts.get(i).isDefaultAccount())
                checked=i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(checked);

	}

    @SuppressWarnings("unused")
	public void newAccount(View view) {
		Intent i = new Intent(this,NewAccountActivity.class);
		startActivity(i);
	}

    @SuppressWarnings("unused")
    public void switchAccount(View view) {

        Account account = getSelectedAccountFromSpinner();
        TweetDB tdb = new TweetDB(this,-1);
        tdb.setDefaultAccount(account.getId());
        switchToSelectedAccount(account);

    }

    @SuppressWarnings("unused")
    public void deleteAccount(View v) {

        final Account account = getSelectedAccountFromSpinner();
        String accountName = account.getName() + "@" + account.getServerType();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.really_delete) +" " + accountName + " ?")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Account next = removeAccountFromDb(account);
                        if (next!=null)
                            switchToSelectedAccount(next);
                        else
                            switchToLoginActivity();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();


    }

    @SuppressWarnings("unused")
    public void done(View v) {
        finish();
    }


    private Account removeAccountFromDb(Account account) {

        TweetDB tdb = new TweetDB(this,-1); // Account does not matter
        tdb.deleteAccount(account);

        // Just select the first available account to switch to
        List<Account> theAccounts = tdb.getAccountsForSelection();
        if (!theAccounts.isEmpty()) {
            Account first = theAccounts.get(0);
            tdb.setDefaultAccount(first.getId());
            return first;
        }
        // No more accounts?
        return null;
    }

    private void switchToLoginActivity() {
        // Deleted last account
        Intent i = new Intent(this,LoginActivity.class);
        startActivity(i);
        finish();
    }

    private void switchToSelectedAccount(Account account) {
        Intent intent = new Intent(this, TabWidget.class);
        intent.putExtra("account",account);
        startActivity(intent);
        finish();
    }

    private Account getSelectedAccountFromSpinner() {
        int pos = spinner.getSelectedItemPosition();
        return accounts.get(pos);
    }

    private void getAccounts() {
        TweetDB tdb = new TweetDB(this,-1); // Account id not needed
        accounts = tdb.getAccountsForSelection();
    }

    private List<String> getStringsForAccounts() {
        List<String> data = new ArrayList<String>(accounts.size());
        for (Account account : accounts) {
            String e = account.getAccountIdentifier();
            data.add(e);
        }
        return data;
    }
}
