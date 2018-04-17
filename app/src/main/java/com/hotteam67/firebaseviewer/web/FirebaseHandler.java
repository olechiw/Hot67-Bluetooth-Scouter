package com.hotteam67.firebaseviewer.web;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;

import org.json.JSONObject;


/**
 * Created by Jakob on 1/13/2018.
 */

public class FirebaseHandler {

    private String firebaseEvent;
    private String firebaseUrl;
    private String firebaseApiKey;

    private static final String LocalDatabase = "localDatabase.json";
    private static final String Directory =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/BluetoothScouter/";

    private Callable firebaseCompleteEvent = null;

    private HashMap<String, Object> results = null;

    public FirebaseHandler(String url, String event, String apiKey)
    {
        firebaseUrl = url;
        firebaseEvent = event;
        firebaseApiKey = apiKey;
    }

    public void Download(Callable completeEvent)
    {
        firebaseCompleteEvent = completeEvent;
        new RetreiveFirebaseTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    class RetreiveFirebaseTask extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... nothing)
        {
            try
            {
                String authToken = firebaseApiKey;
                String finalUrl = firebaseUrl + "/" + firebaseEvent + ".json" + "?auth=" + authToken;
                Log.d("HotTeam67", "URL: " + finalUrl);

                HttpURLConnection conn = (HttpURLConnection) new URL(finalUrl).openConnection();
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

                    // Save locally
                    File f = new File(Directory + LocalDatabase);
                    BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                    writer.write(response.toString());
                    writer.close();

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
        protected void onPostExecute(String result)
        {
            DoLoad(result);
        }
    }


    private void DoLoad(String json)
    {
        DoLoad(json, false);
    }
    private void DoLoad(String json, boolean skipFinish)
    {
        try
        {
            results = new HashMap<>();
            JSONObject jsonObject = new JSONObject(json);

            Iterator<?> iterator = jsonObject.keys();
            while (iterator.hasNext())
            {
                String key = (String) iterator.next();
                JSONObject row = (JSONObject) jsonObject.get(key);

                HashMap<String, String> rowMap = new HashMap<>();

                Iterator<?> rowIterator = row.keys();
                while (rowIterator.hasNext())
                {
                    String columnKey = (String) rowIterator.next();
                    rowMap.put(columnKey, row.get(columnKey).toString());
                }

                results.put(key, rowMap);
            }
        }
        catch (Exception e)
        {
            results = new HashMap<>();
            e.printStackTrace();
        }
        if (!skipFinish)
            DoFinish();
    }

    public void LoadLocal()
    {
        try {
            // Load from local database
            File f = new File(Directory + LocalDatabase);
            BufferedReader reader = new BufferedReader(new FileReader(f));
            StringBuilder contents = new StringBuilder();
            String s = reader.readLine();
            while (s != null)
            {
                contents.append(s);
                s = reader.readLine();
            }

            DoLoad(contents.toString(), true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void DoFinish()
    {
        try {
            firebaseCompleteEvent.call();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e("FirebaseScouter", "Failed to call completeEvent");
        }
    }

    public HashMap<String, Object> getResult()
    {
        return results;
    }
}
