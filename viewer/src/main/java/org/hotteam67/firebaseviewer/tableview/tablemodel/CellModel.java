package org.hotteam67.firebaseviewer.tableview.tablemodel;

import com.evrencoskun.tableview.filter.IFilterableModel;
import com.evrencoskun.tableview.sort.ISortableModel;

import org.hotteam67.common.Constants;

import java.io.Serializable;

/**
 * Created by evrencoskun on 27.11.2017.
 */

public class CellModel implements ISortableModel, IFilterableModel, Serializable {
    private final String mId;
    private final String mData;
    private final String teamNumber;

    public CellModel(String pId, String mData, String teamNumber) {
        this.mId = pId;
        this.mData = mData;
        this.teamNumber = teamNumber;
    }

    public String getTeamNumber() { return teamNumber; }

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

    @Override
    public String getFilterableKeyword() {
        return getTeamNumber();
    }

    private int alliance = Constants.ALLIANCE_NONE;
    public int GetAlliance() { return alliance; }
    public void SetAlliance(int alliance) { this.alliance = alliance; }
}
