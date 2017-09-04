package org.hotteam67.common;

import android.util.Log;

/**
 * Created by Jakob on 4/28/2017.
 */

public class Constants
{
    public static final int SCOUTER_TAG_LENGTH = 8;
    public static final String SCOUTER_TEAMS_TAG =      ":TEAM:  ";
    public static final String SCOUTER_SCHEMA_TAG =     ":SCHEMA:";

    public static String getScouterInputTag(String input)
    {
        return input.substring(0, SCOUTER_TAG_LENGTH);
    }

    public static String getScouterInputWithoutTag(String input)
    {
        return input.substring(SCOUTER_TAG_LENGTH, input.length());
    }

    public static final int PADDING =       0;
    public static final int TYPE_HEADER =   1;
    public static final int TYPE_BOOLEAN =  2;
    public static final int TYPE_INTEGER =  3;
    public static final int TYPE_STRING =   4;

    public static final String PREF_EMAIL = "pref_email";
    public static final String PREF_PASSWORD = "pref_password";
    public static final String PREF_EVENTNAME = "pref_eventName";

    public static String MATCH_NUMBER_JSON_TAG = "matchNumber";
    public static String TEAM_NUMBER_JSON_TAG = "teamNumber";

    public static final String DEFAULT_EVENT_NAME = "DefaultEventName";

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
