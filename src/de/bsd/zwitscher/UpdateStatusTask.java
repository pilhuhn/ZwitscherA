package de.bsd.zwitscher;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.Toast;
import twitter4j.StatusUpdate;

/**
* Task that does async updates to the server
*
* @author Heiko W. Rupp
*/
class UpdateStatusTask extends AsyncTask<StatusUpdate,Void,UpdateResponse> {

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
    protected UpdateResponse doInBackground(StatusUpdate... statusUpdates) {
        update=statusUpdates[0];
        TwitterHelper th = new TwitterHelper(context.getApplicationContext());
        UpdateResponse ret = th.updateStatus(update);
        return ret;
    }

    protected void onPostExecute(UpdateResponse result) {
        if (progressBar!=null)
            progressBar.setVisibility(ProgressBar.INVISIBLE);
        if (result.isSuccess())
            Toast.makeText(context.getApplicationContext(), result.getMessage(), Toast.LENGTH_LONG).show();
        else
            createNotification(result);
    }

    private void createNotification(UpdateResponse result) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
        int icon = R.drawable.icon; // TODO create small version for status bar
        Notification notification = new Notification(icon,result.getUpdateType().toString(),System.currentTimeMillis());

        notification.setLatestEventInfo(context,
                result.getUpdateType() + " failed",
                result.getMessage() + " <br/> ",
                null); // TODO pending ...

        mNotificationManager.notify(1,notification); // TODO better id generation ?
    }

}
