package de.bsd.zwitscher.helper;

import android.os.AsyncTask;
import twitter4j.User;

/**
* Download pictures of the passed {@link twitter4j.User}s in background.
*
* @author Heiko W. Rupp
*/
public class TriggerPictureDownloadTask extends AsyncTask<User,Void,Void> {

    @Override
    protected Void doInBackground(User... users) {
        PicHelper ph = new PicHelper();
        for (User user : users) {
            ph.fetchUserPic(user);
        }
        return null;
    }
}
