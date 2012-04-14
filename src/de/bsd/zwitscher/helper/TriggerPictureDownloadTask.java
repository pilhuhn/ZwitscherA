package de.bsd.zwitscher.helper;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import twitter4j.User;

/**
* Download pictures of the passed {@link twitter4j.User}s in background.
*
* @author Heiko W. Rupp
*/
public class TriggerPictureDownloadTask extends AsyncTask<Void,Void,Bitmap> {

    private UserImageView userImageView;
    private User user;
    private final boolean downloadImages;
    private final twitter4j.Status status;
    private PicHelper picHelper;
    private Bitmap imageBitmap;
    private boolean alreadyLoaded;
    private boolean rtUserLoaded;

    public TriggerPictureDownloadTask(UserImageView userImageView, User user, boolean downloadImages, twitter4j.Status status) {
        this.userImageView = userImageView;
        this.user = user;
        this.downloadImages = downloadImages;
        this.status = status;
        picHelper = new PicHelper();
        alreadyLoaded=false;
        rtUserLoaded =true;
    }

    @Override
    protected void onPreExecute() {
        imageBitmap = picHelper.getBitMapForScreenNameFromFile(user.getScreenName());
        userImageView.setImageBitmap(imageBitmap);
        userImageView.setRtImage(null);

        if (imageBitmap!=null)
            alreadyLoaded=true;

        if (status!=null) {
            userImageView.markFavorite(status.isFavorited());
            userImageView.markRetweet(status.isRetweet());
            if (status.isRetweet()) {
                Bitmap rtbm = picHelper.getBitMapForUserFromFile(status.getUser());
                if (rtbm==null) {
                    rtUserLoaded=false;
                }
                userImageView.setRtImage(rtbm);
            }
            else {
                userImageView.setRtImage(null);
            }
        }
    }

    @Override
    protected Bitmap doInBackground(Void... aVoid) {
        if (!alreadyLoaded) {
            if (imageBitmap==null && downloadImages)
                imageBitmap = picHelper.fetchUserPic(user);
        }

        if (status.isRetweet() && !rtUserLoaded && downloadImages) {
            picHelper.fetchUserPic(status.getUser());
        }

        return imageBitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {

        if (!alreadyLoaded) {
            userImageView.setImageBitmap(bitmap);
        }

        if (status!=null) {
            userImageView.markFavorite(status.isFavorited());
            userImageView.markRetweet(status.isRetweet());
            if (status.isRetweet()) {
                Bitmap rtbm = picHelper.getBitMapForUserFromFile(status.getUser());
                userImageView.setRtImage(rtbm);
            }
            else
                userImageView.setRtImage(null);
        }
    }
}
