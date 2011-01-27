package de.bsd.zwitscher.account;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import de.bsd.zwitscher.MultiSelectListActivity;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TabWidget;
import de.bsd.zwitscher.TweetDB;


public class SelectAccountActivity extends Activity {

    private List<Account> accounts;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.select_account);
	}

	public void newAccount(View view) {
		Intent i = new Intent(this,NewAccountActivity.class);
		startActivity(i);
	}

    public void showAccounts(View view) {

        List<String> data = getAccounts();

        Intent intent = new Intent(this,MultiSelectListActivity.class);
        intent.putStringArrayListExtra("data", (ArrayList<String>) data);
        intent.putExtra("mode","single");

        startActivityForResult(intent, 1);
    }


    public void deleteAccount(View v) {
        List<String> data = getAccounts();

        Intent intent = new Intent(this,MultiSelectListActivity.class);
        intent.putStringArrayListExtra("data", (ArrayList<String>) data);
        intent.putExtra("mode","single");

        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==1 && resultCode==RESULT_OK) {
            Account account = getAccountFromActivityResult(data);

            // tell the tab widget to re-read the account
            switchToSelectedAccount(account);
        }
        else if (requestCode==2 && resultCode==RESULT_OK) {


            final Account account = getAccountFromActivityResult(data);
            String accountName = account.getName() + "@" + account.getServerType();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Really delete " + accountName)
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Account next = removeAccountFromDb(account);
                            if (next!=null)
                                switchToSelectedAccount(next);
                            else
                                switchToLoginActivity();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();

        }
    }

    private Account removeAccountFromDb(Account account) {

        TweetDB tdb = new TweetDB(this,-1); // Account does not matter
        tdb.deleteAccount(account);
        List<Account> theAccounts = tdb.getAccountsForSelection();
        if (!theAccounts.isEmpty()) {
            Account first = theAccounts.get(0);
            tdb.setDefaultAccount(first.getId());
            return first;
        }
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

    private Account getAccountFromActivityResult(Intent data) {
        String o = (String) data.getExtras().get("data");
        System.out.println("res : " + o);

        // id is in the first part up to the colon
        int beginIndex = o.indexOf(':');
        String ids = o.substring(0, beginIndex);

        int id = Integer.parseInt(ids);

        TweetDB tdb = new TweetDB(this,-1);
        tdb.setDefaultAccount(id);
        Account account = null;
        for (Account tmp : accounts) {
            if (tmp.getId()==id) {
                account = tmp;
            }
        }
        return account;
    }

    private List<String> getAccounts() {
        TweetDB tdb = new TweetDB(this,-1); // Account id not needed
        accounts = tdb.getAccountsForSelection();
        List<String> data = new ArrayList<String>(accounts.size());
        for (Account account : accounts) {
            String e = account.getId() + ": " + account.getServerType() + ": " + account.getName();
            if (account.isDefaultAccount())
                e += ", (*)";
            data.add(e);
        }
        return data;
    }


}
