package de.bsd.zwitscher;

import de.bsd.zwitscher.R;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;

public class TabWidget extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.tabs);
		
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;
System.out.println("Creating 1st");		
		intent = new Intent().setClass(this,TweetListActivity.class);
		
		spec = tabHost.newTabSpec("home")
				.setIndicator("Home",res.getDrawable(R.drawable.home))
				.setContent(intent);
		tabHost.addTab(spec);
System.out.println("Creating 2nd");		
		intent = new Intent().setClass(this,MainActivity.class);
		
		spec = tabHost.newTabSpec("main")
				.setIndicator("Main",res.getDrawable(R.drawable.ic_tab_artists))
				.setContent(intent);
		tabHost.addTab(spec);
System.out.println("setting current tab");		
		tabHost.setCurrentTab(1);
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
	    	Toast.makeText(getApplicationContext(), R.string.no_connect, 2500).show();	        
	    	break;
	    case R.id.post_item:
	    	Toast.makeText(getApplicationContext(), R.string.loading, 2500).show();
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
