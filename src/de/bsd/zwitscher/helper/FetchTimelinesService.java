package de.bsd.zwitscher.helper;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import de.bsd.zwitscher.TweetDB;
import de.bsd.zwitscher.TwitterHelper;
import de.bsd.zwitscher.account.Account;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.util.ArrayList;
import java.util.List;

/**
 * Intent service to fetch timeline data from the server and store it in the
 * Database
 *
 * @author Heiko W. Rupp
 */
public class FetchTimelinesService extends IntentService {

    public FetchTimelinesService() {
        super("FetchTimelinesService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Bundle bundle = intent.getExtras();

        int[] listIds = bundle.getIntArray("listIds");

        if (listIds==null) {
            Log.e("Fetcher","No listIds passed, returning");
            stopSelf();
            return;
        }
        Account account = bundle.getParcelable("account");
        int accountId = account.getId();

        TwitterHelper th = new TwitterHelper(getApplicationContext(),account);
        Twitter twitter = th.getTwitter();
        TweetDB tdb = TweetDB.getInstance(getApplicationContext());


        for (int listId : listIds ) {
            List<Status> statuses;
            long lastFetchedId = tdb.getLastFetched(accountId, listId);
            Paging paging ;
            if (lastFetchedId>0)
                paging=new Paging(lastFetchedId);
            else
                paging=new Paging(1,200);

            try {
                switch (listId) {
                    case 0:
                        statuses = twitter.getHomeTimeline(paging);
                        break;
                    case 1:
                        statuses = twitter.getMentionsTimeline(paging);
                        break;
                    case 2:
                        Log.w("Fetcher","Direct messages are not supported");
                        statuses = new ArrayList<Status>(1);
                        break;
                    case 3:
                        statuses = twitter.getUserTimeline(paging);
                        break;
                    default:
                        statuses = twitter.getUserListStatuses(listId,paging);
                }
                if (statuses.size()>0) {
                    th.persistStatus(statuses, listId);
                    long newLast=-1;
                    // Update the 'since' id in the database
                	if (statuses.size()>0) {
                		newLast = statuses.get(0).getId(); // assumption is that twitter sends the newest (=highest id) first
                    tdb.updateOrInsertLastRead(accountId,listId,newLast);
                    Intent loadDone = new Intent("zwitscher.LoadDone");
                    loadDone.putExtra("listId",listId);
                    sendBroadcast(loadDone);
                	}
                }
            } catch (TwitterException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
        }

        stopSelf();
    }
}
