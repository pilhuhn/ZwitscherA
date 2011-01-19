package de.bsd.zwitscher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.helper.PicHelper;
import de.bsd.zwitscher.helper.SpannableBuilder;
import de.bsd.zwitscher.helper.TriggerPictureDownloadTask;
import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.TwitterResponse;
import twitter4j.User;

import java.util.List;

/**
 * Adapter for individual list rows of
 * the TweetList
 *
 * @author Heiko W. Rupp
 */
class StatusAdapter<T extends TwitterResponse> extends ArrayAdapter<T> {

    private List<T> items;
    PicHelper ph;
    TwitterHelper th;
    private Context extContext;
    boolean downloadImages;


    public StatusAdapter(Context context, Account account, int textViewResourceId, List<T> objects) {
        super(context, textViewResourceId, objects);
        extContext = context;
        items = objects;
        ph = new PicHelper();
        th = new TwitterHelper(context, account);
        downloadImages = new NetworkHelper(context).mayDownloadImages();

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
//        Debug.startMethodTracing("sta");

        // Use ViewHolder pattern to only inflate once
        if (convertView ==null) {
            LayoutInflater li = (LayoutInflater) extContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.tweet_list_item,parent,false);

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


        T response = items.get(position);
        Bitmap bi;

        SpannableBuilder builder = new SpannableBuilder(extContext);
        User userOnPicture;
        String statusText;

        if (response instanceof Status) {
            Status status = (Status)response;
            if (status.getRetweetedStatus()==null) {
                userOnPicture =status.getUser();
                builder.append(status.getUser().getName(), Typeface.BOLD);
                if (status.getInReplyToScreenName()!=null) {
                    builder.appendSpace();
                    builder.append(R.string.in_reply_to,Typeface.NORMAL)
                        .appendSpace()
                        .append(status.getInReplyToScreenName(), Typeface.BOLD);
                }
            }
            else {
                userOnPicture = status.getRetweetedStatus().getUser();
                builder.append(status.getRetweetedStatus().getUser().getName(),Typeface.BOLD)
                    .appendSpace()
                    .append(R.string.resent_by, Typeface.NORMAL)
                    .appendSpace()
                    .append(status.getUser().getName(), Typeface.BOLD);
            }
            statusText = status.getText();

        }
        else if (response instanceof DirectMessage) {
            DirectMessage msg = (DirectMessage) response;
            userOnPicture = msg.getSender();
            statusText=msg.getText();
            builder.append(R.string.From,Typeface.NORMAL)
                .appendSpace()
                .append(msg.getSender().getName(),Typeface.BOLD)
                .appendSpace()
                .append(R.string.to,Typeface.NORMAL)
                .appendSpace()
                .append(msg.getRecipient().getName(),Typeface.BOLD);
        }
        else
            throw new IllegalArgumentException("Unknown type " + response);


        bi = ph.getBitMapForUserFromFile(userOnPicture);
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
                new TriggerPictureDownloadTask().execute(userOnPicture);
            }
        }

        viewHolder.userInfo.setText(builder.toSpannableString());
        viewHolder.statusText.setText(statusText);
        String text = th.getStatusDate(response) ;
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

}
