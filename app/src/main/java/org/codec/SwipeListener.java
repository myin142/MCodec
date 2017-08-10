package org.codec;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Created by min on 02/08/2017.
 */

public class SwipeListener implements OnTouchListener{

    private Context mainCxt;
    private GestureDetector gDetector;

    public SwipeListener(Context cxt){
        mainCxt = cxt;
        gDetector = new GestureDetector(mainCxt, new GestureListener());
    }

    // On Touch, Call GestureListener
    public boolean onTouch(View v, MotionEvent e){
        return gDetector.onTouchEvent(e);
    }

    private class GestureListener extends SimpleOnGestureListener{
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        public boolean onDown(MotionEvent e){
            return true;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        result = true;
                    }
                }
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                    result = true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    public void onSwipeRight(){}
    public void onSwipeLeft(){}
    public void onSwipeBottom(){}
    public void onSwipeTop(){}

}
