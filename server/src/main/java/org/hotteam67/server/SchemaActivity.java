package org.hotteam67.server;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.widget.*;

import org.hotteam67.common.FileHandler;
import org.hotteam67.common.SchemaHandler;

public class SchemaActivity extends AppCompatActivity
{
    EditText schemaInput;
    Button previewButton;

    TableLayout previewLayout;

    ImageButton infoButton;

    Button expandSchemaButton;
    private int lines = 1;

    android.support.v7.widget.Toolbar toolbar;

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schema);

        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setCustomView(R.layout.toolbar_schema);
        ab.setDisplayShowCustomEnabled(true);

        schemaInput = (EditText) findViewById(R.id.schemaInput);
        previewButton = (Button) findViewById(R.id.previewButton);
        previewLayout = (TableLayout) findViewById(R.id.schemaLayout);

        final Context c = this;
        previewButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            SchemaHandler.Setup(previewLayout, schemaInput.getText().toString(), c);
            }
        });

        expandSchemaButton = (Button) findViewById(R.id.expandSchemaButton);
        expandSchemaButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                /*
                if (schemaInput.isShown())
                {
                    schemaInput.setVisibility(View.INVISIBLE);
                    previewButton.setVisibility(View.VISIBLE);
                    previewLayout.setVisibility(View.VISIBLE);
                }
                else
                {
                    schemaInput.setVisibility(View.VISIBLE);
                    previewButton.setVisibility(View.INVISIBLE);
                    previewLayout.setVisibility(View.INVISIBLE);
                }
                */
                if (lines == 1)
                {
                    schemaInput.setLines(15);
                    lines = 2;
                }
                else
                {
                    schemaInput.setLines(1);
                    lines = 1;
                }
            }
        });

        infoButton = (ImageButton) findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onInfoClick();
            }
        });

        schemaInput.setText(FileHandler.LoadContents(FileHandler.SCHEMA));
        SchemaHandler.Setup(previewLayout, schemaInput.getText().toString().replace("\n", ""), this);
        Log.d("[Bluetooth_Schema]", schemaInput.getText().toString());
    }

    private void onInfoClick()
    {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        dlg.setTitle("Schema Information");
        dlg.setMessage(
                "Key:\n 1: Header\n 2: Boolean\n 3: Integer\n 4: EditText (Avoid using this)"
        );
        dlg.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dlg.setCancelable(false);
        dlg.create();
        dlg.show();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        FileHandler.Write(FileHandler.SCHEMA, schemaInput.getText().toString());
    }

    protected void toast(String text)
    {
        try {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("");
            dlg.setMessage(text);
            dlg.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dlg.setCancelable(true);
            dlg.create();
            dlg.show();
        }
        catch (Exception e)
        {
            Log.d("Schema Builder", "Failed to create dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
