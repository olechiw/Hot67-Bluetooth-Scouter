package com.hotteam67.firebaseviewer.tableview.holder;

import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.evrencoskun.tableview.ITableView;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractSorterViewHolder;
import com.evrencoskun.tableview.sort.SortState;
import com.hotteam67.firebaseviewer.R;
import com.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;

/**
 * Created by evrencoskun on 1.12.2017.
 */

public class ColumnHeaderViewHolder extends AbstractSorterViewHolder {
    private final LinearLayout column_header_container;
    private final TextView column_header_textview;

    public ColumnHeaderViewHolder(View itemView) {
        super(itemView);
        column_header_textview = itemView.findViewById(R.id.column_header_textView);
        column_header_container = itemView.findViewById(R.id.column_header_container);
    }

    public void setColumnHeaderModel(ColumnHeaderModel pColumnHeaderModel) {

        // Change alignment of textView
        column_header_textview.setGravity(Gravity.CENTER_HORIZONTAL | Gravity
                .CENTER_VERTICAL);

        // Set text data
        column_header_textview.setText(pColumnHeaderModel.getData());

        // It is necessary to remeasure itself.
        column_header_container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        column_header_textview.requestLayout();
    }

    @Override
    public void setSelected(SelectionState p_nSelectionState) {
        super.setSelected(p_nSelectionState);

        int nBackgroundColorId;
        int nForegroundColorId;

        if (p_nSelectionState == SelectionState.SELECTED) {
            nBackgroundColorId = R.color.selected_background_color;
            nForegroundColorId = R.color.selected_text_color;

        } else if (p_nSelectionState == SelectionState.UNSELECTED) {
            nBackgroundColorId = R.color.unselected_header_background_color;
            nForegroundColorId = R.color.unselected_text_color;

        } else { // SelectionState.SHADOWED

            nBackgroundColorId = R.color.shadow_background_color;
            nForegroundColorId = R.color.unselected_text_color;
        }

        column_header_container.setBackgroundColor(ContextCompat.getColor(column_header_container
                .getContext(), nBackgroundColorId));
        column_header_textview.setTextColor(ContextCompat.getColor(column_header_container
                .getContext(), nForegroundColorId));
    }

    @Override
    public void onSortingStatusChanged(SortState pSortState) {
        super.onSortingStatusChanged(pSortState);

        // It is necessary to remeasure itself.
        column_header_container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;

        column_header_textview.requestLayout();
        column_header_container.requestLayout();
        itemView.requestLayout();
    }


}
