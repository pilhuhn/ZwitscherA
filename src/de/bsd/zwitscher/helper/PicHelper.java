package de.bsd.zwitscher.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.*;
import de.bsd.zwitscher.PicHelperState;
import twitter4j.User;
import android.os.Environment;
import android.util.Log;

/**
 * Helper class that deals with handling of pictures and storing
 * them on local file system.
 */
public class PicHelper {

    private static final String APP_BASE_DIR = "/Android/data/de.bsd.zwitscher/";
    private String externalStorageState;

    public PicHelper() {
        externalStorageState = Environment.getExternalStorageState();
    }


	private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    /**
     * Load the user picture for the passed user.
     * If this is on file system, it is checked if it has changed on the server.
     * If it is not yet on filesystem, it is fetched from remote and stored locally.
     * @param user User for which to obtain the picture
     * @return Bitmap of the picture or null if loading failed.
     */
	public Bitmap fetchUserPic(User user) {

        if (user==null)
            return null;

        String tmp = user.getProfileImageURL();
        URL imageUrl;
        try {
            imageUrl = new URL(tmp);
        } catch (MalformedURLException e) {
            Log.w("PicHelper","Got bad picture url for user " + user.getScreenName() + ": " + e.getMessage());
            return null;
        }

        String username = user.getScreenName();
//        Log.i("fetchUserPic","Looking for pic of user '" + username + "' from '" + imageUrl.toString() + "'");
		boolean found = false;
		// TODO use v8 methods when we require v8 in the manifest. Is probably too early yet.
		try {
			if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
				File iconFile = getPictureFileForUser(username);
				if (iconFile!=null && iconFile.exists() && iconFile.lastModified() > System.currentTimeMillis() - ONE_DAY)
					found = true;
			}
			if (found) {
//				Log.i("fetchUserPic","Picture was on file system, returning it");
                return getBitMapForScreenNameFromFile(username);
            }
		}
		catch (Exception ioe) {
			Log.w("PicHelper", ioe.getMessage());
		}

//        Log.i("fetchUserPic","found=" + found);

        // Not found - fetch from remote and store it locally
		if (!found) {

            // See if there is already another thread syncing. If
            // so, wait a while to see if it was successful
            // and then return the data from the file system
            if (PicHelperState.getInstance().isSyncing(username)) {
                int rounds = 10;
                while (rounds>0) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();  // TODO: Customise this generated block
                    }
                    if (!PicHelperState.getInstance().isSyncing(username)) {
                        return getBitMapForScreenNameFromFile(username);
                    }
                    rounds--;
                }
            }
            // Not found yet or first to look for the remote picture, start syncing now
            PicHelperState.getInstance().setSyncing(username);

            Bitmap bitmap=null;
            try {
//                Log.i("fetchUserPic","Downloading image for "+ username +" and persisting it locally");
                BufferedInputStream in = new BufferedInputStream(imageUrl.openStream());
                bitmap = BitmapFactory.decodeStream(in);

                if (bitmap!=null && externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
                    File iconFile = getPictureFileForUser(username);
//                    Log.i("fetchUserPic","Storing picture for " + username +" at " + iconFile.getAbsolutePath());
    				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(iconFile));
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, out);
                    out.flush();
                    out.close();
    			}
            in.close();
//            if (bitmap!=null)
//               bitmap.recycle();
			}
			catch (IOException ioe) {
                Log.w("PicHelper","Failed to download the image: " + ioe.getMessage());
			}

            PicHelperState.getInstance().syncDone(username); // We're done, remove syncing state
//            Log.i("fetchUP","loaded? bm=" + bitmap);
            return bitmap;
		}
        return null;
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
                if (iconFile==null)
                    return null;
                if (!iconFile.exists()) {
//                    Log.w("PicHelper","getPictureFileForUser("+username+"): file does not exist");
                    return null;
                }

                FileInputStream fis = new FileInputStream(iconFile);
                Bitmap bi = BitmapFactory.decodeStream(fis);
                fis.close();
                return bi;
            }
        }
        catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Locate the file object for the passed screen name
     * @param username Screen name to lookup
     * @return File object of the matching file
     */
    private File getPictureFileForUser(String username) {
        if (username==null)
            return null;

		File baseDir = Environment.getExternalStorageDirectory();
		File iconDir = new File(baseDir, APP_BASE_DIR + "files/user_profiles");
		if (!iconDir.exists())
			iconDir.mkdirs();
		File iconFile = new File(iconDir,username);
		return iconFile;
	}

    /**
     * Remove the stored user pictures
     * @param olderThan Only delete pictures that are older than the passed time
     */
    public void cleanup(long olderThan) {
        File baseDir = Environment.getExternalStorageDirectory();
        File iconDir = new File(baseDir, APP_BASE_DIR + "files/user_profiles");

        File[] files = iconDir.listFiles();
        if (files!=null) {
            for (File file : files) {
                if (file.lastModified()< olderThan) {
                    boolean success = file.delete();
                    if (!success)
                        Log.e("PicHelper","Could not delete " + file.getAbsolutePath());
                }
            }
        }
    }


    /**
     * Store the passed bitmap on the file system (recorded camera images)
     *
     * @param bitmap Bitmap to store
     * @param file File to store in
     * @param compressFormat File kind (PNG / JPEG)
     * @param quality compression factor (for jpeg)
     * @return Path where the file was stored or null on error
     */
    public String storeBitmap(Bitmap bitmap, File file, Bitmap.CompressFormat compressFormat, int quality) {

        try {
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
