package org.hotteam67.common;

import android.os.AsyncTask;
import android.util.Log;

import org.hotteam67.common.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Handler for The Blue Alliance Web API
 */

public class TBAHandler {

    /**
     * A class that will retrieve the contents of the url and pass them back as a string with an
     * onCompleteEvent
     */
    static class RetrieveUrl extends AsyncTask<String, Void, String> {

        final OnDownloadResultListener<String> onCompleteEvent;

        /**
         * Constructor
         * @param event the event to be called once the URL has been retreieved
         */
        RetrieveUrl(OnDownloadResultListener<String> event)
        {
            onCompleteEvent = event;
        }

        /**
         * The thread to download from the url
         * @param input the url
         * @return the results of the download
         */
        @Override
        protected String doInBackground(String... input)
        {
            try
            {
                HttpURLConnection conn = (HttpURLConnection) new URL(input[0]).openConnection();
                conn.setRequestMethod("GET");

                Log.d("HotTeam67", "Response code: " + conn.getResponseCode());
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) { // 200

                    InputStream responseStream = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));

                    String line = reader.readLine();
                    StringBuilder response = new StringBuilder();
                    while (line != null)
                    {
                        response.append(line);
                        line = reader.readLine();
                    }

                    Log.d("HotTeam67", "Response: " + response.toString());

                    conn.disconnect();
                    return response.toString();
                }
                conn.disconnect();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String s)
        {
            onCompleteEvent.onComplete(s);
        }
    }

    /**
     * A model for a match containing both red and blue alliances
     */
    public static class Match
    {
        public List<String> redTeams = new ArrayList<>();
        public List<String> blueTeams = new ArrayList<>();
    }

    /**
     * Get the rankings of an event to return as a JSONObject
     * @param eventCode the blue alliance event code, including year
     * @param returnEvent event that consumes a JSONObject - [teamNumber, teamRank]
     */
    public static void Rankings(String eventCode, OnDownloadResultListener<JSONObject> returnEvent)
    {
        String url = Constants.TBA.BASE_URL;

        url += Constants.TBA.EVENT + eventCode + Constants.TBA.TEAMS + Constants.TBA.STATUSES + Constants.AUTH_TOKEN;
        Log.d("HotTeam67", "Pulling data from url: " + url);

        RetrieveUrl retrieveUrl = new RetrieveUrl(new OnDownloadResultListener<String>() {
            @Override
            public void onComplete(String result) {
                JSONObject resultObject;
                JSONObject returnObject = new JSONObject();
                try
                {
                    resultObject = new JSONObject(result);
                    Iterator<?> keys = resultObject.keys();

                    while (keys.hasNext())
                    {
                        String team = (String) keys.next();
                        try {
                            JSONObject statusObject = (JSONObject) resultObject.get(team);

                            if (statusObject == null) {
                                returnObject.put(team.replace("frc", ""), "");
                            } else {
                                JSONObject quals = (JSONObject) statusObject.get("qual");
                                JSONObject ranking = (JSONObject) quals.get("ranking");
                                String value = String.valueOf(ranking.get("rank"));
                                returnObject.put(team.replace("frc", ""), value);
                            }
                        }
                        catch (Exception e)
                        {
                            // Put no team name
                            returnObject.put(team.replace("frc", ""), "");
                        }
                    }

                    returnEvent.onComplete(returnObject);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail() {
                returnEvent.onFail();
            }
        }); retrieveUrl.execute(url);
    }

    /**
     * Get the qualification match schedule for an event
     * @param eventCode the blue alliance event code, including year
     * @param returnEvent consumes a list of matches with red and blue teams
     */
    public static void Matches(String eventCode, final OnDownloadResultListener<List<Match>> returnEvent)
    {
        String url = Constants.TBA.BASE_URL;

        url += Constants.TBA.EVENT + eventCode + Constants.TBA.MATCHES + Constants.AUTH_TOKEN;
        Log.d("HotTeam67", "Pulling data from url: " + url);

        RetrieveUrl retrieveUrl = new RetrieveUrl(new OnDownloadResultListener<String>() {
            @Override
            public void onComplete(String result) {

                try {
                    JSONArray resultArray = new JSONArray(result);
                    List<Match> matches = new ArrayList<>();

                    for (int i = 0; i < resultArray.length(); ++i) {
                        JSONObject matchObject = resultArray.getJSONObject(i);
                        Match match = new Match();

                        // Only qual schedule
                        if (!matchObject.get("comp_level").equals("qm")) continue;

                        JSONObject allianceObject = matchObject.getJSONObject("alliances");
                        JSONObject blue = allianceObject.getJSONObject("alliances").getJSONObject("blue");
                        JSONObject red = allianceObject.getJSONObject("alliances").getJSONObject("red");

                        JSONArray redTeamKeys = (JSONArray) red.get("team_keys");
                        for (int r = 0; r < redTeamKeys.length(); ++r)
                            match.redTeams.add(((String) redTeamKeys.get(r)).replace("frc", ""));

                        JSONArray blueTeamKeys = (JSONArray) blue.get("team_keys");
                        for (int b = 0; b < blueTeamKeys.length(); ++b)
                            match.blueTeams.add(((String) blueTeamKeys.get(b)).replace("frc", ""));

                        matches.add(match);
                    }
                    matches.removeAll(Collections.singleton(null));
                    returnEvent.onComplete(matches);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail() {

            }
        }); retrieveUrl.execute(url);
    }

    /**
     * Get the team names of each team for an event
     * @param eventCode the blue alliance event code, including year
     * @param returnEvent consumes team number as key team name as value
     */
    public static void TeamNames(String eventCode, OnDownloadResultListener<JSONObject> returnEvent)
    {
        String url = Constants.TBA.BASE_URL;

        url += Constants.TBA.EVENT + eventCode + Constants.TBA.TEAMS + Constants.AUTH_TOKEN;
        Log.d("HotTeam67", "Pulling data from url: " + url);

        RetrieveUrl retrieveUrl = new RetrieveUrl(new OnDownloadResultListener<String>() {
            @Override
            public void onComplete(String result) {
                JSONArray resultArray;
                JSONObject returnObject = new JSONObject();
                try {
                    resultArray = new JSONArray(result);

                    for (int i = 0; i < resultArray.length(); ++i) {
                        JSONObject team = resultArray.getJSONObject(i);
                        returnObject.put(String.valueOf(team.get("team_number")), team.get("nickname"));
                    }
                    returnEvent.onComplete(returnObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFail()
            {
                returnEvent.onFail();
            }
        }); retrieveUrl.execute(url);
    }

    /**
     * Get the alliances of teams for eliminations at an event
     * @param eventCode the blue alliance event code, including year
     * @param returnEvent list of list of string team numbers, from highest to lowest rank
     */
    public static void Alliances(String eventCode, OnDownloadResultListener<List<List<String>>> returnEvent)
    {
        String url = Constants.TBA.BASE_URL;

        url += Constants.TBA.EVENT + eventCode + Constants.TBA.ALLIANCES + Constants.AUTH_TOKEN;

        RetrieveUrl retrieveUrl = new RetrieveUrl(new OnDownloadResultListener<String>() {
            @Override
            public void onComplete(String result) {
                JSONArray resultArray;
                List<List<String>> alliances = new ArrayList<>();
                try {
                    resultArray = new JSONArray(result);
                    for (int i = 0; i < resultArray.length(); ++i) {
                        JSONObject alliance = resultArray.getJSONObject(i);
                        if (!alliance.has("picks"))
                            continue;

                        JSONArray picks = alliance.getJSONArray("picks");
                        List<String> currentAlliance = new ArrayList<>();
                        for (int t = 0; t < picks.length(); ++t) {
                            currentAlliance.add(picks.getString(t).replace("frc", ""));
                        }
                        alliances.add(currentAlliance);
                    }
                    returnEvent.onComplete(alliances);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail()
            {
                returnEvent.onFail();
            }
        }); retrieveUrl.execute(url);
    }
}
