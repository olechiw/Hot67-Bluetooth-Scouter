package org.hotteam67.common;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jakob on 4/1/2018.
 */

public class TBAHandler {
    private static final String AUTH_TOKEN =
            "?X-TBA-Auth-Key=HisYRPfFZbTdm3uKUA6cZ2etWXymiIlM8X3XKq2T15TVZQDIc1vaWSr5rX17gHoh";

    private static final String BASE_URL = "https://www.thebluealliance.com/api/v3";
    private static final String EVENT = "/event/";
    private static final String TEAMS = "/teams";
    private static final String MATCHES = "/matches/simple";

    public interface OnCompleteEvent<t>{
        void run(t result);
    }
    static class RetreiveUrl extends AsyncTask<String, Void, String> {

        final OnCompleteEvent<String> onCompleteEvent;

        RetreiveUrl(OnCompleteEvent<String> event)
        {
            onCompleteEvent = event;
        }

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
            onCompleteEvent.run(s);
        }
    }

    // Returns lists of 3 red teams, and 3 blue teams, red first: [[r1, r2, r3], [b1, b2, b3]]
    public static void Matches(String eventCode, OnCompleteEvent<List<List<List<String>>>> returnEvent)
    {
        String url = BASE_URL;

        url += EVENT + eventCode + MATCHES + AUTH_TOKEN;
        Log.d("HotTeam67", "Pulling data from url: " + url);

        RetreiveUrl retreiveUrl = new RetreiveUrl(result -> {
            JSONArray resultArray;
            List<List<List<String>>> matches = new ArrayList<>();
            try
            {
                resultArray = new JSONArray(result);
                for (int i = 0; i < resultArray.length(); ++i)
                {
                    matches.add(null);
                }
                for (int i = 0; i < resultArray.length(); ++i)
                {
                    JSONObject matchObject = resultArray.getJSONObject(i);
                    if (!matchObject.get("comp_level").equals("qm")) continue;
                    JSONObject allianceObject = (JSONObject) matchObject.get("alliances");
                    JSONObject blue = (JSONObject) allianceObject.get("blue");
                    JSONObject red = (JSONObject) allianceObject.get("red");
                    List<List<String>> alliances = new ArrayList<>();

                    JSONArray redTeamKeys = (JSONArray) red.get("team_keys");
                    ArrayList<String> redTeams = new ArrayList<>();
                    redTeams.add(((String)redTeamKeys.get(0)).replace("frc", ""));
                    redTeams.add(((String)redTeamKeys.get(1)).replace("frc", ""));
                    redTeams.add(((String)redTeamKeys.get(2)).replace("frc", ""));

                    JSONArray blueTeamKeys = (JSONArray) blue.get("team_keys");
                    ArrayList<String> blueTeams = new ArrayList<>();
                    blueTeams.add(((String)blueTeamKeys.get(0)).replace("frc", ""));
                    blueTeams.add(((String)blueTeamKeys.get(1)).replace("frc", ""));
                    blueTeams.add(((String)blueTeamKeys.get(2)).replace("frc", ""));

                    alliances.add(redTeams);
                    alliances.add(blueTeams);

                    int matchNumber = (int) matchObject.get("match_number");
                    matches.set(matchNumber - 1, alliances);
                }
                matches.removeAll(Collections.singleton(null));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            returnEvent.run(matches);
        }); retreiveUrl.execute(url);
    }

}
