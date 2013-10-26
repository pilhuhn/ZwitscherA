package de.bsd.zwitscher;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.helper.PicHelper;
import twitter4j.User;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Show details about a user and allow to (un)follow
 * the user or add it to a list.
 *
 * @author Heiko W. Rupp
 */
public class UserDetailActivity extends Activity  {

    Bundle bundle;
    TwitterHelper twitterHelper;
    ProgressBar pg;
    User theUser;
    boolean weAreFollowing = false;
    Button followButton ;
    private long userId;
    private Account account;
    private MenuItem weAreFollowingMenuItem;
    private String userName;
    List<String> userListNames ;
    boolean listsLoaded = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.user_detail);
        followButton = (Button) findViewById(R.id.userDetail_follow_button);
        if (followButton!=null)
            followButton.setEnabled(false);

        account = AccountHolder.getInstance(this).getAccount();
        twitterHelper = new TwitterHelper(this, account);

        bundle = getIntent().getExtras();
        if (bundle!=null) {
            userName = bundle.getString("userName");
            userId = bundle.getLong("userId");
            TextView userNameView = (TextView) findViewById(R.id.UserName);
            userNameView.setText(userName);
        }

        // If the user is in the DB, show the saved state while reloading its data
        if (userId!=0) {
            theUser = twitterHelper.getUserById(userId,true);
            if (theUser!=null)
                fillDetails(theUser,false);
            reload();
        }
        else {
            reload();
        }

        // We don't need to show the "<-- touch" comment any more
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("newUser",false).commit();
    }

    public void onResume() {
        super.onResume();

        userName = bundle.getString("userName");

    }

    private void reload() {
        if (userId!=0)
            new UserDetailDownloadTask(this).execute(userId);
        else {
            new UserDetailDownloadTask(this).execute(this.userName);
        }
    }

    /**
     * Fill data of the passed user in the form fields.
     * @param user User object to display
     * @param weAreFollowing True if we are following that user
     */
    private void fillDetails(User user, boolean weAreFollowing) {
        if (user == null) {
            return;
        }
        TextView userNameView = (TextView) findViewById(R.id.UserName);
        String uName = "<b>" + user.getName() + "</b>" + " (" + user.getScreenName() + ")";
        userNameView.setText(Html.fromHtml(uName));
        userId = user.getId();
        View usersTweetsButton = findViewById(R.id.view_users_tweets_button);
        if (usersTweetsButton!=null)
            usersTweetsButton.setEnabled(true);
        View viewOnWebButton = findViewById(R.id.view_user_on_web_button);
        if (viewOnWebButton!=null)
            viewOnWebButton.setEnabled(true);

        String colorString = user.getProfileBackgroundColor();
        if (colorString==null || colorString.equals("")) {
            colorString = "#EFEFEF";
        }
        if (!colorString.startsWith("#"))  // identi.ca sends the # , but twitter does not
            colorString = "#" + colorString;
        getWindow().setTitleColor(Color.parseColor(colorString));
        String textColorString = user.getProfileTextColor();
        int textColor = 0x555555;
            if (textColorString!= null) {
                if ( textColorString.equals(""))
                    textColorString = colorString;

                if (!textColorString.startsWith("#"))
                    textColorString = "#" + textColorString;
            textColor = Color.parseColor(textColorString);
        }

        userNameView.setTextColor(textColor);

        TableLayout tl = (TableLayout) findViewById(R.id.user_table_layout);
        for (int i = 0 ; i < tl.getChildCount(); i++) {
            TableRow row = (TableRow) tl.getChildAt(i);
            for (int j = 0 ; j < row.getChildCount(); j++) {
                TextView tv = (TextView) row.getChildAt(j);
                tv.setTextColor(textColor);
            }
        }
        String backgroundColorString = user.getProfileSidebarFillColor();
        if (!backgroundColorString.equals("")) {
            if (!backgroundColorString.startsWith("#"))
                backgroundColorString = "#" + backgroundColorString;
            int backgroundColor = Color.parseColor(backgroundColorString);
            tl.setBackgroundColor(backgroundColor);
            userNameView.setBackgroundColor(backgroundColor);
        }

        PicHelper picHelper = new PicHelper();
        Bitmap bitmap;
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
        if (user.getURL()!=null) {
            webView.setText(user.getURL());
            String plc = user.getProfileBackgroundColor();
            if (!plc.equals("")) {
                if (!plc.startsWith("#"))
                    plc = "#" + plc;
                webView.setTextColor(Color.parseColor(plc));
            }
        }

        TextView tweetView = (TextView) findViewById(R.id.userDetail_tweetCount);
        tweetView.setText("" + user.getStatusesCount());

        TextView followersView = (TextView) findViewById(R.id.userDetail_followerCount);
        followersView.setText("" + user.getFollowersCount());

        TextView followingView = (TextView) findViewById(R.id.userDetail_followingCount);
        followingView.setText("" + user.getFriendsCount());

        TextView listedView = (TextView) findViewById(R.id.userDetail_listedCount);
        listedView.setText("" + user.getListedCount());


        if (followButton!=null) {
            followButton.setEnabled(true);
            setFollowingButton(weAreFollowing);
        }
        ImageButton addToListButton = (ImageButton) findViewById(R.id.userDetail_addListButton);
        if (addToListButton!=null)
            addToListButton.setEnabled(true);
    }

    /**
     * Set the appropriate text on the follow button
     * @param weAreFollowing Are we following that user (show 'unfollow' message in this case).
     */
    private void setFollowingButton(boolean weAreFollowing) {
        if (weAreFollowing)
            followButton.setText(R.string.unfollow_user);
        else
            followButton.setText(R.string.follow_user);
    }

    /**
     * Called from the back button to finish the activity
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void done(View v) {
        finish();
    }

   /**
    * Allow sending a direct message to the user.
    * Called from the direct button.
    * TODO check if he follows us and thus sending is possible at all.
    * @param v View object touched
    */
    @SuppressWarnings("unused")
    public void directMessage(View v) {
       Intent i = new Intent(this, NewTweetActivity.class);
       i.putExtra("user",theUser);
       i.putExtra("op", getString(R.string.direct));
       startActivity(i);

    }
    /**
     * Called from the followUser button
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void followUser(View v) {

        UpdateRequest request = new UpdateRequest(UpdateType.FOLLOW_UNFOLLOW);
        request.userId = theUser.getId();
        request.someBool = !weAreFollowing;
        UpdateStatusService.sendUpdate(this,account,request);
    }


    /**
     * Start a browser to view user on server
     * Triggered from a button
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void viewOnWeb(View v) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        String u = "http://twitter.com/#!/" + theUser.getScreenName();
        i.setData(Uri.parse(u));
        startActivity(i);
    }


    /**
     * View users's recent tweets
     * Triggered from a button
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void showUserTweets(View v) {


        Intent intent = new Intent().setClass(this, TweetListActivity.class);
        intent.putExtra("userId",userId);

        startActivity(intent);

    }

    /**
     * Add the user to a list
     * Called from the addToList button
     * @param v View object touched
     */
    @SuppressWarnings("unused")
    public void addToList(View v) {

        TweetDB tdb = TweetDB.getInstance(this);

        List<String> data = new ArrayList<String>();
        Set<Map.Entry<String,Integer>> userListsEntries;

        int i = 0;
        userListsEntries = tdb.getOwnedLists(account).entrySet();
        boolean[] checked = new boolean[userListsEntries.size()];

        for (Map.Entry<String,Integer> userList : userListsEntries) {
            data.add(userList.getKey());
            boolean found = false;
            for (String listName : userListNames) {
                if (listName.equals(userList.getKey())) {
                    found = true;
                }
            }
            checked[i] = found;
            i++;
        }

        Intent intent = new Intent(this,MultiSelectListActivity.class);
        intent.putStringArrayListExtra("data", (ArrayList<String>) data);
        // put enabled items separately
        intent.putExtra("checked", checked);
        intent.putExtra("mode","multiple");

        startActivityForResult(intent, 1);
    }

    /**
     * Callback that is called after the user has selected a list. We need to go thhrough
     * the result and check if an item was added or removed and then fire the respective
     * task with the server.
     * @param requestCode selection code of the original event
     * @param resultCode result (ok, not ok)
     * @param data Data from the called Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==1 && resultCode==RESULT_OK) {
            long[] checkedOnes =  data.getLongArrayExtra("data");

            TweetDB tdb = TweetDB.getInstance(getApplicationContext());
            Set<Map.Entry<String,Integer>> userListsEntries;

            userListsEntries = tdb.getOwnedLists(account).entrySet();
            int i = 0;
            for (Map.Entry<String,Integer> userList : userListsEntries) {
                String listName = userList.getKey();
                if (userListNames.contains(listName)) { // the stored lists contains the above userList
                    if (!isChecked(checkedOnes,i)) {
                        // not checked -> user wants to remove the list
                        UpdateRequest request = new UpdateRequest(UpdateType.REMOVE_FROM_LIST);
                        request.id = userList.getValue();
                        request.userId=theUser.getId();
                        UpdateStatusService.sendUpdate(this,account,request);
                    }

                } else {
                    if (isChecked(checkedOnes,i)) {
                        UpdateRequest request = new UpdateRequest(UpdateType.ADD_TO_LIST);
                        request.id=userList.getValue();
                        request.userId=theUser.getId();
                        UpdateStatusService.sendUpdate(this,account,request);
                    }
                }
                i++;
            }
        }
    }

    private boolean isChecked(long[] checkedOnes, int pos) {
        for (long tmp : checkedOnes) {
            if (tmp==pos)
                return true;
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_detail_menu,menu);

        weAreFollowingMenuItem = menu.findItem(R.id.follow);
        if (weAreFollowing)
            weAreFollowingMenuItem.setTitle(R.string.unfollow_user);
        else
            weAreFollowingMenuItem.setTitle(R.string.follow_user);

        ActionBar actionBar = this.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        MenuItem pgItem  = menu.findItem(R.id.ProgressBar);
        pg = (ProgressBar) pgItem.getActionView();
        pg.setVisibility(ProgressBar.INVISIBLE);

        MenuItem listsItem = menu.findItem(R.id.addToList);
        listsItem.setEnabled(listsLoaded);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.follow:
                followUser(null);
                break;
            case R.id.addToList:
                addToList(null);
                break;
            case R.id.direct:
                directMessage(null);
                break;
            case R.id.viewOnWeb:
                viewOnWeb(null);
                break;
            case R.id.usersTweets:
                showUserTweets(null);
                break;
            case R.id.refresh:
                reload();
                break;

            default:
                Log.e("UserDetailActivity", "Unknown menu item: " + item.toString());

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Async task to download the userdata from server (or db) and
     * trigger its display.
     */
    private class UserDetailDownloadTask extends AsyncTask<Object,Void, Object[]> {

        private Context context;

        private UserDetailDownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (weAreFollowingMenuItem!=null) {
                weAreFollowingMenuItem.setEnabled(false);
            }
            if (pg!=null) {
                pg.setVisibility(ProgressBar.VISIBLE);

            }
            getActionBar().setTitle(R.string.get_user_detail);
        }


        @Override
        protected Object[] doInBackground(Object... params) {

            Long userId;
            User user;

            if (params[0] instanceof Long) {
                userId = (Long) params[0];
                user = twitterHelper.getUserById(userId, false);
            }
            else if (params[0] instanceof String) {
                String name= (String) params[0];
                user = twitterHelper.getUserByScreenName(name, false);
                if (user==null)
                    return new Object[]{};
                userId = user.getId();
            } else {
                // Should not happen
                return new Object[]{};
            }

            boolean downloadImages = new NetworkHelper(context).mayDownloadImages();
            Drawable background = null;
            if (downloadImages) {
                String profileBackgroundImageUrl = user.getProfileBackgroundImageURL();
                if (!"".equals(profileBackgroundImageUrl)) {
                    try {
                        URL url = new URL(profileBackgroundImageUrl);
                        InputStream is = url.openStream();
                        background = Drawable.createFromStream(is,"lala");

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (OutOfMemoryError oome) {
                        oome.printStackTrace();
                    }
                }
                PicHelper picHelper = new PicHelper(); // TODO optimize
                picHelper.fetchUserPic(user);

            }

            userListNames = twitterHelper.getListMembershipFromServer(user.getScreenName());

            Boolean isFriend = twitterHelper.areWeFollowing(userId);
            weAreFollowing = isFriend;
            Object[] res = new Object[3];
            res[0] = user;
            res[1] = isFriend;
            res[2] = background;
            return res;
        }


        @Override
        protected void onPostExecute(Object[] params) {
            super.onPostExecute(params);

            if (params==null || params.length==0 || params[0]==null) {
                Toast.makeText(context,R.string.failed_to_load_user_info,Toast.LENGTH_LONG).show();
                return;
            }

            User user = (User) params[0];
            Boolean isFriend = (Boolean) params[1];
            theUser = user;
            fillDetails(user, isFriend);
            View followButton = findViewById(R.id.userDetail_follow_button);
            if (followButton!=null)
                followButton.setEnabled(true);
                if (weAreFollowingMenuItem!=null) {
                    if (isFriend)
                        weAreFollowingMenuItem.setTitle(R.string.unfollow_user);
                    else
                        weAreFollowingMenuItem.setTitle(R.string.follow_user);
                    weAreFollowingMenuItem.setEnabled(true);
                }

            if (pg!=null)
                pg.setVisibility(ProgressBar.INVISIBLE);
            getActionBar().setTitle(R.string.app_name);

            Drawable background = (Drawable) params[2];
            if (background!=null)
                getWindow().setBackgroundDrawable(background);

            listsLoaded = true;
            invalidateOptionsMenu();
        }
    }
}