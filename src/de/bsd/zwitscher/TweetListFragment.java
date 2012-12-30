package de.bsd.zwitscher;


import android.app.Activity;
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
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.helper.StatusListLoader;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class TweetListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<Status>> {

    private static final String LOAD_DONE = "zwitscher.LoadDone";
    private TweetDB tweetDb;
    private String tag;
    private int listId;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.tweet_list_layout,container,false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tweetDb = TweetDB.getInstance(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);    // TODO: Customise this generated block

        IntentFilter filter = new IntentFilter(LOAD_DONE);
        getActivity().registerReceiver(new UpdateFinishReceiver(),filter);  // TODO unregister on orientation change


        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        Bundle loaderArgs = new Bundle();
        loaderArgs.putInt("listId", listId);
        getLoaderManager().initLoader(0, loaderArgs, this);
        getListView().setOverscrollHeader(getResources().getDrawable(R.drawable.ic_menu_top));
        getListView().setOverscrollFooter(getResources().getDrawable(R.drawable.ic_menu_share));

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public Loader<List<Status>> onCreateLoader(int id, Bundle args) {
        return new StatusListLoader(getActivity(),AccountHolder.getInstance().getAccount(), listId);
    }

    @Override
    public void onLoadFinished(Loader<List<Status>> loader, List<Status> data) {
        setListAdapter(new StatusAdapter<Status>(getActivity(), AccountHolder.getInstance().getAccount(),R.layout.tweet_list_item,data,-1,new ArrayList<Long>()));
    }

    @Override
    public void onLoaderReset(Loader<List<Status>> loader) {
        // TODO: Customise this generated block
    }



    public void setTag(String tag) {
        this.tag = tag;
        if (tag.equals("home"))
            listId = 0;
        else if (tag.equals("mentions"))
            listId = 1;
        else
            listId = 0; // TODO fallback for now
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
