/**
 * Filename: ManualInspection.java Manual inspection dialog window
 */
package org.quantworm.wormgender;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ManualInspection extends JDialog implements ActionListener, ChangeListener {

    public static final String PREV_PAGE = "    Previous    ";
    public static final String NEXT_PAGE = "    Next    ";
    public static final String SAVE = "Save";
    public static final String CLOSE = "Close";
    public static final String VIEW_PLATE = "Plate View";
    public static final String SAVEEXIT = "Save & Close";
    public static final String LOAD = "Load";
    public static final String BROWSE = "Browse";

    // constant used in naming action-commands in buttons
    protected static final String IMGBUTTON = "imgbutton";

    /**
     * constant used for files kept as historical
     */
    public static final String HISTORICAL = "historical.";

    // constant used in popup-menu option 'view image' (first option)
    protected static final String VIEW_IMAGE_POPUP = "View-image";

    // constant used in popup-menu option 1 male, 1 herma
    protected static final String ONE_AND_ONE_POPUP = "1 male, 1 hermaphrodite";

    // constant used in popup-menu option 2 hermas
    protected static final String TWO_HERMA_POPUP = "2 hermaphrodites";

    // constant used in popup-menu option 3 herma
    protected static final String THREE_HERMA_POPUP = "3 hermaphrodites";

    // constant used in popup-menu option 2 males
    protected static final String TWO_MALES_POPUP = "2 males";

    // constant used in popup-menu option 3 males
    protected static final String THREE_MALES_POPUP = "3 males";

    // constant used in popup-menu option suspicious
    protected static final String SUSPICIOUS_POPUP = "Unknown";

    // constant used in popup-menu option 'save image' (one before last)
    protected static final String SAVE_IMAGE_POPUP = "Save-image";

    // constant used in popup-menu option 'reset' (last option)
    protected static final String RESET_POPUP = "Reset";

    // all the constants in pop-menu options
    protected static final String[] ALL_POPUP_MENUS = new String[]{VIEW_IMAGE_POPUP, null, SUSPICIOUS_POPUP, null,
        ONE_AND_ONE_POPUP, TWO_HERMA_POPUP, THREE_HERMA_POPUP, TWO_MALES_POPUP, THREE_MALES_POPUP,
        null, SAVE_IMAGE_POPUP, RESET_POPUP};

    private final JButton prevPageButton;
    private final JButton nextPageButton;
    private final JButton closeButton;
    private final JButton saveButton = new JButton(SAVE);
    private final JButton viewPlateButton = new JButton(VIEW_PLATE);
    private final JButton saveCloseButton;
    private final JButton loadButton = new JButton(LOAD);
    private final JButton browseButton = new JButton(BROWSE);
    private final JLabel statusLabel = new JLabel();
    private final JLabel folderLabel = new JLabel("Folder:");
    private final JTextField folderTextField = new JTextField(60);
    private final JFileChooser fileChooser;

    /**
     * total number of image buttons displayed
     */
    public static final int IMG_BUTTON_TOTAL = 6;

    /**
     * icon padding pixels x-axis
     */
    public static final int ICON_PADDING_X = 20;

    /**
     * icon padding pixels y-axis
     */
    public static final int ICON_PADDING_Y = 20;

    // dummy icon dimensions
    protected final Dimension DUMMY_DIMENSION = new Dimension(20, 20);

    // dummy insets
    protected final Insets DUMMY_INSETS = new Insets(2, 2, 2, 2);

    // the list of worms that the user is looking at (not necessarily same as saved in disk)
    protected final List<WormInfo> wormDisplayList;

    // the buttons to use for inspection
    private final JButton[] imgButton = new JButton[IMG_BUTTON_TOTAL];

    // the spinner to use with buttons for male
    private JSpinner[] maleSpinner = new JSpinner[IMG_BUTTON_TOTAL];

    // the spinner to use with buttons for male
    private JSpinner[] hermSpinner = new JSpinner[IMG_BUTTON_TOTAL];

    // the spinner-model to use with buttons for male
    private SpinnerModel[] maleSpinnerModel = new SpinnerModel[IMG_BUTTON_TOTAL];

    // the spinner-model to use with buttons for herm
    private SpinnerModel[] hermSpinnerModel = new SpinnerModel[IMG_BUTTON_TOTAL];

    // the directory containing well or plate image
    private File directory;

    // the 'enhanced' assembled image
    private ImagePlus assembledEnhanced;

    // the cache of image-icons to be used in buttons
    private ImageIcon[] cachedImageIcons;

    // the results-gender object
    private final ResultsGender resultsGender = new ResultsGender();

    // the popup menu in img-buttons
    private final JPopupMenu popupMenu = new JPopupMenu();

    // the current page
    private int currentPage;

    // flag to remember whether user has clicked on view plate
    private boolean viewedPlateFlag;

    // keeps track of pages visited by human in the interface
    private Map<Integer, Boolean> pageVisitedMap = new TreeMap<Integer, Boolean>();

    // keeps track of opened windows via view-image in order to close them later
    private List<ImagePlus> stuffToCloseList = new ArrayList<ImagePlus>();

    // for convenience only
    private static final PrintStream out = System.out;

    /**
     * Constructor
     *
     * @param parent the parent frame
     * @param fileChooser the file chooser to use
     */
    public ManualInspection(JFrame parent, JFileChooser fileChooser) {
        super(parent);
        this.fileChooser = fileChooser;
        wormDisplayList = new ArrayList<WormInfo>();
        directory = null;

        assembledEnhanced = null;
        cachedImageIcons = null;
        currentPage = 0;
        viewedPlateFlag = false;

        // setup GUI components
        prevPageButton = new JButton(new PrevPageAction(PREV_PAGE, KeyEvent.VK_P));
        nextPageButton = new JButton(new NextPageAction(NEXT_PAGE, KeyEvent.VK_N));

        closeButton = new JButton(CLOSE);
        closeButton.addActionListener(this);
        closeButton.setMnemonic(KeyEvent.VK_C);

        saveButton.addActionListener(this);
        saveButton.setMnemonic(KeyEvent.VK_S);

        viewPlateButton.addActionListener(this);
        viewPlateButton.setMnemonic(KeyEvent.VK_V);

        saveCloseButton = new JButton(SAVEEXIT);
        saveCloseButton.addActionListener(this);
        saveCloseButton.setMnemonic(KeyEvent.VK_X);

        statusLabel.setText("");
        statusLabel.setFont(Font.decode(App.FONT_LABELS));
        folderLabel.setFont(Font.decode(App.FONT_LABELS));

        prevPageButton.setFont(Font.decode(App.FONT_LABELS));
        nextPageButton.setFont(Font.decode(App.FONT_LABELS));
        closeButton.setFont(Font.decode(App.FONT_LABELS));
        saveButton.setFont(Font.decode(App.FONT_LABELS));
        viewPlateButton.setFont(Font.decode(App.FONT_LABELS));
        saveCloseButton.setFont(Font.decode(App.FONT_LABELS));

        // stop automatic closing
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                closeButton.doClick();
            }
        });

        setPreferredSize(new Dimension(1200, 860));
        Container container = getContentPane();
        container.setLayout(new GridBagLayout());
        container.removeAll();

        // setup the folder-panel
        JPanel folderPanel = new JPanel();
        folderTextField.setFont(Font.decode(App.FONT_LABELS));
        loadButton.setFont(Font.decode(App.FONT_LABELS));
        loadButton.setMnemonic(KeyEvent.VK_L);
        loadButton.addActionListener(this);
        loadButton.setActionCommand(LOAD);
        browseButton.setFont(Font.decode(App.FONT_LABELS));
        browseButton.addActionListener(this);
        browseButton.setActionCommand(BROWSE);
        folderLabel.setLabelFor(folderTextField);
        folderLabel.setDisplayedMnemonic(KeyEvent.VK_D);
        folderPanel.add(folderLabel);
        folderPanel.add(folderTextField);
        folderPanel.add(loadButton);
        folderPanel.add(browseButton);
        container.add(folderPanel, new GBC(0, 0).setAnchor(GBC.LINE_START).setSpan(3, 1));

        // inspection buttons
        int gridxStart = 0;
        int gridyStart = 1;
        int gridx = gridxStart;
        int gridy = gridyStart;
        final int MAX_ON_X = 3;
        final int INST = 2;
        for (int i = 0; i < IMG_BUTTON_TOTAL; i++) {
            imgButton[ i] = new JButton();
            imgButton[ i].setHorizontalTextPosition(AbstractButton.LEADING);
            imgButton[ i].setMinimumSize(DUMMY_DIMENSION);
            imgButton[ i].setPreferredSize(DUMMY_DIMENSION);
            imgButton[ i].setActionCommand(IMGBUTTON + i);
            imgButton[ i].addActionListener(this);
            imgButton[ i].setIcon(null);
            imgButton[ i].setFont(Font.decode(App.FONT_LABELS));
            GridBagConstraints gridBag = new GridBagConstraints();
            gridBag.gridx = gridx;
            gridBag.gridy = gridy;
            gridBag.fill = GridBagConstraints.BOTH;
            gridBag.anchor = GridBagConstraints.CENTER;
            gridBag.insets = new Insets(6, 6, 2, 6);
            gridBag.weightx = 0.1;
            gridBag.weighty = 0.2;
            container.add(imgButton[ i], gridBag);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
            maleSpinnerModel[ i] = new SpinnerNumberModel();
            maleSpinner[ i] = new JSpinner(maleSpinnerModel[ i]);
            maleSpinner[ i].setFont(Font.decode(App.FONT_LABELS));
            maleSpinner[ i].addChangeListener(this);
            JComponent component = maleSpinner[ i].getEditor();
            NumberEditor numberEditor = (NumberEditor) component;
            JFormattedTextField formattedTextField = numberEditor.getTextField();
            formattedTextField.setColumns(2);
            SpinnerNumberModel spinnerNumberModel = (SpinnerNumberModel) maleSpinner[ i].getModel();
            spinnerNumberModel.setMinimum(0);
            hermSpinnerModel[ i] = new SpinnerNumberModel();
            hermSpinner[ i] = new JSpinner(hermSpinnerModel[ i]);
            hermSpinner[ i].addChangeListener(this);
            component = hermSpinner[ i].getEditor();
            numberEditor = (NumberEditor) component;
            formattedTextField = numberEditor.getTextField();
            formattedTextField.setColumns(2);
            spinnerNumberModel = (SpinnerNumberModel) hermSpinner[ i].getModel();
            spinnerNumberModel.setMinimum(0);
            JLabel maleLabel = new JLabel("males:");
            JLabel hermLabel = new JLabel("hermaphrodites:");
            maleLabel.setFont(Font.decode(App.FONT_LABELS));
            hermLabel.setFont(Font.decode(App.FONT_LABELS));

            buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(maleLabel);
            buttonPanel.add(maleSpinner[ i]);
            buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
            buttonPanel.add(hermLabel);
            buttonPanel.add(hermSpinner[ i]);
            buttonPanel.add(Box.createHorizontalGlue());
            container.add(buttonPanel, new GBC(gridx, gridy + 1).setAnchor(GBC.PAGE_END));

            // increases for next set of button+spinner
            gridx++;
            if (gridx == (gridxStart + MAX_ON_X)) {
                gridx = gridxStart;
                gridy += 2;
            }

            // add the popup menu into each button
            imgButton[ i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    mouseClickedOnEachButton(mouseEvent);
                }
            });
        }

        // previous page button
        prevPageButton.setEnabled(true);
        GridBagConstraints gridBag = new GridBagConstraints();
        gridBag.gridx = 0;
        gridBag.gridy = gridy;
        gridBag.insets = new Insets(6, 6, 2, 6);
        gridBag.anchor = GridBagConstraints.CENTER;
        container.add(prevPageButton, gridBag);
        prevPageButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("PAGE_UP"), PREV_PAGE);
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

        // setup the popup menu
        popupMenu.setBorderPainted(true);

        JMenuItem menuItem = null;
        for (String each : ALL_POPUP_MENUS) {
            if (each == null) {
                popupMenu.addSeparator();
                continue;
            }
            menuItem = new JMenuItem(each);
            menuItem.setActionCommand(each);
            menuItem.addActionListener(this);
            menuItem.setFont(Font.decode(App.FONT_LABELS));
            popupMenu.add(menuItem);
        }
    }

    /**
     * Actions of buttons take place here
     *
     * @param actionEvent the action-event object
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        // every option has a 'return' so that we can detect errors in the writing of the code
        if (CLOSE.equals(actionEvent.getActionCommand()) == true) {
            closeIt();
            return;
        }

        if (SAVE.equals(actionEvent.getActionCommand()) == true) {
            // load the assembled image
            ImagePlus assembledOriginal = new ImagePlus(directory.getAbsolutePath()
                    + File.separator + IJImgProcessing.ASSEMBLED_JPEG);
            assembledOriginal.trimProcessor();
            if (assembledOriginal == null) {
                JOptionPane.showMessageDialog(this,
                        "Error, unable to properly read assembled image!\nNothing was saved.\nLocation: "
                        + directory.getAbsolutePath(), "Error with: "
                        + IJImgProcessing.ASSEMBLED_JPEG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            save(wormDisplayList, getInspectedStatus(wormDisplayList,
                    viewedPlateFlag, pageVisitedMap), directory, this, assembledOriginal);
            // update images of each-buttons and screen components
            updateImageIconsCache();
            prevPageButton.setEnabled(true);
            currentPage = 0;
            prevPageButton.doClick();
            return;
        }

        if (SAVEEXIT.equals(actionEvent.getActionCommand()) == true) {
            // load the assembled image
            ImagePlus assembledOriginal = new ImagePlus(directory.getAbsolutePath()
                    + File.separator + IJImgProcessing.ASSEMBLED_JPEG);
            assembledOriginal.trimProcessor();
            if (assembledOriginal == null) {
                JOptionPane.showMessageDialog(this,
                        "Error, unable to properly read assembled image!\nNothing was saved.\nLocation: "
                        + directory.getAbsolutePath(), "Error with: "
                        + IJImgProcessing.ASSEMBLED_JPEG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            save(wormDisplayList, getInspectedStatus(wormDisplayList, viewedPlateFlag,
                    pageVisitedMap), directory, this, assembledOriginal);
            closeIt();
            return;
        }

        if (VIEW_PLATE.equals(actionEvent.getActionCommand()) == true) {
            viewedPlateFlag = true;
            viewPlate();
            return;
        }

        if (LOAD.equals(actionEvent.getActionCommand()) == true) {
            boolean discardChanges = verifyUserWantsToContinue();
            if (discardChanges == false) {
                return;
            }
            doLoad();
            return;
        }

        if (BROWSE.equals(actionEvent.getActionCommand()) == true) {
            boolean discardChanges = verifyUserWantsToContinue();
            if (discardChanges == false) {
                return;
            }
            int state = fileChooser.showOpenDialog(this);
            if (state == JFileChooser.APPROVE_OPTION) {
                File folder = fileChooser.getSelectedFile();
                folderTextField.setText(folder.getAbsolutePath());
                doLoad();
            }
            return;
        }

        // check each of available buttons
        for (int buttonIndex = 0; buttonIndex < IMG_BUTTON_TOTAL; buttonIndex++) {
            if ((IMGBUTTON + buttonIndex).equals(actionEvent.getActionCommand()) == true) {
                int i = currentPage * IMG_BUTTON_TOTAL + buttonIndex;
                WormInfo worm = wormDisplayList.get(i);

                // get the status at the time of click
                String status = worm.getViewingStatus();
                if (status == null) {
                    status = worm.getOriginalStatus();
                }

                String changedStatus = null;

                // change the status due to clicking on it
                if (status.equals(WormInfo.NOTHING) == true) {
                    changedStatus = WormInfo.DELETED;
                } else if (status.equals(WormInfo.SUSPICIOUS) == true) {
                    changedStatus = WormInfo.DELETED;
                } else if (status.equals(WormInfo.HERMAPHRODITE) == true) {
                    changedStatus = WormInfo.DELETED;
                } else if (status.startsWith(WormInfo.MULTIPLE) == true) {
                    changedStatus = WormInfo.DELETED;
                } else if (status.equals(WormInfo.MALE) == true) {
                    changedStatus = WormInfo.HERMAPHRODITE;
                } else if (status.equals(WormInfo.DELETED) == true) {
                    changedStatus = WormInfo.MALE;
                }
                worm.setViewingStatus(changedStatus);
                imgButton[ buttonIndex].setText(worm.getLabel());
                updateSpinners(buttonIndex, worm.getMalesForSpinner(), worm.getHermaphroditesForSpinner());
                updateLabel();
                return;
            }
        }

        // check each of available menu-item
        for (String eachMenuOption : ALL_POPUP_MENUS) {
            if (eachMenuOption == null) {
                continue;
            }
            if (eachMenuOption.equals(actionEvent.getActionCommand()) == true) {
                JMenuItem tmpMenuItem = (JMenuItem) actionEvent.getSource();
                JPopupMenu tmpPopupMenu = (JPopupMenu) tmpMenuItem.getParent();
                JButton clickedButton = (JButton) tmpPopupMenu.getInvoker();
                clickedButton.requestFocusInWindow();
                String buttonActionCommand = clickedButton.getActionCommand();
                if (buttonActionCommand.startsWith(IMGBUTTON) == false) {
                    out.println("Unable to figure out the button: " + buttonActionCommand);
                    continue;
                }
                int buttonIndex = Integer.parseInt(buttonActionCommand.substring(IMGBUTTON.length()));
                int index = currentPage * IMG_BUTTON_TOTAL + buttonIndex;
                if (index >= wormDisplayList.size()) {
                    JOptionPane.showMessageDialog(this, "No worm!\nNothing can be done without a worm image.",
                            "No worm!", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                WormInfo worm = wormDisplayList.get(index);

                // see which popup option was selected by user
                if (VIEW_IMAGE_POPUP.equals(actionEvent.getActionCommand()) == true
                        || SAVE_IMAGE_POPUP.equals(actionEvent.getActionCommand()) == true) {
                    String filename = "worm_" + worm.pX + "_" + worm.pY;
                    ImagePlus previewImagePlus = NewImage.createByteImage(filename, worm.width
                            + 40, worm.height + 40, 1, NewImage.GRAY8);
                    for (int i = 0; i < worm.width + 40; i++) {
                        for (int j = 0; j < worm.height + 40; j++) {
                            previewImagePlus.getProcessor().putPixel(i, j,
                                    assembledEnhanced.getPixel(worm.pX + i - 20, worm.pY + j - 20));
                        }
                    }
                    previewImagePlus.trimProcessor();
                    if (VIEW_IMAGE_POPUP.equals(actionEvent.getActionCommand()) == true) {
                        previewImagePlus.show();
                    }
                    if (SAVE_IMAGE_POPUP.equals(actionEvent.getActionCommand()) == true) {
                        FileSaver fileSaver = new FileSaver(previewImagePlus);
                        filename = filename + ".jpeg";
                        fileSaver.saveAsJpeg(directory + File.separator + filename);
                        JOptionPane.showMessageDialog(this, "Saved !\n\nFolder: "
                                + directory + "\nFilename: " + filename, "Saved file.",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                    stuffToCloseList.add(previewImagePlus);
                    return;
                }
                if (RESET_POPUP.equals(actionEvent.getActionCommand()) == true) {
                    worm.setViewingStatus(worm.getOriginalStatus());
                    imgButton[ buttonIndex].setText(worm.getLabel());
                    updateSpinners(buttonIndex, worm.getMalesForSpinner(), worm.getHermaphroditesForSpinner());
                    updateLabel();
                    return;
                }
                if (ONE_AND_ONE_POPUP.equals(actionEvent.getActionCommand()) == true) {
                    worm.setViewingStatus(WormInfo.MULTIPLE + "1,1]");
                    imgButton[ buttonIndex].setText(worm.getLabel());
                    updateSpinners(buttonIndex, worm.getMalesForSpinner(), worm.getHermaphroditesForSpinner());
                    updateLabel();
                    return;
                }
                if (TWO_HERMA_POPUP.equals(actionEvent.getActionCommand()) == true) {
                    worm.setViewingStatus(WormInfo.MULTIPLE + "0,2]");
                    imgButton[ buttonIndex].setText(worm.getLabel());
                    updateSpinners(buttonIndex, worm.getMalesForSpinner(), worm.getHermaphroditesForSpinner());
                    updateLabel();
                    return;
                }
                if (THREE_HERMA_POPUP.equals(actionEvent.getActionCommand()) == true) {
                    worm.setViewingStatus(WormInfo.MULTIPLE + "0,3]");
                    imgButton[ buttonIndex].setText(worm.getLabel());
                    updateSpinners(buttonIndex, worm.getMalesForSpinner(), worm.getHermaphroditesForSpinner());
                    updateLabel();
                    return;
                }
                if (TWO_MALES_POPUP.equals(actionEvent.getActionCommand()) == true) {
                    worm.setViewingStatus(WormInfo.MULTIPLE + "2,0]");
                    imgButton[ buttonIndex].setText(worm.getLabel());
                    updateSpinners(buttonIndex, worm.getMalesForSpinner(), worm.getHermaphroditesForSpinner());
                    updateLabel();
                    return;
                }
                if (THREE_MALES_POPUP.equals(actionEvent.getActionCommand()) == true) {
                    worm.setViewingStatus(WormInfo.MULTIPLE + "3,0]");
                    imgButton[ buttonIndex].setText(worm.getLabel());
                    updateSpinners(buttonIndex, worm.getMalesForSpinner(), worm.getHermaphroditesForSpinner());
                    updateLabel();
                    return;
                }
                if (SUSPICIOUS_POPUP.equals(actionEvent.getActionCommand()) == true) {
                    worm.setViewingStatus(WormInfo.SUSPICIOUS);
                    imgButton[ buttonIndex].setText(worm.getLabel());
                    updateSpinners(buttonIndex, worm.getMalesForSpinner(), worm.getHermaphroditesForSpinner());
                    updateLabel();
                    return;
                }
            }
        }
        // detect any programming errors by showing them on front of the user:
        JOptionPane.showMessageDialog(this, "Programming error!\nPending to write code for:\n"
                + actionEvent.getActionCommand(), "Error of the programmer.", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * displays the plate with colorful annotations on worms
     *
     */
    protected void viewPlate() {
        final String TITLE = "View: " + directory.getAbsolutePath();

        // crate the image
        ImageProcessor imageProcessor = assembledEnhanced.getProcessor();
        TypeConverter typeConverter = new TypeConverter(imageProcessor, false);
        ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
        ImagePlus imagePlus = new ImagePlus("color", colorProcessor);
        imagePlus.trimProcessor();

        // create the plate-view object
        boolean closeItFlag = false;
        PlateView plateView = new PlateView(imagePlus, wormDisplayList,
                getPreferredSize(), (JFrame) getParent(), TITLE, directory);
        if (plateView.getErrors() != null) {
            JOptionPane.showMessageDialog(this, "Plate-view error!\n"
                    + plateView.getErrors(), "Error in Plate-View!", JOptionPane.ERROR_MESSAGE);
        } else {
            plateView.show();
            if (PlateView.SAVE_AND_CLOSE.equals(plateView.getErrors()) == true) {
                // saving is done at view plate, but we also close
                closeItFlag = true;
            }
            // verify whether to bring page-view back to normal
            if (PlateView.PAGE_VIEW.equals(plateView.getErrors()) == true) {
                updateImageIconsCache();
                prevPageButton.setEnabled(true);
                currentPage = 0;
                prevPageButton.doClick();
            }
        }
        imagePlus.close();
        imagePlus.flush();
        plateView.clearStuff();
        plateView = null;
        if (closeItFlag == true) {
            closeIt();
        }
    }

    /**
     * Updates the spinners by removing change listeners first
     *
     * @param buttonIndex
     * @param males the number of males
     * @param hermaphrodites the number of hermaphrodites
     */
    protected void updateSpinners(int buttonIndex, int males, int hermaphrodites) {
        maleSpinner[ buttonIndex].removeChangeListener(this);
        hermSpinner[ buttonIndex].removeChangeListener(this);
        maleSpinner[ buttonIndex].setValue(males);
        hermSpinner[ buttonIndex].setValue(hermaphrodites);
        maleSpinner[ buttonIndex].addChangeListener(this);
        hermSpinner[ buttonIndex].addChangeListener(this);
    }

    /**
     * event of (right) mouse-clicked on the each-button (any of them)
     *
     * @param mosueEvent the mouse-event
     */
    private void mouseClickedOnEachButton(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }
    }

    /**
     * Verifies whether user wants to discard changes, or stay in current place
     *
     * @return true when user wants to discard changes (if any), or there is
     * nothing changed
     */
    public boolean verifyUserWantsToContinue() {
        boolean changes = false;
        for (WormInfo worm : wormDisplayList) {
            if (worm.getViewingStatus() != null
                    && worm.getOriginalStatus().equals(worm.getViewingStatus()) == false) {
                changes = true;
                break;
            }
        }
        if (changes == true) {
            String[] options = {"Yes, discard changes", "No, go back to where I was"};
            int n = JOptionPane.showOptionDialog(this,
                    "Changes have not been saved!\nDiscard Changes?",
                    "Unsaved changes", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[ 1]);
            if (n == 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Closes the dialog window after verifying changes being saved
     */
    protected void closeIt() {
        boolean discardChanges = verifyUserWantsToContinue();
        if (discardChanges == false) {
            return;
        }
        setVisible(false);
        // clear up stuff
        for (ImagePlus each : stuffToCloseList) {
            each.close();
            each.flush();
        }
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
            }
        }
        if (NEXT_PAGE.equals(change) == true) {
            currentPage++;
        }
        int ceiling = (int) Math.ceil(wormDisplayList.size() * 1.0 / IMG_BUTTON_TOTAL);
        if (((currentPage + 1) * IMG_BUTTON_TOTAL) > wormDisplayList.size()) {
            currentPage = ceiling - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }
        prevPageButton.setEnabled(currentPage != 0);
        nextPageButton.setEnabled((currentPage + 1) < ceiling);
        pageVisitedMap.put(currentPage, true);
        updateLabel();
        // fill up images and values for the buttons
        int i = currentPage * IMG_BUTTON_TOTAL;
        for (int buttonIndex = 0; buttonIndex < IMG_BUTTON_TOTAL; buttonIndex++) {
            if (i < wormDisplayList.size()) {
                WormInfo worm = wormDisplayList.get(i);
                imgButton[ buttonIndex].setEnabled(true);
                imgButton[ buttonIndex].setPreferredSize(new Dimension(worm.width
                        + ICON_PADDING_X * 2, worm.height + ICON_PADDING_Y * 2 + 4));
                imgButton[ buttonIndex].setMinimumSize(new Dimension(worm.width
                        + ICON_PADDING_X * 2, worm.height + ICON_PADDING_Y * 2 + 4));
                imgButton[ buttonIndex].setIcon(cachedImageIcons[ i]);
                imgButton[ buttonIndex].setText(worm.getLabel());
                updateSpinners(buttonIndex, worm.getMalesForSpinner(), worm.getHermaphroditesForSpinner());
                maleSpinner[ buttonIndex].setEnabled(true);
                hermSpinner[ buttonIndex].setEnabled(true);
            } else {
                imgButton[ buttonIndex].setText("----");
                imgButton[ buttonIndex].setIcon(null);
                imgButton[ buttonIndex].setPreferredSize(DUMMY_DIMENSION);
                imgButton[ buttonIndex].setMinimumSize(DUMMY_DIMENSION);
                imgButton[ buttonIndex].setEnabled(false);
                updateSpinners(buttonIndex, 0, 0);
                maleSpinner[ buttonIndex].setEnabled(false);
                hermSpinner[ buttonIndex].setEnabled(false);
            }
            imgButton[ buttonIndex].setMargin(DUMMY_INSETS);
            imgButton[ buttonIndex].invalidate();
            maleSpinner[ buttonIndex].invalidate();
            hermSpinner[ buttonIndex].invalidate();
            i++;
        }
        validate();
        repaint();
    }

    /**
     * Updates the status label (using wormDisplayList)
     */
    private void updateLabel() {
        int ceiling = (int) Math.ceil(wormDisplayList.size() * 1.0 / IMG_BUTTON_TOTAL);
        String paging = "<b>page " + (currentPage + 1) + " of " + ceiling + "</b>";
        int deletedCount = 0;
        int suspiciousCount = 0;
        int malesCount = 0;
        int hermaCount = 0;
        boolean anyChangesFlag = false;

        for (WormInfo worm : wormDisplayList) {
            // there are two cases, status is same or status is different to viewing-status
            if (worm.getViewingStatus() == null) {
                if (worm.isSuspicious() == true) {
                    suspiciousCount++;
                    continue;
                }
                malesCount += worm.nMale;
                hermaCount += worm.nHerma;
            } else {
                if (worm.getViewingStatus().equals(worm.getOriginalStatus()) == false) {
                    anyChangesFlag = true;
                }
                // count according to viewing-status
                if (WormInfo.DELETED.equals(worm.getViewingStatus()) == true) {
                    deletedCount++;
                    continue;
                }
                if (WormInfo.NOTHING.equals(worm.getViewingStatus()) == true) {
                    continue;
                }
                // the verification may be redundant/not-needed, but just in case:
                if (WormInfo.SUSPICIOUS.equals(worm.getViewingStatus()) == true) {
                    suspiciousCount++;
                    continue;
                }
                if (WormInfo.MALE.equals(worm.getViewingStatus()) == true) {
                    malesCount++;
                    continue;
                }
                if (WormInfo.HERMAPHRODITE.equals(worm.getViewingStatus()) == true) {
                    hermaCount++;
                    continue;
                }
                if (worm.getViewingStatus().startsWith(WormInfo.MULTIPLE) == true) {
                    malesCount += worm.getViewingMales();
                    hermaCount += worm.getViewingHermaphrodites();
                }
            }
        }

        // count the non-changed items
        int originalMalesCount = 0;
        int originalHermaCount = 0;
        int originalSuspiciousCount = 0;
        for (WormInfo worm : wormDisplayList) {
            if (worm.isSuspicious() == true) {
                originalSuspiciousCount++;
                continue;
            }
            originalMalesCount += worm.nMale;
            originalHermaCount += worm.nHerma;
        }

        String deletedText = "<br>" + wormDisplayList.size() + " images.";
        if (deletedCount > 0) {
            deletedText = "<br>" + deletedCount + " deleted images out of " + wormDisplayList.size();
        }

        String countsText = originalMalesCount + " males, " + originalHermaCount + " hermaphrodites";
        if (originalSuspiciousCount > 0) {
            countsText += ", " + originalSuspiciousCount + " unknown";
        }
        countsText += ".";
        if (anyChangesFlag == true) {
            countsText = "Initially: " + countsText;
            countsText += " <b>Now</b>: " + malesCount + " males, " + hermaCount + " hermaphrodites";
            if (suspiciousCount > 0) {
                countsText += ", " + suspiciousCount + " unknown";
            }
            countsText += ".";
        }
        statusLabel.setText("<html>" + paging + deletedText + "<br>" + countsText + "</html");
        String path = "";
        String inspectedStatus = getInspectedStatus(wormDisplayList, viewedPlateFlag, pageVisitedMap);

        Color color;
        if (ResultsGender.INSPECTED.equals(inspectedStatus) == true) {
            color = Color.GREEN;
        } else {
            color = Color.RED;
        }
        if (directory != null) {
            path = directory.getAbsolutePath();
        } else {
            color = Color.WHITE;
        }
        folderTextField.setBackground(color);
        setTitle(path + "  " + inspectedStatus);
    }

    /**
     * Returns the status of inspection, either inspected or not inspected
     *
     * @param list the list of worm objects
     * @param viewedPlateFlag true indicates that plate view has been selected
     * by user
     * @param pageVisitedMap the map of pages and whether they have been visited
     * by user
     * @return the status of inspection
     */
    public static String getInspectedStatus(List<WormInfo> list,
            boolean viewedPlateFlag, Map<Integer, Boolean> pageVisitedMap) {
        if (list.isEmpty()) {
            if (viewedPlateFlag == true) {
                return ResultsGender.INSPECTED;
            }
            return ResultsGender.NOT_INSPECTED;
        }

        // easy case: any suspicious item is enough to consider not-inspected
        for (WormInfo worm : list) {
            // there are two cases, status is same or status is different to viewing-status
            if (worm.getViewingStatus() == null
                    || worm.getOriginalStatus().equals(worm.getViewingStatus()) == true) {
                if (worm.isSuspicious() == true) {
                    return ResultsGender.NOT_INSPECTED;
                }
            } else {
                if (WormInfo.NOTHING.equals(worm.getViewingStatus()) == true) {
                    return ResultsGender.NOT_INSPECTED;
                }
                // the verification may be redundant/not-needed, but just in case:
                if (WormInfo.SUSPICIOUS.equals(worm.getViewingStatus()) == true) {
                    return ResultsGender.NOT_INSPECTED;
                }
            }
        }

        // easy case, only 1 page
        int ceiling = (int) Math.ceil(list.size() * 1.0 / IMG_BUTTON_TOTAL);
        if (ceiling == 1) {
            return ResultsGender.INSPECTED;
        }
        // see whether any page has not been visited (when supplied)
        if (pageVisitedMap != null) {
            for (Integer page : pageVisitedMap.keySet()) {
                Boolean visited = pageVisitedMap.get(page);
                if (visited == false) {
                    return ResultsGender.NOT_INSPECTED;
                }
            }
        }
        return ResultsGender.INSPECTED;
    }

    /**
     * Manual Inspection of images
     *
     * @param folder the directory containing ResultsGender.RESULTS_TXT and
     * plateImg.jpg
     */
    public void go(File folder) {
        folderTextField.setText(folder.getAbsolutePath());
        loadButton.doClick();
    }

    /**
     * Responds to 'load' button
     */
    public void doLoad() {
        wormDisplayList.clear();
        directory = null;
        File folder = new File(folderTextField.getText());
        if (folder == null) {
            JOptionPane.showMessageDialog(this, "Directory does not exist ( null )",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } else if (folder.exists() == false) {
            JOptionPane.showMessageDialog(this, "Directory does not exist ( "
                    + folder.getAbsolutePath() + " )", "Error", JOptionPane.ERROR_MESSAGE);
            folder = null;
        } else if (folder.isDirectory() == false) {
            JOptionPane.showMessageDialog(this, "Selection is not a directory !\n"
                    + "A directory is expected.\n\nLocation: " + folder.getAbsolutePath() + "",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            // see whether the folder is empty
            File[] tmpContents = folder.listFiles();
            if (tmpContents.length == 0) {
                JOptionPane.showMessageDialog(this, "Directory is empty !\n"
                        + folder.getAbsolutePath() + "", "Error, empty folder.", JOptionPane.ERROR_MESSAGE);
                folder = null;
            } else {
                File resultsFile = new File(folder, ResultsGender.RESULTS_TXT);
                if (resultsFile.exists() == false) {
                    // there's not results file, then see if there are subfolders
                    List<File> subFolders = new ArrayList<File>();
                    for (File each : tmpContents) {
                        if (each.isDirectory() == true) {
                            File tmpFile = new File(each, ResultsGender.RESULTS_TXT);
                            if (tmpFile.exists() == true) {
                                subFolders.add(each);
                            }
                        }
                    }
                    if (subFolders.isEmpty() == false) {
                        String[] options = new String[subFolders.size() + 1];
                        for (int i = 0; i < subFolders.size(); i++) {
                            options[ i] = "Folder: " + subFolders.get(i).getName();
                        }
                        options[ options.length - 1] = "Cancel";
                        int n = JOptionPane.showOptionDialog(this,
                                "Folder does not have images to be analyzed.\n\nHowever,"
                                + " it contains sub-folders with images.\n\n"
                                + "Please select a sub-folder using the buttons below:",
                                "Folder selected does not have "
                                + ResultsGender.RESULTS_TXT, JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, options, options.length - 1);
                        if (n < 0 || n == options.length - 1) {
                            folder = null;
                        } else {
                            folder = subFolders.get(n);
                            folderTextField.setText(folder.getAbsolutePath());
                        }
                    }
                }
            }
        }

        List<WormInfo> readWormsList = new ArrayList<WormInfo>();
        // results-file must exist
        if (folder != null) {
            File resultsFile = new File(folder, ResultsGender.RESULTS_TXT);
            if (resultsFile.exists() == false) {
                JOptionPane.showMessageDialog(this, "Results file ("
                        + ResultsGender.RESULTS_TXT + ") does not exist in directory "
                        + folder.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            directory = folder;

            // read the results-gender plain-text file
            String errors = resultsGender.readWormsFromFile(resultsFile);
            if (errors != null) {
                JOptionPane.showMessageDialog(this, "Errors when reading "
                        + ResultsGender.RESULTS_TXT + "\n\n" + errors, "Errors with "
                        + ResultsGender.RESULTS_TXT, JOptionPane.ERROR_MESSAGE);
                return;
            }
            readWormsList = resultsGender.getWormsList();
            if (readWormsList == null) {
                JOptionPane.showMessageDialog(this, "Unable to load any data from "
                        + ResultsGender.RESULTS_TXT, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // verify that assembled image exists
            File file = new File(directory, IJImgProcessing.ASSEMBLED_JPEG);
            if (file.exists() == false) {
                JOptionPane.showMessageDialog(this, "Error, unable to find assembled image!\nExpected location: "
                        + file.getAbsolutePath(), "Error, missing " + IJImgProcessing.ASSEMBLED_JPEG,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // verify that assembled-enhanced image exists
            file = new File(directory, IJImgProcessing.ASSEMBLED_ENHANCED_JPEG);
            if (file.exists() == false) {
                // use assembled then
                file = new File(directory, IJImgProcessing.ASSEMBLED_JPEG);
            }

            // load the assembled-enhanced image
            assembledEnhanced = new ImagePlus(file.getAbsolutePath());
            assembledEnhanced.trimProcessor();
            if (assembledEnhanced == null) {
                JOptionPane.showMessageDialog(this, "Error, unable to properly read assembled image!\nLocation: "
                        + file.getAbsolutePath(), "Error with: " + file, JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (readWormsList.isEmpty() == true) {
                JOptionPane.showMessageDialog(this, "No worms!\nRecommendation: use the button: '"
                        + VIEW_PLATE + "'", "No worms!", JOptionPane.WARNING_MESSAGE);
            }
        }

        // place the worms into a set for sorting purposes
        Set<WormInfo> wormInfoSet = new TreeSet<WormInfo>();
        for (WormInfo each : readWormsList) {
            wormInfoSet.add(each);
        }

        // just in case verification
        if (wormInfoSet.size() != readWormsList.size()) {
            JOptionPane.showMessageDialog(this, "Error\nreadWormsList and wormInfoSet have different sizes!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // get the ordered elements into the display-list
        for (WormInfo each : wormInfoSet) {
            wormDisplayList.add(each);
        }

        // just in case verification
        if (wormDisplayList.size() != readWormsList.size()) {
            JOptionPane.showMessageDialog(this,
                    "Error\nreadWormsList and wormDisplayList have different sizes!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // find out max height and width
        int maxWidth = 10;
        int maxHeight = 10;
        for (WormInfo worm : wormDisplayList) {
            if (worm.width > maxWidth) {
                maxWidth = worm.width;
            }
            if (worm.height > maxHeight) {
                maxHeight = worm.height;
            }
        }

        // adjust all button's sizes
        Dimension dimension = new Dimension(maxWidth + 6, maxHeight + 6);
        for (int i = 0; i < IMG_BUTTON_TOTAL; i++) {
            imgButton[ i].setMinimumSize(dimension);
            imgButton[ i].setPreferredSize(dimension);
            imgButton[ i].invalidate();
        }

        updateImageIconsCache();

        currentPage = 0;

        // reset the page-visited map
        pageVisitedMap.clear();
        int ceiling = (int) Math.ceil(wormDisplayList.size() * 1.0 / IMG_BUTTON_TOTAL);
        for (int i = 0; i < ceiling; i++) {
            pageVisitedMap.put(i, false);
        }
        viewedPlateFlag = false;
        prevPageButton.setEnabled(true); // has to be enabled so that doClick can work
        prevPageButton.doClick();

        invalidate();
        setSize(getPreferredSize());
        ((JFrame) getParent()).pack();
        setVisible(true);
    }

    /**
     * Updates (or creates) the icons, which are cached for the purpose of
     * creating them only once
     */
    protected void updateImageIconsCache() {
        cachedImageIcons = new ImageIcon[wormDisplayList.size()];

        for (int index = 0; index < cachedImageIcons.length; index++) {
            WormInfo worm = wormDisplayList.get(index);

            ColorProcessor outLineOverlayedIconImageProc = GenderAnalyzer.get_RedOutlineOverlayedIconImageProc(
                    assembledEnhanced.getProcessor(), directory,
                    worm.maskImageIDNumb, worm.pX, worm.pY, worm.width, worm.height);

            cachedImageIcons[ index] = new ImageIcon(outLineOverlayedIconImageProc.getBufferedImage());
        }
    }

    /**
     * Saves the list of display worms, Frst, the existing
     * ResultsGender.RESULTS_TXT file is renamed to a historical name, Second,
     * the results file is saved, third, the assembled-colors image is created,
     * Last, the list of worms is 'fused'
     *
     * @param list
     * @param inspectedText
     * @param location
     * @param component
     * @param assembled
     */
    public static void save(List<WormInfo> list, String inspectedText,
            File location, Component component, ImagePlus assembled) {
        // see if there is historical file, and if so, find the next available number
        File historicalFile = null;
        int number = 0;
        do {
            number++;
            String filename = location + File.separator + HISTORICAL
                    + number + "." + ResultsGender.RESULTS_TXT;
            historicalFile = new File(filename);
        } while (historicalFile.exists() == true);
        File oldResultsFile = new File(location + File.separator + ResultsGender.RESULTS_TXT);
        boolean renamedFlag = oldResultsFile.renameTo(historicalFile);
        if (renamedFlag == false) {
            JOptionPane.showMessageDialog(component, "Error, unable to rename file "
                    + ResultsGender.RESULTS_TXT + " to a historical filename.",
                    "Cannot save!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // update the worms list
        fuseWormList(list);

        String errors = GenderAnalyzer.saveResultsToFile(list, location, inspectedText);
        if (errors != null) {
            JOptionPane.showMessageDialog(component,
                    "Error, unable to save results file: "
                    + ResultsGender.RESULTS_TXT + " .\n\n" + errors,
                    "Cannot save!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // now save the assembled colors image
        GenderAnalyzer.colorworms(assembled, location, false, list);
    }

    /**
     * Updates worms-list (in-place) fusing whatever viewing-status is
     * available; it should be done only after saving results to file
     *
     * @param list the list of worm objects
     */
    public static void fuseWormList(List<WormInfo> list) {
        Set<WormInfo> deleteSet = new TreeSet<WormInfo>();
        for (WormInfo worm : list) {
            if (worm.getOriginalStatus().equals(worm.getViewingStatus()) == true) {
                worm.setViewingStatus(null);
            } else {
                if (WormInfo.NOTHING.equals(worm.getViewingStatus()) == true) {
                    worm.setSuspicious(true);
                    worm.setViewingStatus(null);
                }
                if (WormInfo.DELETED.equals(worm.getViewingStatus()) == true) {
                    worm.nMale = 0;
                    worm.nHerma = 0;
                    worm.setSuspicious(false);
                    worm.setViewingStatus(null);
                    deleteSet.add(worm);
                }
                if (WormInfo.MALE.equals(worm.getViewingStatus()) == true) {
                    worm.nMale = 1;
                    worm.nHerma = 0;
                    worm.setSuspicious(false);
                    worm.setViewingStatus(null);
                } else if (WormInfo.HERMAPHRODITE.equals(worm.getViewingStatus()) == true) {
                    worm.nHerma = 1;
                    worm.nMale = 0;
                    worm.setSuspicious(false);
                    worm.setViewingStatus(null);
                } else if (worm.getViewingStatus() != null
                        && worm.getViewingStatus().startsWith(WormInfo.MULTIPLE) == true) {
                    worm.nMale = worm.getViewingMales();
                    worm.nHerma = worm.getViewingHermaphrodites();
                    worm.setViewingStatus(null);
                    worm.setSuspicious(false);
                } else if (WormInfo.SUSPICIOUS.equals(worm.getViewingStatus()) == true) {
                    worm.nMale = worm.getViewingMales();
                    worm.nHerma = worm.getViewingHermaphrodites();
                    worm.setViewingStatus(null);
                    worm.setSuspicious(true);
                } else {
                    if (worm.getViewingStatus() != null) {
                        JOptionPane.showMessageDialog(null, "Programming Error\nWhat to do with this status:\n"
                                + worm.getViewingStatus() + "\nobject: " + worm, "Programmer error!",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

        // delete anything?
        for (WormInfo worm : deleteSet) {
            list.remove(worm);
        }

        // place the worms into a set for sorting purposes
        Set<WormInfo> wormInfoSet = new TreeSet<WormInfo>();
        for (WormInfo each : list) {
            wormInfoSet.add(each);
        }

        // just in case verification
        if (wormInfoSet.size() != list.size()) {
            JOptionPane.showMessageDialog(null, "Error\n'list' and wormInfoSet have different sizes!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        list.clear();

        // get the ordered elements back into the list
        for (WormInfo each : wormInfoSet) {
            list.add(each);
        }

        // just in case verification
        if (list.size() != wormInfoSet.size()) {
            JOptionPane.showMessageDialog(null, "Error\nOrdered 'list' and wormInfoSet have different sizes!",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Changes of spinners show up here
     *
     * @param changeEvent
     */
    @Override
    public void stateChanged(ChangeEvent changeEvent) {
        if (wormDisplayList == null) {
            return;
        }
        Object source = changeEvent.getSource();
        for (int buttonIndex = 0; buttonIndex < IMG_BUTTON_TOTAL; buttonIndex++) {
            if (maleSpinner[ buttonIndex] == source
                    || hermSpinner[ buttonIndex] == source) {
                int wormIndex = buttonIndex + currentPage * IMG_BUTTON_TOTAL;
                if (wormIndex >= wormDisplayList.size()) {
                    return;
                }
                WormInfo worm = wormDisplayList.get(wormIndex);
                if (worm == null) {
                    JOptionPane.showMessageDialog(this, "Internal error, worm at index " + wormIndex
                            + " is null!", "Internal (stateChanged)", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Integer maleNumber = new Integer(maleSpinner[ buttonIndex].getValue().toString());
                Integer hermNumber = new Integer(hermSpinner[ buttonIndex].getValue().toString());
                // find errors first
                if (maleNumber != null && hermNumber == null) {
                    JOptionPane.showMessageDialog(this, "Internal error, worm at index " + wormIndex
                            + ", maleNumber: " + maleNumber + " but hermNumber is null !", "Internal (stateChanged)", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (maleNumber == null && hermNumber != null) {
                    JOptionPane.showMessageDialog(this, "Internal error, worm at index " + wormIndex
                            + ", hermNumber: " + hermNumber + " but maleNumber is null !", "Internal (stateChanged)", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (maleNumber == null && hermNumber == null) {
                    JOptionPane.showMessageDialog(this, "Internal error, worm at index " + wormIndex
                            + ", hermNumber and maleNumber are null !", "Internal (stateChanged)", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (maleNumber == 0 && hermNumber == 0) {
                    worm.setViewingStatus(WormInfo.DELETED);
                } else if (maleNumber == 0 && hermNumber == 1) {
                    worm.setViewingStatus(WormInfo.HERMAPHRODITE);
                } else if (maleNumber == 1 && hermNumber == 0) {
                    worm.setViewingStatus(WormInfo.MALE);
                } else {
                    worm.setViewingStatus(WormInfo.MULTIPLE + maleNumber + "," + hermNumber + "]");
                }
                imgButton[ buttonIndex].setText(worm.getLabel());
                updateLabel();
            }
        }
    }

    /**
     * inner-class for Action of next-page
     */
    public class NextPageAction extends AbstractAction {

        /**
         * constructor
         *
         * @param text
         * @param mnemonic
         */
        public NextPageAction(String text, Integer mnemonic) {
            super(text);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        @Override
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
         *
         * @param text
         * @param mnemonic
         */
        public PrevPageAction(String text, Integer mnemonic) {
            super(text);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pageChange(PREV_PAGE);
        }
    }

}
