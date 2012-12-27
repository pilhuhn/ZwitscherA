package de.bsd.zwitscher;


import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class TweetListFragment extends ListFragment {

    private TweetDB tweetDb;
    private String tag;


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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        List<String> items = new ArrayList<String>();
        items.add(tag);
        items.add("Li la lu");
        items.add("Hello World");
        setListAdapter(new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,items));

    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
