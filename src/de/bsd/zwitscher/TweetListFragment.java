package de.bsd.zwitscher;


import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.StatusListLoader;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Show the list of tweets.
 * To unify things a bit, we introduce pseudo list ids for timelines that are not lists:
 * <ul>
 * <li>0 : home/friends timeline</li>
 * <li>-1 : mentions </li>
 * <li>-2 : direct </li>
 * <li>&gt;0 : saved search</li>
 * </ul>
 *
 * @author Heiko W. Rupp
 */
public class TweetListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<Status>> {

    private static final String LOAD_DONE = "zwitscher.LoadDone";
    private TweetDB tweetDb;
    private String tag;
    private int listId;
    private Account account;
    private UpdateFinishReceiver receiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tweet_list_layout, container,false);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tweetDb = TweetDB.getInstance(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        IntentFilter filter = new IntentFilter(LOAD_DONE);
        receiver = new UpdateFinishReceiver();
        getActivity().registerReceiver(receiver,filter);


        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        Bundle loaderArgs = new Bundle();
        loaderArgs.putInt("listId", listId);
        getLoaderManager().initLoader(0, loaderArgs, this);
        getListView().setOverscrollHeader(getResources().getDrawable(R.drawable.ic_menu_top)); // TODO what icon?
        getListView().setOverscrollFooter(getResources().getDrawable(R.drawable.ic_menu_share)); // TODO what icon?

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        getActivity().unregisterReceiver(receiver);
    }

    @Override
    public Loader<List<Status>> onCreateLoader(int id, Bundle args) {
        return new StatusListLoader(getActivity(), account, listId);
    }

    @Override
    public void onLoadFinished(Loader<List<Status>> loader, List<Status> data) {
        setListAdapter(new StatusAdapter<Status>(getActivity(), account,R.layout.tweet_list_item,data,-1,new ArrayList<Long>()));
    }

    @Override
    public void onLoaderReset(Loader<List<Status>> loader) {
        // TODO: Customise this generated block
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    void setListId(int listId) {
        this.listId = listId;
    }


    private class UpdateFinishReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int intentListId = intent.getIntExtra("listId",0);
            if (intentListId == listId ) {
                Bundle loaderArgs = new Bundle();
                loaderArgs.putInt("listId", listId);
                getLoaderManager().restartLoader(0, loaderArgs, TweetListFragment.this);
            }
        }
    }
}
