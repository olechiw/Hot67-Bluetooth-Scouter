package org.hotteam67.common;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Jakob on 4/28/2017.
 */

public class Constants
{
    private static final int SCOUTER_TAG_LENGTH = 10;
    public static final String SCOUTER_TEAMS_TAG =          ":TEAM:,,,,";
    public static final String SCOUTER_SCHEMA_TAG =         ":SCHEMA:,,";
    public static final String SERVER_TEAMS_RECEIVED_TAG =  ":HEARD:,,,";
    public static final String SERVER_MESSAGE_TAG =         ":MESSAGE,,";
    public static final String SERVER_SUBMIT_TAG =          ":SUBMIT,,,";
    public static final String SERVER_SYNCALL_TAG =         ":SYNCALL,,";
    public static final String EMPTY = "";
    public static final String RED = "RED";
    public static final String N_A = "N/A";
    public static final String BLUE = "BLUE";
    public static final String ALLIANCE = "A";
    public static final int RawDataRequestCode = 1;
    public static final int REQUEST_ENABLE_PERMISSION = 3;
    public static final String AUTH_TOKEN =
            "?X-TBA-Auth-Key=HisYRPfFZbTdm3uKUA6cZ2etWXymiIlM8X3XKq2T15TVZQDIc1vaWSr5rX17gHoh";
    public static final int PreferencesRequestCode = 12;

    public static class TBA
    {
        public static final String BASE_URL = "https://www.thebluealliance.com/api/v3";
        public static final String EVENT = "/event/";
        public static final String TEAMS = "/teams";
        public static final String MATCHES = "/matches/simple";
        public static final String STATUSES = "/statuses";
        public static final String ALLIANCES = "/alliances";
    }

    public static String getScouterInputTag(String input)
    {
        return input.substring(0, SCOUTER_TAG_LENGTH);
    }

    public static String getScouterInputWithoutTag(String input)
    {
        return input.substring(SCOUTER_TAG_LENGTH, input.length());
    }

    public static final int TYPE_HEADER =   1;
    public static final int TYPE_BOOLEAN =  2;
    public static final int TYPE_INTEGER =  3;
    static final int TYPE_STRING =   4;
    public static final int TYPE_MULTI = 5;

    public static final String PREF_APIKEY = "pref_apiKey";
    public static final String PREF_EVENTNAME = "pref_eventName";
    public static final String PREF_DATABASEURL = "pref_databaseUrl";


    public static final String MATCH_NUMBER_JSON_TAG = "Match Number";
    public static final String TEAM_NUMBER_JSON_TAG = "Team Number";
    public static final String NOTES_JSON_TAG = "Notes";

    // Application UUID to look for during connection, may be configurable in future
    public static final UUID uuid = UUID.fromString("1cb5d5ce-00f5-11e7-93ae-92361f002671");

    public static void OnConfirm(String message, Context context, final Runnable effect)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", (dialog, which) -> effect.run());
        builder.setNegativeButton("Cancel", (dialog, which) ->
        {
        }).create().show();
    }

    public static double Round (double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    private static final HashMap<String, Long> times = new HashMap<>();
    public static void Time(String key)
    {
        if (times.containsKey(key))
        {
            Log.d("HotTeam67-TIME", "Time for " + key + ": " + times.get(key) + " ms");
            times.remove(key);
        }
        else
        {
            times.put(key, System.nanoTime());
        }
    }

    public interface OnCompleteEvent<type>
    {
        void OnComplete(type arg);
    }
}
