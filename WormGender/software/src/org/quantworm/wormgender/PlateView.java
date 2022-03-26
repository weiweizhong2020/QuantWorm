/* 
 * Filename: PlateView.java
 */
package org.quantworm.wormgender;

import org.apache.commons.math.stat.StatUtils;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Roi;
import ij.process.ColorProcessor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

/**
 * Handles plate-view actions
 *
 * @author Boanerges Aleman-Meza
 */
public final class PlateView implements MouseListener, WindowListener {

    /**
     * size of square window (default)
     */
    public static final int SQUARE_SIZE = 600;

    /**
     * pixels width of stroke
     */
    public static final int STROKE_WIDTH = 8;

    /**
     * extra pixels for bounding box
     */
    public static final int EXTRA_PIXELS = 6;

    /**
     * number of zoom levels
     */
    public static final int ZOOM_LEVELS = 2;

    /**
     * constant for save-and-close
     */
    public static final String SAVE_AND_CLOSE = "Save & Close (Alt+C)";

    /**
     * constant for page-view
     */
    public static final String PAGE_VIEW = "Page View (Alt+V)";

    // constant used in popup-menu option 1 herma
    protected static final String ONE_HERMA = "1 hermaphrodite";

    // constant used in popup-menu option 2 hermas
    protected static final String TWO_HERMA_POPUP = "2 hermaphrodites";

    // constant used in popup-menu option 3 herma
    protected static final String THREE_HERMA_POPUP = "3 hermaphrodites";

    // constant used in popup-menu option 1 male
    protected static final String ONE_MALE = "1 male";

    // constant used in popup-menu option 2 males
    protected static final String TWO_MALES_POPUP = "2 males";

    // constant used in popup-menu option 3 males
    protected static final String THREE_MALES_POPUP = "3 males";

    // constant used in popup-menu option suspicious
    protected static final String SUSPICIOUS_POPUP = "Unknown";

    // constant used in popup-menu option other
    protected static final String OTHER_POPUP = "Other";

    // all the constants in pop-menu options
    protected static final String[] ALL_POPUP_MENUS = new String[]{ONE_MALE, ONE_HERMA, null,
        TWO_HERMA_POPUP, TWO_MALES_POPUP, null, THREE_MALES_POPUP, THREE_HERMA_POPUP, null, OTHER_POPUP};

    /**
     * folder where results are to be written
     */
    public final File directory;

    /**
     * the list of worms from page-view
     */
    public final List<WormInfo> wormsList;

    /**
     * scroll pane
     */
    private final JScrollPane pictureScrollPane;

    // the main component is the picture holder (a JLabel)
    private ScrollablePicture scrollablePicture;

    // colors image
    private ImageIcon[] colorsImageIcon = new ImageIcon[ZOOM_LEVELS];

    // non-annotated image-plus (already a color-image)
    private final ImagePlus colorsImagePlus;

    private final Dimension SQUARE_DIMENSION = new Dimension(SQUARE_SIZE, SQUARE_SIZE);
    // any errors, or null when no errors detected,
    // also utilized to signal specific messages to the dialog-window that called plate-view
    private String errors = null;

    // the dialog window
    private JDialog dialog;

    // the popup menu
    private JPopupMenu popupMenu = null;

    // remember the x coordinate where popup shows
    private int xPopup = 0;

    // remember the x coordinate where popup shows
    private int yPopup = 0;

    // remember current zoom
    private int zoom;

    public static final int MALE_COLOR = Color.BLUE.getRGB();
    public static final int HERMA_COLOR = Color.RED.getRGB();
    public static final int SUSPICIOUS_COLOR = Color.GREEN.getRGB();
    public static final int NOTHING_COLOR = Color.DARK_GRAY.getRGB();

    /**
     * Constructor
     *
     * @param imagePlus the 'colors' annotated image
     * @param wormsList the list of worm objects
     * @param dimension the size of this component
     * @param parentFrame the parent frame
     * @param title the title for the dialog-window
     * @param resultsFolder folder where results will be eventually written
     */
    public PlateView(ImagePlus imagePlus, List<WormInfo> wormsList, Dimension dimension, JFrame parentFrame, String title, File resultsFolder) {
        this.wormsList = wormsList;
        this.directory = resultsFolder;
        this.colorsImagePlus = imagePlus;

        // zoom index zero is 100%
        ImagePlus annotatedImagePlus = updateImageAnnotations(colorsImagePlus.duplicate());
        annotatedImagePlus.trimProcessor();
        colorsImageIcon[ 0] = new ImageIcon(annotatedImagePlus.getBufferedImage());

        // make the 50% image
        ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize(annotatedImagePlus.getWidth() / 2);
        updateImageWithWormCount(scaledImageProcessor, 0.5);
        colorsImageIcon[ 1] = new ImageIcon(scaledImageProcessor.getBufferedImage());

        zoom = 1;

        // setup the scrollable-image (it is actually a JLabel)
        scrollablePicture = new ScrollablePicture(new ImageIcon(colorsImageIcon[ zoom].getImage()), this);

        // Set up the scroll pane.
        pictureScrollPane = new JScrollPane(scrollablePicture);

        // the size of the picture-scroll-pane is set based on the dimension parameter (if available)
        Dimension tmpDimension;
        if (dimension != null) {
            tmpDimension = new Dimension(dimension.width - 180, dimension.height - 170);
        } else {
            tmpDimension = SQUARE_DIMENSION;
        }

        // see whether the screen-dimension is big so that we use most of it
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenDimension = toolkit.getScreenSize();
        if ((screenDimension.getWidth() - 240) > tmpDimension.getWidth()
                && (screenDimension.getHeight() - 200) > tmpDimension.getHeight()) {
            tmpDimension.setSize(screenDimension.getWidth() - 240, screenDimension.getHeight() - 200);
        }
        pictureScrollPane.setPreferredSize(tmpDimension);
        pictureScrollPane.setViewportBorder(BorderFactory.createLineBorder(Color.black));

        // create the dialog-window
        dialog = new JDialog(parentFrame, title, true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(this);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);

        JPanel operationPanel = new JPanel(new GridBagLayout());

        // set up save button
        JButton saveButton = new JButton("Save (Alt+S)");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveInspectionResults();
            }
        });
        saveButton.setMnemonic(KeyEvent.VK_S);

        // set up save-and-close button
        JButton saveCloseButton = new JButton(SAVE_AND_CLOSE);
        saveCloseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveAndClose();
            }
        });
        saveCloseButton.setMnemonic(KeyEvent.VK_C);

        // set up page-view button
        JButton pageViewButton = new JButton("Page View (Alt+V)");
        pageViewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                errors = PAGE_VIEW;  // hack to signal that page-view should re-generate
                close();
            }
        });
        pageViewButton.setMnemonic(KeyEvent.VK_V);

        // setup the zoom-components
        JLabel zoomLabel = new JLabel("Zoom:");
        JRadioButton actualRadioButton = new JRadioButton("100%");
        actualRadioButton.setMnemonic(KeyEvent.VK_1);
        actualRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doZoom(0);
            }
        });
        JRadioButton halfRadioButton = new JRadioButton("50%");
        halfRadioButton.setMnemonic(KeyEvent.VK_5);
        halfRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doZoom(1);
            }
        });
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(actualRadioButton);
        buttonGroup.add(halfRadioButton);
        halfRadioButton.setSelected(true);

        operationPanel.add(saveButton, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(saveCloseButton, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(pageViewButton, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.CENTER));
        operationPanel.add(zoomLabel, new GBC(0, 3).setInsets(25, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.CENTER));
        operationPanel.add(actualRadioButton, new GBC(0, 4).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.CENTER));
        operationPanel.add(halfRadioButton, new GBC(0, 5).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.CENTER));

        panel.add(pictureScrollPane, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        panel.add(operationPanel, new GBC(1, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.CENTER));
        dialog.add(panel);
        pictureScrollPane.addMouseListener(this);

        // setup popup menu
        popupMenu = new JPopupMenu();
        popupMenu.setBorderPainted(true);
        JMenuItem menuItem = null;
        for (String each : ALL_POPUP_MENUS) {
            if (each == null) {
                popupMenu.addSeparator();
                continue;
            }
            menuItem = new JMenuItem(each);
            menuItem.setActionCommand(each);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    popupMenuItemActionPerformed(actionEvent);
                }
            });
            popupMenu.add(menuItem);
            menuItem.setFont(Font.decode(App.FONT_LABELS));
            popupMenu.add(menuItem);
        }

        dialog.pack();
    }

    /**
     * Shows the dialog-window
     */
    public void show() {
        dialog.setVisible(true);
    }

    /**
     * Closes the dialog-window (if possible)
     */
    public void close() {
        scrollablePicture.setText("");
        dialog.setVisible(false);
    }

    /**
     * Does zooming as specified
     *
     * @param value the zoom index value
     */
    public void doZoom(int value) {
        if (zoom == value) {
            // nothing to do
            return;
        }
        if (value < 0 || value >= ZOOM_LEVELS) {
            // incorrect value, do nothing
            return;
        }

        Point topleftPoint = pictureScrollPane.getViewport().getViewPosition();
        double xcorner = topleftPoint.getX();
        double ycorner = topleftPoint.getY();
        Dimension visibleDimension = pictureScrollPane.getViewport().getExtentSize();
        // figure out the center-point
        double xcenter = xcorner + visibleDimension.getWidth() / 2;
        double ycenter = ycorner + visibleDimension.getHeight() / 2;
        Point updatePoint = null;
        int previous = zoom;
        zoom = value;
        scrollablePicture.colorImageIcon.setImage(colorsImageIcon[ zoom].getImage());

        Dimension dimension = pictureScrollPane.getViewport().getViewSize();
        if (dimension.getWidth() > 0 && dimension.getHeight() > 0) {
            updatePoint = new Point();
            if (previous == 1 && zoom == 0) {
                // center is basically twice the size
                double x2center = xcenter * 2;
                double y2center = ycenter * 2;
                // new corner is center minus size of viewable area
                updatePoint.setLocation(x2center - visibleDimension.getWidth() / 2, y2center - visibleDimension.getHeight() / 2);
            }
            if (previous == 0 && zoom == 1) {
                // center is basically half the size
                double x2center = xcenter / 2;
                double y2center = ycenter / 2;
                // new corner is center minus size of viewable area
                updatePoint.setLocation(x2center - visibleDimension.getWidth() / 2, y2center - visibleDimension.getHeight() / 2);
            }
        }
        if (updatePoint != null) {
            JViewport viewport = pictureScrollPane.getViewport();
            viewport.setViewPosition(updatePoint);
            pictureScrollPane.setViewport(viewport);
        }
        scrollablePicture.revalidate();
        pictureScrollPane.repaint();
    }

    /**
     * Save inspection results
     */
    public void saveInspectionResults() {
        // load the original assembled image
        ImagePlus assembledOriginal = new ImagePlus(directory.getAbsolutePath() + File.separator
                + IJImgProcessing.ASSEMBLED_JPEG);
        assembledOriginal.trimProcessor();
        if (assembledOriginal == null) {
            JOptionPane.showMessageDialog(dialog,
                    "Error, unable to properly read assembled image!\nNothing was saved.\nLocation: "
                    + directory.getAbsolutePath(), "Error with: " + IJImgProcessing.ASSEMBLED_JPEG,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        ManualInspection.save(wormsList, ManualInspection.getInspectedStatus(wormsList, true, null),
                directory, dialog, assembledOriginal);
    }

    /**
     * Save inspection results, and close
     */
    public void saveAndClose() {
        errors = SAVE_AND_CLOSE;  // hack to signal that page-view should close too
        saveInspectionResults();
        close();
    }

    /**
     * Updates the image-plus with annotations from the worms-list
     *
     * @param imagePlus
     * @return the annotated image
     */
    public ImagePlus updateImageAnnotations(ImagePlus imagePlus) {
        // annotate the colors-image
        ColorProcessor colorProcessor = (ColorProcessor) imagePlus.getProcessor();
        for (WormInfo worm : wormsList) {
            // first draw the bounding box
            Roi roi = new Roi(worm.pX - STROKE_WIDTH, worm.pY - STROKE_WIDTH, worm.width + STROKE_WIDTH + EXTRA_PIXELS, worm.height + STROKE_WIDTH + EXTRA_PIXELS);
            roi.setStrokeWidth(STROKE_WIDTH);
            colorProcessor.setRoi(roi);
            String annotation = null;
            if (worm.isDeletedInAnyWay() == true) {
                // deleted items are marked with a cross
                colorProcessor.setValue(Color.RED.getRGB());
                Line line = new Line(worm.pX - STROKE_WIDTH, worm.pY - STROKE_WIDTH, worm.pX + worm.width + STROKE_WIDTH / 2, worm.pY + worm.height + STROKE_WIDTH / 2);
                colorProcessor.setRoi(line);
                colorProcessor.setValue(Color.RED.getRGB());
                line.setStrokeWidth(STROKE_WIDTH / 2);
                line.drawPixels(colorProcessor);
                line = new Line(worm.pX - STROKE_WIDTH / 2, worm.pY + STROKE_WIDTH / 2 + worm.height, worm.pX + worm.width + STROKE_WIDTH / 2, worm.pY - STROKE_WIDTH / 2);
                colorProcessor.setRoi(line);
                line.setStrokeWidth(STROKE_WIDTH / 2);
                line.drawPixels(colorProcessor);
                line = new Line(worm.pX - STROKE_WIDTH / 2, worm.pY + (worm.height / 2), worm.pX + worm.width + STROKE_WIDTH / 2, worm.pY + (worm.height / 2));
                colorProcessor.setRoi(line);
                line.setStrokeWidth(STROKE_WIDTH / 2);
                line.drawPixels(colorProcessor);
                line = new Line(worm.pX + (worm.width / 2), worm.pY - STROKE_WIDTH / 2, worm.pX + (worm.width / 2), worm.pY + worm.height + STROKE_WIDTH / 2);
                colorProcessor.setRoi(line);
                line.setStrokeWidth(STROKE_WIDTH / 2);
                line.drawPixels(colorProcessor);
                continue;
            }
            if (worm.isSuspiciousInAnyWay() == true) {
                colorProcessor.setValue(SUSPICIOUS_COLOR);
                roi.drawPixels(colorProcessor);
                annotation = "unknown";
            } else if (worm.isNothingInAnyWay() == true) {
                colorProcessor.setValue(NOTHING_COLOR);
                roi.drawPixels(colorProcessor);
                annotation = "Nothing";
            } else {
                roi.setStrokeWidth(STROKE_WIDTH);
                if (worm.getMalesForSpinner() > worm.getHermaphroditesForSpinner()) {
                    colorProcessor.setValue(MALE_COLOR);
                } else {
                    colorProcessor.setValue(HERMA_COLOR);
                }
                roi.drawPixels(colorProcessor);
                annotation = worm.getLabelNoHtml();
            }
            if (annotation != null) {
                int y = worm.pY - STROKE_WIDTH - 2;
                colorProcessor.drawString(annotation, worm.pX - STROKE_WIDTH - 2, y, Color.YELLOW);
            } else {
                JOptionPane.showMessageDialog(dialog, "Unable to figure what to do with:\n" + worm, "Programmer error.", JOptionPane.ERROR_MESSAGE);
            }
        }

        return imagePlus;
    }

    /**
     * Updates a color-processor with text-labels of number of worms inside a
     * bounding box
     *
     * @param colorProcessor
     * @param factor the factor to use in the coordinate system, 50% zoom factor
     * is 0.5
     */
    public void updateImageWithWormCount(ColorProcessor colorProcessor, double factor) {
        for (WormInfo worm : wormsList) {
            String annotation = null;
            if (worm.isDeletedInAnyWay() == true) {
                annotation = "deleted";
            } else if (worm.isSuspiciousInAnyWay() == true) {
                annotation = "unknown";
            } else if (worm.isNothingInAnyWay() == true) {
                annotation = "Nothing";
            } else {
                annotation = worm.getLabelNoHtml();
            }
            if (annotation != null) {
                int y = Math.round(Math.round((worm.pY - STROKE_WIDTH - 2) * factor));
                int x = Math.round(Math.round((worm.pX - STROKE_WIDTH - 2) * factor));
                colorProcessor.drawString(annotation, x, y, Color.YELLOW);
            } else {
                System.out.println("what to do? " + worm);
            }
        }
    }

    /**
     * Action after a click on popup menu
     *
     * @param actionEvent the action-event object
     */
    public void popupMenuItemActionPerformed(ActionEvent actionEvent) {
        String status = null;
        if (ONE_MALE.equals(actionEvent.getActionCommand()) == true) {
            status = WormInfo.MALE;
        }
        if (ONE_HERMA.equals(actionEvent.getActionCommand()) == true) {
            status = WormInfo.HERMAPHRODITE;
        }
        if (SUSPICIOUS_POPUP.equals(actionEvent.getActionCommand()) == true) {
            status = WormInfo.SUSPICIOUS;
        }
        if (TWO_HERMA_POPUP.equals(actionEvent.getActionCommand()) == true) {
            status = WormInfo.MULTIPLE + "0,2" + "]";
        }
        if (TWO_MALES_POPUP.equals(actionEvent.getActionCommand()) == true) {
            status = WormInfo.MULTIPLE + "2,0" + "]";
        }
        if (THREE_MALES_POPUP.equals(actionEvent.getActionCommand()) == true) {
            status = WormInfo.MULTIPLE + "3,0" + "]";
        }
        if (THREE_HERMA_POPUP.equals(actionEvent.getActionCommand()) == true) {
            status = WormInfo.MULTIPLE + "0,3" + "]";
        }
        if (OTHER_POPUP.equals(actionEvent.getActionCommand()) == true) {
            ChooseWormGenderDialog chooseWormGenderDialog = new ChooseWormGenderDialog(dialog, true);
            chooseWormGenderDialog.setVisible(true);
            String value = chooseWormGenderDialog.getValue();
            if (value == null) {
                return;
            }
            status = WormInfo.MULTIPLE + value + "]";
        }
        if (status == null) {
            JOptionPane.showMessageDialog(dialog, "Unable to figure out how many worms to set!", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int x = xPopup;
        int y = yPopup;
        boolean changesFlag = false;
        boolean insideFlag = false;
        List<Integer> sizeList = new ArrayList<Integer>();
        // case1: click occurs inside an existing bounding box
        for (WormInfo worm : wormsList) {
            if (x >= (worm.pX - STROKE_WIDTH) && x <= (worm.pX + worm.width + STROKE_WIDTH)
                    && y >= (worm.pY - STROKE_WIDTH) && y <= (worm.pY + worm.height + STROKE_WIDTH)) {
                insideFlag = true;
                changesFlag = true;
                worm.setViewingStatus(status);
                break;
            }
            sizeList.add(worm.width);
            sizeList.add(worm.height);
        }

        // case2: user did click for purpuse of adding worm(s)
        if (insideFlag == false) {
            int size = 55;
            if (sizeList.size() > 1) {
                double[] values = new double[sizeList.size()];
                for (int i = 0; i < sizeList.size(); i++) {
                    values[ i] = sizeList.get(i);
                }
                size = (int) Math.round(StatUtils.mean(values));
                // make size 50% bigger
                size = (int) Math.round(size * 1.5);
            }
            WormInfo worm = new WormInfo();
            worm.pX = x - size / 2;
            worm.pY = y - size / 2;
            worm.width = size;
            worm.height = size;
            worm.maskImageIDNumb = 9999;
            wormsList.add(worm);
            changesFlag = true;
            worm.setViewingStatus(status);
        }

        // when changes are made, update the image
        if (changesFlag == true) {
            // annotate the colors-image
            ImagePlus annotatedImagePlus = updateImageAnnotations(colorsImagePlus.duplicate());
            colorsImageIcon[ 0].setImage(annotatedImagePlus.getBufferedImage());
            // 50% image
            ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize(annotatedImagePlus.getWidth() / 2);
            updateImageWithWormCount(scaledImageProcessor, 0.5);
            colorsImageIcon[ 1] = new ImageIcon(scaledImageProcessor.getBufferedImage());

            scrollablePicture.colorImageIcon.setImage(colorsImageIcon[ zoom].getImage());
            scrollablePicture.revalidate();
            pictureScrollPane.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        // one-click: delete an existing worm object
        // right-click: show popup menu
        int button = mouseEvent.getButton();
        int x = mouseEvent.getX();
        int y = mouseEvent.getY();
        if (zoom == 1) {
            x = x * 2;
            y = y * 2;
        }
        if (zoom == 2) {
            x = x * 4;
            y = y * 4;
        }

        boolean changesFlag = false;
        if (button == MouseEvent.BUTTON1 && 1 == mouseEvent.getClickCount()) {
            // see whether click was inside a bounding box
            for (WormInfo worm : wormsList) {
                if (x >= (worm.pX - STROKE_WIDTH) && x <= (worm.pX + worm.width + STROKE_WIDTH)
                        && y >= (worm.pY - STROKE_WIDTH) && y <= (worm.pY + worm.height + STROKE_WIDTH)) {
                    if (worm.isDeletedInAnyWay() == true) {
                        worm.setViewingStatus(null);
                    } else {
                        worm.setViewingStatus(WormInfo.DELETED);
                    }
                    changesFlag = true;
                    break;
                }
            }
        }

        // was it a click for popup menu?
        if (button == MouseEvent.BUTTON3 && 1 == mouseEvent.getClickCount()) {
            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            xPopup = x;
            yPopup = y;
        }

        // when changes are made, update the image
        if (changesFlag == true) {
            // annotate the colors-image
            ImagePlus annotatedImagePlus = updateImageAnnotations(colorsImagePlus.duplicate());
            colorsImageIcon[ 0].setImage(annotatedImagePlus.getBufferedImage());
            // 50% image
            ColorProcessor scaledImageProcessor = (ColorProcessor) annotatedImagePlus.getProcessor().resize(annotatedImagePlus.getWidth() / 2);
            updateImageWithWormCount(scaledImageProcessor, 0.5);
            colorsImageIcon[ 1] = new ImageIcon(scaledImageProcessor.getBufferedImage());

            scrollablePicture.colorImageIcon.setImage(colorsImageIcon[ zoom].getImage());
            scrollablePicture.revalidate();
            pictureScrollPane.repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Get the errors
     *
     * @return the errors; null when no errors detected; it is also use to
     * signal a couple of specific event to the window that called plate-view
     */
    public String getErrors() {
        return errors;
    }

    /**
     * Clears up memory
     */
    public void clearStuff() {
        for (int i = 0; i < colorsImageIcon.length; i++) {
            Image image = colorsImageIcon[ i].getImage();
            if (image != null) {
                image.flush();
            }
            colorsImageIcon[ i] = null;
        }
        scrollablePicture = null;
    }

    @Override
    public void windowDeactivated(WindowEvent windowEvent) {
    }

    @Override
    public void windowActivated(WindowEvent windowEvent) {
    }

    @Override
    public void windowDeiconified(WindowEvent windowEvent) {
    }

    @Override
    public void windowIconified(WindowEvent windowEvent) {
    }

    @Override
    public void windowClosed(WindowEvent windowEvent) {
    }

    @Override
    public void windowClosing(WindowEvent windowEvent) {
        if (errors == null) {
            errors = PAGE_VIEW;  // hack to signal that page-view should re-generate
        }
        close();
    }

    @Override
    public void windowOpened(WindowEvent windowEvent) {
    }

} // class PlateView

