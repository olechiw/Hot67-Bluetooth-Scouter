package org.hotteam67.firebaseviewer.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * The viewer schema for columns, adds a preferred order for tags found in the JSON pulled from
 * Firebase
 */

public class ColumnSchema {
    /**
     * The calculated columns desired names, in the same order as the raw names
     */
    public static List<CalculatedColumn> CalculatedColumns() {
        List<CalculatedColumn> calculatedColumns = new ArrayList<>();

        calculatedColumns.add(new CalculatedColumn("To. Cargo", "Total Cargo"));
        calculatedColumns.add(new CalculatedColumn("To. Hatches", "Total Hatch Panels"));
        calculatedColumns.add(new CalculatedColumn("Tel. Cargo", "Teleop Cargo"));
        calculatedColumns.add(new CalculatedColumn("Tel. Hatches", "Teleop Hatch Panels"));
        calculatedColumns.add(new CalculatedColumn("High Cargo", "Total Cargo High"));
        calculatedColumns.add(new CalculatedColumn("High Hatches", "Total Hatch Panels High"));
        calculatedColumns.add(new CalculatedColumn("A. Cargo", "Auton Cargo"));
        calculatedColumns.add(new CalculatedColumn("A. Hatches", "Auton Hatch Panels"));
        calculatedColumns.add(new CalculatedColumn("End HAB", "Endgame HAB Level"));
        calculatedColumns.add(new CalculatedColumn("A. Crossed", "Sandstorm Crossed The Line"));

        /*
        calculatedColumns.add(new CalculatedColumn("A. Cargo", "Auton Cargo"));
        calculatedColumns.add(new CalculatedColumn("A. Hatches", "Auton Hatches"));
        */

        return calculatedColumns;
    }

    public static List<String> PreferredOrder()
    {
        return new ArrayList<>(Arrays.asList(
                "Total Cargo", "Total Hatch Panels", "Teleop Cargo", "Teleop Hatch Panels", "Total Cargo High",
                "Total Hatch Panels High", "Auton Cargo", "Auton Hatch Panels",

                "Sandstorm Cargo Cargo Ship", "Sandstorm Cargo Left Rocket Low", "Sandstorm Cargo Left Rocket Middle",
                "Sandstorm Cargo Left Rocket High", "Sandstorm Cargo Right Rocket Low", "Sandstorm Cargo Right Rocket Middle",
                "Sandstorm Cargo Right Rocket High",

                "Sandstorm Hatch Panels Cargo Ship",
                "Sandstorm Hatch Panels Left Rocket Low", "Sandstorm Hatch Panels Left Rocket Middle", "Sandstorm Hatch Panels Left Rocket High",
                "Sandstorm Hatch Panels Right Rocket Low", "Sandstorm Hatch Panels Right Rocket Middle", "Sandstorm Hatch Panels Right Rocket High",

                "Teleop Cargo Cargo Ship",
                "Teleop Cargo Left Rocket Low", "Teleop Cargo Left Rocket Middle", "Teleop Cargo Left Rocket High",
                "Teleop Cargo Right Rocket Low", "Teleop Cargo Right Rocket Middle", "Teleop Cargo Right Rocket High",

                "Teleop Hatch Panels Cargo Ship",
                "Teleop Hatch Panels Left Rocket Low", "Teleop Hatch Panels Left Rocket Middle", "Teleop Hatch Panels Left Rocket High",
                "Teleop Hatch Panels Right Rocket Low", "Teleop Hatch Panels Right Rocket Middle", "Teleop Hatch Panels Right Rocket High"));
    }

    /**
     * List of columns to sum, with a special class, before doing maximum/average calcs
     *
     * @return List of sumcolumns
     */
    public static List<SumColumn> SumColumns() {

        SumColumn autonCargo = BuildSumColumn("Auton Cargo", "Sandstorm Cargo Cargo Ship",
                "Sandstorm Cargo Left Rocket Low", "Sandstorm Cargo Left Rocket Middle", "Sandstorm Cargo Left Rocket High",
                "Sandstorm Cargo Right Rocket Low", "Sandstorm Cargo Right Rocket Middle", "Sandstorm Cargo Right Rocket High");
        SumColumn teleopCargo = BuildSumColumn("Teleop Cargo", "Teleop Cargo Cargo Ship",
                "Teleop Cargo Left Rocket Low", "Teleop Cargo Left Rocket Middle", "Teleop Cargo Left Rocket High",
                "Teleop Cargo Right Rocket Low", "Teleop Cargo Right Rocket Middle", "Teleop Cargo Right Rocket High");
        SumColumn totalHighCargo = BuildSumColumn("Total Cargo High",
                "Teleop Cargo Left Rocket High", "Teleop Cargo Right Rocket High",
                "Auton Cargo Left Rocket High", "Auton Cargo Right Rocket High");
        SumColumn totalCargo = new SumColumn();
        totalCargo.columnName = "Total Cargo";
        totalCargo.columnsNames = new ArrayList<>();
        totalCargo.columnsNames.addAll(teleopCargo.columnsNames);
        totalCargo.columnsNames.addAll(autonCargo.columnsNames);


        SumColumn autonHatches = BuildSumColumn("Auton Hatch Panels", "Sandstorm Hatch Panels Cargo Ship",
                "Sandstorm Hatch Panels Left Rocket Low", "Sandstorm Hatch Panels Left Rocket Middle", "Sandstorm Hatch Panels Left Rocket High",
                "Sandstorm Hatch Panels Right Rocket Low", "Sandstorm Hatch Panels Right Rocket Middle", "Sandstorm Hatch Panels Right Rocket High");
        SumColumn teleopHatches = BuildSumColumn("Teleop Hatch Panels", "Teleop Hatch Panels Cargo Ship",
                "Teleop Hatch Panels Left Rocket Low", "Teleop Hatch Panels Left Rocket Middle", "Teleop Hatch Panels Left Rocket High",
                "Teleop Hatch Panels Right Rocket Low", "Teleop Hatch Panels Right Rocket Middle", "Teleop Hatch Panels Right Rocket High");
        SumColumn totalHighHatches = BuildSumColumn("Total Hatch Panels High",
                "Teleop Hatch Panels Left Rocket High", "Teleop Hatch Panels Right Rocket High",
                "Auton Hatch Panels Left Rocket High", "Auton Hatch Panels Right Rocket High");
        SumColumn totalHatches = new SumColumn();
        totalHatches.columnName = "Total Hatch Panels";
        totalHatches.columnsNames = new ArrayList<>();
        totalHatches.columnsNames.addAll(teleopHatches.columnsNames);
        totalHatches.columnsNames.addAll(autonHatches.columnsNames);

        ArrayList<SumColumn> sumColumns = new ArrayList<>();
        // No auton hatches rn
        addAll(sumColumns, totalCargo, totalHatches, teleopCargo, teleopHatches, autonCargo, autonHatches, totalHighHatches, totalHighCargo);

        return sumColumns;
    }

    private static void addAll(List<SumColumn> addTo, SumColumn... values) {
        addTo.addAll(Arrays.asList(values));
    }

    private static SumColumn BuildSumColumn(String name, String... columns) {
        SumColumn column = new SumColumn();
        column.columnName = name;
        column.columnsNames = new ArrayList<>(Arrays.asList(columns));
        return column;
    }

    public static class CalculatedColumn {
        public final String RawName;
        public final String Name;

        public CalculatedColumn(String name, String rawName) {
            RawName = rawName;
            Name = name;
        }
    }

    /**
     * Sum column, can be serialized for easier loading and saving of the table to/from memory
     */
    public static class SumColumn implements Serializable {
        public List<String> columnsNames;
        public String columnName;
    }
}
