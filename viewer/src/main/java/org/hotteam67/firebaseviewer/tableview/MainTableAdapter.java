package org.hotteam67.firebaseviewer.tableview;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.CellRecyclerViewAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.ColumnHeaderRecyclerViewAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.RowHeaderRecyclerViewAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.hotteam67.common.TBAHandler;
import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.data.DataTable;
import org.hotteam67.firebaseviewer.tableview.holder.AllianceViewHolder;
import org.hotteam67.firebaseviewer.tableview.holder.CellViewHolder;
import org.hotteam67.firebaseviewer.tableview.holder.ColumnHeaderViewHolder;
import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import java.util.List;

/**
 * Created by evrencoskun on 27.11.2017.
 */

public class MainTableAdapter extends AbstractTableAdapter<ColumnHeaderModel, RowHeaderModel,
        CellModel> {
    private static final int ALLIANCE_CELL = 1;

    public MainTableAdapter(Context p_jContext) {
        super(p_jContext);
    }

    /*
    private HashMap<String, Integer> rowHeaderHighlights = new HashMap<>();

    public void SetRowHeaderHighlight(String rowHeader, Integer color) {
        rowHeaderHighlights.put(rowHeader, color);
    }
    */

    /*
    public void RemoveAllRowHeaderHighlights() {
        rowHeaderHighlights = new HashMap<>();
    }
    */

    @Override
    public AbstractViewHolder onCreateCellViewHolder(ViewGroup parent, int viewType) {
        View layout;

        switch (viewType) {
            case ALLIANCE_CELL:
                layout = LayoutInflater.from(mContext).inflate(R.layout.tableview_alliance,
                        parent, false);
                return new AllianceViewHolder(layout);
            default:
                layout = LayoutInflater.from(mContext).inflate(R.layout.tableview_cell_layout,
                        parent, false);
        }

        // Create a Cell ViewHolder
        return new CellViewHolder(layout);
    }

    @Override
    public void onBindCellViewHolder(AbstractViewHolder holder, Object value, int
            xPosition, int yPosition) {
        CellModel cell = (CellModel) value;
        if (cell.isAlliance())
        {
            if (cell.getContent() == "BLUE")
                holder.itemView.setBackgroundColor(Color.BLUE);
            else
                holder.itemView.setBackgroundColor(Color.RED);
        }

        /*
        if (rowHeaderHighlights.containsKey(rowHeader))
            holder.setBackgroundColor(rowHeaderHighlights.get(rowHeader));
            */

        if (holder instanceof CellViewHolder) {
            // Get the holder to update cell item text
            ((CellViewHolder) holder).setCellModel(cell);
        }
        else if (holder instanceof AllianceViewHolder)
        {
            ((AllianceViewHolder) holder).setCellModel(cell);
        }
    }


    public void setAllItems(DataTable mainTable) {
        setAllItems(mainTable.GetColumns(), mainTable.GetRowHeaders(), mainTable.GetCells());
    }

    Context GetContext() {
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
        } catch (Exception e) {
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

    //TODO: ADD AN ALLIANCE COLUMN HERE WITH RED OR BLUE
    @Override
    public int getCellItemViewType(int position) {

        if (position == 0 && hasAllianceColumns())
            return ALLIANCE_CELL;
        // Not needed, using the data model instead
        return 0;
    }

    public void setAlliance(TBAHandler.Match match) {

        CellRecyclerViewAdapter cells = ((CellRecyclerViewAdapter)getTableView().getCellRecyclerView().getAdapter());
        ColumnHeaderRecyclerViewAdapter headers = ((ColumnHeaderRecyclerViewAdapter)getTableView().getColumnHeaderRecyclerView().getAdapter());
        RowHeaderRecyclerViewAdapter rows = ((RowHeaderRecyclerViewAdapter)getTableView().getRowHeaderRecyclerView().getAdapter());

        List<List<CellModel>> cellsList = ((List<List<CellModel>>)cells.getItems());
        if (hasAllianceColumns()) {
            for (int i = 0; i < cellsList.size(); ++i)
            {
                cellsList.get(i).remove(0);
            }
            if (headers.getItems().size() > 0)
                headers.getItems().remove(0);
        }
        if (match != null)
        {

            headers.getItems().add(0, new ColumnHeaderModel("A"));
            for (int i = 0; i < cells.getItemCount(); ++i)
            {
                boolean blueNotRed;
                String rowval = ((RowHeaderModel) rows.getItems().get(i)).getData();
                blueNotRed = match.blueTeams.contains(rowval);

                CellModel model = new CellModel("0_0", (blueNotRed) ? "BLUE" : "RED", true);
                ((List<CellModel>) cells.getItems().get(i)).add(0, model);
            }
        }

        cells.setItems(cells.getItems());
        headers.setItems(headers.getItems());
    }

    public void clearAllianceColumns()
    {
        if (hasAllianceColumns())
        {
            setAlliance(null);
        }
    }

    private boolean hasAllianceColumns()
    {
        // Check for existing alliance column
        CellRecyclerViewAdapter cells = ((CellRecyclerViewAdapter)getTableView().getCellRecyclerView().getAdapter());
        if (cells.getItems().size() > 0)
        {
            CellModel c1 = ((List<List<CellModel>>) cells.getItems()).get(0).get(0);
            return c1.isAlliance();
        }
        return false;
    }
}
