/**
 * Filename: ScannerLog.java
 * This class contains function to read the log file produced by WormScanner
 */

package edu.rice.wormlab.wormlength;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the log file produced by worm-scanner
 */

public class ScannerLog {

	/** the name of the log file name */
	public static final String LOG_FILENAME = "thelog.txt";

	// keeps list of errors, if any
	protected final List<String> errorList;
	
	protected static final PrintStream out = System.out;
	
	private int numberOfRows = 0;
	
	private int numberOfColumns = 0;
	
	private double stepsPerPixelsX = 0;
	
	private double stepsPerPixelsY = 0;
	
	/**
	 * Default constructor
	 */
	public ScannerLog() {
		errorList = new ArrayList<String>();
	}
	
	public String getErrors() {
		if( errorList.isEmpty() == true ) {
			return null;
		}; // if
		String ret = null;
		for( String each : errorList ) {
			if( ret == null ) {
				ret = each;
			}
			else {
				ret += "\n" + each;
			}; // if
		}; // if
		return ret;
	}
	

	/**
	 * Reads values from the log from a directory, such as steps per pixel for x and y (log file name is LOG_FILENAME)
	 * @param  directory  the directory
	 * @return  the scannerLog object
	 * */
        public static ScannerLog readLog(File directory) {
        ScannerLog scannerLog = new ScannerLog();
        if (directory == null) {
            scannerLog.errorList.add("Internal error, directory is null in the ScannerLog");
            return scannerLog;
        }; // if
        if (directory.exists() == false) {
            scannerLog.errorList.add("Directory does not exist ( " + directory.getAbsolutePath() + " )");
            return scannerLog;
        }; // if
        File file = new File(directory.getAbsolutePath() + File.separator + LOG_FILENAME);
        if (file.exists() == false) {
            scannerLog.errorList.add("Unable to find log file " + LOG_FILENAME + " in directory ( " + directory.getAbsolutePath() + " )");
            return scannerLog;
        }; // if
        Double x = null;
        Double y = null;
        List<String> linesList = Utilities.getLinesFromFile( file );
    	String seen = null;
    	int columns = 0;
    	int total = 0;
    	for( String each : linesList ) {
    		if( each.startsWith( "#StepsPerPixelsX" ) == true ) {
    			String[] pieces = each.split( "\t" );
    			x = Utilities.getDouble( pieces[ 1 ] );
    		}; // if
    		if( each.startsWith( "#StepsPerPixelsY" ) == true ) {
    			String[] pieces = each.split( "\t" );
    			y = Utilities.getDouble( pieces[ 1 ] );
    		}; // if
    		if( each.startsWith( "piece_" ) == true ) {
    			total++;
    			String[] pieces = each.split( "\t" );
    			if( pieces.length == 3 ) {
    				String third = pieces[ 2 ];
    				if( seen == null ) {
    					// first time
    					seen = third;
    				}; // if
    				if( third.equals( seen ) == true ) {
    					columns++;
    				}; // if
    			}; // if
    		}; // if
    	}; // for
    	int rows = 0;
    	if( columns > 0 ) {
    		rows = total / columns;
    	}; // if
    	scannerLog.numberOfColumns = columns;
    	scannerLog.numberOfRows = rows;
    	if( x == null ) {
    		scannerLog.errorList.add( "unable to read StepsPerPixelsX from " + file.getAbsolutePath() );
    	}; //if
    	if( y == null ) {
    		scannerLog.errorList.add( "unable to read StepsPerPixelsY from " + file.getAbsolutePath() );
    	}; //if
    	if( x == null || y == null ) {
    		return scannerLog;
    	}; // if
    	scannerLog.setStepsPerPixelsX( x );
    	scannerLog.setStepsPerPixelsY( y );
    	return scannerLog;
    }

	
	/**
	 * Get the number of rows
	 * @return the numberOfRows
	 */
	public int getNumberOfRows() {
		return numberOfRows;
	}


	/**
	 * Get the number of columns
	 * @return the numberOfColumns
	 */
	public int getNumberOfColumns() {
		return numberOfColumns;
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
