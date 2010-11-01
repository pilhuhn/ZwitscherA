package de.bsd.zwitscher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.MalformedInputException;

import android.graphics.*;
import twitter4j.User;
import android.content.Context;
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
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                bitmap = biteCornersOff(bitmap);

    			if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
    				File iconFile = getPictureFileForUser(username);
    				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(iconFile));
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, out);
                    out.flush();
                    out.close();
    			}
                in.close();

			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
        return getBitMapForUserFromFile(user);
	}

    /**
     * Take a square bitmap as input and bite the four corners off -
     * This means those pixels are set to Color.TRANSPARENT
     * @param bitmap Original bitmap
     * @return modified bitmap
     */
    private Bitmap biteCornersOff(final Bitmap bitmap) {
        int transparent = Color.TRANSPARENT;
        Bitmap out = bitmap.copy(bitmap.getConfig(), true);
        out.setPixel(0,0,transparent);
        out.setPixel(1,0,transparent);
        out.setPixel(0,1,transparent);
        out.setPixel(2,0,transparent);
        out.setPixel(1,1,transparent);
        out.setPixel(0,2,transparent);



        int mx=bitmap.getWidth()-1;
        int my=bitmap.getHeight()-1;

        out.setPixel(mx,my,transparent);
        out.setPixel(mx-1,my,transparent);
        out.setPixel(mx, my - 1, transparent);
        out.setPixel(mx-2,my,transparent);
        out.setPixel(mx-1,my-1,transparent);
        out.setPixel(mx, my - 2, transparent);


        out.setPixel(mx-1,0, transparent);
        out.setPixel(mx,0, transparent);
        out.setPixel(mx,1, transparent);
        out.setPixel(mx-2,0, transparent);
        out.setPixel(mx-1,1, transparent);
        out.setPixel(mx,2, transparent);

        out.setPixel(0,my,transparent);
        out.setPixel(1,my,transparent);
        out.setPixel(0,my-1,transparent);
        out.setPixel(2,my,transparent);
        out.setPixel(1,my-1,transparent);
        out.setPixel(0,my-2,transparent);

        return out;
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

    /**
     * Put decorators (=little images) for favorites and retweets on the passed bitmap
     * @param in bitmap to decorate
     * @param context Context object
     * @param isFav Should a favorite decorator be painted?
     * @param isRt Shout the retweet decorator be painted?
     * @return The decorated bitmap or the original one.
     */
    public Bitmap decorate(final Bitmap in, Context context , boolean isFav, boolean isRt) {


        Bitmap out;
        if (isFav || isRt) {
            out = in.copy(in.getConfig(), true);
            Canvas canvas = new Canvas(out);

            if (isFav) {
                Bitmap favMap = BitmapFactory.decodeResource(context.getResources(),R.drawable.yellow_f);
                canvas.drawBitmap(favMap,new Matrix(),null);
            }
            if (isRt) {
                Bitmap rtMap = BitmapFactory.decodeResource(context.getResources(),R.drawable.green_r);
                float f = out.getWidth() - rtMap.getWidth();
                canvas.drawBitmap(rtMap,f,f,null);
            }
        }
        else {
            out = in;
        }


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
