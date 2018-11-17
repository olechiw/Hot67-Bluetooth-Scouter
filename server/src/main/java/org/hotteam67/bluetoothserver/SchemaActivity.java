package org.hotteam67.bluetoothserver;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TableLayout;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class SchemaActivity extends AppCompatActivity {
    private JSONArray schema = SchemaHandler.LoadSchemaFromFile();

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                doConfirmEnd();
                return true;
            case R.id.menuItemDeleteAll:
                final Context c = this;
                Constants.OnConfirm("Delete All?", this, () ->
                {
                    schema = new JSONArray();
                    SchemaHandler.Setup(
                            findViewById(R.id.scoutLayout),
                            schema,
                            c);
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            android.util.Log.d(this.getClass().getName(), "back button pressed");
            doConfirmEnd();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void doConfirmEnd()
    {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_schema, menu);
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schema);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Button numberButton;
        Button booleanButton;
        Button headerButton;
        Button deleteButton;
        Button multiChoiceButton;

        ScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        scrollView.setFocusable(true);
        scrollView.setFocusableInTouchMode(true);
        scrollView.setOnTouchListener((v, event) ->
        {
            v.requestFocusFromTouch();
            return false;
        });

        final TableLayout tableLayout = findViewById(R.id.scoutLayout);

        final Context c = this;

        numberButton = findViewById(R.id.numberButton);
        numberButton.setOnClickListener(view ->
                GetString("Value Label: ", tag ->
                                GetString("Maximum:", maximum ->
                                {
                                    schema.put(createJSON(Constants.TYPE_INTEGER, tag, maximum));
                                    SchemaHandler.Setup(tableLayout, schema, c);
                                })));

        booleanButton = findViewById(R.id.booleanButton);
        booleanButton.setOnClickListener(view -> GetString("Value Label:", input ->
        {
            schema.put(createJSON(Constants.TYPE_BOOLEAN, input));
            SchemaHandler.Setup(tableLayout, schema, c);
        }));
        headerButton = findViewById(R.id.headerButton);
        headerButton.setOnClickListener(view -> GetString("Value Label:", input ->
        {
            schema.put(createJSON(Constants.TYPE_HEADER, input));
            SchemaHandler.Setup(tableLayout, schema, c);
        }));
        deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(view -> Constants.OnConfirm("Are You Sure?", c, () ->
        {
            if (schema.length() != 0)
                schema.remove(schema.length() - 1);

            SchemaHandler.Setup(tableLayout, schema, c);
        }));
        multiChoiceButton = findViewById(R.id.multiChoiceButton);
        multiChoiceButton.setOnClickListener(view ->
                GetString("Multi Choice Name", label ->
                        GetMultiChoiceInput(new ArrayList<>(), choices -> {
                                schema.put(createJSON(Constants.TYPE_MULTI, label,
                                        choices.toArray(new String[0])));
                                SchemaHandler.Setup(tableLayout, schema, c);
                        })));



        SchemaHandler.Setup(tableLayout, schema, this);

        ImageButton saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> FileHandler.Write(FileHandler.SCHEMA_FILE, schema.toString()));
    }

    public interface onMultiComplete
    {
        void Complete(List<String> options);
    }

    public void GetMultiChoiceInput(List<String> input, onMultiComplete onComplete)
    {
        GetString("Input another choice or choose ok to complete", x -> {
            if (x.trim().isEmpty()) onComplete.Complete(input);
            else
            {
                input.add(x);
                GetMultiChoiceInput(input, onComplete);
            }
        });
    }

    public JSONObject createJSON(Integer type, String tag, String... extras)
    {
        JSONObject object = new JSONObject();
        try
        {
            object.put(SchemaHandler.TYPE, type);
            object.put(SchemaHandler.TAG, tag);
            if (type == Constants.TYPE_INTEGER)
            {
                int max = Integer.valueOf(extras[0]);
                object.put(SchemaHandler.MAX, max);
            }
            else if (type == Constants.TYPE_MULTI)
            {
                JSONArray choices = new JSONArray();
                for (String s : extras)
                {
                    if (!(s.trim().isEmpty()))
                        choices.put(s);
                }
                object.put(SchemaHandler.CHOICES, choices);
            }
            return object;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }


    interface StringInputEvent
    {
        void Run(String input);
    }

    private void GetString(final String prompt, final StringInputEvent onInput)
    {
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                boolean keepOriginal = true;
                StringBuilder sb = new StringBuilder(end - start);
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (isCharAllowed(c)) // put your condition here
                        sb.append(c);
                    else
                        keepOriginal = false;
                }
                if (keepOriginal)
                    return null;
                else {
                    if (source instanceof Spanned) {
                        SpannableString sp = new SpannableString(sb);
                        TextUtils.copySpansFrom((Spanned) source, start, sb.length(), null, sp, 0);
                        return sp;
                    } else {
                        return sb;
                    }
                }
            }

            private boolean isCharAllowed(char c) {
                return Character.isLetterOrDigit(c) || Character.isSpaceChar(c);
            }
        };

        final AlertDialog.Builder builder =  new AlertDialog.Builder(this);
        final EditText view = new EditText(this);
        view.setFilters(new InputFilter[] { filter });
        builder.setView(view).setTitle(prompt).setPositiveButton("Ok", (dialogInterface, i) ->
                onInput.Run(view.getText().toString())).setNegativeButton("Cancel", (dialogInterface, i) ->
        {

        }).create().show();
    }

    @Override
    public void onDestroy()
    {
        FileHandler.Write(FileHandler.SCHEMA_FILE, schema.toString());

        super.onDestroy();
    }
}
