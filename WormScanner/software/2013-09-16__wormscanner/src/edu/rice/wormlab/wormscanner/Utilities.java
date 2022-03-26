/**
 * Filename: Utilities.java 
 */

package edu.rice.wormlab.wormscanner;

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
import java.util.List;


public class Utilities {

	protected static final NumberFormat formatter2 = new DecimalFormat( "#0.##" );


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
	 * Gets the long value out of a string
	 * @param  str  the string
	 * @return  the long as object; null if unable to convert to Long
	 */
	public static Long getLong( String str ) {
		if( str == null ) {
			return null;
		}; // if
		Long ret = null;
		try {
			ret = new Long( str );
		}
		catch( Exception e ) {
			return null;
		}; // try
		return ret;
	}

	
	/**
	 * Gets the double value out of a string
	 * @param  str  the string
	 * @return  the double as object; null if unable to convert it to double
	 */
	public static Double getDouble( String str ) {
		if( str == null ) {
			return null;
		}; // if
		Double ret = null;
		try {
			ret = new Double( str );
		}
		catch( Exception e ) {
			return null;
		}; // try
		return ret;
	}
	  
    
    /**
     * Formats a double value with maximum two decimals
     * @param  value  the vlaue
     * @return  the string; when value is null then the string returned is NULL
     */
    public static String format2( Double value ) {
    	if( value == null ) {
    		return "NULL";
    	}; // if
    	return formatter2.format( value );
    }
    
    
    /**
     * Returns a word for the number given, if 1 it returns first, if 2 it returns second.
     * @param  i  the number value
     * @return  the word for the number, such as first and second.
     */
    public static String intWord( int i ) {
    	if( i == 1 ) {
    		return "first";
    	}; // if
    	if( i == 2 ) {
    		return "second";
    	}; // if
    	return i + "";
    }

    
    /**
     * Appends lines of text into text file, if it exists then it just appends lines but it does not overwrite it
     * @param  file  the file
     * @param  lines  the lines of text
     * @return  null when things go OK; otherwise it returns an error message
     */
    public static String appendLinesToTextFile( File file, String[] lines ) {
    	return writeLinesToTextFile( file, lines, true );
    }
    
    
    /**
     * Writes lines of text into a new text file (if file already exists, it overwrites it)  
     * @param  file  the file
     * @param  lines  the lines of text
     * @return  null when things go OK; otherwise it returns an error message
     */
    public static String writeLinesToNewTextFile( File file, String[] lines ) {
    	return writeLinesToTextFile( file, lines, false );
    }
    
    /**
     * Writes lines of text into text file, if it exists then over-writting it depends upon append-flag
     * @param  file  the file
     * @param  lines  the lines of text
     * @param  appendFlag  when true, file is not over-written, lines are appended;
     *                     when false, file is over-written and lines are written to file
     * @return  null when things go OK; otherwise it returns an error message
     */
    public static String writeLinesToTextFile( File file, String[] lines, boolean appendFlag ) {
    	String operation = appendFlag == true ? "append" : "write";
    	if( file == null ) {
    		return "Unable to " + operation + " lines to text file because it is null.";
    	}; // if
    	if( lines == null ) {
    		return "Unable to " + operation + " anything to text file because the text is null.";
    	}; // if
    	try {
    		FileWriter fileWriter = new FileWriter( file, appendFlag );
    		BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
    		PrintWriter printWriter = new PrintWriter( bufferedWriter );
    		for( String each : lines ) {
    			printWriter.println( each );
    		}; // for
    		printWriter.close();
    	}
    	catch( IOException e ) {
    		return "Problems when trying to " + operation + " lines to the text file.\nDetails: " + e.getMessage();
    	}; // try
    	return null;
    }
    
} // class Utilities
