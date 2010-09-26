package de.bsd.zwitscher;

import java.util.List;

import twitter4j.UserList;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
		Intent intent;
		intent = new Intent().setClass(this,TweetListActivity.class);

		spec = tabHost.newTabSpec("home")
				.setIndicator("Home",res.getDrawable(R.drawable.home))
				.setContent(intent);
		tabHost.addTab(spec);
		
  		TwitterHelper th = new TwitterHelper(getApplicationContext());
		List<UserList> userLists =  th.getUserLists();
		for (UserList userList : userLists) {
			intent = new Intent().setClass(this,TweetListActivity.class);
			intent.putExtra("listName", userList);
			intent.putExtra("id", userList.getId());
			
			spec = tabHost.newTabSpec(userList.getName())
			.setIndicator(userList.getName(),res.getDrawable(R.drawable.list))
			.setContent(intent);
			tabHost.addTab(spec);
		}
		tabHost.setVerticalScrollBarEnabled(true);
		tabHost.setCurrentTab(0); // Home tab, tabs start at 0

		String tabTag = tabHost.getCurrentTabTag();

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
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	    return true;
	}


}
