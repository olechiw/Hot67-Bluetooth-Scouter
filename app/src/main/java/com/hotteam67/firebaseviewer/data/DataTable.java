package com.hotteam67.firebaseviewer.data;

import android.util.Log;

import com.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Jakob on 1/18/2018.
 */

public class DataTable implements Serializable {
    private List<ColumnHeaderModel> columnHeaderList;
    private List<List<CellModel>> cellList;
    private List<RowHeaderModel> rowHeaderList;

    private List<String> preferredOrder;

    private final String TeamNumber = "Team Number";

    private List<ColumnSchema.SumColumn> sumColumns;

    public DataTable(HashMap<String, Object> rawData, List<String> preferredOrder,
                     List<ColumnSchema.SumColumn> sumColumns)
    {
        /*
        Load the Raw Data into model
         */
        this.preferredOrder = preferredOrder;

        columnHeaderList = new ArrayList<>();
        cellList = new ArrayList<>();
        rowHeaderList = new ArrayList<>();

        this.sumColumns = sumColumns;

        if (rawData == null)
            return;

        int row_id = 0;
        // Load rows and headers into cellmodels
        for (HashMap.Entry<String, Object> row : rawData.entrySet())
        {
            // Load the row
            try {
                HashMap<String, String> rowMap = (HashMap<String, String>) row.getValue();
                LoadRow(rowMap, row_id);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if (row_id == 0)
            {
                // Load column headers on first row
                try
                {
                    if (rawData.entrySet().size() > 0)
                    {
                        HashMap<String, String> rowMap = (HashMap<String, String>) row.getValue();

                        for (ColumnSchema.SumColumn sumColumn : sumColumns)
                            columnHeaderList.add(new ColumnHeaderModel(sumColumn.columnName));

                        for (String column : preferredOrder)
                        {
                            if (rowMap.keySet().contains(column))
                            {
                                columnHeaderList.add(new ColumnHeaderModel(column));
                            }
                        }

                        // TeamNumber
                        rowMap.remove(TeamNumber);
                        //columnHeaderList.add(new ColumnHeaderModel(TeamNumber));
                        for (HashMap.Entry<String, String> column : rowMap.entrySet()) {
                            if (!preferredOrder.contains(column.getKey()))
                                columnHeaderList.add(new ColumnHeaderModel(column.getKey()));
                        }
                    }
                    else
                    {
                        Log.e("FirebaseScouter", "Failed to get fire result for columns");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            ++row_id;
        }
    }

    public void Set(List<RowHeaderModel> rows, List<List<CellModel>> cells, List<ColumnHeaderModel> columns)
    {
        rowHeaderList = rows;
        cellList = cells;
        columnHeaderList = columns;
    }

    private void LoadRow(HashMap<String, String> rowMap, int yIndex)
    {
        cellList.add(new ArrayList<>());

        // TeamNumber - before everything else
        String number = rowMap.get(TeamNumber);
        if (number == null)
            number = "";
        //cellList.get(row_id).add(new CellModel(row_id + "_0", number));
        rowHeaderList.add(new RowHeaderModel(number));
        rowMap.remove(TeamNumber);

        List<CellModel> row = cellList.get(yIndex);

        // Sum columns first
        for (ColumnSchema.SumColumn sumColumn : sumColumns)
        {
            int value = 0;
            for (String columnToSum : sumColumn.columnsNames)
            {
                try {
                    if (rowMap.keySet().contains(columnToSum)) {
                        String columnValue = rowMap.get(columnToSum);
                        if (columnValue.equals("true") || columnValue.equals("false"))
                            value += (Boolean.valueOf(columnValue)) ? 1 : 0;
                        else
                            value += Integer.valueOf(columnValue);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            row.add(new CellModel("0_0", String.valueOf(value)));
        }

        // Then preferred order
        for (String column : preferredOrder)
        {
            if (rowMap.keySet().contains(column))
            {
                CellModel model = new CellModel("0_0", rowMap.get(column));
                row.add(model);
            }
        }


        // Last is other columns
        for (HashMap.Entry<String, String> cell : rowMap.entrySet()) {

            CellModel model = new CellModel("0_0", cell.getValue());
            if (!preferredOrder.contains(cell.getKey()))
                row.add(model);
        }

        if (row.size() < columnHeaderList.size())
        {
            for (int i = 0; i < columnHeaderList.size() - row.size(); ++i)
            {
                row.add(new CellModel("0_0", "N/A"));
            }
        }
    }

    public DataTable(List<ColumnHeaderModel> columnNames, List<List<CellModel>> cellValues, List<RowHeaderModel> rowNames)
    {
        columnHeaderList = columnNames;
        cellList = cellValues;
        rowHeaderList = rowNames;
    }


    public void SetTeamNumberFilter(String term)
    {
        teamNumberFilter = term;
    }

    private String teamNumberFilter = "";

    public List<ColumnHeaderModel> GetColumns()
    {
        return columnHeaderList;
    }

    public List<String> GetColumnNames()
    {
        List<String> nameList = new ArrayList<>();
        for (ColumnHeaderModel model : columnHeaderList)
            nameList.add(model.getData());

        return nameList;
    }

    public List<List<CellModel>> GetCells()
    {
        if (teamNumberFilter == null)
            return cellList;
        else if (teamNumberFilter.trim().isEmpty())
            return cellList;
        else
        {
            try {
                List<RowHeaderModel> filteredRows = GetRowHeaders();
                List<List<CellModel>> cells = new ArrayList<>();
                if (filteredRows.size() > 0)
                {
                    for (RowHeaderModel row : filteredRows)
                    {
                        cells.add(cellList.get(rowHeaderList.indexOf(row)));
                    }
                }
                // Log.d("HotTeam67", "Returning cells: " + cells.size());
                return cells;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return cellList;
            }
        }
    }

    public List<RowHeaderModel> GetRowHeaders()
    {
        if (teamNumberFilter == null)
            return rowHeaderList;
        if (teamNumberFilter.trim().isEmpty())
            return rowHeaderList;
        List<RowHeaderModel> filteredRows = new ArrayList<>();
        List<RowHeaderModel> unFilteredRows = new ArrayList<>();
        unFilteredRows.addAll(rowHeaderList);

        for (RowHeaderModel row : unFilteredRows)
        {
            if (row.getData().equals(teamNumberFilter))
            {
                filteredRows.add(row);
            }
        }
        // Log.d("HotTeam67", "Returning rows: " + filteredRows.size());
        return filteredRows;
    }

}
