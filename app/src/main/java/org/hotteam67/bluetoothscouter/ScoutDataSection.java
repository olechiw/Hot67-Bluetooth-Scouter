package org.hotteam67.bluetoothscouter;

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

public class ScoutDataSection extends BaseAdapter {

    public static final int TYPE_BOOL = 1;
    public static final int TYPE_STRING = 2;
    public static final int TYPE_INTEGER = 3;

    private Context context;
    private static List<LinearLayout> variables = new ArrayList<>();
    private static LinkedHashMap<String, Integer> variableInfo = new LinkedHashMap<>();

    public int getCount()
    {
        return variables.size();
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = null;

        if (position < variables.size())
        {
            v = variables.get(position);
            v.setPadding(40, 10, 40, 10);
        }

        return v;
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
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_HORIZONTAL;

            TextView label = new TextView(context);
            label.setText(d.getKey());
            if (d.getValue() == TYPE_BOOL)
                label.setText(" ");
            layout.addView(label, params);


            switch (d.getValue())
            {
                case TYPE_BOOL:
                    CheckBox bool = new CheckBox(context);
                    bool.setText(d.getKey());
                    layout.addView(bool, params);
                    break;
                case TYPE_STRING:
                    EditText string = new EditText(context);
                    layout.addView(string, params);
                    break;
                case TYPE_INTEGER:
                    NumberPicker integer=  new NumberPicker(context);
                    integer.setMinValue(0);
                    integer.setMaxValue(100);

                    layout.addView(integer);
                    break;
            }

            variables.add(layout);
        }
    }


    public ScoutDataSection(Context c) {
        context = c;
    }


    /*
    public static void Build(LinkedHashMap<String, Integer> data, android.widget.GridView gridView)
    {
        l("Contructing Scout Data Section");

        DisplayMetrics displayMetrics = new DisplayMetrics();

        ((Activity) gridView.getContext()).getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displayMetrics);

        int width = displayMetrics.widthPixels;

        int i = 0;
        for (Map.Entry<String, Integer> d : data.entrySet()) {
            View v;
            View vLabel;
            switch (d.getValue()) {
                case TYPE_BOOL:
                    v = new CheckBox(gridView.getContext());
                    //v = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.holo_picker, null);
                    ((CheckBox) v).setText(d.getKey());
                    vLabel = new View(gridView.getContext());
                    break;
                case TYPE_STRING:
                    v = new EditText(gridView.getContext());
                    vLabel = new TextView(gridView.getContext());
                    ((TextView) vLabel).setText(d.getKey());
                    break;
                case TYPE_INTEGER:
                    v = new NumberPicker(gridView.getContext());

                    vLabel = new TextView(gridView.getContext());
                    ((TextView) vLabel).setText(d.getKey());
                    break;
                default:
                    v = null;
                    vLabel = null;
                    Log.e(BluetoothActivity.TAG, "ERROR, INVALID VALUE RECEIVED: " + d.getValue());
            }
            variables.add(vLabel);
            variables.add(v);
            i += 2;
        }

        int padding = 10;


        i = 0;
        l(String.valueOf(i));
        while (i < variables.size())
        {
            l("Getting views, starting at: " + i);
            /*
            TableRow row = new TableRow(getContext());
            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            TableRow secondRow = new TableRow(getContext());
            secondRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            */
/*
            List<View> firstRowViews = new ArrayList<>();
            List<View> secondRowViews = new ArrayList<>();

            int tmp = width;
            */
            /*
            do
            {
                View v1 = variables.get(i);
                View v2 = variables.get(i + 1);
                tmp -= padding * 2;

                int greater = (v1.getWidth() > v2.getWidth()) ? v1.getWidth() : v2.getWidth();
                tmp -= greater;
                if (tmp >= 0)
                {
                    firstRowViews.add(v1);
                    secondRowViews.add(v2);
                }

                i += 2;
            } while (tmp >= 0 && i < variables.size());
            */
            /*
            View v1 = variables.get(i);
            View v2 = variables.get(i + 1);
            float weight = 1.0f;
            tmp = (v1.getWidth() > v2.getWidth()) ? v1.getWidth() : v2.getWidth();

            firstRowViews.add(v1);
            secondRowViews.add(v2);
            if (i + 2 < variables.size())
            {
                View v3 = variables.get(i + 2);
                View v4 = variables.get(i + 3);
                tmp -= (v3.getWidth() > v4.getWidth()) ? v3.getWidth() : v4.getWidth();

                /*
                if (tmp >= padding * 2)
                {
                    weight = 0.5f;
                    firstRowViews.add(v3);
                    secondRowViews.add(v4);
                    i += 4;
                }
                */
                /**/
                // weight = 0.5f;
                /*
                firstRowViews.add(v3);
                secondRowViews.add(v4);
                i += 4;
                */
                /**/
                /*
                else/**//*
                    i += 2;
                    /**//*
            }
            else
                i += 2;

            for (View v : firstRowViews)
            {
                GridView.LayoutParams params = centeredParams();
                //params.weight = weight;
                v.setLayoutParams(params);

                l("Loaded a view");

                GridView.LayoutParams params2 = centeredParams();
                //params2.weight = weight;

                View vContent = secondRowViews.get(firstRowViews.indexOf(v));
                vContent.setLayoutParams(params);

                l("Loaded a view");

                LinearLayout layout = new LinearLayout(gridView.getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.addView(v);
                layout.addView(vContent);

                layout.setLayoutParams(centeredParams());
                gridView.addView(layout);


                l("Added a view");
            }

            /*
            for (View v : secondRowViews)
            {
                TableRow.LayoutParams params = centeredParams();
                params.weight = weight;
                v.setLayoutParams(params);
                l("Loaded a view");
                secondRow.addView(v);
                l("Added a view");
            }

            l("Adding First Row : Index :" + String.valueOf(i));
            l(String.valueOf(i));
            this.addView(row);
            l("Adding Second Row");
            l(String.valueOf(i));
            this.addView(secondRow);
            *//*
        }
    }
    */
    /*
    private static GridView.LayoutParams centeredParams()
    {
        GridView.LayoutParams params = new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, GridView.LayoutParams.MATCH_PARENT);

        return params;
    }

    private static void l(String s)
    {
        Log.d(BluetoothActivity.TAG, s);
    }

    */
}
