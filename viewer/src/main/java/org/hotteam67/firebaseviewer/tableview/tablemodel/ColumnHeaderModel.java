package org.hotteam67.firebaseviewer.tableview.tablemodel;

import java.io.Serializable;

/**
 * Simple ColumnHeaderModel just stores the names of the columns
 */
public class ColumnHeaderModel implements Serializable {

    private final String mData;

    /**
     * Constructor gets the name
     * @param mData the name/label of the column
     */
    public ColumnHeaderModel(String mData) {
        this.mData = mData;
    }

    /**
     * Get the name for populating the viewholder
     * @return string name/label of column
     */
    public String getData() {
        return mData;
    }
}
