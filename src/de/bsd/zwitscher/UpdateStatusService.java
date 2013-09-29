package de.bsd.zwitscher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.other.ReadItLaterStore;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.media.MediaProvider;

/**
* Intent service that does async updates to the server
*
* @author Heiko W. Rupp
*/
public class UpdateStatusService extends IntentService {

    public UpdateStatusService() {
        super("UpdateStatusService");
    }

    UpdateStatusService(String name) {
        super(name);
    }

    public static void sendUpdate(Context caller, Account account, UpdateRequest request) {
        Intent intent = new Intent(caller,UpdateStatusService.class);
        intent.putExtra("account",account);
        intent.putExtra("request",(Parcelable)request);
        caller.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        Bundle bundle = intent.getExtras();

        UpdateRequest request = bundle.getParcelable("request");
        Account account = bundle.getParcelable("account");
        Context context = getApplicationContext();

        TwitterHelper th = new TwitterHelper(context.getApplicationContext(), account);

        UpdateResponse ret;

        MediaProvider mediaProvider = th.getMediaProvider();
        if (request.updateType==UpdateType.UPDATE && request.picturePath!=null) {
            if (mediaProvider.equals(MediaProvider.TWITTER)) {
                request.statusUpdate.setMedia(new File(request.picturePath));
            }
        }

        NetworkHelper nh = new NetworkHelper(context);
        if (!nh.isOnline()) {

            // We are not online, queue the request
           ret = queueUpUpdate(request, context.getString(R.string.queueing), account);
           createNotification(ret);
           stopSelf(); // TODO correct?
        }

        try {

            switch (request.updateType) {
                case UPDATE:
                    if (request.picturePath!=null) {
                        if (mediaProvider.equals(MediaProvider.TWITTER)) {
                            request.statusUpdate.setMedia(new File(request.picturePath));
                        }
                        else {
                            // External picture provider like yfrog
                            StatusUpdate statusUpdate = request.statusUpdate;
                            String tmp = th.postPicture(request.picturePath, statusUpdate.getStatus());
                            // if tmp == null picture upload failed - it is probably a good idea to inform the user.
                            if (tmp==null) {
                                UpdateResponse tmpResponse = new UpdateResponse(request.updateType,"Picture upload failed - was the image too big?");
                                tmpResponse.setFailure();
                                createNotification(tmpResponse);
                                return;

                            }
                            String res = statusUpdate.getStatus() + " " + tmp;
                            StatusUpdate up = new StatusUpdate(res);
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
                    else {
                        ret = new UpdateResponse(request.updateType,request.view,"");
                        ret.setFailure();
                        ret.setMessage("No picture passed");
                    }
                    break;
                case LATER_READING:

                    ReadItLaterStore store = new ReadItLaterStore(request.extUser,request.extPassword);
                    String result = store.store(request.status,!account.isStatusNet(),request.url);

                    ret = new UpdateResponse(request.updateType, result);
                    break;
                case REPORT_AS_SPAMMER:
                    th.reportAsSpammer(request.id);
                    ret = new UpdateResponse(request.updateType, context.getString(R.string.ok));
                    break;
                case ADD_TO_LIST:
                    th.addUserToLists(request.userId,(int)request.id);
                    ret = new UpdateResponse(request.updateType, context.getString(R.string.added));
                    break;
                case REMOVE_FROM_LIST:
                    th.removeUserFromList(request.userId,(int)request.id);
                    ret = new UpdateResponse(request.updateType, context.getString(R.string.removed));
                    break;
                case FOLLOW_UNFOLLOW:
                    th.followUnfollowUser(request.userId,request.someBool);
                    ret = new UpdateResponse(request.updateType, context.getString(R.string.follow_unfollow_set));
                    break;
                case DELETE_STATUS:
                    th.deleteStatus(request.id);
                    ret = new UpdateResponse(request.updateType, context.getString(R.string.status_deleted));
                    break;
                default:
                    throw new IllegalArgumentException(context.getString(R.string.update_not_yet_supported, request.updateType));
            }

            if (ret!=null && (ret.getStatusCode()==502||ret.getStatusCode()==503||ret.getStatusCode()==420)) {
                ret = queueUpUpdate(request,context.getString(R.string.queueing_code, ret.getMessage()), account);
            }
        }
        catch (TwitterException e) {
            BugSenseHandler.sendExceptionMessage("UpdateStatus",request.getUpdateType().toString(),e);
            ret = queueUpUpdate(request,context.getString(R.string.queueing), account);
        }

        onPostExecute(ret);
        stopSelf();
    }

    /**
     * Queue up the Update request for later sending.
     *
     * @param request Request to queue up
     * @param message Reason why this was queued
     * @param account
     * @return a new surrogate request to continue processing with
     * @see de.bsd.zwitscher.helper.FlushQueueTask
     */
    private UpdateResponse queueUpUpdate(UpdateRequest request, String message, Account account) {
        TweetDB tdb = TweetDB.getInstance(getApplicationContext());

        UpdateResponse response;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(request);
            out.close();

            tdb.persistUpdate(account.getId(), bos.toByteArray());

            response = new UpdateResponse(UpdateType.QUEUED, message);
            response.setSuccess(); // This is the success of the queueing, not the inner job.
        } catch (IOException e) {
            BugSenseHandler.sendException(e);
            response = new UpdateResponse(UpdateType.QUEUED, e.getMessage());
        }
        createNotification(response);
        return response;
    }

    protected void onPostExecute(UpdateResponse result) {

        if (result==null) {
            Toast.makeText(getApplicationContext(),"No result - should not happen",Toast.LENGTH_SHORT).show();
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


        createNotification(result);
    }

    /**
     * Create a notification for the (Android) system wide message center and put a message there
     * @param result Result of a status update
     */
    private void createNotification(UpdateResponse result) {
        String ns = Context.NOTIFICATION_SERVICE;
        final NotificationManager mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(ns);
        mNotificationManager.cancelAll();
        int icon = R.drawable.zwitscher_notif;
        Notification notification;
        if (Build.VERSION.SDK_INT<16) {
            if (!result.isSuccess()) {
                notification = new Notification(icon,result.getUpdateType().toString() + " failed",System.currentTimeMillis());
            } else {
                notification = new Notification(icon,result.getUpdateType().toString(), System.currentTimeMillis());
            }
        }
        else {
            Notification.Builder builder = new Notification.Builder(getApplicationContext());
            if (result.isSuccess()) {
                builder.setAutoCancel(true);
            }
            builder.setContentTitle("Zwitscher update");
            String text = "Result of " + result.getUpdateType().toString() + " is success: " + result.isSuccess();
            if (Build.VERSION.SDK_INT<16) {
                builder.setContentText(text);
            } else {
                builder.setSubText(text);
            }
            if (result.getUpdateType()==UpdateType.QUEUED) {
                builder.setTicker("Queued for later sending");
            } else {
                builder.setTicker(getString(R.string.sending_succeeded));
            }
            builder.setSmallIcon(R.drawable.zwitscher_notif);
            if (Build.VERSION.SDK_INT<16) {
                notification = builder.getNotification();
            } else {
                notification = builder.build();
            }
        }


        PendingIntent pintent;
        if (result.isSuccess()) {

            pintent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            // Only set the Error display intent when this is no success
            String head =  result.getUpdateType() + " failed:";
            String text =  result.getMessage();
            String message ="";
            if (result.getUpdateType()==UpdateType.QUEUED)
                message = "Queueing failed : "+ result.getMessage();
            if (result.getUpdateType()==UpdateType.UPDATE) {
                if (result.getUpdate()!=null && result.getUpdate().getStatus()!=null) {
                    message= result.getUpdate().getStatus();
                }
            }
            if (result.getUpdateType()==UpdateType.DIRECT)
                message= result.getOrigMessage();

            Intent intent = new Intent(getApplicationContext(),ErrorDisplayActivity.class);
            Bundle bundle=new Bundle(3);
            bundle.putString("e_head", head);
            bundle.putString("e_body", text);
            bundle.putString("e_text", message);
            intent.putExtras(bundle);
            pintent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            notification.setLatestEventInfo(getApplicationContext(),
                    head,
                    text,
                    pintent);

        }

        mNotificationManager.notify(3,notification);

        if (result.isSuccess()) {
            // remove notifcation after some time secs


            long delayInMilliseconds = 5000;
            final HandlerThread t = new HandlerThread("bla");
            t.start();

            Runnable r = new Runnable() {

                public void run() {
                    mNotificationManager.cancel(3);
                    t.quit();
                }
            };

            Handler h = new Handler(t.getLooper());
            boolean success = h.postDelayed(r, delayInMilliseconds);

        }
    }

}
