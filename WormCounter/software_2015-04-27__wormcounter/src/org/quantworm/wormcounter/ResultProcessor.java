/*
 * Filename: ResultProcessor.java
 */

package org.quantworm.wormcounter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Reads App.RESULT_TXT and outputs avoidance index
 */

public class ResultProcessor{
	

	/**
	 * Recursively processes a directory and all its subdirectories
	 * @param  directory  the directory
	 */
	public void recursivelyProcessDirectory( String directory ) {
		List<String> resultsList = new ArrayList<String>();
		recursivelyProcessDirectory( directory, resultsList );
		// save the new contents
		try {
			FileWriter fileWriter = new FileWriter( directory + File.separator + "avoidance.txt" );
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
	 * Recursively processes a directory and all its subdirectories
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

		String error = avoidanceAssay( dir.getAbsolutePath(), resultsList );
		if( error == null ) {
			// Done with dir
		}
		else {
			if( subdirectoriesList.isEmpty() == true ) {
				// Skipping  dir.getAbsolutePath() 
			}
			else {
				// Skipping dir but will look into its subdirectories
			}; // if
		}; // if

		// recursion happens here
		for( String subdirectory : subdirectoriesList ) {
			recursivelyProcessDirectory( subdirectory, resultsList );
		}; // for
	}


    public String avoidanceAssay(String dirName, List<String> resultsList ){
		int[] component=null;
		double ai = -666;
    	//read file
		try{
			BufferedReader br=new BufferedReader (new FileReader (dirName + File.separator + App.RESULT_TXT));
			String ln=null;
			component=null;
			while ((ln=br.readLine())!=null){
				StringTokenizer st=new StringTokenizer(ln, "\t");
				if (ln.startsWith("# Component Count:")) {
					st.nextToken();
					String nComponent=st.nextToken();
					component=new int[new Integer(nComponent).intValue()];
					for (int i=0; i<component.length; i++) component[i]=0;
					continue;
				}
				if (ln.startsWith("#")) {
					continue;
				}
				
				if (component==null) return "avoidanceError internal error"; //shouldn't get here.
				st.nextToken();st.nextToken();st.nextToken();st.nextToken();
				String nWorm=st.nextToken(); st.nextToken();
				String label=st.nextToken();
				
				int i=new Integer(label).intValue(); 
				int worm=new Integer(nWorm).intValue();
				component[i-1]+=worm;				
			}
			
			double c1=(double)component[0]; 
			double c2=(double)component[1];
			ai=(c1-c2)/(c1+c2);
			System.out.println(dirName+"\t"+component[0]+"\t"+component[1]+"\t"+ai);
		}
		catch (IOException e){
			System.out.println (e);
			return e.toString();
		}
		File imgFile = new File( dirName + File.separator + "assembled_colors.jpeg" );
		if( imgFile.exists() ) {
			resultsList.add(dirName+"\t"+component[0]+"\t"+component[1]+"\t"+ai+"\tinspected");
		}
		else {
			resultsList.add(dirName+"\t"+component[0]+"\t"+component[1]+"\t"+ai+"\tnot-inspected");
		}; // if
		return null;
	}
}

