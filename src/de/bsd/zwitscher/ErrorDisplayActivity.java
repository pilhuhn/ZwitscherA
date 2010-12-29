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
 *  TODO: Document this
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
            head="-nothing provided-";
        if (body==null)
            body="-nothing provided-";
        if (message==null)
            message="-nothing provided-";

        TextView headView = (TextView) findViewById(R.id.error_head);
        headView.setText(Html.fromHtml(head));

        TextView bodyView = (TextView) findViewById(R.id.error_text);
        bodyView.setText(Html.fromHtml(body));

        TextView messageView = (TextView) findViewById(R.id.error_message);
        messageView.setText(Html.fromHtml(message));

    }

    public void doneButton(View v) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(ns);
        mNotificationManager.cancelAll();


        finish();
    }
}