package org.hotteam67.firebaseviewer.tableview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.CellRecyclerViewAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.RowHeaderRecyclerViewAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.hotteam67.common.Constants;
import org.hotteam67.common.TBAHandler;
import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.data.DataTable;
import org.hotteam67.firebaseviewer.tableview.holder.CellViewHolder;
import org.hotteam67.firebaseviewer.tableview.holder.ColumnHeaderViewHolder;
import org.hotteam67.firebaseviewer.tableview.holder.RowHeaderViewHolder;
import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import java.util.List;

/**
 * The adapter responsible for populating views based on the data model. Basically just three
 * adapters for a RecyclerView built into one, for rows/columns/cells.
 *
 * A note about ViewType: it can be used to create different holders with more sophisticated data,
 * this program only uses one type for everything
 */
public class MainTableAdapter extends AbstractTableAdapter<ColumnHeaderModel, RowHeaderModel,
        CellModel> {

    public MainTableAdapter(Context context) {
        super(context);
    }


    /**
     * Create a viewholder from the template layout
     * @param parent the parent to use with the inflator
     * @param viewType the ViewType from getCellViewType(), unused atm
     * @return the viewholder to create, always a CellViewHolder
     */
    @Override
    public AbstractViewHolder onCreateCellViewHolder(ViewGroup parent, int viewType) {
        View layout;

        layout = LayoutInflater.from(mContext).inflate(R.layout.tableview_cell_layout,
                parent, false);

        // Create a Cell ViewHolder
        return new CellViewHolder(layout);
    }

    /**
     * When a value from the model (a CellModeL) is bound to a ViewHolder. Just call the holder's
     * population function
     * @param holder the view to populate
     * @param value the CellModel to populate with
     * @param xPosition the xPos of the cell
     * @param yPosition the yPos of the cell
     */
    @Override
    public void onBindCellViewHolder(AbstractViewHolder holder, Object value, int
            xPosition, int yPosition) {
        CellModel cell = (CellModel) value;

        if (holder instanceof CellViewHolder) {
            // Get the holder to update cell item text
            ((CellViewHolder) holder).setCellModel(cell);
        }
    }


    /**
     * Set all of the data items based on a given DataTable
     * @param mainTable the table to use to populate everything
     */
    public void setAllItems(DataTable mainTable) {
        setAllItems(mainTable.GetColumns(), mainTable.GetRowHeaders(), mainTable.GetCells());
    }

    /**
     * Get the adapter context
     * @return the stored context from the Adapter's constructor
     */
    Context GetContext() {
        return this.mContext;
    }

    /**
     * Populate a column header view holder, ignoring viewtype
     * @param parent the parent to use with the inflator
     * @param viewType from getColumnViewType, ignored
     * @return a ColumnHeaderViewHolder that will go into the RecyclerView
     */
    @Override
    public AbstractViewHolder onCreateColumnHeaderViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout
                .tableview_column_header_layout, parent, false);

        return new ColumnHeaderViewHolder(layout);
    }

    /**
     * Bind a column header view holder with a given model
     * @param holder the holder to bind
     * @param value the ColumnHeaderModel to bind the values with
     * @param xPosition the x Position of the column
     */
    @Override
    public void onBindColumnHeaderViewHolder(AbstractViewHolder holder, Object value, int
            xPosition) {
        ColumnHeaderModel columnHeader = (ColumnHeaderModel) value;

        // Get the holder to update cell item text
        ColumnHeaderViewHolder columnHeaderViewHolder = (ColumnHeaderViewHolder) holder;
        columnHeaderViewHolder.setColumnHeaderModel(columnHeader);
    }

    /**
     * When a rowHeaderViewHolder is created, inflate it from the layout template
     * @param parent the parent to use with the inflater
     * @param viewType the type of view, ignored
     * @return the inflated ViewHolder
     */
    @Override
    public AbstractViewHolder onCreateRowHeaderViewHolder(ViewGroup parent, int viewType) {

        // Get Row Header xml Layout
        View layout = LayoutInflater.from(mContext).inflate(R.layout
                .tableview_row_header_layout, parent, false);

        // Create a Row Header ViewHolder
        return new RowHeaderViewHolder(layout);
    }

    /**
     * When a rowHeaderModel is bound to a specific value, populate it into the holder
     * @param holder the holder to populate
     * @param value the value to populate it with
     * @param xPosition the xPosition of the rowHeader
     */
    @Override
    public void onBindRowHeaderViewHolder(AbstractViewHolder holder, Object value, int
            xPosition) {

        RowHeaderModel rowHeaderModel = (RowHeaderModel) value;

        RowHeaderViewHolder rowHeaderViewHolder = (RowHeaderViewHolder) holder;
        try {
            rowHeaderViewHolder.rowHeaderTextView.setText(String.valueOf(rowHeaderModel.getData()));
        } catch (Exception e) {
            rowHeaderViewHolder.rowHeaderTextView.setText("ERROR");
        }

        rowHeaderViewHolder.setAlliance(rowHeaderModel.GetAlliance());
    }

    /**
     * The inflator for the corner view. Doesn't have functionality, we leave it simple
     * @return the corner View inflated
     */
    @Override
    public View onCreateCornerView() {
        return LayoutInflater.from(mContext).inflate(R.layout.tableview_corner_layout, null,
                false);
    }

    /**
     * The viewType functions, unused rn
     * @param position the position of the ColumnHeaderModel to get a ViewType for
     * @return 0 always, default view type
     */
    @Override
    public int getColumnHeaderItemViewType(int position) {
        return 0;
    }

    /**
     * The viewType functions, unused rn
     * @param position the position of the RowHeaderModel to get a ViewType for
     * @return 0 always, default view type
     */
    @Override
    public int getRowHeaderItemViewType(int position) {
        return 0;
    }

    /**
     * The viewType functions, unused rn
     * @param position the position of the CellModel to get a ViewType for
     * @return 0 always, default view type
     */
    @Override
    public int getCellItemViewType(int position) {

        return 0;
    }

    /**
     * Set the alliances for a current match, flagging CellModels as being Red or Blue alliance,
     * by parsing team number, so when they are bound the holders will adjust the
     * background color likewise
     * @param match the Match object to use
     */
    public void setAllianceHighlights(TBAHandler.Match match) {

        CellRecyclerViewAdapter cells = ((CellRecyclerViewAdapter)getTableView().getCellRecyclerView().getAdapter());
//        ColumnHeaderRecyclerViewAdapter headers = ((ColumnHeaderRecyclerViewAdapter)getTableView().getColumnHeaderRecyclerView().getAdapter());
        RowHeaderRecyclerViewAdapter rows = ((RowHeaderRecyclerViewAdapter)getTableView().getRowHeaderRecyclerView().getAdapter());

        List<List<CellModel>> cellsList = ((List<List<CellModel>>)cells.getItems());
        for (List<CellModel> row : cellsList)
            for (CellModel cell : row)
                cell.SetAlliance(Constants.ALLIANCE_NONE);
        for (RowHeaderModel row : ((List<RowHeaderModel>)rows.getItems()))
            row.SetAlliance(Constants.ALLIANCE_NONE);

        if (match != null)
        {
            for (int i = 0; i < cells.getItemCount(); ++i)
            {
                boolean blueNotRed;
                RowHeaderModel row = ((RowHeaderModel) rows.getItems().get(i));
                blueNotRed = match.blueTeams.contains(row.getData());

                int alliance = (blueNotRed) ? Constants.ALLIANCE_BLUE : Constants.ALLIANCE_RED;

                for(CellModel cell : ((List<CellModel>)cells.getItems().get(i)))
                    cell.SetAlliance(alliance);
                row.SetAlliance(alliance);
            }
        }

        cells.setItems(cells.getItems());
        rows.setItems(rows.getItems());
    }

    /**
     * Remove all of the alilance highlights
     */
    public void clearAllianceHighlights()
    {
        setAllianceHighlights(null);
    }
}
