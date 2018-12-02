package org.hotteam67.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class InterceptAllLayout extends RelativeLayout {
    public InterceptAllLayout(Context c)
    {
        super(c);
    }

    public InterceptAllLayout(Context c, AttributeSet attrs)
    {
        super(c, attrs);
    }

    Callable<Boolean> interceptCondition;
    public void setInterceptCondition(Callable<Boolean> condition)
    {
        interceptCondition = condition;
    }

    Runnable interceptEvent;
    public void setInterceptEvent(Runnable event)
    {
        interceptEvent = event;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            if (interceptCondition.call())
            {
                // With this i tell my layout to consume all the touch events from its childs
                interceptEvent.run();
                return true;
            }
        }
        catch (Exception ignored) {}
        return false;
    }
}
