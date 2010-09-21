package de.bsd.zwitscher;


import twitter4j.Status;
import twitter4j.StatusUpdate;
import de.bsd.zwitscher.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	EditText edittext;
	Status origStatus;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    
	    final Button tweetButton = (Button) findViewById(R.id.TweetButton);  
		edittext = (EditText) findViewById(R.id.edittext);
		edittext.setSelected(true);
		tweetButton.setEnabled(false);
		
		Bundle bundle = getIntent().getExtras();
		if (bundle!=null) {
			origStatus = (Status) bundle.get("status");
			Log.i("Replying..", "Orig is " + origStatus);
			TextView textOben = (TextView) findViewById(R.id.textOben);
			textOben.setText(origStatus.getText());
			edittext.setText("@"+origStatus.getUser().getScreenName()+" ");
		}
		
		edittext.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		    	
		    	
		        // If the event is a key-down event on the "enter" button
		        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
		            (keyCode == KeyEvent.KEYCODE_ENTER)) {
		          // Perform action on key press
//		          Toast.makeText(getApplicationContext(), edittext.getText(), 2000).show();
		        	StatusUpdate up  = new StatusUpdate(edittext.getText().toString());
		        	if (origStatus!=null) {
		        		up.setInReplyToStatusId(origStatus.getId());
		        	}
		        	tweet(up);
		        	origStatus=null;
		        	finish();
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
				StatusUpdate up  = new StatusUpdate(edittext.getText().toString());
	        	if (origStatus!=null) {
	        		up.setInReplyToStatusId(origStatus.getId());
	        	}
	        	tweet(up);
	        	origStatus=null;
	        	finish();
				
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
	

	public void tweet(StatusUpdate update) {
		TwitterHelper th = new TwitterHelper(getApplicationContext());
		th.updateStatus(update);
	}
	
			
}
