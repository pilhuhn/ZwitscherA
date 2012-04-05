package de.bsd.zwitscher.helper;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

/**
 * Helper to create a Spannable string.
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
@SuppressWarnings("unused")
public class SpannableBuilder {

    private int position = 0;
    private List<Holder> holderList;
    private Context context;


    /**
     * Create a new empty SpannableBuilder
     * @param context Context of caller
     */
    public SpannableBuilder(Context context) {
        this.context = context;
        holderList = new ArrayList<Holder>();
    }

    /**
     * Create a SpannableBuilder with some text and style
     * @param context Context of caller
     * @param text Text to render
     * @param typeface Style to use - typically a {@link  android.graphics.Typeface} constant
     */
    public SpannableBuilder(Context context, String text, int typeface) {
        this(context);
        append(text, typeface);
    }

    /**
     * Create a SpannableBuilder from a string resource and style
     * @param context Context of caller
     * @param resourceId Id of a string resource in res/strings.xml
     * @param typeface Style to use - typically a {@link android.graphics.Typeface} constant
     */
    public SpannableBuilder(Context context, int resourceId, int typeface) {
        this(context);
    }

    /**
     * Append text to the existing one
     * @param text New text to append
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
     * Append text to the existing one with default NORMAL typeface
     * @param text New text to append
     * @return this SpannableBuilder
     */
    public SpannableBuilder append(String text) {

        Holder h = new Holder(text,Typeface.NORMAL);
        holderList.add(h);

        return this;
    }

    /**
     * Append text to the existing one
     * @param resourceId Id of a string resource
     * @param typeface Style to use - typically a {@link  android.graphics.Typeface} constant
     * @return this SpannableBuilder
     */
    public SpannableBuilder append(int resourceId, int typeface) {
        String s = context.getString(resourceId);

        append(s,typeface);

        return this;
    }

    /**
     * Append a single space character in normal type
     * @return this SpannableBuilder
     */
    public SpannableBuilder appendSpace() {
        append(" ", Typeface.NORMAL);
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



    /**
     * Private class to store information about a span, until the string gets
     * finally assembled.
     */
    private static class Holder {
        String text;
        int style;
        int len=0;

        Holder(String text, int typeface) {
            this.text = text;
            this.style = typeface;
            if (text!=null)
                this.len = text.length();
        }
    }
}
