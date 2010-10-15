package de.bsd.zwitscher;


import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;
import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class NewTweetActivity extends Activity {

	EditText edittext;
	Status origStatus;
	Pattern p = Pattern.compile(".*?(@\\w+ )*.*");
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.new_tweet);

	    final Button tweetButton = (Button) findViewById(R.id.TweetButton);
		edittext = (EditText) findViewById(R.id.edittext);
		edittext.setSelected(true);
		tweetButton.setEnabled(false);

		Bundle bundle = getIntent().getExtras();
		if (bundle!=null) {
			origStatus = (Status) bundle.get("status");
			Log.i("Replying..", "Orig is " + origStatus);
			TextView textOben = (TextView) findViewById(R.id.textOben);
			textOben.setText(origStatus.getText());
			String op = (String) bundle.get("op");
			if (op!=null) {
				if (op.equals(getString(R.string.reply))) {
					edittext.setText("@"+origStatus.getUser().getScreenName()+" ");
				} else if (op.equals(getString(R.string.replyall))) {
					String oText = origStatus.getText();
//					Matcher m = p.matcher(oText);
					StringBuilder sb = new StringBuilder();
					sb.append("@");
					sb.append(origStatus.getUser().getScreenName()).append(" ");
//					if (m.matches()) {
//						for (int i = 1; i < m.groupCount() ; i++) {
//							sb.append(m.group(i));
//							sb.append(" ");
//						}
//					}
					findUsers (sb,oText);
					edittext.setText(sb.toString());
				} else if (op.equals(getString(R.string.classicretweet))) {
					String msg = "RT @" + origStatus.getUser().getScreenName() + " ";
					msg = msg + origStatus.getText();
					edittext.setText(msg); // TODO limit to 140 chars
				}
			}
		}
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean locationEnabled = preferences.getBoolean("location",false);
		CheckBox box = (CheckBox) findViewById(R.id.GeoCheckBox);
		if (locationEnabled) {
			box.setEnabled(true);
			box.setChecked(true);
		}
			
		

		edittext.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {

		        if ((event.getAction() == KeyEvent.ACTION_UP) && edittext.getText().length() >0 ) {
		        	tweetButton.setEnabled(true);
		        }

		        if (event.getAction() == KeyEvent.ACTION_UP || event.getAction() == KeyEvent.ACTION_DOWN) {
		        	int len = edittext.getText().length();
		        	TextView tv = (TextView) findViewById(R.id.CharCount);
		        	tv.setText(String.valueOf(140-len));
		        }

		        return false;
		    }
		});


		tweetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				StatusUpdate up  = new StatusUpdate(edittext.getText().toString());
                // add location  if enabled in preferences and checked on tweet
                CheckBox box = (CheckBox) findViewById(R.id.GeoCheckBox);
                boolean locationEnabled = box.isChecked();
                if (locationEnabled) {
                    Location location = getCurrentLocation();
                    if (location!=null) {
                        GeoLocation geoLocation = new GeoLocation(location.getLatitude(),location.getLongitude());
                        up.setLocation(geoLocation);
                    }
                }
	        	if (origStatus!=null) {
	        		up.setInReplyToStatusId(origStatus.getId());
	        	}
	        	tweet(up);
	        	origStatus=null;
	        	finish();

			}
		});

		final Button clearButton = (Button) findViewById(R.id.ClearButton);
		clearButton.setOnClickListener(new OnClickListener() {


			@Override
			public void onClick(View v) {
				edittext.setText("");

			}
		});

	}


    private Location getCurrentLocation() {
		LocationManager locMngr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location currLoc = null;
		currLoc = locMngr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (currLoc == null) {
			currLoc = locMngr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		return currLoc;
	}

	/** Extract the @users from the passed oText and put them into sb
	 * Should go away in favor of a RegExp
	 * @deprecated
	 */
	private void findUsers(StringBuilder sb, String oText) {
		if (!oText.contains("@"))
			return;

		String txt = oText;
		while (txt.length()>0) {
			int j = txt.indexOf("@");
			if (j<0)
				return;
			txt = txt.substring(j);
			int k = txt.indexOf(" ");
			if (k<0) { // end
				sb.append(txt);
				return;
			} else {
				sb.append(txt.substring(0, k));
				sb.append(" ");
				txt = txt.substring(k);
			}
		}
	}


	public void tweet(StatusUpdate update) {
        new UpdateStatusTask().execute(update);
	}

    private class UpdateStatusTask extends AsyncTask<StatusUpdate,Void,String> {

        @Override
        protected String doInBackground(StatusUpdate... statusUpdates) {
            TwitterHelper th = new TwitterHelper(getApplicationContext());
            String ret = th.updateStatus(statusUpdates[0]);
            return ret;
        }

        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
        }
    }

}
