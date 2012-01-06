package de.bsd.zwitscher;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.helper.PicHelper;
import de.bsd.zwitscher.helper.UrlHelper;
import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import twitter4j.User;
import twitter4j.media.MediaProvider;

/**
 * This activity is called when the user wants to write a new tweet/dent.
 * This can be for a new message, a new direct message or also when replying
 * to an existing message.
 */
public class NewTweetActivity extends Activity implements LocationListener {

	EditText edittext;
	Status origStatus;
    ProgressBar pg;
    User toUser = null;
    TextView charCountView;
    String picturePath = null;
    LocationManager locationManager;
    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT<11) {
            requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
            setContentView(R.layout.new_tweet);
            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
            pg = (ProgressBar) findViewById(R.id.title_progress_bar);
        }
        else
            setContentView(R.layout.new_tweet_honeycomb);
        charCountView = (TextView) findViewById(R.id.CharCount);
        account = AccountHolder.getInstance().getAccount();


        final ImageButton tweetButton = (ImageButton) findViewById(R.id.TweetButton);
        edittext = (EditText) findViewById(R.id.edittext);
        edittext.setSelected(true);
        if (tweetButton!=null)
            tweetButton.setEnabled(false);

        Bundle bundle = getIntent().getExtras();
        if (bundle!=null) {
            TextView textOben = (TextView) findViewById(R.id.textOben);

            String bundleText = bundle.getString(Intent.EXTRA_TEXT);

            String op = (String) bundle.get("op");
            User directUser = (User) bundle.get("user"); // set when coming from user detail view
            if (op!=null) {
                origStatus = (Status) bundle.get("status");

                if (op.equals(getString(R.string.reply))) {
                    Set<String> hashTags = getHashTags(origStatus.getText());
                    textOben.setText(origStatus.getText());
                    StringBuilder builder = new StringBuilder();
                    builder.append("@").append(origStatus.getUser().getScreenName()).append(" ");
                    for (String hashTag : hashTags) {
                        builder.append(hashTag).append(" ");
                    }

                    edittext.setText(builder.toString());
                } else if (op.equals(getString(R.string.replyall))) {
                    textOben.setText(origStatus.getText());
                    String oText = origStatus.getText();

                    StringBuilder sb = new StringBuilder();
                    sb.append("@");
                    sb.append(origStatus.getUser().getScreenName()).append(" ");

                    findUsers (sb,oText);
                    Set<String> hashTags = getHashTags(origStatus.getText());
                    for (String hashTag : hashTags) {
                        sb.append(hashTag).append(" ");
                    }

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
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Not needed
            }

            public void afterTextChanged(Editable editable) {
                // Not needed
            }

            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

                int tlen = edittext.getText().length();

                if (tweetButton!=null) {
                    if (tlen >0 ) {
                        tweetButton.setEnabled(true);
                    }
                    else
                        tweetButton.setEnabled(false);
                }

                charCountView.setText(String.valueOf(140-tlen)); // TODO if url detected 4 twitter, decrease by 20 chars
            }
        });

    }

    private Set<String> getHashTags(String text) {
        String[] tokens = text.split(" ");

        Set<String> tags = new HashSet<String>();
        for (String token : tokens) {
            if (token.startsWith("#"))
                tags.add(token);
            }
        return tags;
    }

    public void clear(View v) {
        edittext.setText("");

    }

    public void shortenUrls(View v) {
        String text = edittext.getText().toString();
        Map<Integer,String> urls = new HashMap<Integer,String>();
        String[] tokens = text.split(" ");

        for (int i = 0 ; i < tokens.length ; i++) {
            String token = tokens[i];
            if (token.startsWith("http://") || token.startsWith("https://")) {
                urls.put(i,token);
            }
        }
        try {
            Map<Integer,String> replacements = new UrlShortenerTask(this,urls.size()).execute(urls).get();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < tokens.length ; i++) {
                if (replacements.containsKey(i))
                    builder.append(replacements.get(i));
                else
                    builder.append(tokens[i]);
                builder.append(" ");
            }
            String newText = builder.toString();
            edittext.setText(newText);

        } catch (Exception e) {
            String tmp = getString(R.string.url_shortening_failed,e.getMessage());
            Toast.makeText(this,tmp,Toast.LENGTH_LONG).show();
        }

    }

    public void finallySend(View v) {
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
	 * TODO optimize
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
        if (picturePath!=null)
            request.picturePath = picturePath;
        Toast.makeText(this,R.string.trying_to_send,Toast.LENGTH_SHORT).show();
        new UpdateStatusTask(this,pg, account).execute(request);
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
        if (picturePath!=null)
            request.picturePath = picturePath;
        Toast.makeText(this,R.string.trying_to_send,Toast.LENGTH_SHORT).show();

        new UpdateStatusTask(this,pg, account).execute(request);
    }

    /**
     * Trigger a list of usernames to pick one from and to insert
     * into the tweet
     * @param v
     */
    @SuppressWarnings("unused")
    public void selectUser(View v) {

        TwitterHelper th = new TwitterHelper(this, account);
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
     * Trigger taking a picture, called from the camera button.
     * Actually we present a menu from which the user will be able to take a picture
     * or select one from the gallery of pictures taken
     * @param v
     */
    @SuppressWarnings("unused")
    public void takePicture(View v) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.take_picture_via);
        builder.setItems(R.array.take_picture_items,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent;
                    switch (i) {
                    case 0:

                        intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, 1);
                        break;
                    case 1:

                        Uri tmpUri = Uri.fromFile(getTempFile(getApplicationContext()));
                        intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT,tmpUri);
                        startActivityForResult(intent, 3);

                        break;
                    case 2:
                        intent =  new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        startActivityForResult(intent, 4);
                    }

                }
            });

        AlertDialog dialog = builder.create();
        dialog.show();

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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String provider = preferences.getString("pictureService","yfrog");

        // code 1 = take picture
        if(requestCode==1  && resultCode==RESULT_OK) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            PicHelper picHelper = new PicHelper();
            picturePath = picHelper.storeBitmap(bitmap, "tmp-pic", Bitmap.CompressFormat.JPEG, 100); // TODO adjust quality per network
            Log.d("NewTweetActivity.onActivityResult","path: " + picturePath);

//            UpdateRequest req = new UpdateRequest(UpdateType.UPLOAD_PIC);
//            req.picturePath = picturePath;
//            req.view = edittext;
//
//            new UpdateStatusTask(this,pg, account).execute(req);

            if (provider.equals("twitter"))
                Toast.makeText(this,R.string.picture_attached,Toast.LENGTH_SHORT).show();
            else
                edittext.setText(edittext.getText().toString() + "@@@@_image__url_@@@@");
        } else if (requestCode==2 && resultCode==RESULT_OK) {
            String item = (String) data.getExtras().get("data");
            if (item.contains(", ")) {
                String user = item.substring(0,item.indexOf(", "));
                edittext.append("@" + user + " ");
            }
        } else if (requestCode==3 && resultCode==RESULT_OK) {
            // large size image
            File file = getTempFile(this);

//            UpdateRequest req = new UpdateRequest(UpdateType.UPLOAD_PIC);
            Toast.makeText(this,R.string.picture_attached,Toast.LENGTH_SHORT).show();

            picturePath = file.getAbsolutePath();
//            req.view = edittext;
//            new UpdateStatusTask(this,pg, account).execute(req);

        } else if (requestCode==4 && resultCode==RESULT_OK) {
            // image from gallery
            Uri selectedImageUri = data.getData();
            picturePath = getPath(selectedImageUri);
            Toast.makeText(this,R.string.picture_attached,Toast.LENGTH_SHORT).show();

//            UpdateRequest req = new UpdateRequest(UpdateType.UPLOAD_PIC);
//            req.picturePath = picturePath;
//            req.view = edittext;
//            new UpdateStatusTask(this,pg, account).execute(req);

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


    public void onLocationChanged(Location location) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (android.os.Build.VERSION.SDK_INT>=11) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.new_tweet_menu_honey,menu);

            ActionBar actionBar = this.getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);

            return true;
        }
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.send:
                finallySend(null);
                break;
            case R.id.camera:
                takePicture(null);
                break;
            case R.id.clear:
                clear(null);
                break;
            case R.id.pickUser:
                selectUser(null);
                break;
            case R.id.shortenUrls:
                shortenUrls(null);
                break;

            default:
                Log.e(getClass().getName(),"Unknown menu item: " + item.toString());

        }

        return super.onOptionsItemSelected(item);
    }

    private File getTempFile(Context context){
        File path = new File( Environment.getExternalStorageDirectory(), context.getPackageName() );

        if(!path.exists())
            path.mkdir();

        return new File(path, "image.tmp");
    }

    // Taken from http://stackoverflow.com/questions/2169649/open-an-image-in-androids-built-in-gallery-app-programmatically/2636538#2636538
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private class UrlShortenerTask extends AsyncTask<Map<Integer,String>,Integer,Map<Integer,String>>{

        private Context context;
        private int num;
        private ProgressDialog dialog;

        private UrlShortenerTask(Context context,int num) {
            this.context = context;
            this.num = num;
        }

        protected Map<Integer,String> doInBackground(Map<Integer,String>... maps) {

            Map<Integer,String> res = new HashMap<Integer, String>(maps[0].size());
            int i=0;
            for (Map.Entry<Integer,String> entry: maps[0].entrySet()) {
                int id = entry.getKey();
                String oldUrl = entry.getValue();

                String shortUrl = UrlHelper.shortenUrl(oldUrl);
                res.put(id,shortUrl);
                i++;
                publishProgress(i);
            }

            return res;
        }

        protected void onPreExecute() {
            dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.show();
        }

        protected void onPostExecute(Map<Integer, String> integerStringMap) {
            if (dialog!=null)
                dialog.cancel();
        }

        protected void onProgressUpdate(Integer... values) {
            int val = values[0]*10000/num;
            dialog.setProgress(val);
        }
    }
}
