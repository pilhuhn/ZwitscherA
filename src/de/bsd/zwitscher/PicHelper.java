package de.bsd.zwitscher;

import java.net.URL;

import twitter4j.User;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

public class PicHelper {

	public Bitmap getUserPic(User user) {
		
		// TODO optimize see e.g. http://www.androidpeople.com/android-load-image-from-url-example/#comment-388

		URL imageUrl = user.getProfileImageURL();
		
		Log.i("show image",imageUrl.toString());
		try {
			Bitmap bi = BitmapFactory.decodeStream(imageUrl.openConnection() .getInputStream());
			return bi;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
