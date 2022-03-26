/*
 * Filename: WormInfo.java
 */
package org.quantworm.wormcounter;

/**
 * WormInfo keeps various data of a worm that is captured inside a photo
 */
public class WormInfo {

    public int nWorm;
    public int pX;
    public int pY;
    public int width;
    public int height;
    public int area;
    public int label;
    public Integer changedN;

    /**
     * Default constructor
     */
    public WormInfo() {
        this.nWorm = 0;
        this.pX = 0;
        this.pY = 0;
        this.width = 0;
        this.height = 0;
        this.area = 0;
        this.label = 0;
        this.changedN = null;
    }


	/** 
	 * Detailed constructor 
	 * @param  n  the number of worms
	 * @param  x  the x
	 * @param  y  the y
	 * @param  width  the width
	 * @param  height  the height
	 * @param  area  the area
	 * @param  label  the label (component)
	 */
	public WormInfo(int n, int x, int y, int width, int height, int area, int label) {
        this.nWorm = n;
        this.pX = x;
        this.pY = y;
        this.width = width;
        this.height = height;
        this.area = area;
        this.label = label;
        this.changedN = null;
    }



	/**
	 * factory method for creating a WormInfo object from a string array
	 * @param  items  the string array
	 * @return  a WormInfo object, or null if something went wrong
	 */
	public static WormInfo createWormInfo(String[] items) {
        if (items == null || items.length != 7) {
            return null;
        }; // if
        Integer n = getInteger(items[ 4]);
        Integer x = getInteger(items[ 0]);
        Integer y = getInteger(items[ 1]);
        Integer width = getInteger(items[ 2]);
        Integer height = getInteger(items[ 3]);
        Integer area = getInteger(items[ 5]);
        Integer label = getInteger(items[ 6]);
        if (n == null || x == null || y == null || width == null
                || height == null || area == null || label == null) {
            return null;
        }; // if
        return new WormInfo(n, x, y, width, height, area, label);
    }



	/**
	 * Gets the int value out of a string
	 * @param  str  the string
	 * @return  the integer as object; null if unable to convert to integer
	 */
	public static Integer getInteger(String str) {
        if (str == null) {
            return null;
        }; // if
        Integer ret = null;
        try {
            ret = new Integer(str);
        } catch (Exception e) {
            return null;
        }; // try
        return ret;
    }


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
        return hashCode() + "\t" + pX + "\t" + pY + "\t" + width + "\t" + height + "\t" + nWorm + "\tarea: " + area + "\t" + label;
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

} // class WormInfo

