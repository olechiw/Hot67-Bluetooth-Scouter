package org.hotteam67.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import java.util.UUID;

/**
 * Created by Jakob on 4/28/2017.
 */

public class Constants
{
    public static final int SCOUTER_TAG_LENGTH = 10;
    public static final String SCOUTER_TEAMS_TAG =          ":TEAM:,,,,";
    public static final String SCOUTER_SCHEMA_TAG =         ":SCHEMA:,,";
    public static final String SERVER_TEAMS_RECEIVED_TAG =  ":HEARD:,,,";

    public static String getScouterInputTag(String input)
    {
        return input.substring(0, SCOUTER_TAG_LENGTH - 1);
    }

    public static String getScouterInputWithoutTag(String input)
    {
        return input.substring(SCOUTER_TAG_LENGTH, input.length());
    }

    public static final int TYPE_HEADER =   1;
    public static final int TYPE_BOOLEAN =  2;
    public static final int TYPE_INTEGER =  3;
    public static final int TYPE_STRING =   4;

    public static final String PREF_APIKEY = "pref_apiKey";
    public static final String PREF_EVENTNAME = "pref_eventName";
    public static final String PREF_DATABASEURL = "pref_databaseUrl";


    public static String MATCH_NUMBER_JSON_TAG = "Match Number";
    public static String TEAM_NUMBER_JSON_TAG = "Team Number";

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

    // Application UUID to look for during connection, may be configurable in future
    public static final UUID uuid = UUID.fromString("1cb5d5ce-00f5-11e7-93ae-92361f002671");

    public static void OnConfirm(String message, Context context, final Runnable effect)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                effect.run();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).create().show();
    }
}
