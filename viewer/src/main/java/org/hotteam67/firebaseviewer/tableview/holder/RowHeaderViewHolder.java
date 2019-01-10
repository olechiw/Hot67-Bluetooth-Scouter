package org.hotteam67.firebaseviewer.tableview.holder;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import org.hotteam67.firebaseviewer.R;
import org.hotteam67.firebaseviewer.tableview.MainTableAdapter;

import java.lang.reflect.Field;

/**
 * Created by evrencoskun on 1.12.2017.
 */

public class RowHeaderViewHolder extends AbstractViewHolder {
    public final TextView row_header_textview;

    public RowHeaderViewHolder(View p_jItemView) {
        super(p_jItemView);
        row_header_textview = p_jItemView.findViewById(R.id.row_header_textview);
    }

    public static int getBackgroundColor(View view) {
        Drawable drawable = view.getBackground();
        if (drawable instanceof ColorDrawable) {
            ColorDrawable colorDrawable = (ColorDrawable) drawable;
            if (Build.VERSION.SDK_INT >= 11) {
                return colorDrawable.getColor();
            }
            try {
                Field field = colorDrawable.getClass().getDeclaredField("mState");
                field.setAccessible(true);
                Object object = field.get(colorDrawable);
                field = object.getClass().getDeclaredField("mUseColor");
                field.setAccessible(true);
                return field.getInt(object);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public void setSelected(SelectionState p_nSelectionState) {
        super.setSelected(p_nSelectionState);

        int color = getBackgroundColor(row_header_textview);
        if (color == Color.RED || color == Color.BLUE)
            return;

        int nBackgroundColorId;
        int nForegroundColorId;

        if (p_nSelectionState == SelectionState.SELECTED) {
            nForegroundColorId = R.color.selected_text_color;
            nBackgroundColorId = R.color.selected_background_color;
        } else if (p_nSelectionState == SelectionState.UNSELECTED) {
            nBackgroundColorId = R.color.unselected_background_color;
            nForegroundColorId = R.color.unselected_text_color;

        } else { // SelectionState.SHADOWED

            nBackgroundColorId = R.color.shadow_background_color;
            nForegroundColorId = R.color.unselected_text_color;
        }

        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                nBackgroundColorId));
        row_header_textview.setTextColor(ContextCompat.getColor(row_header_textview.getContext(),
                nForegroundColorId));
        row_header_textview.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                nBackgroundColorId));

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
                colorBack = R.color.alliance_blue_highlight;
                colorText = R.color.alliance_blue_text;
                break;
            case MainTableAdapter.ALLIANCE_RED:
                colorBack = R.color.alliance_red_highlight;
                colorText = R.color.alliance_red_text;
                break;
            default:
                colorBack = R.color.unselected_header_background_color;
                colorText = R.color.unselected_text_color;
                break;
        }
        row_header_textview.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                colorBack));
        row_header_textview.setTextColor(ContextCompat.getColor(itemView.getContext(),
                colorText));
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                colorBack));
    }
}
