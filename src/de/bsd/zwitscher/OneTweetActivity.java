package de.bsd.zwitscher;


import android.os.AsyncTask;
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
import twitter4j.User;

import java.util.Locale;

public class OneTweetActivity extends Activity {

	Context ctx = this;
	Status status ;
    ImageView userPictureView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.single_tweet);
        userPictureView = (ImageView) findViewById(R.id.UserPictureImageView);

		Bundle bundle = getIntent().getExtras();
		if (bundle!=null) {
			status = (Status) bundle.get(getString(R.string.status));
			Log.i("OneTweetActivity","Showing status: " + status.toString());

            // Download the user profile image in a background task, as this may
            // mean a network call.
            if (status.getRetweetedStatus()==null)
                new DownloadImageTask().execute(status.getUser());
            else
                new DownloadImageTask().execute(status.getRetweetedStatus().getUser());


			TextView tv01 = (TextView) findViewById(R.id.TextView01);
			if (status.getRetweetedStatus()==null) {
				tv01.setText(status.getUser().getName());
			}
			else {
				StringBuilder sb = new StringBuilder(status.getRetweetedStatus().getUser().getName());
				sb.append(" (");
				sb.append(status.getRetweetedStatus().getUser().getScreenName());
				sb.append(" ) retweeted by  ");
				sb.append(status.getUser().getName());
				tv01.setText(sb.toString());
			}
			TextView mtv = (TextView) findViewById(R.id.MiscTextView);
			if (status.getInReplyToScreenName()!=null) {
				mtv.setText("In reply to: " + status.getInReplyToScreenName());
			}
			else {
				mtv.setText("");
			}



			TextView tweetView = (TextView)findViewById(R.id.TweetTextView);
			tweetView.setText(status.getText());


            // Update Button state depending on Status' properties
			Button threadButton = (Button) findViewById(R.id.ThreadButton);
			if (status.getInReplyToScreenName()==null) {
				threadButton.setEnabled(false);
			}

			Button favoriteButton = (Button) findViewById(R.id.FavoriteButton);
			if (status.isFavorited())
				favoriteButton.setText("Un-Favorite");

		}
	}

	public void reply(View v) {
		Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
		i.putExtra(getString(R.string.status), status);
		i.putExtra("op", getString(R.string.reply));
		startActivity(i);

	}

	public void replyAll(View v) {
		Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
		i.putExtra(getString(R.string.status), status);
		i.putExtra("op", getString(R.string.replyall));
		startActivity(i);

	}



	public void retweet(View v) {
		TwitterHelper th = new TwitterHelper(ctx);
		th.retweet(status.getId());
	}


	public void classicRetweet(View v) {
		Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
		i.putExtra(getString(R.string.status), status);
		i.putExtra("op", getString(R.string.classicretweet));
		startActivity(i);

	}

    public void threadView(View v) {
        TwitterHelper th = new TwitterHelper(ctx);
        Status newStatus = th.getStatusById(status.getInReplyToStatusId());

        Intent i = new Intent(getApplicationContext(),OneTweetActivity.class);
        i.putExtra(getString(R.string.status), newStatus);
        startActivity(i);


    }

    public void favorite(View v) {
        TwitterHelper th = new TwitterHelper(ctx);

        th.favorite(status);
        // TODO update button state
    }

    public void directMessage(View v) {
        Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
        i.putExtra(getString(R.string.status), status);
        i.putExtra("op", getString(R.string.direct));
        startActivity(i);

    }



	public void speak(View v) {
		TextToSpeech tts = new TextToSpeech(getApplicationContext(),new OnInitListener() {

			@Override
			public void onInit(int status) {
				Log.i("speak","onInit " + status);

			}

		});
        tts.setLanguage(Locale.US);
		tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

			@Override
			public void onUtteranceCompleted(String utteranceId) {
				Log.i("speak","Utterance done");

			}
		});
		tts.speak(status.getText(), TextToSpeech.QUEUE_ADD, null);

	}


	public void translate(View v) {
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


	public void done(View v) {
		finish();
	}

    /**
     * Background task to download the user profile images.
     */
    private class DownloadImageTask extends AsyncTask<User, Void,Bitmap> {
        protected Bitmap doInBackground(User... users) {

            User user = users[0];
            PicHelper picHelper = new PicHelper();
            Bitmap bi = picHelper.getUserPic(user,ctx);
            return bi;
        }


        protected void onPostExecute(Bitmap result) {
            userPictureView.setImageBitmap(result);
     }
 }
}
