package org.hotteam67.firebaseviewer.tableview.tablemodel;

import com.evrencoskun.tableview.sort.ISortableModel;

import org.hotteam67.common.Constants;

import java.io.Serializable;

/**
 * Created by evrencoskun on 27.11.2017.
 */

public class RowHeaderModel implements Serializable, ISortableModel {
    private final String mData;

    public RowHeaderModel(String mData) {
        this.mData = mData;
    }

    public String getData() {
        return mData;
    }

    @Override
    public String getId() {
        return "0";
    }

    @Override
    public Object getContent() {
        try
        {
            return Integer.valueOf(mData);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return 0;
        }
    }

    private int alliance = Constants.ALLIANCE_NONE;
    public int GetAlliance() { return alliance; }
    public void SetAlliance(int alliance) { this.alliance = alliance; }
}
