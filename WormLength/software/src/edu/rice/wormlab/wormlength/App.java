/*
 * Filename: App.java
 * This is the main class which shows the application window
 * containing 'Image Processing','Manual Inspection, and 'Print Report' buttons
 */
package edu.rice.wormlab.wormlength;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App extends JPanel implements ActionListener {

    // serial version UID
    private static final long serialVersionUID = 11L;
    /**
     * the version of this software
     */
    public static final String VERSION = "WormLength 9/19/2013";
    public static final String RESULT_TXT = "result-length.txt";
    public static final String HISTORICAL = "historical.";
    public static final String ASSEMBLED_COLORS_JPEG = "assembled_colors.jpeg";
    public static final String DETECTION_CONDITION_PRESET_FILENAME =
            "WormLength_Detection_Conditon_Preset.txt";
    /**
     * icon padding pixels x-axis
     */
    public static final int ICON_PADDING_X = 20;
    /**
     * icon padding pixels y-axis
     */
    public static final int ICON_PADDING_Y = 20;
    protected static final String IMAGE_PROCESSING = "Image Processing";
    protected static final String MANUAL_INSPECTION = "Manual Inspection";
    protected static final String PRINT_REPORT = "Print Report";
    protected static final String IMGBUTTON = "imgbutton";
    protected static final String PREV_PAGE = "previous";
    protected static final String NEXT_PAGE = "next";
    protected static final String SAVE = "save";
    protected static final String CLOSE = "close";
    protected static final String VIEW_PLATE = "Plate View";
    protected static final String SAVEEXIT = "Save & Close (Alt+X)";
    protected static final String UNCHECKALL = "Delete all (U)";
    protected static final int IMG_BUTTON_TOTAL = 9;
    protected static final String METHOD_MANUAL = "# Manual inspection:";
    private static final PrintStream out = System.out;
    private final JFrame parentFrame;
    private final JButton prevPageButton;
    private final JButton nextPageButton;
    private final JButton closeButton;
    private final JButton saveButton = new JButton(SAVE);
    private final JButton viewPlateButton = new JButton(VIEW_PLATE);
    private final JButton saveCloseButton;
    private final JButton uncheckAllButton;
    private final JLabel statusLabel = new JLabel();
    private final JButton[] imgButton = new JButton[IMG_BUTTON_TOTAL];
    private final JDialog dialog;
    private final List<WormInfo> wormDisplayList;
    private int currentPage;
    private int ceiling;
    private String directoryLocation;
    // the assembled image
    private ImagePlus assembled_org;
    // the cropped (assembled) images
    private ImagePlus croppedImagePlus;
    private ImageIcon[] cachedImageIcons;
    protected final JFileChooser fileChooser;
    public static NativeImgProcessing imgProc = new NativeImgProcessing();
    public DetectionCondition detectionCondition = new DetectionCondition();

    /**
     * Constructor
     *
     * @param parent the parent frame
     */
    public App(JFrame parent) {
        this.parentFrame = parent;

        //Define window
        setLayout(null);
        parentFrame.setLocation(150, 100);
        parentFrame.setMinimumSize(new Dimension(720, 190));
        parentFrame.setResizable(false);


        // Add icon image
        ImageIcon logoImageIcon = null;
        File imageFile = new File("images" + File.separator + "logo.png");
        if (imageFile.exists() == true) {
            logoImageIcon = new ImageIcon(imageFile.getAbsolutePath(), VERSION);
        } else {
            URL imageUrl = getClass().getResource("/logo.png");
            Image image = Toolkit.getDefaultToolkit().getImage(imageUrl);
            logoImageIcon = new ImageIcon(image, VERSION);
        }; // if
        JLabel logoLabel = new JLabel(logoImageIcon);
        parentFrame.setIconImage(logoImageIcon.getImage());
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
        String[] titleArray = detectionCondition.get_DetectionConditionPreset_Titles(
                DETECTION_CONDITION_PRESET_FILENAME);
		  if( titleArray == null ) {
			  	// attempt to find it in above folder
        		titleArray = detectionCondition.get_DetectionConditionPreset_Titles(
                ".." + File.separator + DETECTION_CONDITION_PRESET_FILENAME);
		  }; // if


        //Add menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menuOptions = new JMenu("Options");
        menuOptions.setMnemonic( KeyEvent.VK_O );
        parentFrame.setJMenuBar(menuBar);
        menuBar.add(menuOptions);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < titleArray.length; i++) {
            JRadioButtonMenuItem subitem = new JRadioButtonMenuItem(titleArray[i]);
            subitem.setActionCommand("DCPreset:" + titleArray[i]);
            subitem.addActionListener(this);
            menuOptions.add(subitem);
            group.add(subitem);
            if (i == 0) {
                subitem.setSelected(true);
            }
        }
        ImageProcessing.detectionCondition =
                detectionCondition.set_DetectionConditionPreset(
                DETECTION_CONDITION_PRESET_FILENAME,
                titleArray[0]);


        //initialize
        if ("/".equals(File.separator) == true) {
            fileChooser = new JFileChooser(System.getProperty("user.home"));
        } else {
            fileChooser = new JFileChooser("c:\\data");
        }
	fileChooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );

        wormDisplayList = new ArrayList<WormInfo>();
        dialog = new JDialog(parentFrame);
        // stop automatic closing
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                closeButton.doClick();
            }
        });


        //add buttons in manual inspection window
        prevPageButton = new JButton( new PrevPageAction( "   " + PREV_PAGE + "   ", KeyEvent.VK_P ) );

        nextPageButton = new JButton( new NextPageAction( "   " + NEXT_PAGE + "   ", KeyEvent.VK_N ) );

        closeButton = new JButton(CLOSE);
        closeButton.addActionListener(this);
		  closeButton.setMnemonic( KeyEvent.VK_C );

        saveButton.addActionListener(this);
		  saveButton.setMnemonic( KeyEvent.VK_S );

        viewPlateButton.addActionListener(this);
		  viewPlateButton.setMnemonic( KeyEvent.VK_V );

        saveCloseButton = new JButton( SAVEEXIT );
        saveCloseButton.addActionListener(this);
		  saveCloseButton.setMnemonic( KeyEvent.VK_X );

        uncheckAllButton = new JButton( UNCHECKALL );
        uncheckAllButton.addActionListener(this);
        uncheckAllButton.setMnemonic(KeyEvent.VK_U);

        directoryLocation = null;
        assembled_org = null;
        croppedImagePlus = null;
        cachedImageIcons = null;
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
	 *
	 * @param actionEvent the action-event object
	 */
	public void actionPerformed(ActionEvent actionEvent) {
		if (IMAGE_PROCESSING.equals(actionEvent.getActionCommand()) == true) {
			int returnValue = fileChooser.showOpenDialog(this);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				// we expect either a folder or a text-file
				if( fileChooser.getSelectedFile().isDirectory() == true ) {
				
					Map<String, String> outcomeMap = ImageProcessing.recursiveImageProcessing(fileChooser.getSelectedFile());
					String message = "Image Processing completed ";
					for (Entry<String, String> each : outcomeMap.entrySet()) {
						if (each.getValue() != null) {
							message += "<br>" + each.getKey() + " ";
							message += "<b>" + each.getValue() + "</b>";
						}; // if
					}; // for
					JOptionPane.showMessageDialog(this, "<html>" + message + "</html>");
				}
				else {
					List<String> linesList = Utilities.getLinesFromFile( fileChooser.getSelectedFile() );
					if( linesList == null || linesList.size() == 0 ) {
						JOptionPane.showMessageDialog(this, "Nothing to do!\nSpecified folder was empty,\nor had trouble reading it.");
					}
					else {
						String message = "Image Processing completed. ";
						for( String each : linesList ) {
							String line = each.trim();
							if( line.startsWith( File.separator ) == false ) {
								continue;
							}; // if
							String[] parts = line.split( "\t" );
							File eachFolder = new File( parts[ 0 ] );
							if( eachFolder.exists() == false ) {
								message += "<br>did not find this: " + parts[ 0 ] + " ";
								continue;
							}; // if
							if( eachFolder.isDirectory() == false ) {
								message += "<br>not a folder: " + parts[ 0 ] + " ";
								continue;
							}; // if
							out.println( eachFolder.getAbsolutePath() );
							String error = ImageProcessing.imageProcessing( eachFolder );
							if( error != null ) {
								message += "<br>Folder: " + parts[ 0 ] + " <b>" + error + "</b>";
							}; // if
						}; // for
						JOptionPane.showMessageDialog(this, "<html>" + message + "</html>");
					}; // if
				}; // if
			}; // if
		}; // if

        if (MANUAL_INSPECTION.equals(actionEvent.getActionCommand()) == true) {
            int ret = fileChooser.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
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
                    imgButton[ buttonIndex].setForeground(Color.black);
                    imgButton[ buttonIndex].setText(Utilities.format2(worm.trueLength) + " ");
                } else {
                    worm.changedN = 0;
                    imgButton[ buttonIndex].setForeground(Color.red);
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
        if (SAVEEXIT.equals(actionEvent.getActionCommand()) == true) {
            save( this );
            closeIt();
        }; // if
        if (UNCHECKALL.equals(actionEvent.getActionCommand()) == true) {
            uncheckAllIntheCurrentPage();
        }; // if
        if (actionEvent.getActionCommand().startsWith("DCPreset:") == true) {
            ImageProcessing.detectionCondition =
                    detectionCondition.set_DetectionConditionPreset(
                    DETECTION_CONDITION_PRESET_FILENAME,
                    actionEvent.getActionCommand().substring(9));
        }

    }

    /**
     * displays the plate with colorful annotations on worms
     *
     */
    protected void viewPlate() {
        TypeConverter typeConverter = new TypeConverter(assembled_org.getProcessor(), false);
        ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
        PlateView plateView = new PlateView(new ImagePlus("non-annotated", colorProcessor), wormDisplayList, dialog.getPreferredSize(), parentFrame, fileChooser.getSelectedFile().getAbsolutePath(), this);

        plateView.show();

        String message = plateView.getMessages();
        if (PlateView.SAVE_AND_CLOSE.equals(message) == true) {
            closeIt();
            return;
        }; // if

        // see whether we need to reorganize the page-view buttons
        if (PlateView.PAGE_VIEW.equals(message) == true) {
            currentPage = 0;
            pageChange(PREV_PAGE);
            dialog.validate();
            dialog.repaint();
        }; // if
    }

    /**
     * Uncheck all worms in the current page
     *
     */
    protected void uncheckAllIntheCurrentPage() {

        for (int buttonIndex = 0; buttonIndex < IMG_BUTTON_TOTAL; buttonIndex++) {
            int index = currentPage * 9 + buttonIndex;

            if (index < wormDisplayList.size()) {
                WormInfo worm = wormDisplayList.get(index);
                worm.changedN = 0;
                imgButton[ buttonIndex].setText("deleted");
                imgButton[ buttonIndex].setForeground(Color.red);
            }
        }
        updateLabel();
    }

   
    /**
     * Closes the dialog window after verifying changes being saved
     */
    protected void closeIt() {
        dialog.setVisible(false);
    }

    /**
     * Saves the file, it first renames the existing RESULT_TXT file to a
     * historical name
     */
    public static void save( App app ) {
        if (app.directoryLocation == null) {
            JOptionPane.showMessageDialog( app, "Internal error, directory is null!", "Internal Error (save-button)", JOptionPane.ERROR_MESSAGE );
            return;
        }; // if

        // count number of animals
        int nAnimals = 0;
        for (WormInfo worm : app.wormDisplayList) {
            if (worm.changedN == null) {
                nAnimals += worm.nWorm;
            } else {
                if (worm.changedN == worm.nWorm) {
                    nAnimals += worm.nWorm;
                } else {
                    nAnimals += worm.changedN;
                }; // if
            }; // if
        }; // for

        // see if there is historical file, and if so, find the next available number
        File historicalFile = null;
        int number = 0;
        do {
            number++;
            String filename = app.directoryLocation + File.separator + HISTORICAL + number + "." + RESULT_TXT;
            historicalFile = new File(filename);
        } while (historicalFile.exists());
        File oldResultsFile = new File( app.directoryLocation + File.separator + RESULT_TXT );
        boolean renamedFlag = oldResultsFile.renameTo(historicalFile);
        if (renamedFlag == false) {
            JOptionPane.showMessageDialog( app, "Error, unable to rename file " + RESULT_TXT + " to a historical filename.", "Cannot save!", JOptionPane.ERROR_MESSAGE );
            return;
        }; // if

        // save the new contents
        try {
            FileWriter fileWriter = new FileWriter( app.directoryLocation + File.separator + RESULT_TXT );
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter printWriter = new PrintWriter(bufferedWriter);
            printWriter.println("# File saved at:\t" + new Date());
            printWriter.println("# Manual inspection:\tDone");
            printWriter.println("# worms count:\t" + nAnimals);
            printWriter.println("# pX\tpY\twidth\theight\tlength(micrometers)\timageClipN");
            for (WormInfo worm : app.wormDisplayList) {
                if (worm.changedN != null && worm.changedN == 0) {
                    // easy case: deletion, do not write it to file anymore
                    continue;
                }
                printWriter.println(worm.pX + "\t" + worm.pY
                        + "\t" + worm.width + "\t" + worm.height
                        + "\t" + worm.trueLength + "\t" + worm.imageClipN);
            }; // for
            printWriter.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            JOptionPane.showMessageDialog( app, "Error when saving " + RESULT_TXT + " as follows:<br>" + ioe, "I/O Error", JOptionPane.ERROR_MESSAGE );
            return;
        }; // try

		  // now save the assembled colors image
        ImageProcessing.saveAssembledColorImage( app.wormDisplayList, app.assembled_org, app.directoryLocation );
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
            deletedText = "<br>" + deleted + " deleted images out of " + q;
        }; // if
        String changes = "<br>" + wormCount + " worms.";
        if (wormCount != changesCount) {
            changes = "<br>Intially " + wormCount + " worms, now: " + changesCount + " worms.";
        }; // if
        statusLabel.setText("<html>" + paging + deletedText + changes + "</html");
    }

    /**
     * Handles page changes
     *
     * @param change specifies the change
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
            if (i < wormDisplayList.size()) {
                WormInfo worm = wormDisplayList.get(i);
                imgButton[ buttonIndex].setEnabled(true);
                imgButton[ buttonIndex].setPreferredSize(new Dimension(worm.width + ICON_PADDING_X * 2, worm.height + ICON_PADDING_Y * 2 + 4));
                imgButton[ buttonIndex].setMinimumSize(new Dimension(worm.width + ICON_PADDING_X * 2, worm.height + ICON_PADDING_Y * 2 + 4));
                imgButton[ buttonIndex].setIcon(cachedImageIcons[ i]);
                if (worm.changedN != null && worm.changedN == 0) {
                    imgButton[ buttonIndex].setText("deleted");
                    imgButton[ buttonIndex].setForeground(Color.red);
                } else {
                    imgButton[ buttonIndex].setText(Utilities.format2(worm.trueLength) + " ");
                    imgButton[ buttonIndex].setForeground(Color.black);
                }; // if
            } else {
                imgButton[ buttonIndex].setText("----");
                imgButton[ buttonIndex].setIcon(null);
                imgButton[ buttonIndex].setPreferredSize(new Dimension(20, 20));
                imgButton[ buttonIndex].setMinimumSize(new Dimension(20, 20));
                imgButton[ buttonIndex].setEnabled(false);
            }; // if
            imgButton[ buttonIndex].setMargin(new Insets(2, 2, 2, 2));
            imgButton[ buttonIndex].invalidate();
            i++;
        }; // for
        dialog.validate();
        dialog.repaint();
    }

    /**
     * Gets an Image-Plus from an image-Plus
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width
     * @param height the height
     * @return the image icon
     */
    public ImagePlus getImagePlus(ImagePlus ip, int x, int y, int width, int height) {
        ImagePlus previewImagePlus = NewImage.createByteImage("tiny", width, height, 1, NewImage.GRAY8);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                previewImagePlus.getProcessor().putPixel(i, j, ip.getPixel(x + i, y + j));
            }; // for
        }; // for
        return previewImagePlus;
    }

    /**
     * Updates (or creates) the icons, which are cached for the purpose of
     * creating them only once
     */
    protected void updateImageIconsCache(File directory) {
        cachedImageIcons = new ImageIcon[wormDisplayList.size()];

        for (int index = 0; index < cachedImageIcons.length; index++) {
            WormInfo worm = wormDisplayList.get(index);

            ByteProcessor original = (ByteProcessor) getImagePlus(assembled_org,
                    worm.pX - ICON_PADDING_X, worm.pY - ICON_PADDING_Y,
                    worm.width + ICON_PADDING_X * 2, worm.height
                    + ICON_PADDING_Y * 2).getProcessor();
            ImagePlus previewImagePlus = NewImage.createRGBImage("preview",
                    original.getWidth(), original.getHeight(),
                    1, NewImage.RGB);
            BufferedImage imageClip = imgProc.loadImage(
                    directory.getAbsolutePath() + File.separator
                    + "imageClip" + File.separator + "Clip" + worm.imageClipN
                    + ".gif");



            // first we do the copy
            for (int i = 0; i < original.getWidth(); i++) {
                for (int j = 0; j < original.getHeight(); j++) {
                    int[] rgb = new int[3];
                    rgb[ 0] = original.getPixel(i, j);
                    rgb[ 1] = original.getPixel(i, j);
                    rgb[ 2] = original.getPixel(i, j);
                    previewImagePlus.getProcessor().putPixel(i, j, rgb);
                }; // for
            }; // for


            // second we overlay the skeleton
            Color color = new Color(255, 0, 0);
            int redColor = color.getRGB();
            for (int i = 0; i < imageClip.getWidth(); i++) {
                for (int j = 0; j < imageClip.getHeight(); j++) {
                    if (imageClip.getRGB(i, j) == redColor) {
                        previewImagePlus.getProcessor().putPixel(i
                                + ICON_PADDING_X, j + ICON_PADDING_Y, redColor);
                    }

                }; // for
            }; // for


            ImageProcessor previewColor = previewImagePlus.getProcessor();
            previewColor.setColor(Color.BLUE);
            // vertical lines
            previewColor.drawLine(ICON_PADDING_X, 2, ICON_PADDING_X, ICON_PADDING_Y);
            previewColor.drawLine(ICON_PADDING_X + worm.width, 2, ICON_PADDING_X + worm.width, ICON_PADDING_Y);
            previewColor.drawLine(ICON_PADDING_X, ICON_PADDING_Y + worm.height, ICON_PADDING_X, ICON_PADDING_Y * 2 + worm.height - 2);
            previewColor.drawLine(ICON_PADDING_X + worm.width, ICON_PADDING_Y + worm.height, ICON_PADDING_X + worm.width, ICON_PADDING_Y * 2 + worm.height - 2);
            // horizontal lines
            previewColor.drawLine(2, ICON_PADDING_Y, ICON_PADDING_X, ICON_PADDING_Y);
            previewColor.drawLine(ICON_PADDING_X + worm.width, ICON_PADDING_Y, ICON_PADDING_X * 2 + worm.width - 2, ICON_PADDING_Y);
            previewColor.drawLine(2, ICON_PADDING_Y + worm.height, ICON_PADDING_X, ICON_PADDING_Y + worm.height);
            previewColor.drawLine(ICON_PADDING_X + worm.width, ICON_PADDING_Y + worm.height, ICON_PADDING_X * 2 + worm.width - 2, ICON_PADDING_Y + worm.height);
            cachedImageIcons[ index] = new ImageIcon(previewImagePlus.getBufferedImage());
        }; // for
    }

    /**
     * Reads worms from file
     *
     * @param file the file
     * @return null if unable to read, otherwise a list of worm objects
     */
    protected List<WormInfo> readWormsFromFile(File file) {
        List<String> linesList = getLinesFromFile(file);
        if (linesList == null) {
            return null;
        }; // if
        List<WormInfo> retList = new ArrayList<WormInfo>();

        // process the text file
        int lineNumber = 1;
        Integer wormCountLine = null;
        for (String each : linesList) {
            String[] items = each.split("\t");
            if (each.startsWith("#") == true) {
                if (each.startsWith("# worms count:\t") == true & items.length >= 2) {
                    wormCountLine = Utilities.getInteger(items[ 1]);
                }; // if
                continue;
            }; // if
            if (items.length == 6) {
                WormInfo wormInfo = WormInfo.createWormInfo(items);
                if (wormInfo == null) {
                    JOptionPane.showMessageDialog(this, "Unable to process data of line number " + lineNumber + " of " + RESULT_TXT + "\nLine: " + each.replaceAll("\t", " "), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    retList.add(wormInfo);
                }; // if
            } else {
                JOptionPane.showMessageDialog(this, "Unexpected format in line number " + lineNumber + " of " + RESULT_TXT + "\nLine: " + each.replaceAll("\t", " "), "Error", JOptionPane.ERROR_MESSAGE);
            }; // if
            lineNumber++;
        }; // for
        if (wormCountLine == null) {
            JOptionPane.showMessageDialog(this, "Unable to process data of number of worms (worm count is undefined)", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }; // if
        int count = 0;
        for (WormInfo wormInfo : retList) {
            count += wormInfo.nWorm;
        }; // for
        if (count != wormCountLine.intValue()) {
            JOptionPane.showMessageDialog(this, "Worm count in " + RESULT_TXT + " file mismatch with actual worms count!", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }; // if
        return retList;
    }

    /**
     * Manual Inspection of images
     *
     * @param directory the directory containing RESULT_TXT and plateImg.jpg
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

        // read egg count from text file
        List<WormInfo> readWormsList = readWormsFromFile(resultsFile);
        if (readWormsList == null) {
            JOptionPane.showMessageDialog(this, "Unable to load any data from " + RESULT_TXT, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        if (readWormsList.isEmpty() == true) {
            JOptionPane.showMessageDialog(this, "Nothing to do!\nZero worms data in " + RESULT_TXT, "Nothing to do!", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // place the objects into a set for sorting purposes
        Set<WormInfo> wormInfoSet = new TreeSet<WormInfo>();
        int verifyWormCount = 0;
        for (WormInfo each : readWormsList) {
            wormInfoSet.add(each);
            verifyWormCount += each.nWorm;
        }; // for

        // get the ordered elements into the display-list
        wormDisplayList.clear();
        int secondVerification = 0;
        for (WormInfo each : wormInfoSet) {
            wormDisplayList.add(each);
            secondVerification += each.nWorm;
        }; // for

        // just in case verification
        if (verifyWormCount != secondVerification) {
            JOptionPane.showMessageDialog(this, "Error, mismatch counted worms (" + verifyWormCount + ") and second verification (" + secondVerification + ").", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // find out max height and width
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
        dialog.setPreferredSize(new Dimension(1200, 860));
        Container container = dialog.getContentPane();
        container.setLayout(new GridBagLayout());
        container.removeAll();

        // the buttons
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

            // increases for next set of button+spinner
            gridx++;
            if (gridx == (gridxStart + MAX_ON_X)) {
                gridx = gridxStart;
                gridy += 2;
            }; // if
        }; // for

        // previous page button
        prevPageButton.setEnabled(true);
        GridBagConstraints gridBag = new GridBagConstraints();
        gridBag.gridx = 0;
        gridBag.gridy = gridy;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(prevPageButton, gridBag);
		  prevPageButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("PAGE_UP"), PREV_PAGE);
		  prevPageButton.getActionMap().put(PREV_PAGE, prevPageButton.getAction());

        // next page button
        nextPageButton.setMnemonic(KeyEvent.VK_N);
        nextPageButton.setActionCommand(NEXT_PAGE);
        gridBag = new GridBagConstraints();
        gridBag.gridx = 1;
        gridBag.gridy = gridy;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(nextPageButton, gridBag);
		  nextPageButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("PAGE_DOWN"), NEXT_PAGE);
		  nextPageButton.getActionMap().put(NEXT_PAGE, nextPageButton.getAction());

        // Uncheck all button
        uncheckAllButton.setActionCommand(UNCHECKALL);
        gridBag = new GridBagConstraints();
        gridBag.gridx = 2;
        gridBag.gridy = gridy;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(uncheckAllButton, gridBag);


        // status label
        gridBag = new GridBagConstraints();
        gridBag.gridx = 0;
        gridBag.gridy = gridy + 1;
        gridBag.fill = GridBagConstraints.HORIZONTAL;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.LINE_START;
        gridBag.gridwidth = 3;
        container.add(statusLabel, gridBag);

        // view plate button
        viewPlateButton.setActionCommand(VIEW_PLATE);
        gridBag = new GridBagConstraints();
        gridBag.gridx = 2;
        gridBag.gridy = gridy + 1;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(viewPlateButton, gridBag);


        // save button
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
        gridBag.gridx = 1;
        gridBag.gridy = gridy + 2;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(closeButton, gridBag);

        // save and close button
        saveCloseButton.setActionCommand(SAVEEXIT);
        gridBag = new GridBagConstraints();
        gridBag.gridx = 2;
        gridBag.gridy = gridy + 2;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(saveCloseButton, gridBag);


        directoryLocation = directory.getAbsolutePath();

        // load the assembled image
        assembled_org = new ImagePlus(directory.getAbsolutePath() + File.separator + Utilities.ASSEMBLED_JPEG);
        if (assembled_org == null) {
            JOptionPane.showMessageDialog(this, "Error, unable to find assembled image (in folder: " + directory.getAbsolutePath() + ")", "Error, do image processing first!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //croppedImagePlus.getProcessor().autoThreshold();
        updateImageIconsCache(directory);

        currentPage = 0;
        prevPageButton.doClick();

        dialog.setSize(dialog.getPreferredSize());
        dialog.setVisible(true);
    }

    /**
     * Reads a file
     *
     * @param file the file
     * @return the lines of the file, or null when there is error
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
                bufferedReader.close();
                return null;
            }; // if
            bufferedReader.close();
        } catch (FileNotFoundException fnfe) {
            JOptionPane.showMessageDialog(this, "File not found: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, "Input/output error with file: " + file.getAbsolutePath(), "Eror", JOptionPane.ERROR_MESSAGE);
            return null;
        }; // try
        return linesList;
    }

    /**
     * Runs the app via a runnable invocation
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // do nothing
        }; // try
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }


    /**
     * inner-class for Action of next-page
     */
    public class NextPageAction extends AbstractAction {

        /**
         * constructor
         */
        public NextPageAction(String text, Integer mnemonic) {
            super(text);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            pageChange(NEXT_PAGE);
        }
    }


    /**
     * inner-class for Action of prev-page
     */
    public class PrevPageAction extends AbstractAction {

        /**
         * constructor
         */
        public PrevPageAction(String text, Integer mnemonic) {
            super(text);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
            pageChange(PREV_PAGE);
        }
    }


} // class App

