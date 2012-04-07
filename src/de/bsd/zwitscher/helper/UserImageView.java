package de.bsd.zwitscher.helper;


import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;
import de.bsd.zwitscher.R;

/**
 * View to display User images in timeline including some
 * decorators for retweets or such
 *
 * @author Heiko W. Rupp
 */
public class UserImageView extends ImageView {

    private static final int EIGHTY = 64;
    private static final int FOO = 33; // 80-39
    private static final Rect RT_RECT = new Rect(FOO, FOO, EIGHTY, EIGHTY);
    private static final int SIXTYFOUR = 51;
    private static final int SEVENTY_TWO = 58;
    private static final Rect RECT64 = new Rect(0, 0, SIXTYFOUR, SIXTYFOUR);
    private static final Rect RECT72 = new Rect(0, 0, SEVENTY_TWO, SEVENTY_TWO);
    private static final Rect RECT80 = new Rect(0, 0, EIGHTY, EIGHTY);
    private Bitmap baseBitmap;
    private boolean isFavorite;
    private boolean isRetweet;
    private static Xfermode SRC_OVER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private Bitmap rtBitmap;
    private Paint paint = new Paint(); // TODO move to constructor?
    private Bitmap unknownUserBitmap;


//    public UserImageView(Context context) {
//        super(context);
//    }

    public UserImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(context,attrs);
        setLayoutParams(params);
        unknownUserBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.user_unknown);
    }

    public void setImageBitmap(Bitmap baseBitmap) {
        this.baseBitmap = baseBitmap;
    }

    public void setRtImage(Bitmap bitmap) {
        rtBitmap = bitmap;
    }

    public void markFavorite(boolean favorited) {
        isFavorite = favorited;

    }

    public void markRetweet(boolean isRetweet) {
        this.isRetweet =isRetweet;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        float factor = getHeight() / 64.0f;

     //   p.setFilterBitmap(false);
        if (baseBitmap==null)
            baseBitmap= unknownUserBitmap;

        if (isRetweet) {
            if (rtBitmap!=null)
                canvas.drawBitmap(baseBitmap,null, scale(RECT64,factor), paint); // TODO precompute rect once
            else
                canvas.drawBitmap(baseBitmap,null,scale(RECT72,factor), paint);
        }
        else
            canvas.drawBitmap(baseBitmap,null,scale(RECT80,factor), paint);

        paint.setXfermode(SRC_OVER);

        if (isFavorite) {
            paint.setColor(Color.GREEN);
            canvas.drawCircle(0f,0f,10, paint);
        }

        if (isRetweet) {
            if (rtBitmap != null) {
                canvas.drawBitmap(rtBitmap, null, scale(RT_RECT,factor), paint);
            } else {
                paint.setColor(Color.MAGENTA);
                canvas.drawRect(70f, 70f, 80f,80f, paint);
            }
        }
    }

    RectF scale(Rect in,float factor) {

        RectF out = new RectF(in);
        out.left *= factor;
        out.right *= factor;
        out.bottom *= factor;
        out.top *= factor;

        return out;
    }
}
