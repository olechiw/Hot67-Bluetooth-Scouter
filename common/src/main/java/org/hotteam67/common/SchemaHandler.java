package org.hotteam67.common;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.TextViewCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.*;

/**
 * Created by Jakob on 4/28/2017.
 */

public final class SchemaHandler
{
    public static String GetHeader(String schema)
    {
        String scheme = "";
        List<SchemaVariable> vars = GetVariables(schema);
        for (int i = 0; i < vars.size(); ++i)
        {
            if (vars.get(i) != null && vars.get(i).Type != Constants.TYPE_HEADER)
            {
                scheme += vars.get(i).Tag;
            }
            if (i + 1 < vars.size())
                scheme += ",";
        }

        return scheme;
    }

    public static List<TableRow> GetRows(String schema, Context context)
    {
        List<SchemaVariable> variables = GetVariables(schema);
        List<View> views = new ArrayList<>();

        for (SchemaVariable v : variables)
        {
            views.add(GetView(v, context));
        }

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(metrics);

        List<TableRow> rows = new ArrayList<>();

        int columnWidth = MeasureColumnWidth(views);
        int maxColumns = metrics.widthPixels / columnWidth;
        int currentColumn = 0;
        int totalViews = 0;
        while (totalViews + 1 < views.size())
        {
            TableRow row = new TableRow(context);
            if (variables.get(totalViews).Type == Constants.TYPE_HEADER)
            {
                SetParams(views.get(totalViews), columnWidth);
                row.addView(views.get(totalViews));
                currentColumn = maxColumns;
                totalViews++;
            }

            while (currentColumn < maxColumns &&
                    totalViews < views.size() &&
                    variables.get(totalViews).Type != Constants.TYPE_HEADER)
            {
                SetParams(views.get(totalViews), columnWidth);
                row.addView(views.get(totalViews));
                totalViews++;
                currentColumn++;
            }

            row.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
            row.setGravity(Gravity.CENTER);
                        currentColumn = 0;

            rows.add(row);
        }
        return rows;
    }

    public static List<String> GetCurrentValues(TableLayout table)
    {
        List<String> output = new ArrayList<>();

        for (View v : getViews(table))
        {
            switch ((int) v.getTag(R.string.variable_type))
            {
                case Constants.TYPE_INTEGER:
                    output.add(
                            String.valueOf(
                            ((DarkNumberPicker)v.findViewById(R.id.numberPicker)).getValue()
                            )
                    );
                    break;
                case Constants.TYPE_BOOLEAN:
                    output.add(String.valueOf(((CheckBox)v.findViewById(R.id.checkBox1)).isChecked()));
                    break;
                case Constants.TYPE_STRING:
                    output.add(((EditText)v.findViewById(R.id.editText)).getText().toString());
                    break;
            }
        }
        l("Returning output of length: " + output.size());
        return output;
    }

    public static void Setup(TableLayout table, String schema, Context context)
    {
        table.removeAllViews();
        for (View v : GetRows(schema, context))
        {
            table.addView(v);
        }
    }

    public static List<View> getViews(TableLayout table)
    {
        List<View> views = new ArrayList<>();
        for (int i = 0; i < table.getChildCount(); ++i)
        {
            ViewGroup row = (ViewGroup) table.getChildAt(i);
            for (int v = 0; v < row.getChildCount(); ++v)
            {
                views.add(row.getChildAt(v));
            }
        }
        return views;
    }

    public static void ClearCurrentValues(TableLayout table)
    {
        int val = 0;
        for (View v : getViews(table))
        {
            try
            {
                switch ((int) v.getTag(R.string.variable_type))
                {
                    case Constants.TYPE_INTEGER:
                        ((DarkNumberPicker) v.findViewById(R.id.numberPicker)).setValue(Integer.valueOf("0"));
                        break;
                    case Constants.TYPE_BOOLEAN:
                        ((CheckBox) v.findViewById(R.id.checkBox1)).setChecked(Boolean.valueOf("0"));
                        break;
                    case Constants.TYPE_STRING:
                        ((EditText) v.findViewById(R.id.editText)).setText("");
                        break;
                    default:
                        val--;
                }
            }
            catch (Exception e)
            {
                Log.e("[Schema Handler", "Failed to set value : " + e.getMessage(), e);
            }
            ++val;
        }
    }

    public static void SetCurrentValues(TableLayout table, List<String> values)
    {
        int val = 0;
        try
        {
            for (View v : getViews(table))
            {
                switch ((int) v.getTag(R.string.variable_type))
                {
                    case Constants.TYPE_INTEGER:
                        ((DarkNumberPicker) v.findViewById(R.id.numberPicker)).setValue(Integer.valueOf(values.get(val)));
                        l("Loading in value: " + values.get(val));
                        ++val;

                        break;
                    case Constants.TYPE_BOOLEAN:
                        ((CheckBox) v.findViewById(R.id.checkBox1)).setChecked(Boolean.valueOf(values.get(val)));
                        l("Loading in value: " + values.get(val));
                        ++val;
                        break;
                    case Constants.TYPE_STRING:
                        ((EditText) v.findViewById(R.id.editText)).setText(values.get(val));
                        l("Loading in value: " + values.get(val));
                        ++val;
                        break;
                }
                if (val >= values.size())
                    break;
            }
        }
        catch (Exception e)
        {
            Log.e("[Schema Handler", "Failed to set value : " + e.getMessage(), e);
            ClearCurrentValues(table);
        }
    };

    private static void SetParams(View v, int width)
    {
        TableRow.LayoutParams params = new TableRow.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
        v.setLayoutParams(params);
        if (v instanceof LinearLayout)
            ((LinearLayout)v).setGravity(Gravity.CENTER);
        else if (v instanceof CheckBox)
            ((CheckBox)v).setGravity(Gravity.CENTER);
    }

    public static List<SchemaVariable> GetVariables(String schema)
    {
        List<SchemaVariable> vars = new ArrayList<>();
        try
        {
            List<String> vals = Arrays.asList(schema.split(","));
            for (int i = 0; i < vals.size(); ++i)
            {
                String str = vals.get(i);
                SchemaVariable v = null;
                int tmp = i;
                try
                {
                    l("Loading: " + str);
                    l("Found tag: " + getBeforeLast(str));
                    l("Found type: " + getLast(str));
                    int number = Integer.valueOf(getLast(str));
                    int min = 0, max = 0;
                    if (number == Constants.TYPE_INTEGER)
                    {
                        l("Getting integer value of: " + vals.get(tmp + 1));
                        min = Integer.valueOf(vals.get(tmp + 1));
                        ++i;
                        l("Getting integer value of: " + vals.get(tmp + 2));
                        max = Integer.valueOf(vals.get(tmp + 2));
                        ++i;
                    }
                    v = new SchemaVariable(getBeforeLast(str), number, min, max);
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
        } catch (Exception e)
        {
            e.printStackTrace();
            l("Failed to load: " + e.getMessage());
        }

        return vars;
    }


    public static View GetView(SchemaVariable variable, Context c)
    {
        View v = null;
        switch (variable.Type)
        {
            case Constants.TYPE_BOOLEAN:
                /*
                v = new CheckBox(getContext());
                ((CheckBox) v).setText(tag);
                */
                v = getInflater(c).inflate(R.layout.layout_boolean, null);
                LinearLayout l = (LinearLayout)v;
                CheckBox check = ((CheckBox)l.findViewById(R.id.checkBox1));
                check.setEnabled(true);
                check.setText(variable.Tag);
                check.setVisibility(View.VISIBLE);
                /*
                v.setTag(R.string.variable_type, Constants.TYPE_BOOLEAN);
                v.setTag(R.string.variable_name, variable.Tag);
                */
                break;
            case Constants.TYPE_STRING:
                v = getInflater(c).inflate(R.layout.layout_edittext, null);
                TextView text = ((TextView) v.findViewById(R.id.textLabel));
                text.setText(variable.Tag);
                /*
                v.setTag(R.string.variable_type, Constants.TYPE_STRING);
                v.setTag(R.string.variable_name, variable.Tag);
                */
                TextViewCompat.setTextAppearance(
                        text,
                        android.R.style.TextAppearance_DeviceDefault);
                break;
            case Constants.TYPE_INTEGER:
                v = getInflater(c).inflate(R.layout.layout_numberpicker, null);
                ((TextView) v.findViewById(R.id.numberLabel)).setText(variable.Tag);
                /*
                v.setTag(R.string.variable_type, Constants.TYPE_INTEGER);
                v.setTag(R.string.variable_name, variable.Tag);
                */
                TextViewCompat.setTextAppearance(
                        ((TextView) v.findViewById(R.id.numberLabel)),
                        android.R.style.TextAppearance_DeviceDefault);


                DarkNumberPicker picker = (DarkNumberPicker) v.findViewById(R.id.numberPicker);
                picker.setMinimum(variable.Min);
                picker.setMaximum(variable.Max);
                break;
            case Constants.TYPE_HEADER:
                v = getInflater(c).inflate(R.layout.layout_header, null);
                ((TextView)v).setText(variable.Tag);
                /*
                v.setTag(R.string.variable_type, Constants.TYPE_HEADER);
                v.setTag(R.string.variable_name, variable.Tag);
                */
                break;
            default:
                l("Error, invalid type given: " + variable.Type);
        }

        if (v != null)
        {
            v.setTag(R.string.variable_name, variable.Tag);
            l("Adding variable tag: " + variable.Tag);
            l("Returned: " + v.getTag(R.string.variable_name));
            v.setTag(R.string.variable_type, variable.Type);
            l("Adding variable type: " + variable.Type);
            l("Returned: " + v.getTag(R.string.variable_type));
        }

        return v;
    }

    // Get just the last char
    private static String getLast(String s)
    {
        if (s.length() > 0)
            return s.substring(s.length()-1);
        else
            return "";
    }
    // Get all values up to the last char
    private static String getBeforeLast(String s)
    {
        if (s.length() > 0)
            return s.substring(0, s.length()-1);
        else
            return "";
    }

    private static LayoutInflater getInflater(Context c)
    {
        return ((Activity)c).getLayoutInflater();
    }

    private static int MeasureColumnWidth(List<View> views)
    {
        // Measure max column width
        int maxColumnWidth = 0;
        for (View v : views)
        {
            v.measure(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
            int w = v.getMeasuredWidth();
            maxColumnWidth = (w > maxColumnWidth) ? w : maxColumnWidth;
        }
        l("Measured maximum width: " + maxColumnWidth);

        return maxColumnWidth;
    }

    private static void l(String s)
    {
        Log.d("[Schema Handler]", s);
    }
}
