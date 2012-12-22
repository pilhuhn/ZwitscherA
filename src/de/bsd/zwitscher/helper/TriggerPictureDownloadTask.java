package de.bsd.zwitscher.helper;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Pair;
import twitter4j.User;

/**
* Download pictures of the passed {@link twitter4j.User}s in background.
*
* @author Heiko W. Rupp
*/
public class TriggerPictureDownloadTask extends AsyncTask<Void,Void,Pair<Bitmap,Bitmap>> {

    private UserImageView userImageView;
    private User user;
    private final boolean downloadImages;
    private final twitter4j.Status status;
    private PicHelper picHelper;

    public TriggerPictureDownloadTask(UserImageView userImageView, User user, boolean downloadImages, twitter4j.Status status) {
        this.userImageView = userImageView;
        this.user = user;
        this.downloadImages = downloadImages;
        this.status = status;
        picHelper = new PicHelper();
    }


    @Override
    protected void onPreExecute() {

        // If the image is already on file, use it, otherwise set the image to null.
        // This
        // set the "unknown" image on that one. But not on all images unconditionally
        userImageView.setImageBitmap(picHelper.getBitMapForScreenNameFromFile(user.getScreenName()));
    }

    @Override
    protected Pair<Bitmap,Bitmap> doInBackground(Void... aVoid) {

        // main user image
        Bitmap imageBitmap = picHelper.getBitMapForScreenNameFromFile(user.getScreenName());
        if (imageBitmap==null && downloadImages) {
            imageBitmap = picHelper.fetchUserPic(user);
        }

        if (status == null) { // E.g. direct message
            return new Pair<Bitmap,Bitmap>(imageBitmap,null);
        }

        // Image of the user of the retweeted status
        Bitmap rtBitmap = null;
        if (status.isRetweet()) {
            rtBitmap = picHelper.getBitMapForScreenNameFromFile(status.getUser().getScreenName());

            if (rtBitmap==null && downloadImages) {
                rtBitmap = picHelper.fetchUserPic(status.getUser());
            }
        }

        return new Pair<Bitmap,Bitmap>(imageBitmap,rtBitmap);
    }

    @Override
    protected void onPostExecute(Pair<Bitmap,Bitmap> bitmaps) {

        if (userImageView.getTag().equals(user.getScreenName())) {
            userImageView.setImageBitmap(bitmaps.first);

            userImageView.setRtImage(bitmaps.second);

            if (status!=null) {
                userImageView.markFavorite(status.isFavorited());
                userImageView.markRetweet(status.isRetweet());
            }

            userImageView.invalidate(); // Make the view draw itself
        }
    }
}
