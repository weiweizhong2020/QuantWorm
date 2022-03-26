/* 
 * Filename: Utilities.java
 * This class contains helpful functions
 */
package org.quantworm.wormtrapassay;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Utilities {

    protected static final NumberFormat formatter2 = new DecimalFormat("#0.##");
    protected static final NumberFormat formatter4 = new DecimalFormat("#0.####");

    /**
     * Gets the int value out of a string
     *
     * @param str the string
     * @return the integer as object; null if unable to convert to integer
     */
    public static Integer getInteger(String str) {
        if (str == null) {
            return null;
        }
        Integer ret = null;
        try {
            ret = new Integer(str);
        } catch (Exception e) {
            return null;
        }
        return ret;
    }

    /**
     * Gets the double value out of a string
     *
     * @param str the string
     * @return the double as object; null if unable to convert it to double
     */
    public static Double getDouble(String str) {
        if (str == null) {
            return null;
        }
        Double ret = null;
        try {
            ret = new Double(str);
        } catch (Exception e) {
            return null;
        }
        return ret;
    }

    /**
     * Get well number from file name (ex. .../video_1/video_1.avi)
     *
     * @param str the string
     * @return well number
     */
    public static int getWellNumber_From_FullPathFileName(String str) {
        File file = new File(str);
        String folderName = file.getParent();
        String wellNumberStr = "";
        String oneLetter;
        for (int i = 1; i < folderName.length(); i++) {
            oneLetter = folderName.substring(folderName.length() - i,
                    folderName.length() - i + 1);
            if (isNumeric(oneLetter)) {
                wellNumberStr = folderName.substring(folderName.length() - i,
                        folderName.length() - i + 1) + wellNumberStr;
            } else {
                return Integer.parseInt(wellNumberStr);
            }
        }

        return -1;
    }

    /**
     * Check whether source string is numeric or not
     *
     * @param str the source string
     * @return true if source string is numeric
     */
    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    /**
     * Formats a double value with maximum two decimals
     *
     * @param value the vlaue
     * @return the string; when value is null then the string returned is NULL
     */
    public static String format2(Double value) {
        if (value == null) {
            return "NULL";
        }
        return formatter2.format(value);
    }

    /**
     * Formats a double value with maximum four decimals
     *
     * @param value the vlaue
     * @return the string; when value is null then the string returned is NULL
     */
    public static String format4(Double value) {
        if (value == null) {
            return "NULL";
        }
        return formatter4.format(value);
    }

    /**
     * Reads a file
     *
     * @param file the file
     * @return the lines of the file, or null when there is error
     */
    public static List<String> getLinesFromFile(File file) {
        List<String> linesList = new ArrayList<String>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line = null;
            if (bufferedReader.ready()) {
                while ((line = bufferedReader.readLine()) != null) {
                    linesList.add(line);
                }
            } else {
                bufferedReader.close();
                return null;
            }
            bufferedReader.close();
        } catch (FileNotFoundException fnfe) {
            return null;
        } catch (IOException ioe) {
            return null;
        }
        return linesList;
    }

    /**
     * Reads a file
     *
     * @param file the file
     * @return the lines of the file, or null when there is error
     */
    public static List<String> getLinesFromFileWithEndMark(File file, String endMark) {
        List<String> linesList = new ArrayList<String>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line = null;
            if (bufferedReader.ready()) {
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.startsWith(endMark) == true) {
                        break;
                    }
                    linesList.add(line);
                }
            } else {
                bufferedReader.close();
                return null;
            }
            bufferedReader.close();
        } catch (FileNotFoundException fnfe) {
            return null;
        } catch (IOException ioe) {
            return null;
        }
        return linesList;
    }

    /**
     * Get a name list of all files
     *
     * @param directory
     * @param suffix
     * @return
     */
    protected static File[] getAllFiles(File directory, String suffix) {
        List<File> retList = new ArrayList<File>();
        LinkedList<File> list = new LinkedList<File>();
        File[] insideFiles = directory.listFiles();
        list.addAll(Arrays.asList(insideFiles));
        while (list.isEmpty() == false) {
            File tmp = list.removeFirst();
            if (tmp.isDirectory() == true) {
                File[] insideFiles2 = tmp.listFiles();
                if (insideFiles2 != null) {
                    list.addAll(Arrays.asList(insideFiles2));
                }
            } else {
                if (tmp.getName().endsWith(suffix)) {
                    retList.add(tmp);
                }
            }
        }
        return retList.toArray(new File[0]);
    }

    /**
     * Writes results (TRACKING_RESULTS) file
     *
     * @param absoluteFileName
     * @param list contains lines of results to be written
     * @return null when things go OK; otherwise it returns an error message
     */
    public static String writeResults(String absoluteFileName, List<String> list) {

        try {
            FileWriter fileWriter = new FileWriter(absoluteFileName);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter printWriter = new PrintWriter(bufferedWriter);
            for (String each : list) {
                printWriter.println(each);
            }
            printWriter.flush();
            printWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return ex.getMessage();
        }
        return null;
    }

    /**
     * Get file name without file extension
     *
     * @param fileName
     * @return
     */
    public static String get_FileNameWithoutExtension(String fileName) {
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        return fileName;
    }

    /**
     * Get substring from the end of string
     *
     * @param s
     * @param length
     * @return
     */
    public static String lastSubString(String s, int length) {
        return s.substring(s.length() - length, s.length());
    }
}
