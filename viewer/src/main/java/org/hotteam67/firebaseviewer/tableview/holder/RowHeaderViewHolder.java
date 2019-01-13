package org.hotteam67.firebaseviewer.tableview.holder;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.hotteam67.common.Constants;
import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

/**
 * RowHeaderViewHolder has a value for the label but also stores a temporary alliance for highlighting
 */
public class RowHeaderViewHolder extends AbstractViewHolder {
    public final TextView rowHeaderTextView;
    private int yPosition = -1;

    /**
     * Constructor just takse simple inflated view
     * @param itemView the view inflated from template
     */
    public RowHeaderViewHolder(View itemView) {
        super(itemView);
        rowHeaderTextView = itemView.findViewById(R.id.row_header_textview);
    }

    /**
     * Bind the values of a model to the view holder
     * @param row the row to get data from
     * @param yPosition the yPosition of the row
     */
    public void setRowHeaderModel(RowHeaderModel row, int yPosition)
    {
        this.yPosition = yPosition;
        try {
            rowHeaderTextView.setText(String.valueOf(row.getData()));
        } catch (Exception e) {
            rowHeaderTextView.setText("ERROR");
        }
        setAlliance(row.GetAlliance(), SelectionState.UNSELECTED);
    }


    /**
     * Selection event handler. Will be overridden if there is an alliance set
     * @param selectionState for determining colors, assuming there is no alliance
     */
    @Override
    public void setSelected(SelectionState selectionState) {
        super.setSelected(selectionState);

        if (alliance != Constants.ALLIANCE_NONE)
        {
            setAlliance(alliance, selectionState);
            return;
        }

        int nBackgroundColorId;
        int nForegroundColorId;

        if (selectionState == SelectionState.SELECTED) {
            nForegroundColorId = R.color.selected_text_color;
            nBackgroundColorId = R.color.selected_background_color;
        } else if (selectionState == SelectionState.UNSELECTED) {
            if (yPosition != -1 && yPosition % 2 == 0)
                nBackgroundColorId = R.color.unselected_background_color;
            else
                nBackgroundColorId = R.color.unselected_background_color_odd;
            nForegroundColorId = R.color.unselected_text_color;

        } else { // SelectionState.SHADOWED

            nBackgroundColorId = R.color.shadow_background_color;
            nForegroundColorId = R.color.unselected_text_color;
        }

        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                nBackgroundColorId));
        rowHeaderTextView.setTextColor(ContextCompat.getColor(rowHeaderTextView.getContext(),
                nForegroundColorId));
        rowHeaderTextView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                nBackgroundColorId));
    }

    private int alliance = Constants.ALLIANCE_NONE;

    /**
     * Set the alliance, to be stored, and also the highlight colors. Also called when setSelected
     * is called to make sure colors are up to date
     * @param alliance the alliance from Constants to determine colors
     */
    private void setAlliance(int alliance, SelectionState selectionState)
    {
        this.alliance = alliance;
        int colorBack;
        int colorText;
        switch (alliance)
        {
            case Constants.ALLIANCE_BLUE:
                colorBack = R.color.alliance_blue_highlight;
                if (selectionState == SelectionState.SELECTED) colorBack = R.color.alliance_blue_selected;
                colorText = R.color.alliance_blue_text;
                break;
            case Constants.ALLIANCE_RED:
                colorBack = R.color.alliance_red_highlight;
                if (selectionState == SelectionState.SELECTED) colorBack = R.color.alliance_red_selected;
                colorText = R.color.alliance_red_text;
                break;
            default:
                colorBack = R.color.unselected_header_background_color;
                colorText = R.color.unselected_text_color;
                break;
        }
        rowHeaderTextView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                colorBack));
        rowHeaderTextView.setTextColor(ContextCompat.getColor(itemView.getContext(),
                colorText));
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                colorBack));
    }
}
