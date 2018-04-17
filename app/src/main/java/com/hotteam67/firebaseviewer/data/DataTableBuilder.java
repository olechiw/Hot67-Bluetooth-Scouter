package com.hotteam67.firebaseviewer.data;

import android.util.Log;

import com.annimon.stream.Stream;
import com.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Jakob on 1/19/2018.
 */

public class DataTableBuilder implements Serializable {
    private DataTable rawDataTable;
    private List<String> columnsNames;

    private DataTable calculatedDataTable;
    private String teamRanksJson;
    private String teamNamesJson;
    private List<Integer> calculatedColumnIndices;

    private List<ColumnSchema.OutlierAdjustedColumn> outlierAdjustedColumns;

    public final static class Calculation implements Serializable
    {
        public static final int AVERAGE = 0;
        public static final int MAXIMUM = 1;
        public static final int MINIMUM = 2;
    }

    private int calculationType;

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

        Log.d("HotTeam67", "Finding unique teams from rowheader of size: " + rawRowHeaders.size());
        for (RowHeaderModel row : rawRowHeaders)
        {
            String team = row.getData();
            if (!teamNumbers.contains(team))
                teamNumbers.add(team);
        }

        /*
        Create a calculated row for each teamnumber
         */
        int current_row = 0;
        for (String teamNumber : teamNumbers)
        {
            // Get all matches for team number
            List<List<CellModel>> matches = new ArrayList<>();
            for (List<CellModel> row : rawDataTable.GetCells())
            {
                int index = rawDataTable.GetCells().indexOf(row);
                try
                {
                    if (index != -1 && index < rawRowHeaders.size() && rawRowHeaders.get(rawDataTable.GetCells().indexOf(row))
                            .getData().equals(teamNumber))
                        matches.add(row);
                }
                catch (Exception ignored)
                {
                }
            }

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
                    values.add(s.get(column).getContent().toString());
                }

                // Calculate
                Double value = doCalculatedColumn(columnsNames.get(column), values, calculationType);

                // Add cell to row
                row.add(new CellModel(current_row + "_" + column, value.toString()));
            }

            if (calculationType == Calculation.AVERAGE)
            {
                // Add adjusted-average columns
                for (ColumnSchema.OutlierAdjustedColumn column : outlierAdjustedColumns)
                {
                    String targetColumn = column.sourceColumnName;
                    String adjustmentColumn = column.adjustmentColumnName;

                    if (!calculatedColumns.contains(targetColumn) || !calculatedColumns.contains(adjustmentColumn))
                        continue;

                    int targetIndex = calculatedColumns.indexOf(targetColumn);
                    int adjustmentIndex = calculatedColumns.indexOf(adjustmentColumn);

                    List<String> targetValues = new ArrayList<>();
                    List<String> adjustmentValues = new ArrayList<>();

                    try
                    {
                        for (List<CellModel> match : matches)
                        {
                            targetValues.add((String)match.get(targetIndex).getData());
                            adjustmentValues.add((String)match.get(adjustmentIndex).getData());
                        }

                        List<String> outliers = new ArrayList<>();
                        for (String value : targetValues)
                        {
                            int index = targetValues.indexOf(value);
                            if (Outliers.IsBelowQuartile(value, targetValues, column.sourceQuartileDisallowed) &&
                                    !Outliers.IsBelowQuartile(adjustmentValues.get(index), adjustmentValues, column.adjustmentQuartileDisallowed))
                            {
                                outliers.add(value);
                            }
                        }
                        targetValues.removeAll(outliers);

                        String cellValue = String.valueOf(doCalculatedColumn(column.columnName, targetValues, Calculation.AVERAGE));
                        CellModel cell = new CellModel("0_0", cellValue);

                        row.add(0, cell);

                        if (current_row == 0)
                        {
                            calcColumnHeaders.add(0, new ColumnHeaderModel(column.columnName));
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            // Add row to calculated list
            calcCells.add(row);
            calcRowHeaders.add(new RowHeaderModel(teamNumber));

            current_row++;
        }

        for (RowHeaderModel rowHeaderModel : calcRowHeaders)
        {
            String team = rowHeaderModel.getData();
            try {
                String teamRank = (String)  new JSONObject(teamRanksJson).get(team);
                calcCells.get(calcRowHeaders.indexOf(rowHeaderModel)).add(0,
                        new CellModel("0_0", teamRank));
            }
            catch (Exception e)
            {
                //e.printStackTrace();
                calcCells.get(calcRowHeaders.indexOf(rowHeaderModel)).add(0,
                        new CellModel("0_0", ""));
            }
        }

        calcColumnHeaders.add(0, new ColumnHeaderModel("R"));
        calculatedColumns.add(0, "R");

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
            for (int i = 0; i < cellCount; ++i)
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
                    return Math.floor(d * 1000) / 1000;
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

    public static String getCalculatedColumnName(String column, int calculation)
    {
        switch (calculation)
        {
            case Calculation.AVERAGE:
                return column + " Average";
            case Calculation.MINIMUM:
                return column + " Minimum";
            case Calculation.MAXIMUM:
                return column + " Maximum";
            default:
                return column + "Unknown";
        }
    }
}