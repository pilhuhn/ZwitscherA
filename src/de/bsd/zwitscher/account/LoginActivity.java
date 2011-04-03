package de.bsd.zwitscher.account;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TabWidget;
import de.bsd.zwitscher.TweetDB;
import de.bsd.zwitscher.TwitterConsumerToken;
import de.bsd.zwitscher.TwitterHelper;


public class LoginActivity extends Activity {

    TweetDB tweetDB;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        tweetDB = new TweetDB(this,-1); // -1 is no valid account. But does not matter for the moment.
	}

    /**
     * Check if we already have access tokens. If so just proceed to the
     * TabWidget. Otherwise check if xAuth is supported or not and show
     * the respective input screen layout.
     */
	@Override
	protected void onResume() {
		super.onResume();

        Account account = tweetDB.getDefaultAccount();
        if (account!=null) {
            proceed(account);
        }

//		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//
//        String accessTokenToken = preferences.getString("accessToken",null);
//        String accessTokenSecret = preferences.getString("accessTokenSecret",null);
//        if (accessTokenToken!=null && accessTokenSecret!=null) {
//            proceed();
//        	return;
//        }

        if (TwitterConsumerToken.xAuthEnabled) {  // TODO what about laconica etc? -> 2 step 1st type 2nd user/pass
            setContentView(R.layout.login_layout_classic);
        }
        else {
            setContentView(R.layout.login_layout);
        }
	}

    /**
     * Triggers a call to the twitter auth url, where the user
     * will login using username and password and get a pin back
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
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage() , 15000).show();
            e.printStackTrace();
        }
    }

    /**
     * Now generate an auth token from the pin, the user provided (obtained
     * from Twitter), store the pin and auth token and proceed to the TabWidget.
     * @param v
     */
    @SuppressWarnings("unused")
    public void setPinButton(View v) {
        EditText pinField = (EditText) findViewById(R.id.pinText);
        if (pinField!=null) {
            if (pinField.getText()!=null && pinField.getText().toString()!= null) {
                String pin2 = pinField.getText().toString();
                TwitterHelper th = new TwitterHelper(this, null); // pass a null account, which is uninitialized
                try {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                    th.generateAuthToken(pin2);
                    Editor editor = preferences.edit();
                    editor.putString("pin", pin2);
                    editor.commit();


                    // Now lets start
                    proceed(null); // TODO

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Error: " + e.getMessage() , 15000).show();
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Obtain a auth token the xAuth way by providing username and password
     * and then proceed to the TabWidget. Also works for identi.ca
     * @param v Source view
     */
    @SuppressWarnings("unused")
    public void loginXauth(View v) {
        EditText userText = (EditText) findViewById(R.id.login_username);
        String user = userText.getText().toString();
        EditText passwordText = (EditText) findViewById(R.id.login_password);
        String password = passwordText.getText().toString();
        Spinner spinner = (Spinner) findViewById(R.id.spinner1);
        String service = spinner.getSelectedItem().toString();

        TwitterHelper th = new TwitterHelper(this, null); // pass a null account, which is uninitialized
        try {
            Account account = th.generateAccount(user, password, service, true);
            proceed(account);
            finish();

        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
            Toast.makeText(this,"Login failed: " + e.getMessage(),Toast.LENGTH_LONG).show();

        }
    }

    /**
     * Call the TabWidget activity that does the work.
     * @param account Logged in account
     */
    private void proceed(Account account) {
        Intent i = new Intent().setClass(this,TabWidget.class);
        i.putExtra("account",account);
        startActivity(i);
        finish();
    }

}
