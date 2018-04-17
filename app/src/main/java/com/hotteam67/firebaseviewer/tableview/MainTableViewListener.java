package com.hotteam67.firebaseviewer.tableview;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.evrencoskun.tableview.ITableView;
import com.evrencoskun.tableview.listener.ITableViewListener;
import com.hotteam67.firebaseviewer.data.ColumnSchema;
import com.hotteam67.firebaseviewer.FileHandler;
import com.hotteam67.firebaseviewer.data.ScatterPlot;
import com.hotteam67.firebaseviewer.MainActivity;
import com.hotteam67.firebaseviewer.RawDataActivity;
import com.hotteam67.firebaseviewer.data.DataTable;
import com.hotteam67.firebaseviewer.data.Sort;
import com.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by evrencoskun on 2.12.2017.
 */

public class MainTableViewListener implements ITableViewListener {

    private ITableView mTableView;

    private final String MatchNumber = "Match Number";

    public MainTableViewListener(ITableView pTableView) {
        this.mTableView = pTableView;
    }

    @Override
    public void onCellClicked(@NonNull RecyclerView.ViewHolder p_jCellView, int p_nXPosition, int
            p_nYPosition) {
        MainTableAdapter adapter = (MainTableAdapter)mTableView.getAdapter();
        DataTable rawData = adapter.GetRawData();

        if (rawData == null)
            return;

        try {
            String teamNumber = adapter.GetCalculatedData().GetRowHeaders().get(p_nYPosition).getData();

            DataTable table = Sort.BubbleSortAscendingByRowHeader(GetFormattedRawData(adapter, teamNumber));

            String calculatedColumnName =
                    adapter.GetCalculatedData().GetColumns().get(p_nXPosition).getData();

            String rawColumnName = ColumnSchema.CalculatedColumnsRawNames().get(
                    ColumnSchema.CalculatedColumns().indexOf(calculatedColumnName)
            );

            // Find the x value in the raw data table
            int index = -1;
            for (ColumnHeaderModel header : table.GetColumns())
            {
                if (header.getData().equals(rawColumnName))
                    index = table.GetColumns().indexOf(header);
            }

            if (index == -1)
                return;

            List<Integer> values = new ArrayList<>();

            // Get each value and put in a single array
            for (List<CellModel> row : table.GetCells())
            {
                String value = (String)row.get(index).getData();
                if (value.equals("N/A"))
                    continue;
                if (value.equals("true") || value.equals("false"))
                {
                    values.add(Boolean.valueOf(value) ? 1 : 0);
                }
                else
                    values.add(Integer.valueOf(value));
            }

            String title = teamNumber;
            JSONObject teamNumbersNames =((MainActivity)adapter.GetContext()).GetTeamNumbersNames();
            if (!(teamNumbersNames == null) && teamNumbersNames.has(teamNumber))
                title += " - " + teamNumbersNames.get(teamNumber);
            title += ": " + rawColumnName;

            ScatterPlot.Show(
                    values, ((MainTableAdapter) mTableView.getAdapter()).GetContext(), title);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onCellLongPressed(@NonNull RecyclerView.ViewHolder cellView, int column, int row) {

    }

    private int lastColumnClicked = -1;
    @Override
    public void onColumnHeaderClicked(@NonNull RecyclerView.ViewHolder p_jColumnHeaderView, int
            p_nXPosition) {
        Log.d("HotTeam67", "Sorting column: " + p_nXPosition);
        MainTableAdapter adapter = (MainTableAdapter) mTableView.getAdapter();
        DataTable processor = adapter.GetCalculatedData();

        if (processor == null || processor.GetCells() == null || processor.GetCells().size() == 0)
            return;

        if (lastColumnClicked != p_nXPosition) {
            adapter.setAllItems(Sort.BubbleSortByColumn(processor, p_nXPosition, false), adapter.GetRawData());
            lastColumnClicked = p_nXPosition;
        }
        else
        {
            adapter.setAllItems(Sort.BubbleSortByColumn(processor, p_nXPosition, true), adapter.GetRawData());
            lastColumnClicked = -1;
        }

    }

    @Override
    public void onColumnHeaderLongPressed(@NonNull RecyclerView.ViewHolder p_jColumnHeaderView,
                                          int p_nXPosition) {
    }

    @Override
    public void onRowHeaderClicked(@NonNull RecyclerView.ViewHolder p_jRowHeaderView, int
            p_nYPosition) {
        MainTableAdapter adapter = (MainTableAdapter) mTableView.getAdapter();

        String teamNumber = adapter.GetCalculatedData().GetRowHeaders().get(p_nYPosition).getData();

        DataTable rawData = adapter.GetRawData();
        if (rawData == null) {
            ((RawDataActivity) adapter.GetContext()).doEndWithMatchNumber(
                    adapter.GetCalculatedData().GetRowHeaders().get(p_nYPosition).getData()
            );
            return;
        }

            Log.d("HotTeam67", "Set team number filter: " + teamNumber);

        DataTable formattedData = GetFormattedRawData(adapter, teamNumber);

            Intent rawDataIntent = new Intent(adapter.GetContext(), RawDataActivity.class);
            rawDataIntent.putExtra(RawDataActivity.RAW_DATA_ATTRIBUTE, formattedData);
            rawDataIntent.putExtra(RawDataActivity.TEAM_NUMBER_ATTRIBUTE, teamNumber);

            MainActivity activity = (MainActivity)adapter.GetContext();
            try {
                rawDataIntent.putExtra(RawDataActivity.TEAM_NAME_ATTRIBUTE, (String)activity.GetTeamNumbersNames().get(teamNumber));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            activity.startActivityForResult(rawDataIntent, MainActivity.RawDataRequestCode);
    }

    private DataTable GetFormattedRawData(MainTableAdapter adapter, String teamNumber) {
        DataTable rawData = adapter.GetRawData();
        rawData.SetTeamNumberFilter(teamNumber);

        /*
        Copy to final data
         */
        List<List<CellModel>> cells = new ArrayList<>();
        List<List<CellModel>> preCopyData = rawData.GetCells();
        for (List<CellModel> row : preCopyData) {
            ArrayList<CellModel> newRow = new ArrayList<>(row);
            cells.add(newRow);
        }

        List<RowHeaderModel> rows = new ArrayList<>(rawData.GetRowHeaders());
        List<ColumnHeaderModel> columns = new ArrayList<>(rawData.GetColumns());


        /*
        Remove match number, set as row header, add all of the teams unscouted matches
         */
        if (columns.size() == 0 || !columns.get(0).getData().equals(MatchNumber)) {
            int matchNumberColumnIndex = -1;
            /*
            Prep full team schedule
             */
            List<String> matchNumbers = new ArrayList<>();
            String matches = FileHandler.LoadContents(FileHandler.VIEWER_MATCHES);
            if (matches != null && !matches.trim().isEmpty()) {
                List<String> matchesArray = Arrays.asList(matches.split("\n"));
                if (matchesArray.size() > 0)
                    // Load all team matches
                    for (String match : matchesArray) {
                        if (Arrays.asList(match.split(",")).contains(teamNumber))
                            // +1 to make it from index to actual match number
                            matchNumbers.add(String.valueOf(matchesArray.indexOf(match) + 1));
                    }
            }
            /*
            Move header
             */
            for (ColumnHeaderModel column : columns) {
                if (column.getData().equals(MatchNumber)) {
                    matchNumberColumnIndex = columns.indexOf(column);
                }
            }

            /*
            Move value in each row
             */
            if (matchNumberColumnIndex != -1) {
                try {
                    columns.remove(matchNumberColumnIndex);
                    // columns.add(new ColumnHeaderModel("Match Number"));
                    for (List<CellModel> row : cells) {
                        CellModel value = row.get(matchNumberColumnIndex);
                        String matchNumber = value.getData().toString();
                        rows.set(cells.indexOf(row), new RowHeaderModel(matchNumber));
                        row.remove(matchNumberColumnIndex);
                        // row.add(value); // Add to end for sorting
                        if (matchNumbers.size() > 0) {
                            matchNumbers.remove(matchNumber);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Some matches not scouted
            if (matchNumbers.size() > 0) {
                int rowSize = columns.size();

                for (String matchNumber : matchNumbers) {
                    try {
                        rows.add(new RowHeaderModel(matchNumber));
                        List<CellModel> naRow = new ArrayList<>();
                        for (int i = 0; i < rowSize; ++i) {
                            naRow.add(new CellModel("0_0", "N/A"));
                        }
                        cells.add(naRow);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return new DataTable(
                    columns,
                    cells,
                    rows);
        }
        else
            return null;
    }

    @Override
    public void onRowHeaderLongPressed(@NonNull RecyclerView.ViewHolder p_jRowHeaderView, int
            p_nYPosition) {

    }
}
