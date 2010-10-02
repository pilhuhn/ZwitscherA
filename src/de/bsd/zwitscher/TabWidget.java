package de.bsd.zwitscher;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import twitter4j.UserList;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;

public class TabWidget extends TabActivity {

	TabHost tabHost;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.tabs);

		Resources res = getResources();
		tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent homeIntent = new Intent().setClass(this,TweetListActivity.class);

		spec = tabHost.newTabSpec("home")
				.setIndicator("Home",res.getDrawable(R.drawable.home))
				.setContent(homeIntent);
		tabHost.addTab(spec);
		
  		TweetDB tdb = new TweetDB(getApplicationContext());
  		Map<String, Integer> userLists = tdb.getLists();
  		for (Entry<String, Integer> userList : userLists.entrySet()) {
  			setUpTab(res, userList.getKey(),userList.getValue());
		}
		tabHost.setVerticalScrollBarEnabled(true);
		tabHost.setCurrentTab(0); // Home tab, tabs start at 0

	}

	private void setUpTab(Resources res, String listName, Integer listId) {
		TabHost.TabSpec spec;
		Intent intent;
		intent = new Intent().setClass(this,TweetListActivity.class);
		intent.putExtra("listName", listName);
		intent.putExtra("id", listId);
		
		spec = tabHost.newTabSpec(listId.toString())
		.setIndicator(listName,res.getDrawable(R.drawable.list))
		.setContent(intent);
		tabHost.addTab(spec);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.main_menu, menu);
		    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.reload_item:
	    	// Handled within TweetListActivity
	    	break;
	    case R.id.post_item:
	    	i = new Intent(TabWidget.this, NewTweetActivity.class);
	    	startActivity(i);
	        break;
	    case R.id.preferences:
	    	i = new Intent(TabWidget.this, Preferences.class);
			startActivity(i);
			break;
	    case R.id.reloadLists:
	  		syncLists();
	  		break;
	  		
	    	
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	    return true;
	}

	private void syncLists() {
		TwitterHelper th = new TwitterHelper(getApplicationContext());
		TweetDB tdb = new TweetDB(getApplicationContext());
		List<UserList> userLists = th.getUserLists();
		Map<String,Integer> storedLists = tdb.getLists();
		// Check for lists to add
		for (UserList userList : userLists) {
			if (storedLists.containsValue(userList.getId())) {
				continue;
			}
			else { 
				tdb.addList(userList.getName(),userList.getId());
				setUpTab(getResources(), userList.getName(), userList.getId());
			}
		}
		// check for outdated lists and remove them
		for (Entry<String, Integer> entry : storedLists.entrySet()) {
			Integer id = entry.getValue();
			boolean found = false;
			for (UserList userList2 : userLists) {
				if (userList2.getId() == id)
					found = true;
			}
			if (!found) {
				tdb.removeList(id);
				View tab = tabHost.findViewWithTag(id.toString());
				tabHost.removeView(tab);
				tabHost.setCurrentTab(0);
			}
		}
	}
}


