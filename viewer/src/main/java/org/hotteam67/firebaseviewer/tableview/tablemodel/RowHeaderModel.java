package org.hotteam67.firebaseviewer.tableview.tablemodel;

import com.evrencoskun.tableview.sort.ISortableModel;

import org.hotteam67.common.Constants;

import java.io.Serializable;

/**
 * Model for RowHeaders, stores the value which is either a team or match number, and has an alliance
 * assigned for highlighting in the RowHeaderViewHolder
 */
public class RowHeaderModel implements Serializable, ISortableModel {
    private final String mData;

    /**
     * Constructor
     * @param mData the value/label of the row header
     */
    public RowHeaderModel(String mData) {
        this.mData = mData;
    }

    /**
     * Get the data in String format for display
     * @return String value
     */
    public String getData() {
        return mData;
    }

    /**
     * Get the unique id, ignored
     * @return "0"
     */
    @Override
    public String getId() {
        return "0";
    }

    /**
     * Get the data in number format
     * @return an Integer value of the rowHeader, or 0 if something failed
     */
    @Override
    public Object getContent() {
        try
        {
            return Integer.valueOf(mData);
        }
        catch (Exception e)
        {
            Constants.Log(e);
            return 0;
        }
    }

    private int alliance = Constants.ALLIANCE_NONE;

    /**
     * Get the temporarily assigned alliance for highlighting
     * @return Constants.ALLIANCE_NONE, Constants.ALLIANCE_BLUE, or Constants.ALLIANCE_RED
     */
    public int GetAlliance() { return alliance; }

    /**
     * Set the temporary alliance to be cleared later, for highlighting
     */
    public void SetAlliance(int alliance) { this.alliance = alliance; }
}
