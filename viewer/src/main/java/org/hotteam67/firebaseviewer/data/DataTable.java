package org.hotteam67.firebaseviewer.data;

import android.util.Log;
import org.hotteam67.common.Constants;
import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * The memory model for a table, with rows and columns of strings, includes some filtering capabilities
 * that are barely used anymore, and the ability to provide a data HashMap to turn into a table. Data
 * is input in the format of HashMap<String, HashMap<String, String>>
 */
public class DataTable implements Serializable
{
    private final List<ColumnHeaderModel> columnHeaderList;
    private final List<List<CellModel>> cellList;
    private final List<RowHeaderModel> rowHeaderList;

    private List<String> preferredOrder;

    private static final String TeamNumber = "Team Number";

    private List<ColumnSchema.SumColumn> sumColumns;

    /**
     * Constructor
     *
     * @param data           the raw data to populate the table with, if it will be holding raw data
     * @param preferredOrder the preferred column order when parsing the raw data, contains
     *                       string names of the columns that should appear first
     * @param sumColumns     columns where the values are sums of a set of other columns in the row,
     *                       do not actulaly exist in the data
     */
    DataTable(HashMap<String, Object> data, List<String> preferredOrder,
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

        if (data == null)
            return;

        int row_id = 0;
        // Load rows and headers into cellmodels
        for (HashMap.Entry<String, Object> row : data.entrySet())
        {
            // Load the row
            try
            {
                HashMap<String, String> rowMap = (HashMap<String, String>) row.getValue();
                LoadRow(rowMap, row_id);
            }
            catch (Exception e)
            {
                Constants.Log(e);
            }

            if (row_id == 0)
            {
                // Load column headers on first row
                try
                {
                    if (data.entrySet().size() > 0)
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
                        for (HashMap.Entry<String, String> column : rowMap.entrySet())
                        {
                            if (!preferredOrder.contains(column.getKey()))
                            {
                                columnHeaderList.add(new ColumnHeaderModel(column.getKey()));
                                preferredOrder.add(column.getKey());
                            }
                        }
                    }
                    else
                    {
                        Constants.Log("Failed to get fire result for columns");
                    }
                }
                catch (Exception e)
                {
                    Constants.Log(e);
                }
            }

            ++row_id;
        }
    }

    /**
     * Loda one row into the table given the rowMap and the index of the row
     *
     * @param rowMap the map for the row to turn into a table row
     * @param yIndex the y index of the row in the array
     */
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

        // Hack for outdated data where notes didn't exist for part of the data due to tech. difficulties
        if (!rowMap.containsKey("Notes"))
            rowMap.put("Notes", "N/A");

        List<CellModel> row = cellList.get(yIndex);

        // Sum columns first
        for (ColumnSchema.SumColumn sumColumn : sumColumns)
        {
            int value = 0;
            for (String columnToSum : sumColumn.columnsNames)
            {
                try
                {
                    if (rowMap.keySet().contains(columnToSum))
                    {
                        String columnValue = rowMap.get(columnToSum);
                        if (columnValue.equals("true") || columnValue.equals("false"))
                            value += (Boolean.valueOf(columnValue)) ? 1 : 0;
                        else
                            value += Integer.valueOf(columnValue);
                    }
                }
                catch (Exception e)
                {
                    Constants.Log(e);
                }
            }

            row.add(new CellModel("0_0", String.valueOf(value), number));
        }

        for (ColumnHeaderModel model : columnHeaderList)
        {
            if (!rowMap.containsKey(model.getData()))
            {
                boolean contained = false;
                for (ColumnSchema.SumColumn c : sumColumns)
                {
                    if (c.columnName.equals(model.getData()))
                        contained = true;
                }
                if (!contained)
                    rowMap.put(model.getData(), "0");
            }
        }

        // Then preferred order
        for (String column : preferredOrder)
        {
            if (rowMap.keySet().contains(column))
            {
                CellModel model = new CellModel("0_0", rowMap.get(column), number);
                row.add(model);
            }
        }

        // Last is other columns
        for (HashMap.Entry<String, String> cell : rowMap.entrySet())
        {

            CellModel model = new CellModel("0_0", cell.getValue(), number);
            if (!preferredOrder.contains(cell.getKey()))
                row.add(model);
        }

        if (row.size() < columnHeaderList.size())
        {
            for (int i = 0; i < columnHeaderList.size() - row.size(); ++i)
            {
                row.add(new CellModel("0_0", "N/A", number));
            }
        }
    }

    /**
     * Set all of the values
     *
     * @param columnNames the values of the column headers
     * @param cellValues  the values of each row, as a list of lists
     * @param rowNames    the row header values, a name attached to each row
     */
    public DataTable(List<ColumnHeaderModel> columnNames, List<List<CellModel>> cellValues, List<RowHeaderModel> rowNames)
    {
        columnHeaderList = columnNames;
        cellList = cellValues;
        rowHeaderList = rowNames;
    }

    /**
     * Old filtering system, barely used, just adds to a list of filters
     *
     * @param term term to filter by.
     */
    public void SetTeamNumberFilter(String... term)
    {
        if (term.length == 0 || term[0] == null || term[0].trim().isEmpty())
            teamNumberFilters = new ArrayList<>();
        else
            teamNumberFilters = new ArrayList<>(Arrays.asList(term));
    }

    private List<String> teamNumberFilters = new ArrayList<>();

    /**
     * Get the columns header list
     *
     * @return list of ColumnHeaderModel
     */
    public List<ColumnHeaderModel> GetColumns()
    {
        return columnHeaderList;
    }

    /**
     * Get the columns names, not as part of the data model but as a list of strings
     *
     * @return list of string column names
     */
    List<String> GetColumnNames()
    {
        List<String> nameList = new ArrayList<>();
        for (ColumnHeaderModel model : columnHeaderList)
            nameList.add(model.getData());

        return nameList;
    }

    /**
     * Get list of list of cell models for the rows
     *
     * @return list of list of CellModel
     */
    public List<List<CellModel>> GetCells()
    {
        if (teamNumberFilters == null || teamNumberFilters.size() == 0)
            return cellList;
        else
        {
            try
            {
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
                Constants.Log(e);
                return cellList;
            }
        }
    }

    /**
     * Get the Row Headers for a table
     *
     * @return the RowHeaderModel list
     */
    public List<RowHeaderModel> GetRowHeaders()
    {
        if (teamNumberFilters == null || teamNumberFilters.size() == 0)
            return rowHeaderList;
        List<RowHeaderModel> filteredRows = new ArrayList<>();
        List<RowHeaderModel> unFilteredRows = new ArrayList<>(rowHeaderList);

        for (RowHeaderModel row : unFilteredRows)
        {
            if (teamNumberFilters.contains(row.getData()))
            {
                filteredRows.add(row);
            }
        }
        // Log.d("HotTeam67", "Returning rows: " + filteredRows.size());
        return filteredRows;
    }
}