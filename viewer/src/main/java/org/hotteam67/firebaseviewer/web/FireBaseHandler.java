package org.hotteam67.firebaseviewer.web;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;
import org.hotteam67.common.Constants;
import org.hotteam67.common.OnDownloadResultListener;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Class for getting data from FireBase on a separate network thread, and informing the main thread
 * when its done
 */

public class FireBaseHandler
{

    private final String fireBaseEvent;
    private final String fireBaseUrl;
    private final String fireBaseApiKey;

    private OnDownloadResultListener<HashMap<String, Object>> fireBaseCompleteEvent = null;

    private HashMap<String, Object> results = null;

    /**
     * Constructor takes the parameters for the FireBase database
     *
     * @param url    the url of the FireBase master to use as a starting point
     * @param event  the event name, represents a json endpoint where everything is put/retrieved
     * @param apiKey the api key to use with the database
     */
    public FireBaseHandler(String url, String event, String apiKey)
    {
        fireBaseUrl = url;
        fireBaseEvent = event;
        fireBaseApiKey = apiKey;
    }

    /**
     * Download the entire event for the given connection
     *
     * @param completeEvent a HashMap<HashMap<string, string>> is the final format. This is easier
     *                      to turn into a standard table
     */
    public void Download(OnDownloadResultListener<HashMap<String, Object>> completeEvent)
    {
        fireBaseCompleteEvent = completeEvent;
        new RetrieveFireBaseTask().execute();
    }

    /**
     * Asynchronous task to download the data from FireBase, notifies firebaseCompleteEvent when it
     * is done
     */
    @SuppressLint("StaticFieldLeak")
    class RetrieveFireBaseTask extends AsyncTask<Void, Void, String>
    {
        protected String doInBackground(Void... nothing)
        {
            try
            {
                String finalUrl = fireBaseUrl + "/" + fireBaseEvent + ".json" + "?auth=" + fireBaseApiKey;
                Log.d("HotTeam67", "URL: " + finalUrl);

                HttpURLConnection conn = (HttpURLConnection) new URL(finalUrl).openConnection();
                conn.setRequestMethod("GET");

                Log.d("HotTeam67", "Response code: " + conn.getResponseCode());
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                { // 200

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
                Constants.Log(e);
            }
            return "";
        }

        protected void onPostExecute(String result)
        {
            DoLoad(result);
        }
    }


    /**
     * Turn the downloaded JSON object into a HashMap in memory
     *
     * @param json the input json from FireBase
     */
    private void DoLoad(String json)
    {
        try
        {
            if (json == null || json.trim().isEmpty())
            {
                fireBaseCompleteEvent.onFail();
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
            fireBaseCompleteEvent.onFail();
            results = new HashMap<>();
            Constants.Log(e);
            return;
        }
        DoFinish();
    }


    /**
     * Try to do the OnComplete event, handle exception
     */
    private void DoFinish()
    {
        try
        {
            fireBaseCompleteEvent.onComplete(getResult());
        }
        catch (Exception e)
        {
            Constants.Log(e);
            Log.e("FirebaseScouter", "Failed to call completeEvent");
        }
    }

    /**
     * Get the result of the FireBase download
     *
     * @return hashhmap of hashmaps of strings, basically the json but in memory as an easier to
     * iterate over object
     */
    public HashMap<String, Object> getResult()
    {
        return results;
    }
}
