/*
 * Filename: App.java
 * This is the main class which shows the main application window
 * containing 'Analzye One Video' and 'Batch Processing' buttons 
 */
package edu.rice.wormlab.locomotionassay;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App extends JPanel implements ActionListener {

    // serial version UID
    private static final long serialVersionUID = 1L;
    /**
     * the version of this software
     */
    public static final String VERSION = "WormLocomotion 9/19/2013";
    public static final String DETECTION_CONDITION_PRESET_FILENAME =
                         "Locomotion_Detection_Conditon_Preset.txt";    

    
    protected static final String ANALYZE_ONE_VIDEO = "Analyze One Video";
    protected static final String BATCH_PROCESSING = "Batch Processing";
    private final JFrame parent;
    protected final JFileChooser fileChooser;
    protected final JFileChooser videoFileChooser;
    protected final BatchTracking batchTracking;
    public DetectionCondition detectionCondition = new DetectionCondition();
    
    /**
     * Constructor
     *
     * @param parent the parent frame
     */
    public App(JFrame parent) {
        this.parent = parent;
        // buttons from left to right
      
        JButton batchProcessingButton = new JButton(BATCH_PROCESSING);
        batchProcessingButton.setActionCommand(BATCH_PROCESSING);
        batchProcessingButton.addActionListener(this);
        batchProcessingButton.setMnemonic(KeyEvent.VK_B);

        JButton analyzeOneVideoButton = new JButton(ANALYZE_ONE_VIDEO);
        analyzeOneVideoButton.setActionCommand(ANALYZE_ONE_VIDEO);
        analyzeOneVideoButton.addActionListener(this);
        analyzeOneVideoButton.setMnemonic(KeyEvent.VK_O);

        File logoFile = new File( "images" + File.separator + "logo.png" );
        ImageIcon logoImageIcon = null;
        if( logoFile.exists() == false ) {
           logoFile = new File( "logo.png" );
        }; // if
        if( logoFile.exists() == false ) {
           logoFile = new File( ".." + File.separator + "images" + File.separator + "logo.png" );
        }; // if
        if( logoFile.exists() == true ) {
        	logoImageIcon = new ImageIcon( logoFile.getAbsolutePath() , VERSION);
        }; // if
        JLabel logoLabel = null;
        if( logoImageIcon == null ) {
        	logoLabel = new JLabel( "" );
        }
        else {
        	logoLabel = new JLabel(logoImageIcon);
        	this.parent.setIconImage(logoImageIcon.getImage());
        }; // if

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(logoLabel);

        add(Box.createRigidArea(new Dimension(20, 0)));
        add(analyzeOneVideoButton);
        add(Box.createRigidArea(new Dimension(20, 1)));
        add(batchProcessingButton);
        
        
        //Load detection parameter setting profiles
        String[] titleArray = detectionCondition.get_DetectionConditionPreset_Titles(
                DETECTION_CONDITION_PRESET_FILENAME);
		  if( titleArray == null ) {
			  // attempt to load it from above folder 
			  titleArray = detectionCondition.get_DetectionConditionPreset_Titles(
                ".." + File.separator + DETECTION_CONDITION_PRESET_FILENAME);
		  }; // if 
        
        
        Tracker.detectionCondition =
                    detectionCondition.set_DetectionConditionPreset(
                    DETECTION_CONDITION_PRESET_FILENAME,
                    titleArray[0]);
		  if( Tracker.detectionCondition == null ) {
        		// attempt to load it from above folder
        		Tracker.detectionCondition =
                    detectionCondition.set_DetectionConditionPreset(
                    ".." + File.separator + DETECTION_CONDITION_PRESET_FILENAME,
                    titleArray[0]);
		  }; // if
        
        
        if ("/".equals(File.separator) == true) {
            fileChooser = new JFileChooser( System.getProperty( "user.home" ) );
            videoFileChooser = new JFileChooser( System.getProperty( "user.home" ) );
        } else {
            fileChooser = new JFileChooser("c:\\data");
            videoFileChooser = new JFileChooser("c:\\data");
        }; // if
        videoFileChooser.setFileFilter(new VideoFileFilter());
        videoFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        batchTracking = new BatchTracking(this.parent);
    }



	/**
	 * Create the GUI and show it
	 */
	private static void createAndShowGUI() {
        JFrame frame = new JFrame(VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // create the actual object (of the class App)
        App app = new App(frame);
        app.setOpaque(true);
        frame.setContentPane(app);

        // display the window
        frame.pack();
        frame.setVisible(true);
    }



      
     
     
	/** 
	 * Actions of buttons take place here
	 * @param  actionEvent  the action-event object
	 */
	public void actionPerformed(ActionEvent actionEvent) {
       
        if (ANALYZE_ONE_VIDEO.equals(actionEvent.getActionCommand()) == true) {
            int returnValue = videoFileChooser.showOpenDialog(this);
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                return;
            }; // if
            File video = videoFileChooser.getSelectedFile();

            Tracker tracker = new Tracker(video, false, true);
            Thread thread = new Thread(tracker);
            thread.start();
        };
        
         if (BATCH_PROCESSING.equals(actionEvent.getActionCommand()) == true) {
            batchTracking.go(fileChooser);
        };
        
        if (actionEvent.getActionCommand().startsWith("DCPreset:") == true) {
                Tracker.detectionCondition =
                    detectionCondition.set_DetectionConditionPreset(
                    DETECTION_CONDITION_PRESET_FILENAME,
                    actionEvent.getActionCommand().substring(9));
        }        
    }

	

	/** Runs the application via a runnable invocation */
	public static void main(String[] args) {
        // want windows look-and-feel
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            // do nothing
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }

} // class App

