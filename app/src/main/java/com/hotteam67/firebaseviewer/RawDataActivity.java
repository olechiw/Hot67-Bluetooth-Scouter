package com.hotteam67.firebaseviewer;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.evrencoskun.tableview.TableView;
import com.hotteam67.firebaseviewer.data.DataTable;
import com.hotteam67.firebaseviewer.tableview.MainTableAdapter;
import com.hotteam67.firebaseviewer.tableview.MainTableViewListener;
import com.hotteam67.firebaseviewer.data.Sort;

public class RawDataActivity extends AppCompatActivity {

    TextView teamNumberView;

    public static final String RAW_DATA_ATTRIBUTE = "raw_data_attribute";
    public static final String TEAM_NUMBER_ATTRIBUTE = "team_number_attribute";
    public static final String TEAM_NAME_ATTRIBUTE = "team_name_attribute";

    private ImageButton backButton;
    private DataTable dataTable;

    TableView table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_data);

        ActionBar bar = getSupportActionBar();
        View finalView = getLayoutInflater().inflate(R.layout.actionbar_raw, null);
        finalView.setLayoutParams(new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
        ));
        bar.setCustomView(finalView);
        bar.setDisplayShowCustomEnabled(true);

        teamNumberView = findViewById(R.id.teamNumberTextView);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        Bundle b = getIntent().getExtras();
        if (b != null)
        {
            dataTable = (DataTable) b.getSerializable(RAW_DATA_ATTRIBUTE);
            String teamNumber = b.getString(TEAM_NUMBER_ATTRIBUTE);
            String title = "Raw Data: " + teamNumber;
            try
            {
                title += " -" + b.getString(TEAM_NAME_ATTRIBUTE);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            teamNumberView.setText(title);
        }


        try {
            if (dataTable != null) {
                if (!(dataTable.GetCells().size() > 0)) {
                    Log.e("FirebaseScouter", "No input raw data found");
                    return;
                }

                dataTable = Sort.BubbleSortAscendingByRowHeader(dataTable);

                table = findViewById(R.id.mainTableView);
                MainTableAdapter adapter = new MainTableAdapter(this);
                table.setAdapter(adapter);
                table.setTableViewListener(new MainTableViewListener(table));
                adapter.setAllItems(dataTable, null);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public void doEndWithMatchNumber(String matchNumber) {
        Intent result = new Intent();
        result.putExtra("Match Number", matchNumber);
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}