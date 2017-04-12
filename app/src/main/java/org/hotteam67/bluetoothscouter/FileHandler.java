package org.hotteam67.bluetoothscouter;

import android.os.Environment;


/**
 * Created by Jakob on 3/26/2017.
 */

public final class FileHandler
{
    public static final String SERVER_FILE = "database.csv";
    public static final String SCOUTER_FILE = "schema.csv";
    public static final String DIRECTORY =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/BluetoothScouter/";



    public static final int SERVER = 1;
    public static final int SCOUTER = 2;


    public static final String LoadContents(int FILE)
    {
        switch (FILE)
        {
            case SERVER:
                return LoadContents(SERVER_FILE + DIRECTORY);
            case SCOUTER:
                return LoadContents(SCOUTER_FILE + DIRECTORY);
            default:
                return "";
        }
    }

    private static final String LoadContents(String file)
    {
        return "";
    }
}
