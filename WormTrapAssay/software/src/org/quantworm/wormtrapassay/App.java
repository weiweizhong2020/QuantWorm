/*
 * Filename: App.java
 * This is the main class which shows the main application window
 * containing 'Analzye One Video', 'Batch Processing', and 'Print Report' buttons 
 */
package org.quantworm.wormtrapassay;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
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

public class App extends JPanel implements ActionListener {

    // serial version UID
    private static final long serialVersionUID = 1L;
    /**
     * the version of this software
     */
    public static final String VERSION = "WormTrapAssay 3/30/2015";
    protected static final String ANALYZE_ONE_VIDEO = "Analyze One Video";
    protected static final String BATCH_PROCESSING = "Batch Processing";
    protected static final String PRINT_REPORT = "Print Report";
    private final JFrame parent;
    protected final JFileChooser fileChooser;
    protected final JFileChooser videoFileChooser;
    protected final JFileChooser videoTemplateFileChooser;
    protected final BatchProcessor batchProcessor;

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

        JButton printRerportButton = new JButton(PRINT_REPORT);
        printRerportButton.setActionCommand(PRINT_REPORT);
        printRerportButton.addActionListener(this);
        printRerportButton.setMnemonic(KeyEvent.VK_P);

        //Add menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menuOptions = new JMenu("Options");
        parent.setJMenuBar(menuBar);
        menuBar.add(menuOptions);
        JMenuItem menuCreateNewMask = new JMenuItem("Create new mask image from video");
        menuOptions.add(menuCreateNewMask);
        menuCreateNewMask.setActionCommand("CreateNewMask");
        menuCreateNewMask.addActionListener(this);

        // Add icon image
        ImageIcon logoImageIcon = null;
        File logoFile = new File("images" + File.separator + "logo.png");
        if (logoFile.exists() == true) {
            logoImageIcon = new ImageIcon(logoFile.getAbsolutePath(), VERSION);
        } else {
            URL imageUrl = getClass().getResource("/logo.png");
            Image image = Toolkit.getDefaultToolkit().getImage(imageUrl);
            logoImageIcon = new ImageIcon(image, VERSION);
        }
        JLabel logoLabel = null;
        if (logoImageIcon == null) {
            logoLabel = new JLabel("");
        } else {
            logoLabel = new JLabel(logoImageIcon);
            this.parent.setIconImage(logoImageIcon.getImage());
        }

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(logoLabel);

        add(Box.createRigidArea(new Dimension(20, 0)));
        add(analyzeOneVideoButton);
        add(Box.createRigidArea(new Dimension(20, 1)));
        add(batchProcessingButton);
        add(Box.createRigidArea(new Dimension(20, 2)));
        add(printRerportButton);

        if ("/".equals(File.separator) == true) {
            fileChooser = new JFileChooser(System.getProperty("user.home"));
            videoFileChooser = new JFileChooser(System.getProperty("user.home"));
            videoTemplateFileChooser = new JFileChooser(System.getProperty("user.home"));
        } else {
            fileChooser = new JFileChooser("c:\\data");
            videoFileChooser = new JFileChooser("c:\\data");
            videoTemplateFileChooser = new JFileChooser("c:\\data");
        }
        videoFileChooser.setFileFilter(new VideoFileFilter());
        videoFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        videoTemplateFileChooser.setFileFilter(new VideoFileFilter());
        videoTemplateFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        batchProcessor = new BatchProcessor(this.parent);
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
    @Override
    public void actionPerformed(ActionEvent actionEvent) {

        if (ANALYZE_ONE_VIDEO.equals(actionEvent.getActionCommand()) == true) {
            int returnValue = videoFileChooser.showOpenDialog(this);
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File video = videoFileChooser.getSelectedFile();

            VideoAnalyzer videoAnalyzer = new VideoAnalyzer(video);

            String returnMsg = videoAnalyzer.run();
            if (returnMsg == null) {
                JOptionPane.showMessageDialog(this,
                        "Image processing successfully completed!",
                        "Analyze One Video",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        returnMsg,
                        "Analyze One Video",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        if (BATCH_PROCESSING.equals(actionEvent.getActionCommand()) == true) {
            batchProcessor.go(fileChooser);
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
                        "Batch processing completed",
                        "Finished. ",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        error, "Error in creating the report.",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (actionEvent.getActionCommand().equals("CreateNewMask")) {
            int returnValue = videoTemplateFileChooser.showOpenDialog(this);
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File video = videoTemplateFileChooser.getSelectedFile();

            MaskCreator maskCreator = new MaskCreator(parent, video);

            String returnMsg = maskCreator.createMask();
            if (returnMsg == null) {
                JOptionPane.showMessageDialog(this,
                        "Mask image successfully created!",
                        "New mask image",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        returnMsg,
                        "New mask image",
                        JOptionPane.ERROR_MESSAGE);
            }

        }

    }

    /**
     * Runs the application via a runnable invocation
     *
     * @param args
     */
    public static void main(String[] args) {
        // want windows look-and-feel
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            // do nothing
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }

}
