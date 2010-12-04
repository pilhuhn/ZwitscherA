package de.bsd.zwitscher;


import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.Html;
import android.view.Window;
import android.widget.*;
import com.google.api.translate.Language;
import com.google.api.translate.Translate;

import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.helper.PicHelper;
import twitter4j.Place;
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
import twitter4j.User;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.*;

/**
 * This Activity displays one individual status.
 * Layout definition is in res/layout/single_tweet
 *
 * @author Heiko W. Rupp
 */
public class OneTweetActivity extends Activity implements OnInitListener, OnUtteranceCompletedListener {

	Context ctx = this;
	Status status ;
    ImageView userPictureView;
    ProgressBar pg;
    ImageView thumbnailView;
    boolean downloadPictures=false;
    TextToSpeech tts;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.single_tweet);
        setupspeak();

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
        pg = (ProgressBar) findViewById(R.id.title_progress_bar);

        userPictureView = (ImageView) findViewById(R.id.UserPictureImageView);
        thumbnailView = (ImageView) findViewById(R.id.OTImageView);

        NetworkHelper networkHelper = new NetworkHelper(this);
        downloadPictures = networkHelper.mayDownloadImages();

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

            new DownloadThumbnailTask().execute(status);

			TextView tv01 = (TextView) findViewById(R.id.TextView01);
            StringBuilder sb = new StringBuilder("<b>");
			if (status.getRetweetedStatus()==null) {
                sb.append(status.getUser().getName());
                sb.append(" (");
                sb.append(status.getUser().getScreenName());
                sb.append(")");
                sb.append("</b>");
			}
			else {
                sb.append(status.getRetweetedStatus().getUser().getName());
				sb.append(" (");
				sb.append(status.getRetweetedStatus().getUser().getScreenName());
				sb.append(" )</b> ").append(getString(R.string.resent_by)).append(" <b>");
				sb.append(status.getUser().getName());
                sb.append("</b>");
			}
            tv01.setText(Html.fromHtml(sb.toString()));

			TextView mtv = (TextView) findViewById(R.id.MiscTextView);
			if (status.getInReplyToScreenName()!=null) {
                String s = getString(R.string.in_reply_to);
                mtv.setText(Html.fromHtml(s + " <b>" + status.getInReplyToScreenName() + "</b>"));
			}
			else {
				mtv.setText("");
			}

			TextView tweetView = (TextView)findViewById(R.id.TweetTextView);
			tweetView.setText(status.getText());

            TextView timeCientView = (TextView)findViewById(R.id.TimeTextView);
            TwitterHelper th = new TwitterHelper(this);
            String s = getString(R.string.via);
            String text = th.getStatusDate(status) + s + status.getSource();
            String from = getString(R.string.from);
            if (status.getPlace()!=null) {
                Place place = status.getPlace();
                text += " " + from + " " + place.getFullName();
            }
            timeCientView.setText(Html.fromHtml(text));


            // Update Button state depending on Status' properties
			ImageButton threadButton = (ImageButton) findViewById(R.id.ThreadButton);
			if (status.getInReplyToScreenName()==null) {
				threadButton.setEnabled(false);
			}

			ImageButton favoriteButton = (ImageButton) findViewById(R.id.FavoriteButton);
			if (status.isFavorited())
				favoriteButton.setImageResource(R.drawable.favorite_on);

            ImageButton translateButon = (ImageButton) findViewById(R.id.TranslateButton);
            translateButon.setEnabled(networkHelper.isOnline());
		}
	}

    /**
     * Display display of the details of a user from pressing
     * the user icon button.
     * @param v
     */
    @SuppressWarnings("unused")
    public void displayUserDetail(View v) {
        Intent i = new Intent(getApplicationContext(), UserDetailActivity.class);
        User theUser;
        if (status.getRetweetedStatus()==null) {
            theUser = status.getUser();
        } else {
            theUser = status.getRetweetedStatus().getUser();
        }
        i.putExtra("userName", theUser.getName());
        i.putExtra("userId",theUser.getId());
        startActivity(i);
    }

    /**
     * Trigger replying to the current status.
     * @param v
     */
    @SuppressWarnings("unused")
	public void reply(View v) {
		Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
		i.putExtra(getString(R.string.status), status);
		i.putExtra("op", getString(R.string.reply));
		startActivity(i);

	}

    /**
     * Trigger replying to all users mentioned via @xxx in the
     * current status. Opens an editor Window first
     * @param v
     */
    @SuppressWarnings("unused")
	public void replyAll(View v) {
		Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
		i.putExtra(getString(R.string.status), status);
		i.putExtra("op", getString(R.string.replyall));
		startActivity(i);

	}

    /**
     * Trigger a resent of the current status
     * @param v
     */
    @SuppressWarnings("unused")
	public void retweet(View v) {
        UpdateRequest request = new UpdateRequest(UpdateType.RETWEET);
        request.id = status.getId();
        new UpdateStatusTask(this,pg).execute(request);
	}


    /**
     * Do the classical re-send thing by prefixing with 'RT'.
     * Opens an editor window first.
     * @param v
     */
    @SuppressWarnings("unused")
	public void classicRetweet(View v) {
		Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
		i.putExtra(getString(R.string.status), status);
		i.putExtra("op", getString(R.string.classicretweet));
		startActivity(i);

	}

    /**
     * Starts a view that shows the conversation around the current
     * status.
     * @param v
     */
    @SuppressWarnings("unused")
    public void threadView(View v) {
        TwitterHelper th = new TwitterHelper(ctx);

        Intent i = new Intent(getApplicationContext(),ThreadListActivity.class);
        i.putExtra("startId", status.getId());
        startActivity(i);
    }

    /**
     * Marks the current status as (non) favorite
     * @param v
     */
    @SuppressWarnings("unused")
    public void favorite(View v) {
        TwitterHelper th = new TwitterHelper(ctx);

        ImageButton favoriteButton = (ImageButton) findViewById(R.id.FavoriteButton);

        UpdateRequest request = new UpdateRequest(UpdateType.FAVORITE);
        request.status = status;
        request.view = favoriteButton;

        new UpdateStatusTask(this,pg).execute(request);

    }

    /**
     * Start sending a direct message to the user that sent this
     * status.
     * @param v
     */
    @SuppressWarnings("unused")
    public void directMessage(View v) {
        Intent i = new Intent(getApplicationContext(), NewTweetActivity.class);
        i.putExtra(getString(R.string.status), status);
        i.putExtra("op", getString(R.string.direct));
        startActivity(i);

    }


    //////////////// speak related stuff ////////////////////

    public void onInit(int status) {
        String statusString = status == 0 ? "Success" : "Failure";
        System.out.println("speak"+" onInit " + statusString);
    }

    public void onUtteranceCompleted(String utteranceId) {
        Log.i("speak", "Utterance done: " + utteranceId);

    }

    /**
     * Setup speak just in case we may need it.
     * If directly called from within speak() it will not work
     * because the onInit() listener is no ready early enough
     */
	public void setupspeak() {

		tts = new TextToSpeech(this,this);
        tts.setLanguage(Locale.US);
    }

    /**
     * Speak the current status via TTS
     */
    @SuppressWarnings("unused")
    public void speak(View v)
    {
        int res = tts.setOnUtteranceCompletedListener(this);
        if (res==TextToSpeech.ERROR) {
            Log.e("1TA", "Failed to set on utterance listener");
        }

        HashMap<String, String> ttsParams = new HashMap<String, String>();
		ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tweet_status_msg" + status.getUser().getScreenName());
		tts.speak(status.getText(), TextToSpeech.QUEUE_FLUSH, ttsParams);

	}


    //////////////// speak related stuff end ////////////////////

    /**
     * Translate the current status by calling Google translate
     * @param v
     */
    @SuppressWarnings("unused")
	public void translate(View v) {
		Translate.setHttpReferrer("http://bsd.de/zwitscher");
		try {
			// TODO get target language from system
			String result = Translate.execute(status.getText(), Language.AUTO_DETECT, Language.GERMAN);
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setMessage(result);
			builder.setTitle("Translation result");
			builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
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

    /**
     * Finishes this screen and returns to the list of statuses
     * @param v
     */
    @SuppressWarnings("unused")
	public void done(View v) {
        tts.shutdown();
		finish();
	}


    /**
     * Load thumbnails of linked images if the url in the status is recognized
     * as an image service
     * @param status Status to analyze
     * @return Bitmap to display
     * @todo Support multiple images in one status
     */
    private Bitmap loadThumbnail(Status status) {
        URL[] urlArray = status.getURLs();
        Set<String> urls = new HashSet<String>();
        if (urlArray!=null) {
            for (URL url : urlArray) {
                urls.add(url.toString());
            }
        }

        // Nothing provided by twitter, so parse the text
        if (urls.size()==0) {
            String[] tokens = status.getText().split(" ");
            for (String token : tokens) {
                if (token.startsWith("http://") || token.startsWith("https://")) {
                    urls.add(token);
                }
            }
        }
        if (urls.size()==0)
            return null;

        // We have urls, so check for picture services
        // TODO implement preview of multiple pictures.
        for (String url :  urls) {
            Log.d("One tweet","Url = " + url);
            String finalUrlString = null;
            if (url.contains("yfrog.com")) {
                finalUrlString = url + ".th.jpg";
            }
            else if (url.contains("twitpic.com")) {
                String tmp = url;
                tmp = tmp.substring(tmp.lastIndexOf("/")+1);
                finalUrlString = "http://twitpic.com/show/thumb/" + tmp;
            }
            else if (url.contains("plixi.com")) {
                finalUrlString = "http://api.plixi.com/api/tpapi.svc/imagefromurl?size=thumbnail&url=" +  url;
            }
            else
                return null;

            Log.i("loadThumbail","URL to load is " + finalUrlString);

            try {
                URL picUrl = new URL(finalUrlString);
                BufferedInputStream in = new BufferedInputStream(picUrl.openStream());
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                in.close();
                return bitmap;
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }



    /**
     * Background task to download the user profile images.
     */
    private class DownloadImageTask extends AsyncTask<User, Void,Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pg.setVisibility(ProgressBar.VISIBLE);
        }

        protected Bitmap doInBackground(User... users) {

            User user = users[0];
            PicHelper picHelper = new PicHelper();
            Bitmap bi = null;
            if (downloadPictures)
                bi = picHelper.fetchUserPic(user);
            else
                bi = picHelper.getBitMapForUserFromFile(user);
            return bi;
        }


        protected void onPostExecute(Bitmap result) {
        	if (result!=null)
        		userPictureView.setImageBitmap(result);
            pg.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    /**
     * Background task to download the thumbnails of linked images
     */
    private class DownloadThumbnailTask extends AsyncTask<Status,Void,Bitmap> {

        @Override
        protected Bitmap doInBackground(twitter4j.Status... statuses) {
            Bitmap b=null;
            if (downloadPictures)
                b = loadThumbnail(statuses[0]);
            return b;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap!=null)
                thumbnailView.setImageBitmap(bitmap);
        }
    }

}
