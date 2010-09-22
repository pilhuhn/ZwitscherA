package de.bsd.zwitscher;

import java.net.URL;

import twitter4j.Status;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
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
				URL imageUrl = status.getUser().getProfileImageURL();
				ImageView iv = (ImageView) findViewById(R.id.ImageView01);
			}
			
			
			TextView tweetView = (TextView)findViewById(R.id.TweetTextView);
			tweetView.setText(status.getText());
		
		
			final TwitterHelper th = new TwitterHelper(getApplicationContext());
			
			// -- now the buttons --
			
			Button replyButton = (Button) findViewById(R.id.ReplyButton);
			replyButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent i = new Intent(getApplicationContext(),OneTweetActivity.class);
					i.putExtra("status", status.getText());
					i.putExtra("op", "reply");
					startActivity(i);
					
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

			Button speakButton = (Button) findViewById(R.id.SpeakButton);
			speakButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					TextToSpeech tts = new TextToSpeech(getApplicationContext(),new OnInitListener() {
						
						@Override
						public void onInit(int status) {
							// TODO Auto-generated method stub
							
						}
					});
					tts.speak(status.getText(), TextToSpeech.QUEUE_ADD, null);
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
