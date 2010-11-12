package de.bsd.zwitscher.helper;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to create a Spannable strings.
 * Those are strings that get 'markup' on portions of them
 * like different styles.
 *
 * This class can be used to create the equivalent of
 * <pre>
 * String html = "<b>bold text</b>";
 * String txt = Html.fromHtml(html);
 * TextView.setText(txt);
 * </pre>
 * as {@link android.text.Html}.fromHtml() is relatively expensive.
 *
 * @author Heiko W. Rupp
 */
public class SpannableBuilder {

    private int position = 0;
    List<Holder> holderList;


    /**
     * Create a new empty SpannableBuilder
     */
    public SpannableBuilder() {
        holderList = new ArrayList<Holder>();
    }

    /**
     * Create a SpannableBuilder with some text and style
     * @param text Text to render
     * @param typeface Style to use - typically a {@link  android.graphics.Typeface} constant
     * like Typeface.BOLD
     */
    public SpannableBuilder(String text,int typeface) {
        this();
        append(text,typeface);
    }

    /**
     * Append text to the existing one
     * @param text new text
     * @param typeface Style to use - typically a {@link  android.graphics.Typeface} constant
     * like Typeface.BOLD
     * @return this SpannableBuilder
     */
    public SpannableBuilder append(String text,int typeface) {

        Holder h = new Holder(text,typeface);
        holderList.add(h);

        return this;

    }

    /**
     * Render a SpannableString from this SpannableBuilder
     * @return a SpannableString containing all the Spans appended.
     */
    public SpannableString toSpannableString() {

        StringBuilder sb = new StringBuilder();
        for (Holder h : holderList) {
            sb.append(h.text);
        }
        SpannableString spannableString = new SpannableString(sb.toString());
        for (Holder h : holderList) {
            StyleSpan span = new StyleSpan(h.style);
            spannableString.setSpan(span,position,position+ h.len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            position += h.len;
        }
        return spannableString;
    }


    private static class Holder {
        String text;
        int style;
        int len;

        Holder(String text, int typeface) {
            this.text = text;
            this.style = typeface;
            this.len = text.length();
        }
    }
}
