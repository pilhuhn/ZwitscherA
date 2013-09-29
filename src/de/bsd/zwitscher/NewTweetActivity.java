package de.bsd.zwitscher;


import java.io.File;
import java.io.IOException;
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
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.*;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.helper.UserTagTokenizer;
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

/**
 * This activity is called when the user wants to write a new tweet/dent.
 * This can be for a new message, a new direct message or also when replying
 * to an existing message.
 */
public class NewTweetActivity extends Activity implements LocationListener {

    private MultiAutoCompleteTextView edittext;
	private Status origStatus;
    private ProgressBar pg;
    private User toUser = null;
    private TextView charCountView;
    private String picturePath = null;
    private boolean pictureRemovable = false;
    private LocationManager locationManager;
    private Account account;
    private int picUrlCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.new_tweet);

        charCountView = (TextView) findViewById(R.id.CharCount);
        account = AccountHolder.getInstance(this).getAccount();

        final ImageButton tweetButton = (ImageButton) findViewById(R.id.TweetButton);
        edittext = (MultiAutoCompleteTextView) findViewById(R.id.edittext);
        edittext.setSelected(true);
        // Set the MultiAutoCompleteTextView in a mode that allows tokenizing and
        // also the normal spell checker.
        // See http://stackoverflow.com/questions/4552292/edittext-and-multiautocompletetextview-suggestions/7761754#7761754
        edittext.setRawInputType(InputType.TYPE_CLASS_TEXT
          |InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
          |InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
          |InputType.TYPE_TEXT_FLAG_MULTI_LINE);
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

                if (origStatus!=null) {
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
                else {
                    // New tweet as reply to a direct message
                    if (op.equals(getString(R.string.direct))) {
                        if (directUser!=null)
                                                   toUser = directUser;
                        String s = getString(R.string.send_direct_to);
                                                textOben.setText(s + " "+ toUser.getScreenName());
                    }
                }
            }
            else { // OP is null -> new tweet
                if (bundleText!=null)
                    edittext.setText(bundleText);
                else if (bundle.get(Intent.EXTRA_STREAM)!=null) {
                    Uri externalUmageUri = (Uri) bundle.get(Intent.EXTRA_STREAM);
                    // THis is a content:// uri.
                    // decode the picture location
                    picturePath = getPath(externalUmageUri);
                    Toast.makeText(this,R.string.picture_attached,Toast.LENGTH_SHORT).show();
                }
            }
        }
        edittext.setSelection(edittext.getText().length());
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

                charCountView.setText(String.valueOf(140-tlen- picUrlCount)); // TODO if url detected 4 twitter, decrease by 20 chars
            }
        });

        // Now set the adapter and Tokenizer for auto-completion of @user and #hashtag
        edittext.setThreshold(2); // default seems to be 2 as well -> @ + 1 char
        Set<String> usernames = new HashSet<String>();
        usernames.addAll(getUsernames(true));
        usernames.add("#Zwitscher");
        usernames.add("#Android");
        usernames.add("#RHQ");
        usernames.add("#JBoss");
        usernames.add("#java");
        usernames.add("@pilhuhn");
        usernames.add("@RHQ_project");
        System.out.println("hashes " + AccountHolder.getInstance(this).getHashTags().size());
        System.out.println("users  " + AccountHolder.getInstance(this).getUserNames().size());
        usernames.addAll(AccountHolder.getInstance(this).getHashTags());
        usernames.addAll(AccountHolder.getInstance(this).getUserNames());
        ArrayAdapter<String> acAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,
                usernames.toArray(new String[usernames.size()]));
        edittext.setAdapter(acAdapter);
        edittext.setTokenizer(new UserTagTokenizer());

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

    @SuppressWarnings("unused")
    public void clear(View v) {
        edittext.setText("");

    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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
        Location currLoc = locMngr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (currLoc == null) {
			currLoc = locMngr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		return currLoc;
	}

	/** Extract the @users from the passed oText and put them into sb
	 * TODO optimize
     * @param sb Builder to populate
     * @param oText Input text to analyze
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
    void tweet(StatusUpdate update) {
        UpdateRequest request = new UpdateRequest(UpdateType.UPDATE);
        request.statusUpdate = update;
        if (picturePath!=null)
            request.picturePath = picturePath;
        request.someBool = pictureRemovable;
        Toast.makeText(this,R.string.trying_to_send,Toast.LENGTH_SHORT).show();
        UpdateStatusService.sendUpdate(this,account,request);
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
        request.someBool = pictureRemovable;
        Toast.makeText(this,R.string.trying_to_send,Toast.LENGTH_SHORT).show();

        UpdateStatusService.sendUpdate(this,account,request);
    }

    /**
     * Trigger a list of usernames to pick one from and to insert
     * into the tweet
     * @param v Button that was touched
     */
    @SuppressWarnings("unused")
    public void selectUser(View v) {

        List<String> data = getUsernames(false);

        Intent intent = new Intent(this,MultiSelectListActivity.class);
        intent.putStringArrayListExtra("data", (ArrayList<String>) data);
        intent.putExtra("mode","single");

        startActivityForResult(intent, 2);

    }

    private List<String> getUsernames(boolean shortForm) {
        TwitterHelper th = new TwitterHelper(this, account);
        List<User> users = th.getUsersFromDb();
        List<String> data = new ArrayList<String>(users.size());

        for (User user : users) {
            StringBuilder sb = new StringBuilder("@");
            sb.append(user.getScreenName());
            if (!shortForm) {
                sb.append(", ");
                sb.append(user.getName());
            }
            data.add(sb.toString());
        }
        return data;
    }


    /**
     * Called from the Back button to finish (abort) the activity
     * @param v Button that was touched
     */
    @SuppressWarnings("unused")
    public void done(View v) {
        finish();
    }

    /**
     * Trigger taking a picture, called from the camera button.
     * Actually we present a menu from which the user will be able to take a picture
     * or select one from the gallery of pictures taken
     * @param v button that was pressed
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
                        // small image
                        intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, 1);
                        break;
                    case 1:
                        // large image
                        Uri tmpUri = Uri.fromFile(getFixedTempFile(NewTweetActivity.this));
                        intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT,tmpUri);
                        startActivityForResult(intent, 3);

                        break;
                    case 2:
                        // from gallery
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
        pictureRemovable = false;

        // code 1 = take small picture
        if(requestCode==1  && resultCode==RESULT_OK) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            PicHelper picHelper = new PicHelper();
            picturePath = picHelper.storeBitmap(bitmap, getTempFile(this), Bitmap.CompressFormat.JPEG, 100); // TODO adjust quality per network
            Log.d("NewTweetActivity.onActivityResult","path: " + picturePath);
            pictureRemovable = true;

            if (provider.equals("twitter"))
                Toast.makeText(this,R.string.picture_attached,Toast.LENGTH_SHORT).show();
            else
                picUrlCount=21;
        } else if (requestCode==2 && resultCode==RESULT_OK) { // select user
            String item = (String) data.getExtras().get("data");
            if (item.contains(", ")) {
                String user = item.substring(0,item.indexOf(", "));
                edittext.append( user + " ");
            }
        } else if (requestCode==3 && resultCode==RESULT_OK) { // take large picture
            // large size image
            File file = getFixedTempFile(this);
            File newPath = getTempFile(this);
            boolean success = file.renameTo(newPath);
            if (success)
                picturePath = newPath.getAbsolutePath();
            else
                picturePath = file.getAbsolutePath();
            pictureRemovable = true;

            Toast.makeText(this,R.string.picture_attached,Toast.LENGTH_SHORT).show();

            if(!provider.equals("twitter"))
                picUrlCount=21;

        } else if (requestCode==4 && resultCode==RESULT_OK) { // picture from gallery
            // image from gallery
            Uri selectedImageUri = data.getData();
            picturePath = getPath(selectedImageUri);
            Toast.makeText(this,R.string.picture_attached,Toast.LENGTH_SHORT).show();

            if(!provider.equals("twitter"))
                picUrlCount=21;

        }
    }


    protected void onPause() {
        super.onPause();
        switchOffLocationUpdates();
    }

    private void switchOffLocationUpdates() {
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.new_tweet_menu,menu);

        ActionBar actionBar = this.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        return true;
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
                Log.e("NewTweetActivity","Unknown menu item: " + item.toString());

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Get a temporary file with a fixed (=known in advance) file name
     * @param context activity context
     * @return a temp file in the external storage in a package-specific directory
     */
    private File getFixedTempFile(Context context) {
        File path = new File( Environment.getExternalStorageDirectory(), context.getPackageName() );

        if(!path.exists())
            path.mkdir();

        File tempFile;
        tempFile = new File(path,"image.tmp");
        return tempFile;

    }

    /**
     * Get a temporary file with a unique file name
     * @param context activity context
     * @return a temp file in the external storage in a package-specific directory
     */
    private File getTempFile(Context context){
        File path = new File( Environment.getExternalStorageDirectory(), context.getPackageName() );

        if(!path.exists())
            path.mkdir();

        File tempFile;
        try {
            tempFile = File.createTempFile("img_", ".jpg", path);
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            tempFile = new File(path,"image.tmp");
        }
        Log.d("NewTweetActivity.getTempFile",tempFile.getAbsolutePath());
        return tempFile;
    }

    // Taken from http://stackoverflow.com/questions/2169649/open-an-image-in-androids-built-in-gallery-app-programmatically/2636538#2636538
    // In the future use something like InputStream is = getContentResolver().openInputStream(Uri.parse(YOUR_URI_STRING)));
    String getPath(Uri uri) {
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
