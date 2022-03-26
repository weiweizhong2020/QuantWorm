/**
 * Filename: ScannerLog.java This class contains function to read the log file
 * produced by WormScanner
 */
package org.quantworm.wormgender;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class ScannerLog {

    /**
     * the name of the log file name
     */
    public static final String LOG_FILENAME = "thelog.txt";

    // keeps list of errors, if any
    protected final List<String> errorList;

    protected static final PrintStream out = System.out;

    private int numberOfRows = 0;

    private int numberOfColumns = 0;

    private double stepsPerPixelsX = 0;

    private double stepsPerPixelsY = 0;

    public static double micronsPerStepX = 1;

    public static double micronsPerStepY = 1;

    /**
     * Default constructor
     */
    public ScannerLog() {
        errorList = new ArrayList<String>();
    }

    public String getErrors() {
        if (errorList.isEmpty() == true) {
            return null;
        }
        String ret = null;
        for (String each : errorList) {
            if (ret == null) {
                ret = each;
            } else {
                ret += "\n" + each;
            }
        }
        return ret;
    }

    /**
     * Reads values from the log from a directory, such as steps per pixel for x
     * and y (log file name is LOG_FILENAME)
     *
     * @param directory the directory
     * @return the scannerLog object
     *
     */
    public static ScannerLog readLog(File directory) {
        ScannerLog scannerLog = new ScannerLog();
        if (directory == null) {
            scannerLog.errorList.add("Internal error, directory is null in the ScannerLog");
            return scannerLog;
        }
        if (directory.exists() == false) {
            scannerLog.errorList.add("Directory does not exist ( " + directory.getAbsolutePath() + " )");
            return scannerLog;
        }
        File file = new File(directory.getAbsolutePath() + File.separator + LOG_FILENAME);
        if (file.exists() == false) {
            scannerLog.errorList.add("Unable to find log file " + LOG_FILENAME + " in directory ( " + directory.getAbsolutePath() + " )");
            return scannerLog;
        }
        Double x = null;
        Double y = null;
        List<String> linesList = Utilities.getLinesFromFile(file);
        String seen = null;
        int columns = 0;
        int total = 0;
        for (String each : linesList) {
            if (each.startsWith("#StepsPerPixelsX") == true) {
                String[] pieces = each.split("\t");
                x = Utilities.getDouble(pieces[ 1]);
            }
            if (each.startsWith("#StepsPerPixelsY") == true) {
                String[] pieces = each.split("\t");
                y = Utilities.getDouble(pieces[ 1]);
            }
            if (each.startsWith("piece_") == true) {
                total++;
                String[] pieces = each.split("\t");
                if (pieces.length == 3) {
                    String third = pieces[ 2];
                    if (seen == null) {
                        // first time
                        seen = third;
                    }
                    if (third.equals(seen) == true) {
                        columns++;
                    }
                }
            }
        }
        int rows = 0;
        if (columns > 0) {
            rows = total / columns;
        }
        scannerLog.numberOfColumns = columns;
        scannerLog.numberOfRows = rows;
        if (x == null) {
            scannerLog.errorList.add("unable to read StepsPerPixelsX from " + file.getAbsolutePath());
        }
        if (y == null) {
            scannerLog.errorList.add("unable to read StepsPerPixelsY from " + file.getAbsolutePath());
        }
        if (x == null || y == null) {
            return scannerLog;
        }
        scannerLog.setStepsPerPixelsX(x);
        scannerLog.setStepsPerPixelsY(y);
        return scannerLog;
    }

    /**
     * Get the number of rows
     *
     * @return the numberOfRows
     */
    public int getNumberOfRows() {
        return numberOfRows;
    }

    /**
     * Get the number of columns
     *
     * @return the numberOfColumns
     */
    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    /**
     * Get steps per pixel in X Note: 1 step is equal to 1 micrometer in a
     * moving stage
     *
     * @return the stepsPerPixelsX
     */
    public double getStepsPerPixelsX() {
        return stepsPerPixelsX;
    }

    /**
     * Set steps per pixel in X Note: 1 step is equal to 1 micrometer in a
     * moving stage
     *
     * @param stepsPerPixelsX the stepsPerPixelsX to set
     */
    public void setStepsPerPixelsX(double stepsPerPixelsX) {
        this.stepsPerPixelsX = stepsPerPixelsX;
    }

    /**
     * Get steps per pixel in Y Note: 1 step is equal to 1 micrometer in a
     * moving stage
     *
     * @return the stepsPerPixelsY
     */
    public double getStepsPerPixelsY() {
        return stepsPerPixelsY;
    }

    /**
     * Set steps per pixel in Y Note: 1 step is equal to 1 micrometer in a
     * moving stage
     *
     * @param stepsPerPixelsY the stepsPerPixelsY to set
     */
    public void setStepsPerPixelsY(double stepsPerPixelsY) {
        this.stepsPerPixelsY = stepsPerPixelsY;
    }

}
