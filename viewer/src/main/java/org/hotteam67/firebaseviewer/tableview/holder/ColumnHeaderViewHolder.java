package org.hotteam67.firebaseviewer.tableview.holder;

import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractSorterViewHolder;
import com.evrencoskun.tableview.sort.SortState;
import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;

/**
 * ColumnHeaderViewHolder, nothing very fancy just takes a model and populates it into simple layout
 */
public class ColumnHeaderViewHolder extends AbstractSorterViewHolder {
    private final LinearLayout columnHeaderContainer;
    private final TextView columnHeaderTextView;

    /**
     * Constructor takes the view to populate
     * @param itemView the view, inflated from a template in MainTableAdapter
     */
    public ColumnHeaderViewHolder(View itemView) {
        super(itemView);
        columnHeaderTextView = itemView.findViewById(R.id.column_header_textView);
        columnHeaderContainer = itemView.findViewById(R.id.column_header_container);
    }

    /**
     * Set the contents based on the value of a ColumnHeaderModel
     * @param model the model to get values from
     */
    public void setColumnHeaderModel(ColumnHeaderModel model) {

        // Change alignment of textView
        columnHeaderTextView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity
                .CENTER_VERTICAL);

        // Set text data
        columnHeaderTextView.setText(model.getData());

        // It is necessary to remeasure itself.
        columnHeaderContainer.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        columnHeaderTextView.requestLayout();
    }

    /**
     * Event handler for when the model is selected/un-selected
     * @param selectionState selected or unselected, for coloring
     */
    @Override
    public void setSelected(SelectionState selectionState) {
        super.setSelected(selectionState);

        int nBackgroundColorId;
        int nForegroundColorId;

        if (selectionState == SelectionState.SELECTED) {
            nBackgroundColorId = R.color.selected_background_color;
            nForegroundColorId = R.color.selected_text_color;

        } else if (selectionState == SelectionState.UNSELECTED) {
            nBackgroundColorId = R.color.unselected_header_background_color;
            nForegroundColorId = R.color.unselected_text_color;

        } else { // SelectionState.SHADOWED

            nBackgroundColorId = R.color.shadow_background_color;
            nForegroundColorId = R.color.unselected_text_color;
        }

        columnHeaderContainer.setBackgroundColor(ContextCompat.getColor(columnHeaderContainer
                .getContext(), nBackgroundColorId));
        columnHeaderTextView.setTextColor(ContextCompat.getColor(columnHeaderContainer
                .getContext(), nForegroundColorId));
    }

    /**
     * When sorting changes, just make sure to request layout again
     * @param sortState passed to super but ignored
     */
    @Override
    public void onSortingStatusChanged(SortState sortState) {
        super.onSortingStatusChanged(sortState);

        // It is necessary to remeasure itself.
        columnHeaderContainer.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;

        columnHeaderTextView.requestLayout();
        columnHeaderContainer.requestLayout();
        itemView.requestLayout();
    }


}
