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

		URL imageUrl = user.getProfileImageURL();

		String username = user.getScreenName();
        Log.i("getUserPic","Looking for pic of user '" + username + "' from '" + imageUrl.toString() + "'");
		boolean found = false;
		// TODO use v8 methods when we require v8 in the manifest. Is probably too early yet.
		String externalStorageState = Environment.getExternalStorageState();
		try {
			if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
				File iconFile = getPictureFileForUser(username);
				if (iconFile.exists() && iconFile.lastModified() > System.currentTimeMillis() - ONE_DAY)
					found = true;
			} 
			if (found)
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
    				File iconFile = getPictureFileForUser(username);
    				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(iconFile));
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
				File iconFile = getPictureFileForUser(username);
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

	private File getPictureFileForUser(String username) {
		File baseDir = Environment.getExternalStorageDirectory();
		File iconDir = new File(baseDir,"/Android/data/de.bsd.zwitscher/files/user_profiles");
		if (!iconDir.exists())
			iconDir.mkdirs();
		File iconFile = new File(iconDir,username);
		return iconFile;
	}
}
