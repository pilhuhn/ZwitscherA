package de.bsd.zwitscher;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * A spinner, that does not have the flaw that for programmatic invocations it will directly
 * call the onSelectedListener. Instead it defines a onClick listener that is then called when
 * the user selects an item. This even works when the user re-selects that item again.
 *
 * Copied from http://stackoverflow.com/a/8714434/100957
 *
 */
public class WorkingSpinner extends Spinner {

    private OnItemClickListener onItemClickListener;

    public WorkingSpinner(Context context) {
        super(context);
    }

    public WorkingSpinner(Context context, int mode) {
        super(context, mode);
    }

    public WorkingSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WorkingSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WorkingSpinner(Context context, AttributeSet attrs, int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener l) {
        // DO NOT call super.setOn... this will bomb
        this.onItemClickListener = l;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (this.onItemClickListener != null) {
            this.onItemClickListener.onItemClick(this, this.getSelectedView(), which, this.getSelectedItemId());
        }
    }
}
