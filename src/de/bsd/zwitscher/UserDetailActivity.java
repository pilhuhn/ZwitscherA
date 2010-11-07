package de.bsd.zwitscher;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;
import twitter4j.User;

import javax.xml.soap.Text;


/**
 * TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class UserDetailActivity extends Activity {

    Bundle bundle;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.user_detail);

        bundle = getIntent().getExtras();
        if (bundle!=null) {
            String userName = bundle.getString("userName");
            TextView userNameView = (TextView) findViewById(R.id.UserName);
            userNameView.setText(userName);
        }
    }

    public void onResume() {
        super.onResume();


        TwitterHelper thTwitterHelper = new TwitterHelper(this);
        int userId = bundle.getInt("userId");

        User user = thTwitterHelper.getUserById(userId);
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
        webView.setText(user.getURL().toString());

        TextView tweetView = (TextView) findViewById(R.id.userDetail_tweetCount);
        tweetView.setText(""+user.getStatusesCount());

        TextView followersView = (TextView) findViewById(R.id.userDetail_followerCount);
        followersView.setText(""+user.getFollowersCount());

        TextView followingView = (TextView) findViewById(R.id.userDetail_followingCount);
        followingView.setText(""+user.getFriendsCount()); // TODO right data ?

        TextView listedView = (TextView) findViewById(R.id.userDetail_listedCount);
        listedView.setText(""+user.getListedCount());
    }
}