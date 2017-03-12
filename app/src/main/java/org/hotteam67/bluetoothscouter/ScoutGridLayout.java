package org.hotteam67.bluetoothscouter;

import android.app.Activity;
import android.support.v4.widget.TextViewCompat;
import android.widget.GridLayout;
import android.content.Context;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.Gravity;
import java.util.*;
import android.util.Log;


/**
 * Created by Jakob on 3/12/2017.
 */

public class ScoutGridLayout extends GridLayout
{
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_STRING = 2;
    public static final int TYPE_INTEGER = 3;
    public static final int TYPE_HEADER = 4;

    private static List<View> views = new ArrayList<>();

    public ScoutGridLayout(Context context)
    {
        super(context);
    }

    public void Build(LinkedHashMap<String, Integer> data)
    {
        views = new ArrayList<>();
        for (Map.Entry<String, Integer> d : data.entrySet())
        {
            View v = initializeView(d.getKey(), d.getValue());
            if (v != null)
                views.add(v);
            else
                l("Null returned on intializeView. Tag: " + d.getKey() + " Type: " + d.getValue());
        }
    }

    private View initializeView(String tag, Integer type)
    {
        View v = null;
        switch (type)
        {
            case TYPE_BOOLEAN:
                v = new CheckBox(getContext());
                ((CheckBox)v).setText(tag);
                break;
            case TYPE_STRING:
                v = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.layout_edittext, null);
                ((TextView)v.findViewById(R.id.textLabel)).setText(tag);

                TextViewCompat.setTextAppearance(
                        ((TextView)v.findViewById(R.id.textLabel)),
                        android.R.style.TextAppearance_DeviceDefault);
                break;
            case TYPE_INTEGER:
                v = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.layout_numberpicker, null);
                ((TextView)v.findViewById(R.id.numberLabel)).setText(tag);

                TextViewCompat.setTextAppearance(
                        ((TextView)v.findViewById(R.id.numberLabel)),
                        android.R.style.TextAppearance_DeviceDefault);


                ((NumberPicker)v.findViewById(R.id.numberPicker)).setMinValue(0);
                ((NumberPicker)v.findViewById(R.id.numberPicker)).setMaxValue(100);
                break;
            case TYPE_HEADER:

        }
        return v;
    }

    private void l(String s)
    {
        Log.d(ScoutActivity.TAG, s);
    }
}
