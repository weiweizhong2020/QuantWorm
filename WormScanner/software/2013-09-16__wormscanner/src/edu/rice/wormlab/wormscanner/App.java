/*
 * Filename: App.java
 */

package edu.rice.wormlab.wormscanner;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public class App extends JPanel implements ActionListener {

	// serial version UID
	private static final long serialVersionUID = 1L;

	/** the version of this software */
	public static final String VERSION = "WormScanner 9/16/2013";

	// button for pictures-and-video
	protected final JButton picturesAndVideoButton = new JButton( GuiOptionsEnum.PICTURES_AND_VIDEO.guiText );

	// button for image-scanning
	protected final JButton picturesButton = new JButton( GuiOptionsEnum.PICTURES.guiText );

	// button for video-recording
	protected final JButton videoButton = new JButton( GuiOptionsEnum.VIDEO.guiText );

	// the parent-frame that contains this panel 
	private final JFrame parentFrame;

	// Celeste's suggestions:
	// remember selected wells in a plate based on folder, e.g., "c:/data/celeste/experimentname/"----"/platename/"
	// detailed progress bar: well in a plate, whether it is first pass or second pass, video progress 
	// get values of brightness, sharpness from: frameCamControls

	// SangKyu suggestions:
	// "other" microscope configuration?
	// export current image to clipboard
	// option to assemble current folder images
	// progress bar animation as thumbnail of well pictures being taken
	// go to specific well via right-click popup menu
	// when closing window, go back to the small 3-button first window
	
	/** Constructor 
	 * @param  parent  the parent frame
	 */
	public App( JFrame parent ) {
		parentFrame = parent;
		// buttons from left to right
		picturesButton.setActionCommand( GuiOptionsEnum.PICTURES.guiText );
		picturesButton.addActionListener( this );
		picturesButton.setMnemonic( GuiOptionsEnum.PICTURES.mnemonic );

		videoButton.setActionCommand( GuiOptionsEnum.VIDEO.guiText );
		videoButton.addActionListener( this );
		videoButton.setMnemonic( GuiOptionsEnum.VIDEO.mnemonic );

		picturesAndVideoButton.setActionCommand( GuiOptionsEnum.PICTURES_AND_VIDEO.guiText );
		picturesAndVideoButton.addActionListener( this );
		picturesAndVideoButton.setMnemonic( GuiOptionsEnum.PICTURES_AND_VIDEO.mnemonic );
		
		ImageIcon logoImageIcon = new ImageIcon( "images" + File.separator + "logo.jpeg", VERSION );
		JLabel logoLabel = new JLabel( logoImageIcon );
		parent.setIconImage( logoImageIcon.getImage() );

		setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
		setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
		add( logoLabel );
		add( Box.createRigidArea( new Dimension( 20, 0 ) ) );
		add( picturesButton );
		add( Box.createRigidArea( new Dimension( 20, 0 ) ) );
		add( videoButton );
		add( Box.createRigidArea( new Dimension( 20, 0 ) ) );
		add( picturesAndVideoButton );
	}
	

	/**
	 * Create the GUI and show it
	 */
	private static void createAndShowGUI() {
		JFrame frame = new JFrame( VERSION );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		// create the actual object (of the class App)
		final App app = new App( frame );
		app.setOpaque( true );
		frame.getContentPane().add( app );

		// display the window
		frame.pack();
		frame.setVisible( true );

	}


	/** 
	 * Actions of buttons take place here
	 * @param  actionEvent  the action-event object
	 */
	public void actionPerformed( ActionEvent actionEvent ) {
		for( GuiOptionsEnum guiOption : GuiOptionsEnum.values() ) {
			if( guiOption.guiText.equals( actionEvent.getActionCommand() ) == true ) {
				final ScanningAndRecording panel = new ScanningAndRecording( parentFrame, guiOption );
				parentFrame.getContentPane().removeAll();
				parentFrame.getContentPane().add( panel );
				parentFrame.setTitle( VERSION + " - " + guiOption.guiText );
				parentFrame.validate();
				parentFrame.pack();
			}; // if
		}; // for
	}


	/** Runs the app via a runnable invocation */
	public static void main( String[] args ) {
		// want windows look-and-feel
		try {
			UIManager.setLookAndFeel( "com.sun.java.swing.plaf.windows.WindowsLookAndFeel" );
		}
		catch( Exception e ) {
			// do nothing
		}; // try
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				UIManager.put( "swing.boldMetal", Boolean.FALSE );
				App.createAndShowGUI();
			}
		} );
	}


} // class App

