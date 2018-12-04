package com.hotteam67.firebaseviewer.data;

import android.util.Log;

import com.annimon.stream.Stream;
import com.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import org.hotteam67.common.Constants;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Jakob on 1/19/2018.
 */

class DataTableBuilder implements Serializable {
    private final DataTable rawDataTable;
    private final List<String> columnsNames;

    private DataTable calculatedDataTable;
    private final String teamRanksJson;
    private final String teamNamesJson;
    private final List<Integer> calculatedColumnIndices;

    private final List<ColumnSchema.OutlierAdjustedColumn> outlierAdjustedColumns;

    public final static class Calculation implements Serializable
    {
        public static final int AVERAGE = 0;
        public static final int MAXIMUM = 1;
        static final int MINIMUM = 2;
    }

    private final int calculationType;

    public DataTableBuilder(DataTable rawData, List<String> calculatedColumns,
                            List<String> columnIndices,
                            List<ColumnSchema.OutlierAdjustedColumn> outlierAdjustedColumns,
                            JSONObject teamRanks, JSONObject teamNames,
                            int calculationType)
    {
        rawDataTable = rawData;
        teamNamesJson = teamNames.toString();
        columnsNames = rawData.GetColumnNames();
        this.teamRanksJson = teamRanks.toString();
        this.outlierAdjustedColumns = outlierAdjustedColumns;
        calculatedColumnIndices = new ArrayList<>();
        for (int i = 0; i < calculatedColumns.size(); ++i)
        {
            try {
                if (columnsNames.contains(columnIndices.get(i)))
                    calculatedColumnIndices.add(columnsNames.indexOf(columnIndices.get(i)));
                else
                    calculatedColumnIndices.add(-1);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                calculatedColumnIndices.add(-1);
            }
        }
        this.calculationType = calculationType;

        SetupCalculatedColumns(calculatedColumns);
    }

    private void SetupCalculatedColumns(List<String> calculatedColumns)
    {
        List<ColumnHeaderModel> calcColumnHeaders = new ArrayList<>();
        List<List<CellModel>> calcCells = new ArrayList<>();
        List<RowHeaderModel> calcRowHeaders = new ArrayList<>();

        List<RowHeaderModel> rawRowHeaders = rawDataTable.GetRowHeaders();
        List<List<CellModel>> rawRows = rawDataTable.GetCells();

        /*
        Load calculated column names
         */
        for (String s : calculatedColumns)
        {
            calcColumnHeaders.add(new ColumnHeaderModel(s));
        }

        /*
        Load every unique team number
         */
        List<String> teamNumbers = new ArrayList<>();
        HashMap<String, List<List<CellModel>>> teamRows = new HashMap<>();

        Log.d("HotTeam67", "Finding unique teams from rowheader of size: " + rawRowHeaders.size());
        int i = 0;
        for (RowHeaderModel row : rawRowHeaders)
        {
            try {
                String teamNumber = row.getData();
                // Already seen, add to existing list for team
                if (teamRows.containsKey(teamNumber))
                    teamRows.get(teamNumber).add(rawRows.get(i));
                // Newly seen team, make a new list of matches for them
                else {
                    teamNumbers.add(teamNumber);
                    List<List<CellModel>> rows = new ArrayList<>();
                    rows.add(rawRows.get(i));
                    teamRows.put(teamNumber, rows);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            ++i;
        }

        /*
        Create a calculated row for each teamNumber using each of the stored matches
         */
        int current_row = 0;
        for (String teamNumber : teamNumbers)
        {
            // Get all matches for team number
            List<List<CellModel>> matches = teamRows.get(teamNumber);

            List<CellModel> row = new ArrayList<>();
            for (int column : calculatedColumnIndices)
            {
                if (column == -1) {
                    row.add(new CellModel("0_0", "N/A"));
                    continue;
                }

                List<String> values = new ArrayList<>();
                // Get raw data collection
                for (List<CellModel> s : matches)
                {
                    values.add(s.get(column).getData());
                }

                // Calculate
                String value = String.valueOf(doCalculatedColumn(columnsNames.get(column), values, calculationType));

                // Add cell to row
                row.add(new CellModel(current_row + "_" + column, value));
            }
            // Team number at end
            row.add(0, new CellModel("0_0", teamNumber));

            // Add row to calculated list
            calcCells.add(row);
            calcRowHeaders.add(new RowHeaderModel(teamNumber));

            current_row++;
        }

        for (int r = 0; r < calcCells.size(); ++r )
        {
            String team = calcCells.get(r).get(0).getData();
            try {
                String teamRank = (String)  new JSONObject(teamRanksJson).get(team);
                calcCells.get(r).add(1,
                        new CellModel("0_0", teamRank));
            }
            catch (Exception e)
            {
                //e.printStackTrace();
                calcCells.get(r).add(1,
                        new CellModel("0_0", ""));
            }
        }

        // Rank and team number are the two non-calculated columns, so add them manually
        calcColumnHeaders.add(0, new ColumnHeaderModel("Team"));
        calculatedColumns.add(0, "Team Number");
        calcColumnHeaders.add(1, new ColumnHeaderModel("R"));
        calculatedColumns.add(1, "R");

        List<String> extraTeams = new ArrayList<>();
        // Do N/A Teams
        try {
            Iterator<?> teamsIterator = new JSONObject(teamNamesJson).keys();
            while (teamsIterator.hasNext()) {
                String s = (String) teamsIterator.next();
                if (!teamNumbers.contains(s)) extraTeams.add(s);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        int cellCount = calcColumnHeaders.size();
        for (String s : extraTeams)
        {
            RowHeaderModel rowHeaderModel = new RowHeaderModel(s);
            calcRowHeaders.add(rowHeaderModel);

            List<CellModel> row = new ArrayList<>();
            for (int c = 0; c < cellCount; ++c)
            {
                row.add(new CellModel("0_0", "N/A"));
            }
            calcCells.add(row);
        }

        calculatedDataTable = new DataTable(calcColumnHeaders, calcCells, calcRowHeaders);
    }

    public DataTable GetTable()
    {
        return calculatedDataTable;
    }

    private static double doCalculatedColumn(String columnName, List<String> columnValues,
                                             int calculation)
    {
        switch (calculation)
        {
            case Calculation.AVERAGE:
            {
                try
                {
                    double d = 0;
                    for (String s : columnValues) {
                        //Log.e("FirebaseScouter", "Averaging : " + s);
                        d += ConvertToDouble(s);
                    }

                    d /= columnValues.size();
                    return Constants.Round(d, 1);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.e("FirebaseScouter",
                            "Failed to do average calculation on column: " + columnName);
                    return -1;
                }
            }
            case Calculation.MAXIMUM:
                try
                {
                    double d = 0;
                    for (String s : columnValues) {
                        if (ConvertToDouble(s) > d)
                            d = ConvertToDouble(s);
                    }
                    return d;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.e("FirebaseScouter",
                            "Failed to do max calculation on column: " + columnName);
                    return -1;
                }
            case Calculation.MINIMUM:
                try
                {
                    return Stream.of(columnValues)
                            // Convert to number
                            .mapToDouble(DataTableBuilder::ConvertToDouble)
                            .min().getAsDouble();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.e("FirebaseScouter",
                            "Failed to do max calculation on column: " + columnName);
                    return -1;
                }
            default:
                return -1;
        }
    }

    /*
    Safely converts either a boolean or number string to a double, for averaging, minimizing and
    maximizing
     */
    private static double ConvertToDouble(String s)
    {
        switch (s) {
            case "true":
                return 1;
            case "false":
                return 0;
            default:
                return Double.valueOf(s);
        }
    }

}