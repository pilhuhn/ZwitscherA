package de.bsd.zwitscher;

import java.util.Properties;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.PropertyConfiguration;
import de.bsd.zwitscher.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    
	    final Button tweetButton = (Button) findViewById(R.id.TweetButton);  
		final EditText edittext = (EditText) findViewById(R.id.edittext);
		edittext.setSelected(true);
		tweetButton.setEnabled(false);
		edittext.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		    	
		    	
		        // If the event is a key-down event on the "enter" button
		        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
		            (keyCode == KeyEvent.KEYCODE_ENTER)) {
		          // Perform action on key press
//		          Toast.makeText(getApplicationContext(), edittext.getText(), 2000).show();
		        	tweet(edittext.getText().toString());
		          return true;
		        }
		        
		        if ((event.getAction() == KeyEvent.ACTION_UP) && edittext.getTextSize() >0 ) {
		        	tweetButton.setEnabled(true);
		        }
		        
		        return false;
		    }
		});
		
		
		tweetButton.setOnClickListener(new OnClickListener() {
			
			
			@Override
			public void onClick(View v) {
				System.out.println("clicked, text is " + edittext.getText().toString());
				tweet(edittext.getText().toString());
				
			}
		});

		final Button clearButton = (Button) findViewById(R.id.ClearButton);
		clearButton.setOnClickListener(new OnClickListener() {
			
			
			@Override
			public void onClick(View v) {
				edittext.setText("");
				
			}
		});
      
	}
	
	public void tweet(String text) {
        String serverUrl = "http://twitter.com/"; // trailing slash is important!
        String searchBaseUrl = "http://search.twitter.com/";
        String username = "pilhuhn";
        String password = "bgfcrttw08";

        Properties props = new Properties();
        props.put(PropertyConfiguration.SOURCE,"Zwitscher");
        props.put(PropertyConfiguration.HTTP_USER_AGENT,"Zwitscher");
        props.put(PropertyConfiguration.SEARCH_BASE_URL,searchBaseUrl);
        props.put(PropertyConfiguration.REST_BASE_URL,serverUrl);
        Configuration tconf = new PropertyConfiguration(props);

         TwitterFactory tFactory = new TwitterFactory(tconf);
        Twitter twitter = tFactory.getInstance(username,password);
        
        try {
			twitter.updateStatus(text);
			Toast.makeText(getApplicationContext(), R.string.tweet_sent , 2500).show();
		} catch (TwitterException e) {
			Toast.makeText(getApplicationContext(), "Failed to send tweet: " + e.getLocalizedMessage(), 5000).show();
		}

	}
}
