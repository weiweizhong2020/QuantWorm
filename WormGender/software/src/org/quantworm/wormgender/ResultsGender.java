/**
 * Filename: ResultsGender.java
 *
 * Operations on results file
 */
package org.quantworm.wormgender;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

public class ResultsGender {

    /**
     * constant for text filename of results
     */
    public static final String RESULTS_TXT = "result-gender.txt";

    /**
     * constant to mark a folder as inspected
     */
    public static final String INSPECTED = "Inspected";

    /**
     * constant to mark a folder as not inspected
     */
    public static final String NOT_INSPECTED = "Not " + INSPECTED;

    // the list of worm objects
    private List<WormInfo> wormsList;

    // inspected flag
    private boolean inspectedFlag;

    // the number of hermaphrodites as shown in the file
    protected Integer wormHermTotalFromFile;

    // the number of males as shown in the file
    protected Integer wormMaleTotalFromFile;

    // the number of suspicious in the original file
    protected Integer suspiciousOriginalCount;

    /**
     * Constructor
     */
    public ResultsGender() {
        wormsList = null;
        inspectedFlag = false;
        wormHermTotalFromFile = null;
        wormMaleTotalFromFile = null;
        suspiciousOriginalCount = null;
    }

    /**
     * Get the list of worms objects
     *
     * @return list of worms objects (it can be null)
     */
    public List<WormInfo> getWormsList() {
        return wormsList;
    }

    /**
     * Reads worms from file
     *
     * @param file the file
     * @return null if things go OK; otherwise it returns an error message
     */
    public String readWormsFromFile(File file) {
        wormsList = null;
        inspectedFlag = false;
        wormHermTotalFromFile = null;
        wormMaleTotalFromFile = null;
        suspiciousOriginalCount = null;
        if (file == null) {
            return "Unable to read file (null)";
        }
        List<String> linesList = Utilities.getLinesFromFile(file);
        if (linesList == null) {
            return "Unable to read file ( " + file.getAbsolutePath() + " )";
        }
        List<WormInfo> retList = new ArrayList<WormInfo>();

        // process the text file
        int lineNumber = 0;
        for (String each : linesList) {
            lineNumber++;
            String[] items = each.split("\t");
            if (each.startsWith("#") == true) {
                if (each.startsWith("# Male count") == true & items.length >= 2) {
                    Integer num = Utilities.getInteger(items[ 1]);
                    if (num == null) {
                        return "Unable to read total number of males from: '" + items[ 1] + "'";
                    }
                    if (wormMaleTotalFromFile == null) {
                        wormMaleTotalFromFile = num;
                    } else {
                        return "Possible format error related to line " + lineNumber + " ...: " + each;
                    }
                }
                if (each.startsWith("# Herm count") == true & items.length >= 2) {
                    Integer num = Utilities.getInteger(items[ 1]);
                    if (num == null) {
                        return "Unable to read total number of hermaphrodites from: '" + items[ 1] + "'";
                    }
                    if (wormHermTotalFromFile == null) {
                        wormHermTotalFromFile = num;
                    } else {
                        return "Possible format error related to line " + lineNumber + " ...: " + each;
                    }
                }
                if (each.startsWith("# Status") == true & items.length >= 2) {
                    if ("Inspected".equalsIgnoreCase(items[ 1]) == true) {
                        inspectedFlag = true;
                    }
                }
                continue;
            }
            if (items.length == 12) {
                WormInfo worm = new WormInfo();
                Integer num = Utilities.getInteger(items[ 0]);
                if (num == null) {
                    return "Unable to figure out pX value ( " + items[ 0] + " )";
                }
                worm.pX = num;

                num = Utilities.getInteger(items[ 1]);
                if (num == null) {
                    return "Unable to figure out pY value ( " + items[ 1] + " )";
                }
                worm.pY = num;

                num = Utilities.getInteger(items[ 2]);
                if (num == null) {
                    return "Unable to figure out width value ( " + items[ 2] + " )";
                }
                worm.width = num;

                num = Utilities.getInteger(items[ 3]);
                if (num == null) {
                    return "Unable to figure out height value ( " + items[ 3] + " )";
                }
                worm.height = num;

                num = Utilities.getInteger(items[ 4]);
                if (num == null) {
                    return "Unable to figure out nHerma value ( " + items[ 4] + " )";
                }
                worm.nHerma = num;

                num = Utilities.getInteger(items[ 5]);
                if (num == null) {
                    return "Unable to figure out nMale value ( " + items[ 5] + " )";
                }
                worm.nMale = num;

                Double value = Utilities.getDouble(items[ 6]);
                if (value == null) {
                    return "Unable to figure out length (um) value ( " + items[ 6] + " )";
                }
                worm.trueLen = value;

                value = Utilities.getDouble(items[ 7]);
                if (value == null) {
                    return "Unable to figure out fatness value ( " + items[ 7] + " )";
                }
                worm.f = value;

                value = Utilities.getDouble(items[ 8]);
                if (value == null) {
                    return "Unable to figure out e1 value ( " + items[ 8] + " )";
                }
                worm.e[ 0] = value;

                value = Utilities.getDouble(items[ 9]);
                if (value == null) {
                    return "Unable to figure out e2 value ( " + items[ 9] + " )";
                }
                worm.e[ 1] = value;

                num = Utilities.getInteger(items[ 10]);
                if (num == null) {
                    return "Unable to figure out maskImageIDnumb value ( " + items[ 10] + " )";
                }
                worm.maskImageIDNumb = num;

                worm.setSuspicious("true".equalsIgnoreCase(items[ 11]));
                if (worm.isSuspicious() == true) {
                    if (suspiciousOriginalCount == null) {
                        suspiciousOriginalCount = 1;
                    } else {
                        suspiciousOriginalCount = suspiciousOriginalCount + 1;
                    }
                }
                retList.add(worm);
                continue;
            }
            if ("".equals(each.trim()) == false) {
                return "Unexpected format in line number " + lineNumber + " of " + RESULTS_TXT + "\nLine: " + each.replaceAll("\t", " ");
            }
        }

        // verify that there was a herm,male count in text file
        if (wormMaleTotalFromFile == null) {
            return "Missing count of males in " + RESULTS_TXT + " !";
        }
        if (wormHermTotalFromFile == null) {
            return "Missing count of hermaphrodites in " + RESULTS_TXT + " !";
        }

        // verify the worms count with that listed in the file
        int count = 0;
        for (WormInfo wormInfo : retList) {
            count += wormInfo.nHerma + wormInfo.nMale;
        }
        if (count != (wormMaleTotalFromFile.intValue() + wormHermTotalFromFile.intValue())) {
            return "Worm count in " + RESULTS_TXT + " file mismatch with actual worms count!";
        }

        // no errors detected up to now
        wormsList = retList;
        return null;
    }

} // class ResultsGender

