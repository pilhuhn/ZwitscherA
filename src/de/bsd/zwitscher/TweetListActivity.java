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

		List<String> data = getTimlineStringsFromTwitter();
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

    }

	private List<String> getTimlineStringsFromTwitter() {
		TwitterHelper th = new TwitterHelper();
		Paging paging = new Paging();
        
        	
        statuses = th.getFriendsTimeline(getApplicationContext(), paging);
		List<String> data = new ArrayList<String>(statuses.size());
		for (Status status : statuses) {
			String item = status.getUser().getName() + ": " + status.getText();
			data.add(item);
		}
		return data;
	}
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	// TODO Auto-generated method stub

    	if (item!=null && item.getItemId() == R.id.reload_item) {
    		List<String> data = getTimlineStringsFromTwitter();
    		setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, data));
    		getListView().requestLayout();
    		return true;
    	}
    	
    	return super.onMenuItemSelected(featureId, item);
    }
    
}
