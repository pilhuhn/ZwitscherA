package de.bsd.zwitscher.helper;


import android.content.AsyncTaskLoader;
import android.content.Context;
import de.bsd.zwitscher.TwitterHelper;
import de.bsd.zwitscher.account.Account;
import twitter4j.Status;

import java.util.List;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class StatusListLoader extends AsyncTaskLoader<List<Status>> {

    private Account account;
    private int listId;
    private final TwitterHelper twitterHelper;

    public StatusListLoader(Context context, Account account, int listId) {
        super(context);
        this.account = account;
        this.listId = listId;
        twitterHelper = new TwitterHelper(context,account);
    }



    @Override
    public List<Status> loadInBackground() {
        return twitterHelper.getStatuesFromDb(-1,20,listId); // TODO parametrize
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
