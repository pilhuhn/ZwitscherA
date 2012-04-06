package de.bsd.zwitscher.helper;


import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * View to display User images in timeline including some
 * decorators for retweets or such
 *
 * @author Heiko W. Rupp
 */
public class UserImageView extends ImageView {

    private static final Rect RT_RECT = new Rect(80-39, 80-39, 80, 80);
    private static final Rect RECT64 = new Rect(0, 0, 64, 64);
    private static final Rect RECT72 = new Rect(0, 0, 72, 72);
    private static final Rect RECT80 = new Rect(0, 0, 80, 80);
    private Bitmap baseBitmap;
    private boolean isFavorite;
    private boolean isRetweet;
    private static Xfermode SRC_OVER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private Bitmap rtBitmap;
    private Paint paint = new Paint(); // TODO move to constructor?


    public UserImageView(Context context) {
        super(context);
    }

    public UserImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(context,attrs);
        setLayoutParams(params);
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


     //   p.setFilterBitmap(false);

        if (isRetweet) {
            if (rtBitmap!=null)
                canvas.drawBitmap(baseBitmap,null, RECT64, paint);
            else
                canvas.drawBitmap(baseBitmap,null,RECT72, paint);
        }
        else
            canvas.drawBitmap(baseBitmap,null,RECT80, paint);

        paint.setXfermode(SRC_OVER);

        if (isFavorite) {
            paint.setColor(Color.GREEN);
            canvas.drawCircle(5f,5f,10, paint);
        }

        if (isRetweet) {
            if (rtBitmap != null) {
                paint.setColor(Color.MAGENTA);
                canvas.drawRect(38f,38f,80f,80f, paint);

                canvas.drawBitmap(rtBitmap, null, RT_RECT, paint);
            } else {
                paint.setColor(Color.MAGENTA);
                canvas.drawRect(70f, 70f, 80f,80f, paint);
            }
        }
    }
}
