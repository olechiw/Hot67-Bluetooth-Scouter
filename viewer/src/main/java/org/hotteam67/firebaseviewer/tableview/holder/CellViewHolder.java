package org.hotteam67.firebaseviewer.tableview.holder;

import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.tableview.MainTableAdapter;
import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;

/**
 * Created by evrencoskun on 1.12.2017.
 */

public class CellViewHolder extends AbstractViewHolder {
    private final TextView cell_textview;
    private final LinearLayout cell_container;
    CellModel cell;

    public CellViewHolder(View itemView) {
        super(itemView);
        cell_textview = itemView.findViewById(R.id.cell_data);
        cell_container = itemView.findViewById(R.id.cell_container);
    }

    public void setCellModel(CellModel cellModel) {
        cell = cellModel;

        // Change textView align by column
        cell_textview.setGravity(Gravity.CENTER |
                Gravity.CENTER_VERTICAL);

        // Set text
        cell_textview.setText(cellModel.getData());

        setAlliance(cellModel.GetAlliance());

        // It is necessary to remeasure itself.
        cell_container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        cell_textview.requestLayout();
    }

    @Override
    public void setSelected(SelectionState p_nSelectionState) {
        super.setSelected(p_nSelectionState);

        if (p_nSelectionState == SelectionState.SELECTED) {
            cell_textview.setTextColor(ContextCompat.getColor(cell_textview.getContext(), R.color
                    .selected_text_color));
            cell_textview.setBackgroundColor(ContextCompat.getColor(cell_textview.getContext(), R.color
                    .selected_background_color));
        } else {
            cell_textview.setTextColor(ContextCompat.getColor(cell_textview.getContext(), R.color
                    .unselected_text_color));
            cell_textview.setBackgroundColor(ContextCompat.getColor(cell_textview.getContext(), R.color
                    .unselected_background_color));
        }
        if (alliance == MainTableAdapter.ALLIANCE_NONE) return;
        setAlliance(alliance);
    }

    private int alliance = MainTableAdapter.ALLIANCE_NONE;
    public void setAlliance(int alliance)
    {
        this.alliance = alliance;
        int colorBack;
        int colorText;
        switch (alliance)
        {
            case MainTableAdapter.ALLIANCE_BLUE:
                colorBack = Color.BLUE;
                colorText = Color.YELLOW;
                break;
            case MainTableAdapter.ALLIANCE_RED:
                colorBack = Color.RED;
                colorText = Color.GREEN;
                break;
            default:
                colorBack = Color.WHITE;
                colorText = Color.BLACK;
                break;
        }
        cell_textview.setBackgroundColor(colorBack);
        cell_textview.setTextColor(colorText);

        itemView.setBackgroundColor(colorBack);
    }

    public CellModel getCellModel() {
        return cell;
    }
}
