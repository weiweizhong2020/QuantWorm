/**
 * Filename: GenderAnalyzer.java
 *
 * Analysis of images to determine gender (male or hermaphrodite)
 */
package org.quantworm.wormgender;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class GenderAnalyzer {

    /**
     * constant for filename of assembled colors
     */
    public static final String ASSEMBLED_COLORS = "assembled_colors.jpeg";

    public ScannerLog scannerLog;
    public ImagePlus currentRegion;
    public ImagePlus grayRegion;
    public ByteProcessor curProcessor;
    public ByteProcessor savedBinaryImage;
    public ByteProcessor savedOutLine;
    public ByteProcessor curProcessorSafeCopy;
    public ByteProcessor savedOutLineSafeCopy;
    public BufferedImage orignalClipImage;
    public Point2D[] lastDiameterEndPoints1;
    public Point2D[] lastDiameterEndPoints2;
    public int wormLen;
    public double fatness;
    public static NativeImgProcessing imgProc = new NativeImgProcessing();
    public Iterator brIterator;
    public DirectoryReader dirReader = new DirectoryReader();
    public static final String TRAINING_SET_FILENAME = "TrainingSet.txt";
    
    /**
     * constant for image-processing-status
     */
    public static final String IMAGE_PROCESSING_STATUS = "Image Processing Status";

    private final JFrame parentFrame;
    private MahalanobisClassifier mClassifier = null;
    private final int[][] structuringElement = {
        {0, 0, 1, 1, 1, 1, 1, 0, 0},
        {0, 1, 1, 1, 1, 1, 1, 1, 0},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {0, 1, 1, 1, 1, 1, 1, 1, 0},
        {0, 0, 1, 1, 1, 1, 1, 0, 0},};
    private ImagePlus assembled = null;
    private ImagePlus cropped = null;
    private int croppedX = 0;
    private int croppedY = 0;
    private int croppedWidth = 0;
    private int croppedHeight = 0;
    private final int picSizeX = 640;
    private final int picSizeY = 480;
    private final int ncX = 1;
    private final int ncY = 1;
    private final int ncW = 8;
    private final int ncH = 11;
    private SequentialLabeling sq = null;
    private LinkedList<BinaryRegion> roiList = null;
    private LinkedList<WormInfo> infoList = null;

    public static int paddingSize = 10;
    private JDialog inspDialog;
    private final JFrame frameStatusWindow = new JFrame();
    private TextArea textStatusMessage = new TextArea();
    private File curTrainingImageFile;
    private File curImageFolder;
    public boolean isCreateReviewOverlayedImage = false;
    private final WormSettings wormSettings = new WormSettings();

    /**
     * Constructor
     *
     * @param frame the parent frame
     */
    public GenderAnalyzer(JFrame frame) {
        parentFrame = frame;

        //Intialize MahalanobisClassifier
        mClassifier = new MahalanobisClassifier();

        File trainingSetFile = new File(TRAINING_SET_FILENAME);
        if( trainingSetFile.exists() == false ) {
        	  // attempt to load it from one folder up
        	  trainingSetFile = new File(".." + File.separator + TRAINING_SET_FILENAME);
        }

        //Load default training set file
        mClassifier.import_TrainingSet(trainingSetFile);
        frameStatusWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frameStatusWindow.addWindowListener(
                new WindowListener() {
                    @Override
                    public void windowDeactivated(WindowEvent e) {
                    }

                    @Override
                    public void windowActivated(WindowEvent e) {
                    }

                    @Override
                    public void windowClosed(WindowEvent e) {
                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                    }

                    @Override
                    public void windowOpened(WindowEvent e) {
                    }

                    @Override
                    public void windowDeiconified(WindowEvent e) {
                    }

                    @Override
                    public void windowIconified(WindowEvent e) {
                    }
                }
        );
    }

    /**
     * Create and show Status window UI
     */
    private void createAndSetStatusWindow(Point mainWindowLocation) {
        frameStatusWindow.setLocation(mainWindowLocation.x,
                mainWindowLocation.y + 200);
        frameStatusWindow.setResizable(false);

        textStatusMessage = new TextArea("", 15, 70, TextArea.SCROLLBARS_VERTICAL_ONLY);
        textStatusMessage.setLocation(20, 20);
        textStatusMessage.setEditable(false);
        textStatusMessage.setFont(new Font("TimesRoman", Font.PLAIN, 12));

        frameStatusWindow.getContentPane().removeAll();
        frameStatusWindow.getContentPane().add(textStatusMessage);

        frameStatusWindow.pack();
        frameStatusWindow.setAlwaysOnTop(false);
        frameStatusWindow.setVisible(true);
    }

    /**
     * Update status window UI
     */
    private void updateStatusWindow(String windowTitle,
            String statusMessage) {

        if (windowTitle != null) {
            frameStatusWindow.setTitle(windowTitle);
        }

        if (statusMessage != null) {
            textStatusMessage.setText(statusMessage);
        }

        frameStatusWindow.invalidate();
    }

    /**
     * Conduct repetitive-parameter-(loc1 and loc2)-change-analysis This function
     * is used for optimizing parameters
     *
     * @param srcFolder
     */
    public void perform_ParameterChangeAnalysis(File srcFolder) {

        LinkedList<String> outStrList = new LinkedList<String>();
        int[] accuracyCountArray = new int[6];
        double loc1start;
        double loc1end;
        double loc2start;
        double loc2end;
        double intervalStep;

        //Set abosolute or relative distance method
        if (wormSettings.analysisMethod.equals("absolute")) {
            loc1start = 0;
            loc1end = 200;
            loc2start = 0;
            loc2end = 200;
            intervalStep = 20;
        } else {
            loc1start = 0;
            loc1end = 20;
            loc2start = 0;
            loc2end = 20;
            intervalStep = 2;
        }

        createAndSetStatusWindow(parentFrame.getLocation());

        outStrList.add("loc1\tloc2\tcorrect nMale"
                + "\twrong nMale\tcorrect nHerm\twrong nHerm"
                + "\tcorrect nY\twrong nT");

        //Change loc1 and loc2 repeatively
        for (double loc1 = loc1start; loc1 < loc1end; loc1 = loc1 + intervalStep) {
            for (double loc2 = loc2start; loc2 < loc2end; loc2 = loc2 + intervalStep) {

                if (loc1 >= loc2) {
                    //skip because R.inverse does not exist
                    continue;
                }

                wormSettings.curveLocation1 = loc1;
                wormSettings.curveLocation2 = loc2;

                updateStatusWindow("Parameter Change Analysis",
                        "\nWorking at loc1=" + loc1 + "   loc2=" + loc2
                        + "\n\n Phase 1: Analyzing training images...");

                analyze_TrainingImages(srcFolder,
                        new File(srcFolder.toString()
                                + File.separator
                                + TRAINING_SET_FILENAME));

                updateStatusWindow("Parameter Change Analysis",
                        "\nWorking at loc1=" + loc1 + "   loc2=" + loc2
                        + "\n\n Phase 2: Recalling and calculating accuracy...");

                calculate_AccuracyCount_InRecalling(srcFolder,
                        new File(srcFolder.toString()
                                + File.separator + TRAINING_SET_FILENAME),
                        accuracyCountArray);

                outStrList.add(loc1 + "\t" + loc2 + "\t" + accuracyCountArray[0]
                        + "\t" + accuracyCountArray[3]
                        + "\t" + accuracyCountArray[1]
                        + "\t" + accuracyCountArray[4]
                        + "\t" + accuracyCountArray[2]
                        + "\t" + accuracyCountArray[5]);
            }
        }

        Utilities.writeResults(srcFolder.toString()
                + File.separator
                + "ParameterChangeAnalysis by "
                + wormSettings.analysisMethod + ".txt", outStrList);

        frameStatusWindow.dispose();
    }


    /**
     * Conduct recalling and calculate accuracy of the recalling
     * This function is used for optimizing parameters
     * @param sourceFolderName
     * @param TraningTxtFile
     * @param countInfo
     */
        public void calculate_AccuracyCount_InRecalling(File sourceFolderName,
            File TraningTxtFile, int[] countInfo) {
        //countInfo
        //[0]: correct count of male
        countInfo[0] = 0;
        //[1]: correct count of herm
        countInfo[1] = 0;
        //[2]: correct count of Y
        countInfo[2] = 0;
        //[3]: wrong count of male
        countInfo[3] = 0;
        //[4]: wrong count of herm
        countInfo[4] = 0;
        //[5]: wrong count of Y
        countInfo[5] = 0;

        mClassifier.import_TrainingSet(TraningTxtFile);

        analyze_TrainingImages(sourceFolderName, TraningTxtFile);

        List<String> srcTrainingTxtList
                = Utilities.getLinesFromFile(TraningTxtFile);

        for (int q = 1; q < srcTrainingTxtList.size(); q++) {
            String[] subItems = srcTrainingTxtList.get(q).split("\t");

            if (subItems[0].toLowerCase().indexOf("\\male") > 0
                    || subItems[0].toLowerCase().indexOf(File.separator + "male") > 0) {
                if (Utilities.getInteger(subItems[5]) == 1) {
                    countInfo[0]++;
                } else {
                    countInfo[3]++;
                }
            } else if (subItems[0].toLowerCase().indexOf("\\herm") > 0
                    || subItems[0].toLowerCase().indexOf(File.separator + "herm") > 0) {
                if (Utilities.getInteger(subItems[6]) == 1) {
                    countInfo[1]++;
                } else {
                    countInfo[4]++;
                }
            } else if (subItems[0].toLowerCase().indexOf("\\l3l4") > 0
                    || subItems[0].toLowerCase().indexOf(File.separator + "l3l4") > 0) {
                if (Utilities.getInteger(subItems[7]) == 1) {
                    countInfo[2]++;
                } else {
                    countInfo[5]++;
                }
            }
        }
    }


    /**
     * Extract parameters from training images and create training set file
     * sourceFolderName: the root folder that includes three sub-folders of
     *   .../herm,  .../male,  and  .../L3L4
     * These three folders contain lots of jpg images of a single worm
     * @param sourceFolderName
     * @param outputTxtFileName
     */
        public void analyze_TrainingImages(File sourceFolderName,
            File outputTxtFileName) {

        curImageFolder = sourceFolderName;

        //Create review folder to show image processing
        //This folder can be deleted without problems
        if (isCreateReviewOverlayedImage) {
            File reviewOverlayedImageFolder = new File(
                    curImageFolder + File.separator + "ReviewProcessing");
            if (!reviewOverlayedImageFolder.exists()) {
                reviewOverlayedImageFolder.mkdir();
            }

        }

        File[] trainingImageFiles
                = DirectoryReader.getAllFilesInAllSubfolders(sourceFolderName, "jpg");
        List<String> resultList = new ArrayList<String>();
        resultList.add("File\ttrue length (um)\te1\te2"
                + "\tfatness\tnMale\tnHerm\tnY");

        //Process training clip image one by one
        for (File curImageFile : trainingImageFiles) {
            curTrainingImageFile = curImageFile;

            scannerLog = ScannerLog.readLog(
                    new File(curImageFile.getParent()));
            String error = scannerLog.getErrors();
            if (error != null) {
                continue;
            }

            assembled = new ImagePlus(curImageFile.toString());
            prepareCroppedImage();
            processing(true);

            if (infoList.size() == 1) {
                WormInfo curWormInfo = infoList.get(0);

                resultList.add(curImageFile.toString() + "\t"
                        + String.format("%.4f", curWormInfo.trueLen) + "\t"
                        + String.format("%.4f", curWormInfo.e[0]) + "\t"
                        + String.format("%.4f", curWormInfo.e[1]) + "\t"
                        + String.format("%.4f", curWormInfo.f) + "\t"
                        + curWormInfo.nMale + "\t"
                        + curWormInfo.nHerma + "\t"
                        + curWormInfo.nY + "\t");
            }
        }

        Utilities.writeResults(outputTxtFileName.toString(), resultList);
    }

    /**
     * Conducts batch processing
     *
     * @param srcFolder the folder to start with; all sub-folders are processed
     * @return null when things go OK, otherwise it returns a list of errors
     */
    public List<String> do_BatchProcessing(String srcFolder) {
        String startingFolder = "Initial folder: " + srcFolder;
        String[] tasks = new String[]{" - Create/load Assembled Image.",
            " - Analysis of Assembled Image.", " - Create Annotated Image."};
        String[] tasksNow = new String[]{" - Creating/loading Assembled Image.",
            " - Analyzing Assembled Image.", " - Creating Annotated Image."};
        String[] tasksDone = new String[]{" - Loaded Assembled Image.",
            " - Analyzed Assembled Image.", " - Created Annotated Image."};

        createAndSetStatusWindow(parentFrame.getLocation());
        updateStatusWindow(IMAGE_PROCESSING_STATUS, startingFolder 
                + "\n\nSearching all sub-folders. ");

        // get all sub-folders
        List<String> subFoldersList = DirectoryReader.getAllSubFolders(srcFolder);

        // filter out folders without data
        List<File> filteredList = new ArrayList<File>();
        for (String each : subFoldersList) {
            // skip folders that do not exist
            File folder = new File(each);
            if (folder.exists() == false) {
                continue;
            }
            // skip folders that do not containg thelog.txt
            File logFile = new File(each, ScannerLog.LOG_FILENAME);
            if (logFile.exists() == false) {
                continue;
            }
            filteredList.add(folder);
        }

        List<String> errorsList = new ArrayList<String>();
        int count = 1;

        // process filtered folders one by one
        for (File eachFolder : filteredList) {
            curImageFolder = eachFolder;

            // read log file at each folder
            scannerLog = ScannerLog.readLog(curImageFolder);
            String error = scannerLog.getErrors();
            if (error != null) {
                errorsList.add(error);
                continue;
            }

            // see whether to create review folder to show image processing,
            if (isCreateReviewOverlayedImage) {
                File reviewOverlayedImageFolder = new File(
                        curImageFolder + File.separator + "ReviewProcessing");
                if (reviewOverlayedImageFolder.exists() == true) {
                    // 'ReviewProcessing' folder deleted to avoid issues with suspicous elements of (any) previous analysys
                    Utilities.deleteFileAndFolder(reviewOverlayedImageFolder);
                }
                reviewOverlayedImageFolder.mkdir();
            }

            String curDir = eachFolder.getAbsolutePath();
            if (curDir.length() > (srcFolder.length() + 1)) {
                curDir = curDir.substring(srcFolder.length() + 1);
            }
            String details = tasksNow[ 0] + " <==== (now)\n" + tasks[ 1] + "\n" + tasks[ 2];
            if (errorsList.isEmpty() == false) {
                details += "\n\nErrors found so far:";
                for (String err : errorsList) {
                    details += "\n" + err;
                }
            }
            String percentage = Utilities.formatInt(((count * 3 - 3) * 100.0) / (filteredList.size() * 3)) + "%";
            //Assemble and crop image
            updateStatusWindow(IMAGE_PROCESSING_STATUS + ", " + percentage, startingFolder + "\n\nCurrent folder: " + curDir + "\n\nProgress: " + percentage + " (Folder " + count + " of " + filteredList.size() + ").\n\nTasks on this folder:\n" + details);

            assembleImage();

            if (assembled == null) {
                errorsList.add("Error obtaining assembled-image at folder: " + eachFolder.getAbsolutePath());
                count++;
                continue;
            }

            prepareCroppedImage();

            //Analyze image
            details = tasksDone[ 0] + "\n" + tasksNow[ 1] + " <==== (now)\n" + tasks[ 2];
            if (errorsList.isEmpty() == false) {
                details += "\n\nErrors found so far:";
                for (String err : errorsList) {
                    details += "\n" + err;
                }
            }
            percentage = Utilities.formatInt(((count * 3 - 2) * 100.0) / (filteredList.size() * 3)) + "%";
            updateStatusWindow(IMAGE_PROCESSING_STATUS + ", " 
                    + percentage, startingFolder + "\n\nCurrent folder: " 
                            + curDir + "\n\nProgress: " + percentage
                            + " (Folder " + count + " of " + filteredList.size()
                            + ").\n\nTasks on this folder:\n" + details);

            processing(false);

            //Save result to file
            String saveError = saveResultsToFile(infoList, curImageFolder, ResultsGender.NOT_INSPECTED);
            if (saveError != null) {
                errorsList.add("Error saving results-file at folder: " 
                        + eachFolder.getAbsolutePath() + ", " + saveError);
            }

            //Create result image named as 'assembled_color'
            details = tasksDone[ 0] + "\n" + tasksDone[ 1] + "\n" + tasksNow[ 2] + " <==== (now)";
            if (errorsList.isEmpty() == false) {
                details += "\n\nErrors found so far:";
                for (String err : errorsList) {
                    details += "\n" + err;
                }
            }
            percentage = Utilities.formatInt(((count * 3 - 1) * 100.0) / (filteredList.size() * 3)) + "%";
            updateStatusWindow(IMAGE_PROCESSING_STATUS + ", " 
                    + percentage, startingFolder + "\n\nCurrent folder: " 
                            + curDir + "\n\nProgress: " + percentage 
                            + " (Folder " + count + " of " + filteredList.size() 
                            + ").\n\nTasks on this folder:\n" + details);

            colorworms(assembled, curImageFolder, false, infoList);
            count++;
        }
        frameStatusWindow.dispose();
        if (errorsList.isEmpty() == true) {
            return null;
        }
        return errorsList;
    }

    /**
     * Displays colorful worms from worms-list and then save the image file
     *
     * @param imagePlus
     * @param folder the folder
     * @param wormsList
     * @param displayImageFlag true: displays image, false: does not display
     * image
     */
    public static void colorworms(ImagePlus imagePlus, File folder,
            boolean displayImageFlag, List<WormInfo> wormsList) {
        ImageProcessor imageProcessor = imagePlus.getProcessor();
        ij.process.TypeConverter typeConverter = 
                new ij.process.TypeConverter(imageProcessor, false);
        ij.process.ColorProcessor displayImageProcessor =
                (ij.process.ColorProcessor) typeConverter.convertToRGB();

        int males = 0;
        int hermas = 0;
        int suspicious = 0;
        int nothing = 0;
        int maleColor = Color.BLUE.getRGB();
        int hermaColor = Color.RED.getRGB();
        int suspiciousColor = Color.GREEN.getRGB();
        int nothingColor = Color.DARK_GRAY.getRGB();
        double value;
        displayImageProcessor.setValue(maleColor);
        int strokeWidth = 2;
        if (wormsList != null) {
            for (WormInfo worm : wormsList) {
                value = -1;
                if (WormInfo.MALE.equals(worm.getOriginalStatus()) == true) {
                    males++;
                    value = maleColor;
                }
                if (WormInfo.HERMAPHRODITE.equals(worm.getOriginalStatus()) == true) {
                    hermas++;
                    value = hermaColor;
                }
                if (WormInfo.SUSPICIOUS.equals(worm.getOriginalStatus()) == true) {
                    suspicious++;
                    value = suspiciousColor;
                }
                if (worm.getOriginalStatus().startsWith(WormInfo.MULTIPLE) == true) {
                    males += worm.nMale;
                    hermas += worm.nHerma;
                    if (worm.nHerma > 0) {
                        value = hermaColor;
                    }
                    if (worm.nMale > 0) {
                        value = maleColor;
                    }
                }
                if (WormInfo.NOTHING.equals(worm.getOriginalStatus()) == true) {
                    nothing++;
                    value = nothingColor;
                }
                if (value == -1) {
                    System.out.println("what happened? " + worm);
                    System.exit(1);
                }
                Roi roi = new Roi(worm.pX - strokeWidth, worm.pY - strokeWidth
                        , worm.width + strokeWidth, worm.height + strokeWidth);
                displayImageProcessor.setRoi(roi);
                roi.setStrokeWidth(strokeWidth);
                displayImageProcessor.setValue(value);
                roi.drawPixels(displayImageProcessor);
                int y = worm.pY - strokeWidth - 2;
                if (value == Color.YELLOW.getRGB()) {
                    displayImageProcessor.setValue(Color.BLACK.getRGB());
                    y = worm.pY;
                }
                displayImageProcessor.drawString(worm.getOriginalStatusLabel(),
                        worm.pX - strokeWidth - 2, y);
            }
        }

        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 100);
        displayImageProcessor.setFont(font);
        displayImageProcessor.setValue(0);
        displayImageProcessor.drawString("Directory: " + folder, 20, 120);
        displayImageProcessor.setValue(maleColor);
        displayImageProcessor.drawString("Total males: " + males, 20, 320);
        displayImageProcessor.setValue(hermaColor);
        displayImageProcessor.drawString("Total hermaphrodites: " + hermas, 20, 520);
        displayImageProcessor.setValue(suspiciousColor);
        displayImageProcessor.drawString("Total unknown items: " + suspicious, 20, 720);

        // displaying
        ImagePlus displayerImagePlus = new ImagePlus(
                folder.getAbsolutePath(), displayImageProcessor);
        if (displayImageFlag == true) {
            displayerImagePlus.show();
        }
        FileSaver fileSaver = new FileSaver(displayerImagePlus);
        fileSaver.saveAsJpeg(folder + File.separator + ASSEMBLED_COLORS);
    }

    /**
     * Assemble the image
     */
    public void assembleImage() {
        // get assembled_Org image
        File theFile = new File(
                curImageFolder + File.separator
                + IJImgProcessing.ASSEMBLED_JPEG);
        boolean existingAssembleOrgFileFlag = theFile.exists();
        ImagePlus assembledOrg;

        assembled = null;

        if (existingAssembleOrgFileFlag) {
            assembledOrg = new ImagePlus(curImageFolder
                    + File.separator + IJImgProcessing.ASSEMBLED_JPEG);

        } else {
            assembledOrg = IJImgProcessing.assembleImage_Reload(
                    curImageFolder.getAbsolutePath(), scannerLog);

            if (assembledOrg == null) {
                return;
            }
        }

        assembled = new ImagePlus("assembled", assembledOrg.getBufferedImage());

        //Create assembled_enhanced.jpeg
        ByteProcessor binIp = (ByteProcessor) assembledOrg.getProcessor();
        BackgroundSubtracter bgSub = new BackgroundSubtracter();
        bgSub.subtractBackround(binIp, 50);
        FileSaver saver = new FileSaver(assembledOrg);
        saver.saveAsJpeg(curImageFolder + File.separator + IJImgProcessing.ASSEMBLED_ENHANCED_JPEG);
    }

    // Obtain cropped image
    private void prepareCroppedImage() {
        if (assembled.getWidth() < 640 && assembled.getHeight() < 480) {
            croppedX = 0;
            croppedY = 0;
            croppedWidth = assembled.getWidth();
            croppedHeight = assembled.getHeight();
        } else {
            croppedX = ncX * picSizeX;
            croppedY = ncY * picSizeY;
            croppedWidth = ncW * picSizeX;
            croppedHeight = ncH * picSizeY;
        }

        if (assembled == null) {
            System.out.println("no Image!");
            return;
        }

        //get the cropped image
        cropped = IJImgProcessing.crop(
                assembled.getProcessor(), croppedX, croppedY,
                croppedWidth, croppedHeight);

    }

    /**
     * Processing the image of worms and fatness also counts the number of worms
     * of each gender
     *
     * @param isTrainingImageSet
     */
    public void processing(boolean isTrainingImageSet) {

        //Create imageClip folder
        //This folder contains essential mask image files
        //So, it should not be deleted
        File newMaskImageFolder = new File(
                curImageFolder + File.separator + "MaskImages");
        if (isTrainingImageSet == false) {
            if (!newMaskImageFolder.exists()) {
                newMaskImageFolder.mkdir();
            }
        }

        File reviewOverlayedImageFolder = null;
        if (isCreateReviewOverlayedImage) {
            reviewOverlayedImageFolder = new File(
                    curImageFolder + File.separator + "ReviewProcessing");
        }

        binarize(isTrainingImageSet);

        //Region labeling 
        labelValidObjects();

        //Process detected object one by one
        for (int curROI = 0; curROI < roiList.size(); curROI++) {

            BinaryRegion roi = roiList.get(curROI);

            Rectangle rec = roi.getBoundingBox();

            //Create a few processed image
            processCurrentRegion(roi);

            //Screen out invalid objects due to low stdvar
            ImageStatistics is = grayRegion.getStatistics();
            if (is.stdDev < wormSettings.minVar) {
                continue;
            }

            //Screen out invalid objects by fatness
            if (fatness < wormSettings.minFatness
                    || fatness > wormSettings.maxFatness) {
                continue;
            }

            // Check Branch and End points
            LinkedList<int[]> branchPoints
                    = IJImgProcessing.findBranchPoints(curProcessor, 255);
            int nBranch = branchPoints.size();
            LinkedList<int[]> endPoints
                    = IJImgProcessing.findEndPoints(curProcessor, 255);
            int nEnd = endPoints.size();
            double[] e = new double[nEnd];

            WormInfo info = new WormInfo();
            info.pX = croppedX + rec.x;
            info.pY = croppedY + rec.y;
            info.width = rec.width;
            info.height = rec.height;
            info.wormLen = wormLen;
            info.maskImageIDNumb = curROI;

            if (nEnd == 2 && nBranch == 0) {
                info.trueLen = IJImgProcessing.getTrueWormLen(curProcessor, 255,
                        scannerLog.getStepsPerPixelsX(),
                        scannerLog.getStepsPerPixelsY());

                //Screen out invalid objects by true length
                if (info.trueLen < wormSettings.minTrueLen) {
                    continue;
                }

                if (info.trueLen > wormSettings.maxTrueLen) {
                    info.setSuspicious(true);
                    infoList.add(info);
                    // write the mask image
                    if (isTrainingImageSet == false) {
                        imgProc.saveImage(savedBinaryImage.getBufferedImage(),
                                "gif", newMaskImageFolder + File.separator + "Object" + info.maskImageIDNumb + ".gif");
                    }

                    continue;
                }
            } else {
                double approxTrueLength
                        = NativeImgProcessing.get_ApproximateTrueLength_From_LengthInPixel(
                                wormLen,
                                scannerLog.getStepsPerPixelsX(),
                                scannerLog.getStepsPerPixelsX());

                //Screen out invalid objects if length is too long
                if (approxTrueLength < wormSettings.minTrueLen
                        || approxTrueLength > wormSettings.maxTrueLen * 5) {
                    continue;
                }

                info.setSuspicious(true);
                infoList.add(info);
                // write the mask image
                if (isTrainingImageSet == false) {
                    imgProc.saveImage(savedBinaryImage.getBufferedImage(),
                            "gif", newMaskImageFolder + File.separator + "Object" + info.maskImageIDNumb + ".gif");
                }

                continue;
            }

            if (isCreateReviewOverlayedImage) {
                curProcessorSafeCopy = (ByteProcessor) curProcessor.duplicate();
                savedOutLineSafeCopy = (ByteProcessor) savedOutLine.duplicate();

                orignalClipImage = IJImgProcessing.crop(assembled.getProcessor(),
                        info.pX, info.pY,
                        info.width, info.height).getBufferedImage();
            }

            //Prepare overLayedImage and its graphics
            BufferedImage overyLayedImage = null;
            Graphics gra = null;
            if (isCreateReviewOverlayedImage) {

                overyLayedImage = imgProc.get_SkeletonOverlayedRGBImage(
                        orignalClipImage,
                        curProcessorSafeCopy.getBufferedImage(),
                        savedOutLineSafeCopy.getBufferedImage(),
                        rec.x, rec.y);
                gra = overyLayedImage.getGraphics();
            }

            double D1;
            double D2;
            int D1loc;
            int D2loc;

            info.f = fatness;

            //From now on, create skeleton curve and then calculate diameters
            //along the skeleon curve
            //Then, eventually calculate e1 and e2
            //nEnd = 2;
            boolean isErrorFound = false;
            for (int r = 0; r < nEnd; r++) {

                LinkedList<int[]> ptCollector;
                ptCollector = traceSkeletonCurveFromEnd(
                        r, rec.width, rec.height, branchPoints, endPoints);

                if (ptCollector.size() == 0) {
                    isErrorFound = true;
                    continue;
                }

                LinkedList<Point2D[]> diameterEndPoints
                        = get_DiametricEndPoints(rec.width, rec.height, ptCollector);
                if (diameterEndPoints == null) {
                    isErrorFound = true;
                    continue;
                }

                LinkedList<Double> diameterList
                        = calculate_Diameters(diameterEndPoints);

                double[] trueLengthArray
                        = getTrueLengthArray(ptCollector,
                                scannerLog.getStepsPerPixelsX(),
                                scannerLog.getStepsPerPixelsY());

                double absLocation1;
                double absLocation2;
                if (wormSettings.analysisMethod.equals("percentage")) {
                    absLocation1 = info.trueLen * wormSettings.curveLocation1 / 100;
                    absLocation2 = info.trueLen * wormSettings.curveLocation2 / 100;
                } else {
                    absLocation1 = wormSettings.curveLocation1;
                    absLocation2 = wormSettings.curveLocation2;
                }

                //Find D1 and D2
                D1 = -1;
                D2 = -1;
                D1loc = -1;
                D2loc = -1;
                for (int q = 0; q < trueLengthArray.length; q++) {
                    if (q >= diameterList.size()) {
                        break;
                    }

                    if (trueLengthArray[q] > absLocation1) {
                        D1 = (double) diameterList.get(q);
                        D1loc = q;
                        break;
                    }
                }

                for (int q = 0; q < trueLengthArray.length; q++) {
                    if (q >= diameterList.size()) {
                        break;
                    }

                    if (trueLengthArray[q] > absLocation2) {
                        D2 = (double) diameterList.get(q);
                        D2loc = q;
                        break;
                    }
                }

                if (D1 == -1 || D2 == -1) {
                    isErrorFound = true;
                    continue;
                }
                e[r] = D1 / D2;

                // save the final cross sectional lines
                lastDiameterEndPoints1 = diameterEndPoints.get(D1loc);
                lastDiameterEndPoints2 = diameterEndPoints.get(D2loc);

                if (isCreateReviewOverlayedImage) {
                    if (gra != null) {
                        gra.setColor(Color.CYAN);
                        gra.drawLine(
                                (int) lastDiameterEndPoints1[0].getX(),
                                (int) lastDiameterEndPoints1[0].getY(),
                                (int) lastDiameterEndPoints1[1].getX(),
                                (int) lastDiameterEndPoints1[1].getY());

                        gra.drawLine(
                                (int) lastDiameterEndPoints2[0].getX(),
                                (int) lastDiameterEndPoints2[0].getY(),
                                (int) lastDiameterEndPoints2[1].getX(),
                                (int) lastDiameterEndPoints2[1].getY());
                    }
                }

            }

            if (isErrorFound) {
                //some error found
                continue;
            }

            mClassifier.classifyWorm(info, info.trueLen, e, fatness);

            infoList.add(info);

            //Write the mask image
            if (isTrainingImageSet == false) {
                imgProc.saveImage(
                        savedBinaryImage.getBufferedImage(),
                        "gif", newMaskImageFolder
                        + File.separator + "Object"
                        + info.maskImageIDNumb + ".gif");
            }

            //Write review image
            if (isCreateReviewOverlayedImage) {
                
                if (reviewOverlayedImageFolder != null) {
                if (isTrainingImageSet) {
                    imgProc.saveImage(overyLayedImage,
                            "gif",
                            reviewOverlayedImageFolder.getAbsolutePath()
                            + File.separator
                            + Utilities.get_FileNameWithoutExtension(
                                    curTrainingImageFile.getName())
                            + " Object" + info.maskImageIDNumb + ".gif");
                } else {
                    imgProc.saveImage(overyLayedImage,
                            "gif",
                            reviewOverlayedImageFolder.getAbsolutePath()
                            + File.separator + "Object" + curROI + ".gif");
                }
                }
                
            }

            //This is for demonstration how to create red outlined image
            if (false) {
                if (!isTrainingImageSet) {
                    ColorProcessor outLineOverlayedIconImageProc
                            = get_RedOutlineOverlayedIconImageProc(assembled.getProcessor(), curImageFolder,
                                    info.maskImageIDNumb,
                                    info.pX, info.pY, info.width, info.height);

                    imgProc.saveImage(
                            outLineOverlayedIconImageProc.getBufferedImage(),
                            "jpg", newMaskImageFolder
                            + File.separator + "Overlayed"
                            + info.maskImageIDNumb + ".jpg");
                }
            }

        }
    }

    /**
     * Obtain overlayed icon image
     * @param imageProcessor
     * @param folder
     * @param maskImageIDNumb
     * @param pX
     * @param pY
     * @param width
     * @param height
     * @return
     */
    public static ColorProcessor get_RedOutlineOverlayedIconImageProc(ImageProcessor imageProcessor,
            File folder, int maskImageIDNumb, int pX, int pY, int width, int height) {
        ImagePlus imageIcon = IJImgProcessing.crop(imageProcessor,
                pX - paddingSize, pY - paddingSize,
                width + paddingSize * 2,
                height + paddingSize * 2);

        ImagePlus maskImage2 = new ImagePlus(folder
                + File.separator + "MaskImages" + File.separator + "Object"
                + maskImageIDNumb + ".gif");
        maskImage2 = IJImgProcessing.add_Padding(maskImage2, paddingSize);
        maskImage2 = IJImgProcessing.create_OutlineImage(maskImage2, 4);

        ColorProcessor imageIconOverlayedProc
                = IJImgProcessing.overlay_MaskOnToSourceImage(
                        imageIcon, maskImage2, 255);

        return imageIconOverlayedProc;
    }

    /**
     * Conduct adaptive thresholding
     * @param isTrainingImageSet
     */
    public void binarize(boolean isTrainingImageSet) {

        short[][] srcImageArray
                = imgProc.convert_GrayImage_To_GrayShortArray(cropped.getBufferedImage());

        BufferedImage srcImage
                = imgProc.adaptiveThresholding_Core_To_Gray(srcImageArray,
                        100, 0.2f, 300);
        cropped.setImage(srcImage);

        if (isTrainingImageSet) {
            IJImgProcessing.fill(cropped.getProcessor(), 255, 0);
        }
        imClearBorder.imclearborder(cropped);
    }

    /**
     * Conduct region labeling and the first screening out
     */
    public void labelValidObjects() {
        //Labeling
        sq = new SequentialLabeling(cropped.getProcessor());
        sq.applyLabeling();
        sq.collectRegions();
        //for collecting selected image
        roiList = new LinkedList<BinaryRegion>();
        infoList = new LinkedList<WormInfo>();

        List<BinaryRegion> list = sq.regions;  // the infomation of all binary regions is kept in list
        brIterator = list.iterator(); //iterating the list
        while (brIterator.hasNext()) {
            BinaryRegion br = (BinaryRegion) brIterator.next();
            double area = br.getSize();

            if (area > wormSettings.minArea && area < wormSettings.maxArea) {  //filtering according to area
                roiList.add(br);
            }
        }
    }

    /**
     * Create several different images
     *
     * @param roi
     */
    public void processCurrentRegion(BinaryRegion roi) {
        double Area = roi.getSize();
        Rectangle rec = roi.getBoundingBox();
        int label = roi.getLabel();
        int x = rec.x;
        int y = rec.y;
        int height = rec.height;
        int width = rec.width;

        currentRegion = NewImage.createByteImage("currentRegion", width, height, 1, NewImage.FILL_BLACK);
        grayRegion = NewImage.createByteImage("grayRegion", width, height, 1, NewImage.GRAY8);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (sq.labels[(y + j) * cropped.getWidth() + x + i] == label) //excluding other objects in the same bounding box
                {
                    currentRegion.getProcessor().putPixel(i, j, cropped.getPixel(x + i, y + j));
                }
                grayRegion.getProcessor().putPixel(i, j, assembled.getPixel(x + i + croppedX, y + j + croppedY));
            }
        }

        curProcessor = (ByteProcessor) currentRegion.getProcessor();

        //close the image
        //padding and closing
        ByteProcessor padded = new ByteProcessor(width + 20, height + 20);
        padded.copyBits(curProcessor, 10, 10, Blitter.COPY);
        BinMorpher bin = new BinMorpher(structuringElement);
        bin.close(padded, structuringElement);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                curProcessor.putPixel(i, j, padded.getPixel(i + 10, j + 10));
            }
        }

        //get outline
        savedOutLine = (ByteProcessor) curProcessor.duplicate();
        savedOutLine.invert();
        savedOutLine.outline();
        savedOutLine.invert();

        skeletonize();

        wormLen = IJImgProcessing.getWormLen(curProcessor, 255);
        fatness = (double) Area / (double) wormLen;

        IJImgProcessing.trim(curProcessor, 255);

    }

    /**
     * Skeletonize and remove spurs
     */
    public void skeletonize() {
        savedBinaryImage = (ByteProcessor) curProcessor.duplicate();
        ImagePlus saved = NewImage.createByteImage("saved",
                savedBinaryImage.getWidth(),
                savedBinaryImage.getHeight(), 1, NewImage.FILL_BLACK);

        saved.setProcessor(savedBinaryImage);

        IJImgProcessing.removeSpur(curProcessor, 8, 255);   //remove small spurs before further proccessing
        curProcessor.invert();
        curProcessor.skeletonize();
        curProcessor.invert();
        IJImgProcessing.removeSpur(curProcessor, wormSettings.spurTh, 255);

        //Extend skeleton curve to the end
        short[][] skeletonCurveImageArray
                = imgProc.convert_GrayImage_To_GrayShortArray(curProcessor.getBufferedImage());
        short[][] binaryImageArray
                = imgProc.convert_GrayImage_To_GrayShortArray(saved.getBufferedImage());
        short[][] extendSkeletonCurveImageArray
                = imgProc.extend_SkeletonCurve(skeletonCurveImageArray, binaryImageArray);
        BufferedImage extendSkeletonCurveImage
                = imgProc.convert_GrayShortArray_To_GrayImage(extendSkeletonCurveImageArray);

        currentRegion.setImage(extendSkeletonCurveImage);
        curProcessor = (ByteProcessor) currentRegion.getProcessor();
    }

    /* Save the list of worms to files ( ResultsGender.RESULTS_TXT).
     * @param  wormInfoList  the list of worms
     * @param  folder  the folder into which save results
     * @param  inspection  the text to use as inspected value ( when any suspicous worm object is found, then inspected becomes text is forced into NOT_INSPECTED)
     * @return  null when things go OK; otherwise it returns an error message
     */
    public static String saveResultsToFile(List<WormInfo> wormInfoList, File folder, String inspection) {
        if (wormInfoList == null) {
            return "No Results To Save!";
        }

        List<String> resultList = new ArrayList<String>();

        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd E hh:mm:ss");

        //Write head
        resultList.add("# Last update\t" + simpleDateFormat.format(date));
        resultList.add("# pX\tpY\twidth\theight\t"
                + "nHerm\tnMale\t"
                + "length (um)\tfatness\te1\te2\tmaskImageIDNumb\tunknown");

        //Count worms
        int nHerma = 0;
        int nMale = 0;
        boolean anySuspicousFlag = false;

        for (WormInfo wormInfo : wormInfoList) {
            int tmpMales = 0;
            int tmpHermas = 0;
            // there are two cases
            if (wormInfo.getViewingStatus() == null
                    || wormInfo.getOriginalStatus().equals(wormInfo.getViewingStatus()) == true) {
                // case one: original = viewing, or no viewing status has changed
                tmpMales = wormInfo.nMale;
                tmpHermas = wormInfo.nHerma;
                // when there are zero males and zero hermaphrodites, it is set to suspicious
                if (wormInfo.nMale == 0 && wormInfo.nHerma == 0) {
                    wormInfo.setSuspicious(true);
                }
                if (wormInfo.isSuspicious() == true) {
                    anySuspicousFlag = true;
                }
            } else {
                // case two: there has been a status change wrt original status
                if (WormInfo.DELETED.equals(wormInfo.getViewingStatus()) == true) {
                    continue;
                }
                if (WormInfo.NOTHING.equals(wormInfo.getViewingStatus()) == true) {
                    wormInfo.setSuspicious(true);
                }
                if (WormInfo.MALE.equals(wormInfo.getViewingStatus()) == true) {
                    tmpMales++;
                }
                if (WormInfo.HERMAPHRODITE.equals(wormInfo.getViewingStatus()) == true) {
                    tmpHermas++;
                }
                if (wormInfo.getViewingStatus().startsWith(WormInfo.MULTIPLE) == true) {
                    tmpMales = wormInfo.getViewingMales();
                    tmpHermas = wormInfo.getViewingHermaphrodites();
                }
                if (WormInfo.SUSPICIOUS.equals(wormInfo.getViewingStatus()) == true) {
                    tmpMales = wormInfo.getViewingMales();
                    tmpHermas = wormInfo.getViewingHermaphrodites();
                    anySuspicousFlag = true;
                }
            }

            resultList.add(wormInfo.pX + "\t" + wormInfo.pY + "\t"
                    + wormInfo.width + "\t" + wormInfo.height + "\t"
                    + tmpHermas + "\t" + tmpMales + "\t"
                    + String.format("%.4f", wormInfo.trueLen) + "\t"
                    + String.format("%.6f", wormInfo.f) + "\t"
                    + String.format("%.6f", wormInfo.e[0]) + "\t"
                    + String.format("%.6f", wormInfo.e[1]) + "\t"
                    + wormInfo.maskImageIDNumb + "\t" + wormInfo.isSuspicious());

            nMale += tmpMales;
            nHerma += tmpHermas;
        }

        if (anySuspicousFlag == true) {
            inspection = ResultsGender.NOT_INSPECTED;
        }

        resultList.add(1, "# Herm count\t" + nHerma);
        resultList.add(2, "# Male count\t" + nMale);
        resultList.add(3, "# Status\t" + inspection);

        String error = Utilities.writeResults(folder + File.separator + ResultsGender.RESULTS_TXT, resultList);
        if (error != null) {
            return "Error while saving results file, " + error;
        }
        return null;
    }

    /**
     * Trace skeleton curve
     * @param r
     * @param width
     * @param height
     * @param branchPoints
     * @param endPoints
     * @return LinkedList<int[]> ptCollector ptCollector is an array of point
     * locations(x, y)
     */
    public LinkedList<int[]> traceSkeletonCurveFromEnd(
            int r, int width, int height,
            LinkedList<int[]> branchPoints, LinkedList<int[]> endPoints) {

        int[] end = endPoints.get(r);
        int lastW = end[0];
        int lastH = end[1];
        int jump = 0;
        int count = 0;
        int lastcount;
        LinkedList<int[]> ptCollector = new LinkedList<int[]>();

        while (true) {

            if (count >= (wormLen / wormSettings.partitionCount) + wormSettings.rangeOfEndOfFirstPartition) {
                break;
            }

            lastcount = count;
            curProcessor.putPixel(lastW, lastH, 0);
            int fail = 0;
            int starti = lastW - 1;
            int startj = lastH - 1;
            int endi = lastW + 1;
            int endj = lastH + 1;
            if (lastW == 0) {
                starti = 0;
            }
            if (lastW == width - 1) {
                endi = width - 1;
            }
            if (lastH == 0) {
                startj = 0;
            }
            if (lastH == height - 1) {
                endj = height - 1;
            }

            for (int i = starti; i <= endi; i++) {
                for (int j = startj; j <= endj; j++) {

                    if (curProcessor.getPixel(i, j) == 255) {
                        lastW = i;
                        lastH = j;
                        count++;
                        int[] pt = {i, j};
                        ptCollector.add(pt);
                        jump = 1;
                        break;
                    }
                    fail++;
                    if (fail == 9) {
                        jump = 1;
                        break;
                    }
                }
                if (jump == 1) {
                    jump = 0;
                    break;
                }
            }

            if (count == lastcount) {
                break;
            }
        }

        return ptCollector;

    }

    /**
     * Compute length in um
     *
     * @param width
     * @param height
     * @param ptCollector
     * @return an array of lengths
     */
    public LinkedList<Double> calculate_TrueDistanceAtEnd(int width, int height,
            LinkedList<int[]> ptCollector) {

        int count = ptCollector.size() - 1;

        LinkedList<Double> DIST = new LinkedList<Double>();
        for (int g = 0; g < count - wormSettings.rangeOfEndOfFirstPartition; g++) {

            int[] pt1 = ptCollector.get(g);
            int[] pt2 = ptCollector.get(g + wormSettings.rangeOfEndOfFirstPartition);
            int[] cur = ptCollector.get(g);
            double w1 = (double) cur[0];
            double h1 = (double) cur[1];
            double w2 = (double) cur[0];
            double h2 = (double) cur[1];
            double firstD;
            double secondD;
            double dist;
            double k;
            if ((pt2[0] != pt1[0])) {
                //k: slope between pt1 and pt2
                k = (double) (pt2[1] - pt1[1]) / (double) (pt2[0] - pt1[0]);
                if (k != 0) {
                    double ik = -1.000 / k;
                    while (w1 >= 0) {
                        h1 = Math.floor(ik * (w1 - (double) cur[0]) + (double) cur[1]);
                        if (h1 >= 0 && h1 < height && savedBinaryImage.getPixel(
                                (int) (Math.floor(w1)), (int) h1) == 255) {
                            savedOutLine.putPixel((int) Math.floor(w1), (int) h1, 255);
                            w1 = w1 - Math.min(1.000, 1.000 / Math.abs(ik));
                            //

                        } else {
                            break;
                        }
                    }

                    w1 = w1 + Math.min(1.000, 1.000 / Math.abs(ik));
                    h1 = Math.floor(ik * (w1 - (double) cur[0]) + (double) cur[1]);
                    firstD = getTrueLengthBetweenTwoPoints(cur[0], cur[1],
                            w1, h1);

                    while (w2 < width) {
                        h2 = Math.floor(ik * (w2 - (double) cur[0]) + (double) cur[1]);
                        if (h2 >= 0 && h2 < height && savedBinaryImage.getPixel((int) Math.floor(w2),
                                (int) h2) == 255) {
                            savedOutLine.putPixel((int) Math.floor(w2), (int) h2, 255);

                            w2 = w2 + Math.min(1.000, 1.000 / Math.abs(ik));
                        } else {
                            break;
                        }

                    }
                    w2 = w2 - Math.min(1.000 / Math.abs(ik), 1);
                    h2 = Math.floor(ik * (w2 - (double) cur[0]) + (double) cur[1]);
                    secondD = getTrueLengthBetweenTwoPoints(cur[0], cur[1],
                            w2, h2);
                    dist = firstD + secondD;

                } else {
                    while (h1 >= 0 && savedBinaryImage.getPixel((int) w1, (int) h1) == 255) {
                        savedOutLine.putPixel((int) w1, (int) h1, 255);
                        h1--;
                    }
                    firstD = getTrueLengthBetweenTwoPoints(0, cur[1],
                            0, h1 - 1);

                    while (h2 < height && savedBinaryImage.getPixel((int) w2, (int) h2) == 255) {
                        savedOutLine.putPixel((int) w2, (int) h2, 255);
                        h2++;
                    }
                    secondD = getTrueLengthBetweenTwoPoints(0, cur[1],
                            0, h2 - 1);
                    dist = firstD + secondD;

                }
            } else {
                while (w1 >= 0 && savedBinaryImage.getPixel((int) w1, (int) h1) == 255) {
                    savedOutLine.putPixel((int) w1, (int) h1, 255);
                    w1--;
                }
                firstD = getTrueLengthBetweenTwoPoints(cur[0], 0,
                        w1 - 1, 0);

                while (w2 < width && savedBinaryImage.getPixel((int) w2, (int) h2) == 255) {
                    savedOutLine.putPixel((int) w2, (int) h2, 255);
                    w2++;
                }
                secondD = getTrueLengthBetweenTwoPoints(cur[0], 0,
                        w2 - 1, 0);

                dist = firstD + secondD;

            }
            if (dist > 0) {
                DIST.add(dist);
            }

        }

        return DIST;
    }

    /**
     * Obtain an array of true length in um
     *
     * @param srcPoints
     * @param stepsPerPixelsX
     * @param stepsPerPixelsY
     * @return
     */
    public double[] getTrueLengthArray(LinkedList<int[]> srcPoints,
            Double stepsPerPixelsX, Double stepsPerPixelsY) {

        double[] trueLengths = new double[srcPoints.size()];
        int[] curPoint;
        int curPointX;
        int curPointY;
        int lastPointX;
        int lastPointY;
        double trueLengthAccu = 0;
        double diagonalUnitLength
                = Math.sqrt(Math.pow(stepsPerPixelsX * ScannerLog.micronsPerStepX, 2)
                        + Math.pow(stepsPerPixelsY * ScannerLog.micronsPerStepY, 2));

        curPoint = srcPoints.get(0);
        lastPointX = curPoint[0];
        lastPointY = curPoint[1];
        trueLengths[0] = 0;

        for (int q = 1; q < srcPoints.size(); q++) {
            curPoint = srcPoints.get(q);
            curPointX = curPoint[0];
            curPointY = curPoint[1];

            if (curPointX != lastPointX && curPointY != lastPointY) {
                trueLengthAccu += diagonalUnitLength;
            } else if (curPointX == lastPointX) {
                trueLengthAccu += stepsPerPixelsY * ScannerLog.micronsPerStepY;
            } else {
                trueLengthAccu += stepsPerPixelsX * ScannerLog.micronsPerStepX;
            }

            trueLengths[q] = trueLengthAccu;
            lastPointX = curPointX;
            lastPointY = curPointY;

        }

        return trueLengths;
    }

    /**
     * Get array of diameters
     *
     * @param diametricEndPoints
     * @return
     */
    public LinkedList<Double> calculate_Diameters(
            LinkedList<Point2D[]> diametricEndPoints) {

        LinkedList<Double> Diameters = new LinkedList<Double>();

        for (Point2D[] endsPoints : diametricEndPoints) {
            double diameter
                    = getTrueLengthBetweenTwoPoints(
                            endsPoints[0].getX(),
                            endsPoints[0].getY(),
                            endsPoints[1].getX(),
                            endsPoints[1].getY());
            Diameters.add(diameter);
        }

        return Diameters;
    }

    /**
     * Calculate true length (um) between two points
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public double getTrueLengthBetweenTwoPoints(double x1, double y1,
            double x2, double y2) {
        double diagonalDistance
                = Math.sqrt(Math.pow(scannerLog.getStepsPerPixelsX()
                                * ScannerLog.micronsPerStepX * (x2 - x1), 2)
                        + Math.pow(scannerLog.getStepsPerPixelsX()
                                * ScannerLog.micronsPerStepY * (y2 - y1), 2));

        return diagonalDistance;
    }

    /**
     * Get array of two end points of diametric cross sectional lines along a
     * curve The distance between two end points in a array is diameter
     *
     * @param width
     * @param height
     * @param ptCollector
     * @return
     */
    public LinkedList<Point2D[]> get_DiametricEndPoints(int width, int height,
            LinkedList<int[]> ptCollector) {

        int count = ptCollector.size() - 1;
        int tangentLineLength = 500;

        LinkedList<Point2D[]> DiametricEndPoints = new LinkedList<Point2D[]>();
        for (int g = 0; g < count - wormSettings.rangeOfEndOfFirstPartition; g++) {

            int[] pt1 = ptCollector.get(g);
            int[] pt2 = ptCollector.get(g + wormSettings.rangeOfEndOfFirstPartition);
            int[] cur = ptCollector.get(g);

            Point2D[] twoEndPoints;
            Point2D firstEndPoint;
            Point2D secondEndPoint;

            Point2D curLocation = new Point2D.Double(cur[0], cur[1]);
            Point2D curveDirectionalVector
                    = new Point2D.Double(pt1[0] - pt2[0], pt1[1] - pt2[1]);

            Point2D[] tangentLine
                    = LineMath.get_TangentLinePoints(curLocation,
                            curveDirectionalVector, tangentLineLength);

            int curX;
            int curY;
            int lastX = 0;
            int lastY = 0;
            double startq = 0;
            double minDistance = tangentLineLength;

            for (double q = 0; q < tangentLineLength; q = q + 0.1) {
                Point2D curPointInLine
                        = LineMath.get_InnnerPointInLine(tangentLine[0], tangentLine[1], q);

                curX = (int) Math.floor(curPointInLine.getX());
                curY = (int) Math.floor(curPointInLine.getY());

                double newDistance = LineMath.get_Distance(
                        new Point2D.Double(curX, curY),
                        new Point2D.Double(cur[0], cur[1]));

                if (newDistance < minDistance) {
                    minDistance = newDistance;
                    startq = q;
                }
            }

            if (startq == 0) {
                return null;
            }

            //First direction
            for (double q = startq; q < tangentLineLength; q = q + 0.3) {

                Point2D curPointInLine
                        = LineMath.get_InnnerPointInLine(tangentLine[0], tangentLine[1], q);

                curX = (int) Math.floor(curPointInLine.getX());
                curY = (int) Math.floor(curPointInLine.getY());

                if (curX >= 0 && curX < width && curY >= 0 && curY < height) {
                    if (savedBinaryImage.getPixel(curX, curY) == 0) {
                        break;
                    }
                } else {
                    break;
                }

                lastX = curX;
                lastY = curY;
            }
            firstEndPoint = new Point2D.Double(lastX, lastY);

            //Second direction
            for (double q = startq; q >= 0; q = q - 0.3) {

                Point2D curPointInLine
                        = LineMath.get_InnnerPointInLine(tangentLine[0], tangentLine[1], q);

                curX = (int) Math.floor(curPointInLine.getX());
                curY = (int) Math.floor(curPointInLine.getY());

                if (curX >= 0 && curX < width && curY >= 0 && curY < height) {
                    if (savedBinaryImage.getPixel(curX, curY) == 0) {
                        break;
                    }
                } else {
                    break;
                }
                lastX = curX;
                lastY = curY;
            }
            secondEndPoint = new Point2D.Double(lastX, lastY);

            twoEndPoints = new Point2D[]{firstEndPoint, secondEndPoint};
            DiametricEndPoints.add(twoEndPoints);
        }

        return DiametricEndPoints;
    }

}
