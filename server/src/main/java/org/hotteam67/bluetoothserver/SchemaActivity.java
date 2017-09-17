package org.hotteam67.bluetoothserver;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.bluetoothserver.R;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchemaActivity extends AppCompatActivity {
    private String schema = SchemaHandler.LoadSchemaFromFile();

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                doConfirmEnd();
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
        Constants.OnConfirm("Are you sure you want to quit?", this, new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schema);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Button numberButton;
        Button booleanButton;
        Button headerButton;
        Button deleteButton;

        final TableLayout tableLayout = (TableLayout) findViewById(R.id.scoutLayout);

        final Context c = this;

        numberButton = (Button) findViewById(R.id.numberButton);
        numberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetString("Value Label:", new StringInputEvent() {
                    @Override
                    public void Run(String input) {
                        schema += "," + input + Constants.TYPE_INTEGER;
                        SchemaHandler.Setup(tableLayout, schema, c);
                    }
                });
            }
        });

        booleanButton = (Button) findViewById(R.id.booleanButton);
        booleanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetString("Value Label:", new StringInputEvent() {
                    @Override
                    public void Run(String input) {
                        schema += "," + input + Constants.TYPE_BOOLEAN;
                        SchemaHandler.Setup(tableLayout, schema, c);
                    }
                });
            }
        });
        headerButton = (Button) findViewById(R.id.headerButton);
        headerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetString("Value Label:", new StringInputEvent() {
                    @Override
                    public void Run(String input) {
                        schema += "," + input + Constants.TYPE_HEADER;
                        SchemaHandler.Setup(tableLayout, schema, c);
                    }
                });
            }
        });
        deleteButton = (Button) findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Constants.OnConfirm("Are You Sure?", c, new Runnable() {
                    @Override
                    public void run() {
                        List<String> values = new ArrayList<>(Arrays.asList(schema.split(",")));
                        if (values.size() <= 0) return;

                        values.remove(values.size() - 1);

                        schema = "";
                        for (int i = 0; i < values.size(); ++i)
                        {
                            schema += values.get(i);
                            if (i + 1 < values.size()) schema += ",";
                        }
                        SchemaHandler.Setup(tableLayout, schema, c);
                    }
                });
            }
        });

        SchemaHandler.Setup(tableLayout, schema, this);

        ImageButton saveButton = (ImageButton) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileHandler.Write(FileHandler.SCHEMA, schema);
            }
        });
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
        builder.setView(view).setTitle(prompt).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onInput.Run(view.getText().toString());
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).create().show();
    }

    @Override
    public void onDestroy()
    {
        FileHandler.Write(FileHandler.SCHEMA, schema);

        super.onDestroy();
    }
}
