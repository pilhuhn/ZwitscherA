package de.bsd.zwitscher.helper;


import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class TextCompletionListener implements SpellCheckerSession.SpellCheckerSessionListener {

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        // TODO: Customise this generated block
        for (SuggestionsInfo info: results) {
            System.out.println("i: " + info.describeContents()+ ", c=" + info.getSuggestionsCount());
        }
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        // TODO: Customise this generated block
        for (SentenceSuggestionsInfo info: results) {
            System.out.println("is: " + info.describeContents() + ", c=" + info.getSuggestionsCount());
        }

    }
}
