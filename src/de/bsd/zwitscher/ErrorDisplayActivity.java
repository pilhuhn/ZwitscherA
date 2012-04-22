package de.bsd.zwitscher;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

/**
 * Activity that displays error messages and
 * the original text to send if sending failed.
 * users can then copy&paste the text and retry.
 * Needs more work to allow for real 1-touch retry.
 * @author Heiko W. Rupp
 */
public class ErrorDisplayActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.error_layout);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String head = bundle.getString("e_head");
        String body = bundle.getString("e_body");
        String message = bundle.getString("e_text");

        if (head==null)
            head=getString(R.string.error_nothing_provided);
        if (body==null)
            body=getString(R.string.error_nothing_provided);
        if (message==null)
            message=getString(R.string.error_nothing_provided);

        TextView headView = (TextView) findViewById(R.id.error_head);
        headView.setText(Html.fromHtml(head));

        TextView bodyView = (TextView) findViewById(R.id.error_text);
        bodyView.setText(Html.fromHtml(body));

        TextView messageView = (TextView) findViewById(R.id.error_message);
        if (!message.equals("")) {
            messageView.setText(Html.fromHtml(message));
        } else {
            messageView.setEnabled(false);
            findViewById(R.id.copy_and_paste).setEnabled(false);
        }

    }

    public void doneButton(View v) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(ns);
        mNotificationManager.cancelAll();


        finish();
    }
}