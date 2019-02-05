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
 * Static class providing all of the functions and files
 */

public final class FileHandler {
    /**
     * List of file names used by various programs
     */
    public static class Files {

        /**
         * Master scouted matches database
         */
        public static final String MASTER_DATABASE = "Master/masterDatabase.json";

        /**
         * Master/scouter input schema
         */
        public static final String SCHEMA_FILE = "schema.json";

        /**
         * The scouter's scouted + unscouted matches (unscouted are just team numbers)
         */
        public static final String SCOUTER_FILE = "Scouter/scouterDatabase.json";

        /**
         * The match schedule downloaded on the master, all unscouted
         */
        public static final String MASTER_MATCHES_FILE = "Master/matches.csv";

        /**
         * The viewer's team names JSON file containing numbers keyed to the team nicknames from TBA
         */
        public static final String TEAM_NAMES_FILE = "Viewer/teamNames.json";

        /**
         * Viewer team ranks JSON file, team numbers keyed to team ranks in the event
         */
        public static final String RANKS_FILE = "Viewer/teamRanks.json";

        /**
         * Viewer alliances list with csv of 8 alliances and their teams
         */
        public static final String ALLIANCES_FILE = "Viewer/alliances.csv";

        /**
         * Viewer match schedule, in csv, from first match to last with red teams first
         */
        public static final String VIEWER_MATCHES_FILE = "Viewer/viewerMatches.csv";

        /**
         * The viewer's "cache" of averages - the serialized Averages DataTable written to disk
         */
        public static final String AVERAGES_CACHE = "Viewer/averagesCache";

        /**
         * The viewer's "cache" of maximums - the serialized Maximums DataTable written to disk
         */
        public static final String MAXIMUMS_CACHE = "Viewer/maximumsCache";

        /**
         * The viewer's "cache" of raw data - the serialized RawData DataTable written to disk
         */
        public static final String RAW_CACHE = "Viewer/matchesCache";
    }

    private static final String DIRECTORY =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/BluetoothScouter/";


    /**
     * Get the string value of a file with its directory
     *
     * @param file filename
     * @return file path
     */
    private static String file(String file) {
        return DIRECTORY + file;
    }

    /**
     * Get the read for a specific filed
     *
     * @param FILE the file without directory
     * @return the reader for the file
     */
    public static BufferedReader GetReader(String FILE) {
        try {
            return new BufferedReader(new FileReader(file(FILE)));
        } catch (FileNotFoundException e) {
            Constants.Log("File not found: " + file(FILE));
            new File(DIRECTORY).mkdirs();

            try {
                new File(file(FILE)).createNewFile();
                return new BufferedReader(new FileReader(file(FILE)));
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            Constants.Log(e);
        }

        return null;
    }

    /**
     * Get the writer for a file
     *
     * @param FILE the file without its directory
     * @return the writer for the file
     */
    private static FileWriter GetWriter(String FILE) {
        String f = file(FILE);
        try {
            File fileObj = new File(f);
            if (fileObj.isDirectory()) {
                fileObj.delete();
                fileObj.createNewFile();
            }
            else if (!fileObj.exists())
            {
                new File(DIRECTORY).mkdirs();
                fileObj.createNewFile();
            }
            return new FileWriter(fileObj.getAbsolutePath(), false);
        } catch (FileNotFoundException e) {
            Constants.Log("File not found");
            new File(DIRECTORY).mkdirs();

            try {
                new File(f).createNewFile();
                return new FileWriter(new File(f).getAbsolutePath(), false);
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            Constants.Log("Exception occured in loading reader : " + e.getMessage());
        }

        return null;
    }

    /**
     * Open and close a file and return its contents
     *
     * @param file the file to open, without directory
     * @return the contents of the file
     */
    public static String LoadContents(String file) {
        StringBuilder content = new StringBuilder();
        BufferedReader r = GetReader(file);
        if (r == null) return "";
        try {
            String line = r.readLine();
            while (line != null) {
                content.append(line).append("\n");
                line = r.readLine();
            }
        } catch (Exception e) {
            Constants.Log(e);
        }

        return content.toString();
    }

    /**
     * Write to a specific file overwriting its contents
     *
     * @param FILE the file to write to, without directory
     * @param s    the value to write to the file
     */
    public static void Write(String FILE, String s) {
        try {
            FileWriter w = GetWriter(FILE);
            if (w != null) {
                w.write(s);
                w.close();
            }
        } catch (Exception e) {
            Constants.Log("Failed to write: " + s + ": " + e.getMessage());
            Constants.Log(e);
        }
    }

    /**
     * Write a serializable object to a file
     *
     * @param o    the object to write
     * @param file the file to write to, without directory
     */
    public static void Serialize(Serializable o, String file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file(file));
            ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
            outputStream.writeObject(o);
            outputStream.close();
        } catch (Exception e) {
            Constants.Log(e);
        }
    }

    /**
     * Load an object from a file by de-serializing its contents
     *
     * @param file the file to load from, without directory
     * @return the serialized object
     */
    public static Serializable DeSerialize(String file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file(file));
            ObjectInputStream inputStream = new ObjectInputStream(fileInputStream);
            Object returnObject = inputStream.readObject();

            inputStream.close();

            return (Serializable) returnObject;
        } catch (Exception e) {
            Constants.Log(e);
            return null;
        }
    }
}
