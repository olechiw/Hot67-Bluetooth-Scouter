package org.hotteam67.firebaseviewer.tableview;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import com.evrencoskun.tableview.ITableView;
import com.evrencoskun.tableview.listener.ITableViewListener;
import com.evrencoskun.tableview.sort.SortState;
import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.hotteam67.firebaseviewer.RawDataActivity;
import org.hotteam67.firebaseviewer.ViewerActivity;
import org.hotteam67.firebaseviewer.data.*;
import org.hotteam67.firebaseviewer.tableview.holder.CellViewHolder;
import org.hotteam67.firebaseviewer.tableview.holder.ColumnHeaderViewHolder;
import org.hotteam67.firebaseviewer.tableview.holder.RowHeaderViewHolder;
import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The listener for all onTouch events for the TableView
 */
public class MainTableViewListener implements ITableViewListener
{

    private final ITableView tableView;
    private final MainTableAdapter adapter;

    /**
     * Constructor
     *
     * @param tableView the table view that events will come from
     * @param adapter   the main adapter for the tableView
     */
    public MainTableViewListener(ITableView tableView, MainTableAdapter adapter)
    {
        this.tableView = tableView;
        this.adapter = adapter;
    }

    /**
     * When a cell is clicked - if there is rawData then it is a calculated table, so show a
     * ScatterPlot with values from the DataModel.
     *
     * @param cellView the cell that was clicked
     * @param column   the column it was clicked in
     * @param row      the row it was clicked in
     */
    @Override
    public void onCellClicked(@NonNull RecyclerView.ViewHolder cellView, int column, int
            row)
    {

        // Ignored for first column - rankings. HACK btw
        if (column == 0) return;

        MainTableAdapter adapter = (MainTableAdapter) tableView.getAdapter();

        // No event for raw data
        if (adapter.GetContext() instanceof RawDataActivity) return;

        DataTable rawData = DataModel.GetRawData();

        // No raw data means something is not loaded properly
        if (rawData == null)
            return;

        try
        {
            CellModel cell = ((CellViewHolder) cellView).getCellModel();
            if (cell == null) return;
            String teamNumber = cell.getTeamNumber();

            DataTable table = GetFormattedRawData(teamNumber);
            if (table == null) return;
            table = Sort.BubbleSortAscendingByRowHeader(table);

            // -1 to account for ranking column. HACK btw
            String rawColumnName = ColumnSchema.CalculatedColumns().get(column - 1).RawName;

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
            for (List<CellModel> cells : table.GetCells())
            {
                String value = cells.get(index).getData();
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
            JSONObject teamNumbersNames = DataModel.GetTeamsNumbersNames();
            if (!(teamNumbersNames == null) && teamNumbersNames.has(teamNumber))
                title += " - " + teamNumbersNames.get(teamNumber);
            title += ": " + rawColumnName;

            ScatterPlot.Show(
                    values, adapter.GetContext(), title);
        }
        catch (Exception ignored)
        {
            // Will throw an exception if RawData can't be found
        }
    }

    /**
     * Get the row header data for a given row, DOES NOT WORK PROPERLY WITH SORTING
     *
     * @param row the row number
     * @return the value
     */
    private String GetRowHeaderValue(int row)
    {
        List rowHeaders = adapter.getRowHeaderRecyclerViewAdapter().getItems();
        try
        {
            return ((RowHeaderModel) rowHeaders.get(row)).getData();
        }
        catch (Exception e)
        {
            Constants.Log(e);
            return "-1";
        }
    }

    /**
     * Cell long press listener, ignored
     *
     * @param cellView the view long pressed
     * @param column   the xPosition
     * @param row      the yPosition
     */
    @Override
    public void onCellLongPressed(@NonNull RecyclerView.ViewHolder cellView, int column, int row)
    {

    }

    private int lastColumnClicked = -1;

    /**
     * The columnHeaderClicked listener, sorts ascending/descending if not in RawDataActivity
     *
     * @param columnViewHolder the ViewHolder that was clicked
     * @param column           the column index of the clicked column
     */
    @Override
    public void onColumnHeaderClicked(@NonNull RecyclerView.ViewHolder columnViewHolder, int
            column)
    {

        if (adapter.GetContext() instanceof RawDataActivity)
            return;
        else if (!(columnViewHolder instanceof ColumnHeaderViewHolder))
            return;

        if (lastColumnClicked == column)
        {
            lastColumnClicked = -1;
            adapter.getTableView().sortColumn(column, SortState.ASCENDING);
        }
        else
        {
            lastColumnClicked = column;
            adapter.getTableView().sortColumn(column, SortState.DESCENDING);
        }
        adapter.getTableView().scrollToRowPosition(0);
    }


    /**
     * Column header long press is ignored
     *
     * @param columnHeaderView the view pressed
     * @param xPosition        the xPosition of the column
     */
    @Override
    public void onColumnHeaderLongPressed(@NonNull RecyclerView.ViewHolder columnHeaderView,
                                          int xPosition)
    {
    }

    /**
     * rowHeader Clicked. If it is RawDataActivity, end with the selected Match Number to be shown
     * in calculated data. Otherwise Start the RawDataActivity
     *
     * @param rowHeaderView the view that was clicked, will have the team number
     * @param yPosition     the yPosition of the row header
     */
    @Override
    public void onRowHeaderClicked(@NonNull RecyclerView.ViewHolder rowHeaderView, int
            yPosition)
    {


        if (adapter.GetContext() instanceof RawDataActivity)
        {
            try
            {
                ((RawDataActivity) adapter.GetContext()).doEndWithMatchNumber(Integer.valueOf(GetRowHeaderValue(yPosition)));
            }
            catch (Exception e)
            {
                Constants.Log(e);
            }

            return;
        }

        String teamNumber = ((RowHeaderViewHolder) rowHeaderView).rowHeaderTextView.getText().toString();

        Log.d("HotTeam67", "Set team number filter: " + teamNumber);

        DataTable formattedData = GetFormattedRawData(teamNumber);

        Intent rawDataIntent = new Intent(adapter.GetContext(), RawDataActivity.class);
        rawDataIntent.putExtra(RawDataActivity.RAW_DATA_ATTRIBUTE, formattedData);
        rawDataIntent.putExtra(RawDataActivity.TEAM_NUMBER_ATTRIBUTE, teamNumber);

        ViewerActivity activity = (ViewerActivity) adapter.GetContext();
        try
        {
            rawDataIntent.putExtra(RawDataActivity.TEAM_NAME_ATTRIBUTE, (String) DataModel
                    .GetTeamsNumbersNames().get(teamNumber));
        }
        catch (Exception e)
        {
            Constants.Log(e);
        }

        activity.startActivityForResult(rawDataIntent, Constants.RawDataRequestCode);

    }

    /**
     * Get the formatted raw data from the DataModel for a given team, to be given to the RawDataActivity
     *
     * @param teamNumber team number to get data for
     * @return a DataTable containing the formatted/sorted data with match numbers as row headers etc.
     */
    private DataTable GetFormattedRawData(String teamNumber)
    {
        DataTable rawData = DataModel.GetRawData();
        rawData.SetTeamNumberFilter(teamNumber);

        /*
        Copy to final data
         */
        List<List<CellModel>> cells = new ArrayList<>();
        List<List<CellModel>> preCopyData = rawData.GetCells();
        for (List<CellModel> row : preCopyData)
        {
            ArrayList<CellModel> newRow = new ArrayList<>(row);
            cells.add(newRow);
        }

        List<RowHeaderModel> rows = new ArrayList<>(rawData.GetRowHeaders());
        List<ColumnHeaderModel> columns = new ArrayList<>(rawData.GetColumns());


        /*
        Remove match number, set as row header, add all of the teams unscouted matches
         */
        String matchNumber1 = "Match Number";
        if (columns.size() == 0 || !columns.get(0).getData().equals(matchNumber1))
        {
            int matchNumberColumnIndex = -1;
            /*
            Prep full team schedule
             */
            List<String> matchNumbers = new ArrayList<>();
            String matches = FileHandler.LoadContents(FileHandler.Files.VIEWER_MATCHES_FILE);
            if (!matches.trim().isEmpty())
            {
                List<String> matchesArray = Arrays.asList(matches.split("\n"));
                if (matchesArray.size() > 0)
                    // Load all team matches
                    for (String match : matchesArray)
                    {
                        if (Arrays.asList(match.split(",")).contains(teamNumber))
                            // +1 to make it from index to actual match number
                            matchNumbers.add(String.valueOf(matchesArray.indexOf(match) + 1));
                    }
            }
            /*
            Move header
             */
            for (ColumnHeaderModel column : columns)
            {
                if (column.getData().equals(matchNumber1))
                {
                    matchNumberColumnIndex = columns.indexOf(column);
                }
            }

            /*
            Move value in each row
             */
            if (matchNumberColumnIndex != -1)
            {
                try
                {
                    columns.remove(matchNumberColumnIndex);
                    // columns.add(new ColumnHeaderModel("Match Number"));
                    for (List<CellModel> row : cells)
                    {
                        CellModel value = row.get(matchNumberColumnIndex);
                        String matchNumber = value.getData();
                        rows.set(cells.indexOf(row), new RowHeaderModel(matchNumber));
                        row.remove(matchNumberColumnIndex);
                        // row.add(value); // Add to end for sorting
                        if (matchNumbers.size() > 0)
                        {
                            matchNumbers.remove(matchNumber);
                        }
                    }
                }
                catch (Exception e)
                {
                    Constants.Log(e);
                }
            }

            // Some matches not scouted
            if (matchNumbers.size() > 0)
            {
                int rowSize = columns.size();

                for (String matchNumber : matchNumbers)
                {
                    try
                    {
                        rows.add(new RowHeaderModel(matchNumber));
                        List<CellModel> naRow = new ArrayList<>();
                        for (int i = 0; i < rowSize; ++i)
                        {
                            naRow.add(new CellModel("0_0", "N/A", ""));
                        }
                        cells.add(naRow);
                    }
                    catch (Exception e)
                    {
                        Constants.Log(e);
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

    /**
     * Long press ignored
     *
     * @param rowHeaderView the row header view pressed
     * @param yPosition     the yPosition of the row
     */
    @Override
    public void onRowHeaderLongPressed(@NonNull RecyclerView.ViewHolder rowHeaderView, int
            yPosition)
    {

    }
}
