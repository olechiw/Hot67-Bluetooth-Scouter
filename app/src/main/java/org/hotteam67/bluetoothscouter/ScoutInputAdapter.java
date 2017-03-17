package org.hotteam67.bluetoothscouter; /**
 * Created by Jakob on 3/17/2017.
 */

import android.support.v4.widget.TextViewCompat;
import android.widget.BaseAdapter;
import android.widget.*;
import android.app.*;
import android.os.*;
import android.content.Context;
import android.view.*;

import java.util.*;
import android.util.*;

import org.hotteam67.bluetoothscouter.R;


public class ScoutInputAdapter extends BaseAdapter
{
    private List<View> views = new ArrayList<>();

    Context context;

    public ScoutInputAdapter(Context c)
    {
        context = c;
    }

    public int getCount()
    {
        return views.size();
    }

    public Object getItem(int i)
    {
        return views.get(i);
    }

    public long getItemId(int i)
    {
        return 0;
    }

    @Override
    public View getView(int position, View currentView, ViewGroup parent)
    {
        if (position < views.size() && views.get(position) != null)
            return views.get(position);
        else
            return currentView;
    }

    public void SetViews(List<View> v)
    {
        views = v;
    }
}
