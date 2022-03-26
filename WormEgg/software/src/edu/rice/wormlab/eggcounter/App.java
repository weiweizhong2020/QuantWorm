/*
 * Filename: App.java
 * This is the main class which shows the application window
 * containing 'Image Processing','Manual Inspection, and 'Print Report' buttons
 */
package edu.rice.wormlab.eggcounter;

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

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import java.util.Set;
import java.util.TreeSet;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;

public class App extends JPanel implements ActionListener, ChangeListener {

    // serial version UID
    private static final long serialVersionUID = 7L;
    /**
     * the version of this software
     */
    public static final String VERSION = "WormEgg " + "9/19/2013";
    public static final String RESULT_TXT = "result-egg.txt";
    public static final String HISTORICAL = "historical.";
    public static final String DETECTION_CONDITION_PRESET_FILENAME =
            "EggCounter_Detection_Conditon_Preset.txt";
    public static final String ASSEMBLED_JPEG = "assembled.jpeg";
    public static final String ASSEMBLED_COLORS_JPEG = "assembled_colors.jpeg";
    public static final String HUMAN_INSPECTION = "Human-inspection";
    protected static final String IMAGE_PROCESSING = "Image Processing";
    protected static final String MANUAL_INSPECTION = "Manual Inspection";
    protected static final String PRINT_REPORT = "Print Report";
    protected static final String IMGBUTTON = "imgbutton";
    protected static final String PREV_PAGE = "previous";
    protected static final String NEXT_PAGE = "next";
    protected static final String SAVE = "save";
    protected static final String CLOSE = "close";
    protected static final String VIEW_PLATE = "view plate";
    protected static final int IMG_BUTTON_TOTAL = 12;
    protected static final String EGG_COUNT = "# Egg count:";
    protected static final String METHOD_MANUAL = "# Manual inspection:";
    private static final PrintStream out = System.out;
    public final JFrame parentFrame;
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
    private int currentPage;
    private int ceiling;
    private String directoryLocation;
    private ImagePlus assembled;
    public DetectionCondition detectionCondition = new DetectionCondition();
    protected final JFileChooser fileChooser;

    /**
     * Constructor
     *
     * @param frame the parent frame
     */
    public App(JFrame frame) {
        this.parentFrame = frame;

        // buttons from left to right
        JButton imageProcessingButton = new JButton(IMAGE_PROCESSING);
        imageProcessingButton.setActionCommand(IMAGE_PROCESSING);
        imageProcessingButton.addActionListener(this);
        imageProcessingButton.setMnemonic(KeyEvent.VK_I);

        JButton manualInspectionButton = new JButton(MANUAL_INSPECTION);
        manualInspectionButton.setActionCommand(MANUAL_INSPECTION);
        manualInspectionButton.addActionListener(this);
        manualInspectionButton.setMnemonic(KeyEvent.VK_M);

        JButton printReportButton = new JButton(PRINT_REPORT);
        printReportButton.setActionCommand(PRINT_REPORT);
        printReportButton.addActionListener(this);
        printReportButton.setMnemonic(KeyEvent.VK_P);

        // Add icon image
        ImageIcon logoImageIcon = null;
        File imageFile = new File("images" + File.separator + "logo.png");
        if (imageFile.exists() == true) {
            logoImageIcon = new ImageIcon(imageFile.getAbsolutePath(), VERSION);
        } else {
            URL imageUrl = getClass().getResource("/logo.png");
            Image image = Toolkit.getDefaultToolkit().getImage(imageUrl);
            logoImageIcon = new ImageIcon(image, VERSION);
        }
        JLabel logoLabel = new JLabel(logoImageIcon);

        parentFrame.setIconImage(logoImageIcon.getImage());

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(logoLabel);
        add(Box.createRigidArea(new Dimension(20, 0)));
        add(imageProcessingButton);
        add(Box.createRigidArea(new Dimension(20, 0)));
        add(manualInspectionButton);
        add(Box.createRigidArea(new Dimension(20, 0)));
        add(printReportButton);


        //Load detection parameter setting profiles
        String[] titleArray = detectionCondition.get_DetectionConditionPreset_Titles(
                DETECTION_CONDITION_PRESET_FILENAME);
		  if( titleArray == null ) {
			  // attempt to load it from above folder 
			  titleArray = detectionCondition.get_DetectionConditionPreset_Titles(
                ".." + File.separator + DETECTION_CONDITION_PRESET_FILENAME);
		  }; // if

        EggCounter.detectionCondition =
                detectionCondition.set_DetectionConditionPreset(
                DETECTION_CONDITION_PRESET_FILENAME,
                titleArray[0]);


        if ("/".equals(File.separator) == true) {
            fileChooser = new JFileChooser( System.getProperty( "user.home" ) );
        } else {
            fileChooser = new JFileChooser("c:\\data");
        }; // if
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        spinner = new JSpinner[IMG_BUTTON_TOTAL];
        wormDisplayList = new ArrayList<WormInfo>();
        dialog = new JDialog(parentFrame);
        // stop automatic closing
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                closeButton.doClick();
            }
        });
        prevPageButton = new JButton(new PrevPageAction(PREV_PAGE, KeyEvent.VK_P));
        nextPageButton = new JButton(new NextPageAction(NEXT_PAGE, KeyEvent.VK_N));
        closeButton = new JButton(CLOSE);
        closeButton.addActionListener(this);
        saveButton.addActionListener(this);
        viewPlateButton.addActionListener(this);
        directoryLocation = null;
        assembled = null;
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
                EggCounter ia = new EggCounter();
                ia.initialize();
                DirectoryReader dr = new DirectoryReader();
                String allSubFolders[] = dr.scanAllSubFolders(fileChooser.getSelectedFile().getAbsolutePath());
                ia.addFolders(allSubFolders);
                Thread thread = new Thread(ia);
                thread.start();
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
        if (wormDisplayList.isEmpty() == false) {
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
                        imgButton[ buttonIndex].setForeground(Color.black);
                        imgButton[ buttonIndex].setText(worm.nWorm + " ");
                    } else {
                        worm.changedN = 0;
                        spinner[ buttonIndex].setValue(0);
                        imgButton[ buttonIndex].setForeground(Color.red);
                        imgButton[ buttonIndex].setText("deleted");
                    }; // if
                    updateLabel();
                }; // if
            }; // for
        }
        if (CLOSE.equals(actionEvent.getActionCommand()) == true) {
            closeIt();
        }; // if
        if (SAVE.equals(actionEvent.getActionCommand()) == true) {
            save(this);
        }; // if
        if (VIEW_PLATE.equals(actionEvent.getActionCommand()) == true) {
            viewPlate();
        }; // if
    }

    /**
     * displays the plate with colorful annotations on worms
     */
    protected void viewPlate() {
        TypeConverter typeConverter = new TypeConverter(assembled.getProcessor(), false);
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
            // place the objects into a set for sorting purposes
            Set<WormInfo> wormInfoSet = new TreeSet<WormInfo>();
            for (WormInfo each : wormDisplayList) {
                wormInfoSet.add(each);
            }; // for

            // get the ordered elements into the display-list
            wormDisplayList.clear();
            for (WormInfo each : wormInfoSet) {
                wormDisplayList.add(each);
            }; // for

            currentPage = 0;
            pageChange(PREV_PAGE);
            dialog.validate();
            dialog.repaint();
        }; // if
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
    public static void save(App app) {
        if (app.directoryLocation == null) {
            JOptionPane.showMessageDialog(app, "Internal error, directory is null!", "Internal Error (save-button)", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // count #eggs
        int eggCount = 0;
        for (WormInfo worm : app.wormDisplayList) {
            if (worm.changedN == null) {
                eggCount += worm.nWorm;
            } else {
                if (worm.changedN == worm.nWorm) {
                    eggCount += worm.nWorm;
                } else {
                    eggCount += worm.changedN;
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
        File oldResultsFile = new File(app.directoryLocation + File.separator + RESULT_TXT);
        boolean renamedFlag = oldResultsFile.renameTo(historicalFile);
        if (renamedFlag == false) {
            JOptionPane.showMessageDialog(app, "Error, unable to rename file " + RESULT_TXT + " to a historical filename.", "Cannot save!", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if

        // save the new contents
        try {
            FileWriter fileWriter = new FileWriter(app.directoryLocation + File.separator + RESULT_TXT);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter printWriter = new PrintWriter(bufferedWriter);
            printWriter.println("# File saved at:\t" + new Date());
            printWriter.println(EGG_COUNT + "\t" + eggCount);
            printWriter.println("# Automated counting:\tDone");
            printWriter.println("# Manual inspection:\tDone");
            printWriter.println("# pX\tpY\twidth\theight\tnEgg\tprobability\tmethod");
            for (WormInfo worm : app.wormDisplayList) {
                if (worm.changedN == null) {
                    printWriter.println(worm.toString());
                } else {
                    if (worm.changedN != worm.nWorm) {
                        // easy case: deletion, do not write it to file anymore
                        if (worm.changedN == 0) {
                            continue;
                        }
                        printWriter.println(worm.pX + "\t" + worm.pY + "\t" + worm.width + "\t" + worm.height + "\t" + worm.changedN + "\t" + worm.probability + "\t" + HUMAN_INSPECTION);
                    }; // if
                }; // if
            }; // for
            printWriter.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            JOptionPane.showMessageDialog(app, "Error when saving " + RESULT_TXT + " as follows:<br>" + ioe, "I/O Error", JOptionPane.ERROR_MESSAGE);
            return;
        }; // try

        // create the annotated image
        ImageProcessor imageProcessor = app.assembled.getProcessor();
        TypeConverter typeConverter = new TypeConverter(imageProcessor, false);
        ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
        final int defaultColor = Color.BLUE.getRGB();
        final int strokeWidth = 1;
        for (WormInfo worm : app.wormDisplayList) {
            number = worm.nWorm;
            if (worm.changedN != null && worm.changedN != number) {
                number = worm.changedN;
            }; // if
            if (number == 0) {
                continue;
            }; // if
            Roi roi = new Roi(worm.pX - strokeWidth - 1, worm.pY - strokeWidth - 1, worm.width + strokeWidth + 1, worm.height + strokeWidth + 1);
            colorProcessor.setRoi(roi);
            roi.setStrokeWidth(strokeWidth);
            colorProcessor.setValue(defaultColor);
            roi.drawPixels(colorProcessor);
            colorProcessor.setValue(Color.RED.getRGB());
            colorProcessor.drawString("" + number, worm.pX - strokeWidth - 2, worm.pY - strokeWidth - 2);
        }; // for
        ImagePlus imagePlus = new ImagePlus(app.directoryLocation, colorProcessor);
        FileSaver fileSaver = new FileSaver(imagePlus);
        fileSaver.saveAsJpeg(app.directoryLocation + File.separator + ASSEMBLED_COLORS_JPEG);
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
                //text += "<br>worm[" + q + "] was: " + worm.nWorm + " , now: " + worm.changedN;
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
        String changes = "<br>" + wormCount + " eggs.";
        if (wormCount != changesCount) {
            changes = "<br>Intially " + wormCount + " eggs, now: " + changesCount + " eggs.";
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
        final int extraX = 10;
        final int extraY = 10;
        
        if (wormDisplayList.isEmpty()) {
            return;
        }
        
        for (int buttonIndex = 0; buttonIndex < IMG_BUTTON_TOTAL; buttonIndex++) {
            SpinnerNumberModel spinnerNumberModel = (SpinnerNumberModel) spinner[ buttonIndex].getModel();
            
            if (i < wormDisplayList.size()) {
                WormInfo worm = wormDisplayList.get(i);
                imgButton[ buttonIndex].setEnabled(true);
                imgButton[ buttonIndex].setPreferredSize(new Dimension(worm.width + extraX * 2, worm.height + extraY * 2 + 4));
                imgButton[ buttonIndex].setMinimumSize(new Dimension(worm.width + extraX * 2, worm.height + extraY * 2 + 4));
                ImagePlus preview = NewImage.createByteImage("" + worm.nWorm, worm.width + extraX * 2, worm.height + extraY * 2, 1, NewImage.GRAY8);
                for (int x = 0 - extraX; x < worm.width + extraX; x++) {
                    for (int j = 0 - extraY; j < worm.height + extraY; j++) {
                        preview.getProcessor().putPixel(x + extraX, j + extraY, assembled.getPixel(worm.pX + x, worm.pY + j));
                    }; // for
                }; // for
                ImageProcessor previewColor = preview.getProcessor().convertToRGB();
                previewColor.setColor(Color.RED);
                // vertical lines
                previewColor.drawLine(extraX, 2, extraX, extraY);
                previewColor.drawLine(extraX + worm.width, 2, extraX + worm.width, extraY);
                previewColor.drawLine(extraX, extraY + worm.height, extraX, extraY * 2 + worm.height - 2);
                previewColor.drawLine(extraX + worm.width, extraY + worm.height, extraX + worm.width, extraY * 2 + worm.height - 2);
                // horizontal lines
                previewColor.drawLine(2, extraY, extraX, extraY);
                previewColor.drawLine(extraX + worm.width, extraY, extraX * 2 + worm.width - 2, extraY);
                previewColor.drawLine(2, extraY + worm.height, extraX, extraY + worm.height);
                previewColor.drawLine(extraX + worm.width, extraY + worm.height, extraX * 2 + worm.width - 2, extraY + worm.height);
                imgButton[ buttonIndex].setIcon(new ImageIcon(previewColor.getBufferedImage()));
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


    /**
     * Changes of spinners show up here
     *
     * @param changeEvent the change event object
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
                    JOptionPane.showMessageDialog(this, "Internal error, egg at index " + wormIndex + " is null!", "Internal (stateChanged)", JOptionPane.ERROR_MESSAGE);
                    break;
                }; // if
                Integer value = new Integer(spinner[ buttonIndex].getValue().toString());
                if (value == 0) {
                    imgButton[ buttonIndex].setForeground(Color.red);
                    imgButton[ buttonIndex].setText("deleted");
                    worm.changedN = 0;
                } else {
                    imgButton[ buttonIndex].setForeground(Color.black);
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
        Integer eggCountLine = null;
        for (String each : linesList) {
            String[] items = each.split("\t");
            if (each.startsWith("#") == true) {
                if (EGG_COUNT.equals(items[ 0]) == true) {
                    eggCountLine = Utilities.getInteger(items[ 1]);
                }; // if
                continue;
            }; // if
            if (items.length == 7) {
                WormInfo wormInfo = WormInfo.createWormInfo(items);
                if (wormInfo == null) {
                    JOptionPane.showMessageDialog(this, "Unable to process data of line number " + lineNumber + " of " + RESULT_TXT + "\nLine: " + each.replaceAll("\t", " "), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    retList.add(wormInfo);
                }; // if
            } else {
                JOptionPane.showMessageDialog(this, "Line " + lineNumber + " in unexpected format.\nIn file: " + RESULT_TXT + "\nLine: " + each.replaceAll("\t", " "), "Error", JOptionPane.ERROR_MESSAGE);
            }; // if
        }; // for
        if (eggCountLine == null) {
            JOptionPane.showMessageDialog(this, "Unable to process data of number of eggs (egg count is undefined)", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }; // if
        int count = 0;
        for (WormInfo wormInfo : retList) {
            count += wormInfo.nWorm;
        }; // for
        if (count != eggCountLine.intValue()) {
            JOptionPane.showMessageDialog(this, "Egg count in " + RESULT_TXT + " file mismatch with actual egg count!", "Error", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog( this, "Zero eggs detected!\n\nSuggestion: Use Plate-View to add egg-objects.", "Notice: zero eggs detected!", JOptionPane.WARNING_MESSAGE );
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

        // verify the max width and height
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
        dialog.setPreferredSize(new Dimension(800, 600));
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
        gridBag = new GridBagConstraints();
        gridBag.gridx = 1;
        gridBag.gridy = gridy;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(nextPageButton, gridBag);
        nextPageButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("PAGE_DOWN"), NEXT_PAGE);
        nextPageButton.getActionMap().put(NEXT_PAGE, nextPageButton.getAction());

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

        // load the assembled image
        ScannerLog scannerLog = ScannerLog.readLog(directory);
        String errors = scannerLog.getErrors();
        if (errors != null) {
            JOptionPane.showMessageDialog(this, "Error assembling image.\nError: " + errors, "Error assembling image", JOptionPane.ERROR_MESSAGE);
            return;
        }; // if
        assembled = Utilities.assembleImage(directory.getAbsolutePath(), true, scannerLog);

        currentPage = 0;
        directoryLocation = directory.getAbsolutePath();
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
            JOptionPane.showMessageDialog(this, "File input/output error with file: " + file.getAbsolutePath(), "Eror", JOptionPane.ERROR_MESSAGE);
            return null;
        }; // try
        return linesList;
    }

    /**
     * Runs the app via a runnable invocation
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }; // try				
                createAndShowGUI();
            }
        });
    }
} // class App

