package de.bsd.zwitscher;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.helper.PicHelper;
import twitter4j.User;


/**
 * TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class UserDetailActivity extends Activity {

    Bundle bundle;
    TwitterHelper thTwitterHelper;
    ProgressBar pg;
    TextView titleTextBox;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.user_detail);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
        pg = (ProgressBar) findViewById(R.id.title_progress_bar);
        titleTextBox = (TextView) findViewById(R.id.title_msg_box);


        bundle = getIntent().getExtras();
        if (bundle!=null) {
            String userName = bundle.getString("userName");
            TextView userNameView = (TextView) findViewById(R.id.UserName);
            userNameView.setText(userName);
        }
    }

    public void onResume() {
        super.onResume();

        thTwitterHelper = new TwitterHelper(this);
        int userId = bundle.getInt("userId");

        new UserDetailDownloadTask().execute(userId);
    }

    /**
     * Fill data of the passwd user in the form fields.
     * @param user
     */
    private void fillDetails(User user) {
        if (user!=null) {
            TextView userNameView = (TextView) findViewById(R.id.UserName);
            String uName = "<b>" + user.getName() + "</b>" + " (" + user.getScreenName() + ")";
            userNameView.setText(Html.fromHtml(uName));

            PicHelper picHelper = new PicHelper();
            boolean downloadImages = new NetworkHelper(this).mayDownloadImages();
            Bitmap bitmap;
            if (downloadImages)
                bitmap = picHelper.fetchUserPic(user);
            else
                bitmap = picHelper.getBitMapForUserFromFile(user);

            if (bitmap!=null) {
                ImageView iv = (ImageView) findViewById(R.id.UserPictureImageView);
                iv.setImageBitmap(bitmap);
            }
            TextView locationView = (TextView) findViewById(R.id.userDetail_location);
            locationView.setText(user.getLocation());

            TextView bioView = (TextView) findViewById(R.id.userDetail_bio);
            bioView.setText(user.getDescription());

            TextView webView = (TextView) findViewById(R.id.userDetail_web);
            if (user.getURL()!=null)
                webView.setText(user.getURL().toString());

            TextView tweetView = (TextView) findViewById(R.id.userDetail_tweetCount);
            tweetView.setText(""+user.getStatusesCount());

            TextView followersView = (TextView) findViewById(R.id.userDetail_followerCount);
            followersView.setText(""+user.getFollowersCount());

            TextView followingView = (TextView) findViewById(R.id.userDetail_followingCount);
            followingView.setText(""+user.getFriendsCount());

            TextView listedView = (TextView) findViewById(R.id.userDetail_listedCount);
            listedView.setText(""+user.getListedCount());
        }
    }

    /**
     * Called from the back button to finish the activity
     * @param v
     */
    @SuppressWarnings("unused")
    public void done(View v) {
        finish();
    }

    /**
     * Async task to download the userdata from server (or db) and
     * trigger its display.
     */
    private class UserDetailDownloadTask extends AsyncTask<Integer,Void, User> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pg.setVisibility(ProgressBar.VISIBLE);
            titleTextBox.setText("Getting user details ...");
        }


        @Override
        protected User doInBackground(Integer... params) {

            Integer userId = params[0];
            User user = thTwitterHelper.getUserById(userId);
            return user;
        }


        @Override
        protected void onPostExecute(User user) {
            super.onPostExecute(user);
            fillDetails(user);
            pg.setVisibility(ProgressBar.INVISIBLE);
            titleTextBox.setText("");
        }
    }
}