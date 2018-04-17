package com.hotteam67.firebaseviewer.tableview.tablemodel;

import com.evrencoskun.tableview.sort.ISortableModel;

import java.io.Serializable;

/**
 * Created by evrencoskun on 27.11.2017.
 */

public class CellModel implements ISortableModel, Serializable {
    private String mId;
    private Object mData;

    public CellModel(String pId, Object mData) {
        this.mId = pId;
        this.mData = mData;
    }

    public Object getData() {
        return mData;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public Object getContent() {
        return mData;
    }

}
