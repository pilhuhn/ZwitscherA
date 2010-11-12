package de.bsd.zwitscher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.bsd.zwitscher.helper.SpannableBuilder;
import twitter4j.Status;
import twitter4j.User;

import java.util.List;

/**
 * Adapter for individual list rows of
 * the TweetList
 *
 * @author Heiko W. Rupp
 */
class StatusAdapter<T extends Status> extends ArrayAdapter<Status> {

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

        ViewHolder viewHolder;
//        Debug.startMethodTracing("sta");

        // Use ViewHolder pattern to only inflate once
        if (convertView ==null) {
            LayoutInflater li = (LayoutInflater) extContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.list_item,null);

            viewHolder = new ViewHolder();
            viewHolder.iv = (ImageView) convertView.findViewById(R.id.ListImageView);
            viewHolder.statusText = (TextView) convertView.findViewById(R.id.ListTextView);
            viewHolder.userInfo = (TextView) convertView.findViewById(R.id.ListUserView);
            viewHolder.timeClientInfo = (TextView) convertView.findViewById(R.id.ListTimeView);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (position %2 == 0)
            convertView.setBackgroundColor(Color.BLACK);
        else
            convertView.setBackgroundColor(Color.DKGRAY);


        Status status = items.get(position);
        Bitmap bi;

        SpannableBuilder builder = new SpannableBuilder();
        if (status.getRetweetedStatus()==null) {
            bi = ph.getBitMapForUserFromFile(status.getUser());
            builder.append(status.getUser().getName(), Typeface.BOLD);
            if (status.getInReplyToScreenName()!=null) {
                builder.append(" in reply to ",Typeface.NORMAL)
                    .append(status.getInReplyToScreenName(),Typeface.BOLD);
            }
        }
        else {
            bi = ph.getBitMapForUserFromFile(status.getRetweetedStatus().getUser());
             builder.append(status.getRetweetedStatus().getUser().getName(),Typeface.BOLD)
                .append(" resent by ", Typeface.NORMAL)
                .append(status.getUser().getName(),Typeface.BOLD);
        }

        if (bi!=null) {
            // TODO find an alternative for decoration of images, as this is expensive
//            bi = ph.decorate(bi,extContext,status.isFavorited(),status.getRetweetedStatus()!=null);
            viewHolder.iv.setImageBitmap(bi);
        }
        else {
            // underlying convertView seems to be reused, so default image is not loaded when bi==null
            viewHolder.iv.setImageBitmap(BitmapFactory.decodeResource(extContext.getResources(), R.drawable.user_unknown));
            // Trigger fetching of user pic in background
            if (downloadImages) {
                if (status.getRetweetedStatus()==null)
                    new TriggerPictureDownloadTask().execute(status.getUser());
                else
                    new TriggerPictureDownloadTask().execute(status.getRetweetedStatus().getUser());
            }
        }
        viewHolder.userInfo.setText(builder.toSpannableString());
        viewHolder.statusText.setText(status.getText());

        String text = th.getStatusDate(status) ;
        viewHolder.timeClientInfo.setText((text));
//Debug.stopMethodTracing();
        return convertView;
    }

    static class ViewHolder {
        ImageView iv;
        TextView statusText;
        TextView userInfo;
        TextView timeClientInfo;
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
