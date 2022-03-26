/* 
 * Filename: PlateView.java
 */
package edu.rice.wormlab.lifespan;

import org.apache.commons.math.stat.StatUtils;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JViewport;


/**
 * Handles plate-view actions
 * @author  Boanerges Aleman-Meza
 */

public class PlateView implements MouseListener, WindowListener {

	/** size of square window (default) */
	public static final int SQUARE_SIZE = 640;

	/** pixels width of stroke */
	public static final int STROKE_WIDTH = 6;

	/** extra pixels for bounding box */
	public static final int EXTRA_PIXELS = 6;

	/** number of zoom levels */
	public static final int ZOOM_LEVELS = 3;

	/** constant for save-and-close */
	public static final String SAVE_AND_CLOSE = "Save & Close (Alt+C)";

	/** constant for page-view */
	public static final String PAGE_VIEW = "Page View (Alt+V)";

	/** folder where results are to be written */
	public final String resultsFolder;

	/** the list of worms from page-view */
	public final List<WormInfo> wormsList;

	/** scroll pane */
	private final JScrollPane pictureScrollPane;

	// the main component is the picture holder (a JLabel)
	private ScrollablePicture scrollablePicture;

	// base image
	private ImageIcon[] baseImageIcon = new ImageIcon[ ZOOM_LEVELS ];

	// colors image
	private ImageIcon[] colorsImageIcon = new ImageIcon[ ZOOM_LEVELS ];

	// non-annotated image-plus (already a color-image)
	private final ImagePlus colorsImagePlus;

	// base image-plus
	private final ImagePlus baseImagePlus;

	// any errors, or null when no errors detected,
	// also utilized to signal specific messages to the dialog-window that called plate-view
	private String errors = null;

	// the dialog window
	private JDialog dialog;

	// the popup menu
	private JPopupMenu popupMenu = null;

	// remember the x coordinate where popup shows
	private int xPopup = 0;

	// remember the x coordinate where popup shows
	private int yPopup = 0;

	// remember current zoom
	private int zoom;


	/**
	 * Constructor
	 * @param  assembledFilename  file name for 'base' assembled image (no annotations)
	 * @param  imagePlus  the 'colors' annotated image
	 * @param  wormsList  the list of worm objects
	 * @param  dimension  the size of this component
	 * @param  parentFrame  the parent frame
	 * @param  title  the title for the dialog-window
	 * @param  resultsFolder  folder where results will be eventually written
	 */
	public PlateView( String assembledFilename, ImagePlus imagePlus, List<WormInfo> wormsList, Dimension dimension, JFrame parentFrame, String title, String resultsFolder ) {
		this.wormsList = wormsList;
		this.resultsFolder = resultsFolder;
		this.colorsImagePlus = imagePlus;
		File file = new File( assembledFilename );
		if( file.exists() == false ) {
			errors = "File does not exist!\nLocation: " + file.getAbsolutePath();
			System.out.println( errors );
			pictureScrollPane = null;
			this.baseImagePlus = null;
			return;
		}; // if
		this.baseImagePlus = new ImagePlus( assembledFilename );

		// zoom index zero is 100%
		baseImageIcon[ 0 ] = new ImageIcon( file.getAbsolutePath() );
		ImagePlus annotatedImagePlus = updateImageAnnotations( colorsImagePlus.duplicate() );
		colorsImageIcon[ 0 ] = new ImageIcon( annotatedImagePlus.getBufferedImage() );

		// make the 50% image
		ImageProcessor imageProcessor = baseImagePlus.getProcessor().resize( baseImagePlus.getWidth() / 2 );
		baseImageIcon[ 1 ] = new ImageIcon( imageProcessor.getBufferedImage() );
		ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize( annotatedImagePlus.getWidth() / 2 );
		updateImageWithWormCount( scaledImageProcessor, 0.5 );
		colorsImageIcon[ 1 ] = new ImageIcon( scaledImageProcessor.getBufferedImage() );

		// make the 25% image
		imageProcessor = baseImagePlus.getProcessor().resize( baseImagePlus.getWidth() / 4 );
		baseImageIcon[ 2 ] = new ImageIcon( imageProcessor.getBufferedImage() );
		scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize( annotatedImagePlus.getWidth() / 4 );
		updateImageWithWormCount( scaledImageProcessor, 0.25 );
		colorsImageIcon[ 2 ] = new ImageIcon( scaledImageProcessor.getBufferedImage() );

		// default zoom index is set to 50% 
		zoom = 1;

		// setup the scrollable-image (it is actually a JLabel)
		scrollablePicture = new ScrollablePicture( new ImageIcon( baseImageIcon[ zoom ].getImage() ), new ImageIcon( colorsImageIcon[ zoom ].getImage() ), this );

		// Set up the scroll pane.
		pictureScrollPane = new JScrollPane( scrollablePicture );

		// the size of the picture-scroll-pane is set based on the dimension parameter (if available)
		Dimension tmpDimension = null;
		if( dimension != null ) {
			tmpDimension = new Dimension( dimension.width - 150, dimension.height - 50 );
		}
		else {
			tmpDimension = new Dimension( SQUARE_SIZE, SQUARE_SIZE );
		}; // if
		// see whether the screen-dimension is big so that we use most of it
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenDimension = toolkit.getScreenSize();
		if( ( screenDimension.getWidth() - 240 ) > tmpDimension.getWidth() && ( screenDimension.getHeight() - 200 ) > tmpDimension.getHeight() ) {
			tmpDimension.setSize( screenDimension.getWidth() - 240, screenDimension.getHeight() - 200 );
		}; // if
		pictureScrollPane.setPreferredSize( tmpDimension );
		pictureScrollPane.setViewportBorder( BorderFactory.createLineBorder( Color.black ) );

		// create the dialog-window
		dialog = new JDialog( parentFrame, title, true );
		dialog.setDefaultCloseOperation( JDialog.DO_NOTHING_ON_CLOSE );
		dialog.addWindowListener( this );
		JPanel panel = new JPanel( new GridBagLayout() );
		panel.setOpaque( true );

		JPanel operationPanel = new JPanel( new GridBagLayout() );

		// set up save button
		JButton saveButton = new JButton( "Save (Alt+S)" );
		saveButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent actionEvent ) {
				saveInspectionResults();
			}
		} );
		saveButton.setMnemonic( KeyEvent.VK_S );

		// set up save-and-close button
		JButton saveCloseButton = new JButton( SAVE_AND_CLOSE );
		saveCloseButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent actionEvent ) {
				saveAndClose();
			}
		} );
		saveCloseButton.setMnemonic( KeyEvent.VK_C );

		// set up page-view button
		JButton pageViewButton = new JButton( "Page View (Alt+V)" );
		pageViewButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent actionEvent ) {
				errors = PAGE_VIEW;  // hack to signal that page-view should re-generate
				close();
			}
		} );
		pageViewButton.setMnemonic( KeyEvent.VK_V );

		// setup the zoom-components
		JLabel zoomLabel = new JLabel( "Zoom:" );
		JRadioButton actualRadioButton = new JRadioButton( "100%" );
		actualRadioButton.setMnemonic( KeyEvent.VK_1 );
		actualRadioButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent actionEvent ) {
				doZoom( 0 );
			}
		} );
		JRadioButton halfRadioButton = new JRadioButton( "50%" );
		halfRadioButton.setMnemonic( KeyEvent.VK_5 );
		halfRadioButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent actionEvent ) {
				doZoom( 1 );
			}
		} );
		JRadioButton quarterRadioButton = new JRadioButton( "25%" );
		quarterRadioButton.setMnemonic( KeyEvent.VK_2 );
		quarterRadioButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent actionEvent ) {
				doZoom( 2 );
			}
		} );
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add( actualRadioButton );
		buttonGroup.add( halfRadioButton );
		buttonGroup.add( quarterRadioButton );
		halfRadioButton.setSelected( true );

		operationPanel.add( saveButton, new GBC( 0, 0 ).setInsets( 5, 5, 5, 5 ).setFill( GBC.HORIZONTAL ).setAnchor( GBC.WEST ) );
		operationPanel.add( saveCloseButton, new GBC( 0, 1 ).setInsets( 5, 5, 5, 5 ).setFill( GBC.HORIZONTAL ).setAnchor( GBC.WEST ) );
		operationPanel.add( pageViewButton, new GBC( 0, 2 ).setInsets( 5, 5, 5, 5 ).setFill( GBC.HORIZONTAL ).setAnchor( GBC.CENTER ) );
		operationPanel.add( zoomLabel, new GBC( 0, 3 ).setInsets( 25, 5, 5, 5 ).setFill( GBC.HORIZONTAL ).setAnchor( GBC.CENTER ) );
		operationPanel.add( actualRadioButton, new GBC( 0, 4 ).setInsets( 5, 5, 5, 5 ).setFill( GBC.HORIZONTAL ).setAnchor( GBC.CENTER ) );
		operationPanel.add( halfRadioButton, new GBC( 0, 5 ).setInsets( 5, 5, 5, 5 ).setFill( GBC.HORIZONTAL ).setAnchor( GBC.CENTER ) );
		operationPanel.add( quarterRadioButton, new GBC( 0, 6 ).setInsets( 5, 5, 5, 5 ).setFill( GBC.HORIZONTAL ).setAnchor( GBC.CENTER ) );

		panel.add( pictureScrollPane, new GBC( 0, 0 ).setInsets( 5, 5, 5, 5 ).setFill( GBC.HORIZONTAL ).setAnchor( GBC.WEST ) );
		panel.add( operationPanel, new GBC( 1, 0 ).setInsets( 5, 5, 5, 5 ).setFill( GBC.HORIZONTAL ).setAnchor( GBC.CENTER ) );
		dialog.add( panel );
		pictureScrollPane.addMouseListener( this );

		// setup popup menu
		popupMenu = new JPopupMenu();
		popupMenu.setBorderPainted( true );
		for( int i = 0; i < 7; i++ ) {
			JMenuItem menuItem = new JMenuItem( i + " moving" );
			menuItem.setActionCommand( "" + i );
			menuItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent actionEvent ) {
					popupMenuItemActionPerformed( actionEvent );
				}
			} );
			popupMenu.add( menuItem );
			if( i == 0 ) {
				popupMenu.addSeparator();
			}; // if
		}; // for

		dialog.pack();
	}


	/**
	 * Shows the dialog-window
	 */
	public void show() {
		dialog.setVisible( true );
	}


	/**
	 * Closes the dialog-window (if possible)
	 */
	public void close() {
		// finishes the worker thread
		scrollablePicture.setText( "" );
		dialog.setVisible( false );
	}

	/**
	 * Does zooming as specified
	 * @param  value  the zoom index value
	 */
	public void doZoom( int value ) {
		if( zoom == value ) {
			// nothing to do
			return;
		}; // if
		if( value < 0 || value >= ZOOM_LEVELS ) {
			// incorrect value, do nothing
			return;
		}; // if

		Point topleftPoint = pictureScrollPane.getViewport().getViewPosition();
		double xcorner = topleftPoint.getX();
		double ycorner = topleftPoint.getY();
		Dimension visibleDimension = pictureScrollPane.getViewport().getExtentSize();
		// figure out the center-point
		double xcenter = xcorner + visibleDimension.getWidth() / 2;
		double ycenter = ycorner + visibleDimension.getHeight() / 2;
		Point updatePoint = null;
		int previous = zoom;
		zoom = value;
//TODO
		// annotate the colors-image
		ImagePlus annotatedImagePlus = updateImageAnnotations( colorsImagePlus.duplicate() );
		if( zoom == 0 ) {
			colorsImageIcon[ 0 ].setImage( annotatedImagePlus.getBufferedImage() );
		}; // if
		// 50% image
		if( zoom == 1 ) {
			ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize( annotatedImagePlus.getWidth() / 2 );
			updateImageWithWormCount( scaledImageProcessor, 0.5 );
			colorsImageIcon[ 1 ] = new ImageIcon( scaledImageProcessor.getBufferedImage() );
		}; // if
		// 25% image
		if( zoom == 2 ) {
			ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize( annotatedImagePlus.getWidth() / 4 );
			updateImageWithWormCount( scaledImageProcessor, 0.25 );
			colorsImageIcon[ 2 ] = new ImageIcon( scaledImageProcessor.getBufferedImage() );
		}; // if

		scrollablePicture.colorsImageIcon.setImage( colorsImageIcon[ zoom ].getImage() );
		scrollablePicture.baseImageIcon.setImage( baseImageIcon[ zoom ].getImage() );

		Dimension dimension = pictureScrollPane.getViewport().getViewSize();
		if( dimension.getWidth() > 0 && dimension.getHeight() > 0 ) {
			updatePoint = new Point();
			// give appereance of keeping center when zooming in and out
			if( previous == 2 ) {
				// change from 25% to 50%
				double pointx = ( baseImageIcon[ zoom ].getIconWidth() - visibleDimension.getWidth() ) / 2;
				double pointy = ( baseImageIcon[ zoom ].getIconHeight() - visibleDimension.getHeight() ) / 2;
				updatePoint.setLocation( pointx, pointy );
			}; // if
			if( previous == 1 && zoom == 0 ) {
				// center is basically twice the size
				double x2center = xcenter * 2;
				double y2center = ycenter * 2;
				// new corner is center minus size of viewable area
				updatePoint.setLocation( x2center - visibleDimension.getWidth() / 2, y2center - visibleDimension.getHeight() / 2 );
			}; // if
			if( previous == 0 && zoom == 1 ) {
				// center is basically half the size
				double x2center = xcenter / 2;
				double y2center = ycenter / 2;
				// new corner is center minus size of viewable area
				updatePoint.setLocation( x2center - visibleDimension.getWidth() / 2, y2center - visibleDimension.getHeight() / 2 );
			}; // if
		}; // if
		if( updatePoint != null ) {
			JViewport viewport = pictureScrollPane.getViewport();
			viewport.setViewPosition( updatePoint );
			pictureScrollPane.setViewport( viewport );
		}; // if
		scrollablePicture.revalidate();
		pictureScrollPane.repaint();
	}


	/**
	 * Save inspection results
	 */
	public void saveInspectionResults() {
		MotionDetection.saveInspectionResults( true, dialog, wormsList, resultsFolder, colorsImagePlus );
	}


	/**
	 * Save inspection results, and close
	 */
	public void saveAndClose() {
		errors = SAVE_AND_CLOSE;  // hack to signal that page-view should close too
		saveInspectionResults();
		close();
	}


	/**
	 * Updates the image-plus with annotations from the worms-list
	 * @return  the annotated image
	 */
	public ImagePlus updateImageAnnotations( ImagePlus imagePlus ) {
		// annotate the colors-image
		ColorProcessor colorProcessor = (ColorProcessor) imagePlus.getProcessor();
		for( WormInfo worm : wormsList ) {
			// first draw the bounding box
			Roi roi = new Roi( worm.pX - STROKE_WIDTH, worm.pY - STROKE_WIDTH, worm.width + STROKE_WIDTH + EXTRA_PIXELS, worm.height + STROKE_WIDTH + EXTRA_PIXELS );
			colorProcessor.setRoi( roi );
			if( worm.deleted == true ) {
				roi.setStrokeWidth( STROKE_WIDTH / 2 );
				colorProcessor.setValue( Color.RED.getRGB() );
			}
			else {
				roi.setStrokeWidth( STROKE_WIDTH );
				colorProcessor.setValue( Color.BLUE.getRGB() );
			}; // if
			roi.drawPixels( colorProcessor );
			// deleted items are marked with a cross
			if( worm.deleted == true ) {
				Line line = new Line( worm.pX - STROKE_WIDTH, worm.pY - STROKE_WIDTH, worm.pX + worm.width + STROKE_WIDTH / 2, worm.pY + worm.height + STROKE_WIDTH / 2 );
				colorProcessor.setRoi( line );
				colorProcessor.setValue( Color.RED.getRGB() );
				line.setStrokeWidth( STROKE_WIDTH / 2 );
				line.drawPixels( colorProcessor );
				line = new Line( worm.pX - STROKE_WIDTH / 2, worm.pY + STROKE_WIDTH / 2 + worm.height, worm.pX + worm.width + STROKE_WIDTH / 2, worm.pY - STROKE_WIDTH / 2 );
				colorProcessor.setRoi( line );
				line.setStrokeWidth( STROKE_WIDTH / 2 );
				line.drawPixels( colorProcessor );
			}; // if
			if( worm.nLive > 1 ) {
				int y = worm.pY - STROKE_WIDTH - 2;
				colorProcessor.setValue( Color.BLUE.getRGB() );
				colorProcessor.drawString( "" + worm.nLive, worm.pX - STROKE_WIDTH - 2, y, Color.YELLOW );
			}; // if
		}; // for

		return imagePlus;
	}


	/**
	 * Updates a color-processor with text-labels of number of worms inside a bounding box 
	 * @param  factor  the factor to use in the coordinate system, 50% zoom factor is 0.5
	 */
	public void updateImageWithWormCount( ColorProcessor colorProcessor, double factor ) {
		for( WormInfo worm : wormsList ) {
			if( worm.nLive > 1 ) {
				int y = Math.round( Math.round( ( worm.pY - STROKE_WIDTH - 2 ) * factor ) );
				int x = Math.round( Math.round( ( worm.pX - STROKE_WIDTH - 2 ) * factor ) );
				colorProcessor.setValue( Color.BLUE.getRGB() );
				colorProcessor.drawString( "" + worm.nLive, x, y, Color.YELLOW );
			}; // if
		}; // for
	}

	/**
	 * Action after a click on popup menu
	 * @param  actionEvent  the action-event object
	 */
	public void popupMenuItemActionPerformed( ActionEvent actionEvent ) {
		Integer howMany = Utilities.getInteger( actionEvent.getActionCommand() );
		if( howMany == null ) {
			// cannot do anything
			JOptionPane.showMessageDialog( dialog, "Unable to figure out how many worms to set!", "ERROR", JOptionPane.ERROR_MESSAGE );
			return;
		}; // if
		int x = xPopup;
		int y = yPopup;
		boolean changesFlag = false;
		boolean insideFlag = false;
		List<Integer> sizeList = new ArrayList<Integer>();
		// case1: click occurs inside an existing bounding box
		for( WormInfo worm : wormsList ) {
			if( x >= ( worm.pX - STROKE_WIDTH ) && x <= ( worm.pX + worm.width + STROKE_WIDTH ) 
			&&  y >= ( worm.pY - STROKE_WIDTH ) && y <= ( worm.pY + worm.height + STROKE_WIDTH ) ) {
				insideFlag = true;
				if( worm.nLive != howMany ) {
					worm.nLive = howMany;
					changesFlag = true;
					worm.deleted = worm.nLive == 0 ? true : false;
					break;
				}; // if
			}; // if
			if( worm.deleted == false ) {
				sizeList.add( worm.width );
				sizeList.add( worm.height );
			}; // if
		}; // for

		// case2: user did click for purpuse of adding worm(s)
		if( insideFlag == false ) {
			int size = 55;
			if( sizeList.size() > 1 ) {
				double[] values = new double[ sizeList.size() ];
				for( int i = 0; i < sizeList.size(); i++ ) {
					values[ i ] = sizeList.get( i );
				}; // for
				size = (int) Math.round( StatUtils.mean( values ) );
			}; // if
			WormInfo worm = new WormInfo();
			worm.nLive = howMany;
			worm.pX = x - size / 2;
			worm.pY = y - size / 2;
			worm.width = size;
			worm.height = size;
			worm.deleted = worm.nLive == 0 ? true : false;
			wormsList.add( worm );
			changesFlag = true;
		}; // if

		// when changes are made, update the image
		if( changesFlag == true ) {
			// annotate the colors-image
			ImagePlus annotatedImagePlus = updateImageAnnotations( colorsImagePlus.duplicate() );
			if( zoom == 0 ) {
				colorsImageIcon[ 0 ].setImage( annotatedImagePlus.getBufferedImage() );
			}; // if
			// 50% image
			if( zoom == 1 ) {
				ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize( annotatedImagePlus.getWidth() / 2 );
				updateImageWithWormCount( scaledImageProcessor, 0.5 );
				colorsImageIcon[ 1 ] = new ImageIcon( scaledImageProcessor.getBufferedImage() );
			}; // if
			// 25% image
			if( zoom == 2 ) {
				ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize( annotatedImagePlus.getWidth() / 4 );
				updateImageWithWormCount( scaledImageProcessor, 0.25 );
				colorsImageIcon[ 2 ] = new ImageIcon( scaledImageProcessor.getBufferedImage() );
			}; // if
			scrollablePicture.colorsImageIcon.setImage( colorsImageIcon[ zoom ].getImage() );
			scrollablePicture.revalidate();
			pictureScrollPane.repaint();
		}; // if
	}

	@Override
	public void mouseClicked( MouseEvent mouseEvent ){
		// one-click: delete an existing worm object
		// right-click: show popup menu
		int button = mouseEvent.getButton();
		int x = mouseEvent.getX();
		int y = mouseEvent.getY();
		if( zoom == 1 ) {
			x = x * 2;
			y = y * 2;
		}; // if
		if( zoom == 2 ) {
			x = x * 4;
			y = y * 4;
		}; // if

		boolean changesFlag = false;
		if( button == MouseEvent.BUTTON1 && 1 == mouseEvent.getClickCount() ) {
			// see whether click was inside a bounding box
			for( WormInfo worm : wormsList ) {
				if( x >= ( worm.pX - STROKE_WIDTH ) && x <= ( worm.pX + worm.width + STROKE_WIDTH ) 
				&&  y >= ( worm.pY - STROKE_WIDTH ) && y <= ( worm.pY + worm.height + STROKE_WIDTH ) ) {
					worm.deleted = ! worm.deleted;
					if( worm.deleted == true ) {
						worm.nLive = 0;
					}; // if
					changesFlag = true;
					break;
				}; // if
			}; // for
		}; // if

		// was it a click for popup menu?
		if( button == MouseEvent.BUTTON3 && 1 == mouseEvent.getClickCount() ) {
			popupMenu.show( mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY() );
			xPopup = x;
			yPopup = y;
		}; // if

		// when changes are made, update the image
		if( changesFlag == true ) {
			// annotate the colors-image
			ImagePlus annotatedImagePlus = updateImageAnnotations( colorsImagePlus.duplicate() );
			if( zoom == 0 ) {
				colorsImageIcon[ 0 ].setImage( annotatedImagePlus.getBufferedImage() );
			}; // if
			// 50% image
			if( zoom == 1 ) {
				ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize( annotatedImagePlus.getWidth() / 2 );
				updateImageWithWormCount( scaledImageProcessor, 0.5 );
				colorsImageIcon[ 1 ] = new ImageIcon( scaledImageProcessor.getBufferedImage() );
			}; // if
			// 25% image
			if( zoom == 2 ) {
				ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize( annotatedImagePlus.getWidth() / 4 );
				updateImageWithWormCount( scaledImageProcessor, 0.25 );
				colorsImageIcon[ 2 ] = new ImageIcon( scaledImageProcessor.getBufferedImage() );
			}; // if
			scrollablePicture.colorsImageIcon.setImage( colorsImageIcon[ zoom ].getImage() );
			scrollablePicture.revalidate();
			pictureScrollPane.repaint();
		}; // if
	}


	@Override
	public void mouseEntered( MouseEvent e ) { }

	@Override
	public void mouseExited( MouseEvent e ) { }

	@Override
	public void mousePressed( MouseEvent e ) { }

	@Override
	public void mouseReleased( MouseEvent e ) { }


	/**
	 * Get the errors
	 * @return  the errors; null when no errors detected;
	 *          it is also use to signal a couple of specific event to the window that called plate-view
	 */
	public String getErrors() { 
		return errors;
	}


	@Override
	public void windowDeactivated( WindowEvent windowEvent ) { }

	@Override
	public void windowActivated( WindowEvent windowEvent ) { }

	@Override
	public void windowDeiconified( WindowEvent windowEvent ) { }

	@Override
	public void windowIconified( WindowEvent windowEvent ) { }

	@Override
	public void windowClosed( WindowEvent windowEvent ) { }

	@Override
	public void windowClosing( WindowEvent windowEvent ) {
		if( errors == null ) {
			errors = PAGE_VIEW;  // hack to signal that page-view should re-generate
		}; // if
		close();
	}

	@Override
	public void windowOpened( WindowEvent windowEvent ) { }

} // class PlateView

