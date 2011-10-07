package de.bsd.zwitscher.account;

import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TabWidget;
import de.bsd.zwitscher.TweetDB;
import de.bsd.zwitscher.TwitterConsumerToken;
import de.bsd.zwitscher.TwitterHelper;
import de.bsd.zwitscher.helper.NetworkHelper;

public class LoginActivity extends Activity {

	TweetDB tweetDB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tweetDB = new TweetDB(this, -1); // -1 is no valid account. But does not
											// matter for the moment.
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
            builder.setMessage("You seem to be offline. Please turn on networking and come back.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            LoginActivity.this.finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        // TODO change this here, as identi.ca / status.net has nothing to do with Twitter xauth/oauth
		if (TwitterConsumerToken.xAuthEnabled) { // TODO what about identi.ca
													// etc? -> 2 step 1st type
													// 2nd user/pass
			setContentView(R.layout.login_layout_classic);
		} else {
			setContentView(R.layout.login_layout);
		}
	}

	/**
	 * Triggers a call to the twitter auth url, where the user will login using
	 * username and password and get a pin back
	 *
	 * @param v
	 */
	@SuppressWarnings("unused")
	public void getPinButton(View v) {
		Intent i = new Intent(Intent.ACTION_VIEW);

		TwitterHelper th = new TwitterHelper(this, null);
		String authUrl;
		try {
			authUrl = th.getAuthUrl();
			i.setData(Uri.parse(authUrl));
			startActivity(i);

		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(),
					15000).show();
			e.printStackTrace();
		}
	}

	/**
	 * Now generate an auth token from the pin, the user provided (obtained from
	 * Twitter), store the pin and auth token and proceed to the TabWidget.
	 *
	 * @param v
	 */
	@SuppressWarnings("unused")
	public void setPinButton(View v) {
		EditText pinField = (EditText) findViewById(R.id.pinText);
		if (pinField != null) {
			if (pinField.getText() != null
					&& pinField.getText().toString() != null) {
				String pin = pinField.getText().toString();
				TwitterHelper th = new TwitterHelper(this, null); // pass a null
																	// account,
																	// which is
																	// uninitialized

				try {
					Account acct = th.generateAccountWithOauth(pin);
					proceed(acct);

				} catch (Exception e) {
					Toast.makeText(getApplicationContext(),
							"Error: " + e.getMessage(), 15000).show();
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Obtain a auth token the xAuth way by providing username and password and
	 * then proceed to the TabWidget. Also works for identi.ca
	 *
	 * @param v
	 *            Source view
	 */
	@SuppressWarnings("unused")
	public void loginXauth(View v) {
		EditText userText = (EditText) findViewById(R.id.login_username);
		String user = userText.getText().toString();
		EditText passwordText = (EditText) findViewById(R.id.login_password);
		String password = passwordText.getText().toString();
		Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		String service = spinner.getSelectedItem().toString();

        try {
            new CreateAccountTask(this,user,password,service,true).execute().get();
            Log.i("Login.xauth","Account created");
            proceed();
        } catch (InterruptedException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (ExecutionException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
//        finish();

/*

		TwitterHelper th = new TwitterHelper(this, null); // pass a null
															// account, which is
															// uninitialized
		try {
			Account account = th.generateAccountWithXauth(user, password, service, true);
			proceed(account);
			finish();

		} catch (Exception e) {
			e.printStackTrace(); // TODO: Customise this generated block
			Toast.makeText(this, "Login failed: " + e.getMessage(),
					Toast.LENGTH_LONG).show();

		}
*/
	}

    private void proceed() {
        Intent i = new Intent().setClass(this, TabWidget.class);
        startActivity(i);
        finish();
    }

	/**
	 * Call the TabWidget activity that does the work.
	 *
	 * @param account
	 *            Logged in account
	 */
	private void proceed(Account account) {
        AccountHolder.getInstance().setAccount(account);
		Intent i = new Intent().setClass(this, TabWidget.class);
		startActivity(i);
		finish();
	}

}
