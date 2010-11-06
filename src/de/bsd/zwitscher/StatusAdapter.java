package de.bsd.zwitscher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Debug;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import twitter4j.Status;
import twitter4j.User;

import java.text.DateFormat;
import java.util.List;

/**
 * Adapter for individual list rows of
 * the TweetList
 *
 * @author Heiko W. Rupp
 */
class StatusAdapter<T extends Status> extends ArrayAdapter<Status> {

    private static final String STRONG = "<b>";
    private static final String STRONG_END = "</b>";
    private List<Status> items;
    PicHelper ph;
    TwitterHelper th;
    private Context extContext;
    boolean downloadImages;

    public StatusAdapter(Context context, int textViewResourceId, List<Status> objects) {
        super(context, textViewResourceId, objects);
        extContext = context;
        items = objects;
        ph = new PicHelper();
        th = new TwitterHelper(context);
        downloadImages = new NetworkHelper(context).mayDownloadImages();

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView; //= super.getView(position, convertView, parent);
        if (view==null) {
            LayoutInflater li = (LayoutInflater) extContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(R.layout.list_item,null);
        }
//Debug.startMethodTracing("sta");
        if (position %2 == 0)
            view.setBackgroundColor(Color.BLACK);
        else
            view.setBackgroundColor(Color.DKGRAY);

        Status status = items.get(position);

        ImageView iv = (ImageView) view.findViewById(R.id.ListImageView);
        TextView statusText = (TextView) view.findViewById(R.id.ListTextView);
        TextView userInfo = (TextView) view.findViewById(R.id.ListUserView);
        TextView timeClientInfo = (TextView) view.findViewById(R.id.ListTimeView);

        Bitmap bi;
        String userName ;
        if (status.getRetweetedStatus()==null) {
            bi = ph.getBitMapForUserFromFile(status.getUser());
            userName = STRONG + status.getUser().getName() + STRONG_END;
            if (status.getInReplyToScreenName()!=null) {
                userName += " in reply to " + STRONG + status.getInReplyToScreenName() + STRONG_END;
            }
        }
        else {
            bi = ph.getBitMapForUserFromFile(status.getRetweetedStatus().getUser());
            userName = STRONG + status.getRetweetedStatus().getUser().getName() + STRONG_END +
                    " resent by " + STRONG + status.getUser().getName() + STRONG_END;
        }

        if (bi!=null) {
            bi = ph.decorate(bi,extContext,status.isFavorited(),status.getRetweetedStatus()!=null);
            iv.setImageBitmap(bi);
        }
        else {
            // underlying view seems to be reused, so default image is not loaded when bi==null
            iv.setImageBitmap(BitmapFactory.decodeResource(extContext.getResources(), R.drawable.user_unknown));
            // Trigger fetching of user pic in background
            if (downloadImages) {
                if (status.getRetweetedStatus()==null)
                    new TriggerPictureDownloadTask().execute(status.getUser());
                else
                    new TriggerPictureDownloadTask().execute(status.getRetweetedStatus().getUser());
            }

        }
        userInfo.setText(Html.fromHtml(userName)); // TODO replace with something better
//        userInfo.setText((userName));
        statusText.setText(status.getText());

        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String text = th.getStatusDate(status) ;
        timeClientInfo.setText((text));
//Debug.stopMethodTracing();
        return view;
    }

    private class TriggerPictureDownloadTask extends AsyncTask<User,Void,Void> {

        @Override
        protected Void doInBackground(User... users) {
            User user = users[0];
            PicHelper ph = new PicHelper();
            ph.fetchUserPic(user);

            return null;
        }
    }
}
