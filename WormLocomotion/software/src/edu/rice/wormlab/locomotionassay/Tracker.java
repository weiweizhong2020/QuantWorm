/**
 * Filename: Tracker.java This class conducts actual video analysis
 */
package edu.rice.wormlab.locomotionassay;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.awt.Font;
import java.awt.TextArea;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.rank.Percentile;

/**
 * Finds the tracks of the worms.
 */
public class Tracker implements Runnable {

    /**
     * options for ParticleAnalyzer, the 'show results' one will slow down
     * processing due to printing many messages
     */
    //public int OPTIONS = ParticleAnalyzer.SHOW_RESULTS + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
    public int OPTIONS = ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
    public static final String TRACKING_RESULTS = "TrackingResults.txt";
    public static final String TRACKING_RESULTS_DETAIL = "TrackingResults_Detail.txt";
    /**
     * the measurements wanted out of ParticleAnalyzer
     */
    public int SYSTEM_MEASUREMENTS = Measurements.AREA + Measurements.MEAN + Measurements.MIN_MAX + Measurements.CENTROID;
    private File video;
    private Boolean displayFlag;
    private FrameReader frameReader;
    private ImagePlus displayImagePlus = null;
    private ImageWindow displayImageWindow = null;
    private BufferedImage lastActiveImageFrame;
    private int currentFrame = 0;
    private int validFrameCount = 0;
    private int frameCount = 0;
    private final LinkedList<LinkedList<double[]>> activeList = new LinkedList<LinkedList<double[]>>();
    private Integer drawInterval = 2;
    private static final PrintStream out = System.out;
    private NativeImgProcessing imgProc = new NativeImgProcessing();
    private double microMeterPerPixelsX;
    private double microMeterPerPixelsY;
    //Average of microMeterPerPixelsX and microMeterPerPixelsY
    private double microMeterPerPixels;
    private ScannerLog scannerLog;
    private double videoDurationInSec;
    private boolean isAnalyzeOneVideo = true;
    private boolean isReadTrackFromFile = false;
    private static JFrame frameStatusWindow = new JFrame();
    private TextArea textStatusMessage = new TextArea();
    public static DetectionCondition detectionCondition = new DetectionCondition();
    
    //The following variables are used to write frame images
    final public boolean isWriteFrameImages = false;
    final public int writeFrameImageInterval = 10;
    final public String writeFrameImageDestFolder =
            "C:/Users/lab/Documents/Locomotion examples/frameSource/";

    /**
     * Default constructor
     *
     * @param video the video file
     * @param isReadSavedTrackFile if this value is true, read track from saved
     * file
     * @param showWindowFlag flag on whether to show the window
     */
    public Tracker(File video, boolean isReadSavedTrackFile, boolean isOneVideoAnalysis) {

        this.isAnalyzeOneVideo = isOneVideoAnalysis;
        this.isReadTrackFromFile = isReadSavedTrackFile;



        if (this.isReadTrackFromFile) {
            this.video = video;
            this.displayFlag = isOneVideoAnalysis;
            this.frameReader = null;
        } else {
            this.video = video;
            this.displayFlag = isOneVideoAnalysis;

            this.createAndSetStatusWindow();
            this.updateStatusWindow("Video Processing Status",
                    "Video file:\n" + video.getAbsolutePath() + "\n\n");

            try {
                this.updateStatusWindow("Video Processing Status",
                        "Video file:\n" + video.getAbsolutePath() + "\n\n"
                        + "Preparing analysis...");

                this.frameReader = new FrameReader("file://" + video.getAbsolutePath());

            } catch (Exception ex) {
                this.updateStatusWindow(null,
                        "Video file:\n" + video.getAbsolutePath() + "\n\n"
                        + "Analysis failed: " + ex.getMessage());
                this.frameStatusWindow.dispose();
            }

        }

    }

    /**
     * Create and show Status window UI
     */
    private void createAndSetStatusWindow() {
        this.frameStatusWindow.setLocation(1, 150);
        this.frameStatusWindow.setResizable(false);

        this.textStatusMessage = new TextArea("", 15, 70, TextArea.SCROLLBARS_VERTICAL_ONLY);
        this.textStatusMessage.setLocation(20, 20);
        this.textStatusMessage.setEditable(false);
        this.textStatusMessage.setFont(new Font("TimesRoman", Font.PLAIN, 12));

        this.frameStatusWindow.getContentPane().removeAll();
        this.frameStatusWindow.getContentPane().add(this.textStatusMessage);

        this.frameStatusWindow.pack();
        this.frameStatusWindow.setVisible(true);
    }

    /**
     * Update status window UI
     */
    private void updateStatusWindow(String windowTitle,
            String statusMessage) {

        if (windowTitle != null) {
            this.frameStatusWindow.setTitle(windowTitle);
        }


        if (statusMessage != null) {
            this.textStatusMessage.setText(statusMessage);
        }

        //this.frameStatusWindow.setVisible(true);
        this.frameStatusWindow.invalidate();
    }

    /**
     * Reads precomputed tracks (from the .file file)
     *
     * @return null when things went OK; otherwise it returns an error message
     */
    @SuppressWarnings("unchecked")
    public String readPrecomputedTracks() {
        File dotFile = new File(video.getAbsoluteFile() + ".file");
        if (dotFile.exists() == false) {
            return "Unable to find the file containing precomputed tracks (" + dotFile.getAbsolutePath() + ")";
        }; // if
        frameCount = 0;
        activeList.clear();
        FileInputStream fs = null;
        ObjectInputStream oi = null;
        LinkedList<LinkedList<double[]>> list = null;
        try {
            fs = new FileInputStream(dotFile);
            oi = new ObjectInputStream(fs);
            Object listObject = oi.readObject();
            this.frameCount = oi.readInt();
            this.microMeterPerPixelsX = oi.readDouble();
            this.microMeterPerPixelsY = oi.readDouble();
            this.microMeterPerPixels = oi.readDouble();
            this.videoDurationInSec = oi.readDouble();


            list = (LinkedList<LinkedList<double[]>>) listObject;
            oi.close();
            fs.close();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            try {
                oi.close();
            } catch (IOException e) {
                e.printStackTrace();
                return e.getMessage();
            }; // try
            return ex.getMessage();
        } catch (IOException ex) {
            ex.printStackTrace();
            return ex.getMessage();
        } finally {
            this.frameStatusWindow.dispose();
        }
        for (LinkedList<double[]> each : list) {
            activeList.add(each);
        }; // for
        return null;
    }

    /**
     * Everything happens here
     */
    public void run() {

        //Load analysis parameters from video log file
        this.scannerLog = ScannerLog.readLog(video);
        if (this.scannerLog.errorList.size() > 0) {
            if (this.isAnalyzeOneVideo) {
                JOptionPane.showMessageDialog(null, "Unable to find log file in directory.",
                        "Unable to find log file in directory.",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }
        this.microMeterPerPixelsX = scannerLog.getStepsPerPixelsX();
        this.microMeterPerPixelsY = scannerLog.getStepsPerPixelsY();
        this.microMeterPerPixels = (this.microMeterPerPixelsX
                + this.microMeterPerPixelsY) / 2;
        this.videoDurationInSec = frameReader.getVideoDurationInSec();



        //long start = System.currentTimeMillis();
        activeList.clear();
        currentFrame = 0;
        validFrameCount = 0;
        frameCount = 0;
        while (true) {
            int state = trackFrame(currentFrame);
            if (state == -1) {
                break;
            }; // if

            if (isWriteFrameImages) {
                if (currentFrame % writeFrameImageInterval == 0 && currentFrame != 0) {
                    imgProc.saveImage(displayImagePlus.getBufferedImage(),
                            "gif", writeFrameImageDestFolder + File.separator
                            + "tracking" + currentFrame + ".gif");
                }
            }
            currentFrame++;
        }; // while

        frameCount = currentFrame;





        if (currentFrame != 0) {
            //out.println( currentFrame + " total frames, valid: " + validFrameCount );
            validateTracks();


            //Save the last frame
            imgProc.saveImage(displayImagePlus.getBufferedImage(),
                    "gif", video.getAbsolutePath() + ".gif");

            if (displayFlag == true) {
                if (displayImagePlus != null) {
                    displayImagePlus.updateAndRepaintWindow();
                    //displayImagePlus.setImage(
                    //        imgProc.loadImage(video.getAbsolutePath() + ".jpeg"));
                }
            }

            writeTracks();


            if (displayFlag == true) {

                double[] allSpeedPoints = TrackAnalyzer.analyze_AllTrackLists(
                        activeList, frameCount, this.videoDurationInSec,
                        this.microMeterPerPixels);
                if (allSpeedPoints.length == 0) {
                    JOptionPane.showMessageDialog(null, "No tracks data detected in the video.",
                            "No tracks data detected in the video.",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }; // if



                if (allSpeedPoints.length == 0) {
                    JOptionPane.showMessageDialog(null, "No tracks data detected in the video (above speed threshold).",
                            "No tracks data detected in the video.",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }; // if



                double[] histogram = TrackAnalyzer.calcHistogram(allSpeedPoints,
                        detectionCondition.max_BinSize_For_Histogram,
                        detectionCondition.binSpacingSize_For_Histogram);
                TrackAnalyzer.drawHistogram(histogram, video.getName(),
                        detectionCondition.binSpacingSize_For_Histogram,
                        detectionCondition.max_BinSize_For_Histogram);



                // Do not draw this one
                double[] cdf = TrackAnalyzer.calcCDF(histogram);
                TrackAnalyzer.drawCDF(cdf, video.getName(),
                        detectionCondition.binSpacingSize_For_Histogram,
                        detectionCondition.max_BinSize_For_Histogram);




                //Creating result string
                List<String> resultsList = new ArrayList<String>();
                List<String> resultsList_DetailedInfo = new ArrayList<String>();
                resultsList.add(Tracker.get_SummaryOutput_Header(false));
                resultsList_DetailedInfo.add(Tracker.get_SummaryOutput_Detailed_Header(false));


                //Calculate speed info for all tracklist
                resultsList.add(get_SummaryOutput(
                        video.getAbsolutePath(),
                        activeList, frameCount, this.videoDurationInSec,
                        this.microMeterPerPixels));


                //Calculate individual spped info
                resultsList_DetailedInfo.add(get_SummaryOutput_Detailed(
                        video.getAbsolutePath(),
                        activeList, frameCount, this.videoDurationInSec,
                        this.microMeterPerPixels));



                String anyError = BatchTracking.writeTrackingResults(video.getParent(),
                        resultsList, TRACKING_RESULTS);
                if (anyError != null) {
                    JOptionPane.showMessageDialog(null, "Unable to write the results.\nDirectory: " + video.getParent()
                            + "\nFile: " + TRACKING_RESULTS,
                            "Unable to write the results.", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(null,
                        "Finished.\nResults were written to:\n" + video.getParent()
                        + File.separator + TRACKING_RESULTS,
                        "Finished. ", JOptionPane.INFORMATION_MESSAGE);

                BatchTracking.writeTrackingResults(video.getParent(),
                        resultsList_DetailedInfo, TRACKING_RESULTS_DETAIL);
            }
        }


        this.frameStatusWindow.dispose();
    }

    /**
     * Get head text string of summary report file
     * @param isDataLengthZero
     * @return 
     */
    public final static String get_SummaryOutput_Header(boolean isDataLengthZero) {
        if (isDataLengthZero) {
            return "0\t0\t0\t0\t0\t0\t0\t0";
        } else {
            return "file\tvideo frame count\ttrack count\tn\taverage\tstdev\t25%\t50%\t75%\r\n";
        }
    }

    /**
     * Get head text string of summary report file
     * @param isDataLengthZero
     * @return 
     */
    public final static String get_SummaryOutput_Detailed_Header(boolean isDataLengthZero) {
        if (isDataLengthZero) {
            return "0\t0\t0\t0\t0";
        } else {
            return "file\tvideo frame count\ttrack ID\tn\taverage\tstdev\r\n";
        }
    }

    //Obtain summary result as string format
    public String get_SummaryOutput(String videoAbsoluteFileName,
            LinkedList<LinkedList<double[]>> tracksList,
            int videoframeCount, Double videoDurationInSec,
            Double microMeterPerPixel) {

        double[] speedPoints = TrackAnalyzer.analyze_AllTrackLists(
                tracksList, videoframeCount,
                videoDurationInSec, microMeterPerPixel);
        Percentile percentile = new Percentile();

        String resultLine = videoAbsoluteFileName + "\t";


        if (speedPoints.length == 0) {
            resultLine = resultLine
                    + get_SummaryOutput_Header(true);
        } else {
            double averageSpeedPoints = StatUtils.mean(speedPoints);
            double varianceCutoff = StatUtils.variance(speedPoints, averageSpeedPoints);
            percentile.setData(speedPoints);
            double p25 = percentile.evaluate(25);
            double p50 = percentile.evaluate(50);
            double p75 = percentile.evaluate(75);
            resultLine = resultLine
                    + videoframeCount + "\t"
                    + tracksList.size() + "\t"
                    + speedPoints.length + "\t"
                    + Utilities.format4(averageSpeedPoints * 1000) + "\t"
                    + Utilities.format4(Math.sqrt(varianceCutoff) * 1000) + "\t"
                    + Utilities.format4(p25 * 1000) + "\t"
                    + Utilities.format4(p50 * 1000) + "\t"
                    + Utilities.format4(p75 * 1000);
        }

        return resultLine;
    }

    //Obtain detailed summary result as string format
    //Speed of each track list is calculated
    public String get_SummaryOutput_Detailed(String videoAbsoluteFileName,
            LinkedList<LinkedList<double[]>> tracksList,
            int videoframeCount, Double videoDurationInSec, Double microMeterPerPixel) {


        String resultLine = "";


        for (int i = 0; i < tracksList.size(); i++) {
            double[] speedPoints = TrackAnalyzer.analyze_OnlyTargetTrackList(
                    tracksList, videoframeCount,
                    videoDurationInSec,
                    microMeterPerPixel, i);

            Percentile percentile = new Percentile();
            resultLine = resultLine + videoAbsoluteFileName + "\t";

            if (speedPoints.length == 0) {
                resultLine = resultLine
                        + get_SummaryOutput_Detailed_Header(true);
            } else {
                double averageSpeedPoints = StatUtils.mean(speedPoints);
                double varianceCutoff = StatUtils.variance(speedPoints,
                        averageSpeedPoints);
                percentile.setData(speedPoints);
                resultLine = resultLine
                        + videoframeCount + "\t"
                        + (i + 1) + "\t"
                        + speedPoints.length + "\t"
                        + Utilities.format4(averageSpeedPoints * 1000) + "\t"
                        + Utilities.format4(Math.sqrt(varianceCutoff) * 1000) + "\r\n";
            }
        }

        return resultLine;
    }

    /**
     * get video duration time
     * @return 
     */
    public double getVideoDurationInSec() {
        return this.videoDurationInSec;
    }

    
    /**
     * get conversion factor between pixel and micrometer
     * @return 
     */
    public double getMicroMeterPerPixels() {
        return this.microMeterPerPixels;
    }

    /**
     * Gets the active-list (all tracks)
     *
     * @return the activeList
     */
    public LinkedList<LinkedList<double[]>> getActiveList() {
        return activeList;
    }

    /**
     * Gets the Track Count
     *
     * @return the TrackCount
     */
    public int getTrackCount() {
        return activeList.size();
    }

    /**
     * Get the number of valid frames counted
     *
     * @return the valid frame count
     */
    public int getValidFrameCount() {
        return validFrameCount;
    }

    /**
     * Get the number of frames counted Call this function once video play is
     * done
     *
     * @return the valid frame count
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * Get the index of the current frame
     *
     * @return the index of the current frame
     */
    public int getCurrentFrameIndex() {
        return currentFrame;
    }

    /**
     * Writes the active-list and valid-frame-count into a serialized file named
     * same as the video plus ".file"
     */
    public void writeTracks() {
        try {
            FileOutputStream fs = new FileOutputStream(video.getParent() + File.separator + video.getName() + ".file");
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(activeList);
            os.writeInt(this.frameCount);
            os.writeDouble(this.microMeterPerPixelsX);
            os.writeDouble(this.microMeterPerPixelsY);
            os.writeDouble(this.microMeterPerPixels);
            os.writeDouble(this.videoDurationInSec);
            os.close();
            fs.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }; // try
    }

    /**
     * Removes tracks that are too short
     */
    private void validateTracks() {
        for (int i = 0; i < activeList.size(); i++) {
            LinkedList<double[]> activeTrackList = activeList.get(i);
            if (activeTrackList.size() < detectionCondition.min_FrameCount_Of_ActiveTrack
                    || get_MaxBoundarySize_Of_TrackList(activeTrackList)
                    < detectionCondition.min_BoundingSize_Of_ActiveTrack) {
                activeList.remove(i);
                i--;

            } else {
                double[] X = Smoothing.getX(activeTrackList);
                double[] Y = Smoothing.getY(activeTrackList);
                double[] XSmoothed = Smoothing.doBezierSmoothing(X,
                        X.length);
                double[] YSmoothed = Smoothing.doBezierSmoothing(Y,
                        Y.length);

                double pathLength = Smoothing.get_PathLength(XSmoothed, YSmoothed);
                if (pathLength < detectionCondition.min_PathLength_Of_SmoothedTrack) {
                    activeList.remove(i);
                    i--;
                }

            }
        }


        displayImagePlus = new ImagePlus("TheLastFrame");
        displayImagePlus.setImage(lastActiveImageFrame);
        drawTracks();
    }

    /**
     * Checks whether the value is null, when it is, it outputs a message and
     * the program terminates
     *
     * @param value the value
     * @param str the name of the variable, used in displaying message
     */
    private void dieWhenNull(Integer value, String str) {
        if (value == null) {
            out.println(str + " cannot be null! leaving! Bye.");
            System.exit(1);
        }; // if
    }

    /**
     * Grabs a frame from the video; skips frames that are fully blank; applies
     * thresholding level; runs ParticleAnalyzer; when the displayFlag is set,
     * it draws the tracks with red color
     *
     * @param frame the frame number
     * @return zero when things go OK; -1 when unable to process a frame
     */
    public int trackFrame(int frame) {
        ImagePlus imagePlus = frameReader.grab(frame);


        BufferedImage imgBuf;

        if (imagePlus == null) {
            frameReader.getPlayer().stop();
            frameReader.getPlayer().close();
            return -1;
        }; // if


        if (isWriteFrameImages) {
            if (frame % writeFrameImageInterval == 0 && frame != 0) {
                imgProc.saveImage(imagePlus.getBufferedImage(),
                        "gif", writeFrameImageDestFolder + File.separator
                        + "original" + frame + ".gif");
            }
        }


        if (this.isReadTrackFromFile == false) {
            this.updateStatusWindow(null,
                    "Video file:\n" + video.getAbsolutePath() + "\n\n"
                    + "Processing Frame # " + frame + "\n\n"
                    + "Progress: " + (int) (this.frameReader.getCurrentVideoTimeInSec()
                    / this.frameReader.getVideoDurationInSec() * 100)
                    + "%");
        }



        ImagePlus grabbed = imagePlus.duplicate();
        // convert to gray-scale
        if (grabbed.getBitDepth() != 8) {
            ImageConverter icv = new ImageConverter(grabbed);
            icv.convertToGray8();
        }; // if
        ImageStatistics imageStatistics = grabbed.getStatistics();
        double mean = imageStatistics.mean;
        if (mean != 0) {
            validFrameCount++;
        }


        //Binarization
        short[][] srcPixelArray;
        srcPixelArray = imgProc.convert_Image_To_GrayShortArray(
                imagePlus.getBufferedImage());

        imgBuf = imgProc.adaptiveThresholding_Core(srcPixelArray, 15, 0.2f, 300);


        grabbed.setImage(imgBuf);
        grabbed.getProcessor().invert();


        // convert to gray-scale
        // this is needed for Particle analyzer
        if (grabbed.getBitDepth() != 8) {
            ImageConverter icv = new ImageConverter(grabbed);
            icv.convertToGray8();
        }; // if


        ResultsTable resultsTable = new ResultsTable();
        ParticleAnalyzer analyzer = new ParticleAnalyzer(OPTIONS,
                SYSTEM_MEASUREMENTS, resultsTable,
                detectionCondition.min_WormSize, detectionCondition.max_WormSize, 0.0, 1.0);
        analyzer.analyze(grabbed);

        updateActiveTracks(resultsTable);

        lastActiveImageFrame = imagePlus.getBufferedImage();

        if (displayFlag == true) {
            if (displayImagePlus == null) {
                displayImagePlus = imagePlus;
                displayImageWindow = new ImageWindow(displayImagePlus);
                displayImageWindow.setTitle(video.getName());
                displayImagePlus.getProcessor().setColor(Color.RED);

            } else if (frame % drawInterval == 0) {
                //displayImagePlus.setImage(imagePlus);
                displayImagePlus.setImage(imagePlus);
                drawTracks();
                displayImageWindow.setImage(displayImagePlus);
                displayImageWindow.invalidate();
            }; // if
        }; // if

        resultsTable.reset();
        return 0;
    }

    /**
     * Generates drawings of tracks onto the display-image-plus object
     */
    public void drawTracks() {
        if (activeList == null) {
            return;
        }; // if

        
        for (int i = 0; i < activeList.size(); i++) {
            LinkedList<double[]> activeTrackList = activeList.get(i);
            double[] lastPoint = activeTrackList.get(activeTrackList.size() - 1);

            //Do not delete this part
            displayImagePlus.getProcessor().setColor(Color.MAGENTA);
            displayImagePlus.getProcessor().fillOval((int) lastPoint[0] - 2, (int) lastPoint[1] - 2,
                    4, 4);

            displayImagePlus.getProcessor().drawString(Integer.toString(i + 1),
                    (int) lastPoint[0] + 3, (int) lastPoint[1] + 4);


            if (lastPoint[ 3] == 0) {

                displayImagePlus.getProcessor().setColor(Color.BLUE);
                for (int j = 0; j < activeTrackList.size(); j++) {
                    double[] point = activeTrackList.get(j);
                    double X = point[ 0];
                    double Y = point[ 1];
                    displayImagePlus.getProcessor().drawDot((int) Math.ceil(X), (int) Math.ceil(Y));
                }; // for

            } else {

                displayImagePlus.getProcessor().setColor(Color.RED);
                for (int j = 0; j < activeTrackList.size(); j++) {
                    double[] point = activeTrackList.get(j);
                    double X = point[ 0];
                    double Y = point[ 1];
                    displayImagePlus.getProcessor().drawDot((int) Math.ceil(X), (int) Math.ceil(Y));
                }; // for
            }
        }; // for


    }

    /**
     * Find minimum boundary size of given TrackList
     */
    private int get_MaxBoundarySize_Of_TrackList(LinkedList<double[]> cur_ActiveTrackList) {
        if (cur_ActiveTrackList == null) {
            return 0;
        }
        if (cur_ActiveTrackList.size() == 0) {
            return 0;
        }


        double[] curPoint = cur_ActiveTrackList.get(0);
        double minX = curPoint[0];
        double maxX = curPoint[0];
        double minY = curPoint[1];
        double maxY = curPoint[1];

        for (int i = 0; i < cur_ActiveTrackList.size(); i++) {
            curPoint = cur_ActiveTrackList.get(i);
            if (curPoint[0] < minX) {
                minX = curPoint[0];
            }
            if (curPoint[0] > maxX) {
                maxX = curPoint[0];
            }
            if (curPoint[1] < minY) {
                minY = curPoint[1];
            }
            if (curPoint[1] > maxY) {
                maxY = curPoint[1];
            }
        }

        if ((maxX - minX) > (maxY - minY)) {
            return (int) (maxX - minX);
        } else {
            return (int) (maxY - minY);
        }
    }

    public void updateActiveTracks(ResultsTable resultsTable) {


        for (int i = 0; i < activeList.size(); i++) {
            LinkedList<double[]> activeTrackList = activeList.get(i);

            double[] lastPoint = activeTrackList.get(activeTrackList.size() - 1); //get the last point
            double lastX = lastPoint[ 0];
            double lastY = lastPoint[ 1];
            double lastSize = lastPoint[ 2];
            double active = lastPoint[ 3];

            //Check if current TrackList is already completed
            if (active == 0) {
                continue;
            }; // if

            double minShift = 100000;
            int minRow = -1;

            //examine all of the tracks
            for (int row = 0; row < resultsTable.getCounter(); row++) {
                double X = resultsTable.getValue("X", row);
                double Y = resultsTable.getValue("Y", row);
                double shift = Math.sqrt((lastX - X) * (lastX - X) + (lastY - Y) * (lastY - Y));
                if (shift < minShift) {
                    minShift = shift;
                    minRow = row;
                }; // if
            }; // for



            // check if the same object is found      
            if (minRow == -1 || minShift > detectionCondition.min_Distance_Of_SameWorm) {
                if (activeTrackList.size() < detectionCondition.min_FrameCount_Of_ActiveTrack
                        || get_MaxBoundarySize_Of_TrackList(activeTrackList)
                        < detectionCondition.min_BoundingSize_Of_ActiveTrack) {
                    // delete dead tracks that are no longer active & too short
                    activeList.remove(i);
                    i--;
                    continue;
                } else {
                    // mark tracks as completed track
                    lastPoint[ 3] = 0;
                    activeTrackList.set(activeTrackList.size() - 1, lastPoint);
                    activeList.set(i, activeTrackList);
                    continue;
                }
            }


            // check if the same object is found by comparing maxSizeChange
            if (Math.abs((resultsTable.getValue("Area", minRow) - lastSize) / lastSize * 100)
                    > detectionCondition.max_PercentChange_In_WormSize) {
                if (activeTrackList.size() < detectionCondition.min_FrameCount_Of_ActiveTrack
                        || get_MaxBoundarySize_Of_TrackList(activeTrackList)
                        < detectionCondition.min_BoundingSize_Of_ActiveTrack) {
                    // delete dead tracks that are no longer active & too short
                    activeList.remove(i);
                    i--;
                    continue;
                } else {
                    // mark tracks as completed track
                    lastPoint[ 3] = 0;
                    activeTrackList.set(activeTrackList.size() - 1, lastPoint);
                    activeList.set(i, activeTrackList);
                    continue;
                }
            }


            // update-able           
            double[] newPoint = new double[4];
            newPoint[ 0] = resultsTable.getValue("X", minRow);
            newPoint[ 1] = resultsTable.getValue("Y", minRow);
            newPoint[ 2] = resultsTable.getValue("Area", minRow);
            newPoint[ 3] = 1;
            activeTrackList.add(newPoint);
            activeList.set(i, activeTrackList);
            resultsTable.deleteRow(minRow);
        }


        if (resultsTable.getCounter() > 0) {
            // create new tracks for remaining rows in result
            for (int k = 0; k < resultsTable.getCounter(); k++) {
                double[] newPoint = new double[4];
                newPoint[ 0] = resultsTable.getValue("X", k);
                newPoint[ 1] = resultsTable.getValue("Y", k);
                newPoint[ 2] = resultsTable.getValue("Area", k);
                newPoint[ 3] = 1;
                LinkedList<double[]> newTrackList = new LinkedList<double[]>();
                newTrackList.add(newPoint);
                activeList.add(newTrackList);
            }; // for
        }; // if
    }
}
