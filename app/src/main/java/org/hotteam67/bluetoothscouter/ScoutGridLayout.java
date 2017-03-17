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
import android.view.Gravity;
import android.support.v7.widget.GridLayout;
import java.util.*;
import android.util.Log;
import android.widget.LinearLayout;
import android.util.DisplayMetrics;
import android.util.AttributeSet;


/**
 * Created by Jakob on 3/12/2017.
 */

public class ScoutGridLayout extends GridLayout
{
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_INTEGER = 3;
    public static final int TYPE_HEADER = 4;

    private static List<View> views = new ArrayList<>();

    private static List<Variable> variables = new ArrayList<>();

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

    public void Build(List<Variable> vars)
    {
        removeAllViews();
        views = new ArrayList<>();
        variables = vars;
        for (Variable var : vars)
        {
            l("Initializing view: " + var.Tag);
            View v = initializeView(var.Tag, var.Type, var.Min, var.Max);
            if (v != null)
                views.add(v);
            else
                l("Null returned on intializeView(). Tag: " + var.Tag + " Type: " + var.Type);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;

        int i = 0;
        int rows = 0;
        List<View> rowViews;
        l("Screen Width: " + width);
        while (i < views.size())
        {
            rowViews = new ArrayList<>();
            int w = width;
            int h = 0;


            int widthMeasured = measureCellWidth(views.get(i));
            rowViews.add(views.get(i));
            w -= widthMeasured;
            l("View tag: " + views.get(i).getTag(R.string.variable_name));
            l("widthMeasured: " + widthMeasured);
            l("width - widthMeasured: " + w);
            l("Adding view");


            h = (measureCellHeight(getContext(), views.get(i)) > h) ?
                    measureCellHeight(getContext(), views.get(i)) : h;

            ++i;
            while (i < views.size()
                    && w >= 0
                    && (int)views.get(i).getTag(R.string.variable_type) != TYPE_HEADER
                    && (int)views.get(i - 1).getTag(R.string.variable_type) != TYPE_HEADER)
            {
                if (views.get(i) == null)
                {
                    ++i;
                    l("Failed to load view at index: " + i);
                    continue;
                }
                widthMeasured = measureCellWidth(views.get(i));
                w -= widthMeasured;
                l("View tag: " + views.get(i).getTag(R.string.variable_name));
                l("widthMeasured: " + widthMeasured);
                l("width - widthMeasured: " + w);
                if (w >= 0)
                {
                    l("Adding view");
                    rowViews.add(views.get(i));
                    h = (measureCellHeight(getContext(), views.get(i)) > h) ?
                            measureCellHeight(getContext(), views.get(i)) : h;
                    ++i;
                }
                else
                    l("Passed view");
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
            if ((int)v.getTag(R.string.variable_type) == TYPE_HEADER)
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
        l("Adding row: " + rowNumber);
        int i = 0;
        for (View v : views)
        {
            Spec column = spec(i, 1f);
            /*
            if ((int)v.getTag(R.string.value_type) == TYPE_HEADER)
                column = spec(0, 4);
                */
            Spec row = spec(rowNumber);
            LayoutParams params = new LayoutParams(row, column);

            params.height = rowHeight;
            params.width = measureCellWidth(v);

            params.setGravity(Gravity.CENTER);

            v.setLayoutParams(params);
            addView(v, params);
            ++i;
        }
        i = 0;
    }

    private View initializeView(String tag, Integer type, int min, int max)
    {
        View v = null;
        switch (type)
        {
            case TYPE_BOOLEAN:
                /*
                v = new CheckBox(getContext());
                ((CheckBox) v).setText(tag);
                */
                v = getInflater().inflate(R.layout.layout_boolean, null);
                LinearLayout l = (LinearLayout)v;
                CheckBox c = ((CheckBox)l.findViewById(R.id.checkBox1));
                c.setEnabled(true);
                c.setText(tag);
                c.setVisibility(View.VISIBLE);
                break;
            case TYPE_INTEGER:
                v = getInflater().inflate(R.layout.layout_numberpicker, null);
                ((TextView) v.findViewById(R.id.numberLabel)).setText(tag);

                TextViewCompat.setTextAppearance(
                        ((TextView) v.findViewById(R.id.numberLabel)),
                        android.R.style.TextAppearance_DeviceDefault);


                ((NumberPicker) v.findViewById(R.id.numberPicker)).setMinValue(min);
                ((NumberPicker) v.findViewById(R.id.numberPicker)).setMaxValue(max);
                break;
            case TYPE_HEADER:
                v = getInflater().inflate(R.layout.layout_header, null);
                ((TextView)v).setText(tag);
                break;
            default:
                l("Error, invalid type given: " + type);
        }

        if (v != null)
        {
            v.setTag(R.string.variable_name, tag);
            v.setTag(R.string.variable_type, type);
        }

        return v;
    }

    public List<String> GetCurrentValues()
    {
        List<String> values = new ArrayList<>();
        for (View v : views)
        {
            switch ((int)v.getTag(R.string.variable_type))
            {
                case TYPE_BOOLEAN:

                    values.add(
                            String.valueOf(
                                    ((CheckBox)v.findViewById(R.id.checkBox1)).isChecked()
                            ));
                    ((CheckBox)v).setChecked(false);

                    break;
                case TYPE_INTEGER:
                    values.add(String.valueOf(
                            ((NumberPicker)v.findViewById(R.id.numberPicker))
                                    .getValue()));
                    ((NumberPicker)v.findViewById(R.id.numberPicker)).setValue(0);
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


    private int measureCellWidth(View cell )
    {
        /*
        cell.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        return cell.getMeasuredWidth() + 100;
        */

        // We need a fake parent
        FrameLayout buffer = new FrameLayout(getContext());
        android.widget.AbsListView.LayoutParams layoutParams
                = new  android.widget.AbsListView.LayoutParams(
                android.widget.AbsListView.LayoutParams.WRAP_CONTENT,
                android.widget.AbsListView.LayoutParams.WRAP_CONTENT);

        buffer.addView( cell, layoutParams);

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay();

        cell.forceLayout();
        cell.measure(1000, 1000);

        int width = cell.getMeasuredWidth();

        buffer.removeAllViews();

        if (width < 650)
        {

            return width + 60;
        }
        else
            return width + 150;
    }


    // Get just the last char
    private String getLast(String s)
    {
        return s.substring(s.length()-1);
    }
    // Get all values up to the last char
    private String getBefore(String s)
    {
        return s.substring(0, s.length()-1);
    }

    public boolean Build(String variables)
    {
        try
        {
            List<ScoutGridLayout.Variable> vars = new ArrayList<>();
            List<String> vals = Arrays.asList(variables.split(","));
            for (int i = 0; i < vals.size(); ++i)
            {
                String str = vals.get(i);
                ScoutGridLayout.Variable v = null;
                int tmp = i;
                try
                {
                    l("Loading: " + str);
                    l("Found tag: " + getBefore(str));
                    l("Found type: " + getLast(str));
                    int number = Integer.valueOf(getLast(str));
                    int min = 0, max = 0;
                    if (number == ScoutGridLayout.TYPE_INTEGER)
                    {
                        l("Getting integer value of: " + vals.get(tmp + 1));
                        min = Integer.valueOf(vals.get(tmp + 1));
                        ++i;
                        l("Getting integer value of: " + vals.get(tmp + 2));
                        max = Integer.valueOf(vals.get(tmp + 2));
                        ++i;
                    }
                    v = new ScoutGridLayout.Variable(getBefore(str), number, min, max);
                }
                catch (IndexOutOfBoundsException e)
                {
                    l("Failed to load minimum and maximum values. Not present");
                    e.printStackTrace();
                }
                catch (Exception e)
                {
                    l("Failed to load type from input");
                    e.printStackTrace();
                }

                vars.add(v);
            }


            Build(vars);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            l("Failed to load");
            return false;
        }

        return true;
    }

    public List<Variable> GetVariables()
    {
        return variables;
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

    public static class Variable
    {
        public String Tag;
        public int Type;
        public int Max;
        public int Min;
        public Variable() {}
        public Variable(String tag, int type)
        {
            Tag = tag;
            Type = type;
        }
        public Variable(String tag, int type, int min, int max)
        {
            Tag = tag;
            Type = type;
            Min = min;
            Max = max;
        }
    }
}
