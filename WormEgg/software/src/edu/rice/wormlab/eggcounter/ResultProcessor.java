/*
 * Filename: ResultProcessor.java
 * This class is for writing a summary report
 */

package edu.rice.wormlab.eggcounter;

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
		recursivelyProcessDirectory( directory, resultsList );
		// save the new contents
		try {
			FileWriter fileWriter = new FileWriter( directory + File.separator + "report-egg.txt" );
			BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
			PrintWriter printWriter = new PrintWriter( bufferedWriter );
			for( String line : resultsList ) {
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
	 * @param  directory  the directory
	 */
	private void recursivelyProcessDirectory( String directory, List<String> resultsList ) {
		File dir = new File( directory );
		if( dir.exists() == false ) {
			return;
		}; // if
		
		if( dir.isDirectory() == false ) {
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

		reviewResults( dir.getAbsolutePath(), resultsList );

		// recursion happens here
		for( String subdirectory : subdirectoriesList ) {
			recursivelyProcessDirectory( subdirectory, resultsList );
		}; // for
	}


	/** 
	 * Reviews results and when valid, adds them to the list of results
	 * @param  directory  directory containing results file
	 * @param  resultsList  the list of results
	 */
	public void reviewResults( String directory, List<String> resultsList ) {
		File resultFile = new File( directory + File.separator + App.RESULT_TXT );
		if( resultFile.exists() == false ) {
			return;
		}; // if
		List<String> linesList = Utilities.getLinesFromFile( resultFile );
		if( linesList == null ) {
			return;
		}; // if
		
		// process the text file
		Integer eggCountLine = null;
		String status = "not-inspected";
		int count = 0;
		for( String each : linesList ) {
			String[] items = each.split( "\t" );
			if( each.startsWith( "#" ) == true ) {
				if( App.EGG_COUNT.equals( items[ 0 ] ) == true ) {
					eggCountLine = Utilities.getInteger( items[ 1 ] );
				}; // if
				if( App.METHOD_MANUAL.equals( items[ 0 ] ) == true ) {
					if( "Done".equalsIgnoreCase( items[ 1 ] ) == true ) {
						status = "inspected";
					}; // if
				}; // if
				continue;
			}; // if
			if( items.length == 7 ) {
				WormInfo wormInfo = WormInfo.createWormInfo( items );
				if( wormInfo == null ) {
					// Skipping WormInfo line, unexpected format
				}
				else {
					count += wormInfo.nWorm;
				}; // if
			}
			else {
				// Skipping WormInfo line, unexpected format
			}; // if
		}; // for
		if( eggCountLine == null ) {
			//Unable to process data of number of eggs (egg count is undefined)
			return;
		}; // if
		if( count != eggCountLine.intValue() ) {
			// ERROR, egg count in  resultFile.getAbsolutePath() file mismatch with actual egg count!
			return;
		}; // if

		File assembled = new File( directory + File.separator + App.ASSEMBLED_COLORS_JPEG );
		if( assembled.exists() == true ) { 
			if( status.equals( "not-inspected" ) == true ) {
				status += "----assembled-colors-image-does-exist!";
			}; // if
		}
		else {
			if( status.equals( "inspected" ) == true ) {
				status += "----assembled-colors-image-is-missing!";
			}; // if
		}; // if
		resultsList.add( directory + "\t" + count + "\t" + status );
	}
	
} // ResultProcessor
