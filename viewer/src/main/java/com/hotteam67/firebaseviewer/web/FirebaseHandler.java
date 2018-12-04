package com.hotteam67.firebaseviewer.web;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;

import org.hotteam67.common.OnDownloadResultListener;
import org.json.JSONObject;


/**
 * Created by Jakob on 1/13/2018.
 */

public class FirebaseHandler {

    private final String firebaseEvent;
    private final String firebaseUrl;
    private final String firebaseApiKey;

    private OnDownloadResultListener<HashMap<String, Object>> firebaseCompleteEvent = null;

    private HashMap<String, Object> results = null;

    public FirebaseHandler(String url, String event, String apiKey)
    {
        firebaseUrl = url;
        firebaseEvent = event;
        firebaseApiKey = apiKey;
    }

    public void Download(OnDownloadResultListener<HashMap<String, Object>> completeEvent)
    {
        firebaseCompleteEvent = completeEvent;
        new RetrieveFirebaseTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    class RetrieveFirebaseTask extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... nothing)
        {
            try
            {
                String finalUrl = firebaseUrl + "/" + firebaseEvent + ".json" + "?auth=" + firebaseApiKey;
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


                    String resp = response.toString();
                    Log.d("HotTeam67", "Response: " + resp);

                    conn.disconnect();
                    return resp;
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


    // Format the input data into a table
    private void DoLoad(String json)
    {
        try
        {
            if (json == null || json.trim().isEmpty()) {
                firebaseCompleteEvent.onFail();
                return;
            }

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
            firebaseCompleteEvent.onFail();
            results = new HashMap<>();
            e.printStackTrace();
            return;
        }
        DoFinish();
    }


    private void DoFinish()
    {
        try {
            firebaseCompleteEvent.onComplete(getResult());
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
