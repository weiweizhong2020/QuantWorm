/*
 * Filename: GuiOptionsEnum.java
 */

package edu.rice.wormlab.wormscanner;

import java.awt.event.KeyEvent;


public enum GuiOptionsEnum {

	PICTURES( "Image Scanning", KeyEvent.VK_P ),
	VIDEO( "Video Recording", KeyEvent.VK_V ),
	PICTURES_AND_VIDEO( "Recording and Scanning", KeyEvent.VK_A );
	
	/** value to display in gui-components */
	public final String guiText;
	
	/** mnemonic to be used in gui-components */
	public final int mnemonic;
	
	GuiOptionsEnum( String guiText, int mnemonic ) {
		this.guiText = guiText;
		this.mnemonic = mnemonic;
	}
	
	
} // enum GuiOptionsEnum

