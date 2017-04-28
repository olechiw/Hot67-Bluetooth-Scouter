package org.hotteam67.bluetoothscouter;

import android.graphics.Color;
import android.support.v4.widget.TextViewCompat;
import android.widget.*;
import android.app.*;
import android.content.Context;
import android.view.*;
import android.content.res.ColorStateList;

import java.util.*;
import android.util.*;

/**
 * Created by Jakob on 3/17/2017.
 */

public class InputTableLayout extends TableLayout
{
    public static final int PADDING = 0;
    public static final int TYPE_HEADER = 1;
    public static final int TYPE_BOOLEAN = 2;
    public static final int TYPE_INTEGER = 3;
    public static final int TYPE_STRING = 4;

    private List<View> views = new ArrayList<>();
    private List<Variable> variables = new ArrayList<>();

    private String schema = "";

    public InputTableLayout(Context context)
    {
        super(context);
    }

    public InputTableLayout(Context context, AttributeSet set)
    {
        super(context, set);
    }

    public void l(String s)
    {
        Log.d("BLUETOOTH_SCOUTER_UI", s);
    }

    public void ReBuild()
    {
        Build(schema);
    }

    public void Set(List<String> values)
    {
        try
        {
            for (int i = 0; i < values.size(); ++i)
            {
                ((EditText)views.get(i).findViewById(R.id.editText)).setText(values.get(i));
            }
        }
        catch (Exception e)
        {
            l("Failed to load value when calling Set(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void Build(List<View> _v)
    {
        views = _v;
        int maxColumnWidth = 0;
        for (View v : views)
        {
            v.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            int w = v.getMeasuredWidth() + PADDING;
            maxColumnWidth = (w > maxColumnWidth) ? w : maxColumnWidth;
        }
        l("Measured maximum width: " + maxColumnWidth);

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int columns = metrics.widthPixels / maxColumnWidth;
        l("Measured screen width: " + metrics.widthPixels);
        l("Calculated columns: " + columns);

        int i = 0;
        while (i < views.size())
        {
            TableRow row = new TableRow(getContext());

            View v = views.get(i);
            if (v instanceof TextView)
            {
                //l("TextView Gravity");
                ((TextView) v).setGravity(Gravity.CENTER);
            }
            else
            {
                //l("LinearLayout Gravity");
                ((LinearLayout) v).setGravity(Gravity.CENTER);
            }
            TableRow.LayoutParams params =
                    new TableRow.LayoutParams(
                            maxColumnWidth,
                            TableRow.LayoutParams.MATCH_PARENT);
            //params.gravity = Gravity.CENTER;
            // params.weight = 1;
            v.setLayoutParams(params);
            row.addView(v);
            l("Adding view in column 0: " + v.getTag(R.string.variable_name));
            ++i;
            int c = 1;
            while (i < views.size()
                    && (int)v.getTag(R.string.variable_type) != TYPE_HEADER
                    && c < columns)
            {
                v = views.get(i);
                if ((int)v.getTag(R.string.variable_type) == TYPE_HEADER)
                    break;
                // l("Adding a column. View index: " + i);
                v.setLayoutParams(params);
                row.addView(v);
                l("Adding view in column " + c + ": " + v.getTag(R.string.variable_name));
                ++i;
                ++c;
            }
            l("Adding a row.");
            LayoutParams p = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            p.gravity = Gravity.CENTER;
            // p.weight = 1;
            row.setGravity(Gravity.CENTER);
            row.setLayoutParams(p);

            addView(row);
        }
    }


    public boolean Build(String _schema)
    {
        removeAllViews();
        schema = _schema;
        try
        {
            List<Variable> vars = new ArrayList<>();
            List<String> vals = Arrays.asList(_schema.split(","));
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

            /*
            int minimumLength = 0;
            for (Variable var : vars)
            {
                if (var.Tag.length() > minimumLength)
                    minimumLength = var.Tag.length();
            }
            for (Variable var : vars)
            {
                if (var.Tag.length() < minimumLength && var.Type != TYPE_HEADER)
                {
                    for (int i = 0; i < minimumLength - var.Tag.length(); ++i)
                        var.Tag += " ";
                }
            }
            */

            this.variables = vars;
            List<View> viewsList = new ArrayList<>();
            for (Variable var : vars)
            {
                viewsList.add(initializeView(var.Tag, var.Type, var.Min, var.Max));
            }
            Build(viewsList);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            l("Failed to load: " + e.getMessage());
            return false;
        }

        return true;
    }

    public List<Variable> getVariables() { return variables; }

    public List<String> GetCurrentValues()
    {
        List<String> values = new ArrayList<>();
        for (View v : views)
        {
            switch ((int) v.getTag(R.string.variable_type))
            {
                case TYPE_BOOLEAN:
                    values.add(
                            String.valueOf(
                                    ((CheckBox) v.findViewById(R.id.checkBox1)).isChecked()
                            ));
                    ((CheckBox) v.findViewById(R.id.checkBox1)).setChecked(false);

                    break;
                case TYPE_STRING:
                    String s = (((EditText) v.findViewById(R.id.editText))
                            .getText().toString());
                    values.add(s.trim().isEmpty() ? " " : s);
                    ((EditText) v.findViewById(R.id.editText)).setText("");
                    break;
                case TYPE_INTEGER:
                    values.add(String.valueOf(
                            ((NumberPicker) v.findViewById(R.id.numberPicker))
                                    .getValue()));
                    ((NumberPicker) v.findViewById(R.id.numberPicker)).setValue(0);
                    break;
                default:
                    l("Not possible to get value of view with type: " + v.getTag(R.string.variable_type));
            }
        }
        return values;
    }

    // Get just the last char
    private String getLast(String s)
    {
        if (s.length() > 0)
            return s.substring(s.length()-1);
        else
            return "";
    }
    // Get all values up to the last char
    private String getBefore(String s)
    {
        if (s.length() > 0)
            return s.substring(0, s.length()-1);
        else
            return "";
    }

    private LayoutInflater getInflater()
    {
        return ((Activity)getContext()).getLayoutInflater();
    }
    private LayoutInflater getPickerInflater()
    {
        LayoutInflater inflater =  ((Activity)getContext()).getLayoutInflater();
        final Context contextThemeWrapper = new ContextThemeWrapper(getContext(), R.style.NumberPickerTheme);
        return inflater.cloneInContext(contextThemeWrapper);
    }

    private View initializeView(String tag, Integer type, int min, int max)
    {
        l("Initializing View:" + tag);
        l("Type: " + type);
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
            case TYPE_STRING:
                v = getInflater().inflate(R.layout.layout_edittext, null);
                TextView text = ((TextView) v.findViewById(R.id.textLabel));
                text.setText(tag);
                TextViewCompat.setTextAppearance(
                        text,
                        android.R.style.TextAppearance_DeviceDefault);
                break;
            case TYPE_INTEGER:
                v = getPickerInflater().inflate(R.layout.layout_numberpicker, null);
                ((TextView) v.findViewById(R.id.numberLabel)).setText(tag);

                TextViewCompat.setTextAppearance(
                        ((TextView) v.findViewById(R.id.numberLabel)),
                        android.R.style.TextAppearance_DeviceDefault);


                NumberPicker picker = (NumberPicker) v.findViewById(R.id.numberPicker);
                picker.setMinValue(min);
                picker.setMaxValue(max);
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
}
