package org.hotteam67.firebaseviewer;

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
import com.evrencoskun.tableview.sort.SortState;
import org.hotteam67.firebaseviewer.data.DataTable;
import org.hotteam67.firebaseviewer.tableview.MainTableAdapter;
import org.hotteam67.firebaseviewer.tableview.MainTableViewListener;

/**
 * Activity for displaying the raw data selected for a match, and potentially returning a selected
 * match number to filter by
 */
public class RawDataActivity extends AppCompatActivity {

    /**
     * The attribute for the intent extra containing raw data to display (the actual matches)
     */
    public static final String RAW_DATA_ATTRIBUTE = "raw_data_attribute";
    /**
     * The attribute for the intent extra containing the team number
     */
    public static final String TEAM_NUMBER_ATTRIBUTE = "team_number_attribute";
    /**
     * The attribute for the intent extra containing the team name
     */
    public static final String TEAM_NAME_ATTRIBUTE = "team_name_attribute";

    /**
     * The data table (in memory) that contains all of the raw data
     */
    private DataTable dataTable;

    /**
     * Constructor, create the title out of team number and team name, and load the raw data from the
     * intent
     * @param savedInstanceState ignored
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_data);

        ActionBar bar = getSupportActionBar();
        if (bar != null)
        {
            View finalView = getLayoutInflater().inflate(R.layout.actionbar_raw, null);
            finalView.setLayoutParams(new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.MATCH_PARENT,
                    ActionBar.LayoutParams.MATCH_PARENT
            ));
            bar.setCustomView(finalView);
            bar.setDisplayShowCustomEnabled(true);
        }

        TextView teamNumberView = findViewById(R.id.teamNumberTextView);
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        Bundle b = getIntent().getExtras();
        if (b != null)
        {
            dataTable = (DataTable) b.getSerializable(RAW_DATA_ATTRIBUTE);
            String teamNumber = b.getString(TEAM_NUMBER_ATTRIBUTE);
            String title = "Raw Data: " + teamNumber;
            try
            {
                title += " - " + b.getString(TEAM_NAME_ATTRIBUTE);
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

                TableView table = findViewById(R.id.mainTableView);
                MainTableAdapter adapter = new MainTableAdapter(this);
                table.setAdapter(adapter);
                table.setTableViewListener(new MainTableViewListener(table, adapter));
                adapter.setAllItems(dataTable);
                table.sortRowHeader(SortState.ASCENDING);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     * If there is a match number selected, this is called and ends the activity after giving it
     * the match number as a result
     * @param matchNumber the match number to attach to the intent
     */
    public void doEndWithMatchNumber(int matchNumber) {
        if (matchNumber == -1) return;
        Intent result = new Intent();
        result.putExtra("Match Number", matchNumber);
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}