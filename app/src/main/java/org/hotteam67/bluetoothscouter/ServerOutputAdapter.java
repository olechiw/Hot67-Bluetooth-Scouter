package org.hotteam67.bluetoothscouter;

import android.widget.BaseAdapter;
import android.content.Context;
import android.support.v4.widget.TextViewCompat;
import android.widget.*;
import java.util.*;
import android.view.*;
import android.util.Log;
import android.util.DisplayMetrics;

/**
 * Created by Jakob on 3/10/2017.
 */

public class ServerOutputAdapter {

    public static void Build(Context context, List<String> data, TableLayout l)
    {
        l("Building Table With Lines : " + data.size());
        ArrayList<TableRow> rows = new ArrayList<>();

        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.leftMargin = 30;
        lp.rightMargin = 30;

        // int width = 0;


        for (String line : data) {

            l("Loading a Line: " + line);
            String[] split = line.split(",");

            TableRow layout = new TableRow(context);
            // layout.setColumnCount(gridViewColumns);
            for (String value : split)
            {
                l("Loading a Value: " + value);

                TextView text = createValue(value, context);


                /*
                if (measureCellWidth(context, text) > width)
                    width = measureCellWidth(context, text);
                    */
                layout.addView(text);
            }
            l("Loading Row");
            rows.add(layout);
        }

        for (int i = 0; i < rows.size(); ++i)
        {
            TableRow row = rows.get(i);
            l("adding row");
            l.addView(row,i);
        }
    }

    public static TextView createValue(String createText, Context context)
    {
        TextView v = new TextView(context);
        v.setText(createText);
        TextViewCompat.setTextAppearance(
                v,
                android.R.style.TextAppearance_DeviceDefault_Medium);
        return v;
    }

    public static int measureCellWidth( Context context, View cell )
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

    private static void l(String msg)
    {
        Log.d(ServerActivity.TAG, msg);
    }
}
