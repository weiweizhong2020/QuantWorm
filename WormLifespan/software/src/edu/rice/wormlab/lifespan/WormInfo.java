/* 
 * Filename: WormInfo.java
 * This class defines data structure of WormInfo
 */

package edu.rice.wormlab.lifespan;


public class WormInfo implements Comparable<WormInfo> {
	public int nLive;
	public int pX;
	public int pY;
	public int width;
	public int height;
	public boolean deleted;
	public int label;
	public boolean firstView;
	public boolean isWormFoundInDiffImage = false;

	/** constant for header in text-file */
	public static final String HEADER = "#nLive\tX\tY\tWidth\tHeight";

	/** Default constructor */
	public WormInfo() {
		this.nLive = 0;
		this.pX = 0;
		this.pY = 0;
		this.width = 0;
		this.height = 0;
		this.deleted = false;
		this.label = -1;
		this.firstView = true;
		this.isWormFoundInDiffImage = false;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo( WormInfo other ) {
		if( other == null ) {
			return -1;
		}
                
		if( nLive > other.nLive ) {
			return -1;
		}
		if( nLive < other.nLive ) {
			return 1;
		}
		int area = width * height;
		int areaOther = other.width * other.height;
		if( area > areaOther ) {
			return -1;
                }
		if( area < areaOther ) {
			return 1;
		}
		int ret = toString().compareTo( other.toString() );
		if( ret != 0 ) {
			return ret;
		}
		System.out.println( "WormInfo objects are the same! " );
		return 0;
	}; // compareTo

	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		if( arg0 instanceof WormInfo ) {
			WormInfo other = (WormInfo) arg0;
			return compareTo( other ) == 0;
		}
		return toString().equals( arg0.toString() );
	}; // if

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return nLive + "\t" + pX + "\t" + pY + "\t" + width + "\t" + height;
	}

}

