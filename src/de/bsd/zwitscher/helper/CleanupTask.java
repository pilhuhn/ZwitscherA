package de.bsd.zwitscher.helper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import de.bsd.zwitscher.TweetDB;

/**
 * Task that is started in the and
 * which in the background cleans out the oldest tweets, users and user images
 * @author Heiko W. Rupp
 */
public class CleanupTask extends AsyncTask<Void,Void,Void> {

    private final Context context;
    private static final long MILLIS = 1000;
    private static final long DAY = 86400L * MILLIS;
    private final static long WEEK = 7* DAY;
    private ProgressDialog pd ;

    public CleanupTask(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pd = new ProgressDialog(context);
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.setMessage("Cleaning up...");
        pd.show();
    }

    @Override
    protected Void doInBackground(Void... params) {

        // Clean tweets that are a older than a week
        long tOld = System.currentTimeMillis() - WEEK;

        TweetDB tdb = TweetDB.getInstance(context.getApplicationContext()); // account id does not matter
        tdb.cleanStatusesAndUsers(tOld);
        PicHelper ph = new PicHelper();
        ph.cleanup(tOld);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        pd.cancel();
        pd.hide();
    }
}
