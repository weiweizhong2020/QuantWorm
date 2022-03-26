/*
 * Filename: MicroscopeEnum.java
 */

package edu.rice.wormlab.wormscanner;


public enum MicroscopeEnum {

	MICROSCOPE_08X( "0.8x", "worm_08x.cfg" ),
	MICROSCOPE_2X( "2x", "worm_2x.cfg" ),
	MICROSCOPE_3X( "3x", "worm_3x.cfg" ),
	MICROSCOPE_4X( "4x", "worm_4x.cfg" );
	
	/** value to display in gui-components */
	public final String guiText;
	
	/** name of the configuration profile */
	public final String configurationProfile;
	
	/**
	 * 'constructor' of the microscope enum
	 * @param  guiText  text to display in GUI
	 * @param  configurationProfile  name of configuration profile
	 */
	MicroscopeEnum( String guiText, String configurationProfile ) {
		this.guiText = guiText;
		this.configurationProfile = configurationProfile;
	}
	
	
} // enum MicroscopeEnum

