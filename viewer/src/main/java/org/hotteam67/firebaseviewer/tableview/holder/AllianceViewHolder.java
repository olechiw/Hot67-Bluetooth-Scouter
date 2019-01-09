package org.hotteam67.firebaseviewer.tableview.holder;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;

public class AllianceViewHolder extends AbstractViewHolder
{
    private final LinearLayout cell_container;
    private final ImageView cell_alliance;

    public AllianceViewHolder(View itemView) {
        super(itemView);
        cell_container = itemView.findViewById(R.id.cell_container);
        cell_alliance = itemView.findViewById(R.id.cell_alliance);
    }

    public void setCellModel(CellModel cellModel) {
        if (cellModel.isAlliance())
        {
            if (cellModel.getData().equals("BLUE"))
                cell_alliance.setImageResource(R.drawable.blue);
            else
                cell_alliance.setImageResource(R.drawable.red);
        }
        cell_alliance.requestLayout();
        // It is necessary to remeasure itself.
        cell_container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
    }

    @Override
    public void setSelected(SelectionState p_nSelectionState) {
        super.setSelected(p_nSelectionState);
        // selection doesn't change anything
    }
}
