/**
 * Filename: App.java This is the main class which shows the application window
 * containing 'Image Processing','Manual Inspection, and 'Print Report' buttons
 */
package org.quantworm.wormgender;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class App extends JPanel implements ActionListener {

    // serial version UID
    private static final long serialVersionUID = 11L;

    /**
     * the version of this software
     */
    public static final String VERSION = "4/16/2015";

    public static final String TITLE = "WormGender " + VERSION;

    protected static final String IMAGE_PROCESSING = "Image Processing";
    protected static final String MANUAL_INSPECTION = "Manual Inspection";
    protected static final String PRINT_REPORT = "   Print Report   ";
    protected static final String ANALYZE_TRAININGIMAGES = "Analyze and Create New Traning Set ...";

    //Constant for font of labels, text-editors, buttons
    public static final String FONT_LABELS = "Arial-PLAIN-16";

    private final JFrame parent;
    protected final JFileChooser fileChooser;
    protected final GenderAnalyzer genderAnalyzer;

    protected final ManualInspection manualInspection;
    protected final JButton imageProcessingButton = new JButton(IMAGE_PROCESSING);
    protected final JButton manualInspectionButton = new JButton(MANUAL_INSPECTION);
    protected final JButton printReportButton = new JButton(PRINT_REPORT);

    /**
     * Constructor
     *
     * @param parent the parent frame
     */
    public App(JFrame parent) {
        this.parent = parent;

        //Define window
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        final int SPACING = 20;
        setBorder(BorderFactory.createEmptyBorder(SPACING, SPACING, SPACING, SPACING));
        parent.setLocation(150, 100);
        parent.setResizable(false);

        // Add icon image
        ImageIcon logoImageIcon;
        File imageFile = new File("images" + File.separator + "logo.png");
        if (imageFile.exists() == true) {
            logoImageIcon = new ImageIcon(imageFile.getAbsolutePath(), VERSION);
        } else {
            URL imageUrl = getClass().getResource("/logo.png");
            Image image = Toolkit.getDefaultToolkit().getImage(imageUrl);
            logoImageIcon = new ImageIcon(image, VERSION);
        }
        JLabel logoLabel = new JLabel(logoImageIcon);
        this.parent.setIconImage(logoImageIcon.getImage());
        Dimension spacerDimension = new Dimension(SPACING, 0);
        add(logoLabel);

        // add buttons
        imageProcessingButton.setActionCommand(IMAGE_PROCESSING);
        imageProcessingButton.setMnemonic(KeyEvent.VK_I);
        imageProcessingButton.addActionListener(this);
        imageProcessingButton.setFont(Font.decode(FONT_LABELS));
        add(Box.createRigidArea(spacerDimension));
        add(imageProcessingButton);

        manualInspectionButton.setActionCommand(MANUAL_INSPECTION);
        manualInspectionButton.setMnemonic(KeyEvent.VK_M);
        manualInspectionButton.addActionListener(this);
        manualInspectionButton.setFont(Font.decode(FONT_LABELS));
        add(Box.createRigidArea(spacerDimension));
        add(manualInspectionButton);

        printReportButton.setActionCommand(PRINT_REPORT);
        printReportButton.setMnemonic(KeyEvent.VK_P);
        printReportButton.addActionListener(this);
        printReportButton.setFont(Font.decode(FONT_LABELS));
        add(Box.createRigidArea(spacerDimension));
        add(printReportButton);

        //Add menu
        JMenuBar menuBar = new JMenuBar();
        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setFont(Font.decode(FONT_LABELS));
        optionsMenu.setMnemonic(KeyEvent.VK_O);
        menuBar.add(optionsMenu);
        parent.setJMenuBar(menuBar);
        JMenuItem menuItem = new JMenuItem(ANALYZE_TRAININGIMAGES);
        menuItem.setActionCommand(ANALYZE_TRAININGIMAGES);
        menuItem.addActionListener(this);
        menuItem.setMnemonic(KeyEvent.VK_A);
        menuItem.setFont(Font.decode(FONT_LABELS));
        optionsMenu.add(menuItem);

        if ("/".equals(File.separator) == true) {
            fileChooser = new JFileChooser(System.getProperty("user.home") + "/data/");
        } else {
            fileChooser = new JFileChooser("c:\\data");
        }
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        genderAnalyzer = new GenderAnalyzer(this.parent);
        manualInspection = new ManualInspection(this.parent, fileChooser);

        
        // The following code might not be used by users.
        // This is for optimizing parameters
        // conductParameterChangeAnalysis(); 
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
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (IMAGE_PROCESSING.equals(actionEvent.getActionCommand()) == true) {

            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int state = fileChooser.showOpenDialog(null);
            if (state != JFileChooser.APPROVE_OPTION) {
                return;
            }

            // the following (true) creates review images 
            genderAnalyzer.isCreateReviewOverlayedImage = false;

            List<String> errorsList = genderAnalyzer.do_BatchProcessing(
                    fileChooser.getSelectedFile().getAbsolutePath());

            if (errorsList == null) {
                JOptionPane.showMessageDialog(this, "Image Processing completed!\n\nFolder:\n"
                        + fileChooser.getSelectedFile().getAbsolutePath(),
                        "Image Processing Completed.", JOptionPane.INFORMATION_MESSAGE);
            } else {
                String errors = "Errors detected: ";
                for (String each : errorsList) {
                    errors += "\n" + each;
                }
                JOptionPane.showMessageDialog(this, "Image Processing completed (with errors).\n\n"
                        + errors, "Image Processing Completed.", JOptionPane.ERROR_MESSAGE);
            }
        }

        if (MANUAL_INSPECTION.equals(actionEvent.getActionCommand()) == true) {
            int state = fileChooser.showOpenDialog(this);
            if (state == JFileChooser.APPROVE_OPTION) {
                File folder = fileChooser.getSelectedFile();
                manualInspection.go(folder);
            }
        }

        if (PRINT_REPORT.equals(actionEvent.getActionCommand()) == true) {
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                return;
            }
            PrintReportProcessor resultProcessor = new PrintReportProcessor(this.parent);
            String error = resultProcessor.go(fileChooser);
            if (error == null) {
                JOptionPane.showMessageDialog(this,
                        "Batch processing completed.\n" + resultProcessor.getStatus(),
                        "Finished. ",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        error, "Error in creating the report.",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        if (ANALYZE_TRAININGIMAGES.equals(actionEvent.getActionCommand()) == true) {
            analyzeAndCreateNewTrainingSet();
        }
    }

    /**
     * Conduct repeative-parameter-(loc1 and loc2)-change-analysis; this
     * function is used for optimizing parameters; do not delete this function
     * because even if rarely used, it should be available when a
     * programmer-user wants to choose parameters
     */
    public void conductParameterChangeAnalysis() {
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int state = fileChooser.showOpenDialog(null);
        if (state != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File srcFolder = fileChooser.getSelectedFile();

        genderAnalyzer.isCreateReviewOverlayedImage = false;
        genderAnalyzer.perform_ParameterChangeAnalysis(srcFolder);
        JOptionPane.showMessageDialog(this, "Processing completed!");
    }

    /**
     * Extract parameters from training images and create training set file
     * sourceFolderName: the root folder that includes three sub-folders of
     * .../herm, .../male, and .../L3L4 These three folders contain lots of jpg
     * images of a single worm
     */
    public void analyzeAndCreateNewTrainingSet() {

        
        
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int state = fileChooser.showOpenDialog(null);
        if (state != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File srcFolder = fileChooser.getSelectedFile();

        //If you want to create review image processing, 
        //set the following variable true
        genderAnalyzer.isCreateReviewOverlayedImage = true;
        genderAnalyzer.analyze_TrainingImages(
                srcFolder,
                new File(srcFolder.getAbsoluteFile()
                        + File.separator
                        + GenderAnalyzer.TRAINING_SET_FILENAME));

        int[] accuracyCountArray = new int[6];
        genderAnalyzer.calculate_AccuracyCount_InRecalling(srcFolder,
                new File(srcFolder.getAbsoluteFile()
                        + File.separator
                        + GenderAnalyzer.TRAINING_SET_FILENAME),
                accuracyCountArray);

        JOptionPane.showMessageDialog(this, "Completed creation and analysis of training set.\n\n"
                + "Results:\n"
                + accuracyCountArray[0] + " correct of "
                + (accuracyCountArray[0] + accuracyCountArray[3]) + " males.\n"
                + accuracyCountArray[1] + " correct of "
                + (accuracyCountArray[1] + accuracyCountArray[4]) + " hermaphrodites.\n"
                + accuracyCountArray[2] + " correct of "
                + (accuracyCountArray[2] + accuracyCountArray[5]) + " L3/L4.",
                "Completed Training Set Analysis.", JOptionPane.INFORMATION_MESSAGE);
        
    }

    /**
     * Runs the application via a runnable invocation
     *
     * @param args
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException e) {
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                } catch (UnsupportedLookAndFeelException e) {
                }
                createAndShowGUI();
            }
        });
    }

}

