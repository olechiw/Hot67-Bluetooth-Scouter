package org.hotteam67.bluetoothscouter;

import android.app.Activity;
import android.support.v4.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.content.Context;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.support.v7.widget.GridLayout;
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

            h = (measureCellHeight(getContext(), views.get(i)) > h) ?
                    measureCellHeight(getContext(), views.get(i)) : h;

            ++i;
            while (i < views.size()
                    && w >= 0
                    && (int)views.get(i).getTag(R.string.value_type) != TYPE_HEADER
                    && (int)views.get(i - 1).getTag(R.string.value_type) != TYPE_HEADER)
            {
                w -= measureCellWidth(getContext(), views.get(i));
                if (w >= 0)
                {
                    rowViews.add(views.get(i));
                    h = (measureCellHeight(getContext(), views.get(i)) > h) ?
                            measureCellHeight(getContext(), views.get(i)) : h;
                    ++i;
                }
            }

            l("Adding a new row!");
            AddRow(rows, h, rowViews);

            ++rows;
        }
        l("Initialization finished with "
                + getColumnCount() + " columns and "
                + getRowCount() + " rows");


        for (View v : views)
        {
            if ((int)v.getTag(R.string.value_type) == TYPE_HEADER)
            {
                /*
                // l("Getting layout params for header");
                LayoutParams params = (LayoutParams)v.getLayoutParams();
                // l("Setting ColumnSpec");
                params.columnSpec = spec(0, getColumnCount());
                // l("Updating layoutparams");
                v.setLayoutParams(params);
                */
                /*
                View space = new View(getContext());
                LayoutParams params = (LayoutParams)v.getLayoutParams();
                params.columnSpec = spec(1);
                space.setLayoutParams(params);
                addView(space);
                */
            }
        }
    }

    private void AddRow(int rowNumber, int rowHeight, List<View> views)
    {
        int i = 0;
        for (View v : views)
        {
            Spec column = spec(i, 1f);
            if ((int)v.getTag(R.string.value_type) == TYPE_HEADER)
                column = spec(0, 4);
            Spec row = spec(rowNumber);
            LayoutParams params = new LayoutParams(row, column);

            params.height = LayoutParams.WRAP_CONTENT;
            params.width = LayoutParams.WRAP_CONTENT;

            params.setGravity(Gravity.CENTER);

            v.setLayoutParams(params);
            addView(v);
            ++i;
        }
        i = 0;
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
                ((TextView)v).setText(tag);
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

        return width + 30;
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
