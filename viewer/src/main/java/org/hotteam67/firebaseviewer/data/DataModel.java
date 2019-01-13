package org.hotteam67.firebaseviewer.data;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.hotteam67.firebaseviewer.web.FireBaseHandler;

import org.hotteam67.common.FileHandler;
import org.hotteam67.common.OnDownloadResultListener;
import org.hotteam67.common.TBAHandler;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * The entirely static datamodel class that holds all of the data in memory, and runs calculations
 */
public class DataModel
{
    /**
     * List of connection properties in order of xml
     */
    private static String[] connectionProperties;

    /*
    Firebase-loaded data
     */
    private static  DataTable maximums;
    private static DataTable averages;
    private static DataTable rawData;

    private static DataTable outputMaximums;
    private static DataTable outputAverages;

    /**
     * TBA loaded data - alliances from 1 to 8 as list of list of team numbers(string)
     */
    private static List<List<String>> alliances = new ArrayList<>();
    /**
     * Red teams match schedule
     */
    private static List<String> redTeamsQuals = new ArrayList<>();
    /**
     * Blue teams match schedule
     */
    private static List<String> blueTeamsQuals = new ArrayList<>();
    /**
     * JSONObject for team number names and ranks, number is key, name is value
     */
    private static JSONObject teamNumbersNames = new JSONObject();
    /**
     * JSONObject for team ranks, number is key, name is value
     */
    private static JSONObject teamNumbersRanks = new JSONObject();

    /**
     * Event handler for data loading, once a specific download finishes. Can either fail or
     * complete
     */
    private static DataLoadEvent dataLoadEvent;

    /**
     * Setup all of the downloaded data given connection properties, and trigger the progEvent based
     * on completion or failure
     * @param conn the connection properties, order of occurrence in XML
     * @param progEvent the event to trigger on completion/failure
     */
    public static void Setup(String[] conn,
                             DataLoadEvent progEvent)
    {
        outputAverages = averages;
        outputMaximums = maximums;

        connectionProperties = conn;
        dataLoadEvent = progEvent;
    }

    /**
     * Get the table for calculated averages
     * @return the DataTable in memory for already calculated averages
     */
    public static synchronized DataTable GetAverages()
    {
        return outputAverages;
    }

    /**
     * Get the table for calcultaed maximums
     * @return the DataTable in memory for already calculated maximums
     */
    public static synchronized DataTable GetMaximums()
    {
        return outputMaximums;
    }

    /**
     * Get the teamNumberNames JSON object
     * @return JSON object with the keys as team numbers and values as team names
     */
    public static JSONObject GetTeamsNumbersNames()
    {
        return teamNumbersNames;
    }

    /**
     * Get the raw data table, with rows of matches and values containing individual match performance
     * @return DataTable with one row for each match for each team, so ((# matches) * 6)
     */
    public static DataTable GetRawData()
    {
        return rawData;
    }

    /**
     * Serialize the three data tables for easy storage, writing them to disk
     */
    private static synchronized void SerializeTables()
    {
        @SuppressLint("StaticFieldLeak") AsyncTask serializeTask = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] objects)
            {
                FileHandler.Serialize(maximums, FileHandler.Files.MAXIMUMS_CACHE);
                FileHandler.Serialize(averages, FileHandler.Files.AVERAGES_CACHE);
                FileHandler.Serialize(rawData, FileHandler.Files.RAW_CACHE);
                dataLoadEvent.OnCompleteProgress();
                return null;
            }
        };
        serializeTask.execute();

    }

    /**
     * Load tables from disk, and deserialize them into DataTable objects
     */
    public static void LoadSerializedTables()
    {
        try
        {
            dataLoadEvent.OnBeginProgress();
            @SuppressLint("StaticFieldLeak") AsyncTask task = new AsyncTask()
            {
                @Override
                protected Object doInBackground(Object[] objects)
                {
                    try
                    {
                        averages = (DataTable)FileHandler.DeSerialize(FileHandler.Files.AVERAGES_CACHE);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    try
                    {
                        maximums = (DataTable)FileHandler.DeSerialize(FileHandler.Files.MAXIMUMS_CACHE);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    try
                    {
                        rawData = (DataTable)FileHandler.DeSerialize(FileHandler.Files.RAW_CACHE);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Object o)
                {
                    super.onPostExecute(o);
                    outputAverages = averages;
                    outputMaximums = maximums;
                    dataLoadEvent.OnCompleteProgress();
                }
            };
            task.execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Download all of the firebase/tba data based on connection properties, and save it to database
     * @param onCompleteEvent event to run when the tables are populated
     */
    public static void RefreshTable(Runnable onCompleteEvent)
    {
        dataLoadEvent.OnBeginProgress();

        LoadTBAData();


        if (connectionProperties == null || connectionProperties.length != 4)
        {
            Log.d("HotTeam67", "Couldn't load connection string");
            dataLoadEvent.OnCompleteProgress();
            return;
        }

        String databaseUrl = connectionProperties[0];
        String eventName = connectionProperties[1];
        String apiKey = connectionProperties[2];

        final FireBaseHandler model = new FireBaseHandler(
                databaseUrl, eventName, apiKey);

        // Null child to get all raw data
        model.Download(new OnDownloadResultListener<HashMap<String, Object>>() {
            @Override
            public void onComplete(HashMap<String, Object> stringObjectHashMap) {
                rawData = new DataTable(model.getResult(), ColumnSchema.CalculatedColumnsRawNames(), ColumnSchema.SumColumns());

                RunCalculations(onCompleteEvent);
            }

            @Override
            public void onFail() {
                dataLoadEvent.OnCompleteProgress();
            }
        });
    }

    /**
     * Re-run calculations with the currently loaded raw data
     * @param onComplete event to run when the calculations are complete, as they are done in a
     *                   seperate thread
     */
    private static void RunCalculations(Runnable onComplete)
    {
        // Runs
        @SuppressLint("StaticFieldLeak") AsyncTask averagesTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                DataCalculator avg = new DataCalculator(
                        rawData,
                        ColumnSchema.CalculatedColumns(),
                        ColumnSchema.CalculatedColumnsRawNames(),
                        teamNumbersRanks,
                        teamNumbersNames,
                        DataCalculator.Calculation.AVERAGE);
                SetCalculatedDataAverages(avg.GetTable());
                UpdateIfLoaded(onComplete);

                return null;
            }
        };
        @SuppressLint("StaticFieldLeak") AsyncTask maximumsTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                DataCalculator max = new DataCalculator(
                        rawData,
                        ColumnSchema.CalculatedColumns(),
                        ColumnSchema.CalculatedColumnsRawNames(),
                        teamNumbersRanks,
                        teamNumbersNames,
                        DataCalculator.Calculation.MAXIMUM);
                SetCalculatedDataMaximums(max.GetTable());
                UpdateIfLoaded(onComplete);

                return null;
            }
        };
        averagesTask.execute();
        maximumsTask.execute();
    }

    /**
     * Set the maximums table manually
     * @param max the maximums table to replace the current one
     */
    private static synchronized void SetCalculatedDataMaximums(DataTable max)
    {
        maximums = max;
    }

    /**
     * Set the averages table manually
     * @param avg the averages table to replace the current one
     */
    private static synchronized void SetCalculatedDataAverages(DataTable avg)
    {
        averages = avg;
    }

    /**
     * Update the table if the thread is done and it actually exists
     * @param event event to run if the data is actually loaded, and not null
     */
    private static synchronized void UpdateIfLoaded(Runnable event)
    {
        if (maximums != null && averages != null)
        {
            outputMaximums = maximums;
            outputAverages = averages;
            event.run();
            SerializeTables();
        }
    }

    /**
     * Use the TBAHandler to download and format all of the data, loading it into memory
     */
    private static synchronized void LoadTBAData()
    {
        if (connectionProperties == null || connectionProperties.length != 4)
            return;
        String eventKey = connectionProperties[3];

        try
        {
            StringBuilder s = new StringBuilder();

            // Call api and load into csv
            TBAHandler.Matches(eventKey, new OnDownloadResultListener<List<TBAHandler.Match>>() {
                @Override
                public void onComplete(List<TBAHandler.Match> matches) {
                    try {
                        for (TBAHandler.Match m : matches) {
                            List<String> redTeams = m.redTeams;
                            List<String> blueTeams = m.blueTeams;
                            for (String t : redTeams) {
                                s.append(t).append(",");
                            }
                            for (int t = 0; t < blueTeams.size(); ++t) {
                                s.append(blueTeams.get(t));
                                if (t + 1 != blueTeams.size())
                                    s.append(",");
                            }
                            s.append("\n");
                        }
                        FileHandler.Write(FileHandler.Files.VIEWER_MATCHES_FILE, s.toString());
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFail() {
                }

            });

            // Load into json
            try {
                TBAHandler.TeamNames(eventKey, new OnDownloadResultListener<JSONObject>() {
                            @Override
                            public void onComplete(JSONObject teamNames) {
                                FileHandler.Write(FileHandler.Files.TEAM_NAMES_FILE, teamNames.toString());
                                teamNumbersNames = teamNames;
                            }

                            @Override
                            public void onFail() {
                            }
                        });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try {
                TBAHandler.Rankings(eventKey, new OnDownloadResultListener<JSONObject>() {
                            @Override
                            public void onComplete(JSONObject rankings) {
                                FileHandler.Write(FileHandler.Files.RANKS_FILE, rankings.toString());
                                teamNumbersRanks = rankings;
                            }

                            @Override
                            public void onFail() {
                            }
                        }
                );
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try {
                TBAHandler.Alliances(eventKey, new OnDownloadResultListener<List<List<String>>>() {
                    @Override
                    public void onComplete(List<List<String>> a) {
                        alliances = a;
                        StringBuilder alliancesString = new StringBuilder();
                        for (List<String> alliance : alliances)
                        {
                            alliancesString.append(TextUtils.join(",", alliance));
                            alliancesString.append("\n");
                        }
                        FileHandler.Write(FileHandler.Files.ALLIANCES_FILE, alliancesString.toString());
                    }

                    @Override
                    public void onFail() {
                    }

                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            LoadTBADataLocal();
        }
        catch (Exception e)
        {
            Log.e("HotTeam67", "Failed to get event: " + e.getMessage(), e);
        }
    }

    /**
     * Load all of the TBA data from disk into memory, if it is saved locally already
     */
    public static void LoadTBADataLocal()
    {
        redTeamsQuals = new ArrayList<>();
        blueTeamsQuals = new ArrayList<>();

        String content = FileHandler.LoadContents(FileHandler.Files.VIEWER_MATCHES_FILE);
        if (content.trim().isEmpty())
            return;
        String[] contents = content.split("\n");

        for (String match : contents)
        {
            List<String> teams = Arrays.asList(match.split(","));
            // red teams first
            try
            {
                StringBuilder red = new StringBuilder();
                for (int i = 0; i < 3; ++i)
                {
                    red.append(teams.get(i));
                    if (i + 1 != 3)
                        red.append(",");
                }
                redTeamsQuals.add(red.toString());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                StringBuilder blue = new StringBuilder();
                for (int i = 3; i < 6; ++i)
                {
                    blue.append(teams.get(i));
                    if (i + 1 != 6)
                        blue.append(",");
                }
                blueTeamsQuals.add(blue.toString());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        try {
            teamNumbersNames = new JSONObject(FileHandler.LoadContents(FileHandler.Files.TEAM_NAMES_FILE));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            teamNumbersRanks = new JSONObject(FileHandler.LoadContents(FileHandler.Files.RANKS_FILE));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            alliances = new ArrayList<>();
            String[] alliancesFile = FileHandler.LoadContents(FileHandler.Files.ALLIANCES_FILE)
                    .split("\n");
            for (String value : alliancesFile)
            {
                if (value.trim().isEmpty())
                    continue;
                String[] teams = value.split(",");
                if (teams.length == 1)
                    continue;
                alliances.add(new ArrayList<>(Arrays.asList(teams)));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Get the teams for a given alliance number
     * @param seatNumber the alliance seat to get teams for
     * @return list of team names
     */
    public static List<String> GetAlliance(Integer seatNumber)
    {
        try
        {
            return alliances.get(seatNumber);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get the match, with both alliances, for a given match number
     * @param matchNumber the match number to get teams for
     * @return the TBAHandler.Match object, populated or potentially null if something failed
     */
    public static synchronized TBAHandler.Match GetMatch(Integer matchNumber)
    {
        if (matchNumber <= redTeamsQuals.size() && matchNumber <= blueTeamsQuals.size())
        {
            TBAHandler.Match m = new TBAHandler.Match();
            m.redTeams = new ArrayList<>(
                    Arrays.asList(redTeamsQuals.get(matchNumber - 1).split(",")));
            m.blueTeams = new ArrayList<>(
                    Arrays.asList(blueTeamsQuals.get(matchNumber - 1).split(",")));

            return m;
        }
        else
        {
            return null;
        }
    }


    /**
     * Event for data load that has a begin/complete progress for triggering UI elements from the
     * DataModel
     */
    public interface DataLoadEvent
    {
        void OnBeginProgress();
        void OnCompleteProgress();
    }
}