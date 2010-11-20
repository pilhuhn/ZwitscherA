package de.bsd.zwitscher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import twitter4j.StatusUpdate;

/**
* Task that does async updates to the server
*
* @author Heiko W. Rupp
*/
class UpdateStatusTask extends AsyncTask<UpdateRequest,Void,UpdateResponse> {

    StatusUpdate update;
    private Context context;
    private ProgressBar progressBar;

    public UpdateStatusTask(Context context,ProgressBar progressBar) {
        this.context = context;
        this.progressBar = progressBar;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (progressBar!=null)
            progressBar.setVisibility(ProgressBar.VISIBLE);
    }

    @Override
    protected UpdateResponse doInBackground(UpdateRequest... requests) {
        TwitterHelper th = new TwitterHelper(context.getApplicationContext());

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
                }
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
            CharSequence text = textView.getText();
            text = result.getMessage() + " " + text;
            textView.setText(text);

        }
        if (result.isSuccess())
            Toast.makeText(context.getApplicationContext(), result.getMessage(), Toast.LENGTH_LONG).show();
        else
            createNotification(result);
    }

    private void createNotification(UpdateResponse result) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
        int icon = R.drawable.icon; // TODO create small version for status bar
        Notification notification = new Notification(icon,result.getUpdateType().toString() + " failed",System.currentTimeMillis());

        Intent intent = new Intent(context,OneTweetActivity.class);
        PendingIntent pintent = PendingIntent.getActivity(context,0,intent,0);

        String text = result.getMessage() + "<br/>";
        if (result.getUpdateType()==UpdateType.UPDATE)
            text += result.getUpdate().getStatus();

        notification.setLatestEventInfo(context,
                result.getUpdateType() + " failed",
                text,
                pintent);

        mNotificationManager.notify(1,notification); // TODO better id generation ?
    }

}
