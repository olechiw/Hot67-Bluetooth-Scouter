package org.hotteam67.common;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * Created by Jakob on 3/26/2017.
 */

public final class FileHandler
{
    public static final String SERVER_FILE = "Server/serverDatabase.json"; // Server scouted matches database
    public static final String SCHEMA_FILE = "schema.json"; // Server/scouter schema
    public static final String SCOUTER_FILE = "Scouter/scouterDatabase.json"; // Scouter scouted/unscouted matches database
    public static final String MATCHES_FILE = "Server/serverMatches.csv"; // Server unscouted matches (match schedule)
    public static final String TEAM_NAMES_FILE = "Viewer/teamNames.json";  // Viewer team name sjson
    public static final String RANKS_FILE = "Viewer/teamRanks.json"; // Viewer team ranks json
    public static final String CUSTOM_TEAMS_FILE = "Viewer/customTeams.csv"; // Viewer list of custom teams
    public static final String ALLIANCES_FILE = "Viewer/alliances.csv"; // Viewer alliances list
    public static final String VIEWER_MATCHES_FILE = "Viewer/viewerMatches.csv"; // Viewer match schedule
    public static final String AVERAGES_CACHE = "Viewer/averagesCache"; // Cache of viewer average calculations
    public static final String MAXIMUMS_CACHE = "Viewer/maximumsCache"; // Cache of viewer maximum calculations
    public static final String RAW_CACHE = "Viewer/matchesCache"; // Cache of raw data for viewer
    private static final String DIRECTORY =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/BluetoothScouter/";


    private static String file(String file)
    {
        return DIRECTORY + file;
    }

    public static BufferedReader GetReader(String FILE)
    {
        try
        {
            return new BufferedReader(new FileReader(file(FILE)));
        }
        catch (FileNotFoundException e)
        {
            l("File not found: " + file(FILE));
            new File(DIRECTORY).mkdirs();

            try
            {
                new File(file(FILE)).createNewFile();
                return new BufferedReader(new FileReader(file(FILE)));
            }
            catch (Exception ex)
            {
                return null;
            }
        }
        catch (Exception e)
        {
            l("Exception occured in loading reader : " + e.getMessage());
        }

        return null;
    }

    private static FileWriter GetWriter(String FILE)
    {
        String f = file(FILE);

        try
        {
            return new FileWriter(new File(f).getAbsolutePath(), false);
        }
        catch (FileNotFoundException e)
        {
            l("File not found");
            new File(DIRECTORY).mkdirs();
            new File(DIRECTORY + "Scouter").mkdirs();
            new File(DIRECTORY + "Server").mkdirs();
            new File(DIRECTORY + "Viewer").mkdirs();

            try
            {
                new File(f).createNewFile();
                return new FileWriter(new File(f).getAbsolutePath(), false);
            }
            catch (Exception ex)
            {
                return null;
            }
        }
        catch (Exception e)
        {
            l("Exception occured in loading reader : " + e.getMessage());
        }

        return null;
    }

    public static String LoadContents(String file)
    {
        StringBuilder content = new StringBuilder();
        BufferedReader r = GetReader(file);
        try
        {
            String line = r.readLine();
            while (line != null)
            {
                content.append(line).append("\n");
                line = r.readLine();
            }
        }
        catch (Exception e)
        {
            l("Failed to load :" + e.getMessage());
        }

        return content.toString();
    }

    public static void Write(String FILE, String s)
    {
        try
        {
            FileWriter w = GetWriter(FILE);
            if (w != null)
            {
                w.write(s);
                w.close();
            }
        }
        catch (Exception e)
        {
            l("Failed to write: " + s + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void Serialize(Serializable o, String file)
    {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file(file));
            ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
            outputStream.writeObject(o);
            outputStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Serializable DeSerialize(String file)
    {
        try
        {
            FileInputStream fileInputStream = new FileInputStream(file(file));
            ObjectInputStream inputStream = new ObjectInputStream(fileInputStream);
            Object returnObject = inputStream.readObject();

            inputStream.close();

            return (Serializable)returnObject;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static void l(String s)
    {
        Log.d("[File Handling]", s);
    }
}
