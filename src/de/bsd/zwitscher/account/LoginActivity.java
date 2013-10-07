package de.bsd.zwitscher.account;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.bugsense.trace.BugSenseHandler;
import de.bsd.zwitscher.MainActivity;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TabWidget;
import de.bsd.zwitscher.Tokens;
import de.bsd.zwitscher.TweetDB;
import de.bsd.zwitscher.helper.NetworkHelper;

public class LoginActivity extends Activity {

	private TweetDB tweetDB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (Tokens.bugSenseKey!=null && !Tokens.bugSenseKey.isEmpty()) {
 //           BugSenseHandler.initAndStartSession(this, Tokens.bugSenseKey);
        }

		tweetDB = TweetDB.getInstance(getApplicationContext());
	}

	/**
	 * Check if we already have access tokens. If so just proceed to the
	 * TabWidget. Otherwise check if xAuth is supported or not and show the
	 * respective input screen layout.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		Account account = tweetDB.getDefaultAccount();
		if (account != null) {
			proceed(account);
            return;
		}

        /*
         * We have no valid account yet, so first check if the user has networking,
         * as networking is needed for initial login.
         */
        NetworkHelper nh = new NetworkHelper(this);
        if (!nh.isOnline()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.offline_turn_on_networking))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            LoginActivity.this.finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        // Let NewAccountActivity handle the creation of the first account.
        Intent i = new Intent(this,NewAccountActivity.class);
        startActivity(i);

	}

	/**
	 * Call the TabWidget activity that does the work.
	 *
	 * @param account Logged in account
	 */
	private void proceed(Account account) {
        AccountHolder.getInstance(this).setAccount(account);
		Intent i = new Intent().setClass(this, MainActivity.class);
		startActivity(i);
		finish();
	}

}
