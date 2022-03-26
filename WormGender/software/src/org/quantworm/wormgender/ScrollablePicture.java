/* 
 * Filename: ScrollablePicture.java
 */
package org.quantworm.wormgender;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * A JLabel made to work as scrollable. This code was modified from Java's
 * tutorial on "How to use scroll panes"
 */
public class ScrollablePicture extends JLabel implements Scrollable {

    // serial version-id
    private static final long serialVersionUID = 1L;

    // the max unit increment
    private final int maxUnitIncrement = Toolkit.getDefaultToolkit().getScreenResolution();

    /**
     * colors image
     */
    public final ImageIcon colorImageIcon;

    /**
     * Constructor
     *
     * @param colorImageIcon
     * @param mouseListener
     */
    public ScrollablePicture(ImageIcon colorImageIcon, MouseListener mouseListener) {
        super(colorImageIcon);
        this.colorImageIcon = colorImageIcon;
        if (colorImageIcon == null) {
            setText("No picture found.");
            setHorizontalAlignment(CENTER);
            setOpaque(true);
            setBackground(Color.white);
            return;
        }

        setAutoscrolls(true);
        addMouseListener(mouseListener);
    }


    /* (non-Javadoc)
     * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
     */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }


    /* (non-Javadoc)
     * @see javax.swing.Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
     */
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // Get the current position.
        int currentPosition;
        if (orientation == SwingConstants.HORIZONTAL) {
            currentPosition = visibleRect.x;
        } else {
            currentPosition = visibleRect.y;
        }
		// Return the number of pixels between currentPosition
        // and the nearest tick mark in the indicated direction.
        if (direction < 0) {
            int newPosition = currentPosition - (currentPosition / maxUnitIncrement) * maxUnitIncrement;
            return (newPosition == 0) ? maxUnitIncrement : newPosition;
        }
        return ((currentPosition / maxUnitIncrement) + 1) * maxUnitIncrement - currentPosition;
    }


    /* (non-Javadoc)
     * @see javax.swing.Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
     */
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width - maxUnitIncrement;
        }
        return visibleRect.height - maxUnitIncrement;
    }

    /* (non-Javadoc)
     * @see javax.swing.Scrollable#getScrollableTracksViewportWidth()
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }


    /* (non-Javadoc)
     * @see javax.swing.Scrollable#getScrollableTracksViewportHeight()
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

} // class ScrollablePicture

