package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.bsd.zwitscher.R;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.PropertyConfiguration;

/**
 * // TODO: Document this
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

        String serverUrl = "http://twitter.com/"; // trailing slash is important!
        String searchBaseUrl = "http://search.twitter.com/";
        String username = "pilhuhn";
        String password = "xxx";

        Properties props = new Properties();
        props.put(PropertyConfiguration.SOURCE,"Jopr");
        props.put(PropertyConfiguration.HTTP_USER_AGENT,"Jopr");
        props.put(PropertyConfiguration.SEARCH_BASE_URL,searchBaseUrl);
        props.put(PropertyConfiguration.REST_BASE_URL,serverUrl);
        Configuration tconf = new PropertyConfiguration(props);

         TwitterFactory tFactory = new TwitterFactory(tconf);
        Twitter twitter = tFactory.getInstance(username,password);
        Paging paging = new Paging();

System.out.println("Twitter basics initialized");        
        
        try {
            statuses = twitter.getFriendsTimeline(paging );
System.out.println("Got twitter data");            
//        	setListAdapter(new MyListViewAdapter());
			List<String> data = new ArrayList<String>(statuses.size());
			for (Status status : statuses) {
				String item = status.getUser().getName() + ": " + status.getText();
				data.add(item);
			}
			setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, data));
System.out.println("Adapter set");        	



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

        } catch (TwitterException e) {
//            tv.append("\nError reading statuses: " + e.getMessage());
//            tv.append("\n Cause: " + e.getCause().getMessage());
        	System.err.println("Got exception: " + e.getMessage() + ": " + e.getCause().getLocalizedMessage() );
            String text = getString( R.string.no_connect ) + ": " + e.getMessage();
            Toast.makeText(getApplicationContext(), text, 2500).show();
        }

    }
    
}
