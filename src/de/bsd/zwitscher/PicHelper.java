package de.bsd.zwitscher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import twitter4j.User;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class PicHelper {

	private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

	public Bitmap getUserPic(User user,Context context) {

		// TODO optimize see e.g. http://www.androidpeople.com/android-load-image-from-url-example/#comment-388

		URL imageUrl = user.getProfileImageURL();

		String username = user.getScreenName();
        Log.i("getUserPic","Looking for pic of user '" + username + "' from '" + imageUrl.toString() + "'");
		boolean found = false;
		String externalStorageState = Environment.getExternalStorageState();
		try {
			if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
				File baseDir = Environment.getExternalStorageDirectory();
				File iconDir = new File(baseDir,"/Android/data/de.bsd.zwitscher/files/user_profiles");
				if (!iconDir.exists())
					iconDir.mkdirs();
				File iconFile = new File(iconDir,username);
				if (iconFile.exists() && iconFile.lastModified() > System.currentTimeMillis() - ONE_DAY)
					found = true;
			} 
			
//			FileInputStream fis = context.openFileInput(username);
//			found = true;
//			fis.close();
            Log.i("getUserPic","Picture was on file system");
		}
		catch (Exception ioe) {
			Log.i("PicHelper", ioe.getMessage());
		}

		if (!found) {
			try {
                Log.i("getUserPic","Downloading image and persisting it locally");
                BufferedInputStream in = new BufferedInputStream(imageUrl.openStream());
    			if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
    				File baseDir = Environment.getExternalStorageDirectory();
    				File iconDir = new File(baseDir,"/Android/data/de.bsd.zwitscher/files/user_profiles");
    				if (!iconDir.exists())
    					iconDir.mkdirs();
    				File iconFile = new File(iconDir,username);
    				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(iconFile));
//                BufferedOutputStream out = new BufferedOutputStream(context.openFileOutput(username,Context.MODE_WORLD_READABLE));
                int val;
                while ((val = in.read()) > -1)
                    out.write(val);
                out.flush();
                out.close();
                in.close();
    			}
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}


		try {
            Log.i("getUserPic","creating bitmap");
			if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
				File baseDir = Environment.getExternalStorageDirectory();
				File iconDir = new File(baseDir,"/Android/data/de.bsd.zwitscher/files/user_profiles");
				File iconFile = new File(iconDir,username);
				FileInputStream fis = new FileInputStream(iconFile);
			Bitmap bi = BitmapFactory.decodeStream(fis);
			fis.close();
			return bi;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
