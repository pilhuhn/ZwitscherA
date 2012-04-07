package de.bsd.zwitscher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.helper.SpannableBuilder;
import de.bsd.zwitscher.helper.TriggerPictureDownloadTask;
import de.bsd.zwitscher.helper.UserImageView;
import twitter4j.DirectMessage;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.TwitterResponse;
import twitter4j.URLEntity;
import twitter4j.User;

import java.util.List;

/**
 * Adapter for individual list rows of
 * the TweetList
 *
 * @author Heiko W. Rupp
 */
class StatusAdapter<T extends TwitterResponse> extends AbstractAdapter<T> {

    private TwitterHelper th;
    private boolean downloadImages;
    private UserDisplayMode userDisplay;


    public StatusAdapter(Context context, Account account, int textViewResourceId, List<T> objects) {
        super(context, textViewResourceId, objects);
        th = new TwitterHelper(context, account);
        downloadImages = new NetworkHelper(context).mayDownloadImages();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String tmp = preferences.getString("screen_name_overview","USER");
        userDisplay = UserDisplayMode.valueOf(tmp);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
//        Debug.startMethodTracing("sta");

        // Use ViewHolder pattern to only inflate once
        if (convertView ==null) {
            convertView = inflater.inflate(R.layout.tweet_list_item,parent,false);

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


        T response = items.get(position);
        Bitmap bi;

        SpannableBuilder builder = new SpannableBuilder(extContext);
        User userOnPicture;
        String statusText;

        if (response instanceof Status) {
            Status status = (Status)response;
            if (status.getRetweetedStatus()==null) {
                userOnPicture =status.getUser();
                appendUserInfo(userOnPicture,builder);

                if (status.getInReplyToScreenName()!=null) {
                    builder.appendSpace();
                    builder.append(R.string.in_reply_to, Typeface.NORMAL).appendSpace();
                    builder.append(status.getInReplyToScreenName(), Typeface.BOLD); // we only have the screen name here
                }
            }
            else {
                userOnPicture = status.getRetweetedStatus().getUser();
                appendUserInfo(userOnPicture,builder);
                builder.appendSpace()
                    .append(R.string.resent_by, Typeface.NORMAL)
                    .appendSpace();
                appendUserInfo(status.getUser(), builder);
            }
            statusText = textWithReplaceTokens(status);

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


        // User images will all be loaded asynchronously
        // It this is a status, then pass it along as well.
        Status status=null;
        if (response instanceof Status)
            status = (Status) response;

        new TriggerPictureDownloadTask(viewHolder.iv, userOnPicture, downloadImages, status).execute();

        viewHolder.userInfo.setText(builder.toSpannableString());
        viewHolder.statusText.setText(statusText);
        String text = th.getStatusDate(response) ;
        viewHolder.timeClientInfo.setText((text));
//Debug.stopMethodTracing();
        return convertView;
    }


    private void appendUserInfo(User user,SpannableBuilder builder) {
        switch (userDisplay) {
            case USER:
                builder.append(user.getName(), Typeface.BOLD);
                break;
            case SCREENNAME:
               builder.append(user.getScreenName());
                break;
            case BOTH:
                builder.append(user.getName(),Typeface.BOLD)
                .append(" (").append(user.getScreenName()).append(")");
        }
    }


    private String textWithReplaceTokens(Status status) {

        String[] tokens = status.getText().split(" ");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            boolean found=false;
            if (status.getMediaEntities()!=null) {
                for (MediaEntity me : status.getMediaEntities()) {
                    if (me.getURL().toString().equals(token)) {
                        builder.append(me.getDisplayURL());
                        found=true;
                        break;
                    }
                }
            }
            if (!found && status.getURLEntities()!=null) {
                for (URLEntity ue : status.getURLEntities()) {
                    if (ue.getURL()!=null && ue.getURL().toString().equals(token)) {
                        builder.append(ue.getDisplayURL());
                        found=true;
                        break;
                    }
                }
            }
            if (!found)
                builder.append(token);

            builder.append(" "); // TODO not at the end
        }
        return builder.toString();

    }

}
