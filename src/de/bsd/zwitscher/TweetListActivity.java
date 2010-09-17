package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.List;

import de.bsd.zwitscher.R;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import twitter4j.Paging;
import twitter4j.Status;

/**
 * Show the list of tweets (home timeline only at the moment)
 * @author Heiko W. Rupp
 */
public class TweetListActivity extends ListActivity {

    List<Status> statuses;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);

		Toast.makeText(getApplicationContext(), R.string.loading, 1500).show();

		List<String> data = getTimlineStringsFromTwitter(R.string.home_timeline);
		setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, data));

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				Toast.makeText(getApplicationContext(),
						((TextView) view).getText(), Toast.LENGTH_SHORT)
						.show();
			}
		});
		lv.setOnItemLongClickListener(new OnItemLongClickListener() { // TODO better use a context menu?

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Toast.makeText(getApplicationContext(),
						"Long click", Toast.LENGTH_SHORT)
						.show();

				return true; // We've consumed the long click
			}
		});

    }
    
	private List<String> getTimlineStringsFromTwitter(int timeline) {
		TwitterHelper th = new TwitterHelper(getApplicationContext());
		Paging paging = new Paging();
        
        switch (timeline) {
        
        case R.string.home_timeline:
        	statuses = th.getFriendsTimeline(paging);
        	break;
        }
		List<String> data = new ArrayList<String>(statuses.size());
		for (Status status : statuses) {
			String item = status.getUser().getName() + ": " + status.getText();
			data.add(item);
		}
		return data;
	}
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

    	if (item!=null && item.getItemId() == R.id.reload_item) {
    		List<String> data = getTimlineStringsFromTwitter(R.string.home_timeline);
    		setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, data));
    		getListView().requestLayout();
    		return true;
    	}
    	
    	return super.onMenuItemSelected(featureId, item);
    }
    
}
