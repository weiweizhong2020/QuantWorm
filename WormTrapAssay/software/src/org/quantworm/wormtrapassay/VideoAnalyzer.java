/**
 * Filename: VideoAnalyzer.java This class conducts video analysis
 */
package org.quantworm.wormtrapassay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Analyze video
 */
public class VideoAnalyzer {

    //File name
    public static final String CHEMO_VIDEO_RESULT_FILE = ".WormTrapAssay_Result.txt";
    public static final String INITIAL_WORM_COUNT_INFO_FILE = "InitialWormCountInfo.txt";

    //Interface
    private File video;
    private FrameReader frameReader;
    private NativeImgProcessing imgProc = new NativeImgProcessing();
    private JFrame frameImageDisplayer = new JFrame("Image processing");
    private JImagePanel imgPanel;
    private Graphics graphics;

    //Analysis parameters
    private final long nanosecToSec = 1000000000L;
    private final long frameInterval_InCountingProcess = 2 * nanosecToSec;  //2 sec (unit in nano seconds)
    private final long startTimeInNanoSec_InWormAreaEstimation = 30 * nanosecToSec;   //60 sec
    private final long frameInterval_InWormAreaEstimation = 10 * nanosecToSec;     //10 sec

    //Video info
    private double microMeterPerPixels;
    private double videoDurationInSec;
    private int currentFrame = 0;
    private int frameCount = 0;
    private long curTimeInNanoSec;
    private int frameAnalyzedCount;
    private int frameAnalyzedCurIndex;
    private int imageWidthUpperBound = 639;   //Default width
    private int imageHeightUpperBound = 479;  //Default height   

    //Auto alignment info
    private double[] axisAndAngleArray = new double[0];

    //Worm analysis info
    private long[] wormCountTime;
    private float[] wormCountInRegionA;
    private float[] wormCountInRegionB;
    private String controlSide;

    //Mask image
    private BufferedImage imgMask;
    private short[][] arrayMask;

    //Source and processed images
    private BufferedImage curFrame;
    private short[][] arrayCurFrame;
    private BufferedImage curFrameAligned;
    private short[][] arrayCurFrameAligned;
    private BufferedImage curFrameAlignedBinarized;
    private short[][] arraycurFrameAlignedBinarized;
    private BufferedImage curOverlayedImage;
    private short[][][] arrayCurOverlayedImage;
    private BufferedImage regionLabeledImage;

    private String trackerName;

    /**
     * Default constructor
     *
     * @param video
     */
    public VideoAnalyzer(File video) {
        this.video = video;

        this.createAndSetStatusWindow();
        try {
            this.frameReader = new FrameReader("file://" + video.getAbsolutePath());
        } catch (Exception ex) {
        }

    }

    //Create and show Status window UI
    private void createAndSetStatusWindow() {

        //Set Image Viewer Window
        this.frameImageDisplayer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frameImageDisplayer.setBounds(150, 150, 660, 530);
        this.frameImageDisplayer.setLocation(300, 180);
        this.frameImageDisplayer.setMinimumSize(new Dimension(660, 530));
        imgPanel = new JImagePanel(null, 0, 0);
        frameImageDisplayer.add(imgPanel);
        this.frameImageDisplayer.pack();

        //Display 'Preparing..'
        this.frameImageDisplayer.setVisible(true);
        graphics = imgPanel.getGraphics();
        displayImage_Preparing();
    }

    /**
     * Everything happens here
     *
     * @return
     */
    public String run() {

        //Read initial worm count info
        File initialWormCountInfoFile;
        initialWormCountInfoFile = new File(
                video.getParent() + File.separator
                + "InitialWormCountInfo.txt");
        if (initialWormCountInfoFile.exists() == false) {
            cleanup();
            return "InitialWormCountInfo.txt file not found";
        }
        String[] wormCountInfo = get_InitialWormCountInfo(
                initialWormCountInfoFile, video);
        if (wormCountInfo == null) {
            cleanup();
            return "video file was not found in InitialWormCountInfo.txt";
        }
        int wormCountInA = Integer.parseInt(wormCountInfo[0]);
        int wormCountInB = Integer.parseInt(wormCountInfo[1]);
        controlSide = wormCountInfo[2];
        trackerName = wormCountInfo[3];

        //Get tracker used to create video file
        if (trackerName == null) {
            cleanup();
            return "Can not find matched tracker";
        }

        //Read mask image
        try {
            File maskFile = new File("MASK OF " + trackerName
                    + ".gif");
            if (maskFile.exists() == false) {
                cleanup();
                return maskFile.getName() + " not found";
            }
            imgMask = ImageIO.read(maskFile);
        } catch (Exception e) {
            cleanup();
            return "Error in loading mask image file";
        }
        arrayMask = imgProc.convert_Image_To_GrayShortArray(imgMask);

        //Initialize variables
        this.videoDurationInSec = frameReader.getVideoDurationInSec();
        frameAnalyzedCount = (int) (this.videoDurationInSec * nanosecToSec / frameInterval_InCountingProcess);

        int[] pixelCountInRegionA = new int[frameAnalyzedCount];
        int[] pixelCountInRegionB = new int[frameAnalyzedCount];
        wormCountTime = new long[frameAnalyzedCount];
        wormCountInRegionA = new float[frameAnalyzedCount];
        wormCountInRegionB = new float[frameAnalyzedCount];
        int avgWormAreaInPexelInA = 120;
        int avgWormAreaInPexelInB = 120;

        //Find worm area
        int avgWormArea_ByEstimiation = get_WormArea();

        //Process every frame
        frameAnalyzedCurIndex = 0;
        for (curTimeInNanoSec = 0; curTimeInNanoSec < this.videoDurationInSec * nanosecToSec;
                curTimeInNanoSec = curTimeInNanoSec + frameInterval_InCountingProcess) {

            //Extract image frame
            curFrame = frameReader.grabImage_At_TimeInNanoSec(curTimeInNanoSec);
            if (curFrame == null) {
                continue;
            }

            arrayCurFrame = imgProc.convert_Image_To_GrayShortArray(curFrame);

            if (imgProc.isBlankImage(arrayCurFrame, 10) == false) {

                //At the first frame, find center and angle of the guide lines
                if (frameAnalyzedCurIndex == 0) {
                    axisAndAngleArray = imgProc.calculate_CenterAndAngle(curFrame);

                }

                //Apply auto-alignment
                curFrameAligned = imgProc.alignImage(curFrame, axisAndAngleArray);
                arrayCurFrameAligned = imgProc.convert_Image_To_GrayShortArray(curFrameAligned);

                //Apply adaptive thresholding
                curFrameAlignedBinarized
                        = imgProc.adaptiveThresholding_Core(arrayCurFrameAligned, 15, 0.3f, 300);
                arraycurFrameAlignedBinarized
                        = imgProc.multiArrayCopy(imgProc.AdaptiveThreshold_BWPixelArray);

                //Define image dimension
                imageWidthUpperBound = arrayCurFrameAligned.length - 1;
                imageHeightUpperBound = arrayCurFrameAligned[0].length - 1;

                arrayCurOverlayedImage
                        = new short[imageWidthUpperBound + 1][imageHeightUpperBound + 1][3];

                //Count pixels and create overlay image
                for (int y = 0; y <= imageHeightUpperBound; y++) {
                    for (int x = 0; x <= imageWidthUpperBound; x++) {
                        if (arrayMask[x][y] != 0) {

                            if (arrayMask[x][y] == 100) {
                                arrayCurOverlayedImage[x][y][0]
                                        = (short) Math.max(arrayCurFrameAligned[x][y] - 50, 0);
                                arrayCurOverlayedImage[x][y][1] = arrayCurFrameAligned[x][y];
                                arrayCurOverlayedImage[x][y][2] = arrayCurFrameAligned[x][y];
                            } else {
                                arrayCurOverlayedImage[x][y][0] = arrayCurFrameAligned[x][y];
                                arrayCurOverlayedImage[x][y][1]
                                        = (short) Math.max(arrayCurFrameAligned[x][y] - 50, 0);
                                arrayCurOverlayedImage[x][y][2] = arrayCurFrameAligned[x][y];
                            }
                            if (arraycurFrameAlignedBinarized[x][y] == 255) {
                                if (arrayMask[x][y] == 100) {
                                    pixelCountInRegionA[frameAnalyzedCurIndex]++;
                                } else {
                                    pixelCountInRegionB[frameAnalyzedCurIndex]++;
                                }

                                arrayCurOverlayedImage[x][y][0] = 255;
                                arrayCurOverlayedImage[x][y][1] = 0;
                                arrayCurOverlayedImage[x][y][2] = 0;
                            }
                        } else {
                            arrayCurOverlayedImage[x][y][0] = arrayCurFrameAligned[x][y];
                            arrayCurOverlayedImage[x][y][1] = arrayCurFrameAligned[x][y];
                            arrayCurOverlayedImage[x][y][2] = arrayCurFrameAligned[x][y];
                        }

                    }
                }

                //If the first frame, then calculate worm area
                if (frameAnalyzedCurIndex == 0) {
                    if (wormCountInA > 0) {
                        avgWormAreaInPexelInA
                                = Math.round(pixelCountInRegionA[frameAnalyzedCurIndex] / wormCountInA);
                    } else {
                        avgWormAreaInPexelInA = 1000000000;     //meaning large number
                    }

                    if (wormCountInA > 0) {
                        avgWormAreaInPexelInB
                                = Math.round(pixelCountInRegionB[frameAnalyzedCurIndex] / wormCountInB);
                    } else {
                        avgWormAreaInPexelInB = 1000000000;     //meaning large number
                    }

                }

                //Apply calculated worm area
                if (curTimeInNanoSec >= startTimeInNanoSec_InWormAreaEstimation) {
                    if (wormCountInA > 0) {
                        avgWormAreaInPexelInA = avgWormArea_ByEstimiation;
                    } else {
                        avgWormAreaInPexelInA = 1000000000;     //meaning large number
                    }
                    if (wormCountInB > 0) {
                        avgWormAreaInPexelInB = avgWormArea_ByEstimiation;
                    } else {
                        avgWormAreaInPexelInB = 1000000000;     //meaning large number
                    }
                }

                //Calcualte worm count
                wormCountTime[frameAnalyzedCurIndex] = curTimeInNanoSec;
                wormCountInRegionA[frameAnalyzedCurIndex]
                        = (float) pixelCountInRegionA[frameAnalyzedCurIndex]
                        / avgWormAreaInPexelInA;
                wormCountInRegionB[frameAnalyzedCurIndex]
                        = (float) pixelCountInRegionB[frameAnalyzedCurIndex]
                        / avgWormAreaInPexelInB;

                //Correction in worm count
                if (curTimeInNanoSec >= startTimeInNanoSec_InWormAreaEstimation) {
                    if (wormCountInRegionA[frameAnalyzedCurIndex] > 1) {
                        wormCountInRegionA[frameAnalyzedCurIndex]
                                = (float) (wormCountInRegionA[frameAnalyzedCurIndex] + 0.2);
                    }

                    if (wormCountInRegionB[frameAnalyzedCurIndex] > 1) {
                        wormCountInRegionB[frameAnalyzedCurIndex]
                                = (float) (wormCountInRegionB[frameAnalyzedCurIndex] + 0.2);
                    }
                }

                //Adjust worm count
                if (curTimeInNanoSec < startTimeInNanoSec_InWormAreaEstimation) {
                    if (wormCountInRegionA[frameAnalyzedCurIndex] > wormCountInA * 1.1) {
                        wormCountInRegionA[frameAnalyzedCurIndex] = (float) (wormCountInA * 1.1);
                    }
                    if (wormCountInRegionB[frameAnalyzedCurIndex] > wormCountInB * 1.1) {
                        wormCountInRegionB[frameAnalyzedCurIndex] = (float) (wormCountInB * 1.1);
                    }

                }

                //Display image
                curOverlayedImage = imgProc.convert_RGBShortArray_To_Image(arrayCurOverlayedImage);
                displayImage_InCountingProcess(curOverlayedImage);

                frameAnalyzedCurIndex++;
                if (frameAnalyzedCurIndex >= frameAnalyzedCount) {
                    break;
                }
            }

        }

        //Write result
        writeVideoResult(this.video.getAbsoluteFile() + CHEMO_VIDEO_RESULT_FILE);

        //Cleanup memeory
        cleanup();

        //Finish
        return null;
    }

    /**
     * Calculate average worm area in the pixel unit
     *
     * @return
     */
    public int get_WormArea() {

        List<Integer> areaArray = new ArrayList<Integer>();

        //Process every frame
        frameAnalyzedCurIndex = 0;
        for (curTimeInNanoSec = startTimeInNanoSec_InWormAreaEstimation;
                curTimeInNanoSec < this.videoDurationInSec * nanosecToSec;
                curTimeInNanoSec = curTimeInNanoSec + frameInterval_InWormAreaEstimation) {

            curFrame = frameReader.grabImage_At_TimeInNanoSec(curTimeInNanoSec);
            if (curFrame == null) {
                continue;
            }

            arrayCurFrame = imgProc.convert_Image_To_GrayShortArray(curFrame);

            //Check if image frame is empty
            if (imgProc.isBlankImage(arrayCurFrame, 10) == false) {

                if (frameAnalyzedCurIndex == 0) {
                    axisAndAngleArray = imgProc.calculate_CenterAndAngle(curFrame);
                    this.frameImageDisplayer.setVisible(true);
                }

                //Conduct auto-alignment
                curFrameAligned = imgProc.alignImage(curFrame, axisAndAngleArray);
                arrayCurFrameAligned = imgProc.convert_Image_To_GrayShortArray(curFrameAligned);

                //Define image dimension
                imageWidthUpperBound = arrayCurFrameAligned.length - 1;
                imageHeightUpperBound = arrayCurFrameAligned[0].length - 1;

                //Conduct adaptive binarization
                curFrameAlignedBinarized
                        = imgProc.adaptiveThresholding_Core(arrayCurFrameAligned, 15, 0.3f, 300);
                arraycurFrameAlignedBinarized
                        = imgProc.multiArrayCopy(imgProc.AdaptiveThreshold_BWPixelArray);

                //Delete all background except circles
                for (int y = 0; y <= imageHeightUpperBound; y++) {
                    for (int x = 0; x <= imageWidthUpperBound; x++) {
                        if (arrayMask[x][y] == 0) {
                            arraycurFrameAlignedBinarized[x][y] = 0;
                        }

                    }
                }

                //Conduct region labeling and blob analysis
                short[][] IDMap;
                short[][] ColorTable;
                float[][] blobStat;
                regionLabeledImage = imgProc.regionExtract_RasterScanning(arraycurFrameAlignedBinarized, 0);

                IDMap = imgProc.RegionLabeling_LabelIDMap;
                ColorTable = imgProc.RegionLabeling_ColorTable;

                blobStat = imgProc.regionExtract_BasicAnalysis(IDMap,
                        ColorTable);

                //Find rings
                for (int curBlob = 1; curBlob < blobStat.length; curBlob++) {
                    //blobStat[][0]: center X
                    //blobStat[][1]: center Y
                    //blobStat[][2]: pixel count
                    //blobStat[][3]: left X
                    //blobStat[][4]: right X
                    //blobStat[][5]: top Y
                    //blobStat[][6]: bottom Y
                    //blobStat[][7]: average radius
                    //blobStat[][8]: validity
                    //               if validity=0, OK,   if validity=1, invalid)

                    //Add valid worm size
                    if (blobStat[curBlob][2] > 70
                            && blobStat[curBlob][2] < 250) {
                        areaArray.add((int) blobStat[curBlob][2]);
                        blobStat[curBlob][8] = 0;
                    } else {
                        blobStat[curBlob][8] = 1;
                    }
                }

                arrayCurOverlayedImage
                        = new short[imageWidthUpperBound + 1][imageHeightUpperBound + 1][3];

                //Create display image
                for (int y = 0; y <= imageHeightUpperBound; y++) {
                    for (int x = 0; x <= imageWidthUpperBound; x++) {
                        if (arrayMask[x][y] != 0) {
                            if (arrayMask[x][y] == 100) {
                                arrayCurOverlayedImage[x][y][0]
                                        = (short) Math.max(arrayCurFrameAligned[x][y] - 50, 0);
                                arrayCurOverlayedImage[x][y][1] = arrayCurFrameAligned[x][y];
                                arrayCurOverlayedImage[x][y][2] = arrayCurFrameAligned[x][y];
                                if (IDMap[x][y] != 0 && blobStat[IDMap[x][y]][8] == 0) {
                                    arrayCurOverlayedImage[x][y][0] = 255;
                                    arrayCurOverlayedImage[x][y][1] = 0;
                                    arrayCurOverlayedImage[x][y][2] = 0;
                                }
                            } else {
                                arrayCurOverlayedImage[x][y][0] = arrayCurFrameAligned[x][y];
                                arrayCurOverlayedImage[x][y][1]
                                        = (short) Math.max(arrayCurFrameAligned[x][y] - 50, 0);
                                arrayCurOverlayedImage[x][y][2] = arrayCurFrameAligned[x][y];
                                if (IDMap[x][y] != 0 && blobStat[IDMap[x][y]][8] == 0) {
                                    arrayCurOverlayedImage[x][y][0] = 255;
                                    arrayCurOverlayedImage[x][y][1] = 0;
                                    arrayCurOverlayedImage[x][y][2] = 0;
                                }
                            }

                        } else {
                            arrayCurOverlayedImage[x][y][0] = arrayCurFrameAligned[x][y];
                            arrayCurOverlayedImage[x][y][1] = arrayCurFrameAligned[x][y];
                            arrayCurOverlayedImage[x][y][2] = arrayCurFrameAligned[x][y];
                        }

                    }
                }

                //Display image                 
                curOverlayedImage = imgProc.convert_RGBShortArray_To_Image(arrayCurOverlayedImage);
                displayImage_InWormAreaEstimation(curOverlayedImage);

                frameAnalyzedCurIndex++;

                if (frameAnalyzedCurIndex >= frameAnalyzedCount) {
                    break;
                }
            }

        }

        //Check if enough numbers of valid single worms is found
        if (areaArray.size() < 5) {
            return 120;     //return default value
        }

        int[] intArray = listToArray(areaArray);
        int median = (int) (calculate_Median(intArray));
        return median;

    }

    /**
     * Obtain initial worm count info
     *
     * @param initialWormCountInfoFile
     * @param videoFile
     * @return String[3] String[0]: worm count in A String[1]: worm count in B
     * String[2]: control (Left or Right) String[3]: tracker name (like Fur,
     * Ish, RUS, RUB)
     */
    public String[] get_InitialWormCountInfo(File initialWormCountInfoFile,
            File videoFile) {

        List<String> srcLines
                = Utilities.getLinesFromFile(initialWormCountInfoFile);
        String[] srcLineStr = srcLines.toArray(new String[srcLines.size()]);

        String videoFileWithoutExt
                = Utilities.get_FileNameWithoutExtension(
                        videoFile.getName());

        if (srcLineStr[0].toLowerCase().startsWith("file\tleft\tright\tcontrol\ttracker") == false) {
            return null;
        }

        for (int i = 1; i < srcLineStr.length; i++) {

            String[] items = srcLineStr[i].split("\t");

            if (items.length == 5) {
                if (videoFileWithoutExt.toLowerCase().equals(items[0].toLowerCase())) {

                    String[] numberStr = new String[4];
                    numberStr[0] = items[1];
                    numberStr[1] = items[2];
                    numberStr[2] = items[3];
                    numberStr[3] = items[4];
                    return numberStr;
                }
            }
        }

        return null;
    }

    /**
     * Compute normalized area
     *
     * @param timeArray
     * @param wormCountArray
     * @return
     */
    public float calculate_NormalizedArea(long[] timeArray, float[] wormCountArray) {
        float accu_NormalizedArea = 0;

        for (int i = 1; i < frameAnalyzedCount; i++) {
            if (i != 0 && timeArray[i] == 0) {
                break;
            }

            accu_NormalizedArea = accu_NormalizedArea
                    + ((float) (timeArray[i] - timeArray[i - 1]) / nanosecToSec)
                    * wormCountArray[i - 1];

        }

        accu_NormalizedArea = accu_NormalizedArea / wormCountArray[0];

        return accu_NormalizedArea;
    }

    /**
     * Compute time of worm's escape (sec)
     *
     * @param timeArray
     * @param wormCountArray
     * @return
     */
    public String find_EscapeTime(long[] timeArray, float[] wormCountArray) {

        for (int i = 1; i < frameAnalyzedCount; i++) {
            if (i != 0 && timeArray[i] == 0) {
                break;
            }

            if (wormCountArray[0] - wormCountArray[i] > 1) {
                return String.valueOf((long) timeArray[i] / nanosecToSec);
            }

        }

        return "Worms didn't escape the circle";
    }

    /**
     * Write video result file
     *
     * @param absoluteFileName
     */
    public void writeVideoResult(String absoluteFileName) {
        float normalizedAreaA = calculate_NormalizedArea(wormCountTime, wormCountInRegionA);
        float normalizedAreaB = calculate_NormalizedArea(wormCountTime, wormCountInRegionB);
        String escapeTimeAreaA = find_EscapeTime(wormCountTime, wormCountInRegionA);
        String escapeTimeAreaB = find_EscapeTime(wormCountTime, wormCountInRegionB);

        //Swap left and right numbers 
        if (controlSide.toLowerCase().startsWith("right")) {
            float temp = normalizedAreaA;
            normalizedAreaA = normalizedAreaB;
            normalizedAreaB = temp;

            String tempStr = escapeTimeAreaA;
            escapeTimeAreaA = escapeTimeAreaB;
            escapeTimeAreaB = tempStr;
        }

        List<String> list = new ArrayList<String>();

        //Print headline
        list.add("#Avg trapped time in Control (sec):\t"
                + String.format("%.5f", normalizedAreaA));
        list.add("#Avg trapped time in Experimental (sec):\t"
                + String.format("%.5f", normalizedAreaB));
        list.add("#Escape time in Control (sec):\t" + escapeTimeAreaA);
        list.add("#Escape time in Experimental (sec):\t" + escapeTimeAreaB);
        list.add("#Time (sec)\tWorm Count in Control\tWorm Count in Experimental");

        //Print worm count at each time
        for (int i = 0; i < frameAnalyzedCount; i++) {
            if (i != 0 && wormCountTime[i] == 0) {
                break;
            }

            if (controlSide.toLowerCase().startsWith("left")) {
                list.add(String.valueOf((long) wormCountTime[i] / nanosecToSec) + "\t"
                        + String.format("%.3f", wormCountInRegionA[i]) + "\t"
                        + String.format("%.3f", wormCountInRegionB[i]));
            } else {
                list.add(String.valueOf((long) wormCountTime[i] / nanosecToSec) + "\t"
                        + String.format("%.3f", wormCountInRegionB[i]) + "\t"
                        + String.format("%.3f", wormCountInRegionA[i]));
            }
        }

        Utilities.writeResults(absoluteFileName, list);
    }

    /**
     * Get video duration time
     *
     * @return
     */
    public double getVideoDurationInSec() {
        return this.videoDurationInSec;
    }

    /**
     * Get conversion factor between pixel and micrometer
     *
     * @return
     */
    public double getMicroMeterPerPixels() {
        return this.microMeterPerPixels;
    }

    /**
     * Get the number of frames counted
     *
     * @return
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * Get the index of the current frame
     *
     * @return
     */
    public int getCurrentFrameIndex() {
        return currentFrame;
    }

    /**
     * Initialize image panel UI
     */
    public class JImagePanel extends JPanel {

        private final BufferedImage image;
        int x, y;

        public JImagePanel(BufferedImage image, int x, int y) {
            super();
            this.image = image;
            this.x = x;
            this.y = y;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, x, y, null);
        }
    }

    /**
     * Display 'Preparing...'
     */
    public void displayImage_Preparing() {
        graphics.setFont(new Font("Arial", Font.BOLD, 18));
        graphics.setColor(Color.white);
        graphics.fillRect(0, 0, 640, 480);

        displayImage_Title("Opening ");
    }

    /**
     * Draw title
     *
     * @param prefixStr
     */
    public void displayImage_Title(String prefixStr) {
        String title = video.getAbsoluteFile().toString();
        if (title.length() > 60) {
            title = "..." + Utilities.lastSubString(title, 60);
        }
        graphics.setFont(new Font("Arial", Font.BOLD, 14));
        imgProc.draw_OutlinedText(graphics,
                prefixStr + title, 10, 20, Color.white, Color.blue);
    }

    /**
     * Display current image
     *
     * @param srcImage
     */
    public void displayImage_InWormAreaEstimation(BufferedImage srcImage) {
        graphics.drawImage(curOverlayedImage, 0, 0, null);

        displayImage_Title("Processing ");

        //Draw message
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        imgProc.draw_OutlinedText(graphics,
                "Determining worm size ["
                + nanosecToTimeStamp(curTimeInNanoSec) + "]",
                10, 460, Color.white, Color.blue);

        //Draw Control and Experimental
        graphics.setFont(new Font("Arial", Font.BOLD, 18));
        if (controlSide.toLowerCase().startsWith("left")) {
            imgProc.draw_OutlinedText(graphics, "Control",
                    130, 80, Color.black, Color.white);
            imgProc.draw_OutlinedText(graphics, "Experimental",
                    410, 80, Color.black, Color.white);
        } else {
            imgProc.draw_OutlinedText(graphics, "Control",
                    430, 80, Color.black, Color.white);
            imgProc.draw_OutlinedText(graphics, "Experimental",
                    110, 80, Color.black, Color.white);
        }

        //Draw progress bar
        graphics.setColor(Color.blue);
        graphics.fillRect(0, 474,
                (int) ((curTimeInNanoSec / (this.videoDurationInSec * nanosecToSec) * 640)),
                5);

    }

    /**
     * Display current image
     *
     * @param srcImage
     */
    public void displayImage_InCountingProcess(BufferedImage srcImage) {
        graphics.drawImage(srcImage, 0, 0, null);

        displayImage_Title("Processing ");

        //Print worm count
        graphics.setFont(new Font("Arial", Font.BOLD, 18));
        imgProc.draw_OutlinedText(graphics,
                "n = " + String.format("%.3f",
                        wormCountInRegionA[frameAnalyzedCurIndex]),
                130, 420, Color.black, Color.white);
        imgProc.draw_OutlinedText(graphics,
                "n = " + String.format("%.3f",
                        wormCountInRegionB[frameAnalyzedCurIndex]),
                430, 420, Color.black, Color.white);

        //Draw Control and Experimental
        graphics.setFont(new Font("Arial", Font.BOLD, 18));
        if (controlSide.toLowerCase().startsWith("left")) {
            imgProc.draw_OutlinedText(graphics, "Control",
                    130, 80, Color.black, Color.white);
            imgProc.draw_OutlinedText(graphics, "Experimental",
                    410, 80, Color.black, Color.white);
        } else {
            imgProc.draw_OutlinedText(graphics, "Control",
                    430, 80, Color.black, Color.white);
            imgProc.draw_OutlinedText(graphics, "Experimental",
                    110, 80, Color.black, Color.white);
        }

        //Draw message
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        imgProc.draw_OutlinedText(graphics,
                "Counting worms ["
                + nanosecToTimeStamp(curTimeInNanoSec) + "]",
                10, 460, Color.white, Color.blue);

        //Draw progress bar        
        graphics.setColor(Color.blue);
        graphics.fillRect(0, 474,
                (int) ((curTimeInNanoSec / (this.videoDurationInSec * nanosecToSec) * 640)),
                5);
    }

    /**
     * Convert nano seconds to time stamp of mm:ss
     *
     * @param nanosec
     * @return
     */
    public String nanosecToTimeStamp(long nanosec) {
        long millis = nanosec / nanosecToSec * 1000;
        SimpleDateFormat df = new SimpleDateFormat("mm:ss");
        String time = df.format(millis);
        return time;
    }

    /**
     * Compute median
     *
     * @param SrcNumericArray
     * @return
     */
    public static double calculate_Median(int[] SrcNumericArray) {
        int[] NumericArray = SrcNumericArray.clone();
        Arrays.sort(NumericArray);

        int middle = NumericArray.length / 2;

        if (NumericArray.length % 2 == 1) {
            return NumericArray[middle];
        } else {
            return (NumericArray[middle - 1] + NumericArray[middle]) / 2.0;
        }
    }

    /**
     * Convert list array to integer array
     *
     * @param list
     * @return
     */
    public int[] listToArray(List list) {
        int[] ret = new int[list.size()];
        Iterator<Integer> iter = list.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            ret[i] = iter.next();
        }
        return ret;
    }

    /**
     * Collect memory
     */
    public void cleanup() {
        this.frameImageDisplayer.setVisible(false);
        video = null;
        frameReader = null;
        imgProc = null;
        graphics.dispose();
        graphics = null;
        imgPanel = null;
        frameImageDisplayer.removeAll();
        frameImageDisplayer.dispose();

        imgMask = null;
        arrayMask = null;
        curFrame = null;
        arrayCurFrame = null;
        curFrameAligned = null;
        arrayCurFrameAligned = null;
        curFrameAlignedBinarized = null;
        arraycurFrameAlignedBinarized = null;
        curOverlayedImage = null;
        arrayCurOverlayedImage = null;
        regionLabeledImage = null;
    }
}
