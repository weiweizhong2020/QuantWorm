/** 
 * Filename: ResultProcessor.java
 * This class is for the Print Report function
 */

package edu.rice.wormlab.lifespan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


/**
 * Reads MotionDetection.N_LIVE_RESULTS_TXT in a folder (and subfolders) 
 * and creates a report
 *
 */
public class ResultProcessor {

	public static final String REPORT_FILENAME = "report-lifespan.txt";

	/**
	 * Recursively processes a directory and all its sub-directories
	 * @param  directory  the directory
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String recursivelyProcessDirectory( File directory ) {
		List<String> resultsList = new ArrayList<String>();
		recursivelyProcessDirectory( directory, resultsList );
		// save the new contents
		try {
			FileWriter fileWriter = new FileWriter( directory.getAbsolutePath() + File.separator + REPORT_FILENAME );
			BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
			PrintWriter printWriter = new PrintWriter( bufferedWriter );
			printWriter.println( "folder" + "\t" + "moving-worms" + "\t" + "status" );
			for( String line : resultsList ) {
				printWriter.println( line );
			}; // for
			printWriter.close();
		}
		catch( IOException ioe ) {
			ioe.printStackTrace();
			return ioe.getMessage();
		}; // try
		return null;
	}; // recursivelyProcessDirectory
	

	/**
	 * Recursively processes a directory and all its sub-directories (non-public version)
	 */
	private void recursivelyProcessDirectory( File directory, List<String> resultsList ) {
		if( directory.exists() == false ) {
			System.out.println( "Directory does not exist: " + directory );
			return;
		}; // if
		
		if( directory.isDirectory() == false ) {
			System.out.println( "This is not a directory: " + directory );
			return;
		}; // if

		// get the sub-directories
		List<File> subdirectoriesList = new ArrayList<File>();
		File[] folders = directory.listFiles();
		for( File eachFolder : folders ) {
			if( eachFolder.isDirectory() == false ) {
				continue;
			}; // if
			subdirectoriesList.add( eachFolder );
		}; // for

		reviewResults( directory, resultsList );

		// recursion happens here
		for( File subdirectory : subdirectoriesList ) {
			recursivelyProcessDirectory( subdirectory, resultsList );
		}; // for
	}; // recursivelyProcessDirectory


	/** 
	 * Reviews results and when valid, adds them to the list of results
	 * @param  directory  directory containing results file
	 * @param  resultsList  the list of results
	 */
	public void reviewResults( File directory, List<String> resultsList ) {
		// we skip the folder that does not end with "__1"
		if( directory.getName().endsWith( App.UNDERSCORE_UNDERSCORE_ONE ) == false ) {
			return;
		}; // if
		int countImages = Utilities.countPiecesFiles( directory.getAbsolutePath() );
		File resultFile = new File( directory.getAbsolutePath() + File.separator + MotionDetection.N_LIVE_RESULTS_TXT );
		if( resultFile.exists() == false ) {
			if( countImages > 0 ) {
				resultsList.add( directory.getAbsolutePath() + "\tN/A\t" + "Contains data but not processed" );
			}; // if
			return;
		}; // if
		List<String> linesList = Utilities.getLinesFromFile( resultFile );
		if( linesList == null ) {
			if( countImages > 0 ) {
				resultsList.add( directory.getAbsolutePath() + "\tN/A\t" + "Contains data but " + MotionDetection.N_LIVE_RESULTS_TXT + " is empty" );
			}; // if
			return;
		}; // if
		
		// process the text file
		Integer wormCountLine = null;
		String status = "not-inspected";
		int count = 0;
		boolean seenWormDetailsFlag = false;
		for( String each : linesList ) {
			// see whether it says inspected by human
			if( MotionDetection.INSPECTED_BY_HUMAN.equals( each ) == true ) {
				status = "inspected";
				continue;
			}; // if
			// read the total-live-worms line
			if( each.startsWith( MotionDetection.TOTAL_LIVE_WORMS ) == true 
			&& each.length() > MotionDetection.TOTAL_LIVE_WORMS.length() ) {
				wormCountLine = Utilities.getInteger( each.substring( MotionDetection.TOTAL_LIVE_WORMS.length() ) );
				continue;
			}; // if
			// are we at the worm details yet?
			if( MotionDetection.WORM_DETAILS.equals( each ) == true ) {
				seenWormDetailsFlag = true;
			}; // if
			String[] items = each.split( "\t" );
			if( each.startsWith( "#" ) == true || seenWormDetailsFlag == false ) {
				continue;
			}; // if
			if( items.length == 5 ) {
				Integer tmp = Utilities.getInteger( items[ 0 ] );
				if( tmp != null ) {
					count += tmp.intValue();
				}; // if
			}; // if
		}; // for
		if( wormCountLine == null ) {
			final String message = "Unable to process data of number of worms (worm count is undefined)";
			System.out.println( message );
			resultsList.add( directory.getAbsolutePath() + "\tN/A\t" + message );
			return;
		}; // if
		if( count != wormCountLine.intValue() ) {
			final String message = "ERROR, worms count in " + resultFile.getAbsolutePath() + " file mismatch with actual worms count!";
			System.out.println( message );
			resultsList.add( directory.getAbsolutePath() + "\tN/A\t" + message );
			return;
		}; // if

		File assembled = new File( directory.getAbsolutePath() + File.separator + Utilities.ASSEMBLED_JPEG );
		if( assembled.exists() == true ) { 
			if( status.equals( "not-inspected" ) == true ) {
				status += "---assembled-colors-image-does-exist!";
			}; // if
		}
		else {
			if( status.equals( "inspected" ) == true ) {
				status += "---assembled-colors-image-is-missing!";
			}; // if
		}; // if
		resultsList.add( directory.getAbsolutePath() + "\t" + count + "\t" + status );
	}
	
}
