package com.hotteam67.firebaseviewer;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    private static final String MATCHES_FILE = "viewerMatches.csv";
    private static final String TEAMS_FILE = "teamNames.json";
    private static final String RANKS_FILE = "teamRanks.json";
    private static final String AVERAGES_FILE = "averagesCache";
    private static final String MAXIMUMS_FILE = "maximumsCache";
    private static final String CACHE_FILE = "matchesCache";
    private static final String DIRECTORY =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/BluetoothScouter/";

    public static final int VIEWER_MATCHES = 4;
    public static final int TEAM_NAMES = 5;
    public static final int TEAM_RANKS = 6;
    public static final int AVERAGES_CACHE = 7;
    public static final int MAXIMUMS_CACHE = 8;
    public static final int RAW_CACHE = 9;


    private static String file(int FILE)
    {
        String f = DIRECTORY;
        switch (FILE)
        {
            case VIEWER_MATCHES:
                f += MATCHES_FILE;
                break;
            case TEAM_NAMES:
                f += TEAMS_FILE;
                break;
            case TEAM_RANKS:
                f += RANKS_FILE;
                break;
            case AVERAGES_CACHE:
                f += AVERAGES_FILE;
                break;
            case MAXIMUMS_CACHE:
                f += MAXIMUMS_FILE;
                break;
            case RAW_CACHE:
                f += CACHE_FILE;
                break;
            default:
                return null;
        }
        return f;
    }

    public static BufferedReader GetReader(int FILE)
    {
        try
        {
            return  new BufferedReader(new FileReader(file(FILE)));
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

    public static FileWriter GetWriter(int FILE)
    {
        String f = file(FILE);

        try
        {
            FileWriter writer = new FileWriter(new File(f).getAbsolutePath(), false);
            return writer;
        }
        catch (FileNotFoundException e)
        {
            l("File not found");
            new File(DIRECTORY).mkdirs();

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

    public static String LoadContents(int file)
    {
        String content = "";
        BufferedReader r = GetReader(file);
        try
        {
            String line = r.readLine();
            while (line != null)
            {
                content += line + "\n";
                line = r.readLine();
            }
        }
        catch (Exception e)
        {
            l("Failed to load :" + e.getMessage());
        }

        return content;
    }

    public static void Write(int FILE, String s)
    {
        try
        {
            FileWriter w = GetWriter(FILE);
            w.write(s);
            w.close();
        }
        catch (Exception e)
        {
            l("Failed to write: " + s + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void Serialize(Serializable o, int file)
    {
        try {
            if (file(file) == null)
                return;
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

    public static Serializable DeSerialize(int file)
    {
        try
        {
            if (file(file) == null)
                return null;
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
        Log.d("HotTeam67", s);
    }
}
