package de.bsd.zwitscher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

/**
 * Adapter for individual list rows of
 * the TweetList
 *
 * The Adapter's getView() method is repeatedly called when
 * the user is scrolling through the list, so it needs to be quick.
 * So one thing is to use the ViewHolder pattern in order to prevent
 * the repeated inflation of views, which is expensive. When running
 * through the adapter, we record the list of read statuses in a list and
 * have the outer activity persiste that result, when the adapter is
 * about to be replaced or destroyed (in onPause).
 *
 * The loading of user images is done in an AsyncTask, that gets the
 * image view passed. We need to tag that view, as the user may scroll
 * quickly and in that case the image required on a certain position may
 * already be a different one. So the background task compares the tag
 * on the view with the expected one and skips setting the image if the
 * tags don't match.
 *
 * @author Heiko W. Rupp
 */
class StatusAdapter<T extends TwitterResponse> extends AbstractAdapter<T> {

    private TwitterHelper th;
    private UserDisplayMode userDisplay;
    public List<Long> readIds;
    private long oldLast;
    private final boolean downloadImages;
    public Set<Long> newOlds = new HashSet<Long>();


    public StatusAdapter(Context context, Account account, int textViewResourceId, List<T> objects, long oldLast, List<Long> readIds) {
        super(context, textViewResourceId, objects);
        this.readIds = readIds;
        this.oldLast = oldLast;
        th = new TwitterHelper(context, account);
        NetworkHelper networkHelper = new NetworkHelper(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String tmp = preferences.getString("screen_name_overview","USER");
        userDisplay = UserDisplayMode.valueOf(tmp);
        downloadImages = networkHelper.mayDownloadImages();

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
            viewHolder.iv.setFocusable(false);
            viewHolder.statusText = (TextView) convertView.findViewById(R.id.ListTextView);
            viewHolder.userInfo = (TextView) convertView.findViewById(R.id.ListUserView);
            viewHolder.timeClientInfo = (TextView) convertView.findViewById(R.id.ListTimeView);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
//        System.out.println("IV:" + viewHolder.iv + " @ " + position + " | " + viewHolder.iv.getTag());

        T response = items.get(position);
        boolean isOld = false;

        if (response instanceof Status) {

            Status status = (Status) response;
            long oid;
            oid = status.getId();


            long rtStatusId = -1;
            if (status.isRetweet()) {
                rtStatusId = status.getRetweetedStatus().getId();
            }
            if (oid <= oldLast ) {
                convertView.setBackgroundColor(Color.BLACK);
                isOld = true;
            } else if (readIds.contains(oid)) {
                convertView.setBackgroundColor(Color.rgb(0,0,40)); // todo debug color
                isOld=true;
            } else if (newOlds.contains(oid)) {
                convertView.setBackgroundColor(Color.rgb(0,0,60));
                isOld=true;
            } else if (status.isRetweet() && (readIds.contains(rtStatusId) || newOlds.contains(rtStatusId))) {
                convertView.setBackgroundColor(Color.rgb(40,0,0));
                isOld=true;
            }

            // If the status is a RT and we also have the original one
            // on file, then mark the original as seen too
            if (status.isRetweet() && ! readIds.contains(rtStatusId) && !newOlds.contains(rtStatusId)) {
                // Only color when we have the original one too
                Status rtStatus = th.getStatusById(rtStatusId,null,false,false,true);
                if (rtStatus!=null) {
                    newOlds.add(rtStatusId);
                    readIds.add(rtStatusId); // If we have seen the retweet once it is enough
                }
            }
            newOlds.add(status.getId());
/*  If this is enabled, RTs are marked as one goes, but the coloring is too aggressive when slightly scrolling around
    Need to still decide if I want that
            if (rtStatusId!=-1)
                newOlds.add(rtStatusId);
*/
        }

        // Color the lines
        if (!isOld) {
            if (position % 2 == 0)
                convertView.setBackgroundColor(Color.rgb(15,40,20));
            else
                convertView.setBackgroundColor(Color.DKGRAY);
        }


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

        viewHolder.iv.setTag(userOnPicture.getScreenName()); // tag to remember which image should be shown
        TriggerPictureDownloadTask downloadTask = new TriggerPictureDownloadTask(viewHolder.iv, userOnPicture, downloadImages, status);
        try {
            if (Build.VERSION.SDK_INT<16) {
                downloadTask.execute();
            }
            else {
                downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } catch (RejectedExecutionException e) {
            // This means, the threadpool is full - in that case
            // the image is just not set/downloaded. This will happen
            // on further scrolling
            Log.w("StatusAdapter","Could not execute the picture download: " + e.getMessage());
        }


        viewHolder.userInfo.setText(builder.toSpannableString());
        viewHolder.statusText.setText(statusText);
        String text = th.getStatusDate(response) ;
        viewHolder.timeClientInfo.setText((text));
        if (!isOld) {
                viewHolder.userInfo.setTextColor(Color.LTGRAY);
                viewHolder.statusText.setTextColor(Color.LTGRAY);
                viewHolder.timeClientInfo.setTextColor(Color.LTGRAY);
        } else {
            viewHolder.userInfo.setTextColor(Color.GRAY);
            viewHolder.statusText.setTextColor(Color.GRAY);
            viewHolder.timeClientInfo.setTextColor(Color.GRAY);
        }
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

        if (status==null || status.getText()==null)
            return "";

        String[] tokens;
        if (status.isRetweet()) {
            tokens = status.getRetweetedStatus().getText().split(" ");
        } else {
            tokens = status.getText().split(" ");
        }
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            boolean found=false;
            if (status.getMediaEntities()!=null) {
                for (MediaEntity me : status.getMediaEntities()) {
                    String meURL = me.getURL();
                    if (meURL != null && meURL.equals(token) && me.getDisplayURL() != null) {
                        builder.append(me.getDisplayURL());
                        found=true;
                        break;
                    }
                }
            }
            if (!found && status.getURLEntities()!=null) {
                for (URLEntity ue : status.getURLEntities()) {
                    String ueURL = ue.getURL();
                    if (ueURL !=null && ueURL.equals(token) && ue.getDisplayURL()!=null) {
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
