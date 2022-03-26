/* 
 * Filename: Utilities.java
 * This class contains helpful functions
 */
package org.quantworm.wormgender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class Utilities {

    protected static final NumberFormat formatter2 = new DecimalFormat("#0.##");
    protected static final NumberFormat formatterInt = new DecimalFormat("#");
    private static final PrintStream out = System.out;

    /**
     * Counts the number of files named piece_*.jpeg in a folder
     *
     * @param folder the folder
     * @return the number of images
     */
    public static int countPiecesFiles(String folder) {
        int ret = 0;
        if (folder == null) {
            return 0;
        }
        folder = folder.trim();
        if (folder.endsWith(File.separator) == false) {
            folder = folder + File.separator;
        }
        int wanted = 1;
        boolean foundFlag;
        do {
            String filename = folder + "piece_" + wanted + ".jpeg";
            File file = new File(filename);
            foundFlag = false;
            if (file.exists() == true) {
                ret++;
                wanted++;
                foundFlag = true;
            }
        } while (foundFlag == true);
        return ret;
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
            String line;
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
     * Gets the int value out of a string
     *
     * @param str the string
     * @return the integer as object; null if unable to convert to integer
     */
    public static Integer getInteger(String str) {
        if (str == null) {
            return null;
        }
        Integer ret;
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
        Double ret;
        try {
            ret = new Double(str);
        } catch (Exception e) {
            return null;
        }
        return ret;
    }

    /**
     * Formats a double value to an int string
     *
     * @param value the value
     * @return the string; when value is null then the string returned is NULL
     */
    public static String formatInt(Double value) {
        if (value == null) {
            return "NULL";
        }
        return formatterInt.format(value);
    }

    /**
     * Formats a double value with maximum two decimals
     *
     * @param value the value
     * @return the string; when value is null then the string returned is NULL
     */
    public static String format2(Double value) {
        if (value == null) {
            return "NULL";
        }
        return formatter2.format(value);
    }

    /**
     * Writes result file
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
            return ex.getMessage();
        }
        return null;
    }

    /**
     * Convert List<double[]> to double[][]
     *
     * @param srcList
     * @return
     */
    public static double[][] convert_ListOfDouble1D_To_ArrayOfDouble2D(List<double[]> srcList) {
        int subItemCount = srcList.get(0).length;

        double[][] a = new double[srcList.size()][subItemCount];
        for (int j = 0; j < srcList.size(); j++) {
            double[] b = srcList.get(j);

            System.arraycopy(b, 0, a[j], 0, subItemCount);
        }

        return a;
    }

    /**
     * Delete files in all sub-folders This function might be problematic (need
     * further testing)
     *
     * @param file
     * @return
     */
    public static boolean deleteFileAndFolder(File file) {

        File[] flist;

        if (file == null) {
            return false;
        }

        if (file.isFile()) {
            return file.delete();
        }

        if (!file.isDirectory()) {
            return false;
        }

        flist = file.listFiles();
        if (flist != null && flist.length > 0) {
            for (File f : flist) {
                if (!deleteFileAndFolder(f)) {
                    return false;
                }
            }
        }

        return file.delete();
    }

    /**
     * Get pure file name without file extension and path
     *
     * @param fileName
     * @return
     */
    public static String get_FileNameWithoutExtension(String fileName) {
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        return fileName;
    }
}
