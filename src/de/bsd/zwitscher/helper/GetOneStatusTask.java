package de.bsd.zwitscher.helper;


import android.os.AsyncTask;
import de.bsd.zwitscher.TwitterHelper;
import twitter4j.Status;

/**
 * Async Task to fetch one status in a background thread
 *
 * @author Heiko W. Rupp
 */
public class GetOneStatusTask extends AsyncTask<Long,Void, Status> {

    private TwitterHelper th;

    public GetOneStatusTask(TwitterHelper th) {
        this.th = th;
    }

    @Override
    protected twitter4j.Status doInBackground(Long... params) {
            return th.getStatusById(params[0],0L,false,true,false);
    }
}
