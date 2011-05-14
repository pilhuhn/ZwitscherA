package de.bsd.zwitscher.h;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.bsd.zwitscher.R;

/**
 * Show the list of Timelines
 * @author Heiko W. Rupp
 */
public class TimelinesFragment extends ListFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.timelines_list_fragment, container);

        List<String> timelines = new ArrayList<String>();
        timelines.add("Home");
        timelines.add("Mentions");
        timelines.add("Directs");
        timelines.add("Sent");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, timelines);
        setListAdapter(adapter);

        return view;

    }

    public void onListItemClick(ListView listView, View rowView, int position, long id) {
//        super.onListItemClick(listView, rowView, position, id);

        TweetsFragment tf = (TweetsFragment) getFragmentManager().findFragmentById(R.id.h_tweets_fragment);
        if (tf==null)
            return;

        int tl=-position;
        tf.setTimeline(tl);

        // TODO set right list

    }
}
