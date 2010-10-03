package de.bsd.zwitscher;


import com.google.api.translate.Language;
import com.google.api.translate.Translate;

import twitter4j.Status;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class OneTweetActivity extends Activity {

	Context ctx = this;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.single_tweet);
		Bundle bundle = getIntent().getExtras();
		if (bundle!=null) {
			final Status status = (Status) bundle.get(getString(R.string.status));
			Log.i("OneTweetActivity","Showing status: " + status.toString());
						
			TextView tv01 = (TextView) findViewById(R.id.TextView01);
			if (status.getRetweetedStatus()==null) {
				tv01.setText(status.getUser().getName());
			}
			else {
				StringBuilder sb = new StringBuilder(status.getRetweetedStatus().getUser().getName());
				sb.append(" (");
				sb.append(status.getRetweetedStatus().getUser().getScreenName());
				sb.append(" ) retweeted by  ");
				status.getUser().getName();
				tv01.setText(sb.toString());
			}
			TextView mtv = (TextView) findViewById(R.id.MiscTextView);
			if (status.getInReplyToScreenName()!=null) {
				mtv.setText("In reply to: " + status.getInReplyToScreenName());
			}
			else {
				mtv.setText("");
			}
			
			PicHelper ph = new PicHelper();
			Bitmap bi; 
			
			if (status.getRetweetedStatus()==null)
				bi = ph.getUserPic(status.getUser(),this);
			else
				bi = ph.getUserPic(status.getRetweetedStatus().getUser(),this);
			if (bi!=null) {
				ImageView iv = (ImageView) findViewById(R.id.UserPictureImageView);
				iv.setImageBitmap(bi);
			}

			TextView tweetView = (TextView)findViewById(R.id.TweetTextView);
			tweetView.setText(status.getText());


			final TwitterHelper th = new TwitterHelper(getApplicationContext());

			// -- now the buttons --

			Button replyButton = (Button) findViewById(R.id.ReplyButton);
			replyButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
					i.putExtra(getString(R.string.status), status);
					i.putExtra("op", getString(R.string.reply));
					startActivity(i);

				}
			});
			Button replyAllButton = (Button) findViewById(R.id.ReplyAllButton);
			replyAllButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
					i.putExtra(getString(R.string.status), status);
					i.putExtra("op", getString(R.string.replyall));
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
					Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
					i.putExtra(getString(R.string.status), status);
					i.putExtra("op", getString(R.string.classicretweet));
					startActivity(i);

				}
			});
			Button threadButton = (Button) findViewById(R.id.ThreadButton);
			if (status.getInReplyToScreenName()==null) {
				threadButton.setEnabled(false);
			}
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
					Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
					i.putExtra(getString(R.string.status), status);
					i.putExtra("op", getString(R.string.direct));
					startActivity(i);

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
					tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
						
						@Override
						public void onUtteranceCompleted(String utteranceId) {
							System.out.println("Utterance done");
							
						}
					});
					tts.speak(status.getText(), TextToSpeech.QUEUE_ADD, null);

				}
			});
			
			Button translateButton = (Button) findViewById(R.id.TranslateButton);
			translateButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Translate.setHttpReferrer("http://bsd.de/zwitscher");
					try {
						// TODO get target language from system
						String result = Translate.execute(status.getText(), Language.AUTO_DETECT, Language.GERMAN);
						AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
						builder.setMessage(result);
						builder.setTitle("Translation result");
						builder.setNeutralButton("ok", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
						AlertDialog alert = builder.create();
						alert.show();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Toast.makeText(getApplicationContext(), e.getMessage(), 15000).show();
					}
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
