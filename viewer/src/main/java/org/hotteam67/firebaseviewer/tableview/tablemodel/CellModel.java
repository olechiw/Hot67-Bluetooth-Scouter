package org.hotteam67.firebaseviewer.tableview.tablemodel;

import com.evrencoskun.tableview.filter.IFilterableModel;
import com.evrencoskun.tableview.sort.ISortableModel;
import org.hotteam67.common.Constants;

import java.io.Serializable;

/**
 * The data Model for a cell's contents, including the temporary alliance tag and the teamNumber
 * the data is associated with, if there is one
 */
public class CellModel implements ISortableModel, IFilterableModel, Serializable
{
    private final String mId;
    private final String mData;
    private final String teamNumber;

    /**
     * Constructor, requires contents, team number, and id
     *
     * @param pId        the id is basically ignored, technically does duplicates tiebreaker for sorting
     * @param mData      the data to populate the cell with
     * @param teamNumber the teamNumber the cell belongs to
     */
    public CellModel(String pId, String mData, String teamNumber)
    {
        this.mId = pId;
        this.mData = mData;
        this.teamNumber = teamNumber;
    }

    /**
     * Get the team number associated with the cell
     *
     * @return String team number
     */
    public String getTeamNumber()
    {
        return teamNumber;
    }

    /**
     * Get the data for the model, formatted as a readable String
     *
     * @return the value of the data as a String
     */
    public String getData()
    {
        return mData;
    }

    /**
     * Get the id for duplicates
     *
     * @return the id
     */
    @Override
    public String getId()
    {
        return mId;
    }

    /**
     * Get the content for sorting the CellModel
     *
     * @return the content as a Double of some form
     */
    @Override
    public Object getContent()
    {
        try
        {
            return Double.valueOf(mData);
        }
        catch (Exception e)
        {
            switch (mData)
            {
                case "TRUE":
                    return 1.0;
                case "FALSE":
                    return 0.0;
                default:
                    return 0.0;
            }
        }
    }

    /**
     * Get the filtering keyword for the CellModel
     *
     * @return currently just uses the team number
     */
    @Override
    public String getFilterableKeyword()
    {
        return getTeamNumber();
    }

    private int alliance = Constants.ALLIANCE_NONE;

    /**
     * Set the temporary alliance for highlighting during qualification match filtering
     *
     * @return the Alliance, either ALLIANCE_NONE, ALLIANCE_RED, or ALLIANCE_BLUE
     */
    public int GetAlliance()
    {
        return alliance;
    }

    /**
     * Set the temporary alliance for highlighting
     *
     * @param alliance the alliance code
     */
    public void SetAlliance(int alliance)
    {
        this.alliance = alliance;
    }
}
