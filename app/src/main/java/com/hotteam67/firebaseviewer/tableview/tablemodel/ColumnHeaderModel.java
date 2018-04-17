package com.hotteam67.firebaseviewer.tableview.tablemodel;

import java.io.Serializable;

/**
 * Created by evrencoskun on 27.11.2017.
 */

public class ColumnHeaderModel implements Serializable {

    private String mData;

    public ColumnHeaderModel(String mData) {
        this.mData = mData;
    }

    public String getData() {
        return mData;
    }
}
