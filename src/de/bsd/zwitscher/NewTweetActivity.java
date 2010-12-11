package de.bsd.zwitscher;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.widget.*;
import de.bsd.zwitscher.helper.PicHelper;
import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import twitter4j.User;

public class NewTweetActivity extends Activity implements LocationListener {

	EditText edittext;
	Status origStatus;
	Pattern p = Pattern.compile(".*?(@\\w+ )*.*");
    ProgressBar pg;
    User toUser = null;
    TextView charCountView;
    String picturePath;
    LocationManager locationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	    setContentView(R.layout.new_tweet);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
        pg = (ProgressBar) findViewById(R.id.title_progress_bar);
        charCountView = (TextView) findViewById(R.id.CharCount);


	    final Button tweetButton = (Button) findViewById(R.id.TweetButton);
		edittext = (EditText) findViewById(R.id.edittext);
		edittext.setSelected(true);
		tweetButton.setEnabled(false);

		Bundle bundle = getIntent().getExtras();
		if (bundle!=null) {
			TextView textOben = (TextView) findViewById(R.id.textOben);

            String bundleText = bundle.getString(Intent.EXTRA_TEXT);

			String op = (String) bundle.get("op");
         User directUser = (User) bundle.get("user"); // set when coming from user detail view
			if (op!=null) {
            origStatus = (Status) bundle.get("status");
            Log.i("Replying..", "Orig is " + origStatus);
             if (op.equals(getString(R.string.reply))) {
                    textOben.setText(origStatus.getText());
					edittext.setText("@"+origStatus.getUser().getScreenName()+" ");
				} else if (op.equals(getString(R.string.replyall))) {
                    textOben.setText(origStatus.getText());
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
                    textOben.setText(origStatus.getText());
					String msg = "RT @" + origStatus.getUser().getScreenName() + " ";
					msg = msg + origStatus.getText();
					edittext.setText(msg); // limit to 140 chars is done by the edittext via maxLength attribute
				} else if (op.equals(getString(R.string.direct))) {

                 if (directUser!=null)
                     toUser = directUser;
                 else if (origStatus!=null) {
                     toUser = origStatus.getUser();
                 }
                String s = getString(R.string.send_direct_to);
                textOben.setText(s + " "+ toUser.getScreenName());
                }
			}
            else { // OP is null -> new tweet
                if (bundleText!=null)
                    edittext.setText(bundleText);
            }
		}
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean locationEnabled = preferences.getBoolean("location",false);
		CheckBox box = (CheckBox) findViewById(R.id.GeoCheckBox);
		if (locationEnabled) {
			box.setEnabled(true);
			box.setChecked(true);

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1,1,this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1,1,this);


		}

        // Add a listener to count the text length.
        edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

                int tlen = edittext.getText().length();

                if (tlen >0 ) {
                    tweetButton.setEnabled(true);
                }
                else
                    tweetButton.setEnabled(false);

                charCountView.setText(String.valueOf(140-tlen));
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
                if (toUser==null)
	        	    tweet(up);
                else
                    direct(toUser,edittext.getText().toString());
	        	origStatus=null;
                switchOffLocationUpdates();
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


    /**
     * Trigger an update (new tweet, reply )
     * @param update Update to send
     */
	public void tweet(StatusUpdate update) {
        UpdateRequest request = new UpdateRequest(UpdateType.UPDATE);
        request.statusUpdate = update;
        request.picturePath = picturePath;
        new UpdateStatusTask(this,pg).execute(request);
	}

    /**
     * Trigger a direct message to the given user.
     * @param toUser user to send a direct message to
     * @param msg message to send
     */
    private void direct(User toUser, String msg) {
        UpdateRequest request = new UpdateRequest(UpdateType.DIRECT);
        request.message = msg;
        request.id = toUser.getId();
        new UpdateStatusTask(this,pg).execute(request);
    }

    /**
     * Trigger a list of usernames to pick one from and to insert
     * into the tweet
     * @param v
     */
    @SuppressWarnings("unused")
    public void selectUser(View v) {

        TwitterHelper th = new TwitterHelper(this);
        List<User> users = th.getUsersFromDb();
        List<String> data = new ArrayList<String>(users.size());

        for (User user : users) {
            String item = user.getScreenName() + ", " + user.getName();
            data.add(item);
        }

        Intent intent = new Intent(this,MultiSelectListActivity.class);
        intent.putStringArrayListExtra("data", (ArrayList<String>) data);
        intent.putExtra("mode","single");

        startActivityForResult(intent, 2);

    }


    /**
     * Called from the Back button to finish (abort) the activity
     * @param v
     */
    @SuppressWarnings("unused")
    public void done(View v) {
        finish();
    }

    /**
     * Trigger taking a picture
     * Called from the camera button.
     * Image size is small
     * @param v
     */
    @SuppressWarnings("unused")
    public void takePicture(View v) {

        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 1);
    }


    /**
     * Process the result when the picture has been taken.
     * @param requestCode Code of the started activity
     * @param resultCode Indicator of success
     * @param data Values passed from the started activity
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // code 1 = take picture
        if(requestCode==1  && resultCode==RESULT_OK) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            PicHelper picHelper = new PicHelper();
            picturePath = picHelper.storeBitmap(bitmap, "tmp-pic", Bitmap.CompressFormat.JPEG, 100); // TODO adjust quality per network
            Log.d("NewTweetActivity.onActivityResult","path: " + picturePath);

            UpdateRequest req = new UpdateRequest(UpdateType.UPLOAD_PIC);
            req.picturePath = picturePath;
            req.view = edittext;

            new UpdateStatusTask(this,pg).execute(req);
        } else if (requestCode==2 && resultCode==RESULT_OK) {
            String item = (String) data.getExtras().get("data");
            if (item.contains(", ")) {
                String user = item.substring(0,item.indexOf(", "));
                edittext.append("@" + user + " ");
            }
        }
    }


    protected void onPause() {
        super.onPause();
        switchOffLocationUpdates();
    }

    private void switchOffLocationUpdates() {
        System.out.println("Switch off updates");
        if (locationManager!=null) {
            locationManager.removeUpdates(this);
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        // TODO: Customise this generated block
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO: Customise this generated block
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO: Customise this generated block
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO: Customise this generated block
    }

}
