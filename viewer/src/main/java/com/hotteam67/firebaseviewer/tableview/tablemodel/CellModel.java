package com.hotteam67.firebaseviewer.tableview.tablemodel;

import com.evrencoskun.tableview.sort.ISortableModel;

import java.io.Serializable;

/**
 * Created by evrencoskun on 27.11.2017.
 */

public class CellModel implements ISortableModel, Serializable {
    private final String mId;
    private final String mData;

    public CellModel(String pId, String mData) {
        this.mId = pId;
        this.mData = mData;
    }

    public String getData() {
        return mData;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public Object getContent() {
        try
        {
            return Double.valueOf(mData);
        }
        catch (Exception e)
        {
            switch (mData) {
                case "TRUE":
                    return 1.0;
                case "FALSE":
                    return 0.0;
                default:
                    return 0.0;
            }
        }
    }

}
