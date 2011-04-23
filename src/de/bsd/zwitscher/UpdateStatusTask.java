package de.bsd.zwitscher;

import de.bsd.zwitscher.account.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.bsd.zwitscher.other.ReadItLaterStore;

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

        switch (request.updateType) {
            case UPDATE:
                ret = th.updateStatus(request);
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
                    String url = th.postPicture(request.picturePath);
                    if (url!=null) {
                        ret = new UpdateResponse(request.updateType,request.view,url);
                        ret.setSuccess();
                    }
                    else {
                        ret = new UpdateResponse(request.updateType,request.view,"");
                        ret.setFailure();
                        ret.setMessage("Picture upload failed");
                    }
                }
                break;
            case LATER_READING:

                ReadItLaterStore store = new ReadItLaterStore(request.extUser,request.extPassword);
                String result = store.store(request.status,!account.isStatusNet(),request.url);
                boolean success;
                if (result.startsWith("200")) {
                    success = true;
                }
                else {
                    success = false;
                }

                ret = new UpdateResponse(request.updateType,success,result);
                break;
            default:
                throw new IllegalArgumentException("Update type not supported yet : " + request.updateType);
        }
        return ret;
    }

    protected void onPostExecute(UpdateResponse result) {
        if (progressBar!=null)
            progressBar.setVisibility(ProgressBar.INVISIBLE);

        if (result.getUpdateType()==UpdateType.UPLOAD_PIC) {
            TextView textView = (TextView) result.view;
            if (textView==null)
                return;

            if (textView.getText().length()==0)
                textView.setText(result.getMessage());
            else
                textView.append(" " + result.getMessage());

        } else if (result.getUpdateType() == UpdateType.FAVORITE) {
            ImageButton favoriteButton = (ImageButton) result.view;
            if (favoriteButton==null || result.status == null)
                return;

            if (result.status.isFavorited())
                favoriteButton.setImageResource(R.drawable.favorite_on);
            else
                favoriteButton.setImageResource(R.drawable.favorite_off);
        }


        if (result.isSuccess())
            Toast.makeText(context.getApplicationContext(), result.getMessage(), Toast.LENGTH_LONG).show();
        else
            createNotification(result);
    }

    private void createNotification(UpdateResponse result) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
        mNotificationManager.cancelAll();
        int icon = R.drawable.icon; // TODO create small version for status bar
        Notification notification = new Notification(icon,result.getUpdateType().toString() + " failed",System.currentTimeMillis());

        String head =  result.getUpdateType() + " failed:";
        String text =  result.getMessage();
        String message ="";
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
