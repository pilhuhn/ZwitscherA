package de.bsd.zwitscher;

import android.app.ListActivity;
import android.content.Intent;
import android.view.View;

/**
 * Superclass for Tabs wit list stuff
 * @author Heiko W. Rupp
 */
public abstract class AbstractListActivity extends ListActivity {

    /**
     * Scrolls to top, called from the ToTop button
     * @param v
     */
    @SuppressWarnings("unused")
    public void scrollToTop(View v) {
        getListView().setSelection(0);
    }

    /**
     * Called from the post button
     * @param v
     */
    @SuppressWarnings("unused")
    public void post(View v) {
        Intent i = new Intent(this, NewTweetActivity.class);
        startActivity(i);
    }

    abstract public void reload(View v);
}
