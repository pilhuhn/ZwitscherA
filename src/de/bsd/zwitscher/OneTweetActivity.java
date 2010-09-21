package de.bsd.zwitscher;

import twitter4j.Status;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class OneTweetActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.single_tweet);
		Bundle bundle = getIntent().getExtras();
		if (bundle!=null) {
			Status status = (Status) bundle.get("status");
			TextView tv01 = (TextView) findViewById(R.id.TextView01);
			tv01.setText(status.getUser().getName());
			
			TextView tweetView = (TextView)findViewById(R.id.TweetTextView);
			tweetView.setText(status.getText());
		}
	}
}
