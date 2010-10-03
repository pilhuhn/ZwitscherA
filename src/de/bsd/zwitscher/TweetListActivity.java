package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.User;

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
    }

    @Override
    public void onResume() {

    	super.onResume();

        ProgressDialog dialog = ProgressDialog.show(TweetListActivity.this, "Loading tweets",
                "Please wait...", true);
        Bundle bundle = getIntent().getExtras();
        List<String> data;
        if (bundle!=null) {
        	String listName = bundle.getString("listName");
        	int id = bundle.getInt("id");
        	data = getTimlineStringsFromTwitter(R.string.list,id, listName);
        }
        else {
        	data = getTimlineStringsFromTwitter(R.string.home_timeline,0, null);
        }
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item, data);
		setListAdapter(arrayAdapter);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		dialog.cancel();

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				// TODO how to handle the case of a tweet with a link inside?
				Intent i = new Intent(parent.getContext(),OneTweetActivity.class);
				i.putExtra(getString(R.string.status), statuses.get(position));
				startActivity(i);

			}
		});
		lv.setOnItemLongClickListener(new OnItemLongClickListener() { // directly go to reply.

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Log.i("TLA","Long click, pos=" + position + ",id="+id);
				Intent i = new Intent(parent.getContext(), NewTweetActivity.class);
				i.putExtra(getString(R.string.status), statuses.get(position));
				i.putExtra("op",getString(R.string.reply));
				startActivity(i);

				return true; // We've consumed the long click
			}
		});

    }

	private List<String> getTimlineStringsFromTwitter(int timeline,int id, String specialName) {
		TwitterHelper th = new TwitterHelper(getApplicationContext());
		Paging paging = new Paging();
		TweetDB tdb = new TweetDB(this);
		
		// First get saved paging id to limit what to fetch
		if (timeline== R.string.home_timeline)
			specialName = "home";

    	long last = tdb.getLastRead(specialName);
    	if (last!=0 && !Debug.isDebuggerConnected())
    		paging.sinceId(last);
		
        switch (timeline) {

        case R.string.home_timeline:
        	statuses = th.getFriendsTimeline(paging);
        	break;
        case R.string.list:
        	statuses = th.getUserList(paging,id);
        	break;
        }
        
        // Update the 'since' id in the database
    	if (statuses.size()>0) {
    		last = statuses.get(0).getId(); // assumption is that twitter sends the newest (=highest id) first 
    		tdb.updateOrInsertLastRead(specialName, last);
    	}

		List<String> data = new ArrayList<String>(statuses.size());
		for (Status status : statuses) {
			User user = status.getUser();
			String item = user.getName() +  " (" + user.getScreenName() + "): " + status.getText();
			data.add(item);
		}
		if (statuses.size()==0) { // No (new) tweet found
			data.add(">>  Sorry, no tweets currently available, try later << ");
		}
		return data;
	}

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

    	if (item!=null && item.getItemId() == R.id.reload_item) {
    		List<String> data = getTimlineStringsFromTwitter(R.string.home_timeline,0, null); // TODO depending on tab
    		setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, data));
    		getListView().requestLayout();
    		return true;
    	}

    	return super.onMenuItemSelected(featureId, item);
    }

}
