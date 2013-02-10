package de.bsd.zwitscher.helper;


import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
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
    private static final Rect RECT20 = new Rect(0, 0, 20, 20);
    private static final Rect RECT64 = new Rect(0, 0, SIXTYFOUR, SIXTYFOUR);
    private static final Rect RECT72 = new Rect(0, 0, SEVENTY_TWO, SEVENTY_TWO);
    private static final Rect RECT80 = new Rect(0, 0, EIGHTY, EIGHTY);
    private static final Rect RT_UNKNOWN_RECT = new Rect(70,70,80,80);
    private Bitmap baseBitmap;
    private boolean isFavorite;
    private boolean isRetweet;
    private static Xfermode SRC_OVER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private Bitmap rtBitmap;
    private Paint paint = new Paint();
    private Bitmap unknownUserBitmap;
    private Bitmap favBitmap;
    private Rect favRect ;
    private final Rect unknownUserBitmapRect;
    private Rect baseBitmapRect;


    public UserImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(context,attrs);
        setLayoutParams(params);
        unknownUserBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.user_unknown);
        unknownUserBitmapRect = new Rect(0,0,unknownUserBitmap.getWidth(),unknownUserBitmap.getHeight());
        favBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.favorite_on);
        favRect = new Rect(0,0,favBitmap.getWidth(),favBitmap.getHeight());
    }

    /**
     * Set the bitmap for the user of the tweet
     * @param baseBitmap Bitmap to display. Null is allowed
     */
    public void setImageBitmap(Bitmap baseBitmap) {
        this.baseBitmap = baseBitmap;
        if (baseBitmap!=null) {
            baseBitmapRect = new Rect(0,0,baseBitmap.getWidth(),baseBitmap.getHeight());
        }
        else {
            baseBitmapRect = null;
        }
    }

    /**
     * Set the bitmap of the retweeting user
     * @param bitmap Bitmap to display (in small). Null is allowed
     */
    public void setRtImage(Bitmap bitmap) {
        rtBitmap = bitmap;
    }

    /**
     * If the argument is true, the View will get a tiny favorite marker
     * @param favorited Is the matching tweet a Favorite?
     */
    public void markFavorite(boolean favorited) {
        isFavorite = favorited;

    }

    /**
     * If the argument is true, show the rtBitmap (or a marker if not rtBitmap is set).
     * @param isRetweet Is the matching tweet a Retweet?
     */
    public void markRetweet(boolean isRetweet) {
        this.isRetweet =isRetweet;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        float factor = getHeight() / 64.0f;


        Rect SRC;
        if (baseBitmap==null) {
            baseBitmap= unknownUserBitmap;
            SRC = unknownUserBitmapRect;
        } else {
            SRC = baseBitmapRect;
        }

            // First draw the tweet's user bitmap
        if (isRetweet) {
            if (rtBitmap!=null)
                canvas.drawBitmap(baseBitmap,SRC, scale(RECT64,factor), paint);
            else
                canvas.drawBitmap(baseBitmap,SRC,scale(RECT72,factor), paint);
        }
        else
            canvas.drawBitmap(baseBitmap,SRC,scale(RECT80,factor), paint);

        // And then overlay with markers
        paint.setXfermode(SRC_OVER);

        if (isFavorite) {
//            paint.setColor(Color.GREEN);
//            canvas.drawCircle(0f,0f,10, paint);
            canvas.drawBitmap(favBitmap,favRect,RECT20,paint);
        }

        if (isRetweet) {
            if (rtBitmap != null) {
                Rect RT_SRC = new Rect(0,0,rtBitmap.getWidth(),rtBitmap.getHeight());
                canvas.drawBitmap(rtBitmap, RT_SRC, scale(RT_RECT,factor), paint);
            } else {
                paint.setColor(Color.MAGENTA);
                canvas.drawRect(scale(RT_UNKNOWN_RECT,factor), paint);
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
