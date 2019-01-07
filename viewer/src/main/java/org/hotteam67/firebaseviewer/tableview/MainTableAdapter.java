package org.hotteam67.firebaseviewer.tableview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.data.DataTable;
import org.hotteam67.firebaseviewer.tableview.holder.CellViewHolder;
import org.hotteam67.firebaseviewer.tableview.holder.ColumnHeaderViewHolder;
import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by evrencoskun on 27.11.2017.
 */

public class MainTableAdapter extends AbstractTableAdapter<ColumnHeaderModel, RowHeaderModel,
        CellModel> {
    public MainTableAdapter(Context p_jContext) {
        super(p_jContext);
    }

    private HashMap<String, Integer> rowHeaderHighlights = new HashMap<>();

    public void SetRowHeaderHighlight(String rowHeader, Integer color)
    {
        rowHeaderHighlights.put(rowHeader, color);
    }

    public void RemoveRowHeaderHighlight(String rowHeader)
    {
        if (rowHeaderHighlights.containsKey(rowHeader))
            rowHeaderHighlights.remove(rowHeader);
    }

    public void RemoveAllRowHeaderHighlights()
    {
        rowHeaderHighlights = new HashMap<>();
    }

    @Override
    public AbstractViewHolder onCreateCellViewHolder(ViewGroup parent, int viewType) {
        View layout;

        layout = LayoutInflater.from(mContext).inflate(R.layout.tableview_cell_layout,
                parent, false);

        // Create a Cell ViewHolder
        return new CellViewHolder(layout);
    }

    @Override
    public void onBindCellViewHolder(AbstractViewHolder holder, Object value, int
            xPosition, int yPosition) {
        CellModel cell = (CellModel) value;
        String rowHeader = getRowHeaderItem(xPosition).getData();
        if (rowHeaderHighlights.containsKey(rowHeader))
            holder.setBackgroundColor(rowHeaderHighlights.get(rowHeader));

        if (holder instanceof CellViewHolder) {
            // Get the holder to update cell item text
            ((CellViewHolder) holder).setCellModel(cell);
        }
    }


    public void setAllItems(DataTable mainTable)
    {
        setAllItems(mainTable.GetColumns(), mainTable.GetRowHeaders(), mainTable.GetCells());
    }

    public Context GetContext()
    {
        return this.mContext;
    }

    @Override
    public AbstractViewHolder onCreateColumnHeaderViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout
                .tableview_column_header_layout, parent, false);

        return new ColumnHeaderViewHolder(layout);
    }

    @Override
    public void onBindColumnHeaderViewHolder(AbstractViewHolder holder, Object p_jValue, int
            p_nXPosition) {
        ColumnHeaderModel columnHeader = (ColumnHeaderModel) p_jValue;

        // Get the holder to update cell item text
        ColumnHeaderViewHolder columnHeaderViewHolder = (ColumnHeaderViewHolder) holder;

        columnHeaderViewHolder.setColumnHeaderModel(columnHeader);
    }

    @Override
    public AbstractViewHolder onCreateRowHeaderViewHolder(ViewGroup parent, int viewType) {

        // Get Row Header xml Layout
        View layout = LayoutInflater.from(mContext).inflate(R.layout
                .tableview_row_header_layout, parent, false);

        // Create a Row Header ViewHolder
        return new RowHeaderViewHolder(layout);
    }

    @Override
    public void onBindRowHeaderViewHolder(AbstractViewHolder holder, Object p_jValue, int
            p_nYPosition) {

        RowHeaderModel rowHeaderModel = (RowHeaderModel) p_jValue;

        RowHeaderViewHolder rowHeaderViewHolder = (RowHeaderViewHolder) holder;
        try {
            rowHeaderViewHolder.row_header_textview.setText(String.valueOf(rowHeaderModel.getData()));
        }
        catch (Exception e)
        {
            rowHeaderViewHolder.row_header_textview.setText("ERROR");
        }

    }

    @Override
    public View onCreateCornerView() {
        return LayoutInflater.from(mContext).inflate(R.layout.tableview_corner_layout, null,
                false);
    }

    @Override
    public int getColumnHeaderItemViewType(int position) {
        return 0;
    }

    @Override
    public int getRowHeaderItemViewType(int position) {
        return 0;
    }

    @Override
    public int getCellItemViewType(int position) {

        // Not needed, using the data model instead
        return 0;
    }
}
