/**
 * Filename: ScannerLog.java
 */
package edu.rice.wormlab.locomotionassay;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the log file produced by worm-scanner
 */
public class ScannerLog {

    /**
     * the name of the log file name
     */
    // keeps list of errors, if any
    protected final List<String> errorList;
    protected static final PrintStream out = System.out;
    private double stepsPerPixelsX = 0;
    private double stepsPerPixelsY = 0;
    
    /**
     * Default constructor
     */
    public ScannerLog() {
        errorList = new ArrayList<String>();
    }

    public String getErrors() {
        if (errorList.isEmpty() == true) {
            return null;
        }; // if
        String ret = null;
        for (String each : errorList) {
            if (ret == null) {
                ret = each;
            } else {
                ret += "\n" + each;
            }; // if
        }; // if
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
            return null;
        }; // if
        if (directory.exists() == false) {
            scannerLog.errorList.add("Directory does not exist ( " + directory.getAbsolutePath() + " )");
            return null;
        }; // if
        String logFileName;
        logFileName = directory.getAbsolutePath().toString();
        logFileName = logFileName.substring(0, logFileName.length() - 3) + "txt";
        File file = new File(logFileName);
        if (file.exists() == false) {
            
            //if log file is not found, search alternative log file in image folder (ex. ..../LS/03Day/01plate/1)
            int  wellNumber = 
                    Utilities.getWellNumber_From_FullPathFileName(
                                directory.getAbsolutePath().toString());
            String logFileName2;
            logFileName2 = new File(directory.getParent()).getParent().toString();
            logFileName2 = logFileName2 + File.separator + wellNumber
                            + File.separator + "thelog.txt";
            file = new File(logFileName2);
            if (file.exists() == false) {
                scannerLog.errorList.add("Unable to find log file in directory ( " + directory.getAbsolutePath() + " )");
                return scannerLog;
            }
            
        }; // if
        Double x = null;
        Double y = null;
        int z = 0;
        List<String> linesList = Utilities.getLinesFromFile(file);
        for (String each : linesList) {
            if (each.startsWith("#StepsPerPixelsX") == true) {
                String[] pieces = each.split("\t");
                x = Utilities.getDouble(pieces[ 1]);
            }
            if (each.startsWith("#StepsPerPixelsY") == true) {
                String[] pieces = each.split("\t");
                y = Utilities.getDouble(pieces[ 1]);
            }
        }

        scannerLog.setStepsPerPixelsX(x);
        scannerLog.setStepsPerPixelsY(y);
        return scannerLog;
    }

   
    /**
     * @return the stepsPerPixelsX
     */
    public double getStepsPerPixelsX() {
        return stepsPerPixelsX;
    }

    /**
     * @param stepsPerPixelsX the stepsPerPixelsX to set
     */
    public void setStepsPerPixelsX(double stepsPerPixelsX) {
        this.stepsPerPixelsX = stepsPerPixelsX;
    }

    /**
     * @return the stepsPerPixelsY
     */
    public double getStepsPerPixelsY() {
        return stepsPerPixelsY;
    }

    /**
     * @param stepsPerPixelsY the stepsPerPixelsY to set
     */
    public void setStepsPerPixelsY(double stepsPerPixelsY) {
        this.stepsPerPixelsY = stepsPerPixelsY;
    }
}
