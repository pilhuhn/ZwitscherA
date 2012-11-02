package de.bsd.zwitscher.helper;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import de.bsd.zwitscher.R;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class TextCompleterPrefActivity extends PreferenceActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.spell_checker_settings);
    }


}