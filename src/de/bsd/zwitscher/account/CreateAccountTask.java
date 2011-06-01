package de.bsd.zwitscher.account;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;
import de.bsd.zwitscher.TabWidget;
import de.bsd.zwitscher.TwitterHelper;

/**
 * Create an account
 * @author Heiko W. Rupp
 */
public class CreateAccountTask extends AsyncTask<Void,Void,String> {

    ProgressDialog dialog;
    Activity context;
    private String username;
    private String password;
    private String service;
    private boolean shouldSwitch;

    public CreateAccountTask(Activity context,String username, String password, String service, boolean shouldSwitch) {
        this.context=context;
        this.username = username;
        this.password = password;
        this.service = service;
        this.shouldSwitch = shouldSwitch;
    }

    protected String doInBackground(Void... voids) {
        TwitterHelper th = new TwitterHelper(context,null);
        Account account = null;
        try {
            account = th.generateAccountWithXauth(username, password, service, shouldSwitch);
            AccountHolder.getInstance().setAccount(account);
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return "Login failed  " + e.getLocalizedMessage();
        }

    }

    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setIndeterminate(true);
        dialog.setTitle("Syncing...");
        dialog.setCancelable(false);
        dialog.show();

    }

    protected void onPostExecute(String message) {
        dialog.hide();
        dialog.cancel();

        Toast.makeText(context,message,Toast.LENGTH_LONG).show();

		Intent i = new Intent().setClass(context, TabWidget.class);
		context.startActivity(i);

        context.finish();
    }
}
