package de.bsd.zwitscher.helper;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.service.textservice.SpellCheckerService;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class TextCompletionService extends SpellCheckerService {

    @Override
    public Session createSession() {
        System.out.println("TCS: createSession");
        return new TextCompletionSession();
    }

    private class TextCompletionSession extends Session {
        @Override
        public void onCreate() {
            System.out.println("TCS created");
        }

        @Override
        public SuggestionsInfo onGetSuggestions(TextInfo textInfo, int suggestionsLimit) {
            System.out.println("onGetSu: " + textInfo.getText());
            return new SuggestionsInfo(0,new String[] { "lala","foo","bar","@foo","#hash"});
        }

        @Override
        public SentenceSuggestionsInfo[] onGetSentenceSuggestionsMultiple(TextInfo[] textInfos, int suggestionsLimit) {
            StringBuilder builder = new StringBuilder("InfoIn: ");
            for (TextInfo info : textInfos) {
                builder.append(info.getText());
                builder.append("|");
            }
            System.out.println("onGetSentenceSu: " + builder.toString() );
            return super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit);    // TODO: Customise this generated block
        }
    }
}
