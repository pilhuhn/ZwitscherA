package de.bsd.zwitscher;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.ProgressBar;
import de.bsd.zwitscher.helper.PicHelper;
import twitter4j.User;

/**
 * Background task to download the user profile images.
 */
public class DownloadUserImageTask extends AsyncTask<User, Void,Bitmap> {

    private boolean downloadPictures;
    ImageView imageView;

    public DownloadUserImageTask(ImageView imageView,boolean downloadPictures) {
        this.downloadPictures = downloadPictures;
        this.imageView = imageView;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
//        oneTweetActivity.pg.setVisibility(ProgressBar.VISIBLE); // TODO
    }

    protected Bitmap doInBackground(User... users) {

        User user = users[0];
        PicHelper picHelper = new PicHelper();
        Bitmap bi;
        if (downloadPictures)
            bi = picHelper.fetchUserPic(user);
        else
            bi = picHelper.getBitMapForUserFromFile(user);
        return bi;
    }


    protected void onPostExecute(Bitmap result) {
        if (result!=null)
            imageView.setImageBitmap(result);
//        oneTweetActivity.pg.setVisibility(ProgressBar.INVISIBLE); // TODO
    }
}
