/** 
 * Filename: ResultProcessor.java
 * This class is for the Print Report function
 */

package edu.rice.wormlab.wormlength;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;


/**
 * Reads App.RESULT_TXT and writes report
 */

public class ResultProcessor{
	

	/**
	 * Recursively processes a directory and all its sub-directories
	 * @param  directory  the directory
	 */
	public void recursivelyProcessDirectory( String directory ) {
		List<String> resultsList = new ArrayList<String>();
		List<String> detailsList = new ArrayList<String>();
		recursivelyProcessDirectory( directory, resultsList, detailsList );
		// save the new contents
		try {
			FileWriter fileWriter = new FileWriter( directory + File.separator + "report-length.txt" );
			BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
			PrintWriter printWriter = new PrintWriter( bufferedWriter );
			printWriter.println( "folder" + "\t" + "n" + "\t" + "status" );
			for( String line : resultsList ) {
				printWriter.println( line );
			}; // for
			printWriter.close();
		}
		catch( IOException ioe ) {
			ioe.printStackTrace();
		}; // try
		
		// save the new contents
		try {
			FileWriter fileWriter = new FileWriter( directory + File.separator + "report-details-length.txt" );
			BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
			PrintWriter printWriter = new PrintWriter( bufferedWriter );
			printWriter.println( "folder" + "\t" + "length(micrometers)" + "\t" + "status" );
			for( String line : detailsList ) {
				printWriter.println( line );
			}; // for
			printWriter.close();
		}
		catch( IOException ioe ) {
			ioe.printStackTrace();
		}; // try
	}
	

	/**
	 * Recursively processes a directory and all its sub-directories (non-public version)
	 */
	private void recursivelyProcessDirectory( String directory, List<String> resultsList, List<String> detailsList ) {
		File dir = new File( directory );
		if( dir.exists() == false ) {
			// Directory does not exist
			return;
		}; // if
		
		if( dir.isDirectory() == false ) {
			// This is not a directory
			return;
		}; // if

		// get the sub-directories
		List<String> subdirectoriesList = new ArrayList<String>();
		File[] folders = dir.listFiles();
		for( File eachFolder : folders ) {
			if( eachFolder.isDirectory() == false ) {
				continue;
			}; // if
			subdirectoriesList.add( eachFolder.getAbsolutePath() );
		}; // for

		reviewResults( dir.getAbsolutePath(), resultsList, detailsList );

		// recursion happens here
		for( String subdirectory : subdirectoriesList ) {
			recursivelyProcessDirectory( subdirectory, resultsList, detailsList );
		}; // for
	}


	/** 
	 * Reviews results and when valid, adds them to the list of results
	 * @param  directory  directory containing results file
	 * @param  resultsList  the list of results
	 * @param  detailsList  the list of detailed results
	 */
	public void reviewResults( String directory, List<String> resultsList, List<String> detailsList ) {
		int countImages = Utilities.countPiecesFiles( directory );
		File resultFile = new File( directory + File.separator + App.RESULT_TXT );
		if( resultFile.exists() == false ) {
			if( countImages > 0 ) {
				resultsList.add( directory + "\tN/A\t" + "Contains data but not processed" );
			}; // if
			return;
		}; // if
		List<String> linesList = Utilities.getLinesFromFile( resultFile );
		if( linesList == null ) {
			if( countImages > 0 ) {
				resultsList.add( directory + "\tN/A\t" + "Contains data but " + App.RESULT_TXT + " is empty" );
			}; // if
			return;
		}; // if
		
		// process the text file
		Integer wormCountLine = null;
		List<Double> valuesList = new ArrayList<Double>();
		String status = "not-inspected";
		int count = 0;
		for( String each : linesList ) {
			String[] items = each.split( "\t" );
			if( each.startsWith( "#" ) == true ) {
				if( App.METHOD_MANUAL.equals( items[ 0 ] ) == true && items.length >= 2 ) {
					if( "Done".equalsIgnoreCase( items[ 1 ] ) == true ) {
						status = "inspected";
					}; // if
				}; // if
				if( each.startsWith( "# worms count:\t" ) == true & items.length >= 2 ) {
					wormCountLine = Utilities.getInteger( items[ 1 ] );
				}; // if
				continue;
			}; // if
			if( items.length == 6 ) {
				WormInfo wormInfo = WormInfo.createWormInfo( items );
				if( wormInfo == null ) {
					final String message = "ERROR, skipping WormInfo line, unexpected format: " + each;
					resultsList.add( directory + "\tN/A\t" + message );
					return;
				}; // if
				count += wormInfo.nWorm;
				valuesList.add( wormInfo.trueLength );
			}
			else {
				final String message = "ERROR, skipping WormInfo line, unexpected format: " + each;
				resultsList.add( directory + "\tN/A\t" + message );
				return;
			}; // if
		}; // for
		if( wormCountLine == null ) {
			final String message = "Unable to process data of number of worms (worm count is undefined)";
			resultsList.add( directory + "\tN/A\t" + message );
			return;
		}; // if
		if( count != wormCountLine.intValue() ) {
			final String message = "ERROR, worms count in " + resultFile.getAbsolutePath() + " file mismatch with actual worms count!";
			resultsList.add( directory + "\tN/A\t" + message );
			return;
		}; // if

		File assembled = new File( directory + File.separator + App.ASSEMBLED_COLORS_JPEG );
		if( assembled.exists() == true ) { 
			if( status.equals( "not-inspected" ) == true ) {
				status += "---assembled-colors-image-does-exits!";
			}; // if
		}
		else {
			if( status.equals( "inspected" ) == true ) {
				status += "---assembled-colors-image-is-missing!";
			}; // if
		}; // if
		resultsList.add( directory + "\t" + count + "\t" + status );
		for( Double each : valuesList ) {
			detailsList.add( directory + "\t" + each + "\t" + status );
		}; // for
	}
	
}


