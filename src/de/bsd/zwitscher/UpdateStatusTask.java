package de.bsd.zwitscher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
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

        } else if (result.getUpdateType() == UpdateType.FAVORITE) {
            ImageButton favoriteButton = (ImageButton) result.view;
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
        int icon = R.drawable.icon; // TODO create small version for status bar
        Notification notification = new Notification(icon,result.getUpdateType().toString() + " failed",System.currentTimeMillis());

        String head = "<b>" + result.getUpdateType() + " failed:</b> ";
        String text =  result.getMessage() + "<br/>";
        if (result.getUpdateType()==UpdateType.UPDATE)
            text += result.getUpdate().getStatus();
        if (result.getUpdateType()==UpdateType.DIRECT)
            text += result.getOrigMessage();

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.error_layout);
        //contentView.setImageViewResource(R.id.image, R.drawable.notification_image);
        contentView.setTextViewText(R.id.error_head, Html.fromHtml(head));
        contentView.setTextViewText(R.id.error_text, Html.fromHtml(text));
        notification.contentView = contentView;

        Intent intent = new Intent(context,LoginActivity.class);
        PendingIntent pintent = PendingIntent.getActivity(context,0,intent,0);
        notification.contentIntent =pintent;

        mNotificationManager.notify(1,notification); // TODO better id generation ?
    }

}
