/*
 * Mask Creator
 */
package org.quantworm.wormtrapassay;

import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.JOptionPane;

public class MaskCreator {

    private NativeImgProcessing imgProc = new NativeImgProcessing();
    private FrameReader frameReader;
    private File video;
    private double videoDurationInSec;
    final private long frameAnalysisInterval = 500000000;  //0.5 sec (unit in nano seconds)
    private int frameAnalyzedCount;
    private Frame frame;

    public MaskCreator(Frame frame, File video) {
        this.video = video;
        this.frame = frame;

        try {
            this.frameReader = new FrameReader("file://" + video.getAbsolutePath());
            this.videoDurationInSec = frameReader.getVideoDurationInSec();
            frameAnalyzedCount = (int) (this.videoDurationInSec * 1000000000 / frameAnalysisInterval);

        } catch (Exception ex) {
        }
    }

    /**
     * Create and save mask image
     *
     * @return
     */
    public String createMask() {

        short[][] maskGrayArray = find_Circles();
        if (maskGrayArray == null) {
            return "Correct template image not found in the video file";
        }

        String maskTitle = (String) JOptionPane.showInputDialog(this.frame, "Image processing has been completed\n\n"
                + "What is the title of a new mask image?\n"
                + "A single word title is needed\n"
                + "Ex) FUR\n");

        if (maskTitle == null || maskTitle.equals("")) {
            return "Null title.  Aborted";
        } else {
            //Save image
            String maskFileName = "MASK OF " + maskTitle + ".gif";

            BufferedImage maskGrayImg
                    = imgProc.convert_GrayShortArray_To_Image(maskGrayArray);
            imgProc.saveImage(maskGrayImg, "gif", maskFileName);
            return null;
        }

    }

    /**
     * Search circles and return IDMap of the circles
     *
     * @return
     */
    public short[][] find_Circles() {
        final boolean isDebugMode = false;

        BufferedImage binaryImage;
        BufferedImage regionLabeledImage;

        short[][] IDMap;
        short[][] ColorTable;
        float[][] blobStat;
        int imageWidthUpperBound = 0;
        int imageHeightUpperBound = 0;

        int ringCount;
        int circleCount;

        BufferedImage curFrame;
        short[][] arrayCurFrame;
        BufferedImage curFrameAligned;
        short[][] arrayCurFrameAligned = null;
        double[] axisAndAngleArray;

        //Finding the first valid frame image
        for (long curTimeInNanoSec = 0; curTimeInNanoSec < this.videoDurationInSec * 1000000000;
                curTimeInNanoSec = curTimeInNanoSec + frameAnalysisInterval) {

            curFrame = frameReader.grabImage_At_TimeInNanoSec(curTimeInNanoSec);
            if (curFrame == null) {
                continue;
            }

            arrayCurFrame = imgProc.convert_Image_To_GrayShortArray(curFrame);

            if (imgProc.isBlankImage(arrayCurFrame, 10) == false) {

                axisAndAngleArray = imgProc.calculate_CenterAndAngle(curFrame);
                curFrameAligned = imgProc.alignImage(curFrame, axisAndAngleArray);
                arrayCurFrameAligned = imgProc.convert_Image_To_GrayShortArray(curFrameAligned);

                binaryImage
                        = imgProc.adaptiveThresholding_Core(
                                arrayCurFrameAligned, 15, 0.3f, 300);
                arrayCurFrameAligned = imgProc.multiArrayCopy(imgProc.AdaptiveThreshold_BWPixelArray);

                imageWidthUpperBound = arrayCurFrameAligned.length - 1;
                imageHeightUpperBound = arrayCurFrameAligned[0].length - 1;

                if (isDebugMode) {
                    imgProc.saveImage(curFrameAligned, "gif",
                            "C:\\Jung\\My Temp\\curFrameAligned.gif");

                    imgProc.saveImage(binaryImage, "gif",
                            "C:\\Jung\\My Temp\\binaryImage.gif");

                }
                break;
            }

        }

        if (arrayCurFrameAligned == null) {
            return null;
        }

        //Conduct region labeling and blob analysis
        regionLabeledImage = imgProc.regionExtract_RasterScanning(arrayCurFrameAligned, 0);
        if (isDebugMode) {
            imgProc.saveImage(regionLabeledImage, "bmp",
                    "C:\\Jung\\My Temp\\ringBinary.bmp");
        }
        IDMap = imgProc.RegionLabeling_LabelIDMap;
        ColorTable = imgProc.RegionLabeling_ColorTable;

        blobStat = imgProc.regionExtract_BasicAnalysis(IDMap,
                ColorTable);

        //find rings
        ringCount = 0;
        for (int curBlob = 1; curBlob < blobStat.length; curBlob++) {
            //Explanation
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

            //delete invalid objects
            if (blobStat[curBlob][2] < 4000
                    || blobStat[curBlob][7] < 60
                    || (blobStat[curBlob][6] - blobStat[curBlob][5]) > 400) {

                blobStat[curBlob][8] = 1;
            } else {
                blobStat[curBlob][8] = 0;
                ringCount++;
            }
        }

        //Delete invalid objects and create new srcImage
        short[][] srcGrayShortArray
                = new short[imageWidthUpperBound + 1][imageHeightUpperBound + 1];

        for (int x = 0; x <= imageWidthUpperBound; x++) {
            for (int y = 0; y <= imageHeightUpperBound; y++) {
                if (IDMap[x][y] == 0
                        || blobStat[IDMap[x][y]][8] == 1) {
                    srcGrayShortArray[x][y] = 0;
                } else {
                    srcGrayShortArray[x][y] = 255;
                }

            }
        }

        if (isDebugMode) {
            regionLabeledImage = imgProc.convert_GrayShortArray_To_Image(srcGrayShortArray);
            imgProc.saveImage(regionLabeledImage, "bmp",
                    "C:\\Jung\\My Temp\\deleteInvalidImage.bmp");
        }

        //Create filled circle regions
        if (srcGrayShortArray[0][0] == 0) {
            srcGrayShortArray = imgProc.floodFill(srcGrayShortArray, 0, 0, (short) 100, 1);
        } else {
            return null;
        }
        for (int x = 0; x <= imageWidthUpperBound; x++) {
            for (int y = 0; y <= imageHeightUpperBound; y++) {
                if (srcGrayShortArray[x][y] == 100) {
                    srcGrayShortArray[x][y] = 0;
                } else {
                    srcGrayShortArray[x][y] = 255;
                }

            }
        }

        regionLabeledImage = imgProc.regionExtract_RasterScanning(srcGrayShortArray, 0);
        if (isDebugMode) {
            imgProc.saveImage(regionLabeledImage, "bmp",
                    "C:\\Jung\\My Temp\\circleBinary.bmp");
        }

        IDMap = imgProc.RegionLabeling_LabelIDMap;
        ColorTable = imgProc.RegionLabeling_ColorTable;

        blobStat = imgProc.regionExtract_BasicAnalysis(IDMap,
                ColorTable);

        //find circles
        circleCount = 0;
        for (int curBlob = 1; curBlob < blobStat.length; curBlob++) {
            //blobStat[][0]: center X
            //blobStat[][1]: center Y
            //blobStat[][2]: pixel count
            //blobStat[][3]: left X
            //blobStat[][4]: right X
            //blobStat[][5]: min Y
            //blobStat[][6]: max Y
            //blobStat[][7]: average radius
            //blobStat[][8]: validity
            //               if validity=0, OK,   if validity=1, invalid)

            //delete invalid objects
            if (blobStat[curBlob][2] < 20000) {
                blobStat[curBlob][8] = 1;
            } else {
                blobStat[curBlob][8] = 0;
                circleCount++;
            }
        }

        //Delete invalid objects and create new srcImage
        for (int x = 0; x <= imageWidthUpperBound; x++) {
            for (int y = 0; y <= imageHeightUpperBound; y++) {
                if (IDMap[x][y] == 0
                        || blobStat[IDMap[x][y]][8] == 1) {
                    srcGrayShortArray[x][y] = 0;
                } else {
                    srcGrayShortArray[x][y] = 255;
                }

            }
        }
        if (isDebugMode) {
            regionLabeledImage = imgProc.convert_GrayShortArray_To_Image(srcGrayShortArray);
            imgProc.saveImage(regionLabeledImage, "bmp",
                    "C:\\Jung\\My Temp\\finalCircleImage.bmp");
        }

        //final region labeling of valid circles        
        regionLabeledImage = imgProc.regionExtract_RasterScanning(srcGrayShortArray, 0);
        if (isDebugMode) {
            imgProc.saveImage(regionLabeledImage, "bmp",
                    "C:\\Jung\\My Temp\\finalCircleRegionImage.bmp");
        }

        IDMap = imgProc.RegionLabeling_LabelIDMap;
        ColorTable = imgProc.RegionLabeling_ColorTable;
        blobStat = imgProc.regionExtract_BasicAnalysis(IDMap,
                ColorTable);

        //calculate circle count
        circleCount = 0;
        int[] detected_CirclesID = new int[3];

        for (int curBlob = 1; curBlob < blobStat.length; curBlob++) {
            //blobStat[][0]: center X
            //blobStat[][1]: center Y
            //blobStat[][2]: pixel count
            //blobStat[][3]: left X
            //blobStat[][4]: right X
            //blobStat[][5]: min Y
            //blobStat[][6]: max Y
            //blobStat[][7]: average radius
            //blobStat[][8]: validity
            //               if validity=0, OK,   if validity=1, invalid)

            //delete invalid objects
            if (blobStat[curBlob][2] < 10000) {
                blobStat[curBlob][8] = 1;
            } else {
                blobStat[curBlob][8] = 0;
                if (circleCount < 2) {
                    detected_CirclesID[circleCount] = curBlob;
                }
                circleCount++;
            }
        }

        if (circleCount != 2) {
            return null;
        }

        //Create final image for mask image
        short[] replaceInfo = new short[blobStat.length];

        if (blobStat[detected_CirclesID[0]][3]
                < blobStat[detected_CirclesID[1]][3]) {
            replaceInfo[detected_CirclesID[0]] = 100;
            replaceInfo[detected_CirclesID[1]] = 200;
        } else {
            replaceInfo[detected_CirclesID[1]] = 100;
            replaceInfo[detected_CirclesID[0]] = 200;
        }
        for (int x = 0; x <= imageWidthUpperBound; x++) {
            for (int y = 0; y <= imageHeightUpperBound; y++) {
                if (IDMap[x][y] == 0
                        || blobStat[IDMap[x][y]][8] == 1) {
                    srcGrayShortArray[x][y] = 0;
                } else {
                    srcGrayShortArray[x][y] = replaceInfo[IDMap[x][y]];
                }

            }
        }

        return srcGrayShortArray;
    }
}
