package de.bsd.zwitscher.h;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private View view;
    private MetaList<Status> statusList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.tweet_list_layout,container);

        setAdapterForListId(0);
        return view;

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        getListView().setItemChecked(position,true);

        OneTweetFragment otf = (OneTweetFragment) getFragmentManager().findFragmentById(R.id.h_one_tweet_fragment);
        if (otf == null) {
            otf = new OneTweetFragment();
        }

        otf.setStatus(statusList.getList().get(position));

        View vi = view.findViewById(R.id.h_misc_frame);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction fmt = fm.beginTransaction();
        fmt.replace(R.id.h_one_tweet_frame, otf);  // defined in main_layout.xml
        fmt.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        fmt.commit();

        // Make stuff visible



//        super.onListItemClick(l, v, position, id);    // TODO: Customise this generated block
    }




    void setTimeline(int list_id) {
        if (list_id==-2)
            return;

        setAdapterForListId(list_id);
    }

    private void setAdapterForListId(int list_id) {
        Account account = AccountHolder.getInstance().getAccount();
        TwitterHelper th = new TwitterHelper(getActivity(),account);

        Paging paging = new Paging();
        statusList = th.getTimeline(paging, list_id, true);

        ListAdapter a = new StatusAdapter<Status>(getActivity(),account, R.layout.tweet_list_item, statusList.getList());
        setListAdapter(a);
    }
}
