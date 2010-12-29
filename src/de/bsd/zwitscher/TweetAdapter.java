package de.bsd.zwitscher;

import java.util.List;

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
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.helper.PicHelper;
import de.bsd.zwitscher.helper.SpannableBuilder;
import twitter4j.Tweet;
import twitter4j.User;

/**
 * Adapter for individual list rows of
 * the TweetList for Tweet objects (= query results)
 *
 * @author Heiko W. Rupp
 */
class TweetAdapter<T extends Tweet> extends ArrayAdapter<T> {

    private List<T> items;
    PicHelper ph;
    TwitterHelper th;
    private Context extContext;
    boolean downloadImages;


    public TweetAdapter(Context context, int textViewResourceId, List<T> objects) {
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
            convertView = li.inflate(R.layout.tweet_list_item,null);

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


        Tweet tweet = items.get(position);
        Bitmap bi;

        SpannableBuilder builder = new SpannableBuilder(extContext);
        User userOnPicture=null;
        String statusText;

        builder.append(tweet.getFromUser(), Typeface.BOLD);
        if (tweet.getToUser()!=null) {
            builder.appendSpace();
            builder.append(R.string.to,Typeface.NORMAL)
                .appendSpace()
                .append(tweet.getToUser(), Typeface.BOLD);
        }
        statusText = tweet.getText();



        bi = ph.getBitMapForScreenNameFromFile(tweet.getFromUser());
        if (bi!=null) {
            viewHolder.iv.setImageBitmap(bi);
        }
        else {
            // underlying convertView seems to be reused, so default image is not loaded when bi==null
            viewHolder.iv.setImageBitmap(BitmapFactory.decodeResource(extContext.getResources(), R.drawable.user_unknown));
            // Trigger fetching of user pic in background
//            if (downloadImages) {
//                new TriggerPictureDownloadTask().execute(userOnPicture);
//            }
        }

        viewHolder.userInfo.setText(builder.toSpannableString());
        viewHolder.statusText.setText(statusText);
//        String text = th.getStatusDate(response) ;
        String text = tweet.getCreatedAt().toString(); // TODO format
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
