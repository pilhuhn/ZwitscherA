package de.bsd.zwitscher;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import twitter4j.User;


/**
 * TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class UserDetailActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.user_detail);

        Bundle bundle = getIntent().getExtras();
        if (bundle!=null) {
            String userName = bundle.getString("userName");
            int userId = bundle.getInt("userId");
            TextView userNameView = (TextView) findViewById(R.id.UserName);
            userNameView.setText(userName);

            TwitterHelper thTwitterHelper = new TwitterHelper(this);
            User user = thTwitterHelper.getUserById(userId);

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
            TextView tweetView = (TextView) findViewById(R.id.userDetail_tweetCount);
            tweetView.setText("x "+user.getStatusesCount());

            TextView followersView = (TextView) findViewById(R.id.userDetail_followerCount);
            followersView.setText(""+user.getFollowersCount());

            TextView followingView = (TextView) findViewById(R.id.userDetail_followingCount);
            followingView.setText(""+user.getFriendsCount()); // TODO right data ?

            TextView listedView = (TextView) findViewById(R.id.userDetail_listedCount);
            listedView.setText(""+user.getListedCount());
        }
    }
}