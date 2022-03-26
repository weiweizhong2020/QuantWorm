/*
 * This class includes image processing algorithms powered by IJ
 */
package org.quantworm.wormgender;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import java.io.File;
import java.util.LinkedList;

public class IJImgProcessing {

    public static final String ASSEMBLED_JPEG = "assembled.jpeg";
    public static final String ASSEMBLED_ENHANCED_JPEG = "assembled_enhanced.jpeg";
    public static NativeImgProcessing imgProc = new NativeImgProcessing();

    /**
     * return the list of branching points in a give skeleton of worm
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     * @return
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
            }
        }

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
     * return the list of end points in a give skel of worm
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     * @return
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
            }
        }
        return endList;
    }

    /**
     * cropping the image
     *
     * @param ori The image to be cropped
     * @param xt The x coordinate of startpoint
     * @param yt The y coordinate of startpoint
     * @param width The width of the cropped image
     * @param height The height of the cropped image
     * @return
     */
    public static ImagePlus crop(ImageProcessor ori, int xt, int yt, int width, int height) {
        ImagePlus cut = NewImage.createByteImage("cut", width, height, 1, NewImage.GRAY8);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                cut.getProcessor().putPixel(i, j, ori.getPixel(xt + i, yt + j)); //copy pixels
            }
        }
        return cut;
    }

    /**
     * get the length (in micrometer) of worm by chain code retrieve
     *
     * @param skelWorm The binary image of the worm skeleton
     * @param foreground foreground Either be 255(white) or 0(black)
     * @return The length (um) of the worm(distance to diagonal neighborhood
     * pixel treated as sqrt(2))
     */
    public static double getTrueWormLen(ByteProcessor skelWorm, int foreground, Double stepsPerPixelsX, Double stepsPerPixelsY) {
        ByteProcessor template = (ByteProcessor) skelWorm.duplicate();

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

        double diagonalUnitLength
                = Math.sqrt(Math.pow(stepsPerPixelsX * ScannerLog.micronsPerStepX, 2)
                        + Math.pow(stepsPerPixelsY * ScannerLog.micronsPerStepY, 2));

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
                            trueLen += diagonalUnitLength;
                        } else if (i == lastW) {
                            trueLen += stepsPerPixelsY * ScannerLog.micronsPerStepY;
                        } else {
                            trueLen += stepsPerPixelsX * ScannerLog.micronsPerStepX;
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
        LinkedList<int[]> oriEndList = new LinkedList<int[]>();
        oriEndList = findEndPoints(skelWorm, foreground);

        while (times < maxTimes) {
            LinkedList<int[]> cordList = new LinkedList<int[]>();
            cordList = reportSpurPoints(skelWorm, foreground);
            for (int[] record : cordList) {
                skelWorm.putPixel(record[0], record[1], background);
            }
            times++;
        }

        LinkedList<int[]> endList = new LinkedList<int[]>();
        endList = findEndPoints(skelWorm, foreground);
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
                        } else {
                            fail++;
                            if (fail == 9) {
                                jump = 1;
                                break;
                            }
                        }
                    }
                    if (jump == 1) {
                        break;
                    }
                }
                if (jump == 1) {
                    break;
                }
            }
        }

    }

    /**
     * Return the list of spur points in a give skeleton of worm
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     * @return
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

            }
        }
        return cordList;
    }

    /**
     * Remove undesired points
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     */
    public static void trim(ByteProcessor skelWorm, int foreground) {
        trimSingleRound(skelWorm, foreground, 1);
        trimSingleRound(skelWorm, foreground, 2);
    }

    /**
     * Trim undesired points
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
            }
        }

    }

    /**
     * fill holes in binary image. Written by G.landini.
     *
     * @param ip The ImageProcessor of the binary image
     * @param foreground
     * @param background
     */
    public static void fill(ImageProcessor ip, int foreground, int background) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloodFiller ff = new FloodFiller(ip);
        ip.setColor(127);
        for (int y = 0; y < height; y++) {
            if (ip.getPixel(0, y) == background) {
                ff.fill(0, y);
            }
            if (ip.getPixel(width - 1, y) == background) {
                ff.fill(width - 1, y);
            }
        }
        for (int x = 0; x < width; x++) {
            if (ip.getPixel(x, 0) == background) {
                ff.fill(x, 0);
            }
            if (ip.getPixel(x, height - 1) == background) {
                ff.fill(x, height - 1);
            }
        }
        byte[] pixels = (byte[]) ip.getPixels();
        int n = width * height;
        for (int i = 0; i < n; i++) {
            if (pixels[i] == 127) {
                pixels[i] = (byte) background;
            } else {
                pixels[i] = (byte) foreground;
            }
        }
    }

    /**
     * Add padding to image. padded area has color black (0)
     *
     * @param srcGrayImage
     * @param paddingSize
     * @return
     */
    public static ImagePlus add_Padding(ImagePlus srcGrayImage, int paddingSize) {
        int srcImageWidth = srcGrayImage.getWidth();
        int srcImageHeight = srcGrayImage.getHeight();

        ImagePlus newImage = NewImage.createByteImage("padded",
                srcImageWidth + paddingSize * 2,
                srcImageHeight + paddingSize * 2,
                1, NewImage.FILL_BLACK);
        ByteProcessor newImageProc = (ByteProcessor) newImage.getProcessor();

        for (int y = 0; y < srcImageHeight; y++) {
            for (int x = 0; x < srcImageWidth; x++) {
                newImageProc.putPixel(x + paddingSize, y + paddingSize,
                        srcGrayImage.getPixel(x, y));
            }
        }

        return newImage;
    }

    /**
     * Create outline image from source binary image
     *
     * @param srcBinaryImage background is white (255) and object is black (0)
     * @param erodeRepeat
     * @return ImagePlus, outline is white (255)and backgroun is black (0)
     */
    public static ImagePlus create_OutlineImage(ImagePlus srcBinaryImage,
            int erodeRepeat) {

        ByteProcessor savedBinaryImage = (ByteProcessor) srcBinaryImage.getProcessor();

        for (int q = 0; q < erodeRepeat; q++) {
            savedBinaryImage.erode();
        }

        savedBinaryImage.invert();
        savedBinaryImage.outline();
        savedBinaryImage.invert();

        return (new ImagePlus("outline", savedBinaryImage));
    }

    /**
     * Overlay mask image onto source gray image and then return color
     * BufferedImage objectColorInMaskBinaryImage may be white (255)
     *
     * @param srcGrayImage
     * @param maskGrayImage
     * @param objectColorInMaskBinaryImage
     * @return
     */
    public static ColorProcessor overlay_MaskOnToSourceImage(
            ImagePlus srcGrayImage, ImagePlus maskGrayImage,
            int objectColorInMaskBinaryImage) {

        ImageProcessor srcGrayImageProc = srcGrayImage.getProcessor();
        ImageProcessor maskGrayImageProc = maskGrayImage.getProcessor();
        TypeConverter typeConverter = new TypeConverter(srcGrayImageProc, false);
        ColorProcessor colorProcessor = (ColorProcessor) typeConverter.convertToRGB();

        int maskWidth = maskGrayImage.getWidth();
        int maskHeight = maskGrayImage.getHeight();

        for (int i = 0; i < maskWidth; i++) {
            for (int j = 0; j < maskHeight; j++) {
                int[] rgb = new int[3];

                if (maskGrayImageProc.getPixel(i, j) == objectColorInMaskBinaryImage) {
                    rgb[0] = 255;
                    rgb[1] = 0;
                    rgb[2] = 0;
                } else {
                    rgb[0] = srcGrayImageProc.getPixel(
                            i, j);
                    rgb[1] = rgb[0];
                    rgb[2] = rgb[0];
                }
                colorProcessor.putPixel(i, j, rgb);
            }
        }

        return colorProcessor;
    }

    /**
     * Assembles image from an image in specified folder, but if it already
     * exists, then it just reads it from disk If assembled doesn't exist,
     * create file and then read the file
     *
     * @param folder the name of the folder
     * @param scannerLog the ScannerLog object
     * @return ImagePlus
     */
    public static ImagePlus assembleImage_Reload(String folder, ScannerLog scannerLog) {
        long time1 = System.currentTimeMillis();
        File theFile = new File(folder + File.separator + ASSEMBLED_JPEG);
        if (theFile.exists() == true) {
            ImagePlus assembled = new ImagePlus(folder + File.separator + ASSEMBLED_JPEG);
            return assembled;
        }

        int numberOfColumns = scannerLog.getNumberOfColumns();
        int numberOfRows = scannerLog.getNumberOfRows();

        int totalImagesInDisk = Utilities.countPiecesFiles(folder);
        if (totalImagesInDisk != (numberOfColumns * numberOfRows)) {
            return null;
        }

        // assemble from pieces
        ImagePlus assembled = NewImage.createByteImage("assembled", numberOfColumns * 640,
                numberOfRows * 480, 1, NewImage.FILL_BLACK);
        ImageProcessor ipAssembled = assembled.getProcessor();
        for (int i = 1; i <= numberOfColumns; i++) {
            for (int j = 1; j <= numberOfRows; j++) {
                String index = String.valueOf(i + numberOfColumns * (j - 1));
                String path = folder + File.separator + "piece_" + index + ".jpeg";
                ImagePlus tempIP = new ImagePlus(path);
                try {
                    ipAssembled.copyBits(tempIP.getProcessor(), 640 * (i - 1), 480 * (j - 1), Blitter.ADD);
                } catch (Exception ex) {
                }

            }
        }

        FileSaver saver = new FileSaver(assembled);
        saver.saveAsJpeg(folder + File.separator + ASSEMBLED_JPEG);

        ImagePlus assembled2 = new ImagePlus(folder + File.separator + ASSEMBLED_JPEG);

        long time2 = System.currentTimeMillis();
        System.out.println(Utilities.format2(((time2 - time1) / 1000.0)) + " seconds for assembling image");
        return assembled2;
    }

}
