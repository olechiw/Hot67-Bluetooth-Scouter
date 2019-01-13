package org.hotteam67.common;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
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
import java.util.List;

/**
 * The handler for the schema system, which populates and updates the contents of a tableview with
 * the schema's requested items
 */

public final class SchemaHandler
{
    /**
     * The label for a schema item
     */
    public static final String TAG = "Tag";
    /**
     * The type of a schema item
     */
    public static final String TYPE = "Type";
    /**
     * The max value of a schema item
     */
    public static final String MAX = "Max";
    /**
     * The JSONArray list of choices for a schema item
     */
    public static final String CHOICES = "Choices";

    /**
     * Create the UI Rows from a schema
     * @param schema the input schema
     * @param context the parent context to create views with
     * @return a list of TableRows populated from the schema
     */
    private static List<TableRow> GetRows(JSONArray schema, Context context)
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
            catch (Exception ignored)
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
            if (type == Constants.InputTypes.TYPE_HEADER)
            {
                SetParams(view, columnWidth);
                row.addView(view);
                currentColumn = maxColumns;
                currentView++;
            }

            while (currentColumn < maxColumns &&
                    currentView < views.size() &&
                    type != Constants.InputTypes.TYPE_HEADER)
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

    /**
     * Get the current values from an already populated TableLayout
     * @param table the table layout that already fits a schema
     * @return a JSONObject with tags for each schema tag and values from the table
     */
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
                    case Constants.InputTypes.TYPE_INTEGER:
                        output.put(tag,
                                String.valueOf(
                                        ((DarkNumberPicker) v.findViewById(R.id.numberPicker)).getValue()
                                )
                        );
                        break;
                    case Constants.InputTypes.TYPE_BOOLEAN:
                        output.put(tag, String.valueOf(((CheckBox) v.findViewById(R.id.checkBox1)).isChecked()));
                        break;
                    case Constants.InputTypes.TYPE_STRING:
                        output.put(tag, ((EditText) v.findViewById(R.id.editText)).getText().toString());
                        break;
                    case Constants.InputTypes.TYPE_MULTI:
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

    /**
     * Generate a UI from a schem and populate a view
     * @param table The TableLayout to setup
     * @param schema The schema to generate the views from
     * @param context The parent context to give the views
     */
    public static void Setup(TableLayout table, JSONArray schema, Context context)
    {
        table.removeAllViews();
        for (View v : GetRows(schema, context))
        {
            if (v != null)
                table.addView(v);
        }
    }

    /**
     * Load a schema from file into a jsonarray
     * @return the schema as a JSONArray, containing JSONObjects for each schema item
     */
    public static JSONArray LoadSchemaFromFile()
    {
        try
        {
            BufferedReader reader = FileHandler.GetReader(FileHandler.Files.SCHEMA_FILE);
            if (reader == null) return new JSONArray();
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

    /**
     * Get all of the views from a table, iterating over its rows and its rows' children
     * @param table the table to get all of the views form
     * @return a list of all of the children of the tablerows of the table
     */
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

    /**
     * Reset all of the values of a table to default
     * @param table the table to reset the values of
     */
    public static void ClearCurrentValues(TableLayout table)
    {
        for (View v : getViews(table))
        {
            try
            {
                switch ((int) v.getTag(R.string.variable_type))
                {
                    case Constants.InputTypes.TYPE_INTEGER:
                        ((DarkNumberPicker) v.findViewById(R.id.numberPicker)).setValue(Integer.valueOf("0"));
                        break;
                    case Constants.InputTypes.TYPE_BOOLEAN:
                        ((CheckBox) v.findViewById(R.id.checkBox1)).setChecked(Boolean.valueOf("0"));
                        break;
                    case Constants.InputTypes.TYPE_STRING:
                        ((EditText) v.findViewById(R.id.editText)).setText("");
                        break;
                    case Constants.InputTypes.TYPE_MULTI:
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

    /**
     * Set all of the values of a table to a specified value
     * @param table the table to map values to
     * @param values the values to set, with tags corresponding to the schema tags and values to set
     */
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
                    case Constants.InputTypes.TYPE_INTEGER:
                        ((DarkNumberPicker) v.findViewById(R.id.numberPicker)).setValue(Integer.valueOf(values.getString(name)));
                        // l("Loading in value: " + values.get(val));

                        break;
                    case Constants.InputTypes.TYPE_BOOLEAN:
                        ((CheckBox) v.findViewById(R.id.checkBox1)).setChecked(Boolean.valueOf(values.getString(name)));
                        // l("Loading in value: " + values.get(val));
                        break;
                    case Constants.InputTypes.TYPE_STRING:
                        ((EditText) v.findViewById(R.id.editText)).setText(values.getString(name));
                        // l("Loading in value: " + values.get(val));
                        break;
                    case Constants.InputTypes.TYPE_MULTI:
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

    /**
     * Set the layout parameters of a view for the schema
     * @param v the view
     * @param width the views specified witdth
     */
    private static void SetParams(View v, int width)
    {
        TableRow.LayoutParams params = new TableRow.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
        v.setLayoutParams(params);
        if (v instanceof LinearLayout)
            ((LinearLayout)v).setGravity(Gravity.CENTER);
        else if (v instanceof CheckBox)
            ((CheckBox)v).setGravity(Gravity.CENTER);
    }

    /**
     * Create a view from a variable that has been loaded from the schema
     * @param variable the variable or schema item to be turned into a view
     * @param c the parent context
     * @return a view that has been inflated in accordance with the provided variable
     */
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
            case Constants.InputTypes.TYPE_BOOLEAN:
                v = getInflater(c).inflate(R.layout.layout_boolean, null);
                LinearLayout l = (LinearLayout)v;
                CheckBox check = l.findViewById(R.id.checkBox1);
                check.setEnabled(true);
                check.setText(tag);
                check.setVisibility(View.VISIBLE);
                break;
            case Constants.InputTypes.TYPE_STRING:
                v = getInflater(c).inflate(R.layout.layout_edittext, null);
                TextView text = v.findViewById(R.id.textLabel);
                text.setText(tag);
                TextViewCompat.setTextAppearance(
                        text,
                        android.R.style.TextAppearance_DeviceDefault);
                break;
            case Constants.InputTypes.TYPE_INTEGER:
                v = getInflater(c).inflate(R.layout.layout_numberpicker, null);
                ((TextView) v.findViewById(R.id.numberLabel)).setText(tag);
                TextViewCompat.setTextAppearance(
                        v.findViewById(R.id.numberLabel),
                        android.R.style.TextAppearance_DeviceDefault);
                ((DarkNumberPicker)v.findViewById(R.id.numberPicker)).setButtonTextColor(Color.WHITE);


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
            case Constants.InputTypes.TYPE_HEADER:
                v = getInflater(c).inflate(R.layout.layout_header, null);
                ((TextView)v).setText(tag);
                break;
            case -1:
                l("Failed to determine type of obj " + variable.toString());
            case Constants.InputTypes.TYPE_MULTI:
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

    /**
     * Measure the effective column width of views by detecting the maximum columns that can exist
     * without issue
     * @param views a list of all of the views to detect how many columns can fit
     * @return the maximum number of columns possible
     */
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
