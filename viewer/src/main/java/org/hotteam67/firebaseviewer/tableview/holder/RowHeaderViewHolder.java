package org.hotteam67.firebaseviewer.tableview.holder;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.hotteam67.common.Constants;
import org.hotteam67.firebaseviewer.R;

/**
 * RowHeaderViewHolder has a value for the label but also stores a temporary alliance for highlighting
 */
public class RowHeaderViewHolder extends AbstractViewHolder {
    public final TextView rowHeaderTextView;

    public RowHeaderViewHolder(View p_jItemView) {
        super(p_jItemView);
        rowHeaderTextView = p_jItemView.findViewById(R.id.row_header_textview);
    }

    /**
     * Selection event handler. Will be overridden if there is an alliance set
     * @param selectionState for determining colors, assuming there is no alliance
     */
    @Override
    public void setSelected(SelectionState selectionState) {
        super.setSelected(selectionState);

        int nBackgroundColorId;
        int nForegroundColorId;

        if (selectionState == SelectionState.SELECTED) {
            nForegroundColorId = R.color.selected_text_color;
            nBackgroundColorId = R.color.selected_background_color;
        } else if (selectionState == SelectionState.UNSELECTED) {
            nBackgroundColorId = R.color.unselected_background_color;
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

        if (alliance == Constants.ALLIANCE_NONE) return;
        setAlliance(alliance);
    }

    private int alliance = Constants.ALLIANCE_NONE;

    /**
     * Set the alliance, to be stored, and also the highlight colors. Also called when setSelected
     * is called to make sure colors are up to date
     * @param alliance the alliance from Constants to determine colors
     */
    public void setAlliance(int alliance)
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
        rowHeaderTextView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                colorBack));
        rowHeaderTextView.setTextColor(ContextCompat.getColor(itemView.getContext(),
                colorText));
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                colorBack));
    }
}
