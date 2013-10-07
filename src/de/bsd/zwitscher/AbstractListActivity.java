package de.bsd.zwitscher;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;

/**
 * Superclass for Tabs with list stuff
 * @author Heiko W. Rupp
 */
@Deprecated
public abstract class AbstractListActivity extends ListActivity {

    ProgressBar progressBar;
    TextView titleTextBox;
    TwitterHelper th;
    TweetDB tdb;
    Account account;
    TabWidget parent;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity theParent = getParent();
        if (theParent instanceof TabWidget) {
            parent = (TabWidget) theParent;
            progressBar = parent.pg;
            titleTextBox = parent.titleTextBox;
        }

        account = AccountHolder.getInstance(this).getAccount();

        th = new TwitterHelper(this, account);
        tdb = TweetDB.getInstance(this);

    }

    protected void onResume() {
        super.onResume();
        Activity theParent = getParent();
        if (theParent instanceof TabWidget) {
            ((TabWidget) theParent).setInnerActivity(this);
        }
    }

    /**
     * Scrolls to top, called from the ToTop button
     * @param v Button that was pressed
     */
    @SuppressWarnings("unused")
    public void scrollToTop(View v) {
        getListView().setSelection(0);
    }

    /**
     * Called from the post button
     * @param v Button that was pressed
     */
    @SuppressWarnings("unused")
    public void post(View v) {
        Intent i = new Intent(this, NewTweetActivity.class);
        startActivity(i);
    }

    abstract public void reload(View v);

    /**
     * Finish the activity. Called from the done button
     * @param v Button that was invoked
     */
    @SuppressWarnings("unused")
    public void done(View v) {
        finish();
    }
}
