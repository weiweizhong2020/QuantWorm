/**
 * Filename: ScanningAndRecording.java 
 */

package edu.rice.wormlab.wormscanner;

import gnu.io.CommPortIdentifier;
import gui.util.panel.TimePanel;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.media.MediaLocator;
import javax.media.format.VideoFormat;
import javax.media.protocol.FileTypeDescriptor;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.sf.jlibdc1394.JDC1394Cam;
import net.sf.jlibdc1394.JDC1394CamPort;
import net.sf.jlibdc1394.JDC1394VideoSetting;
import net.sf.jlibdc1394.JDC1394VideoSettings;
import net.sf.jlibdc1394.gui.InternalFrameCamSettings;
import net.sf.jlibdc1394.impl.cmu.JDC1394CamPortCMU;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

// the following class are imported from WormTracker 2, which was developed by Eviatar Yemini at the Schafer lab, http://www.mrc-lmb.cam.ac.uk/wormtracker/
import camera.Camera;
import camera.CameraException;
import camera.record.RecordingState;
import camera.record.RecordingStateListener;
import camera.util.CameraInfo;
import camera.util.TimeLength;
import camera.util.VideoLength;
import stage.MotorizedStageIdentifier;
import stage.MovementListener;
import stage.Stage;
import stage.StageException;
import stage.StageInitializationException;
import util.ProgramInfo;
import util.units.Steps;
import config.CameraConfiguration;
import config.ProgramConfiguration;
import config.StageConfiguration;
// end of classes imported from WormTracker.


/**
 * GUI for image-scanning and video-recording;
 * the components that talk to the stage and camera make use of WormTracker 2 as a library. 
 * WormTrakcer 2 was developed by Eviatar Yemini at the Schafer lab, http://www.mrc-lmb.cam.ac.uk/wormtracker/
 *
 * @author Aleman-Meza (
 *
 */
public class ScanningAndRecording extends JPanel implements ActionListener, WindowListener, MovementListener {

	/** default font and size for labels */
	public static final String FONT_LABELS = "Arial-PLAIN-16";
	
	/** default font and size for text-field */
	public static final String FONT_TEXTFIELD = "Arial-PLAIN-16";
	
	/** default location data directory */
	public static final String DATA_DIRECTORY = "C:\\data\\";
	
	/** default properties filename */
	public static final String DEFAULT_PROPERTIES_FILENAME = "settings" + File.separator + "default.properties";
	
	/** constant for menu item 'close' */
	public static final String CLOSE_MENU_ITEM = "Close";
	
	/** constant for menu item 'show camera controls' */
	public static final String SHOW_CAMERA_CONTROLS_MENU_ITEM = "Show camera controls";

	/** constant for menu item 'hide camera controls' */
	public static final String HIDE_CAMERA_CONTROLS_MENU_ITEM = "Hide camera controls";

	/** constant for menu item 'alignment' */
	public static final String ALIGNMENT_MENU_ITEM = "Alignment";
	
	/** constant for menu item 'finish alignment' */
	public static final String FINISH_ALIGNMENT_MENU_ITEM = "Finish Alignment";
	
	/** constant for menu 'recording-locations' */
	public static final String VIDEO_LOCATIONS_MENU = "Recording-Locations";
	
	/** constant for menu item 'load-recording-profile' */
	public static final String LOAD_RECORDING_LOCATIONS_PROFILE_MENU_ITEM = "Load Recording Locations...";
	
	/** constant for menu item 'learn new recording locations' */
	public static final String LEARN_NEW_RECORDING_LOCATIONS = "Learn new recording locations";

	/** constant for button 'browse' */
	public static final String BROWSE_BUTTON = "Browse";
	
	/** constant for button 'start' */
	public static final String START_BUTTON = "Start";
	
	/** constant for button 'cancel' */
	public static final String CANCEL_BUTTON = "Cancel";
	
	/** constant for button 'learn current location' */
	public static final String LEARN_CURRENT_LOCATION_BUTTON = "Learn Current Location";
	
	/** constant for button 'finish and save recording locations to a file' */
	public static final String FINISH_LEARNING_LOCATIONS_BUTTON = "Finish and Save recording locations to a file";
	
	/** constant for button 'cancel' learning locations */
	public static final String CANCEL_LEARNING_LOCATIONS_BUTTON = "Cancel Learning Locations";
	
	/** constant for panel of start/cancel buttons, used in card-layout */
	public static final String START_CANCEL_PANEL = "Start-Cancel-Panel";
	
	/** constant for panel of leaning-new-locations, used in card-layout */
	public static final String LEARNING_LOCATIONS_PANEL = "Learning-Locations-Panel";
	
	/** constant for panel of finish-alignment, used in card-layout */
	public static final String ALIGNMENT_PANEL = "Alignment-Panel";
	
	/** constant for tooltip on select all wells of a plate */
	public static final String SELECT_ALL_WELLS_TOOLTIP = "Select all wells of the plate";
	
	/** constant for tooltip on select none of the wells of a plate */
	public static final String NONE_WELLS_TOOLTIP = "Un-select all wells of the plate";

	/** constant for the name of the log file used in scanning */
	public static final String SCANNING_LOG_FILENAME = "thelog.txt";

	/** constant for default video format: avi */
	public static final String RECORDING_FILE_TYPE = FileTypeDescriptor.MSVIDEO;
	
	/** constant for default video avi extension */
	public static final String AVI_EXTENSION = ".avi";
	
	/** constant for default brightness camera-setting */
	public static final String BRIGHTNESS = "Brightness";

	/** constant for default sharpness camera-setting */
	public static final String SHARPNESS = "Sharpness";

	/** constant for default gain camera-setting */
	public static final String GAIN = "Gain";

	/** constant for time-out of stage movement */
	public static final int STAGE_TIMEOUT = 800000;

	/** The default X resolution */
	public static final int DEFAULT_CAMERA_X_RESOLUTION = 640;

	/** The default Y resolution */
	public static final int DEFAULT_CAMERA_Y_RESOLUTION = 480;
	
	/** constant for number of lines in locations-text-area */
	public static final int LINES_LOCATIONS_TEXTAREA = 4;

	/** constant for number of columns in locations-text-area */
	public static final int COLUMNS_LOCATIONS_TEXTAREA = 30;

	/** (default) time in seconds to wait when doing more than 1 scan of images */
	public static final int SECONDS_TO_WAIT_BETWEEN_SCANS = 120;
	
	/** serial version ID */
	public static final long serialVersionUID = 1L;
	
	/** The default resolution for camera */
	public static final Dimension DEFAULT_CAMERA_RESOLUTION = new Dimension( DEFAULT_CAMERA_X_RESOLUTION, DEFAULT_CAMERA_Y_RESOLUTION );

	// status label
	private final JLabel statusLabel = new JLabel( "Please select a microscope magnification." );
	
	// the frame that contains this panel
	private final JFrame parentFrame;

	// card-layout panel to switch among plate-types
	private final JPanel plateCardLayoutPanel = new JPanel( new CardLayout() );;
	
	// card-layout panel to switch among start/cancel buttons and alignment buttons
	private final JPanel coreButtonsCardLayoutPanel = new JPanel( new CardLayout() );;

	// card-layout panel to switch among pause-selection of different plate-types
	private final JPanel pauseSelectionCardLayoutPanel;

	// camera-settings panel
	private final JPanel cameraSettingsPanel = new JPanel();

	// panel for displaying the video of the camera
	private final JPanel imagePanel = new JPanel();

	// text-field for Directory
	private final JTextField directoryTextField = new JTextField( 60 );

	// locations-profile-text-field
	private final JTextField locationsProfileTextField = new JTextField( 20 );
	
	// panel with hours:minutes:seconds for video-recording time
	private final TimePanel videoTimePanel = new TimePanel();
	
	// formatted text field for seconds value (integer) of wait between two image scans
	private final JFormattedTextField secondsBetweenScansTextField = new JFormattedTextField();
	
	// locations-text-area
	private final JTextArea locationsTextArea = new JTextArea( LINES_LOCATIONS_TEXTAREA, COLUMNS_LOCATIONS_TEXTAREA );

	// cancel-button
	private final JButton cancelButton = new JButton( CANCEL_BUTTON );
	
	// start-button
	private final JButton startButton = new JButton( START_BUTTON );

	// array of the plate type radio buttons
	private final JRadioButton[] plateRadioButton = new JRadioButton[ PlateEnum.values().length ];

	// array of buttons for microscope magnification
	private final JRadioButton[] magnificationRadioButton = new JRadioButton[ MicroscopeEnum.values().length ];

	// scan 1 time radio button
	private final JRadioButton onceScanRadioButton = new JRadioButton( "1 (once)" );
	
	// scan 2 times radio button
	private final JRadioButton twiceScanRadioButton = new JRadioButton( "2 (twice)" );

	// yes-pause to allow user to focus before scanning radio button
	private final JRadioButton yesPauseRadioButton = new JRadioButton( "Yes, pause." );

	// the check-boxes for six-well-plate
	private final JCheckBox[] sixCheckBox = new JCheckBox[ PlateEnum.SIX_WELL.wellsTotal ];
	
	// the check-boxes for twelve-well plate
	private final JCheckBox[] twelveCheckBox = new JCheckBox[ PlateEnum.TWELVE_WELL.wellsTotal ];

	// the check-boxes for twenty4-well plate
	private final JCheckBox[] twenty4CheckBox = new JCheckBox[ PlateEnum.TWENTY4_WELL.wellsTotal ];

	// group of radio buttons that select microscope-magnification
	private final ButtonGroup magnificationButtonGroup = new ButtonGroup();
	
	// locations-profile-file-chooser
	private final JFileChooser locationsProfileFileChooser = new JFileChooser( DATA_DIRECTORY );

	// menu-item for alignment
	private final JMenuItem alignmentMenuItem = new JMenuItem( ALIGNMENT_MENU_ITEM );
	
	// menu-item for showing camera-control
	private final JMenuItem cameraControlsMenuItem = new JMenuItem( SHOW_CAMERA_CONTROLS_MENU_ITEM );

	// menu-item for learning-new-recording-locations
	private final JMenuItem learnLocationsMenuItem = new JMenuItem( LEARN_NEW_RECORDING_LOCATIONS );

	// the effect added into the camera in the alignment step
	private final AlignmentEffect alignmentEffect = new AlignmentEffect( 1 );

	// the GUI option
	private final GuiOptionsEnum guiOption;

	// video format for recording
	private VideoFormat RECORDING_FORMAT = new VideoFormat( VideoFormat.CINEPAK );
	
	// The motorized stage for worm tracking.
	private Stage stage = null;

	// the camera providing the input display
	private Camera camera = null;
	
	// the 1394 camera
	private JDC1394Cam camera1394 = null;
	
	// the scanning/recording tasks are done via a worker-thread
	private ScanWorker scanerWorker;

	// keeps track of currently selected plate type
	private PlateEnum selectedPlate = null;

	// keeps track of selected microscope-configuration profile
	private MicroscopeEnum selectedMicroscopeConfiguration = null;

	// the 1394-camera control settings
	private InternalFrameCamSettings frameCamControls = null;

	// folder from which to get the configuration files
	private String folderWithMicroscopeConfigurations;
	
	// counter for image suffix and works as flag for whether or not to take a photo after stage movement (null = no photo)
	private Integer imageCounter = null;
	
	// brightness value for camera-settings
	private Integer brightnessValue = null;

	// sharpness value for camera-settings
	private Integer sharpnessValue = null;

	// gain value for camera-settings
	private Integer gainValue = null;

	// The number of steps per pixel on the X axis
	private Double stepsPerPixelsX = null;

	// The number of steps per pixel on the Y axis
	private Double stepsPerPixelsY = null;
	
	// the X scan-ready position
	private int xScanReady = 0;

	// the Y scan-ready position
	private int yScanReady = 0;

	// current well number
	private int wellNumber = 1;

	// Number of pictures needed horizontally for current plate and configuration
	private int timesHorizontally = 0;

	// Number of pictures needed vertically for current plate and configuration
	private int timesVertically = 0;

	// keeps track of first, second scan times
	private int actualScanningPass = 1;
	
	// wait/sleep time before taking a snapshot
	private int waitTimeBeforeSnapthot = 200;
	
	// wait/sleep time after taking a snapshot
	private int waitTimeAfterSnapthot = 40;

	// remember start time of a scan-worker task
	private long startTimeScanWorker = 0;
	
	// remember start time of scanning a well
	private long startTimeWellScanning = 0;

	// remember which tracker we are in
	private String tracker = null;

	// we use this to monitor the recording state
	private RecordingState recordingState = null;
	
	// for practical purposes only
	private static final PrintStream out = System.out;

	// for practical purposes only
	private static final String[] EMPTY_STRING_ARRAY = new String[ 0 ];

	
	/** 
	 * Default constructor: 
	 * sets several GUI components and events 
	 * @param  parent  the parent JFrame
	 * @param  option  the GUI option
	 */
	public ScanningAndRecording( JFrame parent, GuiOptionsEnum option ) {
		setOpaque( true );
		this.parentFrame = parent;
		this.guiOption = option;
		parentFrame.setDefaultCloseOperation( JDialog.DO_NOTHING_ON_CLOSE );
		parentFrame.addWindowListener( this );
		setLayout( new GridBagLayout() );

		// directory panel
		JPanel directoryPanel = new JPanel( new GridBagLayout() );
		JLabel directoryLabel = new JLabel( "Directory" );
		directoryLabel.setLabelFor( directoryTextField );
		directoryLabel.setDisplayedMnemonic( KeyEvent.VK_D );
		directoryLabel.setFont( Font.decode( FONT_LABELS ) );
		directoryTextField.setFont( Font.decode( FONT_TEXTFIELD ) );
		directoryTextField.setText( DATA_DIRECTORY );
		final JFileChooser fileChooser = new JFileChooser( DATA_DIRECTORY );
		fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		JButton browseButton = new JButton( BROWSE_BUTTON );
		browseButton.setMnemonic( KeyEvent.VK_B );
		browseButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent arg0 ) {
				int ret = fileChooser.showDialog( parentFrame, "Select" );
				if( ret == JFileChooser.APPROVE_OPTION ) {
					File folder = fileChooser.getSelectedFile();
					if( folder.exists() && folder.isDirectory() ) {
						directoryTextField.setText( folder.getAbsolutePath() );
					}; // if
				}; // if
			}
		});
		directoryPanel.add( directoryLabel, new GBC( 0, 0 ).setDefaultInsets().setAnchor( GBC.LINE_END ) );
		directoryPanel.add( directoryTextField, 
			new GBC( 1, 0 ).setDefaultInsets().setAnchor( GBC.LINE_END ).setWeight( 1.0, 0.0 ).setFill( GBC.HORIZONTAL ) );
		directoryPanel.add( browseButton );

		// plate panel
		JPanel platePanel = new JPanel( new GridBagLayout() );
		platePanel.setBorder( BorderFactory.createEtchedBorder() ); 
		JLabel plateLabel = new JLabel( "<html><center>Type<br>of<br>plate</center></html>" );
		plateLabel.setFont( Font.decode( FONT_LABELS ) );
		ButtonGroup buttonGroup = new ButtonGroup();
		platePanel.add( plateLabel, new GBC( 0, 0 ).setDefaultInsets().setFill( GBC.NONE ).setAnchor( GBC.WEST ).setSpan( 1, 4 ) );
		for( PlateEnum eachPlate : PlateEnum.values() ) {
			plateRadioButton[ eachPlate.ordinal() ] = new JRadioButton( eachPlate.toString() );
			plateRadioButton[ eachPlate.ordinal() ].setActionCommand( eachPlate.name() );
			plateRadioButton[ eachPlate.ordinal() ].addActionListener( this );
			plateRadioButton[ eachPlate.ordinal() ].setFont( Font.decode( FONT_LABELS ) );
			buttonGroup.add( plateRadioButton[ eachPlate.ordinal() ] );
			platePanel.add( plateRadioButton[ eachPlate.ordinal() ], new GBC( 1, eachPlate.ordinal() ).setDefaultInsets().setFill( GBC.NONE ).setAnchor( GBC.WEST ) );
		}; // for
				
		// 6-well panel
		JPanel sixWellOuterPanel = new JPanel( new GridBagLayout() );
		JPanel sixWellInnerPanel = new JPanel( new GridBagLayout() );
		int x = 0;
		int y = 0;
		for( int i = 0; i < sixCheckBox.length; i++ ) {
			sixCheckBox[ i ] = new JCheckBox( "" + ( i + 1 ), true );
			sixWellInnerPanel.add( sixCheckBox[ i ], new GBC( x, y ).setDefaultInsets().setAnchor( GBC.CENTER ) );
			x++;
			if( x > 2 ) {
				x = 0;
				y++;
			}; // if
		}; // for
		sixWellInnerPanel.setBorder( BorderFactory.createEtchedBorder() );
		JButton all6WellButton = new JButton( "all" );
		all6WellButton.setToolTipText( SELECT_ALL_WELLS_TOOLTIP );
		JButton none6WellButton = new JButton( "none" );
		none6WellButton.setToolTipText( NONE_WELLS_TOOLTIP );
		sixWellOuterPanel.add( sixWellInnerPanel, new GBC( 0, 0 ).setDefaultInsets().setWeight( 1.0, 1.0 ).setSpan( 1, 2 ) );
		sixWellOuterPanel.add( all6WellButton, new GBC( 1, 0 ).setDefaultInsets().setAnchor( GBC.PAGE_END ).setWeight( .8, .8 ) );
		sixWellOuterPanel.add( none6WellButton, new GBC( 1, 1).setDefaultInsets().setAnchor( GBC.PAGE_START ).setWeight( .8, .8 ) );
		// select-all-6-wells
		all6WellButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent arg0 ) {
				for( int i = 0; i < sixCheckBox.length; i++ ) {
					sixCheckBox[ i ].setSelected( true );
				}; // for
			}
		});
		// unselect-all-6-wells
		none6WellButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent arg0 ) {
				for( int i = 0; i < sixCheckBox.length; i++ ) {
					sixCheckBox[ i ].setSelected( false );
				}; // for
			}
		});
		
		// 12-well plate
		JPanel twelveWellOuterPanel = new JPanel( new GridBagLayout() );
		JPanel twelveWellInnerPanel = new JPanel( new GridBagLayout() );
		x = 0;
		y = 0;
		for( int i = 0; i < twelveCheckBox.length; i++ ) {
			twelveCheckBox[ i ] = new JCheckBox( "" + ( i + 1 ), true );
			twelveWellInnerPanel.add( twelveCheckBox[ i ], new GBC( x, y ).setDefaultInsets().setAnchor( GBC.CENTER ) );
			x++;
			if( x > 3 ) {
				x = 0;
				y++;
			}; // if
		}; // for
		twelveWellInnerPanel.setBorder( BorderFactory.createEtchedBorder() );
		JButton all12WellButton = new JButton( "all" );
		all12WellButton.setToolTipText( SELECT_ALL_WELLS_TOOLTIP );
		JButton none12WellButton = new JButton( "none" );
		none12WellButton.setToolTipText( NONE_WELLS_TOOLTIP );
		twelveWellOuterPanel.add( twelveWellInnerPanel, new GBC( 0, 0 ).setDefaultInsets().setWeight( 1.0, 1.0 ).setSpan( 1, 2 ) );
		twelveWellOuterPanel.add( all12WellButton, new GBC( 1, 0 ).setDefaultInsets().setAnchor( GBC.PAGE_END ).setWeight( .8, .8 ) );
		twelveWellOuterPanel.add( none12WellButton, new GBC( 1, 1).setDefaultInsets().setAnchor( GBC.PAGE_START ).setWeight( .8, .8 ) );
		// select-all-12-wells
		all12WellButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent arg0 ) {
				for( int i = 0; i < twelveCheckBox.length; i++ ) {
					twelveCheckBox[ i ].setSelected( true );
				}; // for
			}
		});
		// unselect-all-12-wells
		none12WellButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent arg0 ) {
				for( int i = 0; i < twelveCheckBox.length; i++ ) {
					twelveCheckBox[ i ].setSelected( false );
				}; // for
			}
		});
		
		// 24-well plate
		JPanel twenty4WellOuterPanel = new JPanel( new GridBagLayout() );
		JPanel twenty4WellInnerPanel = new JPanel( new GridBagLayout() );
		x = 0;
		y = 0;
		for( int i = 0; i < twenty4CheckBox.length; i++ ) {
			twenty4CheckBox[ i ] = new JCheckBox( "" + ( i + 1 ), true );
			twenty4WellInnerPanel.add( twenty4CheckBox[ i ], new GBC( x, y ).setDefaultInsets().setAnchor( GBC.CENTER ) );
			x++;
			if( x > 5 ) {
				x = 0;
				y++;
			}; // if
		}; // for
		twenty4WellInnerPanel.setBorder( BorderFactory.createEtchedBorder() );
		twenty4WellOuterPanel.add( twenty4WellInnerPanel, new GBC( 0, 0 ).setDefaultInsets().setWeight( 1.0, 1.0 ).setSpan( 1, 2 ) );
		JButton all24WellButton = new JButton( "all" );
		all24WellButton.setToolTipText( SELECT_ALL_WELLS_TOOLTIP );
		JButton none24WellButton = new JButton( "none" );
		none24WellButton.setToolTipText( NONE_WELLS_TOOLTIP );
		twenty4WellOuterPanel.add( all24WellButton, new GBC( 1, 0 ).setDefaultInsets().setAnchor( GBC.PAGE_END ).setWeight( .8, .8 ) );
		twenty4WellOuterPanel.add( none24WellButton, new GBC( 1, 1 ).setDefaultInsets().setAnchor( GBC.PAGE_START ).setWeight( .8, .8 ) );
		// select-all-24-wells
		all24WellButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent arg0 ) {
				for( int i = 0; i < twenty4CheckBox.length; i++ ) {
					twenty4CheckBox[ i ].setSelected( true );
				}; // for
			}
		});
		// unselect-all-24-wells
		none24WellButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent arg0 ) {
				for( int i = 0; i < twenty4CheckBox.length; i++ ) {
					twenty4CheckBox[ i ].setSelected( false );
				}; // for
			}
		});
		
		// cards-layout for plate-types
		plateCardLayoutPanel.add( new JPanel(), PlateEnum.SIX_CM.toString() );
		plateCardLayoutPanel.add( sixWellOuterPanel, PlateEnum.SIX_WELL.toString() );
		plateCardLayoutPanel.add( twelveWellOuterPanel, PlateEnum.TWELVE_WELL.toString() );
		plateCardLayoutPanel.add( twenty4WellOuterPanel, PlateEnum.TWENTY4_WELL.toString() );
		platePanel.add( plateCardLayoutPanel, new GBC( 2, 0 ).setDefaultInsets().setFill( GBC.NONE ).setAnchor( GBC.CENTER ).setSpan( 1, 4 ) );
		
		// pause-or-not panel
		JPanel pauseSelectionPanel = new JPanel( new GridBagLayout() );
		pauseSelectionPanel.setBorder( BorderFactory.createEtchedBorder() ); 
		JLabel pauseLabel = new JLabel( "Pause at each well for focusing:" );
		pauseLabel.setFont( Font.decode( FONT_LABELS ) );
		yesPauseRadioButton.setFont( Font.decode( FONT_LABELS ) );
		JRadioButton noPauseRadioButton = new JRadioButton( "Do NOT pause." );
		noPauseRadioButton.setFont( Font.decode( FONT_LABELS ) );
		noPauseRadioButton.setSelected( true );
		ButtonGroup pauseButtonGroup = new ButtonGroup();
		pauseButtonGroup.add( yesPauseRadioButton );
		pauseButtonGroup.add( noPauseRadioButton );
		pauseSelectionPanel.add( pauseLabel, new GBC( 0, 0 ).setDefaultInsets().setFill( GBC.NONE ).setAnchor( GBC.LINE_START ) );
		pauseSelectionPanel.add( yesPauseRadioButton, new GBC( 1, 0 ).setDefaultInsets().setAnchor( GBC.LINE_START ) );
		pauseSelectionPanel.add( noPauseRadioButton, new GBC( 1, 1 ).setDefaultInsets().setAnchor( GBC.LINE_START ) );
		// cards-layout for pause-selection
		pauseSelectionCardLayoutPanel = new JPanel( new CardLayout() );
		pauseSelectionCardLayoutPanel.add( new JPanel(), PlateEnum.SIX_CM.toString() );
		pauseSelectionCardLayoutPanel.add( pauseSelectionPanel, PlateEnum.WELL_PLATE );
		
		// scan-times panel
		JPanel scanTimesPanel = new JPanel( new GridBagLayout() );
		if( GuiOptionsEnum.VIDEO.equals( guiOption ) == false ) {
			scanTimesPanel.setBorder( BorderFactory.createEtchedBorder() );
			JLabel scanTimesLabel = new JLabel( "Scan times:" );
			scanTimesLabel.setFont( Font.decode( FONT_LABELS ) );
			onceScanRadioButton.setFont( Font.decode( FONT_LABELS ) );
			twiceScanRadioButton.setFont( Font.decode( FONT_LABELS ) );
			ButtonGroup scanTimesButtonGroup = new ButtonGroup();
			scanTimesButtonGroup.add( onceScanRadioButton );
			scanTimesButtonGroup.add( twiceScanRadioButton );
			secondsBetweenScansTextField.setValue( SECONDS_TO_WAIT_BETWEEN_SCANS );
			secondsBetweenScansTextField.setColumns( 4 );
			scanTimesPanel.add( scanTimesLabel, new GBC( 0, 0 ).setDefaultInsets().setFill(  GBC.HORIZONTAL ).setAnchor( GBC.WEST ) );
			scanTimesPanel.add( onceScanRadioButton, new GBC( 1, 0 ).setDefaultInsets().setFill(  GBC.HORIZONTAL ).setAnchor( GBC.WEST ) );
			scanTimesPanel.add( twiceScanRadioButton, new GBC( 1, 1 ).setDefaultInsets().setFill(  GBC.HORIZONTAL ).setAnchor( GBC.WEST ) );
			scanTimesPanel.add( secondsBetweenScansTextField, new GBC( 2, 1 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.WEST ) );
			scanTimesPanel.add( new JLabel( "Time between scans (seconds)." ), new GBC( 3, 1 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.WEST ) );
		}; // if
		
		// microscope panel
		JPanel microscopePanel = new JPanel( new GridBagLayout() );
		microscopePanel.setBorder( BorderFactory.createEtchedBorder() );
		JLabel microscopeLabel = new JLabel( "Microscope:" );
		microscopeLabel.setFont( Font.decode( FONT_LABELS ) );
		microscopePanel.add( microscopeLabel, new GBC( 0, 0 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.LINE_START ) );
		for( MicroscopeEnum eachMicroscope : MicroscopeEnum.values() ) {
			magnificationRadioButton[ eachMicroscope.ordinal() ] = new JRadioButton( eachMicroscope.guiText );
			magnificationRadioButton[ eachMicroscope.ordinal() ].setSelected( false );
			magnificationRadioButton[ eachMicroscope.ordinal() ].setFont( Font.decode( FONT_LABELS ) );
			magnificationRadioButton[ eachMicroscope.ordinal() ].addActionListener( this );
			magnificationRadioButton[ eachMicroscope.ordinal() ].setActionCommand( eachMicroscope.name() );
			magnificationButtonGroup.add( magnificationRadioButton[ eachMicroscope.ordinal() ] );
			microscopePanel.add( magnificationRadioButton[ eachMicroscope.ordinal() ], new GBC( ( eachMicroscope.ordinal() + 1 ), 0 ).setDefaultInsets() );
		}; // for
	
		// image panel : photo or video
		ImageIcon imageIcon = new ImageIcon( "images/piece_56.jpeg" );
		imagePanel.add( new JLabel( imageIcon ) );
		imagePanel.setBorder( BorderFactory.createEtchedBorder() ); 
		
		// recording-length panel
		JPanel recordingLengthPanel = new JPanel( new GridBagLayout() );
		if( GuiOptionsEnum.PICTURES.equals( guiOption ) == false ) {
			recordingLengthPanel.setBorder( BorderFactory.createEtchedBorder() );
			JLabel lengthLabel = new JLabel( "Length of video:" );
			lengthLabel.setLabelFor( videoTimePanel );
			lengthLabel.setDisplayedMnemonic( KeyEvent.VK_L );
			lengthLabel.setFont( Font.decode( FONT_LABELS ) );
			JLabel secondsLabel = new JLabel( "hh:mm:ss" );
			secondsLabel.setLabelFor( videoTimePanel );
			secondsLabel.setFont( Font.decode( FONT_LABELS ) );
			JLabel profileLabel = new JLabel( "Recording locations file:" );
			profileLabel.setFont( Font.decode( FONT_LABELS ) );
			locationsProfileFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
			JButton locationsProfileButton = new JButton( BROWSE_BUTTON );
			locationsProfileButton.setActionCommand( LOAD_RECORDING_LOCATIONS_PROFILE_MENU_ITEM );
			locationsProfileButton.addActionListener( this );
			recordingLengthPanel.add( lengthLabel, new GBC( 0, 0 ).setDefaultInsets().setAnchor( GBC.FIRST_LINE_START ) );
			recordingLengthPanel.add( videoTimePanel, new GBC( 1, 0 ).setDefaultInsets().setAnchor( GBC.FIRST_LINE_START ) );
			recordingLengthPanel.add( secondsLabel, new GBC( 2, 0 ).setDefaultInsets().setAnchor( GBC.FIRST_LINE_START ) );
			recordingLengthPanel.add( profileLabel, new GBC( 0, 1 ).setDefaultInsets().setAnchor( GBC.FIRST_LINE_START ) );
			recordingLengthPanel.add( locationsProfileTextField, new GBC( 1, 1 ).setDefaultInsets().setAnchor( GBC.FIRST_LINE_START ).setSpan( 2, 1 ) );
			recordingLengthPanel.add( locationsProfileButton, new GBC( 3, 1 ).setDefaultInsets().setAnchor( GBC.FIRST_LINE_START ) );
		}; // if
		
		// start/cancel buttons Panel
		JPanel buttonsPanel = new JPanel( new FlowLayout( FlowLayout.CENTER, 22, 5 ) );
		startButton.setActionCommand( START_BUTTON );
		startButton.setFont( Font.decode( FONT_LABELS ) );
		startButton.addActionListener( this );
		cancelButton.setActionCommand( CANCEL_BUTTON );
		cancelButton.addActionListener( this );
		cancelButton.setFont( Font.decode( FONT_LABELS ) );
		cancelButton.setEnabled( false );
		buttonsPanel.add( startButton );
		buttonsPanel.add( cancelButton );
		
		// learning-locations buttons Panel
		JPanel learningLocationsPanel = new JPanel( new GridBagLayout() );
		JButton learnCurrentLocationButton = new JButton( LEARN_CURRENT_LOCATION_BUTTON );
		learnCurrentLocationButton.addActionListener( this );
		learnCurrentLocationButton.setActionCommand( LEARN_CURRENT_LOCATION_BUTTON );
		learnCurrentLocationButton.setFont( Font.decode( FONT_LABELS ) );
		JButton finishLearningLocationsButton = new JButton( FINISH_LEARNING_LOCATIONS_BUTTON );
		finishLearningLocationsButton.addActionListener( this );
		finishLearningLocationsButton.setActionCommand( FINISH_LEARNING_LOCATIONS_BUTTON );
		finishLearningLocationsButton.setFont( Font.decode( FONT_LABELS ) );
		JButton cancelLearningLocationsButton = new JButton( CANCEL_LEARNING_LOCATIONS_BUTTON );
		cancelLearningLocationsButton.addActionListener( this );
		cancelLearningLocationsButton.setActionCommand( CANCEL_LEARNING_LOCATIONS_BUTTON );
		cancelLearningLocationsButton.setFont( Font.decode( FONT_LABELS ) );
		locationsTextArea.setEnabled( false );
		locationsTextArea.setTabSize( 5 );
		JScrollPane scrollPane = new JScrollPane( locationsTextArea );
		JLabel locationsLabel = new JLabel( "position  well      coordinates" );
		// inner panel for text-area and label
		JPanel textAreaPanel = new JPanel( new GridBagLayout() );
		textAreaPanel.add( locationsLabel, new GBC( 0, 0 ).setAnchor( GBC.FIRST_LINE_START ) );
		textAreaPanel.add( scrollPane, new GBC( 0, 1 ).setAnchor( GBC.FIRST_LINE_START ).setWeight( 1.0, 1.0 ) );
		learningLocationsPanel.add( learnCurrentLocationButton, new GBC( 0, 0 ).setDefaultInsets().setAnchor( GBC.CENTER ) );
		learningLocationsPanel.add( finishLearningLocationsButton, new GBC( 0, 1 ).setDefaultInsets().setAnchor( GBC.CENTER ) );
		learningLocationsPanel.add( cancelLearningLocationsButton, new GBC( 0, 2 ).setDefaultInsets().setAnchor( GBC.CENTER ) );
		learningLocationsPanel.add( textAreaPanel, new GBC( 1, 0 ).setAnchor( GBC.FIRST_LINE_START ).setDefaultInsets().setSpan( 0, 3 ) );
		
		// finish-alignment buttons panel
		JPanel alignmentPanel = new JPanel( new FlowLayout( FlowLayout.CENTER, 22, 5 ) );
		JButton finishAlignmentButton = new JButton( "Finish Alignment" );
		finishAlignmentButton.addActionListener( this );
		finishAlignmentButton.setActionCommand( ALIGNMENT_MENU_ITEM );
		finishAlignmentButton.setFont( Font.decode( FONT_LABELS ) );
		alignmentPanel.add( finishAlignmentButton );
		
		coreButtonsCardLayoutPanel.add( buttonsPanel, START_CANCEL_PANEL );
		coreButtonsCardLayoutPanel.add( learningLocationsPanel, LEARNING_LOCATIONS_PANEL );
		coreButtonsCardLayoutPanel.add( alignmentPanel, ALIGNMENT_PANEL );
		
		// set the minimum size of the camera-settings panel
		cameraSettingsPanel.setBorder( BorderFactory.createEtchedBorder() );
		cameraSettingsPanel.setMinimumSize( new Dimension( 60, 170 ) );
		cameraSettingsPanel.setPreferredSize( new Dimension( 60, 170 ) );
		
		// add everything to the main panel
		statusLabel.setHorizontalAlignment( JLabel.LEFT );
		statusLabel.setFont( Font.decode( FONT_LABELS ) );
		add( directoryPanel, new GBC( 0, 0 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.FIRST_LINE_START ).setSpan( 2, 1 ) );
		add( platePanel, new GBC( 0, 1 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.WEST ) );
		add( pauseSelectionCardLayoutPanel, new GBC( 0, 2 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.FIRST_LINE_START ) );
		add( scanTimesPanel, new GBC( 0, 3 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.FIRST_LINE_START ) );
		add( microscopePanel, new GBC( 0, 4 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.FIRST_LINE_START ) );
		add( cameraSettingsPanel, new GBC( 0, 5 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.FIRST_LINE_START ) );
		add( imagePanel, new GBC( 1, 1 ).setDefaultInsets().setSpan( 1, 5 ) );
		add( recordingLengthPanel, new GBC( 0, 6 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.FIRST_LINE_START ) );
		add( coreButtonsCardLayoutPanel, new GBC( 1, 6 ).setDefaultInsets() );
		add( statusLabel, new GBC( 0, 7 ).setDefaultInsets().setFill( GBC.HORIZONTAL ).setAnchor( GBC.LAST_LINE_START ).setSpan( 2, 2 ).setWeight( 1.0, 0.1 ) );
		
		// menu bar
		JMenuBar menuBar = new JMenuBar();
		// file menu
		JMenu fileMenu = new JMenu( "File" );
		fileMenu.setMnemonic( KeyEvent.VK_F );
		JMenuItem exitMenuItem = new JMenuItem( CLOSE_MENU_ITEM );
		exitMenuItem.setMnemonic( KeyEvent.VK_C );
		exitMenuItem.addActionListener( this );
		exitMenuItem.setActionCommand( CLOSE_MENU_ITEM );
		fileMenu.add( exitMenuItem );
		menuBar.add( fileMenu );
		// settings menu
		cameraControlsMenuItem.setMnemonic( KeyEvent.VK_C );
		cameraControlsMenuItem.addActionListener( this );
		cameraControlsMenuItem.setActionCommand( SHOW_CAMERA_CONTROLS_MENU_ITEM );
		alignmentMenuItem.setMnemonic( KeyEvent.VK_A );
		alignmentMenuItem.addActionListener( this );
		alignmentMenuItem.setActionCommand( ALIGNMENT_MENU_ITEM );
		JMenu settingsMenu = new JMenu( "Settings" );
		menuBar.add( settingsMenu );
		settingsMenu.add( cameraControlsMenuItem );
		settingsMenu.add( alignmentMenuItem );
		// video-locations
		if( GuiOptionsEnum.PICTURES.equals( guiOption ) == false ) {
			JMenu videoLocationsMenu = new JMenu( VIDEO_LOCATIONS_MENU );
			videoLocationsMenu.setMnemonic( KeyEvent.VK_R );
			videoLocationsMenu.addActionListener( this );
			videoLocationsMenu.setActionCommand( VIDEO_LOCATIONS_MENU );
			menuBar.add( videoLocationsMenu );
			JMenuItem chooseRecordingLocationsProfile = new JMenuItem( LOAD_RECORDING_LOCATIONS_PROFILE_MENU_ITEM );
			chooseRecordingLocationsProfile.setMnemonic( KeyEvent.VK_L );
			chooseRecordingLocationsProfile.addActionListener( this );
			chooseRecordingLocationsProfile.setActionCommand( LOAD_RECORDING_LOCATIONS_PROFILE_MENU_ITEM );
			videoLocationsMenu.add( chooseRecordingLocationsProfile );
			learnLocationsMenuItem.setMnemonic( KeyEvent.VK_N );
			learnLocationsMenuItem.addActionListener( this );
			learnLocationsMenuItem.setActionCommand( LEARN_NEW_RECORDING_LOCATIONS );
			videoLocationsMenu.add( learnLocationsMenuItem );
		}; // if
	
		parentFrame.setJMenuBar( menuBar );
		
		// defaults for App.PICTURES, App.VIDEO, or App.PICTURES_AND_VIDEO
		Properties properties = new Properties();
		String error = null;
		try {
			FileInputStream fileInputStream = new FileInputStream( DEFAULT_PROPERTIES_FILENAME );
			properties.load( fileInputStream );
			fileInputStream.close();
		}
		catch( FileNotFoundException fnfe ) {
			error = "Unable to find the default settings file ( " + DEFAULT_PROPERTIES_FILENAME + " )";
		}
		catch( IOException ioe ) {
			error = "Unable to read the default settings file ( " + DEFAULT_PROPERTIES_FILENAME + " )\n" + "Details: " + ioe.getMessage();
		}; // try
		if( error != null ) {
			JOptionPane.showMessageDialog( parentFrame, "Error: \n" + error, "Severe Error", JOptionPane.ERROR_MESSAGE );
			windowClosing( null );
			return;
		}; // if
		// get the tracker
		tracker = properties.getProperty( "tracker" );
		if( tracker == null || "".equals( tracker.trim() ) == true ) {
			JOptionPane.showMessageDialog( parentFrame, "Error: Unable to read 'tracker' in settings file.", "Error: Unable to read 'tracker' in settings file.", JOptionPane.ERROR_MESSAGE );
			windowClosing( null );
			return;
		}; // if
		// get the default for plate is similar for the gui-option
		String plate = properties.getProperty( guiOption + ".plate" );
		if( plate != null && "".equals( plate ) == false ) {
			boolean match = false;
			for( final JRadioButton radioButton : plateRadioButton ) {
				if( radioButton.getText().equals( plate ) == true ) {
					match = true;
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							radioButton.doClick();
						}
					});
				}; // if
			}; // for
			if( match == false ) {
				final JRadioButton radioButton = plateRadioButton[ 0 ];
				JOptionPane.showMessageDialog( parentFrame, "Error: \n" + "Invalid default setting for plate: " + plate + "\n" + "Such value has been ignored." + "\n" + "The plate " + radioButton.getText() + " will be selected instead.", "Error: Invalid plate default seting.", JOptionPane.ERROR_MESSAGE );
				SwingUtilities.invokeLater( new Runnable() {
					public void run() {
						radioButton.doClick();
					}
				});
			}; // if
		}; // if
		// deal with other default options
		String scanTimes = properties.getProperty( guiOption + ".scan-times", "1" );
		if( "2".equals( scanTimes ) == true ) {
			twiceScanRadioButton.setSelected( true );
		}
		else {
			onceScanRadioButton.setSelected( true );
		}; // if
		// recording-options are relevant only when not in 'pictures' option
		if( guiOption.equals( GuiOptionsEnum.PICTURES ) == false ) {
			// load the default recording time (in seconds)
			String recordingTime = properties.getProperty( "recording-time-in-seconds", "30" );
			Integer seconds = Utilities.getInteger( recordingTime );
			if( seconds == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Error: \n" + "Invalid default recording time: " + recordingTime + "\n" + "Please fix the default settings file!", "Severe Error", JOptionPane.ERROR_MESSAGE );
				windowClosing( null );
				return;
			}; // if
			videoTimePanel.setTime( seconds * 1000 );
			// load the default location of the recording-locations file
			String location = properties.getProperty( "file-of-recording-locations", "" );
			location = location.trim();
			if( "".equals( location ) == false ) {
				File file = new File( location );
				if( file.exists() == false ) {
					JOptionPane.showMessageDialog( parentFrame, "Warning: \n" + "Invalid default setting for file of recording-locations:\n" + location + "\n" + "The file does not exist!" + "\n" + "Such value has been ignored.", "Warning", JOptionPane.WARNING_MESSAGE );
					location = "";
				}; // if
			}; // if
			locationsProfileTextField.setText( location );
		}; // if
		// set the folder from which to read configuration files
		folderWithMicroscopeConfigurations = properties.getProperty( "folder-with-microscope-configurations" );
		if( folderWithMicroscopeConfigurations != null ) {
			folderWithMicroscopeConfigurations = folderWithMicroscopeConfigurations.trim();
		}; // if
		if( folderWithMicroscopeConfigurations == null || "".equals( folderWithMicroscopeConfigurations ) == true ) {
			JOptionPane.showMessageDialog( parentFrame, "Severe Error: Unspecified folder with microscope configurations.\n" + "Please fix the default settings file!", "Error: Unspecified folder with microscope configurations.", JOptionPane.ERROR_MESSAGE );
			windowClosing( null );
			return;
		}; // if
		File folder = new File( folderWithMicroscopeConfigurations );
		if( folder.exists() == false ) {
			JOptionPane.showMessageDialog( parentFrame, "Severe Error: Folder with microscope configurations does not exist.\n" + "Please fix the default settings file!", "Error: Folder with microscope configurations does not exist.", JOptionPane.ERROR_MESSAGE );
			windowClosing( null );
			return;
		}; // if
		if( folder.isDirectory() == false ) {
			JOptionPane.showMessageDialog( parentFrame, "Severe Error: Folder with microscope configurations must be a directory!\nIt cannot be a regular file!\nLocation: " + folder.getAbsolutePath() + "\nPlease fix the default settings file!", "Error: Folder with microscope configurations cannot be a regular file.", JOptionPane.ERROR_MESSAGE );
			windowClosing( null );
			return;
		}; // if
		
		for( MicroscopeEnum eachMicroscope : MicroscopeEnum.values() ) {
			String filename = folderWithMicroscopeConfigurations;
			if( filename.endsWith( File.separator ) == false ) {
				filename += File.separator;
			}; // if
			filename += eachMicroscope.configurationProfile;
			File file = new File( filename );
			if( file.exists() == false || file.isDirectory() == true || file.canRead() == false ) {
				magnificationRadioButton[ eachMicroscope.ordinal() ].setEnabled( false );
				magnificationRadioButton[ eachMicroscope.ordinal() ].setToolTipText( eachMicroscope.configurationProfile + " not found." );
			}; // if
		}; // for
		
		// any brightness setting remembered?
		brightnessValue = Utilities.getInteger( properties.getProperty( guiOption + "." + BRIGHTNESS ) );
		sharpnessValue = Utilities.getInteger( properties.getProperty( guiOption + "." + SHARPNESS ) );
		gainValue = Utilities.getInteger( properties.getProperty( guiOption + "." + GAIN ) );
		
		// pause or not pause for focusing?
		String pause = properties.getProperty( guiOption + "." + "pause-for-focusing" );
		if( "yes".equalsIgnoreCase( pause ) == true || "true".equalsIgnoreCase( pause ) == true ) {
			yesPauseRadioButton.setSelected( true );
		}; // if
		
		Integer value = Utilities.getInteger( properties.getProperty( "wait-time-before-taking-snapthot" ) );
		if( value != null && value > 0 && value < 5000 ) {
			waitTimeBeforeSnapthot = value;
		}; // if
		value = Utilities.getInteger( properties.getProperty( "wait-time-after-taking-snapthot" ) );
		if( value != null && value > 0 && value < 5000 ) {
			waitTimeAfterSnapthot = value;
		}; // if
		
		// we really want to do the microscope-magnification the last thing being set
		String microscope = properties.getProperty( guiOption + ".microscope" );
		if( microscope != null && "".equals( microscope ) == false ) {
			boolean match = false;
			for( final JRadioButton radioButton : magnificationRadioButton ) {
				if( radioButton.getText().equals( microscope ) == true ) {
					match = true;
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							radioButton.doClick();
						}
					});
				}; // if
			}; // for
			if( match == false ) {
				JOptionPane.showMessageDialog( parentFrame, "Warning: \n" + "Invalid default setting for microscope: " + microscope + "\n" + "Such value has been ignored.", "Warning", JOptionPane.WARNING_MESSAGE );
			}; // if
		}; // if
	}

	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed( ActionEvent actionEvent ) {
		if( selectedPlate != null && selectedPlate.toString().equals( actionEvent.getActionCommand() ) == true ) {
			// we do nothing when plate is already the selected one
			return;
		}; // if

		// we do nothing when the microscope-magnification is already the selected one
		if( selectedMicroscopeConfiguration != null && selectedMicroscopeConfiguration.name().equals( actionEvent.getActionCommand() ) == true ) {
			//out.println( "already have " + selectedMicroscopeConfiguration + " selected." );
			return;
		}; // if
		
		// see whether a plate-type was selected
		for( PlateEnum eachPlate : PlateEnum.values() ) {
			if( eachPlate.name().equals( actionEvent.getActionCommand() ) == true ) {
				// update the panel in plate-type
				CardLayout cardLayout = (CardLayout) plateCardLayoutPanel.getLayout();
				cardLayout.show( plateCardLayoutPanel, eachPlate.toString() );
				// update the panel in pause-selection
				cardLayout = (CardLayout) pauseSelectionCardLayoutPanel.getLayout();
				cardLayout.show( pauseSelectionCardLayoutPanel, eachPlate.getNameCardLayout() );
				selectedPlate = eachPlate;
				if( selectedMicroscopeConfiguration != null ) {
					updateScanningVariables();
					moveToFocusingPosition();
				}; // if
				return;
			}; // if
		}; // for
		
		if( CLOSE_MENU_ITEM.equals( actionEvent.getActionCommand() ) == true ) {
			windowClosing( null );
		}; // if
		
		// see whether a microscope-magnification was selected?
		for( MicroscopeEnum eachMicroscope : MicroscopeEnum.values() ) {
			if( eachMicroscope.name().equals( actionEvent.getActionCommand() ) == true ) {
				String errors = doSelectMicroscopeMagnification( eachMicroscope );
				if( errors == null ) {
					// when configuration loads OK, we remember the selected magnification
					selectedMicroscopeConfiguration = eachMicroscope;
					statusLabel.setText( "Ready." );
					// wait fraction of a second before sending the first movement command to the stage
					try { 
						Thread.sleep( 200 );
					}
					catch( InterruptedException ie ) {
						// do nothing
					}; // try
					moveToFocusingPosition();
				}
				else {
					selectedMicroscopeConfiguration = null;
					magnificationButtonGroup.clearSelection();
					JOptionPane.showMessageDialog( parentFrame, "Error ... \nUnable to set microscope-magnification profile for: " + eachMicroscope.guiText + "\nPlease choose other magnification or diagnose the problem.\nDetails: " + errors, "Error: Can not set the microscope profile.", JOptionPane.ERROR_MESSAGE );
					statusLabel.setText( "Unable to set microscope-magnification profile for: " + eachMicroscope.guiText );
				}; // if
				return;
			}; // if
		}; // for
		
		if( START_BUTTON.equals( actionEvent.getActionCommand() ) == true ) {
			doStartButton();
		}; // if
		
		if( CANCEL_BUTTON.equals( actionEvent.getActionCommand() ) == true ) {
			if( scanerWorker != null ) {
				scanerWorker.cancel( true );
				scanerWorker = null;
			}; // if
			startButton.setEnabled( true );
			startButton.setText( START_BUTTON );
			cancelButton.setEnabled( false );
			reEnableMicroscopeMagnificationRadioButtons();
		}; // if
		
		if( LOAD_RECORDING_LOCATIONS_PROFILE_MENU_ITEM.equals( actionEvent.getActionCommand() ) == true ) {
			int ret = locationsProfileFileChooser.showDialog( parentFrame, "Select" );
			if( ret == JFileChooser.APPROVE_OPTION ) {
				File file = locationsProfileFileChooser.getSelectedFile();
				if( file.exists() && file.isDirectory() == false ) {
					List<PlateEnum> typeOfPlateList = new ArrayList<PlateEnum>();
					String errors = RecordingLocation.verifyLocationsProfileValidity( file, null, typeOfPlateList );
					if( errors == null ) {
						locationsProfileTextField.setText( file.getAbsolutePath() );
						if( typeOfPlateList.size() > 0 ) {
							PlateEnum plateInLocationsProfile = typeOfPlateList.get( 0 );
							if( plateInLocationsProfile.equals( selectedPlate ) == false ) {
								JOptionPane.showMessageDialog( parentFrame, "Warning:\nThe selected plate does not match the one in the recording-locations profile.\nThe type of plate in the recording-locations profile is: " + plateInLocationsProfile + " .\n", "Warning: Type of plate in recording-locations does not match current selection.", JOptionPane.WARNING_MESSAGE );
							}
						}; // if
					}
					else {
						JOptionPane.showMessageDialog( parentFrame, "Error ... \n" + errors + "\n" + "This recording-locations file can not be used.", "Error: Recording-locations file can not be used.", JOptionPane.ERROR_MESSAGE );
						locationsProfileTextField.setText( "" );
					}; // if
				}; // if
			}; // if
		}; // if
		
		if( ALIGNMENT_MENU_ITEM.equals( actionEvent.getActionCommand() ) == true ) {
			// check that the stage works
			if( stage == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Error! Cannot do alignment when the stage is not connected!" + "\nSelect a microscope configuration.", "Unable to do alignment when stage is not connected.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			// check that the camera works
			if( camera == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Error! Cannot do alignment when the camera is not connected!" + "\nSelect a microscope configuration.", "Unable to do alignment when camera is not connected.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			// there are two cases
			if( ALIGNMENT_MENU_ITEM.equals( alignmentMenuItem.getText() ) == true ) {
				// case1: start the alignment
				statusLabel.setText( "Moving the stage to the alignment position ..." );
				alignmentMenuItem.setText( FINISH_ALIGNMENT_MENU_ITEM );
				// change core-buttons to finish-alignment-panel 
				CardLayout cardLayout = (CardLayout) coreButtonsCardLayoutPanel.getLayout();
				cardLayout.show( coreButtonsCardLayoutPanel, ALIGNMENT_PANEL );
				try {
					imageCounter = null;
					stage.moveTo( 0, 0, true, STAGE_TIMEOUT );
				}
				catch( Exception e ) {
					e.printStackTrace();
				}; // try
				camera.addEffect( alignmentEffect );
			}
			else {
				// case2: finish the alignment
				alignmentMenuItem.setText( ALIGNMENT_MENU_ITEM );
				statusLabel.setText( "Alignment completed." );
				// change core-buttons to start-cancel-panel 
				CardLayout cardLayout = (CardLayout) coreButtonsCardLayoutPanel.getLayout();
				cardLayout.show( coreButtonsCardLayoutPanel, START_CANCEL_PANEL );
				camera.removeEffect( alignmentEffect );
				moveToFocusingPosition();
			}; // if
		}; // if
		
		if( LEARN_NEW_RECORDING_LOCATIONS.equals( actionEvent.getActionCommand() ) == true ) {
			// check that the stage works
			if( stage == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Error! Cannot learn recording-locations when the stage is not connected!" + "\nSelect a microscope configuration.", "Unable to learn recording-locations when the stage is not connected.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			// check that the camera works
			if( camera == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Error! Cannot learn recording-locations when the camera is not connected!" + "\nSelect a microscope configuration.", "Unable to learn recording-locations when the camera is not connected.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			learnLocationsMenuItem.setEnabled( false );
			// change core-buttons to learning-locations-panel 
			CardLayout cardLayout = (CardLayout) coreButtonsCardLayoutPanel.getLayout();
			cardLayout.show( coreButtonsCardLayoutPanel, LEARNING_LOCATIONS_PANEL );
		}; // if
		
		if( LEARN_CURRENT_LOCATION_BUTTON.equals( actionEvent.getActionCommand() ) == true ) {
			// check that the stage works (maybe it is a redundant verification)
			if( stage == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Error! Cannot learn recording-locations when the stage is not connected!" + "\nSelect a microscope configuration.", "Unable to learn recording-locations when the stage is not connected.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			// check that the camera works (maybe it is a redundant verification)
			if( camera == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Error! Cannot learn recording-locations when the camera is not connected!" + "\nSelect a microscope configuration.", "Unable to learn recording-locations when the camera is not connected.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			if( stepsPerPixelsX == null || stepsPerPixelsY == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Bad stage/camera settings (steps-per-pixel).", "Error! Bad stage/camera settings (steps-per-pixel)", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			if( selectedPlate == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Unable to learn-locations without a plate selection.", "Error! A plate needs to be selected.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			String existingText = locationsTextArea.getText().trim();
			if( "".equals( existingText ) == true ) {
				existingText = RecordingLocation.TYPE_OF_PLATE + "\t" + selectedPlate.name();
			}; // if
			existingText += "\n";
			Integer lastPosition = getLastPosition( existingText );
			if( lastPosition == null ) {
				lastPosition = 1;
			}
			else {
				lastPosition++;
			}; // if
			Steps location = null;
			try {
				stage.updateLocation();
				location = stage.getLocation();
			}
			catch( IOException e ){
				JOptionPane.showMessageDialog( parentFrame, "Error when reading stage coordinates!" + "\nVerify that the stage is connected.", "Error when reading stage coordinates.\nDetails: " + e.getMessage(), JOptionPane.ERROR_MESSAGE );
				return;
			}; // try

			if( location == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Error when reading stage coordinates!" + "\nVerify that the stage is connected.", "Error when reading stage coordinates.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			int well = 1; // default is 6-cm plate
			if( selectedPlate.equals( PlateEnum.SIX_CM ) == false ) {
				// other well-plates need their specific calculation of which well we are in
				well = whichWellAreWePositionedIn( location.getX(), location.getY() );
				if( well == 0 ) {
					JOptionPane.showMessageDialog( parentFrame, "Unable to detect well nubmer!" + "\nMove the stage and try again.\nVerify the plate selection is the want you want.", "Unable to detect well number!", JOptionPane.WARNING_MESSAGE );
					return;
				}; // if
			}; // if
			String currentLocation = lastPosition + "\t" + well + "\t" + location.getX() + "," + location.getY();
			locationsTextArea.setText( existingText + currentLocation );
		}; // if
		
		if( FINISH_LEARNING_LOCATIONS_BUTTON.equals( actionEvent.getActionCommand() ) == true ) {
			doFinishLearningLocations();
		}; // if
		
		if( CANCEL_LEARNING_LOCATIONS_BUTTON.equals( actionEvent.getActionCommand() ) == true ) {
			learnLocationsMenuItem.setEnabled( true );
			locationsTextArea.setText( "" );
			// change core-buttons to start-cancel-panel 
			CardLayout cardLayout = (CardLayout) coreButtonsCardLayoutPanel.getLayout();
			cardLayout.show( coreButtonsCardLayoutPanel, START_CANCEL_PANEL );
		}; // if
		
		if( SHOW_CAMERA_CONTROLS_MENU_ITEM.equals( actionEvent.getActionCommand() ) == true ) {
			if( frameCamControls == null ) {
				JOptionPane.showMessageDialog( parentFrame, "Warning: \n" + "Unable to show camera-settings for 1394 Camera.:\nVerify that the camera is connected, or\nSelect a microscope-configuration (it will attempt to connect the camera).", "Warning: Unable to show 1394-camera settings.", JOptionPane.WARNING_MESSAGE );
				return;
			}; // if
			if( cameraSettingsPanel.getComponentCount() == 0 ) {
				frameCamControls.setTitle( "Camera Settings." );
				cameraSettingsPanel.add( frameCamControls );
				cameraControlsMenuItem.setText( HIDE_CAMERA_CONTROLS_MENU_ITEM );
				return;
			}; // if
			if( cameraSettingsPanel.getComponentCount() == 1 ) {
				if( HIDE_CAMERA_CONTROLS_MENU_ITEM.equals( cameraControlsMenuItem.getText() ) == true ) {
					frameCamControls.setVisible( false );
					cameraControlsMenuItem.setText( SHOW_CAMERA_CONTROLS_MENU_ITEM );
				}
				else {
					frameCamControls.setVisible( true );
					cameraControlsMenuItem.setText( HIDE_CAMERA_CONTROLS_MENU_ITEM );
				}; // if
			}; // if
		}; // if
		
	}
	

	/**
	 * Figures out which well are we positioned in
	 * @param  x  current x position
	 * @param  y  current y position
	 * @return  the well number; it returns zero when it is unable to detech the well
	 */
	private int whichWellAreWePositionedIn( long x, long y ) {
		if( selectedPlate == null ) {
			return 1;
		}; // if
		long xAdjusted = x - (long) ( DEFAULT_CAMERA_X_RESOLUTION * stepsPerPixelsX / 2.0 );
		long yAdjusted = y - (long) ( DEFAULT_CAMERA_Y_RESOLUTION * stepsPerPixelsY / 2.0 );
		int rememberWellNumber = wellNumber;
		int ret = 0;
		for( int w = 1; w <= selectedPlate.wellsTotal; w++ ) {
			wellNumber = w;
			updateXScanReady();
			if( xAdjusted > xScanReady || xAdjusted < ( xScanReady - selectedPlate.length ) ) {
				continue;
			}; // if
			updateYScanReady();
			if( yAdjusted > yScanReady || yAdjusted < ( yScanReady - selectedPlate.length ) ) {
				continue;
			}; // if
			ret = w;
		}; // for
		wellNumber = rememberWellNumber;
		return ret;
	}


	/**
	 * Selects a microscope configuration by the indicated magnification option
	 * @param  microscope  the microscope magnification
	 */
	private String doSelectMicroscopeMagnification( MicroscopeEnum microscope ) {
		String filename = folderWithMicroscopeConfigurations;
		if( filename.endsWith( File.separator ) == false ) {
			filename += File.separator;
		}; // if
		filename += microscope.configurationProfile;
		if( camera == null ) {
			out.println( "initialize camera: " + filename );
			camera1394 = null;
			String errors = loadCameraConfiguration( filename );
			if( errors != null ) {
				return errors;
			}; // if
			// Initialize the first available camera, if it was null
			if( camera == null ) {
				System.out.println( "Initializing the first available camera." );
				System.out.println( "The preferred camera was not initialized as expected." );
				String[] cameras = CameraInfo.getConnectedCameras();
				try {
					if( cameras != null && cameras.length > 0 ) {
						camera = new Camera( cameras[ 0 ] );
						// Set the default camera resolution.
						for( Dimension resolution : camera.getResolutions() ) {
							if( resolution.equals( DEFAULT_CAMERA_RESOLUTION ) ) {
								camera.setResolution( resolution );
							}; // if
						}; // for
					}
				} 
				catch( camera.CameraException e ) {
					return "Camera could not be connected ( " + e + " )";
				}; // try
			}
			else {
				out.println( "Default camera configuration is OK." );
				out.println( "Disconnect and connect again to get 1394 controls" );
				// set up camera settings for 1394 camera
				boolean reconectCMU1394 = false;
				if( CameraInfo.CMU1394.equals( camera.getCamera() ) == true ) {
					camera.disconnect();
					reconectCMU1394 = true;
				}; // if
				JDC1394Cam cam = null;
				JDC1394CamPort port = new JDC1394CamPortCMU();
				try {
					port.checkLink();
					cam = port.selectCamera( 0 );
				}
				catch( Exception e ) {
					e.printStackTrace();
				}; // try
				if( cam != null ) {
					JDC1394VideoSettings videoSettings = cam.getVideoSettings();
					if( videoSettings != null ) {
						// any brightness value read from settings?
						if( brightnessValue != null ) {
							JDC1394VideoSetting brightnessVideoSetting = videoSettings.getBrightness();
							brightnessVideoSetting.setValue1( brightnessValue );
							videoSettings.setBrightness( brightnessVideoSetting );
						}; // if
						// any sharpness value read from settings?
						if( sharpnessValue != null ) {
							JDC1394VideoSetting sharpnessVideoSetting = videoSettings.getSharpness();
							sharpnessVideoSetting.setValue1( sharpnessValue );
							videoSettings.setSharpness( sharpnessVideoSetting );
						}; // if
						// any gain value read from settings?
						if( gainValue != null ) {
							JDC1394VideoSetting gainVideoSetting = videoSettings.getGain();
							gainVideoSetting.setValue1( gainValue );
							videoSettings.setGain( gainVideoSetting );
						}; // if
					}; // if
					frameCamControls = new InternalFrameCamSettings( cam );
					frameCamControls.setVisible( true );
					frameCamControls.setTitle( "Camera Settings." );
					cameraSettingsPanel.add( frameCamControls );
					cameraControlsMenuItem.setText( HIDE_CAMERA_CONTROLS_MENU_ITEM );
					camera1394 = cam;
				}; // if
				if( frameCamControls == null ) {
					out.println( "Unable to initialize 1394 controls" ); 
				}; // if

				if( reconectCMU1394 == true ) {
					try {
						camera.setCamera( CameraInfo.CMU1394 );
					}
					catch( Exception e ) {
						e.printStackTrace();
					}; // try
				}; // if
			}; // if

			if( camera == null ) {
				return "Camera could not initialize!"; 
			}; // if
			// display the camera
			imagePanel.removeAll();
			imagePanel.add( camera.getDisplay() );
		}; // if
		
		// Initialize the stage
		if( stage == null ) {
			stage = new Stage();
			stage.addMovementListener( this );
		}; // if

		if( stage == null ) {
			return "Stage could not be initialized !";
		}; // if
		String errors = loadConfiguration( new File( filename ) );
		return errors;
	}


	/*
	 * Moves to x,y 'focusing' location (without taking a snapshot: sets image-counter to null),
	 * when stage is null, nothing is done; when there is no selected plate, nothing is done
	 */
	private void moveToFocusingPosition() {
		if( stage == null ) {
			return;
		}; // if
		if( selectedPlate == null ) {
			return;
		}; // if
		int x = xScanReady - selectedPlate.length / 4;
		int y = yScanReady - selectedPlate.length / 4;

		// focusing on 6cm plate is 0,0
		if( selectedPlate.equals( PlateEnum.SIX_CM ) == true ) {
			x = 0;
			y = 0;
		}; // if
		imageCounter = null;
		try {
			stage.moveTo( x, y, true, STAGE_TIMEOUT );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}; // try
	}

	
	// does the final step of learning locations, which is selecting a file and writing learned locations into it
	private void doFinishLearningLocations() {
		int ret = locationsProfileFileChooser.showDialog( parentFrame, "Save Locations" );
		if( ret == JFileChooser.APPROVE_OPTION ) {
			File file = locationsProfileFileChooser.getSelectedFile();
			if( file.exists() == true ) {
				// verify whether user wants to overwrite it
				String[] options = { "Yes, overwrite the existing file.", "No, go back to where I was." };
				int n = JOptionPane.showOptionDialog( parentFrame, "File already exists!\nDo you want to overwrite it?", "Caution: File already exists!", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[ 1 ] );
				if( n != 0 ) {
					return;
				}; // if
			}; // if
			// at this point, either file did not exist or user said it is OK to overwrite it
			BufferedWriter bufferedWriter = null;
			String errors = null;
			try {
				bufferedWriter = new BufferedWriter( new FileWriter( file ) );
				locationsTextArea.write( bufferedWriter );
				bufferedWriter.close();
			}
			catch( IOException ioe ) {
				errors = "Unable to write file (error: " + ioe.getMessage() + " )";
			}
			finally {
				if( bufferedWriter != null ) {
					try {
						bufferedWriter.close();
					}
					catch( IOException ioe ) {
						errors = "Unable to write file (error: " + ioe.getMessage() + " )";
					}
				}; // if
			}; // if
			if( errors == null ) {
				locationsProfileTextField.setText( file.getAbsolutePath() );
				learnLocationsMenuItem.setEnabled( true );
				locationsTextArea.setText( "" );
				// change core-buttons to start-cancel-panel 
				CardLayout cardLayout = (CardLayout) coreButtonsCardLayoutPanel.getLayout();
				cardLayout.show( coreButtonsCardLayoutPanel, START_CANCEL_PANEL );
			}
			else {
				JOptionPane.showMessageDialog( parentFrame, "Error ... \nUnable to write recording-locations into file:\n" + file.getAbsolutePath() + "\nError was: " + errors, "Error writing recording-locations file.", JOptionPane.ERROR_MESSAGE );
			}; // if
		}; // if
	}


	/**
	 * Obtains the last (numeric) position
	 * @param  text  the text, such as from text-area or text-file
	 * @return  null when unable to determine the position, otherwise the last position
	 */
	private static Integer getLastPosition( String text ) {
		if( text == null ) {
			return null;
		}; // if
		if( "".equals( text ) == true ) {
			return null;
		}; // if
		Integer position = null;
		for( String each : text.split( "\n" ) ) {
			if( each.startsWith( "#" ) == true ) {
				continue;
			}; // if
			String[] pieces = each.split( "\t" );
			position = Utilities.getInteger( pieces[ 0 ] );
		}; // for
		return position;
	}


	// execution of the 'start' button
	protected void doStartButton() {
		// hack for pausing: text is different to START_BUTTON
		if( START_BUTTON.equals( startButton.getText() ) == false ) {
			// we are in special case of 'Pausing',
			// disabling it is enough
			startButton.setEnabled( false );
			return;
		}; // if
		
		// we first do verifications on the directory-text-field
		String directory = directoryTextField.getText();
		if( directory != null ) {
			directory = directory.trim();
		}; // if
		if( directory == null || "".equals( directory ) == true ) {
			// ask user to specify directory
			JOptionPane.showMessageDialog( parentFrame, "Error: Unspecified directory!\nA directory needs to be typed or selected using 'browse'.\nPlease specify a directory, such as: " + DATA_DIRECTORY + "weiwei", "Unspecified directory", JOptionPane.ERROR_MESSAGE );
			return;
		}; // if
		
		// verify that the directory is empty or it does not exist
		File folder = new File( directory );
		if( folder.exists() == true ) {
			if( folder.isFile() == true ) {
				// we do not like it when it is a regular file
				JOptionPane.showMessageDialog( parentFrame, "Error: The directory is a regular file!\nPlease specify a directory, such as: " + DATA_DIRECTORY + "weiwei", "Error: directory cannot be a regular file.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			if( folder.isDirectory() == true && folder.list().length > 0 ) {
				// we do not like it when it is a folder and it has stuff inside
				String line = "Warning: The directory is not empty!";
				String message = "<html>Warning: <b>The directory is not empty!</b><br>"
						+ "It contains the following: <br>  ";
				int i = 0;
				for( String each : folder.list() ) {
					if( i > 30 ) {
						message += ".....";
						break;
					}
					if( i % 6 == 0 && i > 0 ) {
						message += "<br>  ";
					}; // if
					message += each + ", ";
					i++;
				}; // for
				message = message.substring( 0, message.length() - 2 ) + "<br>";
				String[] options = { "Yes, go ahead and use this directory.", "No, cancel." };
				int n = JOptionPane.showOptionDialog( parentFrame, message + "<b>Do you want to use this directory?</b>" + "</html>", line, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[ 1 ] );
				if( n == -1 || n == 1 ) {
					return;
				}; // if
			}; // if
		}; // if
		
		// verify validity of recording-locations profile, if any.
		String location = locationsProfileTextField.getText();
		if( location != null ) {
			location = location.trim();
		}; // if
		if( location != null && "".equals( location ) == false ) {
			File file = new File( location );
			// the file has to exist
			if( file.exists() == false ) {
				JOptionPane.showMessageDialog( parentFrame, "Error: The recording-locations file does not exist.\nLocation: " + file.getAbsolutePath(), "Error: Recording-locations file does not exist.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			// the file has to be a valid profile
			List<PlateEnum> typeOfPlateList = new ArrayList<PlateEnum>();
			String errors = RecordingLocation.verifyLocationsProfileValidity( file, null, typeOfPlateList );
			if( errors != null ) {
				JOptionPane.showMessageDialog( parentFrame, "Error ... \n" + errors + "\n" + "The recording-locations file can not be used.\nLocation: " + file.getAbsolutePath(), "Error: Invalid Recording-Locations File!", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			if( typeOfPlateList.size() == 0 ) {
				JOptionPane.showMessageDialog( parentFrame, "Error:\nUnable to determine the type of plate in the recording-locations profile.\nThe type of plate in the recording-locations file is missing.", "Error: Missing type of plate in recording-locations file.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			if( typeOfPlateList.size() > 0 ) {
				PlateEnum plateInLocationsProfile = typeOfPlateList.get( 0 );
				if( plateInLocationsProfile.equals( selectedPlate ) == false ) {
					JOptionPane.showMessageDialog( parentFrame, "Error:\nThe selected plate does not match the one in the recording-locations profile.\nThe type of plate in the recording-locations file is: " + plateInLocationsProfile + " .\nUnable to continue until the selection of plate-type is the same.", "Error: Type of plate in recording-locations does not match current selection.", JOptionPane.ERROR_MESSAGE );
					return;
				}; // if
			}; // if
		}; // if
		
		// make sure that a microscope-magnification is selected
		if( selectedMicroscopeConfiguration == null ) {
			JOptionPane.showMessageDialog( parentFrame, "Error, a microscope magnification needs to be selected!\n" + "Please select a microscope magnification and try again.", "Error: Microscope selection is needed!", JOptionPane.ERROR_MESSAGE );
			return;
		}; // if
		
		// make sure that a plate-type is selected
		if( selectedPlate == null ) {
			JOptionPane.showMessageDialog( parentFrame, "Error, a type of plate needs to be selected!\n" + "Please select type of plate and try again.", "Error: Plate selection is needed!", JOptionPane.ERROR_MESSAGE );
			return;
		}; // if
		
		startButton.setEnabled( false );
		disableMicroscopeMagnificationRadioButtons();
		cancelButton.setEnabled( true );
	
		( scanerWorker = new ScanWorker() ).execute(); 
	}
	

	/**
	 * Moves stage to a location and indirectly obtains a snapshot 
	 * (when the newMove listener method is called)
	 * @param  theX  the x position
	 * @param  theY  the y position
	 */
	public void moveAndTakeSnapshot( int theX, int theY ) {
		if( stage == null ) {
			System.out.println( "moveAndTakeSnapshot( " + theX + " , " + theY + " ) leaving because stage is null" );
			return;
		}; // if
		out.println( "\t["+  waitTimeBeforeSnapthot + "/" + waitTimeAfterSnapthot + "] take photo x,y " + theX + " , " + theY + "   photo# " + imageCounter + " , well: " + wellNumber );

		try {
			stage.moveTo( theX, theY, true, STAGE_TIMEOUT );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}; // try
	}

	
	/*
	 * Re-enables radio-buttons of microscope-magnification, 
	 * except the ones that have tooltip, which means that they were previously disabled 
	 */
	private void reEnableMicroscopeMagnificationRadioButtons() {
		// just in case verification
		if( selectedMicroscopeConfiguration == null ) {
			return;
		}; // if
		for( int index = 0; index < magnificationRadioButton.length; index++ ) {
			if( selectedMicroscopeConfiguration.guiText.equals( magnificationRadioButton[ index ].getText() ) == false ) {
				if( magnificationRadioButton[ index ].getToolTipText() == null ) {
					magnificationRadioButton[ index ].setEnabled( true );
				}; // if
			}; // if
		}; // for
	}


	/*
	 * Disables radio-buttons of microscope-magnification, except the one in: selectedMicroscopeConfiguration 
	 */
	private void disableMicroscopeMagnificationRadioButtons() {
		// just in case verification
		if( selectedMicroscopeConfiguration == null ) {
			return;
		}; // if
		for( int index = 0; index < magnificationRadioButton.length; index++ ) {
			if( selectedMicroscopeConfiguration.guiText.equals( magnificationRadioButton[ index ].getText() ) == false ) {
				magnificationRadioButton[ index ].setEnabled( false );
			}; // if
		}; // for
	}


	/* (non-Javadoc)
	 * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
	 */
	public void windowClosing( WindowEvent arg0 ) {
		final String[] options = { "Yes, leave now!", "No, stay!" }; 
		int n = JOptionPane.showOptionDialog( this, "Are you sure that you want to quit?", 
				"Exit WormScanner?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[ 0 ] );
		if( n == -1 || n == 1 ) { 
			return;
		}; // if
		if( scanerWorker != null ) {
			scanerWorker.cancel( true );
			scanerWorker = null;
		}; // if
		// move the stage to zero,zero position before leaving
		if( stage != null ) {
			try {
				imageCounter = null;
				stage.moveTo( 0, 0, true, STAGE_TIMEOUT );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}; // try
		}; // if
		// close the stage
		if( stage != null ) {
			stage.close();
		}; // if
		if( camera1394 != null ) {
			JDC1394VideoSettings videoSettings = camera1394.getVideoSettings();
			if( videoSettings != null ) {
				JDC1394VideoSetting brightnessVideoSetting = videoSettings.getBrightness();
				JDC1394VideoSetting sharpnessVideoSetting = videoSettings.getSharpness();
				JDC1394VideoSetting gainVideoSetting = videoSettings.getGain();
				//out.println( "#" + BRIGHTNESS + ":\t" + brightnessVideoSetting.getValue1() );
				//out.println( "#" + SHARPNESS + ":\t" + sharpnessVideoSetting.getValue1() );
				//out.println( "#" + GAIN + ":\t" + gainVideoSetting.getValue1() );
				Properties properties = new Properties();
				String error = null;
				try {
					FileInputStream fileInputStream = new FileInputStream( DEFAULT_PROPERTIES_FILENAME );
					properties.load( fileInputStream );
					fileInputStream.close();
				}
				catch( FileNotFoundException fnfe ) {
					error = "Unable to find the default settings file ( " + DEFAULT_PROPERTIES_FILENAME + " )";
				}
				catch( IOException ioe ) {
					error = "Unable to read the default settings file ( " + DEFAULT_PROPERTIES_FILENAME + " )\n" + "Details: " + ioe.getMessage();
				}; // try
				if( error != null ) {
					out.println( "Error reading properties file, " + error );
				}
				else {
					properties.setProperty( guiOption + "." + BRIGHTNESS, "" + brightnessVideoSetting.getValue1() );
					properties.setProperty( guiOption + "." + SHARPNESS, "" + sharpnessVideoSetting.getValue1() );
					properties.setProperty( guiOption + "." + GAIN, "" + gainVideoSetting.getValue1() );
					try {
						FileOutputStream fileOutputStream = new FileOutputStream( DEFAULT_PROPERTIES_FILENAME );
						properties.store( fileOutputStream, "Updated: " + new Date() );
					}
					catch( Exception e ) {
						out.println( "Error writing properties file, " + e.getMessage() );
					}; // try
				}; // if
			}; // if
		}; // if
		// turn off the camera
		if( camera != null ) {
			camera.disconnect();
		}; // if
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				parentFrame.dispose();
			}
		});
	}


	public void windowDeactivated(WindowEvent arg0) {
		// do nothing
	}


	public void windowDeiconified(WindowEvent arg0) {
		// do nothing
	}


	public void windowIconified(WindowEvent arg0) {
		// do nothing
	}


	public void windowOpened(WindowEvent arg0) {
		// do nothing
	}

	public void windowActivated(WindowEvent arg0) {
		// do nothing
	}


	public void windowClosed(WindowEvent arg0) {
		// do nothing
	}



	/** 
	 * Loads configuration and connects the stage
	 * @param  configurationFile  the configuration file
	 * @return  null if things are OK;
	 *          otherwise it returns the error message
	 */
	public String loadConfiguration( File configurationFile ) {
		SubnodeConfiguration node = null;
		try {
			XMLConfiguration configuration = new XMLConfiguration( configurationFile );
			configuration.setThrowExceptionOnMissing( true );
			node = configuration.configurationAt( "worm-tracker" );
			if( node == null ) {
				return "Node is null, it happened when loading the configuration (" + configurationFile.getAbsolutePath() + ")";
			}; // if

			ProgramConfiguration programConfiguration = new ProgramConfiguration( node );
			String version = programConfiguration.loadVersion();
			if( version == null ) {
				return "Configuration version is null";
			}; // if
			if( ProgramInfo.VERSION_COMPARATOR.compare( version, ProgramInfo.VERSION ) < 0 ) {
				System.out.println( "Configuration Warning ... The loaded configuration is out of date. You are running " + ProgramInfo.ID + " and the configuration corresponds to version " + version + "!\n Please re-save your configuration." );
			}; // if

			StageConfiguration stageConfiguration = new StageConfiguration( node );

			// Load the x-axis steps/pixels.
			try {
				stepsPerPixelsX = stageConfiguration.loadStepsPerPixelsX();
			} 
			catch( Exception e ) { 
				return "Unable to read StepsPerPixelsX, exception was: " + e; 
			}; // try

			// Load the y-axis steps/pixels.
			try {
				stepsPerPixelsY = stageConfiguration.loadStepsPerPixelsY();
			} 
			catch( Exception e ) { 
				return "Unable to read StepsPerPixelsY, exception was: " + e; 
			}; // try

			if( stage == null ) {
				return "Stage has not been initialized.";
			}; // if
			
			// Load whether we are synchronizing (i.e., waiting on) stage responses.
			try {
				boolean isSync = stageConfiguration.loadSync();
				stage.setSync( isSync );
				//System.out.println( "stage, synchronizing, waiting on, stage responses: " + isSync );
			}
			catch( Exception e ) {
				e.printStackTrace();
				return "Unable to read Sync, exception was: " + e; 
			}; // try

			// Load the timeout (in milliseconds) for synchronizing (i.e., waiting on) stage responses.
			try {
				long timeout = stageConfiguration.loadSyncTimeout();
				stage.setSyncTimeout( timeout );
				//System.out.println( "stage, syncTimeout : " + timeout );
			}
			catch( Exception e ) { 
				return "Unable to read SyncTimeout, exception was: " + e; 
			}; // try

			// Load whether we are moving the stage to absolute locations (or by relative distances).
			try {
				boolean isMoveAbsolute = stageConfiguration.loadMoveAbsolute();
				if( isMoveAbsolute == true ) {
					stage.setMoveAbsolute( true );
				}
				//System.out.println( "stage, MoveAbsolute : " + isMoveAbsolute );
			}
			catch( Exception e ) { 
				return "Unable to set or read 'MoveAbsolute', exception was: " + e; 
			}; // try

			// Load the stage type.
			MotorizedStageIdentifier id = stageConfiguration.loadType();
			if( id == null ) {
				return "Unable to load the type (stage ID)";
			}; // if
			//System.out.println( "stage, id = " + id );
			stage.setStageID( id );

			// Load the stage port.
			CommPortIdentifier port = stageConfiguration.loadPort();
			if( port == null ) {
				return "Unable to load the Port.";
			}; // if

			//System.out.println( "stage, port : " + port );
			stage.setPort( port );
		} 
		catch( StageInitializationException sie ) {
			return "Unable to initialize stage. Is the stage on?"; 
		}
		catch( Exception e ) {
			return "Configuration problem, the program configuration \""
					+ configurationFile.getAbsolutePath() + "\" cannot be loaded.\nException was: " + e.getMessage();
		}; // try

		updateScanningVariables();
		return null;
	}
	
	
	/**
	 * Updates variables used for scanning such as number of horizontal/vertical pics,
	 * depending on the type of plate selected
	 */
	public void updateScanningVariables() {
		if( stepsPerPixelsX == null ) {
			System.out.println( "Can't update scanning variables because stepsPerPixelsX is zero" );
			return;
		}; // if
		if( stepsPerPixelsY == null ) {
			System.out.println( "Can't update scanning variables because stepsPerPixelsY is zero" );
			return;
		}; // if
		//System.out.println( "---- stepsPerPixelsX,Y : " + stepsPerPixelsX + " , " + stepsPerPixelsY );

		if( selectedPlate == null ) {
			//out.println( "no plate is selected, nothing to do!" );
			return;
		}; // if

		timesHorizontally = (int) Math.ceil( selectedPlate.length / ( stepsPerPixelsX * DEFAULT_CAMERA_X_RESOLUTION ) );
		timesVertically =   (int) Math.ceil( selectedPlate.length / ( stepsPerPixelsY * DEFAULT_CAMERA_Y_RESOLUTION ) );
		//out.println( "area: " + selectedPlate.length + " ,  horizontal " + timesHorizontally + " by " + timesVertically + " vertically" );
		wellNumber = 1;
		updateXScanReady();
		updateYScanReady();
	}

	
	/** 
	 * Updates value of x-scan-ready coordinate for the current well ( wellNumber ) 
	 */
	protected void updateXScanReady() {
		if( selectedPlate == null ) {
			out.println( "no plate is selected, cannot update x-scan-ready, bye!" );
			return;
		}; // if
		if( selectedPlate.equals( PlateEnum.SIX_CM ) == true ) {
			xScanReady = (int) ( DEFAULT_CAMERA_X_RESOLUTION * ( timesHorizontally / 2.0 - 0.5 ) * stepsPerPixelsX );
			return;
		}; // if
		
		if( wellNumber > selectedPlate.wellsTotal ) {
			out.println( "SEVERE error: well number (" + wellNumber + ") is bigger than the total of wells in this plate (" + selectedPlate.wellsTotal + ")" );
			JOptionPane.showMessageDialog( parentFrame, "Severe error: well number is too big!\n" + "The well number (" + wellNumber + ") is bigger than the total of wells in this plate (" + selectedPlate.wellsTotal + ")", "Error: well number is too big!", JOptionPane.ERROR_MESSAGE );
			return;
		}; // if
		
		int xWell = wellNumber;
		while( xWell > selectedPlate.wellsInEachRow ) {
			xWell -= selectedPlate.wellsInEachRow;
		}; // while
		xScanReady = - (int) ( selectedPlate.length * ( xWell - 1 - selectedPlate.wellsInEachRow / 2.0 ) );
		// adjust for the screen resolution
		xScanReady = xScanReady - (int) ( DEFAULT_CAMERA_X_RESOLUTION * stepsPerPixelsX / 2.0 );
	}

	
	/** 
	 * Updates value of y-scan-ready coordinate for the current well ( wellNumber ) 
	 */
	protected void updateYScanReady() {
		if( selectedPlate == null ) {
			out.println( "no plate is selected, cannot update x-scan-ready, bye!" );
			return;
		}; // if
		if( selectedPlate.equals( PlateEnum.SIX_CM ) == true ) {
			yScanReady = (int) ( DEFAULT_CAMERA_Y_RESOLUTION * ( timesVertically / 2.0 - 0.5 ) * stepsPerPixelsY );
			return;
		}; // if
		
		if( wellNumber > selectedPlate.wellsTotal ) {
			out.println( "SEVERE error: well number (" + wellNumber + ") is bigger than the total of wells in this plate (" + selectedPlate.wellsTotal + ")" );
			JOptionPane.showMessageDialog( parentFrame, "Severe error: well number is too big!\n" + "The well number (" + wellNumber + ") is bigger than the total of wells in this plate (" + selectedPlate.wellsTotal + ")", "Error: well number is too big!", JOptionPane.ERROR_MESSAGE );
			return;
		}; // if
		double offset = selectedPlate.wellsInEachColumn / 2.0;
		
		yScanReady = (int) ( selectedPlate.length * ( offset - (int) Math.floor( ( wellNumber - 1 ) / selectedPlate.wellsInEachRow ) ) );
		// adjust for the screen resolution
		yScanReady = yScanReady - (int) ( DEFAULT_CAMERA_Y_RESOLUTION * stepsPerPixelsY / 2.0 );
	}


	/**
	 * Loads the camera configuration; initializes the Camera object, if possible as listed in the default configuration
	 * @param  filename  the configuration filename
	 * @return  error message if any, otherwise null means things are ok
	 */
	public String loadCameraConfiguration( String filename ) {
		File configurationFile = new File( filename );
		if( configurationFile.exists() == false ) {
			return "The configuration file does not exist (" + filename + ")";
		}; // if

		// Open the configuraton
		SubnodeConfiguration node = null;
		try {
			XMLConfiguration configuration = new XMLConfiguration( configurationFile );
			node = configuration.configurationAt( "worm-tracker" );
		} 
		catch( Exception e ) {
			return "Configuration Error - The program configuration \""
			+ configurationFile.getAbsolutePath() 
			+ "\" cannot be loaded (location: " + filename + " )";
		}; // try
			
		if( node == null ) {
			return "Unable to find camera information in the configuration file (" + filename + ")";
		}; // if

		// Load the camera.
		try {
			CameraConfiguration cameraConfiguration = new CameraConfiguration( node );
			CameraConfiguration.Display displayConfiguration = cameraConfiguration.getDisplay();
			//out.println( "----display-configuration camera id " + displayConfiguration.loadCameraId() );
			//out.println( "----display-configuration camera width " + displayConfiguration.loadResolutionWidth() );
			//out.println( "----display-configuration camera height " + displayConfiguration.loadResolutionHeight() );
			//out.println( "----display-configuration camera framerate " + displayConfiguration.loadFrameRate() );
			camera = new Camera( displayConfiguration.loadCameraId(),
							new Dimension( displayConfiguration.loadResolutionWidth(),
									displayConfiguration.loadResolutionHeight() ),
							displayConfiguration.loadFrameRate() );
		} 
		catch( Exception e ) {
			e.printStackTrace();
			return "Unable to initialize the camera; this is likely to happen when the jlib,jmf jar files are not in the classpath\nIt can also happen when the jmf.jar used is not the one created in the computer when JMF was installed\n(Exception is: " + e + ")";
		}; // try
    	camera.addRecordingStateListener(
			new RecordingStateListener() {
				public void newState( RecordingState state ) {
					recordingState = state;
					if( state == RecordingState.STOPPED ) {
						//out.println( "Camera STOPPED" );
					}
					else if( state == RecordingState.READY ) {
						//out.println( "Camera READY" );
					}
					else if( state == RecordingState.DONE ) {
						//out.println( "Camera DONE" );
					}
					else {
						//out.println( "Camera state: " + state );
					}; // if					
				}
			});
//		RecordingProgressPanel progressPanel = new RecordingProgressPanel( camera );
//    	camera.addRecordingStateListener( progressPanel );
//		final Dimension progressSize = new Dimension(550,75);
//		progressPanel.setMinimumSize(progressSize);
//		progressPanel.setPreferredSize(progressSize);
//		cameraSettingsPanel.add( progressPanel );
//		progressPanel.setVisible( true );
		return null;
	}

	
	/**
	 * Composes the directory for image scanning; it creates it when it does not exist
	 * @return  directory name; or null when there is an error
	 */
	public String getDirectoryForImageScanning() {
		String directory = directoryTextField.getText();
		if( directory.endsWith( File.separator ) == false ) {
			directory += File.separator;
		}; // if
		if( selectedPlate.equals( PlateEnum.SIX_CM ) == false ) {
			directory += wellNumber;
		}; // if
		if( actualScanningPass > 1 ) {
			directory += "__1";
		}; // if
		File folder = new File( directory );
		if( folder.exists() == false ) {
			boolean success = folder.mkdirs();
			if( success == false ) {
				JOptionPane.showMessageDialog( parentFrame, "Severe error: Unable to create folder: " + folder.getAbsolutePath() + "\n", "Severe error: Unable to create folder!", JOptionPane.ERROR_MESSAGE );
				cancelButton.doClick();
				return null;
			}; // if
		}; // if
		return directory;
	}
	
	/**
	 * Takes a snapshot (as long as imageCounter is not null)
	 */
	public void snapshot() {
		if( imageCounter == null ) {
			return;
		}; // if
		if( imageCounter == 1 ) {
			startTimeWellScanning = System.currentTimeMillis();
		}; // if

		// sleep before the taking of the photo
		try {
			Thread.sleep( waitTimeBeforeSnapthot );
		}
		catch( InterruptedException ie ) {
			// do nothing
			out.println( "Sleep in snapshot interrupted, " + ie.getMessage() );
		}; // try
		
		// verify again image-counter in case user clicked cancel-button
		if( imageCounter == null ) {
			return;
		}; // if
		
		String directory = getDirectoryForImageScanning();
		if( directory == null ) {
			return;
		}; // if
		
		String filename = "piece_" + imageCounter + ".jpeg";
		File file = new File( directory, filename );

		try {
			camera.saveImage( file, "jpeg" );
		} 
		catch( CameraException e ) {
			JOptionPane.showMessageDialog( parentFrame, "Severe error: Unable to save image to disk!\nFile:" + file.getAbsolutePath() + "\nDetails: " + e.getMessage(), "Severe error: Unable to save image!", JOptionPane.ERROR_MESSAGE );
			cancelButton.doClick();
			return;
		}; // try

		Steps location = null;
		try {
			location = stage.getLocation();
		} 
		catch( StageException e ) {
			JOptionPane.showMessageDialog( parentFrame, "Severe error: Unable to get the stage location!\nDetails: " + e.getMessage(), "Severe error: Unable to get the stage location!", JOptionPane.ERROR_MESSAGE );
			cancelButton.doClick();
			return;
		}
		// append into the log file
		File logFile = new File( directory, SCANNING_LOG_FILENAME );
		if( imageCounter == 1 ) {
			List<String> linesList = new ArrayList<String>();
			linesList.add( "#\t" + new Date() );  // time-stamp
			linesList.add( "#StepsPerPixelsX\t" + stepsPerPixelsX );
			linesList.add( "#StepsPerPixelsY\t" + stepsPerPixelsY );
			linesList.add( "#Times-Horizontal:\t" + timesHorizontally );
			linesList.add( "#Times-Vertical:\t" + timesVertically );
			linesList.add( "#Microscope-configuration:\t" + selectedMicroscopeConfiguration.guiText );
			linesList.add( "#Tracker:\t" + tracker );
			linesList.add( "#Plate-type:\t" + selectedPlate );
			linesList.add( "#Well-number:\t" + wellNumber );
			linesList.add( "#Folder:\t" + directory );
			if( camera1394 != null ) {
				JDC1394VideoSettings videoSettings = camera1394.getVideoSettings();
				if( videoSettings != null ) {
					JDC1394VideoSetting brightnessVideoSetting = videoSettings.getBrightness();
					linesList.add( "#" + BRIGHTNESS + ":\t" + brightnessVideoSetting.getValue1() );
					JDC1394VideoSetting sharpnessVideoSetting = videoSettings.getSharpness();
					linesList.add( "#" + SHARPNESS + ":\t" + sharpnessVideoSetting.getValue1() );
					JDC1394VideoSetting gainVideoSetting = videoSettings.getGain();
					linesList.add( "#" + GAIN + ":\t" + gainVideoSetting.getValue1() );
				}; // if
			}; // if
			String errors = Utilities.writeLinesToNewTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
			if( errors != null ) {
				JOptionPane.showMessageDialog( parentFrame, "Severe error: Problems writing log file!\nLog file in:" + directory + "\nDetails: " + errors, "Severe error: Problems writing log file!", JOptionPane.ERROR_MESSAGE );
				cancelButton.doClick();
				return;
			}; // if
			// special case for first image only: sleep 
			try {
				Thread.sleep( waitTimeBeforeSnapthot );
			}
			catch( InterruptedException ie ) {
				// do nothing
				out.println( "Sleep in snapshot interrupted, " + ie.getMessage() );
			}; // try
		}; // if

		String errors = Utilities.appendLinesToTextFile( logFile, new String[] { filename + "\t" + location.getX() + "\t" + location.y } );
		if( errors != null ) {
			JOptionPane.showMessageDialog( parentFrame, "Severe error: Problems writing log file!\nLog file in:" + directory + "\nDetails: " + errors, "Severe error: Problems writing log file!", JOptionPane.ERROR_MESSAGE );
			cancelButton.doClick();
			return;
		}; // if

		// sleep after taking the photo
		try {
			Thread.sleep( waitTimeAfterSnapthot );
		}
		catch( InterruptedException ie ) {
			// do nothing
			out.println( "Sleep in snapshot interrupted, " + ie.getMessage() );
		}; // try
	}

	
	/**
	 * Creates media-locator file into which video will be saved;
	 * this method was slightly modified from the WormTracker's original code;
	 * (WormTracker 2 was developed by Eviatar Yemini at the Schafer lab, http://www.mrc-lmb.cam.ac.uk/wormtracker/ )
	 * @param  filename  the filename 
	 * @param  recordingFileType  the recording file type
	 * @return  null when something went wrong; otherwise it returns the media-locator object
	 */
	private static MediaLocator createVideoFile( String filename, String recordingFileType ) {
		if( filename == null ) {
			return null;
		}; // if

		// Set the file extension.
		if (recordingFileType == FileTypeDescriptor.QUICKTIME)
			filename += ".mov";
		else if (recordingFileType == FileTypeDescriptor.MPEG)
			filename += ".mpg";
		else if (recordingFileType == FileTypeDescriptor.MSVIDEO)
			filename += AVI_EXTENSION;
		else if (recordingFileType == FileTypeDescriptor.VIVO)
			filename += ".vivo";

		// Create the file.
		try {
			File file = createFile( filename, "Video Recording", "video recording" );
			if (file != null) {
				return new MediaLocator( "file:" + file.getCanonicalPath() );
			}; // if
		} 
		catch( Exception e ) {
			JOptionPane.showMessageDialog( null, "Error: Unable to create the video.\nFile: " + filename + "\nDetails: " + e.getMessage(), "Error: Unable to create the video!", JOptionPane.ERROR_MESSAGE );
		}
		return null;
	}

	
	/**
	 * This method was taken from WormTracker's code (few very minor changes were included),
	 * (WormTracker 2 was developed by Eviatar Yemini at the Schafer lab, http://www.mrc-lmb.cam.ac.uk/wormtracker/ )
	 * @param  filename  the file name
	 * @param  title  text to use in title of dialog-windows
	 * @param  info  text to use in content of dialog-windows
	 * @return  the newly created file or null when there was error 
	 */
	private static File createFile( String filename, String title, String info ) {
		// Create the file.
		File file = new File( filename );
		try {
			if( file.createNewFile() ) {
				return file;
			}; // if
			
			// The file already exists.
			if( file.exists() == true ) {
				int choice = JOptionPane.showConfirmDialog(null, "The " + info + " \"" +
						file.getCanonicalPath() +
						"\" already exists. Would you like to overwrite it?", title + " Warning",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (choice == JOptionPane.YES_OPTION) {
					// We don't have write access.
					if (!file.canWrite())
						JOptionPane.showMessageDialog(null, "The "+ info + " \"" +
								file.getCanonicalPath() +
								"\" couldn't be saved because the program doesn't have write access.",
								title + " Error", JOptionPane.ERROR_MESSAGE);

					// Overwrite the file.
					else
						return file;
				}
			}

			// We don't have write access.
			else if (!file.canWrite())
				JOptionPane.showMessageDialog(null, "The " + info + " \"" + file.getCanonicalPath() +
						"\" couldn't be saved because the program doesn't have write access.",
						title + " Error", JOptionPane.ERROR_MESSAGE);

			// We can't create the file.
			else
				JOptionPane.showMessageDialog(null, "The " + info + " \"" + file.getCanonicalPath() +
						"\" couldn't be saved.", title + " Error", JOptionPane.ERROR_MESSAGE);

		// An IO or security exception occurred.
		} 
		catch (Exception e) {
			JOptionPane.showMessageDialog( null, "Error: Unable to create file.\nFile: " + filename + "\nDetails: " + e.getMessage(), "Error: Unable to create the file!", JOptionPane.ERROR_MESSAGE );
		}		
		return null;
	}

	
	@Override
	public void connected(Steps arg0) {
		// do nothing
	}


	@Override
	public void halt(Steps arg0) {
		// do nothing
	}


	/**
	 * This method gets called after a 'move' ; 
	 * we use it to take a snapshot or video
	 * @param  movedBy  the moved-by steps
	 * @param  movedTo  the moved-to steps position
	 */
	public void newMove( Steps movedBy, Steps movedTo ) {
		snapshot(); // takes snapshot only when image-counter > 0
	}


	@Override
	public void newRoll(double arg0, double arg1) {
		// do nothing
	}


	@Override
	public void newRollX(double arg0) {
		// do nothing
	}


	@Override
	public void newRollY(double arg0) {
		// do nothing
	}

	/**
	 * Worker that does the scanning-and-video on the background
	 */
	private class ScanWorker extends SwingWorker<String,Void> {

		// full path name of video file 
		private String videoFileLocation = null;

		@Override
		protected String doInBackground() throws Exception {
			startTimeScanWorker = System.currentTimeMillis();
			String status = null;
			if( guiOption.equals( GuiOptionsEnum.VIDEO ) == false ) {
				// we are doing image-scanning
				status = "Image-Scanning started.";
				statusLabel.setText( status );

				int totalScanTimes = twiceScanRadioButton.isSelected() ? 2 : 1;
				actualScanningPass = 1;
				do {
					// we only bother to show status when scan-times is > 1
					if( totalScanTimes > 1 ) {
						status = "Image-Scanning started. Plate will be scanned " + totalScanTimes + " times, this is the " + Utilities.intWord( actualScanningPass ) + " .";
						statusLabel.setText( status );
					}; // if
					//out.println( "scan pass " + actualScanningPass + " of " + totalScanTimes );
					do {
						// which well-number do we go to?
						if( selectedPlate.equals( PlateEnum.SIX_WELL ) == true ) {
							if( sixCheckBox[ wellNumber - 1 ].isSelected() == false ) {
								wellNumber++;
								continue;
							}; // if
						}; // if
						if( selectedPlate.equals( PlateEnum.TWELVE_WELL ) == true ) {
							if( twelveCheckBox[ wellNumber - 1 ].isSelected() == false ) {
								wellNumber++;
								continue;
							}; // if
						}; // if
						if( selectedPlate.equals( PlateEnum.TWENTY4_WELL ) == true ) {
							if( twenty4CheckBox[ wellNumber - 1 ].isSelected() == false ) {
								wellNumber++;
								continue;
							}; // if
						}; // if
						updateXScanReady();
						updateYScanReady();

						if( selectedPlate.wellsTotal > 1 ) {
							// only update the status when in a multiple-well plate
							statusLabel.setText( status + " Currently in well " + wellNumber + "." );
						}; // if

						// do we do focusing? (do not bother in the well #1 ) 
						if( yesPauseRadioButton.isSelected() == true 
						&& selectedPlate.equals( PlateEnum.SIX_CM ) == false
						&& ( wellNumber > 1 || actualScanningPass > 1 ) ) {
							//out.println( "gotta focus--- gotta focus--- gotta focus--- gotta focus--- gotta focus--- " );
							moveToFocusingPosition();
							// Pausing: wait for user to focus,
							// hack: change text of start-button, and enable it again
							startButton.setText( "Scan well # " + wellNumber );
							startButton.setEnabled( true );
							// the condition to continue is that start-button is disabled and its text is not START_BUTTON
							do {
								try { 
									Thread.sleep( 100 );
								}
								catch( InterruptedException ie ) {
									// do nothing
								}; // try
								if( isCancelled() == true ) {
									return "Image-Scanning has been cancelled";
								}; // if
							} while( startButton.isEnabled() == true && START_BUTTON.equals( startButton.getText() ) == false );
						}; // if
						
						// scanning takes place
						// out.println( "------scan well " + wellNumber + " of a total of " + selectedPlate.wellsTotal );
						// the movement and photo snaps happen in this loop
						for( int vertical = 0; vertical < timesVertically; vertical++ ) {
							int theY = yScanReady - (int) ( vertical * DEFAULT_CAMERA_Y_RESOLUTION * stepsPerPixelsY );
							if( vertical % 2 == 0 ) {
								//out.println("// takes photos from left to right" );
								for( int horizontal = 0; horizontal < timesHorizontally; horizontal++ ) {
									int theX = xScanReady - (int) ( horizontal * DEFAULT_CAMERA_X_RESOLUTION * stepsPerPixelsX );
									imageCounter = vertical * timesHorizontally + 1 + horizontal;
									if( isCancelled() == true ) {
										imageCounter = null;
										return "Image-Scanning has been cancelled"; 
									}; // if
									moveAndTakeSnapshot( theX, theY );
								}; // for
							}
							else {
								for( int horizontal = ( timesHorizontally - 1 ); horizontal >= 0; horizontal-- ) {
									int theX = xScanReady - (int) ( horizontal * DEFAULT_CAMERA_X_RESOLUTION * stepsPerPixelsX );
									imageCounter = vertical * timesHorizontally + 1 + horizontal;
									if( isCancelled() == true ) {
										imageCounter = null;
										return "Image-Scanning has been cancelled";
									}; // if
									moveAndTakeSnapshot( theX, theY );
								}; // for
							}; // if
						}; // for
						String directory = getDirectoryForImageScanning();
						if( directory == null ) {
							return "Problem in the directory into which to write data";
						}; // if
						File logFile = new File( directory, SCANNING_LOG_FILENAME );
						long totalTime = System.currentTimeMillis() - startTimeWellScanning;
						String errors = Utilities.appendLinesToTextFile( logFile, new String[] { "#time(seconds)\t" + Math.round( totalTime / 1000.0 ) } );
						if( errors != null ) {
							return "Severe error: Problems writing log file!\nLog file in:" + directory + "\nDetails: " + errors;
						}; // if
						
						wellNumber++;
					} while( wellNumber <= selectedPlate.wellsTotal );

					wellNumber = 1;
					updateXScanReady();
					updateYScanReady();
					
					// the following line helps detect a change of scan-times in the GUI
					totalScanTimes = twiceScanRadioButton.isSelected() ? 2 : 1;
					// do we have more scanning to do, if so, check whether we have to wait seconds-to-wait-between-scans
					if( actualScanningPass < totalScanTimes ) {
						moveToFocusingPosition();
						// verify that 2 minutes have lapsed, otherwise wait
						long endTime = System.currentTimeMillis();
						long seconds = endTime / 1000 - startTimeScanWorker / 1000;
						while( seconds < ( (Integer) secondsBetweenScansTextField.getValue() ) ) {
							status = "Image-Scanning started. Plate will be scanned " + totalScanTimes + " times.";
							status += " Waiting " + ( ( (Integer) secondsBetweenScansTextField.getValue() ) - seconds ) + " seconds before scanning the " + Utilities.intWord( actualScanningPass + 1 ) + " time.";
							statusLabel.setText( status );
							try { 
								Thread.sleep( 1000 );
							}
							catch( InterruptedException ie ) {
								// do nothing
							}; // try
							endTime = System.currentTimeMillis();
							seconds = endTime / 1000 - startTimeScanWorker / 1000;
							if( isCancelled() == true ) {
								return "Image-Scanning has been cancelled";
							}; // if
						}; // while
					}; // if
					actualScanningPass++;
				} while( actualScanningPass <= totalScanTimes );

			}; // if
			
			if( guiOption.equals( GuiOptionsEnum.PICTURES ) == false ) {
				// we are doing video-recording
				long videoRecordingStartingTime = System.currentTimeMillis();
				status = "Video-Recording started.";
				statusLabel.setText( status );
				String locationOfRecordingLocations = locationsProfileTextField.getText();
				if( locationOfRecordingLocations != null ) {
					locationOfRecordingLocations = locationOfRecordingLocations.trim();
				}; // if
				imageCounter = null;  // making sure we do not take any more photos on stage movements
				// these log-lines are in common for the two cases of video-recording
				List<String> commonLogLinesList = new ArrayList<String>();
				commonLogLinesList.add( "#Timestamp:\t" + new Date() );  // time-stamp
				commonLogLinesList.add( "#StepsPerPixelsX\t" + stepsPerPixelsX );
				commonLogLinesList.add( "#StepsPerPixelsY\t" + stepsPerPixelsY );
				commonLogLinesList.add( "#Microscope-configuration:\t" + selectedMicroscopeConfiguration.guiText );
				commonLogLinesList.add( "#Tracker:\t" + tracker );
				commonLogLinesList.add( "#Plate-type:\t" + selectedPlate );
				commonLogLinesList.add( "#Frame-Rate:\t" + camera.getRecordingFrameRate() );
				if( camera1394 != null ) {
					JDC1394VideoSettings videoSettings = camera1394.getVideoSettings();
					if( videoSettings != null ) {
						JDC1394VideoSetting brightnessVideoSetting = videoSettings.getBrightness();
						commonLogLinesList.add( "#" + BRIGHTNESS + ":\t" + brightnessVideoSetting.getValue1() );
						JDC1394VideoSetting sharpnessVideoSetting = videoSettings.getSharpness();
						commonLogLinesList.add( "#" + SHARPNESS + ":\t" + sharpnessVideoSetting.getValue1() );
						JDC1394VideoSetting gainVideoSetting = videoSettings.getGain();
						commonLogLinesList.add( "#" + GAIN + ":\t" + gainVideoSetting.getValue1() );
					}; // if
				}; // if
				// there are two cases depending on the value of location-profile-text-field
				if( locationOfRecordingLocations != null && "".equals( locationOfRecordingLocations ) == false ) {
					// case1: it contains a recording-locations profile
					File file = new File( locationOfRecordingLocations );
					// the file has to exist
					if( file.exists() == false ) {
						JOptionPane.showMessageDialog( parentFrame, "Error: The recording-locations file does not exist.\nLocation: " + file.getAbsolutePath() + "\nUnable to do video-recording.", "Error: Recording-locations file does not exist.", JOptionPane.ERROR_MESSAGE );
						return "Error: Recording-locations file does not exist.";
					}; // if
					List<RecordingLocation> list = new ArrayList<RecordingLocation>();
					List<PlateEnum> typeOfPlateList = new ArrayList<PlateEnum>();
					// get the recording locations from the text-file
					String errors = RecordingLocation.verifyLocationsProfileValidity( file, list, typeOfPlateList );
					// the file has to be a valid profile
					if( errors != null ) {
						JOptionPane.showMessageDialog( parentFrame, "Error ... \n" + errors + "\n" + "The recording-locations file can not be used.\nLocation: " + file.getAbsolutePath() + "\nUnable to do video-recording.", "Error: Invalid Recording-Locations File!", JOptionPane.ERROR_MESSAGE );
						return "Error: Invalid Recording-Locations File!";
					}; // if
					// the following verification maybe redundant but just in case (it was verified in doStartButton)
					if( typeOfPlateList.size() == 0 ) {
						JOptionPane.showMessageDialog( parentFrame, "Error:\nUnable to determine the type of plate in the recording-locations profile.\nThe type of plate in the recording-locations file is missing.", "Error: Missing type of plate in recording-locations file.", JOptionPane.ERROR_MESSAGE );
						return "Missing type of plate in recording-locations file.";
					}; // if
					// the following verification maybe redundant but just in case (it was verified in doStartButton)
					if( typeOfPlateList.size() > 0 ) {
						PlateEnum plateInLocationsProfile = typeOfPlateList.get( 0 );
						if( plateInLocationsProfile.equals( selectedPlate ) == false ) {
							JOptionPane.showMessageDialog( parentFrame, "Error:\nThe selected plate does not match the one in the recording-locations profile.\nThe type of plate in the recording-locations file is: " + plateInLocationsProfile + " .\nUnable to continue until the selection of plate-type is the same.", "Error: Type of plate in recording-locations does not match current selection.", JOptionPane.ERROR_MESSAGE );
							return "Type of plate in recording-locations does not match current selection.";
						}; // if
					}; // if

					// prepare the destination directory
					String directory = directoryTextField.getText();
					if( directory.endsWith( File.separator ) == false ) {
						directory += File.separator;
					}; // if
					File folder = new File( directory );
					if( folder.exists() == false ) {
						boolean success = folder.mkdirs();
						if( success == false ) {
							JOptionPane.showMessageDialog( parentFrame, "Severe error: Unable to create folder: " + folder.getAbsolutePath() + "\n", "Severe error: Unable to create folder!", JOptionPane.ERROR_MESSAGE );
							return "Unable to create folder: " + folder.getAbsolutePath();
						}; // if
					}; // if


					// do the recording on the locations
					for( RecordingLocation recLocation : list ) {
						boolean failedVideo = false;
						do {
							failedVideo = false;
							videoRecordingStartingTime = System.currentTimeMillis();  // every video gets its time measured
						
							// prepare the log file
							String videoDirectory = directory + "video_well_" + recLocation.well;
							File logFile = new File( videoDirectory + File.separator + "video_" + recLocation.position + ".txt" );
							
							List<String> linesList = new ArrayList<String>();
							for( String each : commonLogLinesList ) {
								linesList.add( each );
							}; // for
							//out.println( "\t\tloc: " + recLocation.position + " well " + recLocation.well );
							linesList.add( "Recording-locations-file:\t" + locationOfRecordingLocations );
							linesList.add( "Position(from-recording-locations-file):\t" + recLocation.position );
							linesList.add( "Well-number:\t" + recLocation.well );
							// verify whether the location is to be skipped, based on well number
							if( selectedPlate.equals( PlateEnum.SIX_WELL ) == true ) {
								if( sixCheckBox[ recLocation.well - 1 ].isSelected() == false ) {
									continue;
								}; // if
							}; // if
							if( selectedPlate.equals( PlateEnum.TWELVE_WELL ) == true ) {
								if( twelveCheckBox[ recLocation.well - 1 ].isSelected() == false ) {
									continue;
								}; // if
							}; // if
							if( selectedPlate.equals( PlateEnum.TWENTY4_WELL ) == true ) {
								if( twenty4CheckBox[ recLocation.well - 1 ].isSelected() == false ) {
									continue;
								}; // if
							}; // if
							// move the stage to the location
							try {
								statusLabel.setText( status + " Location #" + recLocation.position + " (of " + list.size() + "). Well " + recLocation.well + "." );
								stage.moveTo( recLocation.xCoordinate, recLocation.yCoordinate, true, STAGE_TIMEOUT );
								//out.println( "\t\t\tmove stage to " + recLocation.xCoordinate + " , " + recLocation.yCoordinate );
							}
							catch( Exception e ) {
								if( isCancelled() == true ) {
									// write to the log, display error message and leave
									out.println( "Video-Recording has been cancelled, waiting one second, " + e.getMessage() );
									linesList.add( "#Operation canceled by user!" );
									errors = Utilities.writeLinesToNewTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
									if( errors != null ) {
										JOptionPane.showMessageDialog( parentFrame, "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors, "Error: Problems writing log file for video!", JOptionPane.ERROR_MESSAGE );
									}; // if
									return "Video-Recording has been cancelled";
								}; // if
								e.printStackTrace();
								// write to the log, display error message and leave
								linesList.add( "#Problems when moving to recording locations, error:\t" + e.getMessage() );
								errors = Utilities.writeLinesToNewTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
								if( errors != null ) {
									JOptionPane.showMessageDialog( parentFrame, "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors, "Error: Problems writing log file for video!", JOptionPane.ERROR_MESSAGE );
								}; // if
								JOptionPane.showMessageDialog( parentFrame, "Exception in move-to-recording-location\n" + e.getMessage() + "\nlocation: " + recLocation );
								return "Error while moving to recording-locations, error: " + e.getMessage();
							}; // try
							// do we do focusing? 
							if( yesPauseRadioButton.isSelected() == true ) {
								//out.println( "focus---  focus---  focus---  focus---  focus--- " );
								// Pausing: wait for user to focus,
								// hack: change text of start-button, and enable it again
								startButton.setText( "Record location " + recLocation.position );
								startButton.setEnabled( true );
								// the condition to continue is that start-button is disabled and its text is not START_BUTTON
								do {
									try { 
										Thread.sleep( 100 );
									}
									catch( InterruptedException ie ) {
										// do nothing
									}; // try
									if( isCancelled() == true ) {
										// write to the log, display error message and leave
										linesList.add( "#Operation canceled by user!" );
										errors = Utilities.writeLinesToNewTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
										if( errors != null ) {
											JOptionPane.showMessageDialog( parentFrame, "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors, "Error: Problems writing log file for video!", JOptionPane.ERROR_MESSAGE );
										}; // if
										return "Video-Recording has been cancelled";
									}; // if
								} while( startButton.isEnabled() == true && START_BUTTON.equals( startButton.getText() ) == false );
							}
							else {
								// Pausing for 1/5 second before taking the video
								try { 
									Thread.sleep( 500 );
								}
								catch( InterruptedException ie ) {
									// do nothing
								}; // try
								if( isCancelled() == true ) {
									// write to the log, display error message and leave
									linesList.add( "#Operation canceled by user!" );
									errors = Utilities.writeLinesToNewTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
									if( errors != null ) {
										JOptionPane.showMessageDialog( parentFrame, "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors, "Error: Problems writing log file for video!", JOptionPane.ERROR_MESSAGE );
									}; // if
									return "Video-Recording has been cancelled";
								}; // if
							}; // if
							
							// prepare the destination folder for the video
							File videoFolder = new File( videoDirectory );
							if( videoFolder.exists() == false ) {
								boolean success = videoFolder.mkdirs();
								if( success == false ) {
									// write to the log, display error message and leave
									linesList.add( "#Unable to create folder:\t" + videoFolder.getAbsolutePath() );
									errors = Utilities.writeLinesToNewTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
									JOptionPane.showMessageDialog( parentFrame, "Severe error: Unable to create folder: " + videoFolder.getAbsolutePath() + "\n", "Severe error: Unable to create folder!", JOptionPane.ERROR_MESSAGE );
									if( errors != null ) {
										JOptionPane.showMessageDialog( parentFrame, "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors, "Error: Problems writing log file for video!", JOptionPane.ERROR_MESSAGE );
									}; // if
									return "Unable to create folder: " + folder.getAbsolutePath();
								}; // if
							}; // if
						
							// record the video
							videoFileLocation = videoDirectory + File.separator + "video_" + recLocation.position;
							linesList.add( "Video-filename:\t" + videoFileLocation + AVI_EXTENSION );
							String recordingError = record( linesList );
							if( recordingError != null ) {
								// write to the log, display error message and leave
								linesList.add( "Recording-error:\t" + recordingError.replace( '\n', ' ' ) );
								errors = Utilities.writeLinesToNewTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
								JOptionPane.showMessageDialog( parentFrame, "Error during video recording.\n" + recordingError, "Error during video recording!", JOptionPane.ERROR_MESSAGE );
								if( errors != null ) {
									JOptionPane.showMessageDialog( parentFrame, "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors, "Error: Problems writing log file for video!", JOptionPane.ERROR_MESSAGE );
								}; // if
								return "Error during video recording.\n" + recordingError;
							}; // if
	
							// last thing to do is write the log
							long totalTime = ( System.currentTimeMillis() - videoRecordingStartingTime ) / 100;
							linesList.add( "#Time of recording this video including overhead (seconds):\t" + ( totalTime / 10.0 ) );
							errors = Utilities.writeLinesToNewTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
							if( errors != null ) {
								JOptionPane.showMessageDialog( parentFrame, "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors, "Error: Problems writing log file for video!", JOptionPane.ERROR_MESSAGE );
							}; // if
	
							// verify whether the video size is at least 40KB * seconds
							//HEREEEE
							out.println( "Verify: " + videoFileLocation + AVI_EXTENSION );
							File tmpFile = new File( videoFileLocation + AVI_EXTENSION );
							if( tmpFile.exists() == false ) {
								failedVideo = true;
							}
							else {
								if( tmpFile.canRead() == false ) {
									failedVideo = true;
								}
								else {
									if( tmpFile.isFile() == false ) {
										failedVideo = true;
									}
									else {
										out.println( "\t" + tmpFile.length() );
										long miliSecondsValue = videoTimePanel.getTime();
										out.println( "\t" + miliSecondsValue );
										// mili seconds is a rough estimate to find out videos that fail
										if( tmpFile.length() < ( miliSecondsValue * 10 ) ) {
											failedVideo = true;
										}; // if
									}; // if
								}; // if
							}; // if
							if( failedVideo == true ) {
								out.println( "video is not valid!, recording it again." );
								// see whether video can be deleted.
								tmpFile = new File( videoFileLocation + AVI_EXTENSION );
								if( tmpFile.exists() == true ) {
									tmpFile.delete();
								}; // if
							}; // if
						} while( failedVideo == true );
					}; // for
					
				}
				else {
					// case 2: recording wherever the stage is located right now, only one video
					String directory = directoryTextField.getText();
					if( directory.endsWith( File.separator ) == false ) {
						directory += File.separator;
					}; // if
					File folder = new File( directory );
					if( folder.exists() == false ) {
						boolean success = folder.mkdirs();
						if( success == false ) {
							JOptionPane.showMessageDialog( parentFrame, "Severe error: Unable to create folder: " + folder.getAbsolutePath() + "\n", "Severe error: Unable to create folder!", JOptionPane.ERROR_MESSAGE );
							return "Unable to create folder: " + folder.getAbsolutePath();
						}; // if
					}; // if
					Date today = Calendar.getInstance().getTime();
					SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd-hh-mm-ss" );
					videoFileLocation = directory + "video_" + formatter.format( today );
					// first write log-file, in this case it is named similarly as the video but with .txt extension
					List<String> linesList = new ArrayList<String>();
					for( String each : commonLogLinesList ) {
						linesList.add( each );
					}; // for
					linesList.add( "Well-number:\t" + wellNumber );
					linesList.add( "Video-filename:\t" + videoFileLocation + AVI_EXTENSION );
					File logFile = new File( videoFileLocation + ".txt" );
					String errors = Utilities.writeLinesToNewTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
					if( errors != null ) {
						JOptionPane.showMessageDialog( parentFrame, "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors, "Error: Problems writing log file for video!", JOptionPane.ERROR_MESSAGE );
						return "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors;
					}; // if
					linesList.clear();
					String recordingError = record( linesList );
					// after recording ends, append time it took to the log
					long totalTime = ( System.currentTimeMillis() - videoRecordingStartingTime ) / 100;
					linesList.add( "#Time of recording this video including overthead (seconds):\t" + ( totalTime / 10.0 ) );
					if( recordingError != null ) {
						linesList.add( "Recording-error:\t" + recordingError.replace( '\n', ' ' ) );
					}; // if
					// log the lines regardless of any error, as long as there is anything to log
					errors = Utilities.appendLinesToTextFile( logFile, linesList.toArray( EMPTY_STRING_ARRAY ) );
					if( errors != null ) {
						JOptionPane.showMessageDialog( parentFrame, "Error: Problems writing log file for video!\nLog file in:" + directory + "\nDetails: " + errors, "Error: Problems writing log file for video!", JOptionPane.ERROR_MESSAGE );
						// no return here because there might be a recording-error and because there are no critical lines of code that follow
					}; // if
					if( recordingError != null ) {
						JOptionPane.showMessageDialog( parentFrame, "Error during video recording.\n" + recordingError, "Error during video recording!", JOptionPane.ERROR_MESSAGE );
						return "Error during video recording.\n" + recordingError;
					}; // if
				}; // if
			}; // if
			
			return null;
		}
		

		/**
		 * Records a video at the current position (video filename starts with: videoFileLocation)
		 * @param  logList  list into which log messages will be added
		 * @return  null when things go OK; otherwise it returns an error message
		 */
		private String record( List<String> logList ) {
			// get the recording-time (seconds)
			long miliSecondsValue = videoTimePanel.getTime();
			if( 0 == miliSecondsValue ) {
				return "Unable to record video!\nTime in seconds is not valid (time: " + miliSecondsValue + " )";
			}; // if

			// quick sleep before reading stage location
			try {
				Thread.sleep( 100 );
			}
			catch( Exception e ) {
				out.println( "Interrupted in sleep before checking state location, " + e.getMessage() );
				return "Interrupted in sleep before checking state location, " + e.getMessage();
			}; // try

			Steps location = null;
			try {
				stage.updateLocation();
				location = stage.getLocation();
			}
			catch( IOException e ){
				e.printStackTrace();
				return "Unable to get the stage location!\nDetails: " + e.getMessage();
			}; // try

			if( location == null ) {
				return "Unable to obtain the stage location!";
			}; // if
					
			logList.add( "Stage,x:\t" + location.getX() );
			logList.add( "Stage,y:\t" + location.getY() );
			logList.add( "Video-duration(seconds)\t" + ( miliSecondsValue / 1000 ) );
			
			// Start the recording.
			final MediaLocator file = createVideoFile( videoFileLocation, RECORDING_FILE_TYPE );
			if( file == null ) {
				return "Unable to create media-locator file!";
			}; // if
			final VideoLength length = new TimeLength( videoTimePanel.getTime() );  // miliseconds
			try {
				// the recording goes on its own thread, we have to monitor it via the recording-status
				camera.record( file, new FileTypeDescriptor( RECORDING_FILE_TYPE ), RECORDING_FORMAT, length );
			} 
			catch( CameraException e ) {
				return "Unable to record video!\nFile: " + videoFileLocation + "\nDetails: " + e.getMessage();
			}; // try

			long timeVideoStarted = System.currentTimeMillis();
			recordingState = null;
			do {
				// wait for the video to finish recording
				try { 
					Thread.sleep( 1000 );  // half-second
				}
				catch( InterruptedException ie ) {
					out.println( "Interrupted when waiting for video, " + ie.getMessage() );
					// do nothing because it could be a cancel-click from user
				}; // try
				if( recordingState == RecordingState.DONE ) {
					return null;
				}; // if
				//out.println( " -waited-so-far " + ( ( System.currentTimeMillis() - timeVideoStarted ) / 1000 ) + " seconds " );
				if( isCancelled() == true ) {
					camera.stopRecording();
					recordingState = null;
					logList.add( "Recording was cancelled by user. Actual video-duration(seconds) :\t" + ( ( System.currentTimeMillis() - timeVideoStarted ) / 1000 ) );
					return null;  // cancellation by user is not exactly an error, hence we return null
				}; // if
				// exit condition: maximum wait is video-duration + 4 seconds
			} while( ( ( System.currentTimeMillis() - timeVideoStarted ) / 1000 ) < ( miliSecondsValue / 1000 + 4 ) );
			// verification, just in case
			if( recordingState == RecordingState.DONE ) {
				return null;
			}; // if
			return "Recording timeout indicates that recording did fail";
		}
		
		@Override
		protected void done() {
			super.done();
			try {
				int howlong = isCancelled() == true ? 3000 : 1000;
				//out.println( "waiting " + ( howlong / 1000 ) + " seconds in 'done'" );
				Thread.sleep( howlong );
			}
			catch( Exception e ) {
				out.println( "This happened while waiting in 'done', " + e.getMessage() );
			}; // try
			try{
				startButton.setEnabled( true );
				startButton.setText( START_BUTTON );
				cancelButton.setEnabled( false );
				reEnableMicroscopeMagnificationRadioButtons();
				if( isCancelled() == true ) {
					statusLabel.setText( "Cancelled by user!" );
				}
				else {
					long endTime = System.currentTimeMillis();
					long seconds = endTime / 1000 - startTimeScanWorker / 1000;
					statusLabel.setText( "Completed. " + guiOption.guiText + " took " + seconds + " seconds." );
					JOptionPane.showMessageDialog( parentFrame, "Completed " + guiOption.guiText + "." );
				}; // if
				// leave things same as they where, but wait in case stage is still moving (such as when canceled by user)
				wellNumber = 1;
				updateXScanReady();
				updateYScanReady();
				// in only one situation, we do not move the stage
				if( guiOption.equals( GuiOptionsEnum.VIDEO ) == true && "".equals( locationsProfileTextField.getText().trim() ) == true ) {
					// do nothing
					//out.println( "// in only one situation, we do not move the stage." );
				}
				else {
					moveToFocusingPosition();
				}; // if
			}
			catch( Exception ignore ) {
				ignore.printStackTrace();
			}; // try
		}
	}

	// other 1394-camera controls/functions, not currently used
	// DialogChooseMode d = new DialogChooseMode(cam);
} // class ScanningAndRecording
