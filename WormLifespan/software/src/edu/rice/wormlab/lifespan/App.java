/*
 * Filename: App.java
 * This is the main class which shows the application window
 * containing 'Image Processing','Manual Inspection, and 'Print Report' buttons
 */
package edu.rice.wormlab.lifespan;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App extends JPanel implements ActionListener {

    // serial version UID
    private static final long serialVersionUID = 11L;
    /** the version of this software */
    public static final String VERSION = "9/18/2013";
    public static final String TITLE = "WormLifespan " + VERSION;
    protected static final String IMAGE_PROCESSING = "Image Processing";
    protected static final String MANUAL_INSPECTION = "Manual Inspection";
    protected static final String PRINT_REPORT = "Print Report";
    public static final String UNDERSCORE_UNDERSCORE_ONE = "__1";
    private final JFrame parent;
    protected final JFileChooser fileChooser;
    protected final MotionDetection motionDetection;
    public static final String DETECTION_CONDITION_PRESET_FILENAME =
            "Lifespan_Detection_Conditon_Preset.txt";
    public DetectionCondition detectionCondition = new DetectionCondition();

    /**
     * Constructor
     *
     * @param parent the parent frame
     */
    public App(JFrame parent) {
        this.parent = parent;
        
        //Define window
        setLayout(null);
        parent.setLocation(150, 100);
        parent.setMinimumSize(new Dimension(720, 190));
        parent.setResizable(false);


        // Add icon image
        ImageIcon logoImageIcon = null;
        File imageFile = new File("images" + File.separator + "logo.gif");
        if (imageFile.exists() == true) {
            logoImageIcon = new ImageIcon(imageFile.getAbsolutePath(), VERSION);
        } else {
            URL imageUrl = getClass().getResource("/logo.gif");
            Image image = Toolkit.getDefaultToolkit().getImage(imageUrl);
            logoImageIcon = new ImageIcon(image, VERSION);
        }
        JLabel logoLabel = new JLabel(logoImageIcon);
        this.parent.setIconImage(logoImageIcon.getImage());
        logoLabel.setLocation(5, 5);
        logoLabel.setSize(128, 128);
        add(logoLabel);


        // add buttons
        JButton imageProcessingButton = new JButton(IMAGE_PROCESSING);
        imageProcessingButton.setActionCommand(IMAGE_PROCESSING);
		  imageProcessingButton.setMnemonic( KeyEvent.VK_I );
        imageProcessingButton.addActionListener(this);
        imageProcessingButton.setSize(150, 30);
        imageProcessingButton.setLocation(150, 50);
        add(imageProcessingButton);

        JButton manualInspectionButton = new JButton(MANUAL_INSPECTION);
        manualInspectionButton.setActionCommand(MANUAL_INSPECTION);
		  manualInspectionButton.setMnemonic( KeyEvent.VK_M );
        manualInspectionButton.addActionListener(this);
        manualInspectionButton.setSize(150, 30);
        manualInspectionButton.setLocation(320, 50);
        add(manualInspectionButton);

        JButton printReportButton = new JButton(PRINT_REPORT);
        printReportButton.setActionCommand(PRINT_REPORT);
		  printReportButton.setMnemonic( KeyEvent.VK_P );
        printReportButton.addActionListener(this);
        printReportButton.setSize(150, 30);
        printReportButton.setLocation(490, 50);
        add(printReportButton);



        //Load detection parameter setting profiles
        String[] titleArray = detectionCondition.get_DetectionConditionPreset_Titles(
                DETECTION_CONDITION_PRESET_FILENAME);
		  if( titleArray == null ) {
			  // attempt to load it from above folder 
			  titleArray = detectionCondition.get_DetectionConditionPreset_Titles(
                ".." + File.separator + DETECTION_CONDITION_PRESET_FILENAME);
		  }; // if

        //Add menu
        JMenuBar menuBar = new JMenuBar();
        JMenu optionsMenu = new JMenu( "Options" );
		  optionsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( optionsMenu );
        parent.setJMenuBar( menuBar );

        ButtonGroup group = new ButtonGroup();
        for( int i = 0; i < titleArray.length; i++ ) {
            JRadioButtonMenuItem subitem = new JRadioButtonMenuItem( titleArray[ i ] );
            subitem.setActionCommand( "DCPreset:" + titleArray[ i ] );
            subitem.addActionListener( this );
            optionsMenu.add( subitem );
            group.add( subitem );
            if( i == 0 ) {
                subitem.setSelected( true );
            }; // if
        }; // for
        BatchImageProcessing.detectionCondition =
                detectionCondition.set_DetectionConditionPreset(
                DETECTION_CONDITION_PRESET_FILENAME,
                titleArray[0]);
        
        if ("/".equals(File.separator) == true) {
            fileChooser = new JFileChooser( System.getProperty( "user.home" ) );
        } 
        else {
            fileChooser = new JFileChooser( "c:\\data\\" );
        }; // if
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        motionDetection = new MotionDetection(this.parent);
    }

    /**
     * Create the GUI and show it
     */
    private static void createAndShowGUI() {
        JFrame frame = new JFrame(TITLE);
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
     *
     * @param actionEvent the action-event object
     */
    public void actionPerformed(ActionEvent actionEvent) {
        if (IMAGE_PROCESSING.equals(actionEvent.getActionCommand()) == true) {
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                Map<String, String> outcomeMap = BatchImageProcessing.recursiveImageProcessing(fileChooser.getSelectedFile());
                String message = "Image Processing completed ";
                int goodCount = 0;
                for (Entry<String, String> each : outcomeMap.entrySet()) {
                    if (each.getValue() == null) {
                        goodCount++;
                    } else {
                        message += "<br>" + each.getKey() + " ";
                        message += "<b>" + each.getValue() + "</b>";
                    }; // if
                }; // for
                if (outcomeMap.size() > 0) {
                    message += "<br>" + goodCount + " folders processed";
                }; // if
                JOptionPane.showMessageDialog(this, "<html>" + message + "</html>");
            }; // if
        }; // if
        if (MANUAL_INSPECTION.equals(actionEvent.getActionCommand()) == true) {
            int state = fileChooser.showOpenDialog(this);
            if (state == JFileChooser.APPROVE_OPTION) {
                File folder = fileChooser.getSelectedFile();
                String path = folder.getAbsolutePath();
                String pathOther = null;
                if (path.endsWith(UNDERSCORE_UNDERSCORE_ONE) == false) {
                    pathOther = path + UNDERSCORE_UNDERSCORE_ONE;
                } else {
                    pathOther = path.substring(0, path.length() - UNDERSCORE_UNDERSCORE_ONE.length());
                }; // if
                File otherFolder = new File(pathOther);
                if (otherFolder.exists() == false) {
                    JOptionPane.showMessageDialog(this, "Unable to find the other folder:\n" + pathOther + "\nUnable to do anything with only one folder.",
                            "Error, the other folder not found!", JOptionPane.ERROR_MESSAGE);
                    return;
                }; // if
                String error = motionDetection.setFolders(folder, otherFolder);
                if (error != null) {
                    JOptionPane.showMessageDialog(this, error, "Error in folder names.", JOptionPane.ERROR_MESSAGE);
                    return;
                }; // if
                motionDetection.detect();
            }; // if
        }; // if
        if (PRINT_REPORT.equals(actionEvent.getActionCommand()) == true) {
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                return;
            }; // if
            File folder = fileChooser.getSelectedFile();
            ResultProcessor resultProcessor = new ResultProcessor();
            String error = resultProcessor.recursivelyProcessDirectory(folder);
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Error in creating the report.", JOptionPane.ERROR_MESSAGE);
                return;
            }; // if
            JOptionPane.showMessageDialog(this, "Finished Results Processing.\nReport is here: "
                    + folder.getAbsolutePath() + File.separator + ResultProcessor.REPORT_FILENAME, "Finished Results Processing.",
                    JOptionPane.INFORMATION_MESSAGE);
        }; // if
        
          if (actionEvent.getActionCommand().startsWith("DCPreset:") == true) {
                BatchImageProcessing.detectionCondition =
                    detectionCondition.set_DetectionConditionPreset(
                    DETECTION_CONDITION_PRESET_FILENAME,
                    actionEvent.getActionCommand().substring(9));
        }
          
    }

    /**
     * Runs the application via a runnable invocation
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
					try {
                				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
					}
					catch( Exception ignore ) {
						ignore.printStackTrace();
					}; // try
                createAndShowGUI();
            }
        });
    }

    public void keyReleased(KeyEvent e) {
          // do nothing
    }

    
    
   public void keyTyped(KeyEvent e) {
        // do nothing
    }

    public void keyPressed(KeyEvent e) {
        // do nothing
    }

   
    
} // class App

