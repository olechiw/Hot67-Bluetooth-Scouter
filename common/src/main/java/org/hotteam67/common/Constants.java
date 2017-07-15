package org.hotteam67.common;

import android.util.Log;

/**
 * Created by Jakob on 4/28/2017.
 */

public class Constants
{
    public static final int TAG_LENGTH = 8;

    public static final String MATCH_TAG=     ":SCOUT: ";
    public static final String MATCH_TEAM_TAG =     ":TEAM:  ";
    public static final String MATCH_NUMBER_TAG =   ":MATCH: ";
    public static final String SCHEMA_TAG =         ":SCHEMA:";

    public static final int PADDING =       0;
    public static final int TYPE_HEADER =   1;
    public static final int TYPE_BOOLEAN =  2;
    public static final int TYPE_INTEGER =  3;
    public static final int TYPE_STRING =   4;

    public static String getTag(String s)
    {
        try
        {
            return s.substring(0, 7);
        }
        catch (Exception e)
        {
            Log.d("[BluetoothScouter]", "Failed to get tag from string: " + s);
            e.printStackTrace();

            return s;
        }
    }
    public static String tagless(String s)
    {
        try
        {
            return s.substring(8, s.length());
        }
        catch (Exception e)
        {
            Log.d("[BluetoothScouter]", "Failed to strip tag from string: " + s);
            e.printStackTrace();

            return s;
        }
    }
}
