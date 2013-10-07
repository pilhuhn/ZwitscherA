package de.bsd.zwitscher;


import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.MetaList;
import twitter4j.Paging;
import twitter4j.Status;

import java.util.ArrayList;

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
public class TweetListFragment extends ListFragment {

    private int listId;
    private Account account;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tweet_list_layout, container,false);

        return view;
    }

    void setListId(int listId) {
        this.listId = listId;
    }

    @Override
    public void onActivityCreated(Bundle savedState) {

        super.onActivityCreated(savedState);
        ListView lv ;


        TwitterHelper th = new TwitterHelper(getActivity(),account);
        MetaList<Status> statuses = th.getTimeline(new Paging(), listId, true);

        setListAdapter(new StatusAdapter<Status>(getActivity(), account, R.layout.tweet_list_item, statuses.getList(), 0, new ArrayList<Long>()));

        lv = getListView();
        lv.setItemsCanFocus(false);

    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
