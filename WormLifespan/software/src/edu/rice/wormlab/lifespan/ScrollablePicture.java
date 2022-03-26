/* 
 * Filename: ScrollablePicture.java
 */
package edu.rice.wormlab.lifespan;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * A JLabel made to work as scrollable. 
 * This code was modified from Java's tutorial on "How to use scroll panes"
 */

public class ScrollablePicture extends JLabel implements Scrollable {

	// serial version-id
	private static final long serialVersionUID = 1L;

	// the max unit increment
	private final int maxUnitIncrement = Toolkit.getDefaultToolkit().getScreenResolution();

	/** base image */
	public final ImageIcon baseImageIcon;

	/** colors image */
	public final ImageIcon colorsImageIcon;


	/** 
	* Constructor
	*/
	public ScrollablePicture( ImageIcon imageIcon1, ImageIcon imageIcon2, MouseListener mouseListener ) {
		super( imageIcon1 );
		this.baseImageIcon = imageIcon1;
		this.colorsImageIcon = imageIcon2;
		if( imageIcon1 == null || imageIcon2 == null ) {
			setText( "No picture found." );
			setHorizontalAlignment( CENTER );
			setOpaque( true );
			setBackground( Color.white );
			return;
		}; // if

		setAutoscrolls( true ); 
		addMouseListener( mouseListener );
		// the worker swaps images as a background task
		SwingWorker<Void,Void> worker = new SwingWorker<Void,Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				do {
					int sleepTime = 200;
					if( baseImageIcon == null ) {
						return null;
					}; // if
					if( baseImageIcon.equals( getIcon() ) == true ) {
						setIcon( colorsImageIcon );
						sleepTime = 800;
					}
					else {
						setIcon( baseImageIcon );
					}; // if
					try {
						Thread.sleep( sleepTime );
					}
					catch( InterruptedException ignore ) {
						// do nothing
					}; // try
				} while( getText() == null );
				return null;
			}
		};
		worker.execute();
	}


	/* (non-Javadoc)
	 * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
	 */
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}


	/* (non-Javadoc)
	 * @see javax.swing.Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
	 */
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


	/* (non-Javadoc)
	 * @see javax.swing.Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
	 */
	public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
		if( orientation == SwingConstants.HORIZONTAL ) {
			return visibleRect.width - maxUnitIncrement;
		}; // if 
		return visibleRect.height - maxUnitIncrement;
	}

	/* (non-Javadoc)
	 * @see javax.swing.Scrollable#getScrollableTracksViewportWidth()
	 */
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}


	/* (non-Javadoc)
	 * @see javax.swing.Scrollable#getScrollableTracksViewportHeight()
	 */
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

} // class ScrollablePicture

