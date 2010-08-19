package de.bsd.zwitscher;

import java.util.List;

import de.bsd.zwitscher.R;
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
		intent = new Intent().setClass(this,MainActivity.class);
		
		spec = tabHost.newTabSpec("main")
				.setIndicator("Post",res.getDrawable(R.drawable.ic_tab_artists))
				.setContent(intent);
		tabHost.addTab(spec);
		
		TwitterHelper th = new TwitterHelper(getApplicationContext());
		List<String> userLists =  th.getListNames();
		for (String userList : userLists) {
			intent = new Intent().setClass(this,MainActivity.class); // TODO Fix intent class
			spec = tabHost.newTabSpec(userList)
			.setIndicator(userList,res.getDrawable(R.drawable.ic_tab_artists))
			.setContent(intent);
			tabHost.addTab(spec);
		}
		tabHost.setVerticalScrollBarEnabled(true);
		tabHost.setCurrentTab(1); // Post tab, tabs start at 0
		
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
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.reload_item:
	    	// Handled within TweetListActivity
	    	break;
	    case R.id.post_item:
	    	tabHost.setCurrentTab(1); // Post tab, tabs start at 0
	        break;
	    case R.id.preferences:
	    	Intent i = new Intent(TabWidget.this, Preferences.class);
			startActivity(i);
			break;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	    return true;
	}

	
}
