package org.hotteam67.bluetoothscouter;

import android.app.Activity;
import android.support.v4.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
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
import android.util.DisplayMetrics;
import android.util.AttributeSet;


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

    public ScoutGridLayout(Context context, AttributeSet set)
    {
        super(context, set);
    }

    public ScoutGridLayout(Context context, AttributeSet set, int defStyle)
    {
        super(context, set, defStyle);
    }

    public void Build(LinkedHashMap<String, Integer> data)
    {
        views = new ArrayList<>();
        for (Map.Entry<String, Integer> d : data.entrySet())
        {
            l("Initializing view: " + d.getKey());
            View v = initializeView(d.getKey(), d.getValue());
            if (v != null)
                views.add(v);
            else
                l("Null returned on intializeView(). Tag: " + d.getKey() + " Type: " + d.getValue());
        }

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;

        int i = 0;
        int rows = 0;
        List<View> rowViews;
        while (i < views.size())
        {
            rowViews = new ArrayList<>();
            int w = width;
            int h = 0;


            w -= measureCellWidth(getContext(), views.get(i));
            rowViews.add(views.get(i));
            l("View added: " + views.get(i).getTag(R.string.value_name));
            l("View measured width: " + measureCellWidth(getContext(), views.get(i)));
            l("Remaining width: " + w);

            h = (measureCellHeight(getContext(), views.get(i)) > h) ?
                    measureCellHeight(getContext(), views.get(i)) : h;

            ++i;
            while (i < views.size() && w >= 0 && (int)views.get(i).getTag(R.string.value_type) != TYPE_HEADER)
            {
                w -= measureCellWidth(getContext(), views.get(i));
                if (w >= 0)
                {
                    rowViews.add(views.get(i));
                    h = (measureCellHeight(getContext(), views.get(i)) > h) ?
                            measureCellHeight(getContext(), views.get(i)) : h;
                    l("View added: " + views.get(i).getTag(R.string.value_name));
                    l("View measured width: " + measureCellWidth(getContext(), views.get(i)));
                    l("Remaining width: " + w);
                    ++i;
                }
            }

            AddRow(rows, h, rowViews);

            ++rows;
        }
    }

    private void AddRow(int rowNumber, int rowHeight, List<View> views)
    {
        int i = 0;
        for (View v : views)
        {
            Spec column = spec(i);
            Spec row = spec(rowNumber);
            LayoutParams params = new LayoutParams(row, column);
            params.height = rowHeight;
            params.width = measureCellWidth(getContext(), v);
            params.setGravity(Gravity.CENTER);
            v.setLayoutParams(params);
            addView(v);
            ++i;
        }
    }

    private View initializeView(String tag, Integer type)
    {
        View v = null;
        switch (type)
        {
            case TYPE_BOOLEAN:
                v = new CheckBox(getContext());
                ((CheckBox) v).setText(tag);
                break;
            case TYPE_STRING:
                v = getInflater().inflate(R.layout.layout_edittext, null);
                ((TextView) v.findViewById(R.id.textLabel)).setText(tag);

                TextViewCompat.setTextAppearance(
                        ((TextView) v.findViewById(R.id.textLabel)),
                        android.R.style.TextAppearance_DeviceDefault);
                break;
            case TYPE_INTEGER:
                v = getInflater().inflate(R.layout.layout_numberpicker, null);
                ((TextView) v.findViewById(R.id.numberLabel)).setText(tag);

                TextViewCompat.setTextAppearance(
                        ((TextView) v.findViewById(R.id.numberLabel)),
                        android.R.style.TextAppearance_DeviceDefault);


                ((NumberPicker) v.findViewById(R.id.numberPicker)).setMinValue(0);
                ((NumberPicker) v.findViewById(R.id.numberPicker)).setMaxValue(100);
                break;
            case TYPE_HEADER:
                v = getInflater().inflate(R.layout.layout_header, null);
        }

        if (v != null)
        {
            v.setTag(R.string.value_name, tag);
            v.setTag(R.string.value_type, type);
        }

        return v;
    }

    public List<String> GetCurrentValues()
    {
        List<String> values = new ArrayList<>();
        for (View v : views)
        {
            switch ((int)v.getTag(R.string.value_type))
            {
                case TYPE_BOOLEAN:
                    values.add(
                            String.valueOf(
                                    ((CheckBox)v).isChecked()
                            ));
                    break;
                case TYPE_STRING:
                    String s = ((EditText)v.findViewById(R.id.editText))
                            .getText().toString();
                    if (s.trim().isEmpty())
                        s = " ";
                    values.add(s);
                    break;
                case TYPE_INTEGER:
                    values.add(String.valueOf(
                            ((NumberPicker)v.findViewById(R.id.numberPicker))
                                    .getValue()));
                    break;
            }
        }
        return values;
    }

    private void l(String s)
    {
        Log.d(ScoutActivity.TAG, s);
    }

    private LayoutInflater getInflater()
    {
        return ((Activity)getContext()).getLayoutInflater();
    }


    private int measureCellWidth( Context context, View cell )
    {

        // We need a fake parent
        FrameLayout buffer = new FrameLayout( context );
        android.widget.AbsListView.LayoutParams layoutParams
                = new  android.widget.AbsListView.LayoutParams(
                android.widget.AbsListView.LayoutParams.WRAP_CONTENT,
                android.widget.AbsListView.LayoutParams.WRAP_CONTENT);

        buffer.addView( cell, layoutParams);

        cell.forceLayout();
        cell.measure(1000, 1000);

        int width = cell.getMeasuredWidth();

        buffer.removeAllViews();

        return width;
    }
    private int measureCellHeight( Context context, View cell )
    {

        // We need a fake parent
        FrameLayout buffer = new FrameLayout( context );
        android.widget.AbsListView.LayoutParams layoutParams
                = new  android.widget.AbsListView.LayoutParams(
                android.widget.AbsListView.LayoutParams.WRAP_CONTENT,
                android.widget.AbsListView.LayoutParams.WRAP_CONTENT);

        buffer.addView( cell, layoutParams);

        cell.forceLayout();
        cell.measure(1000, 1000);

        int height = cell.getMeasuredHeight();

        buffer.removeAllViews();

        return height;
    }
}
