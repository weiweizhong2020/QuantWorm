/*
 * Filename: PlateEnum.java
 */

package edu.rice.wormlab.wormscanner;


public enum PlateEnum {

	SIX_CM( 1, "6 cm.", "6 cm.", 50500, 1 ),
	SIX_WELL( 6, "6-well", null, 40000, 3 ),
	TWELVE_WELL( 12, "12-well", null, 26000, 4 ),
	TWENTY4_WELL( 24, "24-well", null, 19480, 6 );
	
	/** constant of well-plate to capture cases of any x-well plate */
	public static final String WELL_PLATE = "Well Plate";
	
	// value to display in toString
	private final String text;
	
	// value associated to a card-layout (intended to be used in pause-card-layout-panel)
	private final String nameCardLayout;

	/** length to be scanned */
	public final int length;
	
	/** number of wells in the plate */
	public final int wellsTotal;
	
	/** number of wells in each row of the plate */
	public final int wellsInEachRow;

	/** number of wells in each column of the plate */
	public final int wellsInEachColumn;

	/**
	 * 'Constructor' for a enumeration
	 * @param  wellsTotal  total number of wells
	 * @param  text  human-readable text (used in toString)
	 * @param  nameCardLayout  convenience value to be used in a Card-Layout
	 * @param  length  length that needs to be covered in scanning a well
	 * @param  wellsInEachRow  number of wells in each row of the plate
	 */
	PlateEnum( int wellsTotal, String text, String nameCardLayout, int length, int wellsInEachRow ) {
		this.text = text;
		this.nameCardLayout = nameCardLayout;
		this.length = length;
		this.wellsTotal = wellsTotal;
		this.wellsInEachRow = wellsInEachRow;
		this.wellsInEachColumn = wellsTotal / wellsInEachRow;
	}
	
	
	
	/**
	 * Get the name associated to a value to be used as name in a card-layout (intended for pause-card-layout)
	 * @return  name of this value to be used in a card-layout
	 */
	public String getNameCardLayout() {
		if( nameCardLayout != null ) {
			return nameCardLayout;
		}; // if
		return WELL_PLATE;
	}


	@Override
	public String toString() {
		return text;
	}



	/**
	 * Testing purposes only
	 * @param  args  not used.
	 */
	public static void main( String[] args ) {
		for( PlateEnum plate : PlateEnum.values() ) {
			System.out.println( plate + "\t" + plate.getNameCardLayout() + "\t" + plate.ordinal() );
			for( int w = 1; w <= plate.wellsTotal; w++ ) {
				int xWell = w;
				while( xWell > plate.wellsInEachRow ) {
					xWell -= plate.wellsInEachRow;
				}
				System.out.println( w + "  x: " + xWell );
			}
		}
	}

} // enum PlateEnum

