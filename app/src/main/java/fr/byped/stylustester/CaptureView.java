package fr.byped.stylustester;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class CaptureView extends View {
    private String mExampleString = "XXX"; // TODO: use a default from R.string...
    private int mExampleColor = Color.BLACK; // TODO: use a default from R.color...
    private float mExampleDimension = 16; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;
    private Bitmap mCache;
    private Canvas mContent;

    private TextPaint mTextPaint;
    private Paint     mPaint;
    private float mTextWidth;
    private float mTextHeight;

    public CaptureView(Context context) {
        super(context);
        init(null, 0);
    }

    public CaptureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CaptureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();

        minPressure = Float.MAX_VALUE;
        maxPressure = Float.MIN_VALUE;
        lastEvents = new ArrayList<>();
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        current = new SerEvent();

    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mContent = new Canvas();
        mContent.setBitmap(newBitmap);

        mCache = newBitmap;
        mCache.eraseColor(Color.TRANSPARENT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        // Create big status
        String status = "Tool: " + tool + "\n" +
                current.toString(0) + "\n";
        for (int i = 0; i < lastEvents.size(); i++)
        {
            status = status + lastEvents.get(i).toString(i+1) + "\n";
        }

        int y = 20;
        for (String line: status.split("\n")) {
            canvas.drawText(line, 20, y, mTextPaint);
            y += mTextPaint.descent() - mTextPaint.ascent();
        }


        // And draw the last point on screen
        mContent.drawCircle(current.pos.x, current.pos.y, current.pressure * 3, mPaint);
        canvas.drawBitmap(mCache, 0, 0, null);
    }


    public class SerEvent {
        float pressure;
        PointF pos;
        float orientation;


        public SerEvent() {
            pos = new PointF(0, 0);
        }
        public String toString(int index) {
            return String.format("[%d] pressure(%g), pos(%g,%g), orientation(%g°)", index, pressure, pos.x, pos.y, orientation);
        }
    }

    String tool = "";
    SerEvent current;
    ArrayList<SerEvent> lastEvents;
    String state = "";
    float maxPressure;
    float minPressure;
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        // Serialize the motion event so it's displayed on top of the screen
        boolean sw_androidOverFour = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH);
        if (sw_androidOverFour)
        { //for android 4.0+
            switch (e.getToolType(0)) {
                case MotionEvent.TOOL_TYPE_STYLUS: tool = "Stylus"; break;
                case MotionEvent.TOOL_TYPE_ERASER: tool = "Eraser"; break;
                default:                           tool = "Finger"; break;
            }
        } else {
            switch (e.getMetaState()) {
                case 512:  tool = "Stylus"; break;
                case 1024: tool = "Eraser"; break;
                default:   tool = "Finger"; break;
            }
        }

        current.pressure = e.getPressure();
        if (current.pressure > maxPressure) maxPressure = current.pressure;
        if (current.pressure < minPressure) minPressure = current.pressure;

        // Get the current position
        current.pos = new PointF(e.getX(0), e.getY(0));
        current.orientation = e.getOrientation();



        switch (e.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                state = "Down";
            break;
            case MotionEvent.ACTION_UP:
                state = "Up";
                break;
            case MotionEvent.ACTION_MOVE:
                state = "Move";
                lastEvents.clear();
                for (int i = 0; i < e.getHistorySize(); i++) {
                    SerEvent ev = new SerEvent();
                    ev.pressure = e.getHistoricalPressure(i);
                    ev.orientation = e.getHistoricalOrientation(i);
                    ev.pos = new PointF(e.getHistoricalX(i), e.getHistoricalY(i));
                    lastEvents.add(ev);
                }
                break;
        }
        invalidate();
        return true;
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }
}
