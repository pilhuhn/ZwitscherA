package de.bsd.zwitscher;

import android.app.Activity;
import android.os.Bundle;

/**
 * Just display the help page
 * @author Heiko W. Rupp
 */
public class HelpActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.help);
    }
}