package de.bsd.zwitscher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class LoginActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

    /**
     * Check if we already have access tokens. If so just proceed to the
     * TabWidget. Otherwise check if xAuth is supported or not and show
     * the respective input screen layout.
     */
	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String accessTokenToken = preferences.getString("accessToken",null);
        String accessTokenSecret = preferences.getString("accessTokenSecret",null);
        if (accessTokenToken!=null && accessTokenSecret!=null) {
            proceed();
        	return;
        }

        if (TwitterConsumerToken.xAuthEnabled) {
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

        TwitterHelper th = new TwitterHelper(getApplicationContext());
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
                TwitterHelper th = new TwitterHelper(getApplicationContext());
                try {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                    th.generateAuthToken(pin2);
                    Editor editor = preferences.edit();
                    editor.putString("pin", pin2);
                    editor.commit();


                    // Now lets start
                    proceed();

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Error: " + e.getMessage() , 15000).show();
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Obtain a auth token the xAuth way by providing username and password
     * and then proceed to the TabWidget
     * @param v Source view
     */
    @SuppressWarnings("unused")
    public void loginXauth(View v) {
        EditText userText = (EditText) findViewById(R.id.login_username);
        String user = userText.getText().toString();
        EditText passwordText = (EditText) findViewById(R.id.login_password);
        String password = passwordText.getText().toString();

        TwitterHelper th = new TwitterHelper(this);
        try {
            th.generateAuthToken(user,password);
            proceed();
            finish();

        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
            Toast.makeText(this,"Login failed: " + e.getMessage(),Toast.LENGTH_LONG).show();

        }
    }

    /**
     * Call the TabWidget activity that does the work.
     */
    private void proceed() {
        Intent i = new Intent().setClass(this,TabWidget.class);
        startActivity(i);
    }

}
