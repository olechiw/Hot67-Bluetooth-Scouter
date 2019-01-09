package org.hotteam67.firebaseviewer.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The viewer schema for columns, adds a preferred order for tags found in the JSON pulled from
 * Firebase
 */

public class ColumnSchema {
    /**
     * The calculated columns desired names, in the same order as the raw names
     */
    public static List<String> CalculatedColumns()
    {
        List<String> calculatedColumns = new ArrayList<>();

        calculatedColumns.add("Cubes");
        calculatedColumns.add("T. Scale");
        calculatedColumns.add("T. Switch");
        calculatedColumns.add("O. Switch");
        calculatedColumns.add("T. Vault");
        calculatedColumns.add("A. Crossed");
        calculatedColumns.add("A. Scale");
        calculatedColumns.add("A. Switch");
        calculatedColumns.add("Dropped");
        calculatedColumns.add("Climbed");
        calculatedColumns.add("Assisted");
        calculatedColumns.add("A. Vault");

        return  calculatedColumns;
    }

    /**
     * The calculated columns raw names, in the same order as the calculated names
     * @return a list of the raw names corresponding in index to the calculated
     */
    public static List<String> CalculatedColumnsRawNames()
    {
        List<String> calculatedColumnsIndices = new ArrayList<>();

        calculatedColumnsIndices.add("Total Cubes");
        calculatedColumnsIndices.add("Teleop Scale");
        calculatedColumnsIndices.add("Teleop Switch");
        calculatedColumnsIndices.add("Opponent Switch");
        calculatedColumnsIndices.add("Teleop Vault");
        calculatedColumnsIndices.add("Crossed Line");
        calculatedColumnsIndices.add("Auton Scale");
        calculatedColumnsIndices.add("Auton Switch");
        calculatedColumnsIndices.add("Cubes Dropped");
        calculatedColumnsIndices.add("Climbed");
        calculatedColumnsIndices.add("Assisted");
        calculatedColumnsIndices.add("Auton Vault");

        return calculatedColumnsIndices;
    }

    /**
     * List of columns to sum, with a special class, before doing maximum/average calcs
     * @return List of sumcolumns
     */
    public static List<SumColumn> SumColumns()
    {
        SumColumn column = new SumColumn();
        column.columnName = "Total Cubes";
        column.columnsNames = new ArrayList<>();
        column.columnsNames.add("Auton Scale");
        column.columnsNames.add("Teleop Scale");
        column.columnsNames.add("Auton Vault");
        column.columnsNames.add("Teleop Vault");
        column.columnsNames.add("Auton Switch");
        column.columnsNames.add("Teleop Switch");
        column.columnsNames.add("Opponent Switch");

        ArrayList<SumColumn> sumColumns = new ArrayList<>();
        sumColumns.add(column);

        return sumColumns;
    }

    /**
     * Sum column, can be serialized for easier loading and saving of the table to/from memory
     */
    public static class SumColumn implements Serializable
    {
        public List<String> columnsNames;
        public String columnName;
    }

    /**
     * Outlier adjusted column, also serializable, fancy and useless atm
     */
    public static class OutlierAdjustedColumn implements Serializable
    {
        public String columnName;
        public String sourceColumnName;
        public String adjustmentColumnName;
        public Integer sourceQuartileDisallowed;
        public Integer adjustmentQuartileDisallowed;
    }
}
