/*
 * Filename: AlignmentEffect.java 
 */

package edu.rice.wormlab.wormscanner;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

// the following class is imported from WormTracker 2, which was developed by Eviatar Yemini at the Schafer lab, http://www.mrc-lmb.cam.ac.uk/wormtracker/
import camera.effects.ImageEffect;

/**
 * A <code>CaliberationEffect</code> adds a caliberation grid to images
 * 
 * @author Boanerges Aleman-Meza
 *
 */


public class AlignmentEffect extends ImageEffect {

	/** The max x value */
	public static final int MAX_X = 640;

	/** The max y value */
	public static final int MAX_Y = 480;

	/**
	 * Default constructor
	 * @param	 priority  The priority for this special effect
	 */
	public AlignmentEffect( int priority ) {
		super( priority );
	}; // default constructor

	
	@Override
	public BufferedImage process( BufferedImage image ) {
		// Get the image graphics.
		Graphics graphics = image.getGraphics();
		if( graphics == null ) {
			return null;
		}; // if
		
		// draw the instructions
		Font font = new Font( null, Font.PLAIN, 24 );
		Point point = new Point( 35, 5 + font.getSize() );

		graphics.setColor( Color.BLUE );
		graphics.setFont( font );
		graphics.drawString( "Move the transparency template so that the", point.x, point.y );
		graphics.drawString( "cross fits the center shown in the screen.", point.x, point.y + font.getSize() + 10 );
		graphics.drawString( "Tape the tranparency to the stage.", point.x, point.y + ( font.getSize() + 10 ) * 2 );
		graphics.drawString( "When done, click 'Finish Alignment'.", point.x + 20, point.y + ( font.getSize() + 10 ) * 3 );

		// draw the guideline
		final int size = 18;
		final int padding = size / 2;
		final Point center = new Point( ScanningAndRecording.DEFAULT_CAMERA_X_RESOLUTION / 2, ScanningAndRecording.DEFAULT_CAMERA_Y_RESOLUTION / 2 );
		graphics.drawLine( 0, center.y, center.x - padding, center.y );
		graphics.drawLine( center.x + padding, center.y, ScanningAndRecording.DEFAULT_CAMERA_X_RESOLUTION - 1, center.y );
		graphics.drawLine( center.x, 0, center.x, center.y - padding );
		graphics.drawLine( center.x, center.y + padding, center.x, MAX_Y - 1 );
		graphics.drawOval( ( ScanningAndRecording.DEFAULT_CAMERA_X_RESOLUTION / 2 ) - ( size / 2 ), ( ScanningAndRecording.DEFAULT_CAMERA_Y_RESOLUTION / 2 ) - ( size / 2 ) , size, size );
		return image;
	}
	
} // class AlignmentEffect
