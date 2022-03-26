/*
 * Filename: Utilities.java
 * This class contains helpful functions
 */

package edu.rice.wormlab.eggcounter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.ImageProcessor;

public class Utilities {

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
		File theFile = new File(folder + File.separator + App.ASSEMBLED_JPEG);
		if (theFile.exists() == true) {
			ImagePlus assembled = new ImagePlus(folder + File.separator + App.ASSEMBLED_JPEG);
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
				ImagePlus tempIP = new ImagePlus(path);
				ipAssembled.copyBits(tempIP.getProcessor(), 640 * (i - 1), 480 * (j - 1), Blitter.ADD);
			}; // for
		}; // for
		if (saveToDiskFlag == true) {
			FileSaver saver = new FileSaver(assembled);
			saver.saveAsJpeg(folder + File.separator + App.ASSEMBLED_JPEG);
		}; // if

		return assembled;
	}
	/**
	 * Reads a file 
	 * @param  file  the file
	 * @return  the lines of the file, or null when there is error
	 */
	public static List<String> getLinesFromFile( File file ) {
		List<String> linesList = new ArrayList<String>();
		try {
			BufferedReader bufferedReader = new BufferedReader( new FileReader( file ) );
			String line = null;
			if( bufferedReader.ready() ) {
				while( ( line = bufferedReader.readLine() ) != null ) {
					linesList.add( line );
				}; // while
			}
			else {
				bufferedReader.close();
				return null;
			}; // if
			bufferedReader.close();
		}
		catch( FileNotFoundException fnfe ) {
			return null;
		}
		catch( IOException ioe ) {
			return null;
		}; // try
		return linesList;
	}
	
	
	/**
	 * Gets the int value out of a string
	 * @param  str  the string
	 * @return  the integer as object; null if unable to convert to integer
	 */
	public static Integer getInteger( String str ) {
		if( str == null ) {
			return null;
		}; // if
		Integer ret = null;
		try {
			ret = new Integer( str );
		}
		catch( Exception e ) {
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

} // class Utilities

