/* 
 * Filename: Utilities.java
 * Last update on 5/30/2013
 */
package org.quantworm.wormcounter;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class Utilities {

    public static final String ASSEMBLED_JPEG = "assembled.jpeg";
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
     * Assembles image from an image in specified folder, but if it already
     * exists, then it just reads it from disk
     *
     * @param folder the name of the folder
     * @param saveToDiskFlag true: saves the assembled image to disk; false:
     * does not save the assembled image to disk
     * @param scannerLog the ScannerLog object
     * @return
     */
    public static ImagePlus assembleImage(String folder, boolean saveToDiskFlag, ScannerLog scannerLog) {
        long time1 = System.currentTimeMillis();
        File theFile = new File(folder + File.separator + ASSEMBLED_JPEG);
        if (theFile.exists() == true) {
            ImagePlus assembled = new ImagePlus(theFile.getAbsolutePath());
            return assembled;
        }; // if

        int numberOfColumns = scannerLog.getNumberOfColumns();
        int numberOfRows = scannerLog.getNumberOfRows();

        // assemble from pieces
        ImagePlus assembled = NewImage.createByteImage("assembled", numberOfColumns * 640, numberOfRows * 480, 1, NewImage.FILL_BLACK);
        ImageProcessor ipAssembled = assembled.getProcessor();
        for (int i = 1; i <= numberOfColumns; i++) {
            for (int j = 1; j <= numberOfRows; j++) {
                String index = String.valueOf(i + numberOfColumns * (j - 1));
                String path = folder + File.separator + "piece_" + index + ".jpeg";
                if (new File(path).exists()==false) {
                    return null;
                }                
                ImagePlus tempIP = new ImagePlus(path);
                ipAssembled.copyBits(tempIP.getProcessor(), 640 * (i - 1), 480 * (j - 1), Blitter.ADD);
            }; // for
        }; // for
        if (saveToDiskFlag == true) {
            FileSaver saver = new FileSaver(assembled);
            saver.saveAsJpeg(folder + File.separator + ASSEMBLED_JPEG);
        }; // if

        long time2 = System.currentTimeMillis();
        System.out.println(Utilities.format2(((time2 - time1) / 1000.0)) + " seconds for assembling image");
        return assembled;
    }

    
    //Test unit: do not delete
     public static ImagePlus assembleImage_TestUnit(String folder, boolean saveToDiskFlag, ScannerLog scannerLog) {
        long time1 = System.currentTimeMillis();
        File theFile = new File(
                    folder + File.separator + "assembled_org.jpeg");
        if (theFile.exists() == true) {
            ImagePlus assembled = new ImagePlus(
                    folder + File.separator + "assembled_org.jpeg");
            return assembled;
        }; // if

        int numberOfColumns = scannerLog.getNumberOfColumns();
        int numberOfRows = scannerLog.getNumberOfRows();

        // assemble from pieces
        ImagePlus assembled = NewImage.createByteImage("assembled", numberOfColumns * 640, numberOfRows * 480, 1, NewImage.FILL_BLACK);
        ImageProcessor ipAssembled = assembled.getProcessor();
        for (int i = 1; i <= numberOfColumns; i++) {
            for (int j = 1; j <= numberOfRows; j++) {
                String index = String.valueOf(i + numberOfColumns * (j - 1));
                String path = folder + File.separator + "piece_" + index + ".jpeg";
                if (new File(path).exists()==false) {
                    return null;
                }                
                ImagePlus tempIP = new ImagePlus(path);
                ipAssembled.copyBits(tempIP.getProcessor(), 640 * (i - 1), 480 * (j - 1), Blitter.ADD);
            }; // for
        }; // for
        if (saveToDiskFlag == true) {
            FileSaver saver = new FileSaver(assembled);
            saver.saveAsJpeg(folder + File.separator + ASSEMBLED_JPEG);
        }; // if

        long time2 = System.currentTimeMillis();
        System.out.println(Utilities.format2(((time2 - time1) / 1000.0)) + " seconds for assembling image");
        return assembled;
    }
     
     
    
    public static ImagePlus assembleImage_CreateNewly(String folder, boolean saveToDiskFlag, ScannerLog scannerLog) {
        long time1 = System.currentTimeMillis();

        int numberOfColumns = scannerLog.getNumberOfColumns();
        int numberOfRows = scannerLog.getNumberOfRows();

        // assemble from pieces
        ImagePlus assembled = NewImage.createByteImage("assembled", numberOfColumns * 640, numberOfRows * 480, 1, NewImage.FILL_BLACK);
        ImageProcessor ipAssembled = assembled.getProcessor();
        for (int i = 1; i <= numberOfColumns; i++) {
            for (int j = 1; j <= numberOfRows; j++) {
                String index = String.valueOf(i + numberOfColumns * (j - 1));
                String path = folder + File.separator + "piece_" + index + ".jpeg";
                if (new File(path).exists()==false) {
                    return null;
                }
                ImagePlus tempIP = new ImagePlus(path);
                ipAssembled.copyBits(tempIP.getProcessor(), 640 * (i - 1), 480 * (j - 1), Blitter.ADD);
            }; // for
        }; // for
        if (saveToDiskFlag == true) {
            FileSaver saver = new FileSaver(assembled);
            saver.saveAsJpeg(folder + File.separator + ASSEMBLED_JPEG);
        }; // if

        long time2 = System.currentTimeMillis();
        System.out.println(Utilities.format2(((time2 - time1) / 1000.0)) + " seconds for assembling image");
        return assembled;
    }
    
    
     /**
     * Assembles image from an image in specified folder
     * It doesn't not return image!
     * 
     * @param folder the name of the folder
     * @param saveToDiskFlag true: saves the assembled image to disk; false:
     * does not save the assembled image to disk
     * @param scannerLog the ScannerLog object
     * @return if piece images are not found, return false
     */
    public static boolean assembleImage_NoImageReturn(String folder, ScannerLog scannerLog) {
        long time1 = System.currentTimeMillis();
        File theFile = new File(folder + File.separator + ASSEMBLED_JPEG);

        if (theFile.exists() == true) {
            return true;
        }

        int numberOfColumns = scannerLog.getNumberOfColumns();
        int numberOfRows = scannerLog.getNumberOfRows();

        // assemble from pieces
        ImagePlus assembled = NewImage.createByteImage("assembled", numberOfColumns * 640, numberOfRows * 480, 1, NewImage.FILL_BLACK);
        ImageProcessor ipAssembled = assembled.getProcessor();
        for (int i = 1; i <= numberOfColumns; i++) {
            for (int j = 1; j <= numberOfRows; j++) {
                String index = String.valueOf(i + numberOfColumns * (j - 1));
                String path = folder + File.separator + "piece_" + index + ".jpeg";
                if (new File(path).exists()==false) {
                    return false;
                }
                ImagePlus tempIP = new ImagePlus(path);
                ipAssembled.copyBits(tempIP.getProcessor(), 640 * (i - 1), 480 * (j - 1), Blitter.ADD);
            }; // for
        }; // for


        if (assembled == null) {
            return false;
        }


        FileSaver saver = new FileSaver(assembled);
        saver.saveAsJpeg(folder + File.separator + ASSEMBLED_JPEG);

        long time2 = System.currentTimeMillis();
        System.out.println(Utilities.format2(((time2 - time1) / 1000.0)) + " seconds for assembling image");

        return true;
    }

    
    
     /**
     * Assembles image from an image in specified folder, but if it already
     * exists, then it just reads it from disk
     * If assembled doesn't exist, create file and then read the file
     *
     * @param folder the name of the folder
     * @param saveToDiskFlag true: saves the assembled image to disk; false:
     * does not save the assembled image to disk
     * @param scannerLog the ScannerLog object
     * @return ImagePlus
     */
    public static ImagePlus assembleImage_Reload(
            String folder, ScannerLog scannerLog) {
        long time1 = System.currentTimeMillis();
        File theFile = new File(folder + File.separator + ASSEMBLED_JPEG);
        if (theFile.exists() == true) {
            ImagePlus assembled = new ImagePlus(folder + File.separator + ASSEMBLED_JPEG);
            return assembled;
        }; // if

        int numberOfColumns = scannerLog.getNumberOfColumns();
        int numberOfRows = scannerLog.getNumberOfRows();

        // assemble from pieces
        ImagePlus assembled = NewImage.createByteImage("assembled", numberOfColumns * 640, numberOfRows * 480, 1, NewImage.FILL_BLACK);
        ImageProcessor ipAssembled = assembled.getProcessor();
        for (int i = 1; i <= numberOfColumns; i++) {
            for (int j = 1; j <= numberOfRows; j++) {
                String index = String.valueOf(i + numberOfColumns * (j - 1));
                String path = folder + File.separator + "piece_" + index + ".jpeg";
                if (new File(path).exists()==false) {
                    return null;
                }
                ImagePlus tempIP = new ImagePlus(path);
                ipAssembled.copyBits(tempIP.getProcessor(), 640 * (i - 1), 480 * (j - 1), Blitter.ADD);
            }; // for
        }; // for

        
        FileSaver saver = new FileSaver(assembled);
        saver.saveAsJpeg(folder + File.separator + ASSEMBLED_JPEG);

        ImagePlus assembled2 = new ImagePlus(folder + File.separator + ASSEMBLED_JPEG);
                        
        long time2 = System.currentTimeMillis();
        System.out.println(Utilities.format2(((time2 - time1) / 1000.0)) + " seconds for assembling image");
        return assembled2;
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
     * Counts the number of files named piece_*.jpeg in a folder
     *
     * @param folder the folder
     * @return the number of images
     */
    public static int countPiecesFiles(String folder) {
        int ret = 0;
        if (folder == null) {
            return 0;
        }; // if
        folder = folder.trim();
        if (folder.endsWith(File.separator) == false) {
            folder = folder + File.separator;
        }; // if
        int wanted = 1;
        boolean foundFlag = false;
        do {
            String filename = folder + "piece_" + wanted + ".jpeg";
            File file = new File(filename);
            foundFlag = false;
            if (file.exists() == true) {
                ret++;
                wanted++;
                foundFlag = true;
            }; // if
        } while (foundFlag == true);
        return ret;
    }
} // class Utilities

