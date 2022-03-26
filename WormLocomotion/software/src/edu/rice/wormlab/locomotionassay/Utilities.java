/* 
 * Filename: Utilities.java
 * This class contains helpful functions
 */

package edu.rice.wormlab.locomotionassay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
        }; // if
        Integer ret = null;
        try {
            ret = new Integer(str);
        } catch (Exception e) {
            return null;
        }; // try
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
        }; // if
        Double ret = null;
        try {
            ret = new Double(str);
        } catch (Exception e) {
            return null;
        }; // try
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
        String folderName = file.getParent().toString();
        String wellNumberStr = "";
        String oneLetter;
        for (int i = 1; i < folderName.length(); i++) {
            oneLetter = folderName.substring(folderName.length() - i,
                    folderName.length() - i + 1);
            if (isNumeric(oneLetter)) {
                wellNumberStr = folderName.substring(folderName.length() - i,
                        folderName.length() - i + 1) + wellNumberStr;
            } else {
					Integer wellNumber = null;
					try {
                wellNumber = Integer.parseInt(wellNumberStr);
					}
					catch( Exception ignore ) {
						wellNumber = null;
					}; // try
					if( wellNumber == null ) {
						return -1;
					}; // if
               return wellNumber;
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
        }; // if
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
        }; // if
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
                }; // while
            } else {
                bufferedReader.close();
                return null;
            }; // if
            bufferedReader.close();
        } catch (FileNotFoundException fnfe) {
            return null;
        } catch (IOException ioe) {
            return null;
        }; // try
        return linesList;
    }

    
    /**
     * Get a name list of all files
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
    
    
}
