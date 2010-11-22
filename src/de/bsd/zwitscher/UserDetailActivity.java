package de.bsd.zwitscher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.helper.PicHelper;
import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.UserList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Show details about a user and allow to follow
 *
 * @author Heiko W. Rupp
 */
public class UserDetailActivity extends Activity  {

    Bundle bundle;
    TwitterHelper thTwitterHelper;
    ProgressBar pg;
    TextView titleTextBox;
    User theUser;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.user_detail);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
        pg = (ProgressBar) findViewById(R.id.title_progress_bar);
        titleTextBox = (TextView) findViewById(R.id.title_msg_box);
        Button followButton = (Button) findViewById(R.id.userDetail_follow_button);
        followButton.setEnabled(false);


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

            Button followButton = (Button) findViewById(R.id.userDetail_follow_button);
            followButton.setEnabled(true);
            if (user.isFollowRequestSent())
                followButton.setText(R.string.unfollow_user);
            else
                followButton.setText(R.string.follow_user);
            Button addToListButton = (Button) findViewById(R.id.userDetail_addListButton);
            addToListButton.setEnabled(true);
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
     * Called from the followUser button
     * @param v
     */
    @SuppressWarnings("unused")
    public void followUser(View v) {

        thTwitterHelper.followUnfollowUser(theUser.getId(),!theUser.isFollowRequestSent());

    }


    /**
     * Called from the addToList button
     * @param v
     */
    @SuppressWarnings("unused")
    public void addToList(View v) {

        TweetDB tdb = new TweetDB(this);

        List<String> data = new ArrayList<String>();
        Set<Map.Entry<String, Integer>> userListsEntries;

        userListsEntries = tdb.getLists().entrySet();
        for (Map.Entry<String, Integer> userList : userListsEntries) {
            data.add(userList.getKey());
        }

        Intent intent = new Intent(this,MultiSelectListActivity.class);
        intent.putStringArrayListExtra("data", (ArrayList<String>) data);
        intent.putExtra("mode","single");

        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==1 && resultCode==RESULT_OK) {
            Object o = data.getExtras().get("data");
            System.out.println("res : " + o);


            TweetDB tdb = new TweetDB(this);
            Set<Map.Entry<String, Integer>> userListsEntries;

            int listId =-1;
            userListsEntries = tdb.getLists().entrySet();
            for (Map.Entry<String, Integer> userList : userListsEntries) {
                if (userList.getKey().equals((String)o)) {
                    listId = userList.getValue();
                    thTwitterHelper.addUserToLists(theUser.getId(),listId);
                }
            }
        }
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
            String s = getString(R.string.get_user_detail);
            titleTextBox.setText(s);
        }


        @Override
        protected User doInBackground(Integer... params) {

            Integer userId = params[0];
            User user = thTwitterHelper.getUserById(userId, false);
            return user;
        }


        @Override
        protected void onPostExecute(User user) {
            super.onPostExecute(user);
            theUser = user;
            fillDetails(user);
            pg.setVisibility(ProgressBar.INVISIBLE);
            titleTextBox.setText("");
        }
    }
}