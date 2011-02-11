package de.bsd.zwitscher.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import android.graphics.*;
import twitter4j.User;
import android.os.Environment;
import android.util.Log;

/**
 * Helper class that deals with handling of pictures and storing
 * them on local file system.
 */
public class PicHelper {

    static final String APP_BASE_DIR = "/Android/data/de.bsd.zwitscher/";
    String externalStorageState;

    public PicHelper() {
        externalStorageState = Environment.getExternalStorageState();
    }


	private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    /**
     * Load the user picture for the passed user.
     * If this is on file system, it is checked if it has changed on the server.
     * If it is not yet on filesystem, it is fetched from remote and stored locally.
     * After fetching the corners are bitten off
     * @param user User for which to obtain the picture
     * @return Bitmap of the picture of null if loading failed.
     */
	public Bitmap fetchUserPic(User user) {

        if (user==null)
            return null;

		URL imageUrl = user.getProfileImageURL();

		String username = user.getScreenName();
        Log.i("fetchUserPic","Looking for pic of user '" + username + "' from '" + imageUrl.toString() + "'");
		boolean found = false;
		// TODO use v8 methods when we require v8 in the manifest. Is probably too early yet.
		try {
			if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
				File iconFile = getPictureFileForUser(username);
				if (iconFile.exists() && iconFile.lastModified() > System.currentTimeMillis() - ONE_DAY)
					found = true;
			}
			if (found)
				Log.i("fetchUserPic","Picture was on file system");
		}
		catch (Exception ioe) {
			Log.i("PicHelper", ioe.getMessage());
		}

		if (!found) {
			try {
                Log.i("fetchUserPic","Downloading image and persisting it locally");
                BufferedInputStream in = new BufferedInputStream(imageUrl.openStream());
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                bitmap = biteCornersOff(bitmap);

    			if (bitmap!=null && externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
    				File iconFile = getPictureFileForUser(username);
    				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(iconFile));
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, out);
                    out.flush();
                    out.close();
    			}
            in.close();
            if (bitmap!=null)
               bitmap.recycle();
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
     * @return modified bitmap or null if original was null
     */
    private Bitmap biteCornersOff(final Bitmap bitmap) {
        int transparent = Color.TRANSPARENT;
        if (bitmap==null || bitmap.getConfig()==null) {
            Log.d("BiteCornersOff", "bitmap or config was null");
            return null;
        }

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

        bitmap.recycle();
        return out;
    }

    /**
     * Load the bitmap of the user icon for the given user
     * @param user Screen name of the user
     * @return Bitmap if present on file system or null if not found
     */
    public Bitmap getBitMapForUserFromFile(User user) {
        if (user==null)
            return null;

        String username = user.getScreenName();
        return getBitMapForScreenNameFromFile(username);
    }

    public Bitmap getBitMapForScreenNameFromFile(String username) {
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
            return null;
        }
        return null;
    }

    /**
     * Locate the file for the passed screen name
     * @param username Screen name to lookup
     * @return File object of the matching file
     */
    private File getPictureFileForUser(String username) {
		File baseDir = Environment.getExternalStorageDirectory();
		File iconDir = new File(baseDir, APP_BASE_DIR + "files/user_profiles");
		if (!iconDir.exists())
			iconDir.mkdirs();
		File iconFile = new File(iconDir,username);
		return iconFile;
	}

    /**
     * Remove the stored user pictures
     */
    public void cleanup() {
        File baseDir = Environment.getExternalStorageDirectory();
        File iconDir = new File(baseDir, APP_BASE_DIR + "files/user_profiles");

        File[] files = iconDir.listFiles();
        if (files!=null) {
            for (File file : files) {
                file.delete();
            }
        }
    }


    /**
     * Store the passed bitmap on the file system
     * @param bitmap Bitmap to store
     * @param fileName Name to store under
     * @param compressFormat File kind (PNG / JPEG)
     * @param quality compression factor (for jpeg)
     * @return Path where the file was stored or null on error
     */
    public String storeBitmap(Bitmap bitmap, String fileName, Bitmap.CompressFormat compressFormat, int quality) {

        try {
            File baseDir = Environment.getExternalStorageDirectory();
            File tmpDir = new File(baseDir, APP_BASE_DIR + "pictures");
            if (!tmpDir.exists())
                tmpDir.mkdirs();
            File file = new File(tmpDir,fileName);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(compressFormat, quality, out);
            out.flush();
            out.close();
            bitmap.recycle();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return null;
        }
    }
}
