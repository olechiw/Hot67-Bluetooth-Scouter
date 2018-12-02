package org.hotteam67.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class InterceptAllLayout extends RelativeLayout {
    public InterceptAllLayout(Context c)
    {
        super(c);
    }

    public InterceptAllLayout(Context c, AttributeSet attrs)
    {
        super(c, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true; // With this i tell my layout to consume all the touch events from its childs
    }
}
