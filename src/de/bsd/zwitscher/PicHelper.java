package de.bsd.zwitscher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import android.graphics.Color;
import twitter4j.User;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class PicHelper {

    String externalStorageState;

    public PicHelper() {
        externalStorageState = Environment.getExternalStorageState();
    }


	private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

	public Bitmap getUserPic(User user,Context context) {

		URL imageUrl = user.getProfileImageURL();

		String username = user.getScreenName();
        Log.i("getUserPic","Looking for pic of user '" + username + "' from '" + imageUrl.toString() + "'");
		boolean found = false;
		// TODO use v8 methods when we require v8 in the manifest. Is probably too early yet.
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
        return getBitMapForUserFromFile(user);
	}

    public Bitmap getBitMapForUserFromFile(User user) {
        String username = user.getScreenName();
        Log.d("getBitMapForUserFromFile","user = " +username);
        try {
            if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
                File iconFile = getPictureFileForUser(username);
                FileInputStream fis = new FileInputStream(iconFile);
                Bitmap bi = BitmapFactory.decodeStream(fis);
                fis.close();
                return bi;
            }
        }
        catch (Exception e) {
            Log.w("getBitMapForUserFromFile", "Picture for " + username + " not found: " + e.getMessage());
            return null;
        }
        return null;
    }

    public Bitmap decorate(final Bitmap in, boolean isFav, boolean isRt) {
        int transparent = Color.TRANSPARENT;
        Bitmap out = in.copy(in.getConfig(),true);
        out.setPixel(0,0,transparent);
        out.setPixel(1,0,transparent);
        out.setPixel(0,1,transparent);
        out.setPixel(2,0,transparent);
        out.setPixel(1,1,transparent);
        out.setPixel(0,2,transparent);



        int mx=in.getWidth()-1;
        int my=in.getHeight()-1;

        out.setPixel(mx,my,transparent);
        out.setPixel(mx-1,my,transparent);
        out.setPixel(mx, my - 1, transparent);
        out.setPixel(mx-2,my,transparent);
        out.setPixel(mx-1,my-1,transparent);
        out.setPixel(mx, my - 2, transparent);

        int color;
        if (isFav)
            color = Color.GREEN;
        else
            color = Color.TRANSPARENT;
        out.setPixel(mx-1,0, color);
        out.setPixel(mx,0, color);
        out.setPixel(mx,1, color);
        out.setPixel(mx-2,0, color);
        out.setPixel(mx-1,1, color);
        out.setPixel(mx,2, color);

        if (isRt)
            color = Color.YELLOW;
        else
            color = Color.TRANSPARENT;

        out.setPixel(0,my,color);
        out.setPixel(1,my,color);
        out.setPixel(0,my-1,color);
        out.setPixel(2,my,color);
        out.setPixel(1,my-1,color);
        out.setPixel(0,my-2,color);

        return out;
    }


    private File getPictureFileForUser(String username) {
		File baseDir = Environment.getExternalStorageDirectory();
		File iconDir = new File(baseDir,"/Android/data/de.bsd.zwitscher/files/user_profiles");
		if (!iconDir.exists())
			iconDir.mkdirs();
		File iconFile = new File(iconDir,username);
		return iconFile;
	}

    public void cleanup() {
        File baseDir = Environment.getExternalStorageDirectory();
        File iconDir = new File(baseDir,"/Android/data/de.bsd.zwitscher/files/user_profiles");

        File[] files = iconDir.listFiles();
        for (File file : files) {
            file.delete();
        }
    }
}
