/*
 * Filename: ImageProcessing.java
 */
package edu.rice.wormlab.wormlength;

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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ImageProcessing {

    private static final PrintStream out = System.out;
    public static NativeImgProcessing imgProc = new NativeImgProcessing();
    protected static final int[][] structuringElement = {
        {0, 0, 1, 1, 1, 1, 1, 0, 0},
        {0, 1, 1, 1, 1, 1, 1, 1, 0},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {0, 1, 1, 1, 1, 1, 1, 1, 0},
        {0, 0, 1, 1, 1, 1, 1, 0, 0},};

    public static DetectionCondition detectionCondition = new DetectionCondition();
    
    
	 // performs recursive image processing
    public static Map<String, String> recursiveImageProcessing(File directory) {
        if (directory == null) {
            return null;
        }; // if
        Map<String, String> ret = new TreeMap<String, String>();
        recursiveImageProcessing(directory, ret);
        return ret;
    }

    
	 // performs recursive image processing (helper private function)
    private static void recursiveImageProcessing(File folder, Map<String, String> outcomeMap) {
        if (folder == null) {
            return;
        }; // if

        // does this folder contain images?
        int imageFilesCount = Utilities.countPiecesFiles(folder.getAbsolutePath());
        File theFile = new File(folder.getAbsolutePath() + File.separator + Utilities.ASSEMBLED_JPEG);
        boolean existingAssembleOrgFileFlag = theFile.exists();
        
        if (imageFilesCount > 0 || existingAssembleOrgFileFlag) {
            String error = imageProcessing(folder);
            outcomeMap.put(folder.getAbsolutePath(), error);
        } 

        // get the sub-directories
        List<File> subdirectoriesList = new ArrayList<File>();
        File[] subfolders = folder.listFiles();
        for (File eachFolder : subfolders) {
            if (eachFolder.isDirectory() == true) {
                subdirectoriesList.add(eachFolder);
            }; // if
        }; // for

        // recursion happens here
        for (File each : subdirectoriesList) {
            recursiveImageProcessing(each, outcomeMap);
        }; // for
    }

    /**
     * Performs the wormlength image processing on a directory
     *
     * @param directory the directory
     * @return null when things go OK, otherwise an error message
     */
    public static String imageProcessing(File directory) {
        long startTime = System.currentTimeMillis();
        
        ScannerLog scannerLog = ScannerLog.readLog(directory);
        String error = scannerLog.getErrors();
        if (error != null) {
            return error;
        }; // if


        // get assembled_Org image
        File theFile = new File(directory.getAbsolutePath() + File.separator + Utilities.ASSEMBLED_JPEG);
        boolean existingAssembleOrgFileFlag = theFile.exists();
        ImagePlus assembledOrg = new ImagePlus("assembled");


        if (existingAssembleOrgFileFlag) {
            assembledOrg = new ImagePlus(directory.getAbsolutePath()
                    + File.separator + Utilities.ASSEMBLED_JPEG);

        } else {
            assembledOrg = Utilities.assembleImage_Reload(directory.getAbsolutePath(), scannerLog);

            if (assembledOrg == null) {
                return "Unable to create assembled image";
            }

        }


        ImagePlus assembled = new ImagePlus("assembled", assembledOrg.getBufferedImage());


        if (assembled == null) {
            return "Internal Error (assembled is null)";
        }; // if


        //Conduct adaptive thresholding
        short[][] srcImageArray = imgProc.convert_GrayImage_To_GrayShortArray(assembled.getBufferedImage());
        BufferedImage srcImage = imgProc.adaptiveThresholding_Core_To_Gray(srcImageArray,
                60, 0.2f, 200);
        assembled.setImage(srcImage);
        assembled.getProcessor().invert();


        int pieces = Utilities.countPiecesFiles(directory.getAbsolutePath());
        int numberOfColumns = 0;
        int numberOfRows = 0;
        if (pieces == 130) {
            numberOfColumns = 10;
            numberOfRows = 13;
        }; // if
        if (pieces == 154) {
            numberOfColumns = 11;
            numberOfRows = 14;
        }; // if


        int croppedX = 0 * 640;
        int croppedY = 0 * 480;
        int croppedWidth = assembled.getWidth();
        int croppedHeight = assembled.getHeight();
        if (numberOfColumns > 2 && numberOfRows > 2) {
            croppedX = 1 * 640; // skip the first column
            croppedY = 1 * 480; // skip the first row
            croppedWidth = (numberOfColumns - 2) * 640; // skip the last column
            croppedHeight = (numberOfRows - 2) * 480;  // skip the last row
        }; // if
        ImagePlus cropped = Utilities.cropImage(assembled.getProcessor(), croppedX, croppedY,
                croppedWidth, croppedHeight);
        List<WormInfo> infoList = processing(assembled, cropped, detectionCondition, directory,
                croppedX, croppedY, scannerLog);


        error = saveResultsToFile(infoList, directory.getAbsolutePath());
        saveAssembledColorImage( infoList, assembledOrg, directory.getAbsolutePath() );
        long endTime = System.currentTimeMillis();
        out.println(directory.getAbsolutePath() + " \t" + ((endTime - startTime) / 1000.0) + " seconds");
        return error;
    }



    /**
     * Processing the image of worms
     */
    public static List<WormInfo> processing(ImagePlus assembled, ImagePlus cropped, DetectionCondition wormSettings, File directory, int croppedX, int croppedY, ScannerLog scannerLog) {


        cropped.getProcessor().invert();

        if (cropped.getBitDepth() != 8) {
            ImageConverter icv = new ImageConverter(cropped);
            icv.convertToGray8();
        }; // if  

        imClearBorder.imclearborder(cropped);
        

        //Label and measure
        SequentialLabeling sq = new SequentialLabeling(cropped.getProcessor());
        sq.applyLabeling();
        sq.collectRegions();
        // these two are for collecting selected image
        List<BinaryRegion> roiList = new LinkedList<BinaryRegion>();
        List<WormInfo> infoList = new LinkedList<WormInfo>();

        List<BinaryRegion> list = sq.regions;  // the information of all binary regions is kept in list
        Iterator<BinaryRegion> brIterator = list.iterator(); //iterating the list
        while (brIterator.hasNext()) {
            BinaryRegion br = (BinaryRegion) brIterator.next();
            double area = br.getSize();
            
            
            //Exclude invalid objects
            if (area > wormSettings.minArea && area < wormSettings.maxArea) {  //filtering according to area
                roiList.add(br);
            }
        }

        Iterator<BinaryRegion> roiIterator = roiList.iterator();

        int currentRegion_Count = 0;

        //Create imageClip folder
        File newClipImageFolder = new File(directory.getAbsolutePath()
                + File.separator + "imageClip");
        if (!newClipImageFolder.exists()) {
            newClipImageFolder.mkdir();
        }

        PrintWriter pw = null;


        while (roiIterator.hasNext()) {
                //generating the image of the roi

                BinaryRegion roi = (BinaryRegion) roiIterator.next();
                double Area = roi.getSize();
                Rectangle rec = roi.getBoundingBox();
                int label = roi.getLabel();
                int x = rec.x;
                int y = rec.y;
                int height = rec.height;
                int width = rec.width;

                

                //Exclude invalid objects
                if (height > wormSettings.minBoundingSize ||
                        width > wormSettings.minBoundingSize) {
                    //OK
                } else {
                    continue;
                }
                
                if (height >  wormSettings.maxBoundingSize || 
                        width > wormSettings.maxBoundingSize) {
                    continue;
                }


                ImagePlus currentRegion = NewImage.createByteImage("currentRegion", width, height, 1, NewImage.FILL_BLACK);
                ImagePlus grayRegion = NewImage.createByteImage("grayRegion", width, height, 1, NewImage.GRAY8);

                BufferedImage imageGrayRegion;
                BufferedImage imageBW;
                BufferedImage imagePadedClosed;

                //For debugging..
                BufferedImage imageSkeleton;
                BufferedImage imageSkeletonTrimed;

                short[][] imageGrayRegionArray = new short[width][height];
                short[][] imageBWArray = new short[width][height];
                short[][] imagePadedClosedArray = new short[width][height];
                short[][] imageSkeletonArray = new short[width][height];
                short[][] imageSkeletonTrimedArray = new short[width][height];
                short[][] imageSkeletonExtendedArray = new short[width][height];
                short[][] imageSkeletonExtendedTrimedArray = new short[width][height];


                currentRegion_Count++;


                //Get imageGrayRegion and imageBW
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        if (sq.labels[(y + j) * cropped.getWidth() + x + i] == label) //excluding other objects in the same bounding box
                        {
                            imageBWArray[i][j] = (short) cropped.getPixel(x + i, y + j)[0];
                            imageGrayRegionArray[i][j] = (short) assembled.getPixel(x + i + croppedX, y + j + croppedY)[0];
                        }
                    }
                }
                imageBW = imgProc.convert_GrayShortArray_To_Image(imageBWArray);
                imageGrayRegion = imgProc.convert_GrayShortArray_To_Image(imageGrayRegionArray);

                currentRegion.setImage(imageBW);
                grayRegion.setImage(imageGrayRegion);



                //Convert rgb image to gray image
                if (currentRegion.getBitDepth() != 8) {
                    ImageConverter icv = new ImageConverter(currentRegion);
                    icv.convertToGray8();
                }; // if  



                //close the image
                //padding and closing
                ByteProcessor curProcessor = (ByteProcessor) currentRegion.getProcessor();
                ByteProcessor padded = new ByteProcessor(width + 20, height + 20);
                padded.copyBits(curProcessor, 10, 10, Blitter.COPY);
                BinMorpher bin = new BinMorpher(structuringElement);
                bin.close(padded, structuringElement);



                //Get imagePadedClosed
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        imagePadedClosedArray[i][j] = (short) padded.getPixel(i + 10, j + 10);
                    }
                }
                imagePadedClosed = imgProc.convert_GrayShortArray_To_Image(imagePadedClosedArray);
                currentRegion.setImage(imagePadedClosed);


                //Updating curProcessor 
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        curProcessor.putPixel(i, j, (short) padded.getPixel(i + 10, j + 10));
                    }
                }
                
                
                //skeletonize
                removeSpur(curProcessor, wormSettings.spurTh, 255);
                curProcessor.invert();
                curProcessor.skeletonize();
                curProcessor.invert();
                removeSpur(curProcessor, wormSettings.spurTh, 255);


      

                //Get imageSkeleton
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        imageSkeletonArray[i][j] = (short) curProcessor.getPixel(i, j);
                    }
                }

                //trim the skeleton
                trim(curProcessor, 255);

                
                //Extending curve to the end of shape
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        imageSkeletonTrimedArray[i][j] = (short) curProcessor.getPixel(i, j);
                    }
                }

                
                imageSkeletonExtendedArray = extend_SkeletonCurve(imageSkeletonTrimedArray, imagePadedClosedArray);


                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        curProcessor.putPixel(i, j, imageSkeletonExtendedArray[i][j]);
                    }
                }

                //Trim again because extend_SkeltonCurve may creates noisy pixels
                trim(curProcessor, 255);
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        imageSkeletonExtendedTrimedArray[i][j] = (short) curProcessor.getPixel(i, j);
                    }
                }

            //Exclude invalid objects
            int wormLen = getWormLen(curProcessor, 255);
            double f = (double) Area / (double) wormLen;                
            if (f < wormSettings.minMeanWormFat ||
                    f > wormSettings.maxMeanWormFat) {
                continue;
            }

            
            

            //Checking branch-points and end-points
            Double stepsPerPixelsX = scannerLog.getStepsPerPixelsX();
            Double stepsPerPixelsY = scannerLog.getStepsPerPixelsY();

            LinkedList<int[]> branchPoints = findBranchPoints(curProcessor, 255);
            int nBranch = branchPoints.size();

            LinkedList<int[]> endPoints = findEndPoints(curProcessor, 255);
            int nEnd = endPoints.size();
            double trueLen = -1.00;



            
            //Exclude invalid objects    
            if (nEnd == 2 && nBranch == 0) {
                trueLen = getTrueWormLen(curProcessor, 255, stepsPerPixelsX, stepsPerPixelsY);
            } else {
                continue;  //if true length not computable , bypass!
            }
            
            
            //Exclude invalid objects
            if (trueLen < wormSettings.minTrueLength ||
                    trueLen > wormSettings.maxTrueLength) {
                continue;
            }
            

            
            //Save clip image
            //Step 1: we do the copy
            BufferedImage maskImageWithSkeleton;
            maskImageWithSkeleton = imgProc.deepCopy(imagePadedClosed);
            //Step 2: we overlay the skeleton
            for (int i = 0; i < curProcessor.getWidth(); i++) {
                for (int j = 0; j < curProcessor.getHeight(); j++) {
                    if (curProcessor.getPixel(i, j) == 255) {
                        Color color = new Color(imageSkeletonExtendedTrimedArray[i][j], 0, 0);
                        maskImageWithSkeleton.setRGB(i, j, color.getRGB());
                    }
                }; // for
            }; // for
            // Step 3: Save clip images            
            imgProc.saveImage(maskImageWithSkeleton, "gif", directory.getAbsolutePath()
                    + File.separator + "imageClip" + File.separator + "Clip"
                    + currentRegion_Count + ".gif");



            //Saving results
            final int ONE_WORM = 1;
            final int PROBABILITY = 100; // useless value
            WormInfo info = new WormInfo(ONE_WORM, croppedX + x, croppedY + y,
                    width, height, PROBABILITY, null, currentRegion_Count);
            info.height = height;
            info.trueLength = trueLen;
            info.length = wormLen;
            info.currentRegion = new ImagePlus("region", currentRegion.getProcessor());
            info.suspicious = false;
            info.imageClipN = currentRegion_Count;
            infoList.add(info);
        }

        return infoList;
    }

    //display on the screen
    protected static void imview(ImageProcessor ipc) {
        ImageProcessor dupIPC = ipc.duplicate();
        ImagePlus displayer = new ImagePlus();
        displayer.setProcessor(dupIPC);
        displayer.show();
    }

    /**
     * remove spur points
     *
     * @param skelWorm The binary image of the worm skeleton
     * @param maxTimes The length of the spurs to be removed
     * @param foreground Either be 255(white) or 0(black)
     */
    public static void removeSpur(ByteProcessor skelWorm, int maxTimes, int foreground) {
        ByteProcessor template = (ByteProcessor) skelWorm.duplicate();
        int background = (foreground == 255) ? 0 : 255;
        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        int times = 0;
        LinkedList<int[]> oriEndList = findEndPoints(skelWorm, foreground);


        while (times < maxTimes) {
            LinkedList<int[]> cordList = new LinkedList<int[]>();
            cordList = reportSpurPoints(skelWorm, foreground);
            Iterator<int[]> cordIter = cordList.iterator();
            while (cordIter.hasNext()) {
                int[] record = (int[]) cordIter.next();
                skelWorm.putPixel(record[0], record[1], background);
            }
            times++;
        }//end of loop while

        //regrowth
        LinkedList<int[]> endList = findEndPoints(skelWorm, foreground);
        for (int s = 0; s < endList.size(); s++) {

            int[] end = (int[]) endList.get(s);
            int lastW = end[0];
            int lastH = end[1];
            int curW = lastW;
            int curH = lastH;
            int jump = 0;
            int count = 0;

            while (count < maxTimes) {
                int fail = 0;
                for (int m = -1; m <= 1; m++) {
                    for (int n = -1; n <= 1; n++) {
                        curW = lastW + m;
                        curH = lastH + n;
                        if (curW >= 0 && curH >= 0 && curW < width && curH < height && skelWorm.getPixel(curW, curH) == background && template.getPixel(curW, curH) == foreground) {
                            lastW = curW;
                            lastH = curH;
                            skelWorm.putPixel(curW, curH, foreground);
                            count = count + 1;
                            for (int p = 0; p < oriEndList.size(); p++) {
                                int[] ori = oriEndList.get(p);
                                if (ori[0] == curW && ori[1] == curH) {
                                    jump = 1;
                                }
                            }
                            if (jump == 1) {
                                break;
                            }
                        }//end of if curW>0
                        else {
                            fail++;
                            if (fail == 9) {
                                jump = 1;
                                break;
                            }
                        } //end of else
                    }//end of for n
                    if (jump == 1) {
                        break;
                    }
                }//end of for m
                if (jump == 1) {
                    break;
                }
            }
        }
    }

    /**
     * return the list of end points in a give skel of worm
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     */
    public static LinkedList<int[]> findEndPoints(ByteProcessor skelWorm, int foreground) {
        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        LinkedList<int[]> endList = new LinkedList<int[]>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (skelWorm.getPixel(i, j) == foreground) {
                    int starti = i - 1;
                    int startj = j - 1;
                    int endi = i + 1;
                    int endj = j + 1;
                    if (i == 0) {
                        starti = i;
                    }
                    if (i == width - 1) {
                        endi = width - 1;
                    }
                    if (j == 0) {
                        startj = j;
                    }
                    if (j == height - 1) {
                        endj = height - 1;
                    }
                    int sum = 0;
                    for (int m = starti; m <= endi; m++) {
                        for (int n = startj; n <= endj; n++) {
                            if (skelWorm.getPixel(m, n) == foreground) {
                                sum++;
                            }
                        }
                    }
                    if (sum == 2) {
                        int[] cord = {i, j};
                        endList.add(cord);
                    }
                }
            }//end of loop j
        }// end of loop i
        return endList;
    }

    /**
     * return the list of spur points in a give skel of worm
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     */
    public static LinkedList<int[]> reportSpurPoints(ByteProcessor skelWorm, int foreground) {
        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        LinkedList<int[]> cordList = new LinkedList<int[]>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (skelWorm.getPixel(i, j) == foreground) {
                    int starti = i - 1;
                    int startj = j - 1;
                    int endi = i + 1;
                    int endj = j + 1;
                    if (i == 0) {
                        starti = i;
                    }
                    if (i == width - 1) {
                        endi = width - 1;
                    }
                    if (j == 0) {
                        startj = j;
                    }
                    if (j == height - 1) {
                        endj = height - 1;
                    }
                    int sum = 0;
                    for (int m = starti; m <= endi; m++) {
                        for (int n = startj; n <= endj; n++) {
                            if (skelWorm.getPixel(m, n) == foreground) {
                                sum++;
                            }
                        }
                    }
                    if (sum == 2 || sum == 1) {
                        int[] cord = {i, j};
                        cordList.add(cord);
                    }
                }
            }//end of loop j
        }// end of loop i
        return cordList;
    }

    /**
     * get the length of worm by counting pixels
     *
     * @param skelWorm The binary image of the worm skeleton
     * @param foreground Either be 255(white) or 0(black)
     * @return The length of the skeleton
     */
    public static int getWormLen(ByteProcessor skelWorm, int foreground) {
        int wormLen = 0;
        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (skelWorm.getPixel(i, j) == foreground) {
                    wormLen++;
                }
            }
        }
        return wormLen;
    }

    /**
     * remove undesired points
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     */
    public static void trim(ByteProcessor skelWorm, int foreground) {
        trimSingleRound(skelWorm, foreground, 1);
        trimSingleRound(skelWorm, foreground, 2);
    }

    /**
     * trim undesired points
     *
     * @param skelWorm The binary image of the worm skeleton
     * @param foreground Either be 255(white) or 0(black)
     * @param mode Specify the task . mode 1: remove T and L; mode 2: remove
     * corner points
     */
    public static void trimSingleRound(ByteProcessor skelWorm, int foreground, int mode) {
        int background = (foreground == 255) ? 0 : 255;
        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (skelWorm.getPixel(i, j) == foreground) {
                    int starti = i - 1;
                    int startj = j - 1;
                    int endi = i + 1;
                    int endj = j + 1;
                    if (i == 0) {
                        starti = i;
                    }
                    if (i == width - 1) {
                        endi = width - 1;
                    }
                    if (j == 0) {
                        startj = j;
                    }
                    if (j == height - 1) {
                        endj = height - 1;
                    }
                    int sum = 0;
                    for (int m = starti; m <= endi; m++) {
                        for (int n = startj; n <= endj; n++) {
                            if (skelWorm.getPixel(m, n) == foreground) {
                                sum++;
                            }
                        }
                    }
                    if (mode == 1) {
                        if (sum == 3) {
                            if (i > 0 && i < width - 1 && j > 0 && j < height - 1) {
                                boolean b1 = (skelWorm.getPixel(i - 1, j - 1) == foreground && skelWorm.getPixel(i - 1, j) == foreground);
                                boolean b2 = (skelWorm.getPixel(i - 1, j - 1) == foreground && skelWorm.getPixel(i, j - 1) == foreground);
                                boolean b3 = (skelWorm.getPixel(i, j - 1) == foreground && skelWorm.getPixel(i + 1, j - 1) == foreground);
                                boolean b4 = (skelWorm.getPixel(i + 1, j - 1) == foreground && skelWorm.getPixel(i + 1, j) == foreground);
                                boolean b5 = (skelWorm.getPixel(i + 1, j) == foreground && skelWorm.getPixel(i + 1, j + 1) == foreground);
                                boolean b6 = (skelWorm.getPixel(i + 1, j + 1) == foreground && skelWorm.getPixel(i, j + 1) == foreground);
                                boolean b7 = (skelWorm.getPixel(i, j + 1) == foreground && skelWorm.getPixel(i - 1, j + 1) == foreground);
                                boolean b8 = (skelWorm.getPixel(i - 1, j + 1) == foreground && skelWorm.getPixel(i - 1, j) == foreground);
                                if (b1 || b2 || b3 || b4 || b5 || b6 || b7 || b8) {
                                    skelWorm.putPixel(i, j, background);
                                }
                            }
                        }
                        if (sum == 4) {
                            boolean b1 = (skelWorm.getPixel(i + 1, j) == background && skelWorm.getPixel(i - 1, j - 1) == foreground && skelWorm.getPixel(i - 1, j) == foreground && skelWorm.getPixel(i - 1, j + 1) == foreground);
                            boolean b2 = (skelWorm.getPixel(i - 1, j) == background && skelWorm.getPixel(i + 1, j - 1) == foreground && skelWorm.getPixel(i + 1, j) == foreground && skelWorm.getPixel(i + 1, j + 1) == foreground);
                            boolean b3 = (skelWorm.getPixel(i, j + 1) == background && skelWorm.getPixel(i - 1, j - 1) == foreground && skelWorm.getPixel(i, j - 1) == foreground && skelWorm.getPixel(i + 1, j - 1) == foreground);
                            boolean b4 = (skelWorm.getPixel(i, j - 1) == background && skelWorm.getPixel(i - 1, j + 1) == foreground && skelWorm.getPixel(i, j + 1) == foreground && skelWorm.getPixel(i + 1, j + 1) == foreground);
                            if (b1 || b2 || b3 || b4) {
                                skelWorm.putPixel(i, j, background);
                            }
                        }
                    }
                    if (mode == 2) {
                        if (sum >= 3) {
                            boolean b1 = (skelWorm.getPixel(i, j - 1) == foreground && skelWorm.getPixel(i + 1, j) == foreground && skelWorm.getPixel(i - 1, j + 1) == background);
                            boolean b2 = (skelWorm.getPixel(i + 1, j) == foreground && skelWorm.getPixel(i, j + 1) == foreground && skelWorm.getPixel(i - 1, j - 1) == background);
                            boolean b3 = (skelWorm.getPixel(i, j + 1) == foreground && skelWorm.getPixel(i - 1, j) == foreground && skelWorm.getPixel(i + 1, j - 1) == background);
                            boolean b4 = (skelWorm.getPixel(i - 1, j) == foreground && skelWorm.getPixel(i, j - 1) == foreground && skelWorm.getPixel(i + 1, j + 1) == background);
                            if (b1 || b2 || b3 || b4) {
                                skelWorm.putPixel(i, j, background);
                            }
                        }
                    }
                }
            }//end of loop j
        }// end of loop i
    }

    /**
     * return the list of branching points in a give skel of worm
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     */
    public static LinkedList<int[]> findBranchPoints(ByteProcessor skelWorm, int foreground) {
        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        LinkedList<int[]> branchList = new LinkedList<int[]>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (skelWorm.getPixel(i, j) == foreground) {
                    int starti = i - 1;
                    int startj = j - 1;
                    int endi = i + 1;
                    int endj = j + 1;
                    if (i == 0) {
                        starti = i;
                    }
                    if (i == width - 1) {
                        endi = width - 1;
                    }
                    if (j == 0) {
                        startj = j;
                    }
                    if (j == height - 1) {
                        endj = height - 1;
                    }
                    int sum = 0;
                    for (int m = starti; m <= endi; m++) {
                        for (int n = startj; n <= endj; n++) {
                            if (skelWorm.getPixel(m, n) == foreground) {
                                sum++;
                            }
                        }
                    }
                    if (sum >= 4) {
                        int[] cord = {i, j};
                        branchList.add(cord);
                    }
                }
            }//end of loop j
        }// end of loop i

        //clear the extra points


        LinkedList<int[]> finalBranchList = new LinkedList<int[]>();
        int initSize = branchList.size();
        int[] available = new int[initSize]; //flags
        for (int t = 0; t < initSize; t++) {
            available[t] = 0;
        }
        if (initSize > 1) {
            for (int q = 0; q < initSize; q++) {
                if (available[q] == 0) {
                    int[] point = (int[]) branchList.get(q);
                    finalBranchList.add(point);
                    int r1 = point[0];
                    int r2 = point[1];
                    for (int s = q + 1; s < initSize; s++) {
                        int[] point2 = (int[]) branchList.get(s);
                        int v1 = point2[0];
                        int v2 = point2[1];
                        if (Math.abs(v1 - r1) <= 2 && Math.abs(v2 - r2) <= 2) {
                            available[s] = 1;
                        }
                    }
                }
            }
            return finalBranchList;
        }
        return branchList;
    }

    /**
     * get the length of worm by chain code retrieve
     *
     * @param skelWorm The binary image of the worm skeleton
     * @param foreground foreground Either be 255(white) or 0(black)
     * @param stepsPerPixelsX the x steps per pixel value
     * @param stepsPerPixelsY the y steps per pixel value
     * @return The length of the worm(distance to diagonal neighborhood pixel
     * treated as sqrt(2))
     */
    public static double getTrueWormLen_Old(ByteProcessor skelWorm, int foreground, Double stepsPerPixelsX, Double stepsPerPixelsY) {
        ByteProcessor template = (ByteProcessor) skelWorm.duplicate();

        final double micronsPerStepX = 1.0; //micron
        final double micronsPerStepY = 1.0;

        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        int wormLen = getWormLen(skelWorm, foreground);
        LinkedList<int[]> ePoints = findEndPoints(skelWorm, foreground);
        int[] end = ePoints.get(1);
        int lastW = end[0];
        int lastH = end[1];
        int jump = 0;
        int count = 0;
        int lastcount = 0;
        double trueLen = 0.0;
        while (count < wormLen) {

            lastcount = count;
            template.putPixel(lastW, lastH, 0);
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

                    if (template.getPixel(i, j) == foreground) {
                        if (i != lastW && j != lastH) {
                            trueLen += Math.sqrt(stepsPerPixelsX * micronsPerStepX +
                                            stepsPerPixelsY * micronsPerStepY);
                        } else if (i == lastW) {
                            trueLen += stepsPerPixelsX * micronsPerStepX;
                        } else {
                            trueLen += stepsPerPixelsY * micronsPerStepY;
                        }
                        lastW = i;
                        lastH = j;
                        count++;
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
        return trueLen;
    }

    
    
    public static double getTrueWormLen(ByteProcessor skelWorm, int foreground, Double stepsPerPixelsX, Double stepsPerPixelsY) {
        ByteProcessor template = (ByteProcessor) skelWorm.duplicate();

        final double micronsPerStepX = 1.0; //micron
        final double micronsPerStepY = 1.0;

        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        int wormLen = getWormLen(skelWorm, foreground);
        LinkedList<int[]> ePoints = findEndPoints(skelWorm, foreground);
        int[] end = ePoints.get(1);
        int lastW = end[0];
        int lastH = end[1];
        int jump = 0;
        int count = 0;
        int lastcount = 0;
        double trueLen = 0.0;
        while (count < wormLen) {

            lastcount = count;
            template.putPixel(lastW, lastH, 0);
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

                    if (template.getPixel(i, j) == foreground) {
                        if (i != lastW && j != lastH) {
                            trueLen += Math.sqrt(Math.pow(stepsPerPixelsX * micronsPerStepX, 2) + 
                                       Math.pow(stepsPerPixelsY * micronsPerStepY, 2));
                        } else if (i == lastW) {
                            trueLen += stepsPerPixelsY * micronsPerStepY;
                        } else {
                            trueLen += stepsPerPixelsX * micronsPerStepX;
                        }
                        lastW = i;
                        lastH = j;
                        count++;
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
        return trueLen;
    }
        
        
        
        
    /**
     * Save the list of worms to files
     *
     * @param wormInfoList the list containing worms information
     * @param directory directory where to save the RESULT_TXT file
     * @return null when everything goes ok, otherwise it returns an error
     * message
     */
    private static String saveResultsToFile(List<WormInfo> wormInfoList, String directory) {
        if (wormInfoList == null) {
            return "no-data";
        }; // if

        // place the objects into a set for sorting purposes
        // and, count the animals
        int nAnimals = 0;
        Set<WormInfo> wormInfoSet = new TreeSet<WormInfo>();
        for (WormInfo each : wormInfoList) {
            // we skip the ones that are suspicious
            if (each.suspicious == true) {
                continue;

            }; // if
            wormInfoSet.add(each);
            nAnimals++;
        }; // for

        try {
            FileWriter fileWriter = new FileWriter(directory + File.separator + App.RESULT_TXT);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter printWriter = new PrintWriter(bufferedWriter);
            printWriter.println("# File saved at:\t" + new Date());
            printWriter.println("# worms count:\t" + nAnimals);
            printWriter.println("# pX\tpY\twidth\theight\tlength(micrometers)\timageClipN");
            for (WormInfo wormInfo : wormInfoSet) {
                // we skip the ones that are suspicious
                if (wormInfo.suspicious == true) {

                    continue;
                }; // if
                printWriter.println(wormInfo.pX + "\t" + wormInfo.pY + "\t"
                        + wormInfo.width + "\t" + wormInfo.height + "\t"
                        + wormInfo.trueLength + "\t" + wormInfo.imageClipN);
            }; // for
            printWriter.flush();
            printWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return ex.getMessage();
        }; // try
        return null;
    }

    
    
    
   // Apply least squares to raw data to determine the coefficients for
   // an n-order equation: y = a0*X^0 + a1*X^1 + ... + an*X^n.
   // Returns the coefficients for the solved equation, given a number
   // of y and x data points. The rawData input is given in the form of
   // {{y0, x0}, {y1, x1},...,{yn, xn}}.   The coefficients returned by
   // the regression are {a0, a1,...,an} which corresponds to
   // {X^0, X^1,...,X^n}. The number of coefficients returned is the
   // requested equation order (norder) plus 1.
   double[] linear_equation(double rawData[][], int norder) {
        double a[][] = new double[norder + 1][norder + 1];
        double b[] = new double[norder + 1];
        double term[] = new double[norder + 1];
        double ysquare = 0;

        // step through each raw data entries
        for (int i = 0; i < rawData.length; i++) {

            // sum the y values
            b[0] += rawData[i][0];
            ysquare += rawData[i][0] * rawData[i][0];

            // sum the x power values
            double xpower = 1;
            for (int j = 0; j < norder + 1; j++) {
                term[j] = xpower;
                a[0][j] += xpower;
                xpower = xpower * rawData[i][1];
            }

            // now set up the rest of rows in the matrix - multiplying each row by each term
            for (int j = 1; j < norder + 1; j++) {
                b[j] += rawData[i][0] * term[j];
                for (int k = 0; k < b.length; k++) {
                    a[j][k] += term[j] * term[k];
                }
            }
        }

        // solve for the coefficients
        double coef[] = gauss(a, b);

        // solve the simultaneous equations via gauss
        return coef;
    }

    // it's been so long since I wrote this, that I don't recall the math
    // logic behind it. IIRC, it's just a standard gaussian technique for
    // solving simultaneous equations of the form: |A| = |B| * |C| where we
    // know the values of |A| and |B|, and we are solving for the coefficients
    // in |C|
    double[] gauss(double ax[][], double bx[]) {
        double a[][] = new double[ax.length][ax[0].length];
        double b[] = new double[bx.length];
        double pivot;
        double mult;
        double top;
        int n = b.length;
        double coef[] = new double[n];

        // copy over the array values - inplace solution changes values
        for (int i = 0; i < ax.length; i++) {
            for (int j = 0; j < ax[i].length; j++) {
                a[i][j] = ax[i][j];
            }
            b[i] = bx[i];
        }

        for (int j = 0; j < (n - 1); j++) {
            pivot = a[j][j];
            for (int i = j + 1; i < n; i++) {
                mult = a[i][j] / pivot;
                for (int k = j + 1; k < n; k++) {
                    a[i][k] = a[i][k] - mult * a[j][k];
                }
                b[i] = b[i] - mult * b[j];
            }
        }

        coef[n - 1] = b[n - 1] / a[n - 1][n - 1];
        for (int i = n - 2; i >= 0; i--) {
            top = b[i];
            for (int k = i + 1; k < n; k++) {
                top = top - a[i][k] * coef[k];
            }
            coef[i] = top / a[i][i];
        }
        return coef;
    }

    static int countValidNeighborhoodPixel(short[][] srcGrayArray, int searchPixelGrayColor,
            int i, int j) {

        int clipwidth = srcGrayArray.length;
        int clipheight = srcGrayArray[0].length;
        int neighborhoodPixelCount = 0;



        if ((i - 1 >= 0) && (j - 1 >= 0)) {
            if (srcGrayArray[i - 1][j - 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }

        }

        if (j - 1 >= 0) {
            if (srcGrayArray[i][j - 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }

        if ((i + 1 < clipwidth) && (j - 1 >= 0)) {
            if (srcGrayArray[i + 1][j - 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }
        //-----------------------
        if (i - 1 >= 0) {
            if (srcGrayArray[i - 1][j] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }

        if (i + 1 < clipwidth) {
            if (srcGrayArray[i + 1][j] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }
        //------------------------
        if ((i - 1 >= 0) && (j + 1 < clipheight)) {
            if (srcGrayArray[i - 1][j + 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }

        if (j + 1 < clipheight) {
            if (srcGrayArray[i][j + 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }

        if ((i + 1 < clipwidth) && (j + 1 < clipheight)) {
            if (srcGrayArray[i + 1][j + 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }

        }

        return neighborhoodPixelCount;
    }

    static boolean isBifurcationFound(Point[] foundTailLocation) {

        int foundTailLocationLength = foundTailLocation.length;


        for (int i = 0; i < foundTailLocationLength; i++) {
            int neightborhoodPixelCount = 0;
            for (int j = 0; j < foundTailLocationLength; j++) {
                if ((Math.abs(foundTailLocation[i].x - foundTailLocation[j].x) <= 1)
                        && (Math.abs(foundTailLocation[i].y - foundTailLocation[j].y) <= 1)) {
                    neightborhoodPixelCount++;
                }
            }

            if (neightborhoodPixelCount >= 4) {
                return true;
            }

        }

        return false;

    }

    static short[][] extend_SkeletonCurve(short[][] srcGrayArray, short[][] maskGrayArray) {

        int clipwidth = srcGrayArray.length;
        int clipheight = srcGrayArray[0].length;


        for (int i = 0; i < clipwidth; i++) {
            for (int j = 0; j < clipheight; j++) {



                //Finding end point
                if (srcGrayArray[i][j] == 255) {
                    int neighborhoodPixelCount = 0;
                    boolean processedTailFound = false;
                    boolean emptyMaskAreaFound = false;

                    neighborhoodPixelCount =
                            countValidNeighborhoodPixel(srcGrayArray, 255, i, j);


                    if (countValidNeighborhoodPixel(srcGrayArray, 125, i, j) > 0) {
                        processedTailFound = true;
                    }

                    if (countValidNeighborhoodPixel(maskGrayArray, 0, i, j) > 0) {
                        emptyMaskAreaFound = true;
                    }


                    if ((neighborhoodPixelCount == 1)
                            && (processedTailFound == false)
                            && (emptyMaskAreaFound == false)) {


                        //Finding skeleton tail
                        int tailLength = 10;
                        Point[] foundTailLocation = new Point[tailLength];


                        foundTailLocation = imgProc.search_SkeletonTail(
                                srcGrayArray, i, j, (short) 125, 0, tailLength);




                        if (foundTailLocation != null) {

                            if (isBifurcationFound(foundTailLocation) == false) {

                                //Calculating vector
                                double avgvectorX = 0;
                                double avgvectorY = 0;
                                for (int k = 0; k < tailLength - 1; k++) {
                                    avgvectorX = avgvectorX
                                            + foundTailLocation[k].x - foundTailLocation[k + 1].x;
                                    avgvectorY = avgvectorY
                                            + foundTailLocation[k].y - foundTailLocation[k + 1].y;
                                }
                                avgvectorX = avgvectorX / tailLength;
                                avgvectorY = avgvectorY / tailLength;


                                //Extrapolate vector to empty region in maskimage
                                float startX = foundTailLocation[0].x;
                                float startY = foundTailLocation[0].y;
                                Point lastValidPoint = new Point();
                                Point curValidPoint = new Point();
                                lastValidPoint.x = foundTailLocation[0].x;
                                lastValidPoint.y = foundTailLocation[0].y;

                                while (true) {
                                    startX = (float) (startX + avgvectorX / 5);
                                    startY = (float) (startY + avgvectorY / 5);

                                    curValidPoint.x = Math.round(startX);
                                    curValidPoint.y = Math.round(startY);

                                    if (curValidPoint.x < 0 || curValidPoint.x > clipwidth - 1
                                            || curValidPoint.y < 0 || curValidPoint.y > clipheight - 1) {
                                        break;
                                    }


                                    if ((lastValidPoint.x != curValidPoint.x)
                                            || (lastValidPoint.y != curValidPoint.y)) {
                                        lastValidPoint.x = curValidPoint.x;
                                        lastValidPoint.y = curValidPoint.y;


                                        if (maskGrayArray[lastValidPoint.x][lastValidPoint.y] == 0) {
                                            break;
                                        }
                                        srcGrayArray[lastValidPoint.x][lastValidPoint.y] = 255;

                                    }

                                }
                            }

                        }
                    }
                }


            }
        }


        return srcGrayArray;
    }

    static BufferedImage convert_ByteProcessor_To_Image(ByteProcessor curProcessor) {
        short[][] imageClipArray = convert_ByteProcessor_To_GrayShortArray(curProcessor);

        BufferedImage bufImage = imgProc.convert_GrayShortArray_To_Image(imageClipArray);

        return bufImage;
    }

    static short[][] convert_ByteProcessor_To_GrayShortArray(ByteProcessor curProcessor) {
        int clipwidth = curProcessor.getWidth();
        int clipheight = curProcessor.getHeight();
        short[][] imageClipArray = new short[clipwidth][clipheight];
        for (int i = 0; i < clipwidth; i++) {
            for (int j = 0; j < clipheight; j++) {
                imageClipArray[i][j] = (short) curProcessor.getPixel(i, j);
            }
        }
        return imageClipArray;
    }
    
    
    
    /**
     * Save annotated assembled color image
     */
    public static void saveAssembledColorImage( List<WormInfo> wormDisplayList,
            ImagePlus assembled_org, String directoryLocation) {
        // crate the image
        ImageProcessor imageProcessor = assembled_org.getProcessor();
        TypeConverter typeConverter = new TypeConverter(imageProcessor, false);
        ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();
        final int defaultColor = Color.BLUE.getRGB();
        final int strokeWidth = 8;
        for (WormInfo worm : wormDisplayList) {
            int number = worm.nWorm;
            if (worm.changedN != null && worm.changedN != number) {
                number = worm.changedN;
            }; // if
            if (number == 0) {
                continue;
            }; // if
            int color = defaultColor;
            Roi roi = new Roi(worm.pX - strokeWidth, worm.pY - strokeWidth, worm.width + strokeWidth + 6, worm.height + strokeWidth + 6);
            colorProcessor.setRoi(roi);
            roi.setStrokeWidth(strokeWidth);
            colorProcessor.setValue(color);
            roi.drawPixels(colorProcessor);
        }; // for
        ImagePlus imagePlus = new ImagePlus(directoryLocation, colorProcessor);
        FileSaver fileSaver = new FileSaver(imagePlus);
        fileSaver.saveAsJpeg(directoryLocation + File.separator + App.ASSEMBLED_COLORS_JPEG );
    }
    
    
	 /** for testing only, it runs image-processing via command-line */
	 public static void main( String[] args ) {
		 if( args.length != 1 ) {
			 out.println( "USAGE: specify folder to use in image processing" );
			 return;
		 }; // if
		 File folder = new File( args[ 0 ] );
		 if( folder.exists() == false ) {
			 out.println( "Error, folder does not exist: " + args[ 0 ] );
			 return;
		 }; // if
		 if( folder.isDirectory() == false ) {
			 out.println( "Error, folder is not a valid directory: " + args[ 0 ] );
			 return;
		 }; // if
       //Load detection parameter setting profiles
    	 DetectionCondition detectionCondition = new DetectionCondition();
       String[] titleArray = detectionCondition.get_DetectionConditionPreset_Titles( App.DETECTION_CONDITION_PRESET_FILENAME);
		 out.println( "---- " + titleArray[ 1 ] );
       ImageProcessing.detectionCondition =
                detectionCondition.set_DetectionConditionPreset(
                App.DETECTION_CONDITION_PRESET_FILENAME,
                titleArray[1]);

       // does this folder contain images?
       int imageFilesCount = Utilities.countPiecesFiles(folder.getAbsolutePath());
       File theFile = new File(folder.getAbsolutePath() + File.separator + Utilities.ASSEMBLED_JPEG);
       boolean existingAssembleOrgFileFlag = theFile.exists();
        
       if (imageFilesCount > 0 || existingAssembleOrgFileFlag) {
            String error = imageProcessing(folder);
				if( error != null ) {
					out.println( "Error: " + error );
					return;
				}; // if
       }; // if
		 out.println( "done." );

	 }
    
} // class ImageProcessing

