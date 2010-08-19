package de.bsd.zwitscher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class LoginActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        
        String accessTokenToken = preferences.getString("accessToken",null);
        String accessTokenSecret = preferences.getString("accessTokenSecret",null);
        if (accessTokenToken!=null && accessTokenSecret!=null) {
        	
        	Intent i = new Intent().setClass(getApplicationContext(), TabWidget.class);
        	startActivity(i);
        	return;
        }
        
        setContentView(R.layout.login_layout);
        
        final Button getPinButton = (Button) findViewById(R.id.GetPinButton);
        getPinButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				//		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						
				TwitterHelper th = new TwitterHelper(getApplicationContext());
				String authUrl;
				try {
					authUrl = th.getAuthUrl();
					i.setData(Uri.parse(authUrl));
					startActivity(i);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(), "Error: " + e.getMessage() , 15000).show();
					e.printStackTrace();
				}
			}
		});
        
        final Button setPinButton = (Button) findViewById(R.id.setPinButton);
        setPinButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
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
							Intent i = new Intent().setClass(getApplication(),TabWidget.class);
							startActivity(i);
							
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Toast.makeText(getApplicationContext(), "Error: " + e.getMessage() , 15000).show();
							e.printStackTrace();
						}
			        }
		        }
			}
		});
        
        final Button continueButton = (Button) findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// Now lets start
				Intent i = new Intent().setClass(getApplication(),TabWidget.class);
				startActivity(i);
			}
		});
	}
	
	
}
