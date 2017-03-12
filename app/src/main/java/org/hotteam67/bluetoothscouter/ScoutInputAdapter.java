package org.hotteam67.bluetoothscouter;

import android.support.v4.widget.TextViewCompat;
import android.widget.*;
import android.content.*;
import android.view.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import android.util.DisplayMetrics;
import android.app.Activity;
import android.util.Log;


/**
 * Created by Jakob on 3/7/2017.
 */

public class ScoutInputAdapter extends BaseAdapter {

    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_STRING = 2;
    public static final int TYPE_INTEGER = 3;

    private Context context;
    // private static List<LinearLayout> variables = new ArrayList<>();
    private static List<View> variables = new ArrayList<>();
    private static LinkedHashMap<String, Integer> variableInfo = new LinkedHashMap<>();

    public int getCount()
    {
        return variables.size();
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;


        if (position < variables.size())
        {
            return variables.get(position);
        }

        /*
        GridView gv = (GridView) parent;
        gv.setNumColumns(columns);
        gv.setColumnWidth(columnWidth);
        */

        return v;
    }

    public int measureCellWidth( Context context, View cell )
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

    public Object getItem(int position)
    {
        return null;
    }

    public long getItemId(int position)
    {
        return 0;
    }

    public void ReBuild()
    {
        Build(variableInfo);
    }
    public void Build(LinkedHashMap<String, Integer> data)
    {
        variableInfo = data;
        variables = new ArrayList<>();

        for (Map.Entry<String, Integer> d : data.entrySet())
        {
            View layout;

            switch (d.getValue())
            {
                case TYPE_BOOLEAN:
                    l("Creating a boolean: " + d.getKey());
                    layout = new CheckBox(context);
                    ((CheckBox)layout).setText(d.getKey());
                    // ((CheckBox)layout).setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small);
                    break;
                case TYPE_STRING:
                    l("Creating a string: " + d.getKey());
                    layout = ((Activity)context).getLayoutInflater().inflate(R.layout.layout_edittext, null);
                    ((TextView)layout.findViewById(R.id.textLabel)).setText(d.getKey());

                    TextViewCompat.setTextAppearance(
                            ((TextView)layout.findViewById(R.id.textLabel)),
                            android.R.style.TextAppearance_DeviceDefault);

                    break;
                case TYPE_INTEGER:
                    l("Creating an integer: " + d.getKey());
                    layout = ((Activity)context).getLayoutInflater().inflate(R.layout.layout_numberpicker, null);
                    ((TextView)layout.findViewById(R.id.numberLabel)).setText(d.getKey());

                    TextViewCompat.setTextAppearance(
                            ((TextView)layout.findViewById(R.id.numberLabel)),
                            android.R.style.TextAppearance_DeviceDefault);


                    ((NumberPicker)layout.findViewById(R.id.numberPicker)).setMinValue(0);
                    ((NumberPicker)layout.findViewById(R.id.numberPicker)).setMaxValue(50);

                    break;
                default:
                    l("Invalid ID Given!:" + d.getValue());
                    layout = null;
                    continue;
            }

            layout.setTag(R.string.value_name, d.getKey());
            layout.setTag(R.string.value_type, d.getValue());

            variables.add(layout);
        }

        int width = 0;
        for (View v : variables)
        {
            int id = (int)v.getTag(R.string.value_type);

            if (id == TYPE_INTEGER)
                v.setPadding(130, 20, 130, 20);
            else
                v.setPadding(20, 20, 20, 20);

            int measure = measureCellWidth(context, v);
            width = (width > measure ) ? width : measure;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;

        l("Columns Measured at Width: " + width);
        l("Screen Measured at Width: " + screenWidth);

        /*
        columns = (screenWidth/width);
        columnWidth = width;
        */
        l("Columns Calculated: " + columns);
    } int columns = 2;
    int columnWidth = 0;


    public ScoutInputAdapter(Context c) {
        context = c;
    }

    public List<String> GetCurrentValues()
    {
        List<String> values = new ArrayList<>();
        for (View v : variables)
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

    private static void l(String s)
    {
        Log.d(BluetoothActivity.TAG, s);
    }


}