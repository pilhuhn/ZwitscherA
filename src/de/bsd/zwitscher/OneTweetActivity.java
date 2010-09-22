package de.bsd.zwitscher;

import twitter4j.Status;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class OneTweetActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.single_tweet);
		Bundle bundle = getIntent().getExtras();
		if (bundle!=null) {
			final Status status = (Status) bundle.get("status");
			TextView tv01 = (TextView) findViewById(R.id.TextView01);
			tv01.setText(status.getUser().getName());
			if (status.getInReplyToScreenName()!=null) {
				TextView mtv = (TextView) findViewById(R.id.MiscTextView);
				mtv.setText("In reply to: " + status.getInReplyToScreenName());
			}
			
			
			TextView tweetView = (TextView)findViewById(R.id.TweetTextView);
			tweetView.setText(status.getText());
		
		
			final TwitterHelper th = new TwitterHelper(getApplicationContext());
			
			Button replyButton = (Button) findViewById(R.id.ReplyButton);
			replyButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
				}
			});
			Button retweetButton = (Button) findViewById(R.id.RetweetButton);
			retweetButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					th.retweet(status.getId());
				}
			});
			Button classicRetweetButton = (Button) findViewById(R.id.ClassicRetweetButton);
			classicRetweetButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
				}
			});
			Button threadButton = (Button) findViewById(R.id.ThreadButton);
			threadButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
				}
			});
			Button directButton = (Button) findViewById(R.id.DirectButton);
			directButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
				}
			});
			Button favoriteButton = (Button) findViewById(R.id.FavoriteButton);
			if (status.isFavorited())
				favoriteButton.setText("Un-Favorite");
			favoriteButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					th.favorite(status);
					// TODO update button state
				}
			});
			
			Button doneButton = (Button) findViewById(R.id.DoneButton);
			doneButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					finish(); // finish the activity
				}
			});

		}
	}
}
