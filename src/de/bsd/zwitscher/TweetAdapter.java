package de.bsd.zwitscher;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.SpannableBuilder;
import de.bsd.zwitscher.helper.UserImageView;
import twitter4j.Status;

/**
 * Adapter for individual list rows of
 * the TweetList for Tweet objects (= query results)
 *
 * @author Heiko W. Rupp
 */
class TweetAdapter<T extends Status> extends AbstractAdapter<T> {


    public TweetAdapter(Context context, Account account, int textViewResourceId, List<T> objects) {
        super(context, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
//        Debug.startMethodTracing("sta");

        // Use ViewHolder pattern to only inflate once
        if (convertView ==null) {

            convertView = inflater.inflate(R.layout.tweet_list_item,null);

            viewHolder = new ViewHolder();
            viewHolder.iv = (UserImageView) convertView.findViewById(R.id.ListImageView);
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


        Status tweet = items.get(position);
        Bitmap bi;

        SpannableBuilder builder = new SpannableBuilder(extContext);
        String statusText;

        builder.append(tweet.getUser().getName(), Typeface.BOLD);
/* TODO what is / was this?
        if (tweet.getRecipientScreenName()!=null) {
            builder.appendSpace();
            builder.append(R.string.to,Typeface.NORMAL)
                .appendSpace()
                .append(tweet.getRecipientScreenName(), Typeface.BOLD);
        }
*/
        statusText = tweet.getText();


        bi = ph.getBitMapForScreenNameFromFile(tweet.getUser().getScreenName());
        if (bi!=null) {
            viewHolder.iv.setImageBitmap(bi);
        }
        else {
            // underlying convertView seems to be reused, so default image is not loaded when bi==null
            viewHolder.iv.setImageBitmap(BitmapFactory.decodeResource(extContext.getResources(), R.drawable.user_unknown));
        }

        viewHolder.userInfo.setText(builder.toSpannableString());
        viewHolder.statusText.setText(statusText);
        String text = tweet.getCreatedAt().toString(); // TODO format
        viewHolder.timeClientInfo.setText((text));
//Debug.stopMethodTracing();
        return convertView;
    }

}
