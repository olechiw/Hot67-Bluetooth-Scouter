package com.hotteam67.firebaseviewer.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Stream;
import com.hotteam67.firebaseviewer.TeamsGroupHandler;
import com.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import com.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;
import com.hotteam67.firebaseviewer.web.FirebaseHandler;
import com.hotteam67.firebaseviewer.web.TBAHandler;

import org.hotteam67.common.Constants;
import org.hotteam67.common.FileHandler;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
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

    private static ProgressEvent progressEvent;

    public static void Setup(String[] conn,
                             ProgressEvent progEvent)
    {
        outputAverages = averages;
        outputMaximums = maximums;

        connectionProperties = conn;
        progressEvent = progEvent;
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
    public static synchronized void SerializeTables()
    {
        @SuppressLint("StaticFieldLeak") AsyncTask serializeTask = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] objects)
            {
                FileHandler.Serialize(maximums, FileHandler.MAXIMUMS_CACHE);
                FileHandler.Serialize(averages, FileHandler.AVERAGES_CACHE);
                FileHandler.Serialize(rawData, FileHandler.RAW_CACHE);
                progressEvent.EndProgress();
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
            progressEvent.BeginProgress();
            @SuppressLint("StaticFieldLeak") AsyncTask task = new AsyncTask()
            {
                @Override
                protected Object doInBackground(Object[] objects)
                {
                    try
                    {
                        averages = (DataTable)FileHandler.DeSerialize(FileHandler.AVERAGES_CACHE);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    try
                    {
                        maximums = (DataTable)FileHandler.DeSerialize(FileHandler.MAXIMUMS_CACHE);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    try
                    {
                        rawData = (DataTable)FileHandler.DeSerialize(FileHandler.RAW_CACHE);
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
                    progressEvent.EndProgress();
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
    public static void RefreshTable(Context c, Constants.OnCompleteEvent onCompleteEvent)
    {
        progressEvent.BeginProgress();

        LoadTBAData(c);


        if (connectionProperties == null || connectionProperties.length != 4)
        {
            Log.d("HotTeam67", "Couldn't load connection string");
            progressEvent.EndProgress();
            return;
        }

        String databaseUrl = connectionProperties[0];
        String eventName = connectionProperties[1];
        String apiKey = connectionProperties[2];

        final FirebaseHandler model = new FirebaseHandler(
                databaseUrl, eventName, apiKey);

        // Null child to get all raw data
        model.Download(() -> {

            rawData = new DataTable(model.getResult(), ColumnSchema.CalculatedColumnsRawNames(), ColumnSchema.SumColumns());

            RunCalculations(onCompleteEvent);

            return null;
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
    }

    /*
    Re-run all calculations with the current raw data
     */
    private static void RunCalculations(Constants.OnCompleteEvent event)
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
                UpdateIfLoaded(event);

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
                UpdateIfLoaded(event);

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
    private static synchronized void UpdateIfLoaded(Constants.OnCompleteEvent event)
    {
        if (maximums != null && averages != null)
        {
            outputMaximums = maximums;
            outputAverages = averages;
            event.OnComplete();
            SerializeTables();
        }
    }

    /*
    Load TBA data from the API v3
     */
    public static synchronized void LoadTBAData(Context c)
    {
        if (connectionProperties == null || connectionProperties.length != 4)
            return;
        String eventKey = connectionProperties[3];

        try
        {
            StringBuilder s = new StringBuilder();

            // Call api and load into csv
            TBAHandler.Matches(eventKey, matches -> {
                try {
                    for (List<List<String>> m : matches) {
                        List<String> redTeams = m.get(0);
                        List<String> blueTeams = m.get(1);
                        for (String t : redTeams) {
                            s.append(t.replace("frc", Constants.EMPTY)).append(",");
                        }
                        for (int t = 0; t < blueTeams.size(); ++t) {
                            s.append(blueTeams.get(t).replace("frc", Constants.EMPTY));
                            if (t + 1 != blueTeams.size())
                                s.append(",");
                        }
                        s.append("\n");
                    }
                    FileHandler.Write(FileHandler.VIEWER_MATCHES_FILE, s.toString());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });

            // Load into json
            try {
                TBAHandler.TeamNames(eventKey, teamNames -> {
                    FileHandler.Write(FileHandler.TEAM_NAMES_FILE, teamNames.toString());
                    teamNumbersNames = teamNames;
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try {
                TBAHandler.Rankings(eventKey, rankings ->
                        {
                            FileHandler.Write(FileHandler.RANKS_FILE, rankings.toString());
                            teamNumbersRanks = rankings;
                        }
                );
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try {
                TBAHandler.Alliances(eventKey, a ->
                {
                    alliances = a;
                    StringBuilder alliancesString = new StringBuilder();
                    for (List<String> alliance : alliances)
                    {
                        alliancesString.append(TextUtils.join(",", alliance));
                        alliancesString.append("\n");
                    }
                    FileHandler.Write(FileHandler.ALLIANCES_FILE, alliancesString.toString());
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

        String content = FileHandler.LoadContents(FileHandler.VIEWER_MATCHES_FILE);
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
            teamNumbersNames = new JSONObject(FileHandler.LoadContents(FileHandler.TEAM_NAMES_FILE));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            teamNumbersRanks = new JSONObject(FileHandler.LoadContents(FileHandler.RANKS_FILE));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            alliances = new ArrayList<>();
            String[] alliancesFile = FileHandler.LoadContents(FileHandler.ALLIANCES_FILE)
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
    public static void ShowAlliance(Integer seatNumber)
    {
        try
        {
            if (seatNumber <= 0 || seatNumber > alliances.size())
            {
                SetTeamNumberFilter();
                return;
            }

            List<String> alliance = alliances.get(seatNumber - 1);
            outputMaximums = maximums;
            outputAverages = averages;
            SetTeamNumberFilter(Stream.of(alliance.toArray()).toArray(String[]::new));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    Show Custom teams
     */
    public static void ShowTeams(List<String> teams)
    {
        try
        {
            if (teams == null || teams.size() == 0)
            {
                SetTeamNumberFilter();
                return;
            }

            outputMaximums = maximums;
            outputAverages = averages;
            SetTeamNumberFilter(Stream.of(teams.toArray()).toArray(String[]::new));
        }
        catch (Exception e)
        {
            e.printStackTrace();
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

    /*
    When the match search text changes
     */
    public static synchronized void ShowMatch(Integer matchNumber)
    {
        try
        {
            if (matchNumber <= 0)
            {
                SetTeamNumberFilter(Constants.EMPTY);
                return;
            }
            outputMaximums = GetMatchForTable(matchNumber, maximums);
            outputAverages = GetMatchForTable(matchNumber, averages);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static synchronized DataTable GetMatchForTable(Integer matchNumber, DataTable table)
    {
        if (matchNumber <= redTeamsQuals.size() && matchNumber <= blueTeamsQuals.size())
        {
            List<String> red = new ArrayList<>(
                    Arrays.asList(redTeamsQuals.get(matchNumber - 1).split(",")));
            List<String> blue = new ArrayList<>(
                    Arrays.asList(blueTeamsQuals.get(matchNumber - 1).split(",")));

            List<String> filters = new ArrayList<>();
            filters.addAll(red);
            filters.addAll(blue);

            List<RowHeaderModel> rows = new ArrayList<>();
            List<List<CellModel>> cells = new ArrayList<>();
            List<ColumnHeaderModel> columns = new ArrayList<>(table.GetColumns());

            for (String team : filters)
            {
                SetTeamNumberFilter(team);
                rows.addAll(table.GetRowHeaders());

                for (List<CellModel> cell : table.GetCells())
                {
                    List<CellModel> newRow = new ArrayList<>(cell);
                    cells.add(newRow);
                }
            }

            DataTable processor = new DataTable(columns, cells, rows);
            processor.SetTeamNumberFilter(Constants.EMPTY);

            List<ColumnHeaderModel> columnHeaderModels = processor.GetColumns();
            columnHeaderModels.add(0, new ColumnHeaderModel(Constants.ALLIANCE));

            List<List<CellModel>> outputCells = processor.GetCells();
            for (int i = 0; i < outputCells.size(); ++i)
            {
                String teamNumber = processor.GetRowHeaders().get(i).getData();

                if (red.contains(teamNumber))
                {
                    outputCells.get(i).add(0, new CellModel(i + "_00", Constants.RED));
                }
                else {
                    outputCells.get(i).add(0, new CellModel(i + "_00", Constants.BLUE));
                    blue.remove(teamNumber);
                }
                red.remove(teamNumber);
                blue.remove(teamNumber);
            }

            List<RowHeaderModel> rowHeaders = processor.GetRowHeaders();

            int firstRowSize = 0;
            if (outputCells.size() > 0)
            {
                firstRowSize = outputCells.get(0).size() - 1; // -1 for alliance
            }
            for (String team : red)
            {
                List<CellModel> row = new ArrayList<>();
                row.add(new CellModel("0", Constants.RED));

                for (int i = 0; i < firstRowSize; ++i)
                {
                    row.add(new CellModel("0", Constants.N_A));
                }


                outputCells.add(row);
                rowHeaders.add(new RowHeaderModel(team));
            }
            for (String team : blue)
            {
                List<CellModel> row = new ArrayList<>();
                row.add(new CellModel("0", Constants.BLUE));

                for (int i = 0; i < firstRowSize; ++i)
                {
                    row.add(new CellModel("0", Constants.N_A));
                }

                outputCells.add(row);
                rowHeaders.add(new RowHeaderModel(team));
            }

            DataTable newProcessor = new DataTable(columnHeaderModels, outputCells, rowHeaders);

            //Sort by alliance
            return Sort.SortByColumn(newProcessor, 0, false);
        }
        else
        {
            return table;
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

    public interface ProgressEvent
    {
        void BeginProgress();
        void EndProgress();
    }
}