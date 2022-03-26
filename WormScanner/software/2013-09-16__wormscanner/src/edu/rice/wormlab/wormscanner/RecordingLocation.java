/*
 * Filename: RecordingLocation.java
 */

package edu.rice.wormlab.wormscanner;

import java.io.File;
import java.util.List;

/**
 * A Recording-Location is a line containing a position (index), well number, and x,y coordinates 
 * @author Aleman-Meza
 *
 */

public class RecordingLocation {

	/** the position, such as index in an array */
	public final Integer position;
	
	/** the well number; zero if unknown or not/applicable */
	public final Integer well;
	
	/** the x coordinate */
	public final Long xCoordinate;
	
	/** the y coordinate */
	public final Long yCoordinate;

	/** any error when creating the object; when things are ok it has null */
	public final String errors;
	
	/** constant for specifying the type of plate */
	public static final String TYPE_OF_PLATE = "#Type of plate:";
	
	/** Constructor 
	 * @param  text  a line of text with values separated by tab
	 */
	public RecordingLocation( String text ) {
		String[] parts = null;
		if( text != null ) {
			parts = text.split( "\t" );
		}; // if
		if( parts != null && parts.length == 3 ) {
			position = Utilities.getInteger( parts[ 0 ] );
			well = Utilities.getInteger( parts[ 1 ] );
			String[] coordinates = parts[ 2 ].split( "," );
			if( coordinates.length == 2 ) {
				xCoordinate = Utilities.getLong( coordinates[ 0 ] );
				yCoordinate = Utilities.getLong( coordinates[ 1 ] );
			}
			else {
				xCoordinate = null;
				yCoordinate = null;
			}
			if( position == null || well == null || xCoordinate == null || yCoordinate == null ) {
				errors = "Invalid value in line: " + text;
			}
			else {
				errors = null;
			}; // if
		}
		else {
			position = null;
			well = null;
			xCoordinate = null;
			yCoordinate = null;
			errors = "Invalid value in line: " + text;
		}
	}

	

	@Override
	public String toString() {
		return "[" + position + "] well #" + well + " x,y " + xCoordinate + " , " + yCoordinate;
	}


	/**
	 * Verifies that a recording-locations-profile is valid
	 * @param  file  the file to verify
	 * @return  null when things are ok; otherwise it returns an error message
	 */
	protected static String verifyLocationsProfileValidity( File file ) {
		return verifyLocationsProfileValidity( file, null, null );
	}

		
	/**
	 * Verifies that a recording-locations-profile is valid
	 * @param  file  the file to verify
	 * @param  list  the list where the read locations are placed into
	 * @param  typeOfPlateList  the list where the type of plate is set (if found in the text-file)
	 * @return  null when things are ok; otherwise it returns an error message
	 */
	protected static String verifyLocationsProfileValidity( File file, List<RecordingLocation> list, List<PlateEnum> typeOfPlateList ) {
		if( file == null ) {
			return "File is null";
		}; // if
		if( file.canRead() == false ) {
			return "Unable to read the file";
		}; // if
		List<String> linesList = Utilities.getLinesFromFile( file );
		if( linesList == null ) {
			return "There was error reading the file";
		}; // if
		if( linesList.size() == 0 ) {
			return "The file is empty!";
		}; // if
		int positionCounter = 0;
		for( String line : linesList ) {
			line = line.trim();
			if( line.startsWith( "#" ) == true ) {
				// see whether there is a line indicating type of plate
				if( line.startsWith( TYPE_OF_PLATE ) == true && typeOfPlateList != null ) {
					// see whether we can get the type of plate
					String[] parts = line.split( "\t" );
					if( parts.length == 2 ) {
						for( PlateEnum plate : PlateEnum.values() ) {
							if( plate.name().equals( parts[ 1 ] ) == true ) {
								typeOfPlateList.add( plate );
							}; // if
						}; // if
					}; // if
				}; // if
				// other lines starting with # are comments, skip them
				continue;
			}; // if
			if( "".equals( line ) == true ) {
				// skip empty lines
				continue;
			}; // if
			RecordingLocation rec = new RecordingLocation( line );
			if( rec.errors != null ) {
				return rec.errors;
			}; // if
			if( list != null ) {
				list.add( rec );
			}; // if
			if( rec.position != ( positionCounter + 1 ) ) {
				return "The recording-locations file is not correct (unexpected position number: " + rec.position + " )"; 
			}; // if
			positionCounter++;
		}; // for
		if( positionCounter == 0 ) {
			return "The file has no recording-locations";
		}; // if
		return null;
	}
	
	
} // class RecordingLocation

