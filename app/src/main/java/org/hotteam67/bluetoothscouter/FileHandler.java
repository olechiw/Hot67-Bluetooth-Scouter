package org.hotteam67.bluetoothscouter;

import android.os.Environment;

import java.io.*;
import java.util.*;
import android.util.Log;


/**
 * Created by Jakob on 3/26/2017.
 */

public final class FileHandler
{
    private static final String SERVER_FILE = "database.csv";
    private static final String SCHEMA_FILE = "schema.csv";
    private static final String SCOUTER_FILE = "localdatabase.csv";
    private static final String MATCHES_FILE = "matches.csv";
    private static final String DIRECTORY =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/BluetoothScouter/";


    public static final int SERVER = 1;
    public static final int SCHEMA = 2;
    public static final int SCOUTER = 3;
    public static final int MATCHES = 4;


    private static String file(int FILE)
    {
        String f = DIRECTORY;
        switch (FILE)
        {
            case SERVER:
                f += SERVER_FILE;
                break;
            case SCHEMA:
                f += SCHEMA_FILE;
                break;
            case SCOUTER:
                f += SCOUTER_FILE;
                break;
            case MATCHES:
                f += MATCHES_FILE;
                break;
            default:
                return null;
        }
        return f;
    }

    public static final BufferedReader GetReader(int FILE)
    {
        String f = file(FILE);

        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            return reader;
        }
        catch (FileNotFoundException e)
        {
            l("File not found");
            new File(DIRECTORY).mkdirs();

            try
            {
                new File(f).createNewFile();
                return new BufferedReader(new FileReader(f));
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

    public static final FileWriter GetWriter(int FILE)
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

    public static final String LoadContents(int file)
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
            e.printStackTrace();
        }

        return content;
    }

    public static final void Write(int FILE, String s)
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

    private static final void l(String s)
    {
        Log.d("[File Handling]", s);
    }
}
