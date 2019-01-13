package org.hotteam67.firebaseviewer.tableview.holder;

import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.hotteam67.common.Constants;
import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;

/**
 * The CellViewHolder that is populated by CellModels. Uses the alliance to set its background color,
 * or otherwise handles selected/unselected colors
 */
public class CellViewHolder extends AbstractViewHolder {
    private final TextView cellTextView;
    private final LinearLayout cellContainer;
    private CellModel cell;

    /**
     * Constructor, takes the view that will be used.
     * @param itemView The view, this is inflated in mainTableAdapter
     */
    public CellViewHolder(View itemView) {
        super(itemView);
        cellTextView = itemView.findViewById(R.id.cell_data);
        cellContainer = itemView.findViewById(R.id.cell_container);
    }

    /**
     * Set the CellModel, updating the values, colors, and layout params
     * @param cellModel the cellModel to populate the view with
     */
    public void setCellModel(CellModel cellModel) {
        cell = cellModel;

        // Change textView align by column
        cellTextView.setGravity(Gravity.CENTER |
                Gravity.CENTER_VERTICAL);

        // Set text
        cellTextView.setText(cellModel.getData());

        setAlliance(cellModel.GetAlliance());

        // It is necessary to remeasure itself.
        cellContainer.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        cellTextView.requestLayout();
    }

    /**
     * Event handler for selection/un-selection, to change the colors of the view. Ignored if there
     * is an alliance set
     * @param selectionState either selected or unselected
     */
    @Override
    public void setSelected(SelectionState selectionState) {
        super.setSelected(selectionState);

        if (selectionState == SelectionState.SELECTED) {
            cellTextView.setTextColor(ContextCompat.getColor(cellTextView.getContext(), R.color
                    .selected_text_color));
            cellTextView.setBackgroundColor(ContextCompat.getColor(cellTextView.getContext(), R.color
                    .selected_background_color));
        } else {
            cellTextView.setTextColor(ContextCompat.getColor(cellTextView.getContext(), R.color
                    .unselected_text_color));
            cellTextView.setBackgroundColor(ContextCompat.getColor(cellTextView.getContext(), R.color
                    .unselected_background_color));
        }
        if (alliance == Constants.ALLIANCE_NONE) return;
        setAlliance(alliance);
    }

    private int alliance = Constants.ALLIANCE_NONE;

    /**'
     * Set the view highlight based on the alliance given
     * @param alliance the alliance code from Constants
     */
    private void setAlliance(int alliance)
    {
        this.alliance = alliance;
        int colorBack;
        int colorText;
        switch (alliance)
        {
            case Constants.ALLIANCE_BLUE:
                colorBack = R.color.alliance_blue_highlight;
                colorText = R.color.alliance_blue_text;
                break;
            case Constants.ALLIANCE_RED:
                colorBack = R.color.alliance_red_highlight;
                colorText = R.color.alliance_red_text;
                break;
            default:
                colorBack = R.color.unselected_header_background_color;
                colorText = R.color.unselected_text_color;
                break;
        }
        cellTextView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                colorBack));
        cellTextView.setTextColor(ContextCompat.getColor(itemView.getContext(),
                colorText));
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                colorBack));
    }

    /**
     * Get the CellModel that the view is currently populated with
     * @return CellModel assigned to the view last
     */
    public CellModel getCellModel() {
        return cell;
    }
}
