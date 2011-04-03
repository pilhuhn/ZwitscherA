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
public class CleanupTask extends AsyncTask<Integer,Void,Void> {

    private Context context;
    long MILLIS = 1000;
    long DAY = 86400L * MILLIS;
    long WEEK = 7* DAY;
    ProgressDialog pd ;

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
    protected Void doInBackground(Integer... params) {

        // Clean tweets that are a older than a week
        long tOld = System.currentTimeMillis() - WEEK;

        TweetDB tdb = new TweetDB(context,-1); // account id does not matter
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
