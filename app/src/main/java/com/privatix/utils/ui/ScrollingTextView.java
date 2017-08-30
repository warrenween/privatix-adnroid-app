package com.privatix.utils.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * Created by Lotar on 24.11.2015.
 */
public class ScrollingTextView extends TextView implements Runnable {

    private static final float DEFAULT_SPEED = 15.0f;

    private Scroller scroller;
    private float speed = DEFAULT_SPEED;
    private boolean continuousScrolling = true;

    public ScrollingTextView(Context context) {
        super(context);
        setup(context);
    }

    public ScrollingTextView(Context context, AttributeSet attributes) {
        super(context, attributes);
        setup(context);
    }

    private void setup(Context context) {
        scroller = new Scroller(context, new LinearInterpolator());
        setScroller(scroller);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (scroller.isFinished()) {
            scroll();
        }
    }

    private void scroll() {
        int viewWidth = getWidth();
        int visibleHeight = viewWidth - getPaddingLeft() - getPaddingRight();
        //int lineHeight = get();

        int offset = -1 * visibleHeight;
        int distance = visibleHeight + getLineCount() * viewWidth;
        int duration = (int) (distance * speed);

        scroller.startScroll(offset, 0, distance, 0, duration);

        if (continuousScrolling) {
            post(this);
        }
    }

    @Override
    public void run() {
        if (scroller.isFinished()) {
            scroll();
        } else {
            post(this);
        }
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean isContinuousScrolling() {
        return continuousScrolling;
    }

    public void setContinuousScrolling(boolean continuousScrolling) {
        this.continuousScrolling = continuousScrolling;
    }
}
