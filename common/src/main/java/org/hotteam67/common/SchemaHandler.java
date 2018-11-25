package org.hotteam67.common;

import android.app.Activity;
import android.content.Context;
import android.support.v4.widget.TextViewCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jakob on 4/28/2017.
 */

public final class SchemaHandler
{
    // Tags used in JSON format for schema
    public static final String TAG = "Tag";
    public static final String TYPE = "Type";
    public static final String MAX = "Max";
    public static final String CHOICES = "Choices";

    public static String GetTableHeader(JSONArray schema)
    {
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < schema.length(); ++i)
        {
            try
            {
                JSONObject obj = schema.getJSONObject(i);
                if (obj != null && obj.getInt(TYPE) != Constants.TYPE_HEADER)
                {
                    header.append(obj.getString(TAG));
                    if (i + 1 < schema.length())
                        header.append(",");
                }
            }
            catch (Exception e)
            {
            }
        }

        return header.toString();
    }

    public static List<TableRow> GetRows(JSONArray schema, Context context)
    {

        if (schema.length() == 0)
            return new ArrayList<>();

        // Measure screen
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(metrics);

        List<TableRow> rows = new ArrayList<>();

        // Calculate column width
        List<View> views = new ArrayList<>();
        for (int i = 0; i < schema.length(); ++i)
        {
            try
            {
                views.add(GetView(schema.getJSONObject(i), context));
            }
            catch (Exception e)
            {
            }
        }

        views.remove(null);
        int columnWidth = MeasureColumnWidth(views);
        if (columnWidth == 0) return new ArrayList<>();
        int maxColumns = metrics.widthPixels / columnWidth;


        int currentColumn = 0;
        int currentView = 0;
        while (currentView < views.size())
        {
            TableRow row = new TableRow(context);
            View view = views.get(currentView);
            int type = Integer.valueOf(view.getTag(R.string.variable_type).toString());
            if (type == Constants.TYPE_HEADER)
            {
                SetParams(view, columnWidth);
                row.addView(view);
                currentColumn = maxColumns;
                currentView++;
            }

            while (currentColumn < maxColumns &&
                    currentView < views.size() &&
                    type != Constants.TYPE_HEADER)
            {
                SetParams(view, columnWidth);
                row.addView(view);

                currentView++;
                currentColumn++;
                if (currentView < views.size())
                    view = views.get(currentView);
                type = Integer.valueOf(view.getTag(R.string.variable_type).toString());
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

    public static JSONObject GetCurrentValues(TableLayout table)
    {
        JSONObject output = new JSONObject();

        for (View v : getViews(table))
        {
            try
            {
                String tag = v.getTag(R.string.variable_name).toString();
                switch ((int) v.getTag(R.string.variable_type))
                {
                    case Constants.TYPE_INTEGER:
                        output.put(tag,
                                String.valueOf(
                                        ((DarkNumberPicker) v.findViewById(R.id.numberPicker)).getValue()
                                )
                        );
                        break;
                    case Constants.TYPE_BOOLEAN:
                        output.put(tag, String.valueOf(((CheckBox) v.findViewById(R.id.checkBox1)).isChecked()));
                        break;
                    case Constants.TYPE_STRING:
                        output.put(tag, ((EditText) v.findViewById(R.id.editText)).getText().toString());
                        break;
                    case Constants.TYPE_MULTI:
                        Spinner s = v.findViewById(R.id.multiChoiceSpinner);
                        if (s.getSelectedItem() == null)
                            output.put(tag, s.getItemAtPosition(0).toString());
                        else output.put(tag, s.getSelectedItem().toString());
                        break;
                    default:
                        // l("Found a header of tag: " + v.getTag(R.string.variable_name));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
       // "Returning output of length: " + output.size());
        return output;
    }

    public static void Setup(TableLayout table, JSONArray schema, Context context)
    {
        table.removeAllViews();
        for (View v : GetRows(schema, context))
        {
            if (v != null)
                table.addView(v);
        }
    }

    public static JSONArray LoadSchemaFromFile()
    {
        try
        {
            BufferedReader reader = FileHandler.GetReader(FileHandler.SCHEMA_FILE);
            String line = reader.readLine();
            reader.close();
            if (line != null)
            {
                return new JSONArray(line);
            }
            else
                return new JSONArray();
        }
        catch (Exception e)
        {
            l("Failed to load schema: " + e.getMessage());
            return new JSONArray();
        }
    }

    private static List<View> getViews(TableLayout table)
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
                    case Constants.TYPE_MULTI:
                        ((Spinner)v.findViewById(R.id.multiChoiceSpinner)).setSelection(0);
                        break;
                }
            }
            catch (Exception e)
            {
                Log.e("[Schema Handler", "Failed to set value : " + e.getMessage(), e);
            }
        }
    }

    public static void SetCurrentValues(TableLayout table, JSONObject values)
    {
        for (View v : getViews(table))
        {
            try
            {
                String name = v.getTag(R.string.variable_name).toString();
                if (!values.has(name)) continue;
                switch ((int) v.getTag(R.string.variable_type))
                {
                    case Constants.TYPE_INTEGER:
                        ((DarkNumberPicker) v.findViewById(R.id.numberPicker)).setValue(Integer.valueOf(values.getString(name)));
                        // l("Loading in value: " + values.get(val));

                        break;
                    case Constants.TYPE_BOOLEAN:
                        ((CheckBox) v.findViewById(R.id.checkBox1)).setChecked(Boolean.valueOf(values.getString(name)));
                        // l("Loading in value: " + values.get(val));
                        break;
                    case Constants.TYPE_STRING:
                        ((EditText) v.findViewById(R.id.editText)).setText(values.getString(name));
                        // l("Loading in value: " + values.get(val));
                        break;
                    case Constants.TYPE_MULTI:
                        Spinner s = v.findViewById(R.id.multiChoiceSpinner);
                        s.setSelection(((ArrayAdapter<String>)s.getAdapter()).getPosition(values.getString(name)));
                        break;
                }
            } catch (Exception e)
            {
                Log.e("[Schema Handler", "Failed to set value : " + e.getMessage(), e);
                ClearCurrentValues(table);
            }
        }
    }

    private static void SetParams(View v, int width)
    {
        TableRow.LayoutParams params = new TableRow.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
        v.setLayoutParams(params);
        if (v instanceof LinearLayout)
            ((LinearLayout)v).setGravity(Gravity.CENTER);
        else if (v instanceof CheckBox)
            ((CheckBox)v).setGravity(Gravity.CENTER);
    }


    private static View GetView(JSONObject variable, Context c)
    {
        View v = null;
        if (variable == null)
            return null;
        Integer type;
        String tag;
        try
        {
            type = variable.getInt(TYPE);
            tag = variable.getString(TAG);
        }
        catch (Exception e)
        {
            type = -1;
            tag = "N/A";
        }
        switch (type)
        {
            case Constants.TYPE_BOOLEAN:
                v = getInflater(c).inflate(R.layout.layout_boolean, null);
                LinearLayout l = (LinearLayout)v;
                CheckBox check = l.findViewById(R.id.checkBox1);
                check.setEnabled(true);
                check.setText(tag);
                check.setVisibility(View.VISIBLE);
                break;
            case Constants.TYPE_STRING:
                v = getInflater(c).inflate(R.layout.layout_edittext, null);
                TextView text = v.findViewById(R.id.textLabel);
                text.setText(tag);
                TextViewCompat.setTextAppearance(
                        text,
                        android.R.style.TextAppearance_DeviceDefault);
                break;
            case Constants.TYPE_INTEGER:
                v = getInflater(c).inflate(R.layout.layout_numberpicker, null);
                ((TextView) v.findViewById(R.id.numberLabel)).setText(tag);
                TextViewCompat.setTextAppearance(
                        v.findViewById(R.id.numberLabel),
                        android.R.style.TextAppearance_DeviceDefault);


                DarkNumberPicker picker = v.findViewById(R.id.numberPicker);
                try
                {
                    picker.setMinimum(0); // Always zero, removed config option for this
                    picker.setMaximum(variable.getInt(MAX));
                }
                catch (Exception ignored)
                {

                }
                break;
            case Constants.TYPE_HEADER:
                v = getInflater(c).inflate(R.layout.layout_header, null);
                ((TextView)v).setText(tag);
                break;
            case -1:
                l("Failed to determine type of obj " + variable.toString());
            case Constants.TYPE_MULTI:
                v = getInflater(c).inflate(R.layout.layout_multichoice, null);
                ((TextView)v.findViewById(R.id.multiChoiceLabel)).setText(tag);
                Spinner spinner = v.findViewById(R.id.multiChoiceSpinner);
                try
                {
                    JSONArray choices = variable.getJSONArray(CHOICES);

                    List<String> choiceStrings = new ArrayList<>();
                    for (int i = 0; i < choices.length(); ++i)
                    {
                        choiceStrings.add((String)choices.get(i));
                    }
                    ArrayAdapter adapter = new ArrayAdapter<>(c,
                            android.R.layout.simple_spinner_item,
                            choiceStrings);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                }
                catch (Exception ignored)
                {

                }
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
