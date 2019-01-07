package com.hotteam67.firebaseviewer.data;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;
import com.hotteam67.firebaseviewer.web.FirebaseHandler;

import org.hotteam67.common.FileHandler;
import org.hotteam67.common.OnDownloadResultListener;
import org.hotteam67.common.TBAHandler;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DataModel
{
    /*
    Connection properties
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

    private static Integer calculationState = DataTableBuilder.Calculation.AVERAGE;

    /*
    TBA-Loaded data
     */
    private static List<List<String>> alliances = new ArrayList<>();
    private static List<String> redTeamsQuals = new ArrayList<>();
    private static List<String> blueTeamsQuals = new ArrayList<>();
    private static JSONObject teamNumbersNames = new JSONObject();
    private static JSONObject teamNumbersRanks = new JSONObject();

    private static DataLoadEvent dataLoadEvent;

    public static void Setup(String[] conn,
                             DataLoadEvent progEvent)
    {
        outputAverages = averages;
        outputMaximums = maximums;

        connectionProperties = conn;
        dataLoadEvent = progEvent;
    }

    /*
    Switch the calculation type
     */
    public static void SwitchCalculation()
    {
        calculationState = calculationState == DataTableBuilder.Calculation.AVERAGE ?
                DataTableBuilder.Calculation.MAXIMUM : DataTableBuilder.Calculation.AVERAGE;
    }

    /*
    Get the active output table
     */
    public static synchronized DataTable GetTable()
    {
        return calculationState == DataTableBuilder.Calculation.AVERAGE ?
                outputAverages : outputMaximums;
    }

    public static synchronized DataTable GetAverages()
    {
        return outputAverages;
    }
    public static synchronized DataTable GetMaximums()
    {
        return outputMaximums;
    }

    /*
    Get the teamnumbersnames json
     */
    public static JSONObject GetTeamsNumbersNames()
    {
        return teamNumbersNames;
    }

    /*
    Get the raw data table
     */
    public static DataTable GetRawData()
    {
        return rawData;
    }

    /*
    Serialize the three datatables and write them to disk
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

    /*
    Load tables from disk into memory (raw, both calculated tables)
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

    /*
    Re-download all scouting data + TBA data, then refresh
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

        final FirebaseHandler model = new FirebaseHandler(
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

    /*
    Clear all filters, return to original UI
     */
    public static void ClearFilters()
    {
        SetTeamNumberFilter();
        outputMaximums = maximums;
        outputAverages = averages;
        Sort(0, true);
    }

    /*
    Re-run all calculations with the current raw data
     */
    private static void RunCalculations(Runnable onComplete)
    {
        // Runs
        @SuppressLint("StaticFieldLeak") AsyncTask averagesTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                DataTableBuilder avg = new DataTableBuilder(
                        rawData,
                        ColumnSchema.CalculatedColumns(),
                        ColumnSchema.CalculatedColumnsRawNames(),
                        ColumnSchema.OutlierAdjustedColumns(),
                        teamNumbersRanks,
                        teamNumbersNames,
                        DataTableBuilder.Calculation.AVERAGE);
                SetCalculatedDataAverages(avg.GetTable());
                UpdateIfLoaded(onComplete);

                return null;
            }
        };
        @SuppressLint("StaticFieldLeak") AsyncTask maximumsTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                DataTableBuilder max = new DataTableBuilder(
                        rawData,
                        ColumnSchema.CalculatedColumns(),
                        ColumnSchema.CalculatedColumnsRawNames(),
                        ColumnSchema.OutlierAdjustedColumns(),
                        teamNumbersRanks,
                        teamNumbersNames,
                        DataTableBuilder.Calculation.MAXIMUM);
                SetCalculatedDataMaximums(max.GetTable());
                UpdateIfLoaded(onComplete);

                return null;
            }
        };
        averagesTask.execute();
        maximumsTask.execute();
    }

    /*
    Synchronized updating functions
     */
    private static synchronized void SetCalculatedDataMaximums(DataTable max)
    {
        maximums = max;
    }
    private static synchronized void SetCalculatedDataAverages(DataTable avg)
    {
        averages = avg;
    }
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

    /*
    Load TBA data from the API v3
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

    /*
    Load TBA data from files
     */
    public static void LoadTBADataLocal()
    {
        redTeamsQuals = new ArrayList<>();
        blueTeamsQuals = new ArrayList<>();

        String content = FileHandler.LoadContents(FileHandler.Files.VIEWER_MATCHES_FILE);
        if (content == null || content.trim().isEmpty())
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

    /*
    Show a designated alliance's teams
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


    /*
    Sort by a target column
     */
    public static void Sort(int column, boolean ascending)
    {
        if (calculationState == DataTableBuilder.Calculation.AVERAGE)
        {
            outputAverages = Sort.SortByColumn(outputAverages, column, ascending);
            SynchronizeOrder(outputAverages, outputMaximums);
        }
        else
        {
            outputMaximums = Sort.SortByColumn(outputMaximums, column, ascending);
            SynchronizeOrder(outputMaximums, outputAverages);
        }
    }


    /*
    Synchronize the order of teams in maximums and averages
     */
    private static synchronized void SynchronizeOrder(DataTable source, DataTable target)
    {
        if (source == null || target == null)
            return;

        List<RowHeaderModel> sourceRows = source.GetRowHeaders();
        List<RowHeaderModel> targetRows = target.GetRowHeaders();

        if (targetRows.size() != sourceRows.size())
            return;

        List<List<CellModel>> targetCells = target.GetCells();
        for (RowHeaderModel row : sourceRows)
        {
            try
            {
                int index = -1;
                for (RowHeaderModel r : targetRows)
                {
                    if (r.getData().equals(row.getData()))
                        index = targetRows.indexOf(r);
                }
                if (index == -1)
                    continue;

                int newIndex = sourceRows.indexOf(row);
                List<CellModel> tmpCells = targetCells.get(newIndex);
                RowHeaderModel tmpRow = targetRows.get(newIndex);
                targetCells.set(newIndex, targetCells.get(index));
                targetRows.set(newIndex, targetRows.get(index));

                targetCells.set(index, tmpCells);
                targetRows.set(index, tmpRow);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }


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

    /*
    Set the team number filter on the active table
     */
    public static synchronized void SetTeamNumberFilter(String... s)
    {
        if (averages == null || maximums == null)
            return;

        maximums.SetTeamNumberFilter(s);
        averages.SetTeamNumberFilter(s);
        averages.SetTeamNumberFilter(s);
    }

    public interface DataLoadEvent
    {
        void OnBeginProgress();
        void OnCompleteProgress();
    }
}