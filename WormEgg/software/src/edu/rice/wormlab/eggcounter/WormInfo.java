/*
 * Filename: WormInfo.java
 * This class defines data structure of Worm objects
 */

package edu.rice.wormlab.eggcounter;

/**
 * WormInfo keeps various data of a worm that is captured inside a photo
 */

public class WormInfo implements Comparable<WormInfo> {
    
	public int pX;
	public int pY;
	public int width;
	public int height;
	public int nWorm;
	public Integer changedN;
	public int probability;
	public String method;

	
	/** 
	 * Detailed constructor 
	 * @param  n  the number of worms
	 * @param  x  the x
	 * @param  y  the y
	 * @param  width  the width
	 * @param  height  the height
	 * @param  probability
	 * @param  method
	 */
	public WormInfo( int n, int x, int y, int width, int height, int probability, String method ) {
		this.nWorm = n;
		this.pX = x;
		this.pY = y;
		this.width = width;
		this.height = height;
		this.changedN = null;
		this.probability = probability;
		this.method = method;
	}


	/**
	 * factory method for creating a WormInfo object from a string array
	 * @param  items  the string array
	 * @return  a WormInfo object, or null if something went wrong
	 */
	public static WormInfo createWormInfo( String[] items ) {
		if( items == null || items.length != 7 ) {
			// ERROR, WormInfo::createWormInfo, items is the problem.
			return null;
		}; // if
		Integer x = Utilities.getInteger( items[ 0 ] );
		Integer y = Utilities.getInteger( items[ 1 ] );
		Integer width = Utilities.getInteger( items[ 2 ] );
		Integer height = Utilities.getInteger( items[ 3 ] );
		Integer n = Utilities.getInteger( items[ 4 ] );
		Integer p = Utilities.getInteger( items[ 5 ] );
		if( n == null || x == null || y == null || width == null 
		|| height == null ) {
			return null;
		}; // if
		return new WormInfo( n, x, y, width, height, p, items[ 6 ] );
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo( WormInfo other ) {
		if( other == null ) {
			return -1;
		}; // if
		if( nWorm > other.nWorm ) {
			return -1;
		}; // if
		if( nWorm < other.nWorm ) {
			return 1;
		}; // if
		int area = width * height;
		int areaOther = other.width * other.height;
		if( area > areaOther ) {
			return -1;
		}; // if
		if( area < areaOther ) {
			return 1;
		}; // if
		int ret = toString().compareTo( other.toString() );
		if( ret != 0 ) {
			return ret;
		}; // if
		// WormInfo objects are the same! 
		return 0;
	}
	

	/**
	 * Returns whether this worm is deleted
	 * @return  true when deleted; false otherwise
	 */
	public boolean isDeleted() {
		if( changedN == null ) {
			return nWorm == 0;
		}; // if
		return changedN == 0;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		return toString().endsWith( arg0.toString() );
	}


	@Override
	public String toString() {
		return pX + "\t" + pY + "\t" + width + "\t" + height + "\t" + nWorm + "\t" + probability + "\t" + method;
	}


} // class WormInfo

