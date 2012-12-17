package de.bsd.zwitscher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.other.ReadItLaterStore;
import twitter4j.StatusUpdate;
import twitter4j.media.MediaProvider;

/**
* Task that does async updates to the server
*
* @author Heiko W. Rupp
*/
class UpdateStatusTask extends AsyncTask<UpdateRequest,Void,UpdateResponse> {

    private Context context;
    private ProgressBar progressBar;
    private Account account;

    public UpdateStatusTask(Context context, ProgressBar progressBar, Account account) {
        this.context = context;
        this.progressBar = progressBar;
        this.account = account;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (progressBar!=null)
            progressBar.setVisibility(ProgressBar.VISIBLE);
    }

    @Override
    protected UpdateResponse doInBackground(UpdateRequest... requests) {
        TwitterHelper th = new TwitterHelper(context.getApplicationContext(), account);

        UpdateRequest request = requests[0];
        UpdateResponse ret=null;

        MediaProvider mediaProvider = th.getMediaProvider();
        if (request.updateType==UpdateType.UPDATE && request.picturePath!=null) {
            if (mediaProvider.equals(MediaProvider.TWITTER)) {
                request.statusUpdate.setMedia(new File(request.picturePath));
            }
        }

        NetworkHelper nh = new NetworkHelper(context);
        if (!nh.isOnline()) {

            // We are not online, queue the request
           return queueUpUpdate(request, "Offline - Queued for later sending");
        }


        switch (request.updateType) {
            case UPDATE:
                if (request.picturePath!=null) {
                    if (mediaProvider.equals(MediaProvider.TWITTER)) {
                        request.statusUpdate.setMedia(new File(request.picturePath));
                    }
                    else {
                        StatusUpdate statusUpdate = request.statusUpdate;
                        String tmp = th.postPicture(request.picturePath, statusUpdate.getStatus()); // TODO remove place holder here

                        String res = statusUpdate.getStatus() + " " + tmp;
                        StatusUpdate up = new StatusUpdate(res);
                        up.setAnnotations(statusUpdate.getAnnotations());
                        up.setInReplyToStatusId(statusUpdate.getInReplyToStatusId());
                        up.setLocation(statusUpdate.getLocation());
                        up.setPlaceId(statusUpdate.getPlaceId());
                        up.setPossiblySensitive(statusUpdate.isPossiblySensitive());
                        up.setDisplayCoordinates(statusUpdate.isDisplayCoordinates());

                        request.statusUpdate = up;
                    }
                }
                ret = th.updateStatus(request);
                ret.someBool = request.someBool;
                break;
            case FAVORITE:
                ret = th.favorite(request);
                break;
            case DIRECT:
                ret = th.direct(request);
                break;
            case RETWEET:
                ret = th.retweet(request);
                break;
            case UPLOAD_PIC:
                if (request.picturePath!=null) {
                    String url = th.postPicture(request.picturePath, request.message);
                    if (url!=null) {
                        ret = new UpdateResponse(request.updateType,request.view,url);
                        ret.setSuccess();
                    }
                    else {
                        ret = new UpdateResponse(request.updateType,request.view,"");
                        ret.setFailure();
                        ret.setMessage("Picture upload failed");
                    }
                    ret.someBool = request.someBool;
                }
                break;
            case LATER_READING:

                ReadItLaterStore store = new ReadItLaterStore(request.extUser,request.extPassword);
                String result = store.store(request.status,!account.isStatusNet(),request.url);
                boolean success;
                success = result.startsWith("200");

                ret = new UpdateResponse(request.updateType,success,result);
                break;
            case REPORT_AS_SPAMMER:
                th.reportAsSpammer(request.id);
                ret = new UpdateResponse(request.updateType,true,"OK");
                break;
            case ADD_TO_LIST:
                success = th.addUserToLists(request.userId,(int)request.id);
                ret = new UpdateResponse(request.updateType,success,"Added");
                break;
            case FOLLOW_UNFOLLOW:
                success = th.followUnfollowUser(request.userId,request.someBool);
                ret = new UpdateResponse(request.updateType,success,"Follow/Unfollow set");
                break;
            case DELETE_STATUS:
                success = th.deleteStatus(request.id);
                ret = new UpdateResponse(request.updateType,success,"Status deleted");
                break;
            default:
                throw new IllegalArgumentException(context.getString(R.string.update_not_yet_supported, request.updateType));
        }

        if (ret!=null && (ret.getStatusCode()==502||ret.getStatusCode()==503))
            ret = queueUpUpdate(request,context.getString(R.string.queueing, ret.getMessage()));

        return ret;
    }

    /**
     * Queue up the Update request for later sending.
     * @param request Request to queue up
     * @param message Reason why this was queued
     * @return a new surrogate request to continue processing with
     * @see de.bsd.zwitscher.helper.FlushQueueTask
     */
    private UpdateResponse queueUpUpdate(UpdateRequest request, String message) {
        TweetDB tdb = TweetDB.getInstance(context.getApplicationContext());

        UpdateResponse response = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(request);
            out.close();

            tdb.persistUpdate(account.getId(), bos.toByteArray());

            response = new UpdateResponse(UpdateType.QUEUED,true,message);
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            response = new UpdateResponse(UpdateType.QUEUED,false,e.getMessage());
        }
        return response;
    }

    protected void onPostExecute(UpdateResponse result) {
        if (progressBar!=null)
            progressBar.setVisibility(ProgressBar.INVISIBLE);

        if (result==null) {
            Toast.makeText(context,"No result - should not happen",Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getUpdateType()==UpdateType.UPLOAD_PIC) {
            TextView textView = (TextView) result.view;
            if (textView==null)
                return;

            if (textView.getText().length()==0)
                textView.setText(result.getMessage());
            else
                textView.append(" " + result.getMessage());

        } else if (result.getUpdateType() == UpdateType.FAVORITE) {
            ImageView favoriteButton = (ImageView) result.view;
            if (favoriteButton==null || result.status == null)
                return;

            try {
                if (result.status.isFavorited())
                    favoriteButton.setImageResource(R.drawable.favorite_on);
                else
                    favoriteButton.setImageResource(R.drawable.favorite_off);
                }
            catch (Exception e) {
                Log.i("UpdateStatusTask", "Favorite button seems to be gone");
            }
        } else if (result.getUpdateType()==UpdateType.UPDATE) {
            if (result.isSuccess() && result.getPicturePath()!=null) {
                if (result.someBool) { // only delete if some bool is set, which means that the we allow to remove the picture
                    File file = new File(result.getPicturePath());
                    if (file.exists()) {
                       // file.delete(); // TODO only for camera shots, not for Gallery images
                    }
                }
            }
        }


        if (result.isSuccess())
            Toast.makeText(context.getApplicationContext(), result.getMessage(), Toast.LENGTH_LONG).show();
        else
            createNotification(result);
    }

    /**
     * Create a notification for the (Android) system wide message center and put a message there
     * @param result Result of a status update
     */
    private void createNotification(UpdateResponse result) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
        mNotificationManager.cancelAll();
        int icon = R.drawable.icon; // TODO create small version for status bar
        Notification notification = new Notification(icon,result.getUpdateType().toString() + " failed",System.currentTimeMillis());

        String head =  result.getUpdateType() + " failed:";
        String text =  result.getMessage();
        String message ="";
        if (result.getUpdateType()==UpdateType.QUEUED)
            message = "Queueing failed : "+ result.getMessage();
        if (result.getUpdateType()==UpdateType.UPDATE)
            message= result.getUpdate().getStatus();
        if (result.getUpdateType()==UpdateType.DIRECT)
            message= result.getOrigMessage();

        //contentView.setImageViewResource(R.id.image, R.drawable.notification_image);

        Intent intent = new Intent(context,ErrorDisplayActivity.class);
        Bundle bundle=new Bundle(3);
        bundle.putString("e_head", head);
        bundle.putString("e_body", text);
        bundle.putString("e_text", message);
        intent.putExtras(bundle);
        PendingIntent pintent = PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_CANCEL_CURRENT);

        notification.setLatestEventInfo(context,
                head,
                text,
                pintent);
        mNotificationManager.notify(3,notification);
    }

}
