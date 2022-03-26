/*
 * Filename: App.java
 */
package org.quantworm.wormcounter;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class App extends JPanel implements ActionListener, ChangeListener {

   // serial version ID
	private static final long serialVersionUID = 13L;
	
	/** the version of this software */
    public static final String VERSION = "WormCounter 4/27/2015";
    public static final String RESULT_TXT = "result.txt";
    public static final String HISTORICAL = "historical.";
    public static final String DETECTION_CONDITION_PRESET_FILENAME =
            "WormCounter_Detection_Conditon_Preset.txt";
    /** icon padding pixels x-axis */
    public static final int ICON_PADDING_X = 2;

    /** icon padding pixels y-axis */
    public static final int ICON_PADDING_Y = 2;
    
    protected static final String IMAGE_PROCESSING = "Image Processing";
    protected static final String MANUAL_INSPECTION = "Manual Inspection";
    protected static final String PRINT_REPORT = "Print Report";
    protected static final String IMGBUTTON = "imgbutton";
    protected static final String PREV_PAGE = "previous";
    protected static final String NEXT_PAGE = "next";
    protected static final String SAVE = "save";
    protected static final String CLOSE = "close";
    protected static final String VIEW_PLATE = "Plate View";
    protected static final String MASK = "MASK";
    protected static final String MASK_DEFAULT = "mask.jpg";
    protected static final int IMG_BUTTON_TOTAL = 12;
    
    private static final PrintStream out = System.out;
    
    private final JFrame parent;
    private final JSpinner[] spinner;
    private final JButton prevPageButton;
    private final JButton nextPageButton;
    private final JButton closeButton;
    private final JButton saveButton = new JButton(SAVE);
    private final JButton viewPlateButton = new JButton(VIEW_PLATE);
    private final JLabel statusLabel = new JLabel();
    private final JButton[] imgButton = new JButton[IMG_BUTTON_TOTAL];
    private final JDialog dialog;
    private final List<WormInfo> wormDisplayList;
    private final List<WormInfo> wormOriginalOrderList;
    
    private int currentPage;
    private int ceiling;
    private String directoryLocation;
    private Integer particleCount;
    private Integer componentCount;
    private ImagePlus assembled;
    protected final JFileChooser fileChooser;
    protected final JFileChooser maskFileChooser = new JFileChooser( System.getProperty( "user.home" ) );

	 // mask to use if it has been specified for only this session
	 protected String maskOnlyThisSession;
    public DetectionCondition detectionCondition = new DetectionCondition();

    /**
     * Constructor
     *
     * @param parent the parent frame
     */
    public App(JFrame parent) {
        this.parent = parent;
		  if( File.separator.equals( "\\" ) == true ) {
		  		fileChooser = new JFileChooser( "C:\\data" );
		  }
		  else {
		  		fileChooser = new JFileChooser( System.getProperty( "user.home" ) );
		  }; // if

        //Define window
        setLayout(null);
        parent.setLocation(150, 100);
        parent.setMinimumSize(new Dimension(680, 190));
        parent.setResizable(false);

        
        // Add icon image
        ImageIcon logoImageIcon = null;
        File imageFile = new File("images" + File.separator + "logo.png");
        if (imageFile.exists() == true) {
            logoImageIcon = new ImageIcon(imageFile.getAbsolutePath(), VERSION);
        } 
		  else {
            URL imageUrl = getClass().getResource("/logo.png");
            Image image = Toolkit.getDefaultToolkit().getImage(imageUrl);
            logoImageIcon = new ImageIcon(image, VERSION);
        }; // if
        JLabel logoLabel = new JLabel(logoImageIcon);
        this.parent.setIconImage(logoImageIcon.getImage());
        logoLabel.setLocation(5, 5);
        logoLabel.setSize(128, 128);
        add(logoLabel);


        // add buttons
        JButton imageProcessingButton = new JButton(IMAGE_PROCESSING);
        imageProcessingButton.setActionCommand(IMAGE_PROCESSING);
        imageProcessingButton.addActionListener(this);
        imageProcessingButton.setSize(150, 30);
        imageProcessingButton.setLocation(150, 50);
        imageProcessingButton.setMnemonic( KeyEvent.VK_I );
        add(imageProcessingButton);

        JButton manualInspectionButton = new JButton(MANUAL_INSPECTION);
        manualInspectionButton.setActionCommand(MANUAL_INSPECTION);
        manualInspectionButton.addActionListener(this);
        manualInspectionButton.setSize(150, 30);
        manualInspectionButton.setLocation(320, 50);
        manualInspectionButton.setMnemonic( KeyEvent.VK_M );
        add(manualInspectionButton);

        JButton printReportButton = new JButton(PRINT_REPORT);
        printReportButton.setActionCommand(PRINT_REPORT);
        printReportButton.addActionListener(this);
        printReportButton.setSize(150, 30);
        printReportButton.setLocation(490, 50);
        printReportButton.setMnemonic( KeyEvent.VK_P );
        add(printReportButton);


        //Load detection parameter setting profiles
        String[] titleArray = DetectionCondition.get_DetectionConditionPreset_Titles(
                DETECTION_CONDITION_PRESET_FILENAME);


        //Add menu
        JMenuBar menuBar = new JMenuBar();
        parent.setJMenuBar( menuBar );
        JMenu optionsMenu = new JMenu( "Options" );
        menuBar.add( optionsMenu );
        optionsMenu.setMnemonic( KeyEvent.VK_O );

        JMenuItem menuItem = new JMenuItem( "Specify mask ..." );
        menuItem.setActionCommand( MASK );
        menuItem.addActionListener( this );
        optionsMenu.add( menuItem );
        
        PlateProcessor.detectionCondition =
                detectionCondition.set_DetectionConditionPreset(
                DETECTION_CONDITION_PRESET_FILENAME,
                titleArray[0]);


        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        maskFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );

        spinner = new JSpinner[IMG_BUTTON_TOTAL];
        wormDisplayList = new ArrayList<WormInfo>();
        wormOriginalOrderList = new ArrayList<WormInfo>();
        dialog = new JDialog(parent);
        // stop automatic closing
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                closeButton.doClick();
            }
        });
        prevPageButton = new JButton(PREV_PAGE);
        prevPageButton.addActionListener(this);
        nextPageButton = new JButton(NEXT_PAGE);
        nextPageButton.addActionListener(this);
        closeButton = new JButton(CLOSE);
        closeButton.addActionListener(this);
        saveButton.addActionListener(this);
        viewPlateButton.addActionListener(this);
        directoryLocation = null;
        particleCount = null;
        componentCount = null;
        assembled = null;
		  maskOnlyThisSession = null;
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
	public void actionPerformed( ActionEvent actionEvent ) {
		if( IMAGE_PROCESSING.equals( actionEvent.getActionCommand() ) == true ) {
			String maskFilename = null;
			if( maskOnlyThisSession != null ) {
				maskFilename = maskOnlyThisSession;
			}
			else {
				Preferences preferences = Preferences.userNodeForPackage(org.quantworm.wormcounter.App.class);
				maskFilename = preferences.get( MASK, MASK_DEFAULT );
			}; // if
			// verify that the file exists
			File file = new File( maskFilename );
			if( file.exists() == false ) {
				if( MASK_DEFAULT.equals( maskFilename ) == true ) {
					JOptionPane.showMessageDialog( this, "Error: Unable to find default 'mask' image !\nFile: " 
					+ MASK_DEFAULT + " \nPlease select a mask file using the Options menu.", 
					"Unable to find mask image.", JOptionPane.ERROR_MESSAGE);
					return;
				}; // if
				JOptionPane.showMessageDialog( this, "Error: Unable to find 'mask' image !\nFile: " 
				+ maskFilename + " \nPlease select a mask file using the Options menu.", 
				"Unable to find mask image.", JOptionPane.ERROR_MESSAGE);
				return;
			}; // if
			// verify that it is a valid image
			BufferedImage bufferedImage = null;
			try {
				bufferedImage = ImageIO.read( file );
			}
			catch( Exception ignore ) {
				bufferedImage = null;
			}; // try
			if( bufferedImage == null ) {
				JOptionPane.showMessageDialog( this, "Error: 'mask' image is not a valid image !\nFile: " 
				+ maskFilename + " \nPlease select a mask file using the Options menu.", 
				"Mask image is not a valid image.", JOptionPane.ERROR_MESSAGE);
				return;
			}; // if
			int returnValue = fileChooser.showOpenDialog(this);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				PlateProcessor plateProcessor = new PlateProcessor( maskFilename );
				plateProcessor.recursivelyProcessDirectory(fileChooser.getSelectedFile().getAbsolutePath());
				JOptionPane.showMessageDialog(this, "Finished Image Processing.");
			}; // if
		  }; // if
        if (MANUAL_INSPECTION.equals(actionEvent.getActionCommand()) == true) {
            int ret = fileChooser.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                //out.println( "Selected: " + fileChooser.getSelectedFile().getAbsolutePath() );
                manualInspection(fileChooser.getSelectedFile());
            }; // if
        }; // if
        if (PRINT_REPORT.equals(actionEvent.getActionCommand()) == true) {
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File dir = fileChooser.getSelectedFile();
                ResultProcessor resultProcessor = new ResultProcessor();
                resultProcessor.recursivelyProcessDirectory(dir.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Finished Results Processing.");
            }; // if
        }; // if
        // which button was it?
        for (int buttonIndex = 0; buttonIndex < IMG_BUTTON_TOTAL; buttonIndex++) {
            if ((IMGBUTTON + buttonIndex).equals(actionEvent.getActionCommand()) == true) {
                int wormIndex = buttonIndex + currentPage * IMG_BUTTON_TOTAL;
                WormInfo worm = wormDisplayList.get(wormIndex);
                if (worm == null) {
                    JOptionPane.showMessageDialog(this, "Internal error, worm at index " + wormIndex + " is null!", "Internal Error (button)", JOptionPane.ERROR_MESSAGE);
                    break;
                }; // if
                if (worm.changedN != null && worm.changedN == 0) {
                    worm.changedN = null;
                    spinner[ buttonIndex].setValue(worm.nWorm);
                    imgButton[ buttonIndex].setText(worm.nWorm + " ");
                } else {
                    worm.changedN = 0;
                    spinner[ buttonIndex].setValue(0);
                    imgButton[ buttonIndex].setText("deleted");
                }; // if
                updateLabel();
            }; // if
        }; // for
        if (PREV_PAGE.equals(actionEvent.getActionCommand()) == true) {
            pageChange(PREV_PAGE);
        }; // if
        if (NEXT_PAGE.equals(actionEvent.getActionCommand()) == true) {
            pageChange(NEXT_PAGE);
        }; // if
        if (CLOSE.equals(actionEvent.getActionCommand()) == true) {
            closeIt();
        }; // if
        if (SAVE.equals(actionEvent.getActionCommand()) == true) {
            save( this );
        }; // if
        if (VIEW_PLATE.equals(actionEvent.getActionCommand()) == true) {
            viewPlate();
        }; // if
        if (actionEvent.getActionCommand().startsWith("DCPreset:") == true) {
            PlateProcessor.detectionCondition =
                    detectionCondition.set_DetectionConditionPreset(
                    DETECTION_CONDITION_PRESET_FILENAME,
                    actionEvent.getActionCommand().substring(9));
        }
			if( MASK.equals( actionEvent.getActionCommand() ) == true ) {
				int returnValue = maskFileChooser.showOpenDialog( this );
				if( returnValue == JFileChooser.APPROVE_OPTION ) {
					File maskFile = maskFileChooser.getSelectedFile();
					// verify that it is a valid image
					BufferedImage bufferedImage = null;
					try {
						bufferedImage = ImageIO.read( maskFile );
					}
					catch( Exception ignore ) {
						bufferedImage = null;
					}; // try
					if( bufferedImage == null ) {
						JOptionPane.showMessageDialog( this, "Error: Selected file is not a valid image !\nFile: " 
						+ maskFile + " \nPlease select a mask image file again.", 
						"Selected mask is not a valid image.", JOptionPane.ERROR_MESSAGE);
						return;
					}; // if
					// ask user whether to use it only on this session or set it as default
					String[] options = { "Use it only during this session.", "Make it the new default mask.", "Cancel." };
					int answer = JOptionPane.showOptionDialog( this, 
					"How do you want to use this mask?\nThe mask can be used only this one time,\nor it can be set to be the new default mask.", 
					"How do you want to use this mask?",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[ 0 ] );
					if( answer == 0 ) {
						maskOnlyThisSession = maskFile.getAbsolutePath();
					}; // if
					if( answer == 1 ) {
						maskOnlyThisSession = null;
						Preferences preferences = Preferences.userNodeForPackage(org.quantworm.wormcounter.App.class);
						preferences.put( MASK, maskFile.getAbsolutePath() );
					}; // if
				}; // if
			}; // if
    }


	/**
	 * Closes the dialog window 
	 */
	protected void closeIt() {
        dialog.setVisible(false);
    }


	/**
	 * Saves the file, it first renames the previos RESULT_TXT file to a historical name
	 */
	public static void save( App app ) {
        if (app.directoryLocation == null) {
            JOptionPane.showMessageDialog(app, "Internal error, directory is null!", "Internal Error (save-button)", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // get the info to be saved into a list
        List<String> linesList = new ArrayList<String>();
        for( WormInfo worm : app.wormDisplayList ) {
            String line = null;
            if (worm.changedN == null) {
                line = worm.pX + "\t" + worm.pY + "\t" + worm.width + "\t" + worm.height + "\t" + worm.nWorm + "\t" + worm.area + "\t" + worm.label;
            } else {
                if (worm.changedN > 0) {
                    line = worm.pX + "\t" + worm.pY + "\t" + worm.width + "\t" + worm.height + "\t" + worm.changedN + "\t" + worm.area + "\t" + worm.label;
					 }
					 else {
                    out.println( "deletedworm: " + worm.pX + "\t" + worm.pY + "\t" + worm.width + "\t" + worm.height + "\t" + worm.changedN + "\t" + worm.area + "\t" + worm.label );
                }; // if
            }; // if
            if (line != null) {
                linesList.add(line);
            }; // if
        }; // for

        // see if there is historical file, and if so, which number
        File historicalFile = null;
        int number = 0;
        do {
            number++;
            String filename = app.directoryLocation + File.separator + HISTORICAL + number + "." + RESULT_TXT;
            historicalFile = new File(filename);
        } while (historicalFile.exists());
        
        File oldResultsFile = new File(app.directoryLocation + File.separator + RESULT_TXT);
        boolean renamedFlag = oldResultsFile.renameTo(historicalFile);
        if (renamedFlag == false) {
            JOptionPane.showMessageDialog(app, "Error, unable to rename file " + RESULT_TXT + " to a historical filename.", "Cannot save!", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // save the new contents
        File resultsFile = null;
        try {
        		resultsFile = new File(app.directoryLocation + File.separator + RESULT_TXT);
            FileWriter fileWriter = new FileWriter( resultsFile );
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter printWriter = new PrintWriter(bufferedWriter);
            printWriter.print("# Particle Count:\t" + linesList.size() + "\n");
            printWriter.print("# Component Count:\t" + app.componentCount + "\n");
            printWriter.print("# pX\tpY\twidth\theight\tnWorm\tarea\tlabel No\n");
            for (String line : linesList) {
                printWriter.print(line + "\n");
            }; // for
            printWriter.close();
        } 
		  catch (IOException ioe) {
            ioe.printStackTrace();
            JOptionPane.showMessageDialog(app, "Error when saving " + RESULT_TXT + " as follows:\n" + ioe, "I/O Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // try
	
		if( resultsFile == null ) {
           	JOptionPane.showMessageDialog(app, "Error when saving " + RESULT_TXT + " :\n", "Unable to write!", JOptionPane.ERROR_MESSAGE);
			return;
		}; // if
        List<WormInfo> readWormsList = app.readWormsFromFile(resultsFile);

        // crate the image
        ImageProcessor imageProcessor = app.assembled.getProcessor();
        TypeConverter typeConverter = new TypeConverter(imageProcessor, false);
        ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
        final int oneColor = Color.BLUE.getRGB();
        final int unkColor = Color.RED.getRGB();
        final int strokeWidth = 8;
        for (WormInfo worm : readWormsList) {
            number = worm.nWorm;
            int color = unkColor;
            if (worm.label == 1 || worm.label == 2) {
                color = oneColor;
            }; // if
            Roi roi = new Roi(worm.pX - strokeWidth, worm.pY - strokeWidth, worm.width + strokeWidth + 6, worm.height + strokeWidth + 6);
            colorProcessor.setRoi(roi);
            roi.setStrokeWidth(strokeWidth);
            colorProcessor.setValue(color);
            roi.drawPixels(colorProcessor);
            int y = worm.pY - strokeWidth - 2;
            
            if (number > 1) {
                colorProcessor.drawString("" + number, worm.pX - strokeWidth - 2, y);
            }
        }; // for
        ImagePlus imagePlus = new ImagePlus(app.directoryLocation, colorProcessor);
        FileSaver fileSaver = new FileSaver(imagePlus);
        fileSaver.saveAsJpeg(app.directoryLocation + File.separator + "assembled_colors.jpeg");
    }


	/**
	 * Displays the plate with colorful annotations on worms
	 */
	protected void viewPlate() {
		ImageProcessor imageProcessor = assembled.getProcessor();
		TypeConverter typeConverter = new TypeConverter(imageProcessor, false);
		ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
		PlateView plateView = new PlateView( new ImagePlus( "non-annotated", colorProcessor ), wormDisplayList, dialog.getSize(), parent, directoryLocation, this );
		plateView.show();
		String message = plateView.getMessages();
		if( PlateView.SAVE_AND_CLOSE.equals( message ) == true ) {
         closeIt();
			return;
		}; // if

		// see whether to reorganize the page-view buttons
		if( PlateView.PAGE_VIEW.equals( message ) == true ) {
         currentPage = 0;
			pageChange(PREV_PAGE);
		}; // if
	}


	/**
	 * Updates the status label
	 */
	private void updateLabel() {
        String paging = "<b>page " + (currentPage + 1) + " of " + ceiling + "</b>";
        int q = 0;
        int deleted = 0;
        int wormCount = 0;
        int changesCount = 0;
        for (WormInfo worm : wormDisplayList) {
            if (worm.changedN != null) {
                if (worm.changedN == 0) {
                    deleted++;
                }; // if
                changesCount += worm.changedN;
            } else {
                changesCount += worm.nWorm;
            }; // if
            wormCount += worm.nWorm;
            q++;
        }; // for
        String deletedText = "<br>" + q + " images.";
        if (deleted > 0) {
            deletedText = "<br>deleted images: " + deleted;
        }; // if
        String changes = "<br>" + wormCount + " worms.";
        if (wormCount != changesCount) {
            changes = "<br>Intially " + wormCount + " worms, now: " + changesCount + " worms.";
        }; // if
        statusLabel.setText("<html>" + paging + deletedText + changes + "</html");
    }


	/** 
	 * Handles page changes
	 * @param  change  spacifies the change
	 */
	private void pageChange(String change) {
        if (PREV_PAGE.equals(change) == true) {
            currentPage--;
            if (currentPage < 0) {
                currentPage = 0;
            }; // if
        }; // if
        if (NEXT_PAGE.equals(change) == true) {
            currentPage++;
        }; // if
        ceiling = (int) Math.ceil(wormDisplayList.size() * 1.0 / IMG_BUTTON_TOTAL);
        if (((currentPage + 1) * IMG_BUTTON_TOTAL) > wormDisplayList.size()) {
            currentPage = ceiling - 1;
        }; // if
        prevPageButton.setEnabled(currentPage != 0);
        nextPageButton.setEnabled((currentPage + 1) < ceiling);
        updateLabel();

        // fill up images and values for the buttons
        int i = currentPage * IMG_BUTTON_TOTAL;
        for (int buttonIndex = 0; buttonIndex < IMG_BUTTON_TOTAL; buttonIndex++) {
            SpinnerNumberModel spinnerNumberModel = (SpinnerNumberModel) spinner[ buttonIndex].getModel();
            //spinnerNumberModel.setMaximum( 99 );
            if (i < wormDisplayList.size()) {
                WormInfo worm = wormDisplayList.get(i);
                imgButton[ buttonIndex].setEnabled(true);
                imgButton[ buttonIndex].setPreferredSize(new Dimension(worm.width + ICON_PADDING_X * 2, worm.height + ICON_PADDING_Y * 2 + 4));
                imgButton[ buttonIndex].setMinimumSize(new Dimension(worm.width + ICON_PADDING_X * 2, worm.height + ICON_PADDING_Y * 2 + 4));
                ImagePlus preview = NewImage.createByteImage("" + worm.nWorm, worm.width + ICON_PADDING_X * 2, worm.height + ICON_PADDING_Y * 2, 1, NewImage.GRAY8);
                for (int x = 0 - ICON_PADDING_X; x < worm.width + ICON_PADDING_X; x++) {
                    for (int j = 0 - ICON_PADDING_Y; j < worm.height + ICON_PADDING_Y; j++) {
                        preview.getProcessor().putPixel(x + ICON_PADDING_X, j + ICON_PADDING_Y, assembled.getPixel(worm.pX + x, worm.pY + j));
                    }; // for
                }; // for
               
                imgButton[ buttonIndex].setIcon(new ImageIcon(preview.getBufferedImage()));
                if (worm.changedN != null && worm.changedN == 0) {
                    imgButton[ buttonIndex].setText("deleted");
                    spinnerNumberModel.setValue(0);
                } else {
                    if (worm.changedN != null) {
                        spinnerNumberModel.setValue(worm.changedN);
                        imgButton[ buttonIndex].setText(worm.changedN + " ");
                    } else {
                        spinnerNumberModel.setValue(worm.nWorm);
                        imgButton[ buttonIndex].setText(worm.nWorm + " ");
                    }; // if
                }; // if
                spinner[ buttonIndex].setEnabled(true);
            } else {
                imgButton[ buttonIndex].setText("----");
                imgButton[ buttonIndex].setIcon(null);
                imgButton[ buttonIndex].setPreferredSize(new Dimension(20, 20));
                imgButton[ buttonIndex].setMinimumSize(new Dimension(20, 20));
                imgButton[ buttonIndex].setEnabled(false);
                spinner[ buttonIndex].setEnabled(false);
                spinnerNumberModel.setValue(0);
            }; // if
            spinnerNumberModel.setMinimum(0);
            imgButton[ buttonIndex].setMargin(new Insets(2, 2, 2, 2));
            imgButton[ buttonIndex].invalidate();
            i++;
        }; // for
        dialog.validate();
        dialog.repaint();
    }


	/**
	 * Changes of spinners show up here
	 * @param  changeEvent  the change event object
	 */
	public void stateChanged(ChangeEvent changeEvent) {
        Object source = changeEvent.getSource();
        for (int buttonIndex = 0; buttonIndex < IMG_BUTTON_TOTAL; buttonIndex++) {
            if (spinner[ buttonIndex] == source) {
                int wormIndex = buttonIndex + currentPage * IMG_BUTTON_TOTAL;
                if (wormIndex >= wormDisplayList.size()) {
                    return;
                }; // if
                WormInfo worm = wormDisplayList.get(wormIndex);
                if (worm == null) {
                    JOptionPane.showMessageDialog(this, "Internal error, worm at index " + wormIndex + " is null!", "Internal (stateChanged)", JOptionPane.ERROR_MESSAGE);
                    break;
                }; // if
                Integer value = new Integer(spinner[ buttonIndex].getValue().toString());
                if (value == 0) {
                    imgButton[ buttonIndex].setText("deleted");
                    worm.changedN = 0;
                } else {
                    if (value.equals(worm.nWorm) == true) {
                        worm.changedN = null;
                    } else {
                        worm.changedN = value;
                    }; // if
                    spinner[ buttonIndex].setValue(value);
                    imgButton[ buttonIndex].setText(value + " ");
                }; // if
                updateLabel();
            }; // if
        }; // for
    }


	/**
	 * Reads worms from file
	 * @param  file  the file
	 * @return  null if unable to read, otherwise a list of worm objects
	 */
	protected List<WormInfo> readWormsFromFile(File file) {
        List<String> linesList = getLinesFromFile(file);
        if (linesList == null) {
            return null;
        }; // if
        List<WormInfo> retList = new ArrayList<WormInfo>();

        // process the text file
        int count = 0;
        for (String each : linesList) {
            String[] items = each.split("\t");
            count++;
            if (each.startsWith("#") == true) {
                if ("# Particle Count:".equals(items[ 0]) == true) {
                    particleCount = WormInfo.getInteger(items[ 1]);
                }; // if
                if ("# Component Count:".equals(items[ 0]) == true) {
                    componentCount = WormInfo.getInteger(items[ 1]);
                }; // if
                continue;
            }; // if
            if (items.length == 7) {
                WormInfo wormInfo = WormInfo.createWormInfo(items);
                if (wormInfo == null) {
                    JOptionPane.showMessageDialog(this, "Unable to process data of line number " + count + " of " + RESULT_TXT + "\nLine: " + each.replaceAll("\t", " "), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    retList.add(wormInfo);
                }; // if
            }; // if
        }; // for
        return retList;
    }


	/**
	 * Manual Inspection of images
	 * @param  directory  the directory containing RESULT_TXT and plateImg.jpg
	 */
	public void manualInspection(File directory) {
        if (directory == null) {
            JOptionPane.showMessageDialog(this, "Directory does not exist ( null )", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if
        if (directory.exists() == false) {
            JOptionPane.showMessageDialog(this, "Directory does not exist ( " + directory.getAbsolutePath() + " )", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // results-file must exist
        File resultsFile = new File(directory.getAbsolutePath() + File.separator + RESULT_TXT);
        if (resultsFile.exists() == false) {
            JOptionPane.showMessageDialog(this, "Results file (" + RESULT_TXT + ") does not exist in directory " + directory.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // plate-image must exist
        File plateFile = new File(directory.getAbsolutePath() + File.separator + "assembled.jpeg");
        if (plateFile.exists() == false) {
            JOptionPane.showMessageDialog(this, "Plate-image file (assembled.jpeg) does not exist in directory " + directory.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // load the assembled image
        assembled = new ImagePlus(plateFile.getAbsolutePath());

        // reset the worm original order list
        wormOriginalOrderList.clear();
        particleCount = null;
        componentCount = null;
        List<WormInfo> readWormsList = readWormsFromFile(resultsFile);
        if (readWormsList == null) {
            JOptionPane.showMessageDialog(this, "Unable to load anything from " + RESULT_TXT, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // copy the elements into the worm-original-order-list
        for (WormInfo worm : readWormsList) {
            wormOriginalOrderList.add(worm);
        }; // for

        // verify that we read values for particle count, and component count
        if (particleCount == null) {
            JOptionPane.showMessageDialog(this, "ERROR, did not find particle count in the file " + RESULT_TXT, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if
        if (componentCount == null) {
            JOptionPane.showMessageDialog(this, "ERROR, did not find component count in the file " + RESULT_TXT, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // place the objects into a map for sorting purposes
        Map<Integer, List<WormInfo>> wormMap = new TreeMap<Integer, List<WormInfo>>();
        int verifyWormCount = 0;
        for (WormInfo each : wormOriginalOrderList) {
            List<WormInfo> tmpList = wormMap.get(each.area);
            if (tmpList == null) {
                tmpList = new ArrayList<WormInfo>();
                wormMap.put(each.area, tmpList);
            }; // if
            tmpList.add(each);
            verifyWormCount += each.nWorm;
        }; // for
        
        // reset the worm list 
        wormDisplayList.clear();
        
        int secondVerification = 0;
        for (Integer area : wormMap.keySet()) {
            for (WormInfo each : wormMap.get(area)) {
                //out.println( each );
                wormDisplayList.add(each);
                secondVerification += each.nWorm;
            }; // for
        }; // for

        // just in case verification
        if (verifyWormCount != secondVerification) {
            JOptionPane.showMessageDialog(this, "Error, mismatch counted worms (" + verifyWormCount + ") and second verification (" + secondVerification + ").", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // find max height and widht
        int thirdVerification = 0;
        int maxWidth = 0;
        int maxHeight = 0;
        for (WormInfo worm : wormDisplayList) {
            thirdVerification += worm.nWorm;
            if (worm.width > maxWidth) {
                maxWidth = worm.width;
            }; // if
            if (worm.height > maxHeight) {
                maxHeight = worm.height;
            }; // if
        }; // for

        // just in case verification
        if (verifyWormCount != thirdVerification) {
            JOptionPane.showMessageDialog(this, "Error, mismatch counted worms (" + verifyWormCount + ") and third verification (" + thirdVerification + ").", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        dialog.setTitle(directory.getAbsolutePath());
        dialog.setPreferredSize(new Dimension(980, 800));
        Container container = dialog.getContentPane();
        container.setLayout(new GridBagLayout());
        container.removeAll();

        // the buttons
        SpinnerNumberModel[] spinnerModel = new SpinnerNumberModel[IMG_BUTTON_TOTAL];
        int gridxStart = 0;
        int gridyStart = 0;
        int gridx = gridxStart;
        int gridy = gridyStart;
        final int MAX_ON_X = 3;
        for (int i = 0; i < IMG_BUTTON_TOTAL; i++) {
            imgButton[ i] = new JButton();
            imgButton[ i].setHorizontalTextPosition(AbstractButton.LEADING);
            imgButton[ i].setMinimumSize(new Dimension(maxWidth + 6, maxHeight + 6));
            imgButton[ i].setPreferredSize(new Dimension(maxWidth + 6, maxHeight + 6));
            imgButton[ i].setActionCommand(IMGBUTTON + i);
            imgButton[ i].addActionListener(this);
            imgButton[ i].setIcon(null);
            GridBagConstraints gridBag = new GridBagConstraints();
            gridBag.gridx = gridx;
            gridBag.gridy = gridy;
            gridBag.fill = GridBagConstraints.BOTH;
            gridBag.anchor = GridBagConstraints.CENTER;
            gridBag.insets = new Insets(6, 6, 2, 6);
            gridBag.weightx = 0.1;
            gridBag.weighty = 0.2;
            container.add(imgButton[ i], gridBag);

            // spinner stuff
            spinnerModel[ i] = new SpinnerNumberModel();
            spinner[ i] = new JSpinner(spinnerModel[ i]);
            spinner[ i].addChangeListener(this);
            JComponent component = spinner[ i].getEditor();
            NumberEditor numberEditor = (NumberEditor) component;
            JFormattedTextField formattedTextField = numberEditor.getTextField();
            formattedTextField.setColumns(2);
            GridBagConstraints gridBag2 = new GridBagConstraints();
            gridBag2.gridx = gridx;
            gridBag2.gridy = gridy + 1;
            gridBag.anchor = GridBagConstraints.CENTER;
            container.add(spinner[ i], gridBag2);

            // increases for next set of button+spinner
            gridx++;
            if (gridx == (gridxStart + MAX_ON_X)) {
                gridx = gridxStart;
                gridy += 2;
            }; // if
        }; // for

        // previous page button
        prevPageButton.setMnemonic(KeyEvent.VK_P);
        prevPageButton.setActionCommand(PREV_PAGE);
        prevPageButton.setEnabled(true);
        GridBagConstraints gridBag = new GridBagConstraints();
        gridBag.gridx = 0;
        gridBag.gridy = gridy;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(prevPageButton, gridBag);

        // next page button
        nextPageButton.setMnemonic(KeyEvent.VK_N);
        nextPageButton.setActionCommand(NEXT_PAGE);
        gridBag = new GridBagConstraints();
        gridBag.gridx = 1;
        gridBag.gridy = gridy;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(nextPageButton, gridBag);

        // view plate button
        viewPlateButton.setMnemonic(KeyEvent.VK_V);
        viewPlateButton.setActionCommand(VIEW_PLATE);
        gridBag = new GridBagConstraints();
        gridBag.gridx = 2;
        gridBag.gridy = gridy;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(viewPlateButton, gridBag);

        // status label
        gridBag = new GridBagConstraints();
        gridBag.gridx = 0;
        gridBag.gridy = gridy + 1;
        gridBag.fill = GridBagConstraints.HORIZONTAL;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.LINE_START;
        gridBag.gridwidth = 3;
        container.add(statusLabel, gridBag);

        // save button
        saveButton.setMnemonic(KeyEvent.VK_S);
        saveButton.setActionCommand(SAVE);
        gridBag = new GridBagConstraints();
        gridBag.gridx = 0;
        gridBag.gridy = gridy + 2;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(saveButton, gridBag);

        // close button
        closeButton.setActionCommand(CLOSE);
        gridBag = new GridBagConstraints();
        gridBag.gridx = 2;
        gridBag.gridy = gridy + 2;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(closeButton, gridBag);

        currentPage = 0;
        directoryLocation = directory.getAbsolutePath();
        prevPageButton.doClick();

        Dimension preferredDimension = dialog.getPreferredSize();
		  Toolkit toolkit = Toolkit.getDefaultToolkit();
		  Dimension screenDimension = toolkit.getScreenSize();
		  if( screenDimension.height < preferredDimension.height ) {
          dialog.setSize( new Dimension( screenDimension.width - 40, screenDimension.height - 40 ) );
		  }
		  else {
          dialog.setSize(dialog.getPreferredSize());
		  }
        dialog.setVisible(true);
    }


	/**
	 * Reads a file 
	 * @param  file  the file
	 * @return  the lines of the file, or null when there is error
	 */
	public List<String> getLinesFromFile(File file) {
        List<String> linesList = new ArrayList<String>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line = null;
            if (bufferedReader.ready()) {
                while ((line = bufferedReader.readLine()) != null) {
                    linesList.add(line);
                }; // while
            } else {
                JOptionPane.showMessageDialog(this, "Unable to read " + RESULT_TXT + " file, please try again!", "ERROR", JOptionPane.ERROR_MESSAGE);
                return null;
            }; // if
            bufferedReader.close();
        } catch (FileNotFoundException fnfe) {
            JOptionPane.showMessageDialog(this, "File not found: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, "File input/output error with file: " + file.getAbsolutePath(), "Eror", JOptionPane.ERROR_MESSAGE);
            return null;
        }; // try
        return linesList;
    }

		

	/** Runs the app via a runnable invocation */
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
		} );
	}

} // class App

