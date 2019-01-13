package org.hotteam67.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import java.util.concurrent.Callable;

/**
 * A layout that will intercept all of its touches based on a condition
 */
public class InterceptAllLayout extends RelativeLayout {
    public InterceptAllLayout(Context c)
    {
        super(c);
    }

    public InterceptAllLayout(Context c, AttributeSet attrs)
    {
        super(c, attrs);
    }

    private Callable<Boolean> interceptCondition;

    /**
     * A condition for whether the touches will be intercepted
     * @param condition the condition that will be checked
     */
    public void setInterceptCondition(Callable<Boolean> condition)
    {
        interceptCondition = condition;
    }

    private Runnable interceptEvent;

    /**
     * An event to run when a touch is intercepted and the condition is met
     * @param event the event to run
     */
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
