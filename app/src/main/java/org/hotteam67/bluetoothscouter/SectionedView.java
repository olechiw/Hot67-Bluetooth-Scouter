package org.hotteam67.bluetoothscouter;

import android.support.v4.widget.TextViewCompat;
import android.widget.*;
import android.app.*;
import android.content.Context;
import android.view.*;

import java.util.*;
import android.util.*;

import org.hotteam67.bluetoothscouter.R;
import org.hotteam67.bluetoothscouter.ScoutInputAdapter;

/**
 * Created by Jakob on 3/17/2017.
 */

public class SectionedView extends LinearLayout
{
    List<View> views = new ArrayList<>();

    public SectionedView(Context context)
    {
        super(context);
        setOrientation(VERTICAL);
    }

    public SectionedView(Context context, AttributeSet set)
    {
        super(context, set);
        setOrientation(VERTICAL);
    }

    public SectionedView(Context context, AttributeSet set, int defStyle)
    {
        super(context, set, defStyle);
        setOrientation(VERTICAL);
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
            List<Variable> vars = new ArrayList<>();
            List<String> vals = Arrays.asList(variables.split(","));
            for (int i = 0; i < vals.size(); ++i)
            {
                String str = vals.get(i);
                Variable v = null;
                int tmp = i;
                try
                {
                    l("Loading: " + str);
                    l("Found tag: " + getBefore(str));
                    l("Found type: " + getLast(str));
                    int number = Integer.valueOf(getLast(str));
                    int min = 0, max = 0;
                    if (number == TYPE_INTEGER)
                    {
                        l("Getting integer value of: " + vals.get(tmp + 1));
                        min = Integer.valueOf(vals.get(tmp + 1));
                        ++i;
                        l("Getting integer value of: " + vals.get(tmp + 2));
                        max = Integer.valueOf(vals.get(tmp + 2));
                        ++i;
                    }
                    v = new Variable(getBefore(str), number, min, max);
                } catch (IndexOutOfBoundsException e)
                {
                    l("Failed to load minimum and maximum values. Not present");
                    e.printStackTrace();
                } catch (Exception e)
                {
                    l("Failed to load type from input");
                    e.printStackTrace();
                }

                vars.add(v);
            }


            Build(vars);
        } catch (Exception e)
        {
            e.printStackTrace();
            l("Failed to load");
            return false;
        }

        return true;
    }

    public void Build(List<Variable> vars)
    {
        int i = 0;
        while (i < vars.size())
        {
            int height = 0;
            Variable v = vars.get(i);
            if (v.Type != TYPE_HEADER)
            {
                l("Failed to build, not a header to start");
                break;
            }
            TextView label = new TextView(getContext());
            // label.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            // height += label.getMeasuredHeight();
            // TextViewCompat.setTextAppearance(label, R.style.TextAppearance_AppCompat);
            label.setText(v.Tag);

            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_HORIZONTAL;

            addView(label);
            ++i;

            List<View> sectionViews = new ArrayList<>();
            while (i < vars.size() &&
                    vars.get(i).Type != TYPE_HEADER)
            {
                View tmpView = initializeView(vars.get(i).Tag, vars.get(i).Type, vars.get(i).Min, vars.get(i).Max);
                sectionViews.add(tmpView);
                views.add(tmpView);
                tmpView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                height += tmpView.getMeasuredHeight();
                ++i;
            }
            l("Assembling a grid, size: " + sectionViews.size());
            GridView grid = new GridView(getContext());
            grid.setNumColumns(GridView.AUTO_FIT);

            grid.setOnTouchListener(new OnTouchListener(){

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return event.getAction() == MotionEvent.ACTION_MOVE;
                }

            });

            ScoutInputAdapter adapter = new ScoutInputAdapter(getContext());
            adapter.SetViews(sectionViews);
            grid.setAdapter(adapter);
            params.width = LayoutParams.MATCH_PARENT;
            params.height =(int)( height * .66);
            grid.setLayoutParams(params);
            addView(grid);
        }
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

    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_INTEGER = 3;
    public static final int TYPE_HEADER = 4;

    private LayoutInflater getInflater()
    {
        return ((Activity)getContext()).getLayoutInflater();
    }

    private View initializeView(String tag, Integer type, int min, int max)
    {
        l("Initializing View:" + tag);
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

    public void l(String s)
    {
        Log.d("BLUETOOTH_SCOUTER_UI", s);
    }
}
