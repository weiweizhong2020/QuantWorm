/* 
 * Filename: MotionDetection.java
 * This class conducts actual image processing and analysis
 * in the Lifespan assay software 
 */
package edu.rice.wormlab.lifespan;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public final class MotionDetection implements ChangeListener, KeyListener {

    /**
     * filename to use for saving results
     */
    public static final String N_LIVE_RESULTS_TXT = "result-lifespan.txt";
    /**
     * filename prefix to use for historical files of results
     */
    public static final String HISTORICAL = "historical.";
    /**
     * constant used in results file
     */
    public static final String WORM_DETAILS = "#worm-details:";
    /**
     * constant used in results file
     */
    public static final String INSPECTED_BY_HUMAN = "Inspected-by-human";
    /**
     * total-life-worms constant used in results file
     */
    public static final String TOTAL_LIVE_WORMS = "totalLiveWorms=";
    
    // current page when inspecting results manually
    private int currentPage;
    private final int BUTTON_WIDTH = 300;
    // size-height of button
    private final int BUTTON_HEIGHT = 200;
    private DetectionCondition wormSetting = new DetectionCondition();
    private SequentialLabeling sq_sub = null;
    private SequentialLabeling sq = null;
    private List<BinaryRegion> roiList = null;
    ImagePlus assembled1 = null;
    ImagePlus assembled2 = null;
    ImagePlus substractedImage = null;
    ImagePlus binarySubtractedImage = null;
    ImagePlus assembled2_bw = null;
    ImagePlus assembled2_bw_beforeRemovingBorder = null;
    private JDialog inspectDialog;
    private JPanel buttonPanel;
    private JPanel operationPanel;
    private JPanel panel;
    private JButton previousPageButton;
    private JButton nextPageButton;
    private JButton saveButton;
    private JButton saveCloseButton;
    private JButton viewPlateButton;
    private JButton deleteAllButton;
    private JLabel countLabel;
    private JLabel pageLabel;
    private JButton[] eachButton = null;
    private JSpinner[] spinner = null;
    private SpinnerModel[] spinnerModel = null;
    private List<WormInfo> wormsList = null;
    private List<Integer> wormsOriginalList = null;
    private List<ImageIcon> wormColorIconList = null;
    private List<ImageIcon> wormColorIcon2List = null;
    private JPopupMenu popupMenu = null;
    private File folder1;
    private File folder2;
    private final JFrame parentFrame;
    private NativeImgProcessing imgProc = new NativeImgProcessing();

    
    /**
     * Constructor
     * @param frame the parent-frame
     */
    public MotionDetection(JFrame frame) {
        super();
        parentFrame = frame;
    }


    /**
     * Sets the two folders containing the images
     *
     * @param folder1 the first folder
     * @param folder2 the other folder
     * @return null when things go OK; otherwise it returns an error message
     */
    public String setFolders(File folder1, File folder2) {
        if (folder1.getName().endsWith(App.UNDERSCORE_UNDERSCORE_ONE) == true && folder2.getName().endsWith(App.UNDERSCORE_UNDERSCORE_ONE) == false) {
            this.folder1 = folder2;
            this.folder2 = folder1;
        }; // if
        if (folder1.getName().endsWith(App.UNDERSCORE_UNDERSCORE_ONE) == false && folder2.getName().endsWith(App.UNDERSCORE_UNDERSCORE_ONE) == true) {
            this.folder1 = folder1;
            this.folder2 = folder2;
        }; // if
        
        
        if (folder1.getName().equals(folder2.getName())
                && folder1.getName().endsWith(App.UNDERSCORE_UNDERSCORE_ONE) == false) {
            
            String path = folder1.getAbsolutePath();
            String pathOther = path + App.UNDERSCORE_UNDERSCORE_ONE;
                        
            this.folder1 = folder1;
            this.folder2 = new File(pathOther);
        }
        
        
        if (folder1.getName().equals(folder2.getName())
                && folder1.getName().endsWith(App.UNDERSCORE_UNDERSCORE_ONE) == true) {
            
            String path = folder1.getAbsolutePath();
            String pathOther = path.substring(0, 
                        path.length() - App.UNDERSCORE_UNDERSCORE_ONE.length());
            
            this.folder1 = new File(pathOther);
            this.folder2 = folder1;
        }
        
        
        if (this.folder1 == null || this.folder2 == null) {
            return "Unable to determine the before/after folders: " + folder1 + " , " + folder2;
        }; // if
        return null;
    }


	/**
	 * Runs the image-processing tasks
	 * @return  null when things go OK, otherwise it returns an error message
	 */
	public String do_imageProcessing() {
		return do_imageProcessing( null );
	}


	/**
	 * Runs the image-processing tasks
	 * @param  list  the worms-list to utilize; 
	 *               when null, then it is read from file or created via image-processing
	 * @return  null when things go OK, otherwise it returns an error message
	 */
	public String do_imageProcessing( List<WormInfo> list ) {
		// get the scanner-log for the purpose of assembling image
		ScannerLog scannerLog = null;
		
		roiList = null;
		wormsOriginalList = null;
		wormColorIconList = null;
		wormColorIcon2List = null;
		
		// when the list is null, a few things do have to be always be processed
		if( list == null ) {
			scannerLog = ScannerLog.readLog(folder1);
			String error = scannerLog.getErrors();
			if (error != null) {
				return error;
			}; // if

			assembled2_bw = null;
			wormsList = null;
			sq = null;
			sq_sub = null;
			binarySubtractedImage = null;
			assembled1 = Utilities.assembleImage_Reload(folder1.getAbsolutePath(), scannerLog);
			// we expect that the same scannerLog can be used for folder2
			assembled2 = Utilities.assembleImage_Reload(folder2.getAbsolutePath(), scannerLog);
		
			// Auto alignment
			BufferedImage assembled1Aligned = imgProc.alignImage(
				assembled1.getBufferedImage(), assembled2.getBufferedImage(), 10, 10);
			if (assembled1Aligned != null) {
				imgProc.saveImage(assembled1Aligned, "jpeg", folder1.getAbsolutePath()
					+ File.separator + Utilities.ASSEMBLED_JPEG );
				assembled1 = Utilities.assembleImage_Reload(folder1.getAbsolutePath(), scannerLog);
			}
		
			// image1-image2
			substractedImage = NewImage.createByteImage("substractedImage", assembled1.getWidth(), assembled1.getHeight(), 1, NewImage.FILL_BLACK);
			ImageProcessor ipSub1 = substractedImage.getProcessor();
			ipSub1.copyBits(assembled1.getProcessor(), 0, 0, Blitter.ADD);
			ipSub1.copyBits(assembled2.getProcessor(), 0, 0, Blitter.SUBTRACT);
		
		}; // if
		
		getRegions( list );
		
		return null;
	}


	/**
	 * Runs the User Interface, worms list is read from file or created via image-processing
	 */
	public void detect() {
		detect( null );
	}


	/**
	 * Runs the User Interface
	 * @param  list  the worms-list to utilize; 
	 *               when null, then it is read from file or created via image-processing
	 */
	public void detect( List<WormInfo> list ) {
		initComponents();

		String errors = do_imageProcessing( list );
		if( errors != null ) {
			JOptionPane.showMessageDialog( inspectDialog, "Error in Image-processing.\n" + "Error: " + errors, "Image Processing Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}; // if

		// 'do' press a page button for "Previous"
		pageButtonActionPerformed(new ActionEvent(parentFrame, 7, "Previous"));
    }

    /**
     * Initializes GUI components
     */
    public void initComponents() {
        currentPage = 0;
        popupMenu = new JPopupMenu();
        countLabel = new JLabel("");
        pageLabel = new JLabel("");
        inspectDialog = new JDialog(parentFrame, folder1.getAbsolutePath(), false);
		  inspectDialog.setDefaultCloseOperation( JDialog.DO_NOTHING_ON_CLOSE );
		  inspectDialog.addWindowListener( new WindowAdapter() {
			  public void windowClosing( WindowEvent e ) {
				  doCleanClose();
			  }
		  } );
        previousPageButton = new JButton("Previous (PgUp)");
        nextPageButton = new JButton("Next (PgDn)");
        previousPageButton.setActionCommand("Previous");
        nextPageButton.setActionCommand("Next");
        saveButton = new JButton("Save (Alt+S)");
        saveCloseButton = new JButton("Save & Close (Alt+C)");
        viewPlateButton = new JButton("Plate View (Alt+V)");
        deleteAllButton = new JButton("Delete all (U)");
        operationPanel = new JPanel(new GridBagLayout());
        buttonPanel = new JPanel(new GridBagLayout());
        panel = new JPanel(new GridBagLayout());

        eachButton = new JButton[9];
        spinner = new JSpinner[9];
        spinnerModel = new SpinnerModel[9];
        for (int i = 0; i < 9; i++) {
            eachButton[ i ] = new JButton();
            eachButton[ i ].setHorizontalTextPosition(SwingConstants.CENTER);
            eachButton[ i ].setVerticalTextPosition(SwingConstants.BOTTOM);
            eachButton[ i ].setMinimumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
            eachButton[ i ].setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
            eachButton[ i ].setActionCommand("" + i); //set the tag
            eachButton[ i ].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    eachButtonActionPerformed(evt);
                }
            });

            eachButton[ i ].addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent mouseEvent) {
                    mouseClickedOnEachButton(mouseEvent);
                }
            });
            spinnerModel[ i ] = new SpinnerNumberModel();
            spinner[ i ] = new JSpinner(spinnerModel[ i]);
            JComponent component = spinner[ i ].getEditor();
            NumberEditor numberEditor = (NumberEditor) component;
            JFormattedTextField formattedTextField = numberEditor.getTextField();
            formattedTextField.setColumns(2);
            SpinnerNumberModel spinnerNumberModel = (SpinnerNumberModel) spinner[ i].getModel();
            spinnerNumberModel.setMinimum(0);
            eachButton[ i ].addKeyListener(this);
        }; // for
        final int INST = 2;
        buttonPanel.add(eachButton[ 0 ], new GBC(0, 0).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[ 1 ], new GBC(1, 0).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[ 2 ], new GBC(2, 0).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(spinner[ 0 ], new GBC(0, 1).setInsets(1, 1, 1, 1).setAnchor(GBC.CENTER));
        buttonPanel.add(spinner[ 1 ], new GBC(1, 1).setInsets(1, 1, 1, 1).setAnchor(GBC.CENTER));
        buttonPanel.add(spinner[ 2 ], new GBC(2, 1).setInsets(1, 1, 1, 1).setAnchor(GBC.CENTER));
        buttonPanel.add(eachButton[ 3 ], new GBC(0, 2).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[ 4 ], new GBC(1, 2).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[ 5 ], new GBC(2, 2).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(spinner[ 3 ], new GBC(0, 3).setInsets(1, 1, 1, 1).setAnchor(GBC.CENTER));
        buttonPanel.add(spinner[ 4 ], new GBC(1, 3).setInsets(1, 1, 1, 1).setAnchor(GBC.CENTER));
        buttonPanel.add(spinner[ 5 ], new GBC(2, 3).setInsets(1, 1, 1, 1).setAnchor(GBC.CENTER));
        buttonPanel.add(eachButton[ 6 ], new GBC(0, 4).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[ 7 ], new GBC(1, 4).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(eachButton[ 8 ], new GBC(2, 4).setInsets(INST, INST, INST, INST).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        buttonPanel.add(spinner[ 6 ], new GBC(0, 5).setInsets(1, 1, 1, 1).setAnchor(GBC.CENTER));
        buttonPanel.add(spinner[ 7 ], new GBC(1, 5).setInsets(1, 1, 1, 1).setAnchor(GBC.CENTER));
        buttonPanel.add(spinner[ 8 ], new GBC(2, 5).setInsets(1, 1, 1, 1).setAnchor(GBC.CENTER));

        operationPanel.add(previousPageButton, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(nextPageButton, new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(pageLabel, new GBC(0, 2).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.CENTER));
        operationPanel.add(saveButton, new GBC(0, 3).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(saveCloseButton, new GBC(0, 4).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(viewPlateButton, new GBC(0, 5).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(deleteAllButton, new GBC(0, 6).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        operationPanel.add(countLabel, new GBC(0, 7).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));



        panel.add(buttonPanel, new GBC(0, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        panel.add(operationPanel, new GBC(1, 0).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST));
        inspectDialog.add(panel);


        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                saveInspectionResults( true, inspectDialog, wormsList, folder2.getAbsolutePath(), assembled2 );
            }
        });
        saveButton.setMnemonic(KeyEvent.VK_S);
        saveButton.addKeyListener(this);



        saveCloseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                saveClose();
            }
        });
        saveCloseButton.setMnemonic( KeyEvent.VK_C );
        saveCloseButton.addKeyListener(this);



        previousPageButton.setMnemonic(KeyEvent.VK_P);
        previousPageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pageButtonActionPerformed(evt);
            }
        });
        previousPageButton.addKeyListener(this);

        nextPageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pageButtonActionPerformed(evt);
            }
        });
        nextPageButton.setMnemonic(KeyEvent.VK_N);
        nextPageButton.addKeyListener(this);


        viewPlateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                viewPlate();
            }
        });
        viewPlateButton.setMnemonic(KeyEvent.VK_V);
        viewPlateButton.addKeyListener(this);



        deleteAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                deleteAllIntheCurrentPage();
            }
        });
        deleteAllButton.setMnemonic(KeyEvent.VK_U);
        deleteAllButton.addKeyListener(this);



        //pop menu
        JMenuItem menuItem = new JMenuItem("0 moving");
        menuItem.setActionCommand("000");

        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.setBorderPainted(true);
        popupMenu.add(menuItem);
        popupMenu.addSeparator();
        menuItem = new JMenuItem("1 moving");
        menuItem.setActionCommand("100");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("2 moving");
        menuItem.setActionCommand("200");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("3 moving");
        menuItem.setActionCommand("300");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("4 moving");
        menuItem.setActionCommand("400");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);

        menuItem = new JMenuItem("5 moving");
        menuItem.setActionCommand("500");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);

        menuItem = new JMenuItem("6 moving");
        menuItem.setActionCommand("600");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                menuItemActionPerformed(actionEvent);
            }
        });
        popupMenu.add(menuItem);

		// create the menu-bar
		JMenuBar menuBar = new JMenuBar();
		JMenu optionsMenu = new JMenu( "Options" );
		optionsMenu.setMnemonic( KeyEvent.VK_O );
		menuBar.add( optionsMenu );
		inspectDialog.setJMenuBar( menuBar );
		// add the delete-all option
		JMenuItem deleteAllMenuItem = new JMenuItem( "Delete all worms" );
		deleteAllMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent actionEvent ) {
				deleteAllWorms();
			}
		});
		optionsMenu.add( deleteAllMenuItem );

		SwingWorker<Void,Void> worker = new SwingWorker<Void,Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				boolean whichFlag = false;
				do {
					int i = currentPage * 9;
					for (int buttonIndex = 0; buttonIndex < eachButton.length; buttonIndex++) {
						if( eachButton[ buttonIndex ].getIcon() == null ) {
							continue;
						}; // if
						if( i < wormsList.size() ) {
							if( whichFlag == false ) {
								eachButton[ buttonIndex ].setRolloverIcon( wormColorIcon2List.get( i ) );
							}
							else {
								eachButton[ buttonIndex ].setRolloverIcon( wormColorIconList.get( i ) );
							}; // if
						}
						else {
							eachButton[ buttonIndex ].setRolloverIcon( null );
						}; // if
						i++;
					}; // for     
					whichFlag = ! whichFlag;
					try {
						Thread.sleep( 300 );
					}
					catch( InterruptedException ignore ) {
						// do nothing
					}; // try
				} while( currentPage >= 0 );
				return null;
			}
		};
		worker.execute();
    }

    /**
     * Event triggered when clicking Next or Previous page
     *
     * @param actionEvent the action-event object
     */
    private void pageButtonActionPerformed(ActionEvent actionEvent) {
        if ("Previous".equals(actionEvent.getActionCommand()) == true) {
            currentPage--;
            if (currentPage < 0) {
                currentPage = 0;
            }; // if
        }; // if

        if ("Next".equals(actionEvent.getActionCommand()) == true) {
            currentPage++;
        }; // if
        if (((currentPage + 1) * 9) > wormsList.size()) {
            currentPage = wormsList.size() / 9;
        }; // if
        int ceiling = (int) Math.ceil(wormsList.size() / 9.0);

        previousPageButton.setEnabled(currentPage != 0);
        nextPageButton.setEnabled((currentPage + 1) < ceiling);
        fillIcons();
    }

    
    /**
     * Event triggered when clicking panel image
     *
     * @param actionEvent the action-event object
     */
    private void eachButtonActionPerformed(ActionEvent actionEvent) {
        int buttonIndex = Integer.parseInt(actionEvent.getActionCommand());
        int i = currentPage * 9 + buttonIndex;
        WormInfo worm = wormsList.get(i);
        if (worm.deleted) {
            // switch back to its original value
            eachButton[ buttonIndex].setText(wormsOriginalList.get(i) + " moving");
            spinner[ buttonIndex].setValue(wormsOriginalList.get(i));
            worm.deleted = false;
        } else {
            eachButton[ buttonIndex].setText("deleted");
            worm.deleted = true;
            spinner[ buttonIndex].setValue(0);
        }
        if (worm.deleted || worm.nLive == 0) {
            eachButton[ buttonIndex].setForeground(Color.red);
        } else {
            eachButton[ buttonIndex].setForeground(Color.black);
        }
        updateCountLabel();
    }

    /**
     * Event triggered when clicking menu item of pop-up menu in the Plate view 
     *
     * @param actionEvent the action-event object
     */    
    private void menuItemActionPerformed(ActionEvent actionEvent) {
        JMenuItem tmpMenuItem = (JMenuItem) actionEvent.getSource();
        JPopupMenu tmpPopupMenu = (JPopupMenu) tmpMenuItem.getParent();
        JButton clickedButton = (JButton) tmpPopupMenu.getInvoker();
        int buttonIndex = Integer.parseInt(clickedButton.getActionCommand());
        int index = currentPage * 9 + buttonIndex;

        WormInfo worm = wormsList.get(index);

        if ("000".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 0;
            worm.deleted = true;
        }; // if
        if ("100".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 1;
            worm.deleted = false;
        }; // if
        if ("200".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 2;
            worm.deleted = false;
        }; // if
        if ("300".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 3;
            worm.deleted = false;
        }; // if
        if ("400".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 4;
            worm.deleted = false;
        }; // if
        if ("500".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 5;
            worm.deleted = false;
        }; // if
        if ("600".equals(actionEvent.getActionCommand()) == true) {
            worm.nLive = 6;
            worm.deleted = false;
        }; // if

        if (worm.deleted == true) {
            eachButton[ buttonIndex ].setText("deleted");
            spinner[ buttonIndex ].setValue(0);
        } else {
            eachButton[ buttonIndex].setText(worm.nLive + " moving");
            spinner[ buttonIndex ].setValue(worm.nLive);
        }; // if

        if (worm.deleted || worm.nLive == 0) {
            eachButton[ buttonIndex] .setForeground(Color.red);
        } else {
            eachButton[ buttonIndex ].setForeground(Color.black);
        }
        updateCountLabel();
    }

    
    /**
     * Shows up the popup menu 
     * @param  mouseEvent  the mouse-event
     */
    private void mouseClickedOnEachButton(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }; // if
    }

    /**
     * Processes the differential image, binarySubtractedImage is created
     */
    public void processDiffImage() {
        //binarize the differential image
        binarySubtractedImage = substractedImage.duplicate();


        //Binarization
        final int diff_ThresholdValue = 20;
        for (int i = 0; i < binarySubtractedImage.getWidth(); i++) {
            for (int j = 0; j < binarySubtractedImage.getHeight(); j++) {
                if (substractedImage.getProcessor().getPixelValue(i, j) >= diff_ThresholdValue) {
                    binarySubtractedImage.getProcessor().putPixel(i, j, 255);
                } else {
                    binarySubtractedImage.getProcessor().putPixel(i, j, 0);
                }
            }
        }

        //imgProc.saveImage(binarySubtractedImage.getBufferedImage(), "gif", folder1.getAbsolutePath()
        //                + File.separator + "assembled_diff.gif" );



        imClearBorder.imclearborder(binarySubtractedImage);
        //Label and measure
        sq_sub = new SequentialLabeling(binarySubtractedImage.getProcessor());
        sq_sub.applyLabeling();
        sq_sub.collectRegions();
        //for collecting selected image

        List<BinaryRegion> list = sq_sub.regions;  // the infomation of all binary regions is kept in list
        Iterator<BinaryRegion> brIterator = list.iterator();
        while (brIterator.hasNext()) {
            BinaryRegion br = (BinaryRegion) brIterator.next();
            double area = br.getSize();
            if (area > wormSetting.min_DiffPixelCount_Of_Worm_In_DiffBinaryImage && area < wormSetting.max_WormSize) {  //filtering according to area
                // do nothing
            } else { //clear small particles from the binary image
                Rectangle rec = br.getBoundingBox();
                int label = br.getLabel();
                int x = rec.x;
                int y = rec.y;
                int height = rec.height;
                int width = rec.width;
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        if (sq_sub.labels[(y + j) * binarySubtractedImage.getWidth() + x + i] == label) //excluding other objects in the same bounding box
                        {
                            binarySubtractedImage.getProcessor().putPixel(x + i, y + j, 0);
                        }
                    }
                }
            }
        }//end of while

    }

    /**
     * Updates the count-label
     */
    protected void updateCountLabel() {
        int totalLiveWorms = 0;
        for (WormInfo worm : wormsList) {
            if (worm.deleted == false) {
                totalLiveWorms += worm.nLive;
            }; // if
        }; // for
        countLabel.setText(totalLiveWorms + " moving worms");
    }

    
    /**
     * Create panel image
     */
    public void fillIcons() {
        int ceiling = (int) Math.ceil(wormsList.size() / 9.0);
        pageLabel.setText("page " + (currentPage + 1) + " of " + ceiling);
        updateCountLabel();

        int i = currentPage * 9;
        for (int buttonIndex = 0; buttonIndex < eachButton.length; buttonIndex++) {
            if (i < wormsList.size()) {
                WormInfo worm = wormsList.get(i);
                eachButton[ buttonIndex ].setEnabled(true);
			  
                eachButton[ buttonIndex ].setIcon(wormColorIconList.get(i));
                eachButton[ buttonIndex ].setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
                if (worm.deleted == false) {
                    eachButton[ buttonIndex ].setText(worm.nLive + " moving"); //display number of living worms
                    spinner[ buttonIndex ].setValue(worm.nLive);
                } else {
                    eachButton[ buttonIndex ].setText("deleted");
                    spinner[ buttonIndex ].setValue(0);
                }; // if

                if (worm.deleted || worm.nLive == 0) {
                    eachButton[ buttonIndex ].setForeground(Color.red);
                } else {
                    eachButton[ buttonIndex ].setForeground(Color.black);
                }


                eachButton[ buttonIndex ].setMinimumSize(new Dimension(worm.width * 2 + 20, worm.height * 2 + 60));
                spinner[ buttonIndex ].setEnabled(true);
            } else {
                eachButton[ buttonIndex ].setText("-----");
                eachButton[ buttonIndex ].setIcon(null);
                eachButton[ buttonIndex ].setPreferredSize(new Dimension(BUTTON_WIDTH / 2, BUTTON_HEIGHT / 2));
                eachButton[ buttonIndex ].setEnabled(false);
                spinner[ buttonIndex ].setEnabled(false);
                spinner[ buttonIndex ].setValue(0);
            }; // if
            eachButton[ buttonIndex ].setMargin(new Insets(2, 2, 2, 2));
            eachButton[ buttonIndex ].invalidate();
            spinner[ buttonIndex ].invalidate();

            i++;
        }; // for     

			// last thing is to setup the change-listener
        for( int s = 0; s < 9; s++ ) {
            spinner[ s ].addChangeListener( this );
        }; // for     

        inspectDialog.validate();
        inspectDialog.repaint();
        inspectDialog.pack();
        inspectDialog.setVisible(true);
    }

    
    /**
     * Process image2 to create binary image applying adaptive thresholding
     * and conduct region labeling to identify worms 
     * 
     */
    public void processImage2() {
        assembled2_bw = assembled2.duplicate();

        //Binarize image
        short[][] srcPixelArray;
        srcPixelArray = imgProc.convert_Image_To_GrayShortArray(
                assembled2_bw.getProcessor().convertToRGB().getBufferedImage());

        BufferedImage imgBW =
                imgProc.adaptiveThresholding_Core(srcPixelArray, 15, 0.2f, 200);
        assembled2_bw.setImage(imgBW);

        if (assembled2_bw.getBitDepth() != 8) {
            ImageConverter icv = new ImageConverter(assembled2_bw);
            icv.convertToGray8();
        }; // if  

        assembled2_bw_beforeRemovingBorder = assembled2_bw.duplicate();

        //Label and measure
        sq = new SequentialLabeling(assembled2_bw.getProcessor());
        sq.applyLabeling();
        sq.collectRegions();
        //for collecting selected image
        roiList = new LinkedList<BinaryRegion>();

        List<BinaryRegion> list2 = sq.regions;  // the information of all binary regions is kept in list
        Iterator<BinaryRegion> brIterator2 = list2.iterator();
        while (brIterator2.hasNext()) {
            BinaryRegion br = (BinaryRegion) brIterator2.next();
            double area = br.getSize();
            Rectangle regionBoundingBox = br.getBoundingBox();
            if (area > wormSetting.min_WormSize && area < wormSetting.max_WormSize) {

                if (regionBoundingBox.getWidth() > 16 || regionBoundingBox.getHeight() > 16) {
                    roiList.add(br);
                }

            }
        }//end of while
    }

	/**
	 * Populates the 'wormsList' containing identified worms
	 * @param  list  the worms-list to utilize; 
	 *               when null, then it is read from file or created via image-processing
	 */
	public void getRegions( List<WormInfo> list ) {
		// two cases: 
		// utilize existing worm-list (via parameter or file), or not (do image-processing)
		boolean utilizeExistingWormsListFlag = false;

		if( list == null ) {
		processDiffImage();
		//binarize image 2
		processImage2();
			wormsList = new ArrayList<WormInfo>();
		}
		else {
			// sub-case 1: utilize worm-list via parameter
			wormsList = list;
			utilizeExistingWormsListFlag = true;
		}; // if
		wormsOriginalList = new ArrayList<Integer>();

		// sub-case 2: utilize worm-list via reading from file
		if( list == null ) {
			String directory = folder2.getAbsolutePath();
			if (directory.endsWith(File.separator) == false) {
				directory += File.separator;
			}; // if
			// see if there is a results file already
			File resultsFile = new File(directory + N_LIVE_RESULTS_TXT);
			if (resultsFile.exists() == true) {
				// case1: read worms from text file
				List<String> linesList = null;
				try {
					BufferedReader bufferedReader = new BufferedReader(new FileReader(resultsFile));
					String line = null;
					if (bufferedReader.ready()) {
						while ((line = bufferedReader.readLine()) != null) {
							if (linesList != null) {
								linesList.add(line);
							}; // if
							if (WORM_DETAILS.equalsIgnoreCase(line) == true) {
								linesList = new ArrayList<String>();
							}; // if
						}; // while
					} 
					else {
						JOptionPane.showMessageDialog(inspectDialog, "Unable to read " + N_LIVE_RESULTS_TXT + " file, please try again!", "ERROR", JOptionPane.ERROR_MESSAGE);
						bufferedReader.close();
						return;
					}; // if
					bufferedReader.close();
				} catch (FileNotFoundException fnfe) {
					JOptionPane.showMessageDialog(inspectDialog, "File not found: " + resultsFile.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
					return;
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(inspectDialog, "File input/output error with file: " + resultsFile.getAbsolutePath(), "Eror", JOptionPane.ERROR_MESSAGE);
					return;
				}; // try
				for (String each : linesList) {
					if (each.startsWith("#") == true) {
						continue;
					}; // if
					String[] pieces = each.split("\t");
					if (pieces.length != 5) {
						System.out.println("ignoring line " + each);
						continue;
					}; // if
					WormInfo worm = new WormInfo();
					worm.nLive = new Integer(pieces[ 0]);
					worm.pX = new Integer(pieces[ 1]);
					worm.pY = new Integer(pieces[ 2]);
					worm.width = new Integer(pieces[ 3]);
					worm.height = new Integer(pieces[ 4]);
					wormsList.add(worm);
				}; // for

				// update the worms-list with 'label'  from binarized assembled2
				for (BinaryRegion binaryRegion : roiList) {
					Rectangle rec = binaryRegion.getBoundingBox();
					// find it in the worms-list 
					for (WormInfo each : wormsList) {
						if (each.pX == rec.x && each.pY == rec.y && each.width == rec.width && each.height == rec.height) {
							each.label = binaryRegion.getLabel();
							each.isWormFoundInDiffImage = false;
							break;
						}
					}
				}


				// update the worms-list with 'label' from binarized differential image
				for (BinaryRegion binaryRegion : sq_sub.regions) {
					Rectangle rec = binaryRegion.getBoundingBox();
					// find it in the worms-list 
					for (WormInfo each : wormsList) {
						if (each.pX == rec.x && each.pY == rec.y && each.width == rec.width && each.height == rec.height) {
							each.label = binaryRegion.getLabel();
							each.isWormFoundInDiffImage = true;
							break;
						}
					}
				}

				utilizeExistingWormsListFlag = true;
			}; // if
		}; // if


        if (utilizeExistingWormsListFlag == false) { //de novo
            Iterator<BinaryRegion> roiIterator = roiList.iterator();
            while (roiIterator.hasNext()) {
                BinaryRegion roi = (BinaryRegion) roiIterator.next();
                Rectangle rec = roi.getBoundingBox();
                WormInfo info = new WormInfo();
                info.pX = rec.x;
                info.pY = rec.y;
                info.width = rec.width;
                info.height = rec.height;
                info.nLive = 1;
                info.label = roi.getLabel();
                info.deleted = false;
                info.isWormFoundInDiffImage = false;
                wormsList.add(info);
            }; // while

            trimWormsList();

            findMissingWorms_From_DiffImage();
        }; // if



        // place the objects into a set for sorting purposes
        Set<WormInfo> wormInfoSet = new TreeSet<WormInfo>();
        int wormCount = 0;
        for (WormInfo each : wormsList) {
            wormInfoSet.add(each);
            wormCount += each.nLive;
        }; // for

        // copy them back ordered
        wormsList.clear();
        int verifyWormCount = 0;
        for (WormInfo each : wormInfoSet) {
            wormsList.add(each);
            verifyWormCount += each.nLive;
            wormsOriginalList.add(each.nLive);
        }; // for



        //Conduct grouping for attached findings
        int marginIntersect = 3;
        for (int i = 0; i < wormsList.size(); i++) {

            for (int j = i + 1; j < wormsList.size(); j++) {
                WormInfo curWormsList = wormsList.get(i);
                WormInfo nextWormsList = wormsList.get(j);
                Rectangle curRectangle = new Rectangle(curWormsList.pX - marginIntersect,
                        curWormsList.pY - marginIntersect,
                        curWormsList.width + marginIntersect * 2,
                        curWormsList.height + marginIntersect * 2);
                Rectangle nextRectangle = new Rectangle(nextWormsList.pX - marginIntersect,
                        nextWormsList.pY - marginIntersect,
                        nextWormsList.width + marginIntersect * 2,
                        nextWormsList.height + marginIntersect * 2);
                if (curRectangle.intersects(nextRectangle)) {
                    wormsList.remove(j);
                    wormsList.add(i + 1, nextWormsList);
                    wormsOriginalList.remove(j);
                    wormsOriginalList.add(i + 1, nextWormsList.nLive);
                }
            }
        }


        // verification just in case
        if (wormCount != verifyWormCount) {
            System.out.println("ERROR, wormcount: " + wormCount + " verifyWormCount: " + verifyWormCount);
				JOptionPane.showMessageDialog( inspectDialog, "Error in worm-count!\n worm count: " + wormCount + "\nverify count: " + verifyWormCount, "Error in WormCount Verification!", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }; // if

        wormColorIconList = new ArrayList<ImageIcon>();
        wormColorIcon2List = new ArrayList<ImageIcon>();
        for (WormInfo worm : wormsList) {
            wormColorIconList.add( getImageIconDetail(worm, utilizeExistingWormsListFlag, false) );
            wormColorIcon2List.add( getImageIconDetail(worm, utilizeExistingWormsListFlag, true) );
        }; // for
    }

        
    /**
     * Excludes invalid findings that have negligible white area in subtracted bw image
     */
    public void trimWormsList() {
        if (wormsList == null) {
            return;
        }; // if

        NativeImgProcessing ImgProc = new NativeImgProcessing();


        for (int k = 0; k < wormsList.size(); k++) {
            WormInfo worm = wormsList.get(k);
            int x = worm.pX;
            int y = worm.pY;
            int height = worm.height;
            int width = worm.width;
            int label = worm.label;

            short imgSubClipArray[][] = new short[width][height];

            short imgSubBWClipArray[][] = new short[width][height];

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {

                    if (assembled2_bw.getPixel(x + i, y + j)[0] == 255
                            && sq.labels[(y + j) * substractedImage.getWidth() + (x + i)] == label) {
                        imgSubClipArray[i][j] = (short) substractedImage.getPixel(x + i, y + j)[0];
                    } else {
                        imgSubClipArray[i][j] = 0;
                    }


                    if (imgSubClipArray[i][j] > wormSetting.min_GrayDiff_In_DiffImage) {
                        imgSubBWClipArray[i][j] = 255;
                    } else {
                        imgSubBWClipArray[i][j] = 0;
                    }
                }; // for
            }; // for


            ImgProc.regionExtract_RasterScanning(imgSubBWClipArray, 0);

            short outIDMap[][];
            outIDMap = ImgProc.RegionLabeling_LabelIDMap;

            int outIDCount;
            outIDCount = ImgProc.RegionLabeling_ColorTable.length;

            int regionArray_WhiteCount[] = new int[outIDCount];

            //Initialzing array
            for (int q = 1; q < outIDCount; q++) {
                regionArray_WhiteCount[q] = 0;
            }

            //Counting pixel size of regions
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    regionArray_WhiteCount[outIDMap[i][j]]++;
                };
            };

            //Searching max area size of regions
            int maxWhiteCount = 0;
            for (int q = 1; q < outIDCount; q++) {
                if (maxWhiteCount < regionArray_WhiteCount[q]) {
                    maxWhiteCount = regionArray_WhiteCount[q];
                }
            }

            //Excluding invalid findings
            if (maxWhiteCount < wormSetting.min_WhitePixelCount_In_DiffImage) {
                worm.nLive = 0;
                wormsList.remove(k);
                k--;
            } else {
                worm.nLive = 1;
            }
        }
    }

    
    /**
     * Check if object is excluded in big regions
     * @param binSubImage
     * @param brObject
     * @param assem2_bw
     * @return 
     */
    public boolean isObjectInExcludedBigObject(ImagePlus binSubImage, BinaryRegion brObject, ImagePlus assem2_bw) {
        int label_Object_In_Diff;
        int label_Object_In_Assemble;
        Rectangle rec = brObject.getBoundingBox();

        label_Object_In_Diff = brObject.getLabel();


        List<BinaryRegion> list = sq.regions;
        Iterator<BinaryRegion> brIterator = list.iterator();


        while (brIterator.hasNext()) {
            BinaryRegion br = (BinaryRegion) brIterator.next();
            double area = br.getSize();
            if (area > wormSetting.max_WormSize) {
                label_Object_In_Assemble = br.getLabel();

                for (int y = rec.y; y < rec.y + rec.height; y++) {
                    for (int x = rec.x; x < rec.x + rec.width; x++) {
                        if (sq.labels[y * binSubImage.getWidth() + x] == label_Object_In_Assemble
                                && sq_sub.labels[y * binSubImage.getWidth() + x] == label_Object_In_Diff) {
                            return true;
                        }
                    }
                }
            }

        }

        return false;
    }

    
    /**
     * Find missing worms from differential image
     */
    public void findMissingWorms_From_DiffImage() {

        WormInfo worm;

        List<BinaryRegion> list = sq_sub.regions;
        Iterator<BinaryRegion> brIterator = list.iterator();


        while (brIterator.hasNext()) {
            BinaryRegion br = (BinaryRegion) brIterator.next();
            double area = br.getSize();
            if (area > wormSetting.min_MissingWormSize_In_DiffImage && area < wormSetting.max_WormSize) {
                Rectangle rec = br.getBoundingBox();

                //Find existing worms
                boolean isWormFound = false;
                for (int k = 0; k < wormsList.size(); k++) {
                    worm = wormsList.get(k);
                    if (rec.intersects(worm.pX, worm.pY, worm.width, worm.height) == true && worm.deleted == false) {
                        isWormFound = true;
                        break;
                    }
                }

                if (isWormFound == false
                        && isObjectInExcludedBigObject(binarySubtractedImage, br, assembled2_bw)) {
                    WormInfo newWorm = new WormInfo();
                    newWorm.nLive = 1;
                    newWorm.pX = rec.x;
                    newWorm.pY = rec.y;
                    newWorm.width = rec.width;
                    newWorm.height = rec.height;
                    newWorm.label = br.getLabel();
                    newWorm.deleted = false;
                    newWorm.isWormFoundInDiffImage = true;
						  wormsList.add( newWorm );
                }

            } //if
        } //while

    }

    
    /**
     * @param worm the worm
     * @param readFromFileFlag
     * @param isAnimatedSecondImageIcon if false, draw image1 to the left top segment (the first animated image)
     *                                   if true, draw image2 to the left top segment (the second animated image)
     * @return image-icon that is usable inside a button
     */
    public ImageIcon getImageIconDetail(WormInfo worm, boolean readFromFileFlag,
            boolean isAnimatedSecondImageIcon) {
        int x = worm.pX;
        int y = worm.pY;
        int height = worm.height;
        int width = worm.width;
        int label = worm.label;

        int paddingSizeInX;
        int paddingSizeInY;
        int x1_withPadding;
        int y1_withPadding;
        int x2_withPadding;
        int y2_withPadding;

        if (width < 130) {
            paddingSizeInX = 20;
        } else {
            paddingSizeInX = 0;
        }

        if (height < 80) {
            paddingSizeInY = 20;
        } else {
            paddingSizeInY = 0;
        }

        if (x - paddingSizeInX >= 0) {
            x1_withPadding = x - paddingSizeInX;
        } else {
            x1_withPadding = 0;
        }
        if (y - paddingSizeInY >= 0) {
            y1_withPadding = y - paddingSizeInY;
        } else {
            y1_withPadding = 0;
        }
        if ((x + width + paddingSizeInX) < assembled1.getWidth()) {
            x2_withPadding = x + width + paddingSizeInX;
        } else {
            x2_withPadding = assembled1.getWidth() - 1;
        }
        if ((y + height + paddingSizeInY) < assembled1.getHeight()) {
            y2_withPadding = y + height + paddingSizeInY;
        } else {
            y2_withPadding = assembled1.getHeight() - 1;
        }


        ImagePlus previewImagePlus = NewImage.createByteImage("previewImageIconDetail",
                (x2_withPadding - x1_withPadding + 1) * 2 + 2,
                (y2_withPadding - y1_withPadding + 1) * 2 + 2, 1, NewImage.GRAY8);


        //Copy the first image1
        if (isAnimatedSecondImageIcon) {
            for (int i = x1_withPadding; i < x2_withPadding + 1; i++) {
                for (int j = y1_withPadding; j < y2_withPadding + 1; j++) {
                    previewImagePlus.getProcessor().putPixel(i - x1_withPadding, j - y1_withPadding,
                            assembled2.getProcessor().getPixel(i, j));
                }
            }
        } else {
            for (int i = x1_withPadding; i < x2_withPadding + 1; i++) {
                for (int j = y1_withPadding; j < y2_withPadding + 1; j++) {
                    previewImagePlus.getProcessor().putPixel(i - x1_withPadding, j - y1_withPadding,
                            assembled1.getProcessor().getPixel(i, j));
                }
            }
        }


        //Copy the firsty image2
        int y_shifted = (y2_withPadding - y1_withPadding + 1) + 2;
        for (int i = x1_withPadding; i < x2_withPadding + 1; i++) {
            for (int j = y1_withPadding; j < y2_withPadding + 1; j++) {
                previewImagePlus.getProcessor().putPixel(i - x1_withPadding,
                        j - y1_withPadding + y_shifted,
                        assembled2.getProcessor().getPixel(i, j));
            }
        }


        //Copy differential image
        int x_shifted = (x2_withPadding - x1_withPadding + 1) + 2;
        if (worm.isWormFoundInDiffImage) {
            for (int i = x1_withPadding; i < x2_withPadding + 1; i++) {
                for (int j = y1_withPadding; j < y2_withPadding + 1; j++) {
                    if (binarySubtractedImage.getPixel(i, j)[0] == 255
                            && sq_sub.labels[j * substractedImage.getWidth() + i] == label) {
                        if (binarySubtractedImage.getProcessor().getPixel(i, j) > wormSetting.min_GrayDiff_In_DiffImage) {
                            previewImagePlus.getProcessor().putPixel(i - x1_withPadding + x_shifted,
                                    j - y1_withPadding,
                                    255);
                        } else {
                            previewImagePlus.getProcessor().putPixel(i - x1_withPadding + x_shifted,
                                    j - y1_withPadding,
                                    0);
                        }

                    } else {
                        previewImagePlus.getProcessor().putPixel(i - x1_withPadding + x_shifted,
                                j - y1_withPadding,
                                0);
                    }
                }
            }


        } else {
            for (int i = x1_withPadding; i < x2_withPadding + 1; i++) {
                for (int j = y1_withPadding; j < y2_withPadding + 1; j++) {
                    if (assembled2_bw.getPixel(i, j)[0] == 255
                            && sq.labels[j * substractedImage.getWidth() + i] == label) {
                        if (substractedImage.getProcessor().getPixel(i, j) > wormSetting.min_GrayDiff_In_DiffImage) {
                            previewImagePlus.getProcessor().putPixel(i - x1_withPadding + x_shifted,
                                    j - y1_withPadding,
                                    255);
                        } else {
                            previewImagePlus.getProcessor().putPixel(i - x1_withPadding + x_shifted,
                                    j - y1_withPadding,
                                    0);
                        }

                    } else {
                        previewImagePlus.getProcessor().putPixel(i - x1_withPadding + x_shifted,
                                j - y1_withPadding,
                                0);
                    }
                }
            }
        }


        //Create differential image with red-outlined

        //Create image clip
        ImagePlus binaryClipImagePlus = NewImage.createByteImage("skeletonClipImagePlus",
                (x2_withPadding - x1_withPadding + 1),
                (y2_withPadding - y1_withPadding + 1), 1, NewImage.GRAY8);



        if (worm.isWormFoundInDiffImage) {
            for (int i = x1_withPadding; i < x2_withPadding + 1; i++) {
                for (int j = y1_withPadding; j < y2_withPadding + 1; j++) {
                    if (binarySubtractedImage.getPixel(i, j)[0] == 255
                            && sq_sub.labels[j * substractedImage.getWidth() + i] == label) {
                        binaryClipImagePlus.getProcessor().putPixel(i - x1_withPadding,
                                j - y1_withPadding, 255);
                    } else {
                        binaryClipImagePlus.getProcessor().putPixel(i - x1_withPadding,
                                j - y1_withPadding, 0);
                    }
                }
            }
        } else {

            for (int i = x1_withPadding; i < x2_withPadding + 1; i++) {
                for (int j = y1_withPadding; j < y2_withPadding + 1; j++) {
                    if (assembled2_bw.getPixel(i, j)[0] == 255
                            && sq.labels[j * substractedImage.getWidth() + i] == label) {
                        binaryClipImagePlus.getProcessor().putPixel(i - x1_withPadding,
                                j - y1_withPadding, 255);
                    } else {
                        binaryClipImagePlus.getProcessor().putPixel(i - x1_withPadding,
                                j - y1_withPadding, 0);
                    }
                }
            }
        }


        //Get outlined image
        ByteProcessor savedBinaryImage = (ByteProcessor) binaryClipImagePlus.getProcessor();
        savedBinaryImage.erode();
        savedBinaryImage.erode();
        savedBinaryImage.invert();
        savedBinaryImage.outline();
        savedBinaryImage.invert();


        //Overlay outlined image onto 4th imageclip
        ImageProcessor imageProcessor = previewImagePlus.getProcessor();
        TypeConverter typeConverter = new TypeConverter(imageProcessor, false);
        ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
        for (int i = x1_withPadding; i < x2_withPadding + 1; i++) {
            for (int j = y1_withPadding; j < y2_withPadding + 1; j++) {
                int[] rgb = new int[3];

                if (binaryClipImagePlus.getProcessor().getPixel(i - x1_withPadding,
                        j - y1_withPadding) == 255) {
                    rgb[0] = 255;
                    rgb[1] = 0;
                    rgb[2] = 0;
                } else {
                    rgb[0] = assembled2.getProcessor().getPixel(i, j);
                    rgb[1] = rgb[0];
                    rgb[2] = rgb[0];
                }
                colorProcessor.putPixel(i - x1_withPadding + x_shifted,
                        j - y1_withPadding + y_shifted, rgb);
            }; // for
        }; // for


        if (worm.firstView) {
            if (readFromFileFlag == false) {
                worm.nLive = 1;
            }
            worm.firstView = false;
        }

        int clipWidth = previewImagePlus.getWidth();
        int clipHeight = previewImagePlus.getHeight();
        int clipIWidthMid = (x2_withPadding - x1_withPadding + 1);
        int clipHeightMid = (y2_withPadding - y1_withPadding + 1);
        colorProcessor.setValue(Color.BLUE.getRGB());
        colorProcessor.drawLine(0, clipHeightMid, clipWidth, clipHeightMid);
        colorProcessor.drawLine(clipIWidthMid, 0, clipIWidthMid, clipHeight);
        colorProcessor.drawLine(0, clipHeightMid + 1, clipWidth, clipHeightMid + 1);
        colorProcessor.drawLine(clipIWidthMid + 1, 0, clipIWidthMid + 1, clipHeight);


        ImagePlus imagePlus = new ImagePlus("color", colorProcessor);
        return new ImageIcon(imagePlus.getBufferedImage());
    }

    /**
     * save data and then close window
     *
     */
    protected void saveClose() {
        saveInspectionResults( true, inspectDialog, wormsList, folder2.getAbsolutePath(), assembled2 );
		  doCleanClose();
    }

	/**
	 * Does a clean Close
	 */
	protected void doCleanClose() {
		currentPage = -1;
		// allow time for worker-thread to finish
		try {
			Thread.sleep( 500 );
		}
		catch( InterruptedException ie ) {
			// do nothing
		}; // try
		this.inspectDialog.dispose();
	}


    /**
     * delete all worms in the current page
     *
     */
    protected void deleteAllIntheCurrentPage() {
        for (int buttonIndex = 0; buttonIndex < eachButton.length; buttonIndex++) {
            int index = currentPage * 9 + buttonIndex;
            if (index < wormsList.size()) {
                WormInfo worm = wormsList.get(index);
                worm.nLive = 0;
                worm.deleted = true;

                eachButton[ buttonIndex].setText("deleted");
                spinner[ buttonIndex].setValue(0);
                eachButton[ buttonIndex].setForeground(Color.red);
            }
        }
        updateCountLabel();
    }


    /**
     * delete all worms 
     */
    protected void deleteAllWorms() {
        for (int buttonIndex = 0; buttonIndex < eachButton.length; buttonIndex++) {
            int index = currentPage * 9 + buttonIndex;
            if (index < wormsList.size()) {

                eachButton[ buttonIndex].setText("deleted");
                spinner[ buttonIndex].setValue(0);
                eachButton[ buttonIndex].setForeground(Color.red);
            }
        }
		  for( WormInfo worm : wormsList ) {
            worm.nLive = 0;
            worm.deleted = true;
		  }; // for
        updateCountLabel();
    }


	/**
	 * Get the worm objects list
	 * @return  the worms list
	 */
	public List<WormInfo> getWormsList() {
		return wormsList;
	}


	/**
	 * Get folder where results are written
	 * @return  folder path
	 */
	public String getFolder() {
		return folder2.getAbsolutePath();
	}


	/**
	 * Get the assembled image inside of folder where results are written
	 * @return  assembled image
	 */
	public ImagePlus getAssembledImage() {
		return assembled2;
	}


    /**
     * displays the plate with colorful annotations on worms
     *
     */
    protected void viewPlate() {
		String directory = folder2.getAbsolutePath();
		if (directory.endsWith(File.separator) == false) {
			directory += File.separator;
		}; // if
		final String TITLE = "View: " + directory;
		final String BASE_IMAGE_FOLDER = folder1.getAbsolutePath();

		// crate the image
		ImageProcessor imageProcessor = assembled2.getProcessor();
		TypeConverter typeConverter = new TypeConverter( imageProcessor, false );
		ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
		PlateView plateView = new PlateView( BASE_IMAGE_FOLDER + File.separator + Utilities.ASSEMBLED_JPEG, new ImagePlus( "color", colorProcessor ), wormsList, inspectDialog.getPreferredSize(), parentFrame, TITLE, folder2.getAbsolutePath() );
		if( plateView.getErrors() != null ) {
			JOptionPane.showMessageDialog( inspectDialog, "Plate-view error!\n" + plateView.getErrors(), "Error in Plate-View!", JOptionPane.ERROR_MESSAGE);
		}
		else {
			// the following line will stop the worker-thread that updates button-images
			currentPage = -1;
			// allow time for worker-thread to finish
			try {
				Thread.sleep( 500 );
			}
			catch( InterruptedException ie ) {
				// do nothing
			}; // try
			// remove listeners from spinners
			plateView.show();
			if( PlateView.SAVE_AND_CLOSE.equals( plateView.getErrors() ) == true ) {
				// we also close
				doCleanClose();
				return;  // maybe not needed but just in case
			}; // if
			// verify whether to bring motion-detection back to normal
			if( PlateView.PAGE_VIEW.equals( plateView.getErrors() ) == true ) {
				inspectDialog.dispose();
				detect( plateView.wormsList );
			}; // if
		}; // if
		plateView = null;
	}

	/**
	 * Saves the inspection results
	 *
	 * @param  humanInspectionFlag  whether it was inspected by human
	 * @param  dialog  the dialog-window to use for error/warning messages if needed
	 * @param  wormsList the list of worms
	 * @param  folder  the folder into which write the results
	 * @param  assembledImagePlus  the assembled-image inside of folder into which write the results
	 */
 	protected static void saveInspectionResults( boolean humanInspectionFlag, JDialog dialog, List<WormInfo> wormsList, String folder, ImagePlus assembledImagePlus ) {
		if (folder == null) {
			JOptionPane.showMessageDialog( dialog, "Warning, unable to save results, folder is null!", "Unable to save results!", JOptionPane.ERROR_MESSAGE);
			return;
		}; // if
		if (folder.endsWith(File.separator) == false) {
			folder += File.separator;
		}; // if
		// get the info to be saved into a list
		List<String> linesList = new ArrayList<String>();
		int totalLiveWorms = 0;
		for (WormInfo worm : wormsList) {
			if (worm.deleted == true) {
				continue;
			}; // if
			if (worm.nLive == 0) {
				continue;
			}; // if
			linesList.add(worm.toString());
			totalLiveWorms += worm.nLive;
		}; // for

		// see if there is historical file, and if so, which number
		File historicalFile = null;
		int number = 0;
		do {
			number++;
			String filename = folder + HISTORICAL + number + "." + N_LIVE_RESULTS_TXT;
			historicalFile = new File(filename);
		} while (historicalFile.exists());

		// see if there is a results file already
		File resultsFile = new File(folder + N_LIVE_RESULTS_TXT);
		if (resultsFile.exists() == true) {
			File oldResultsFile = new File(folder + N_LIVE_RESULTS_TXT);
			boolean renamedFlag = oldResultsFile.renameTo(historicalFile);
			if (renamedFlag == false) {
				JOptionPane.showMessageDialog( dialog, "Error, unable to rename file " + N_LIVE_RESULTS_TXT + " to a historical filename.", "Cannot save!", JOptionPane.ERROR_MESSAGE);
				return;
			}; // if
		}; // if

		// save the new contents
		try {
			FileWriter fileWriter = new FileWriter(folder + N_LIVE_RESULTS_TXT);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			PrintWriter printWriter = new PrintWriter(bufferedWriter);
			printWriter.println(TOTAL_LIVE_WORMS + totalLiveWorms);
			if (humanInspectionFlag == true) {
				printWriter.println(INSPECTED_BY_HUMAN);
			}; // if
			printWriter.println("#Date: " + new Date());
			printWriter.println("#lifespan-assay-software-version: " + App.VERSION);
			printWriter.println(WORM_DETAILS);
			printWriter.println(WormInfo.HEADER);
			for (String line : linesList) {
				printWriter.println(line);
			}; // for
			printWriter.close();
		} 
		catch (IOException ioe) {
			ioe.printStackTrace();
			JOptionPane.showMessageDialog( dialog, "Error when saving " + N_LIVE_RESULTS_TXT + " as follows:<br>" + ioe, "I/O Error", JOptionPane.ERROR_MESSAGE);
			return;
		}; // try

		if( folder.endsWith( File.separator ) == false ) {
			folder += File.separator;
		}; // if

		// crate the assembled-colors image only when human inspects results
		if( humanInspectionFlag == true ) {
			ImageProcessor imageProcessor = assembledImagePlus.getProcessor();
			TypeConverter typeConverter = new TypeConverter(imageProcessor, false);
			ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
	
			final int oneColor = Color.BLUE.getRGB();
			final int strokeWidth = 8;
			for (WormInfo worm : wormsList) {
				if (worm.deleted == true) {
					continue;
				}; // if
				int color = oneColor;
				Roi roi = new Roi(worm.pX - strokeWidth, worm.pY - strokeWidth, worm.width + strokeWidth + 6, worm.height + strokeWidth + 6);
				colorProcessor.setRoi(roi);
				roi.setStrokeWidth(strokeWidth);
				colorProcessor.setValue(color);
				roi.drawPixels(colorProcessor);
				int y = worm.pY - strokeWidth - 2;
				colorProcessor.drawString("" + worm.nLive, worm.pX - strokeWidth - 2, y);
			}; // for

			ImagePlus imagePlus = new ImagePlus(folder, colorProcessor);
			FileSaver fileSaver = new FileSaver( imagePlus );
			fileSaver.saveAsJpeg( folder + "assembled_colors.jpeg" );
		}; // if
	}

    /**
     * Changes of spinners show up here
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent changeEvent) {
        if (wormsList == null) {
            return;
        }; // if
        Object source = changeEvent.getSource();
        for (int buttonIndex = 0; buttonIndex < 9; buttonIndex++) {
            if (spinner[ buttonIndex] == source) {
                int wormIndex = buttonIndex + currentPage * 9;
                if (wormIndex >= wormsList.size()) {
                    return;
                }; // if
                WormInfo worm = wormsList.get(wormIndex);
                if (worm == null) {
                    JOptionPane.showMessageDialog(inspectDialog, "Internal error, worm at index " + wormIndex
                            + " is null!", "Internal (stateChanged)", JOptionPane.ERROR_MESSAGE);
                    return;
                }; // if
                Integer value = new Integer(spinner[ buttonIndex].getValue().toString());
                if (value == 0) {
                    eachButton[ buttonIndex].setText("deleted");
                    worm.nLive = 0;
                    worm.deleted = true;
                } else {
                    worm.nLive = value;
                    worm.deleted = false;
                    spinner[ buttonIndex].setValue(value);
                    eachButton[ buttonIndex].setText(value + " moving");
                }; // if

                if (worm.deleted || worm.nLive == 0) {
                    eachButton[ buttonIndex].setForeground(Color.red);
                } else {
                    eachButton[ buttonIndex].setForeground(Color.black);
                }

                updateCountLabel();
            }; // if
        }; // for
    }

    // needed for KeyListener
    public void keyTyped(KeyEvent e) {
        // do nothing
    }

    // needed for KeyListener
    public void keyPressed(KeyEvent e) {
        // do nothing
    }

    // needed for KeyListener, handle page-up, page-down, and escape keys
    public void keyReleased(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_RELEASED) {
            if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                pageButtonActionPerformed(new ActionEvent(parentFrame, 8, "Previous"));
            }
            if( e.getKeyCode() == KeyEvent.VK_PAGE_DOWN
		&& nextPageButton.isEnabled() == true ) {
                pageButtonActionPerformed(new ActionEvent(parentFrame, 9, "Next"));
            }


            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					doCleanClose();
            }

        }
    }
}
