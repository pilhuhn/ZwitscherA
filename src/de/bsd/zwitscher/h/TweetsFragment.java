package de.bsd.zwitscher.h;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TwitterHelper;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.helper.MetaList;
import de.bsd.zwitscher.StatusAdapter;
import twitter4j.Paging;
import twitter4j.Status;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
public class TweetsFragment extends ListFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tweet_list_layout,container);

        return view;

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);    // TODO: Customise this generated block
    }

    void setTimeline(int list_id) {
        if (list_id==-2)
            return;

        Account account = AccountHolder.getInstance().getAccount();
        TwitterHelper th = new TwitterHelper(getActivity(),account);

        Paging paging = new Paging();
        MetaList<Status> ml = th.getTimeline(paging, list_id, true);

        ListAdapter a = new StatusAdapter<Status>(getActivity(),account,R.layout.tweet_list_item,ml.getList());
        setListAdapter(a);
    }
}
