/* 
 * Filename: ScrollablePicture.java
 */
package edu.rice.wormlab.eggcounter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * A JLabel made to work as scrollable.
 * This code was modified from Java's tutorial on "How to use scroll panes"
 */

public class ScrollablePicture extends JLabel implements Scrollable, MouseMotionListener {

	// the max unit increment
	private final int maxUnitIncrement = Toolkit.getDefaultToolkit().getScreenResolution();

	/** colors image */
	public final ImageIcon colorsImageIcon;


	/** 
	 * Constructor
	 */
	public ScrollablePicture( ImageIcon imageIcon, MouseListener mouseListener ) {
		super( imageIcon );
		this.colorsImageIcon = imageIcon;
		if( imageIcon == null ) {
			setText( "No picture found." );
			setHorizontalAlignment( CENTER );
			setOpaque( true );
			setBackground( Color.white );
			return;
		}; // if

		setAutoscrolls( true ); 
		addMouseListener( mouseListener );
		addMouseMotionListener( this );
	}


	// required for Scrollable
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}


	// required for Scrollable
	public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction ) {
		// Get the current position.
		int currentPosition = 0;
		if( orientation == SwingConstants.HORIZONTAL ) {
			currentPosition = visibleRect.x;
		} 
		else {
			currentPosition = visibleRect.y;
		}; // if
		// Return the number of pixels between currentPosition
		// and the nearest tick mark in the indicated direction.
		if( direction < 0 ) {
			int newPosition = currentPosition - (currentPosition / maxUnitIncrement) * maxUnitIncrement;
			return ( newPosition == 0 ) ? maxUnitIncrement : newPosition;
		}; // if
		return ( ( currentPosition / maxUnitIncrement ) + 1) * maxUnitIncrement - currentPosition;
	}


	// required for Scrollable
	public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
		if( orientation == SwingConstants.HORIZONTAL ) {
			return visibleRect.width - maxUnitIncrement;
		}; // if 
		return visibleRect.height - maxUnitIncrement;
	}

	// required for Scrollable
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}


	// required for Scrollable
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	// required for MouseMotionListener
	public void mouseDragged(MouseEvent e) {
		Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
		scrollRectToVisible(r);
	}

	// required for MouseMotionListener
	public void mouseMoved( MouseEvent e ) { }

} // class ScrollablePicture

