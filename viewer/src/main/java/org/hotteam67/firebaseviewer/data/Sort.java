package org.hotteam67.firebaseviewer.data;

import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import org.hotteam67.common.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sort class containing one sort function that copies a data table into a sorted state
 */

public final class Sort {
    /**
     * Sort by the DataTable row header, bubble sort so very slow
     * @param input the input table to be sorted
     * @return a copy of the table sorted by its row headers
     */
    public static DataTable BubbleSortAscendingByRowHeader(DataTable input)
    {
        List<ColumnHeaderModel> columns = input.GetColumns();
        List<List<CellModel>> cells = input.GetCells();
        List<RowHeaderModel> rows = input.GetRowHeaders();

        boolean changed = true;
        while (changed)
        {
            changed = false;
            try {
                for (int i = 0; i < cells.size(); ++i)
                {
                    List<CellModel> row = cells.get(i);

                    int value = Integer.valueOf(rows.get(i).getData());

                    if (i + 1 >= cells.size())
                        continue;

                    int nextValue = Integer.valueOf(
                            rows.get(i + 1).getData());

                    if (value > nextValue)
                    {
                        cells.set(i, cells.get(i + 1));
                        RowHeaderModel prevRow = rows.get(i);
                        rows.set(i, rows.get(i + 1));
                        cells.set(i + 1, row);
                        rows.set(i + 1, prevRow);
                        changed = true;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return new DataTable(columns, cells, rows);
    }
}
