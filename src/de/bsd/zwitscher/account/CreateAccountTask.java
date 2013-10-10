package de.bsd.zwitscher.account;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;
import de.bsd.zwitscher.MainActivity;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TwitterHelper;

/**
 * Create an account
 * @author Heiko W. Rupp
 */
class CreateAccountTask extends AsyncTask<Void,Void,String> {

    private ProgressDialog dialog;
    private final Activity context;
    private final String username;
    private final String password;
    private final Account.Type service;
    private final boolean shouldSwitch;
    private final String url;

    public CreateAccountTask(Activity context, String username, String password, Account.Type service, boolean shouldSwitch, String url) {
        this.context=context;
        this.username = username;
        this.password = password;
        this.service = service;
        this.shouldSwitch = shouldSwitch;
        this.url = url;
    }

    protected String doInBackground(Void... voids) {
        TwitterHelper th = new TwitterHelper(context,null);
        try {
            Account account = th.generateAccountWithXauth(username, password, service, shouldSwitch, url);
            AccountHolder.getInstance(context).setAccount(account);
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return context.getString(R.string.login_failed) + " " + e.getLocalizedMessage();
        }

    }

    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setIndeterminate(true);
        dialog.setTitle(context.getString(R.string.logging_in));
        dialog.setCancelable(false);
        dialog.show();

    }

    protected void onPostExecute(String message) {
        dialog.hide();
        dialog.cancel();

        Toast.makeText(context,message,Toast.LENGTH_LONG).show();

		Intent i = new Intent().setClass(context, MainActivity.class);
		context.startActivity(i);

        context.finish();
    }
}
