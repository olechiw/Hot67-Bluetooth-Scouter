package org.hotteam67.common;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

/**
 * A class which holds most constants across all applications
 */
public class Constants
{
    public static final int TEAM_NUMBER_COLUMN = 0;

    /**
     * A stringinputevent for after the user has input a string. No consumer because compatibility
     */
    public interface StringInputEvent
    {
        void Run(String input);
    }

    public static final class ViewerTeamsGroupTypes {
        public static final String MATCH = "Qualification Match";
        public static final String ALLIANCE = "Alliance";
    }

    /**
     * Length of the tags that determine scouter input type
     */
    private static final int SCOUTER_TAG_LENGTH = 10;
    /**
     * Match schedule sent to scouter
     */
    public static final String SCOUTER_TEAMS_TAG =          ":TEAM:,,,,";
    /**
     * Schema sent to the scouter
     */
    public static final String SCOUTER_SCHEMA_TAG =         ":SCHEMA:,,";
    /**
     * No data, just that the server has received the last sent team from the scouter, for sync-all
     */
    public static final String SERVER_TEAMS_RECEIVED_TAG =  ":HEARD:,,,";
    /**
     * A message sent to the scouter with text info to be displayed to scouters
     */
    public static final String SERVER_MESSAGE_TAG =         ":MESSAGE,,";
    /**
     * A prompt from the server to the scouter to submit a specific match
     */
    public static final String SERVER_SUBMIT_TAG =          ":SUBMIT,,,";
    /**
     * A prompt from the server to the scouter to sync all of its matches
     */
    public static final String SERVER_SENDALL_TAG =         ":SYNCALL,,";
    /**
     * Activity request code for the rawDataActivity, which is used to get the results such as
     * displaying a specific match in the calculated table view
     */
    public static final int RawDataRequestCode = 1;
    /**
     * Activity request code for enabling permissions in the server/scouter app
     */
    public static final int REQUEST_ENABLE_PERMISSION = 3;
    /**
     * Authentication token for the blue alliance API
     */
    public static final String AUTH_TOKEN =
            "?X-TBA-Auth-Key=HisYRPfFZbTdm3uKUA6cZ2etWXymiIlM8X3XKq2T15TVZQDIc1vaWSr5rX17gHoh";
    /**
     * Activity request code for the preferences activity, to trigger updating of viewer constants
     */
    public static final int PreferencesRequestCode = 12;

    /**
     * Endpoints for the blue alliance API
     */
    public static class TBA
    {
        public static final String BASE_URL = "https://www.thebluealliance.com/api/v3";
        public static final String EVENT = "/event/";
        public static final String TEAMS = "/teams";
        public static final String MATCHES = "/matches/simple";
        public static final String STATUSES = "/statuses";
        public static final String ALLIANCES = "/alliances";
    }

    /**
     * Get the tag from a bluetooth input
     * @param input the original message
     * @return the value of the prefixed tag in the original message
     */
    public static String getScouterInputTag(String input)
    {
        return input.substring(0, SCOUTER_TAG_LENGTH);
    }

    /**
     * Get everything but the tag from bluetooth input
     * @param input the original message
     * @return the value of the message without its type tag
     */
    public static String getScouterInputWithoutTag(String input)
    {
        return input.substring(SCOUTER_TAG_LENGTH, input.length());
    }

    /**
     * The different types of input available in the schema
     */
    public static final class InputTypes
    {

        public static final int TYPE_HEADER =   1;
        public static final int TYPE_BOOLEAN =  2;
        public static final int TYPE_INTEGER =  3;
        public static final int TYPE_MULTI = 5;
        static final int TYPE_STRING =   4;
    }

    /**
     * Preference tag for the api key
     */
    public static final String PREF_APIKEY = "pref_apiKey";
    /**
     * Preference tag for the event name
     */
    public static final String PREF_EVENTNAME = "pref_eventName";
    /**
     * Preference tag for the firebase database url
     */
    public static final String PREF_DATABASEURL = "pref_databaseUrl";


    /**
     * The json tag for the match number
     */
    public static final String MATCH_NUMBER_JSON_TAG = "Match Number";
    /**
     * The json tag for the team number
     */
    public static final String TEAM_NUMBER_JSON_TAG = "Team Number";
    /**
     * The json tag for the notes
     */
    public static final String NOTES_JSON_TAG = "Notes";

    /**
     * Application UUID to look for during connection, may be configurable in future
     */
    public static final UUID uuid = UUID.fromString("1cb5d5ce-00f5-11e7-93ae-92361f002671");

    /**
     * Show a dialog and run an effect if the user chooses to confirm
     * @param message the message to prompt the user if they are sure
     * @param context the parent context
     * @param effect the event handler for what happens after confirmation
     */
    public static void OnConfirm(String message, Context context, final Runnable effect)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", (dialog, which) -> effect.run());
        builder.setNegativeButton("Cancel", (dialog, which) ->
        {
        }).create().show();
    }

    /**
     * Round a given value to a given precision
     * @param value value
     * @param precision precision (int)
     * @return The rounded value
     */
    public static double Round (double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }


    private static final HashMap<String, Long> times = new HashMap<>();

    /**
     * Time a value, either starting the timer or ending it and logging it
     * @param key the key for the timed value
     */
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

    /**
     * An oncomplete interface that consumes an input, used over java.function.Consumer for
     * backwards compatibility
     * @param <type> the type that will be consumed
     */
    public interface OnCompleteEvent<type>
    {
        void OnComplete(type arg);
    }
}
