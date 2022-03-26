/*
 * Filename: NativeImgProcessing.java
 */

package edu.rice.wormlab.lifespan;

/**
 * This class contains native image processing modules
 */
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import javax.imageio.ImageIO;

public class NativeImgProcessing {

    LinkedList<Point> FloodFillQue = new LinkedList<Point>();
    //RegionLabeling_ColorTable: The first dimension for IDnumber (starts from 1)
    //                           The second dimension for color value
    //                                      0:R, 1:G, 2:B
    public short[][] RegionLabeling_ColorTable;
    public short[][] RegionLabeling_LabelIDMap;
    public float[][] LUT_RGB_To_PartialGray = new float[4][256];
    public boolean[][] _FloodFilledDoneMap = new boolean[1][1];
    public BufferedImage MagicThreshold_BWImage;
    public BufferedImage AdaptiveThreshold_BWImage;
    public short[][] AdaptiveThreshold_BWPixelArray;
    public int MagicThreshold_OptimalThresholdValue;
    public int AdaptiveThreshold_OptimalThresholdValue;
    public final float AdaptiveThreshold_tPercent = 0.3f;

    
    //Initialzing
    //Create look-up-table for RGB conversion into gray
    public NativeImgProcessing() {
        int i;

        //Generate LUT for gray scaling
        //Formula: Gray=(0.299 * r) + (0.587 * g) + (0.114 * b)
        for (i = 0; i <= 255; i++) {
            LUT_RGB_To_PartialGray[1][i] = (float) (0.299 * i);
            LUT_RGB_To_PartialGray[2][i] = (float) (0.587 * i);
            LUT_RGB_To_PartialGray[3][i] = (float) (0.114 * i);
        }
    }

    /**
     * Load source image
     *
     * @param ref source file name including full path
     * @return BufferedImage
     */
    public BufferedImage loadImage(String ref) {
        BufferedImage bimg;
        try {

            bimg = ImageIO.read(new File(ref));
        } catch (Exception e) {
            bimg = null;
        }
        return bimg;
    }

    /**
     * Save source image to destination
     *
     * @param srcImage source image
     * @param fileName file name including full path
     * @return nothing
     */
    public void saveImage(BufferedImage srcImage, String imgFormat, String fileName) {
        try {
            ImageIO.write(srcImage, imgFormat, new File(fileName));
        } catch (IOException e) {
            //do nothing
        }
    }

    
    //Resize image    
    public static BufferedImage resizeImage(Image originalImage, int type,
            Integer img_width, Integer img_height) {
        BufferedImage resizedImage = new BufferedImage(img_width, img_height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, img_width, img_height, null);
        g.dispose();

        return resizedImage;
    }

    /**
     * Convert RGB array to image
     *
     * @param SrcRGBShortArray source RGB array
     * @return BufferedImage
     */
    public BufferedImage convert_RGBShortArray_To_Image(short[][][] SrcRGBShortArray) {
        int x_upperBound = SrcRGBShortArray.length - 1;
        int y_upperBound = SrcRGBShortArray[0].length - 1;
        int rgb;


        BufferedImage outImage = new BufferedImage(x_upperBound + 1,
                y_upperBound + 1, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                rgb = new Color(SrcRGBShortArray[x][y][0],
                        SrcRGBShortArray[x][y][1],
                        SrcRGBShortArray[x][y][2]).getRGB();
                outImage.setRGB(x, y, rgb);
            }
        }
        return outImage;
    }

    /**
     * Get RGB array from image
     *
     * @param SrcImage source image
     * @return output array
     */
    public short[][][] convert_Image_To_RGBShortArray(BufferedImage SrcImage) {
        int x_upperBound = SrcImage.getWidth();
        int y_upperBound = SrcImage.getHeight();
        int red, green, blue;


        short[][][] outShortArray = new short[x_upperBound][y_upperBound][3];

        for (int y = 0; y < y_upperBound; y++) {
            for (int x = 0; x < x_upperBound; x++) {
                int rgb = SrcImage.getRGB(x, y);
                red = (rgb >> 16) & 0x000000FF;
                green = (rgb >> 8) & 0x000000FF;
                blue = (rgb) & 0x000000FF;

                outShortArray[x][y][0] = (short) red;
                outShortArray[x][y][1] = (short) green;
                outShortArray[x][y][2] = (short) blue;
            }
        }
        return outShortArray;
    }

    /**
     * Convert gray array to image
     *
     * @param SrcGrayShortArray source array
     * @return BufferedImage
     */
    public BufferedImage convert_GrayShortArray_To_Image(short[][] SrcGrayShortArray) {
        int x_upperBound = SrcGrayShortArray.length - 1;
        int y_upperBound = SrcGrayShortArray[0].length - 1;
        int rgb;
        short grayValue;

        BufferedImage outImage = new BufferedImage(x_upperBound + 1,
                y_upperBound + 1, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                grayValue = SrcGrayShortArray[x][y];
                if (grayValue > 255) {
                    grayValue = 255;
                } else {
                    if (grayValue < 0) {
                        grayValue = 0;
                    }
                }

                rgb = new Color(grayValue, grayValue,
                        grayValue).getRGB();
                outImage.setRGB(x, y, rgb);
            }
        }
        return outImage;
    }

    /**
     * Convert RGB to Gray value
     *
     * @return BufferedImage
     */
    public short RGBToGray(int r, int g, int b) {
        return (short) Math.rint(LUT_RGB_To_PartialGray[1][r]
                + LUT_RGB_To_PartialGray[2][g]
                + LUT_RGB_To_PartialGray[3][b]);
    }

    /**
     * Convert gray array to RGB array
     *
     * @param SrcGrayShortArray source array
     * @return short array
     */
    public short[][][] convert_GrayShortArray_To_RGBShortArray(short[][] SrcGrayShortArray) {
        int x_upperBound = SrcGrayShortArray.length - 1;
        int y_upperBound = SrcGrayShortArray[0].length - 1;

        short[][][] OutRGBShortArray =
                new short[SrcGrayShortArray.length][SrcGrayShortArray[0].length][3];


        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                OutRGBShortArray[x][y][0] = SrcGrayShortArray[x][y];
                OutRGBShortArray[x][y][1] = SrcGrayShortArray[x][y];
                OutRGBShortArray[x][y][2] = SrcGrayShortArray[x][y];
            }
        }

        return OutRGBShortArray;
    }

    /**
     * Convert RGB array to gray array
     *
     * @param SrcRGBShortArray source RGB array
     * @return Gray array
     */
    public short[][] convert_RGBShortArray_To_GrayShortArray(short[][][] SrcRGBShortArray) {
        int x_upperBound = SrcRGBShortArray.length - 1;
        int y_upperBound = SrcRGBShortArray[0].length - 1;

        short[][] OutGrayShortArray =
                new short[SrcRGBShortArray.length][SrcRGBShortArray[0].length];


        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                OutGrayShortArray[x][y] = RGBToGray(SrcRGBShortArray[x][y][0],
                        SrcRGBShortArray[x][y][1], SrcRGBShortArray[x][y][2]);
            }
        }

        return OutGrayShortArray;
    }

    /**
     * Convert image to gray array
     *
     * @param SrcImage source image
     * @return Gray array
     */
    public short[][] convert_Image_To_GrayShortArray(BufferedImage SrcImage) {
        int x_upperBound = SrcImage.getWidth();
        int y_upperBound = SrcImage.getHeight();
        int rgb, red, green, blue;


        short[][] outShortArray = new short[x_upperBound][y_upperBound];

        for (int y = 0; y < y_upperBound; y++) {
            for (int x = 0; x < x_upperBound; x++) {
                rgb = SrcImage.getRGB(x, y);
                red = (rgb >> 16) & 0x000000FF;
                green = (rgb >> 8) & 0x000000FF;
                blue = (rgb) & 0x000000FF;

                outShortArray[x][y] = RGBToGray(red, green, blue);
            }
        }
        return outShortArray;
    }

    /**
     * copy of multiple array, what we call deepCopy
     *
     * @param source source array
     * @return array
     */
    public short[][] multiArrayCopy(short[][] source) {
        short[][] destination = new short[source.length][source[0].length];

        for (int a = 0; a < source.length; a++) {
            System.arraycopy(source[a], 0, destination[a], 0, source[a].length);
        }

        return destination;
    }
    
    public int[][] multiArrayCopy(int[][] source) {
        int[][] destination = new int[source.length][source[0].length];

        for (int a = 0; a < source.length; a++) {
            System.arraycopy(source[a], 0, destination[a], 0, source[a].length);
        }

        return destination;
    }

    /**
     * copy of multiple array, what we call deepCopy
     *
     * @param source source array
     * @return array
     */
    public short[][][] multiArrayCopy(short[][][] source) {
        short[][][] destination = new short[source.length][source[0].length][source[0][0].length];

        for (int a = 0; a < source.length; a++) {
            for (int b = 0; a < source[0].length; a++) {
                System.arraycopy(source[a][b], 0, destination[a][b], 0, source[a][b].length);
            }
        }

        return destination;
    }

    public int[][][] multiArrayCopy(int[][][] source) {
        int[][][] destination = new int[source.length][source[0].length][source[0][0].length];

        for (int a = 0; a < source.length; a++) {
            for (int b = 0; a < source[0].length; a++) {
                System.arraycopy(source[a][b], 0, destination[a][b], 0, source[a][b].length);
            }
        }

        return destination;
    }

    /**
     * gap filling
     *
     * @param length source array
     * @return array
     */
    public short[][] gapFilling(short[][] SrcGrayShortArray) {
        int SrcImgWidth = SrcGrayShortArray.length - 1;
        int SrcImgHeight = SrcGrayShortArray[0].length - 1;
        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);


        for (int y = 1; y <= SrcImgHeight - 1; y++) {
            for (int x = 1; x <= SrcImgWidth - 1; x++) {
                if (outPixels[x][y] == 0) {
                    if (outPixels[x][y - 1] == 255 && outPixels[x][y + 1] == 255) {
                        outPixels[x][y] = 255;
                    }

                    if (outPixels[x - 1][y] == 255 && outPixels[x + 1][y] == 255) {
                        outPixels[x][y] = 255;
                    }
                }
            }
        }

        return outPixels;
    }

    /**
     * flood filling
     *
     * @param SrcGrayShortArray source array
     * @param CenterX center x
     * @param CenterY center y
     * @return array
     */
    public short[][] floodFill(short[][] SrcGrayShortArray, int CenterX, int CenterY,
            short FillColorG, int ToleranceG) {
        int SrcImageWidth = SrcGrayShortArray.length;
        int SrcImageHeight = SrcGrayShortArray[0].length;
        short[][] outArray;

        _FloodFilledDoneMap = new boolean[SrcImageWidth][SrcImageHeight];


        outArray = floodFill_Core(SrcGrayShortArray, CenterX, CenterY, FillColorG, ToleranceG);

        return outArray;

    }

    /**
     * flood filling, core routine
     *
     * @param SrcGrayShortArray source array
     * @param CenterX center x
     * @param CenterY center y
     * @param FillColorG fill color in gray
     * @param ToleranceG tolerance
     * @return array
     */
    public short[][] floodFill_Core(short[][] SrcGrayShortArray, int CenterX, int CenterY,
            short FillColorG, int ToleranceG) {


        int SrcImageWidth = SrcGrayShortArray.length;
        int SrcImageHeight = SrcGrayShortArray[0].length;


        Point Cur_Location = new Point();
        Point Upper_Location;
        Point Lower_Location;
        Point Left_Location;
        Point Right_Location;
        int CenterColor_G;

        boolean[][] QueueMap = new boolean[SrcImageWidth][SrcImageHeight];

        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);


        Cur_Location.x = CenterX;
        Cur_Location.y = CenterY;
        FloodFillQue.add(Cur_Location);
        QueueMap[Cur_Location.x][Cur_Location.y] = true;
        _FloodFilledDoneMap[Cur_Location.x][Cur_Location.y] = true;

        CenterColor_G = SrcGrayShortArray[Cur_Location.x][Cur_Location.y];

        do {
            Cur_Location.x = FloodFillQue.peek().x;
            Cur_Location.y = FloodFillQue.peek().y;

            outPixels[Cur_Location.x][Cur_Location.y] = FillColorG;
            _FloodFilledDoneMap[Cur_Location.x][Cur_Location.y] = true;



            FloodFillQue.poll();
            QueueMap[Cur_Location.x][Cur_Location.y] = false;


            Upper_Location = new Point(Cur_Location.x, Cur_Location.y - 1);
            Lower_Location = new Point(Cur_Location.x, Cur_Location.y + 1);
            Left_Location = new Point(Cur_Location.x - 1, Cur_Location.y);
            Right_Location = new Point(Cur_Location.x + 1, Cur_Location.y);



            if (Cur_Location.y == 0) {
                Upper_Location.x = -1;
            }

            if (Cur_Location.y == SrcImageHeight - 1) {
                Lower_Location.x = -1;
            }

            if (Cur_Location.x == 0) {
                Left_Location.x = -1;
            }

            if (Cur_Location.x == SrcImageWidth - 1) {
                Right_Location.x = -1;
            }

            if (Upper_Location.x != -1) {
                if (QueueMap[Upper_Location.x][Upper_Location.y] == false) {
                    if (_FloodFilledDoneMap[Upper_Location.x][Upper_Location.y] == false
                            && Math.abs(CenterColor_G
                            - SrcGrayShortArray[Upper_Location.x][Upper_Location.y]) <= ToleranceG) {
                        FloodFillQue.add(Upper_Location);
                        QueueMap[Upper_Location.x][Upper_Location.y] = true;
                    }
                }
            }


            if (Lower_Location.x != -1) {
                if (QueueMap[Lower_Location.x][Lower_Location.y] == false) {
                    if (_FloodFilledDoneMap[Lower_Location.x][Lower_Location.y] == false
                            && Math.abs(CenterColor_G
                            - SrcGrayShortArray[Lower_Location.x][Lower_Location.y]) <= ToleranceG) {
                        FloodFillQue.add(Lower_Location);
                        QueueMap[Lower_Location.x][Lower_Location.y] = true;
                    }
                }
            }


            if (Right_Location.x != -1) {
                if (QueueMap[Right_Location.x][Right_Location.y] == false) {
                    if (_FloodFilledDoneMap[Right_Location.x][Right_Location.y] == false
                            && Math.abs(CenterColor_G
                            - SrcGrayShortArray[Right_Location.x][Right_Location.y]) <= ToleranceG) {
                        FloodFillQue.add(Right_Location);
                        QueueMap[Right_Location.x][Right_Location.y] = true;
                    }
                }
            }

            if (Left_Location.x != -1) {
                if (QueueMap[Left_Location.x][Left_Location.y] == false) {
                    if (_FloodFilledDoneMap[Left_Location.x][Left_Location.y] == false
                            && Math.abs(CenterColor_G
                            - SrcGrayShortArray[Left_Location.x][Left_Location.y]) <= ToleranceG) {
                        FloodFillQue.add(Left_Location);
                        QueueMap[Left_Location.x][Left_Location.y] = true;
                    }
                }
            }


        } while (FloodFillQue.size() != 0);


        return outPixels;
    }

    /**
     * negative (invert)
     *
     * @param SrcGrayShortArray source array
     * @return array
     */
    public short[][] negative_UsingGrayShortArray(short[][] SrcGrayShortArray) {
        int SrcImgWidth = SrcGrayShortArray.length - 1;
        int SrcImgHeight = SrcGrayShortArray[0].length - 1;
        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);


        for (int y = 0; y <= SrcImgHeight; y++) {
            for (int x = 0; x <= SrcImgWidth; x++) {
                outPixels[x][y] = (short) (255 - outPixels[x][y]);
            }
        }

        return outPixels;
    }

    /**
     * negative (invert)
     *
     * @param SrcRGBShortArray RGB source array
     * @return array
     */
    public short[][][] negative_UsingRGBShortArray(short[][][] SrcRGBShortArray) {
        int SrcImgWidth = SrcRGBShortArray.length - 1;
        int SrcImgHeight = SrcRGBShortArray[0].length - 1;
        short[][][] outPixels = multiArrayCopy(SrcRGBShortArray);


        for (int y = 0; y <= SrcImgHeight; y++) {
            for (int x = 0; x <= SrcImgWidth; x++) {
                outPixels[x][y][0] = (short) (255 - outPixels[x][y][0]);
                outPixels[x][y][1] = (short) (255 - outPixels[x][y][1]);
                outPixels[x][y][2] = (short) (255 - outPixels[x][y][2]);
            }
        }

        return outPixels;
    }

    /**
     * Conduct region labeling
     *
     * @param SrcImage source image
     * @param BkGrayColor background gray color (0~255)
     * @return image
     */
    public BufferedImage regionExtract_RasterScanning(BufferedImage SrcImage, int BkGrayColor) {
        short[][] SrcPixels;

        SrcPixels = convert_Image_To_GrayShortArray(SrcImage);

        return regionExtract_RasterScanning(SrcPixels, BkGrayColor);
    }

    /**
     * Conduct region labeling
     *
     * @param SrcPixels gray array
     * @param BkGrayColor background gray color (0~255)
     * @return array in RGB
     */
    public BufferedImage regionExtract_RasterScanning(short[][] SrcPixels, int BkGrayColor) {
        short[][] OutPixels;
        int x, y;
        short[][][] OutPixels_C;

        OutPixels = multiArrayCopy(SrcPixels);


        int SrcBitmapWidth = SrcPixels.length;
        int SrcBitmapHeight = SrcPixels[0].length;
        Random randomGenerator = new Random();

        OutPixels_C = new short[SrcBitmapWidth][SrcBitmapHeight][3];


        short[] Linked1D;
        boolean[][] Linked2D;
        short CurrentID;
        int IDCountMax = 10000;
        short[][] IDMap = new short[SrcBitmapWidth][SrcBitmapHeight];
        int Pixel_Center;
        int Pixel_1;
        int Pixel_2;
        int Pixel_3;
        int Pixel_4;
        int BitmapHeightUpper;
        int BitmapWidthUpper;
        int BorderHeightUpper;
        int BorderWidthUpper;
        int BoxSizeHalf = 1;


        BitmapHeightUpper = SrcBitmapHeight - 1;
        BitmapWidthUpper = SrcBitmapWidth - 1;
        BorderHeightUpper = SrcBitmapHeight - BoxSizeHalf - 1;
        BorderWidthUpper = SrcBitmapWidth - BoxSizeHalf - 1;



        //Initialize IDmap
        for (y = 0; y <= BitmapHeightUpper; y++) {
            for (x = 0; x <= BitmapWidthUpper; x++) {
                IDMap[x][y] = 0;
            }
        }

        //First pass
        CurrentID = 0;
        for (y = BoxSizeHalf; y <= BorderHeightUpper; y++) {
            for (x = BoxSizeHalf; x <= BorderWidthUpper; x++) {

                if (SrcPixels[x][y] != BkGrayColor) {


                    Pixel_Center = IDMap[x][y];
                    Pixel_1 = IDMap[x - 1][y - 1];
                    Pixel_2 = IDMap[x][y - 1];
                    Pixel_3 = IDMap[x + 1][y - 1];
                    Pixel_4 = IDMap[x - 1][y];


                    if (Pixel_1 == 0 && Pixel_2 == 0 && Pixel_3 == 0 && Pixel_4 == 0) {

                        if (CurrentID < IDCountMax) {
                            CurrentID = (short) (CurrentID + 1);
                            IDMap[x][y] = CurrentID;
                        }
                    } else {
                        IDMap[x][y] =
                                (short) get_MinValue(Pixel_Center,
                                Pixel_1, Pixel_2, Pixel_3, Pixel_4);
                    }
                }
            }

        }

        int Repeat_Twice;
        int MinValue;
        Linked1D = new short[CurrentID + 1];
        Linked2D = new boolean[CurrentID + 1][CurrentID + 1];

        for (Repeat_Twice = 1; Repeat_Twice <= 2; Repeat_Twice++) {

            //Second pass and record their equivalence relationship
            for (y = 0; y <= BorderHeightUpper; y++) {
                for (x = 0; x <= BorderWidthUpper; x++) {

                    if (IDMap[x][y] != 0) {

                        Pixel_Center = IDMap[x][y];
                        Pixel_1 = IDMap[x + 1][y];
                        Pixel_2 = IDMap[x][y + 1];
                        Pixel_3 = IDMap[x + 1][y + 1];
                        Pixel_4 = IDMap[x - 1][y + 1];


                        Linked2D[Pixel_Center][Pixel_Center] = true;

                        if (Pixel_1 != 0) {
                            Linked2D[Pixel_Center][Pixel_1] = true;
                            Linked2D[Pixel_1][Pixel_Center] = true;
                        }
                        if (Pixel_2 != 0) {
                            Linked2D[Pixel_Center][Pixel_2] = true;
                            Linked2D[Pixel_2][Pixel_Center] = true;
                        }
                        if (Pixel_3 != 0) {
                            Linked2D[Pixel_Center][Pixel_3] = true;
                            Linked2D[Pixel_3][Pixel_Center] = true;
                        }
                        if (Pixel_4 != 0) {
                            Linked2D[Pixel_Center][Pixel_4] = true;
                            Linked2D[Pixel_4][Pixel_Center] = true;
                        }
                    }
                }

            }

            //Reduce equivalence relationship
            for (x = 1; x <= CurrentID; x++) {

                MinValue = x;
                Linked1D[x] = (short) x;

                for (y = x; y >= 1; y--) {

                    if (Linked2D[x][y]) {
                        if (MinValue > y) {
                            MinValue = y;
                        }
                    }
                }

                Linked1D[x] = Linked1D[MinValue];
            }



            //Remove equivalent relationship
            for (y = BoxSizeHalf; y <= BorderHeightUpper; y++) {
                for (x = BoxSizeHalf; x <= BorderWidthUpper; x++) {
                    IDMap[x][y] = Linked1D[IDMap[x][y]];
                }
            }
        }

        //Setup color code randomly
        int q;
        int w;
        boolean IsExistSameColor;
        short[][] ColorCodeTable = new short[CurrentID + 1][3];


        for (q = 1; q <= CurrentID; q++) {
            do {
                do {
                    ColorCodeTable[q][0] = (short) (randomGenerator.nextInt(255));
                    ColorCodeTable[q][1] = (short) (randomGenerator.nextInt(255));
                    ColorCodeTable[q][2] = (short) (randomGenerator.nextInt(255));
                } while (ColorCodeTable[q][0] == 0 && ColorCodeTable[q][1] == 0
                        && ColorCodeTable[q][2] == 0);


                IsExistSameColor = false;
                for (w = 1; w < q; w++) {
                    if (ColorCodeTable[q][0] == ColorCodeTable[w][0]
                            && ColorCodeTable[q][1] == ColorCodeTable[w][1]
                            && ColorCodeTable[q][2] == ColorCodeTable[w][2]) {
                        IsExistSameColor = true;
                        break;
                    }
                }


            } while (IsExistSameColor == true);
        }

        //Draw final image
        for (y = BoxSizeHalf; y <= BorderHeightUpper; y++) {
            for (x = BoxSizeHalf; x <= BorderWidthUpper; x++) {
                OutPixels[x][y] = ColorCodeTable[IDMap[x][y]][0];
                OutPixels_C[x][y][0] = ColorCodeTable[IDMap[x][y]][0];
                OutPixels_C[x][y][1] = ColorCodeTable[IDMap[x][y]][1];
                OutPixels_C[x][y][2] = ColorCodeTable[IDMap[x][y]][2];

            }
        }

        RegionLabeling_LabelIDMap = IDMap;
        RegionLabeling_ColorTable = ColorCodeTable;

        return convert_RGBShortArray_To_Image(OutPixels_C);
    }

    /**
     * find min value
     *
     */
    public final float get_MinValue(float Value1, float Value2,
            float Value3, float Value4, float Value5) {
        float CurMin;

        CurMin = get_MaxValue(Value1, Value2, Value3, Value4, Value5);
        //If CurMin <> 1 Then Stop
        if (Value1 != 0F) {
            CurMin = Math.min(CurMin, Value1);
        }

        if (Value2 != 0F) {
            CurMin = Math.min(CurMin, Value2);
        }

        if (Value3 != 0F) {
            CurMin = Math.min(CurMin, Value3);
        }

        if (Value4 != 0F) {
            CurMin = Math.min(CurMin, Value4);
        }

        if (Value5 != 0F) {
            CurMin = Math.min(CurMin, Value5);
        }

        return CurMin;
    }

    /**
     * find max value
     *
     */
    public final float get_MaxValue(float Value1, float Value2,
            float Value3, float Value4, float Value5) {
        float CurMax;

        CurMax = Value1;
        CurMax = Math.max(CurMax, Value2);
        CurMax = Math.max(CurMax, Value3);
        CurMax = Math.max(CurMax, Value4);
        CurMax = Math.max(CurMax, Value5);

        return CurMax;
    }

    /**
     * Conduct simple global thresholding
     *
     * @param SourceImage source image
     * @param ThresholdValue threshold value in gray (0~255)
     * @param IsInvertedImage if true, conduct inversion
     * @return image
     */
    public BufferedImage bwLeveling(BufferedImage SourceImage, int ThresholdValue,
            boolean IsInvertedImage) {

        short[] LUT_Digitization = new short[256];
        int i;
        short[][] SrcPixels, OutPixels;
        int SrcBitmapWidth = SourceImage.getWidth();
        int SrcBitmapHeight = SourceImage.getHeight();


        SrcPixels = convert_Image_To_GrayShortArray(SourceImage);
        OutPixels = multiArrayCopy(SrcPixels);



        if (ThresholdValue < 1) {
            ThresholdValue = 1;
        }
        if (ThresholdValue > 255) {
            ThresholdValue = 255;
        }


        //Generate LUT
        if (IsInvertedImage == false) {
            for (i = 0; i <= 255; i++) {
                if (i > ThresholdValue) {
                    LUT_Digitization[i] = 255;
                } else {
                    LUT_Digitization[i] = 0;
                }
            }
        } else {
            for (i = 0; i <= 255; i++) {
                if (i > ThresholdValue) {
                    LUT_Digitization[i] = 0;
                } else {
                    LUT_Digitization[i] = (short) 255;
                }
            }
        }

        short Gray;

        for (int y = 0; y <= SrcBitmapHeight - 1; y++) {
            for (int x = 0; x <= SrcBitmapWidth - 1; x++) {
                Gray = LUT_Digitization[SrcPixels[x][y]];
                OutPixels[x][y] = Gray;
            }
        }

        return convert_GrayShortArray_To_Image(OutPixels);
    }

    /**
     * Deep copy of image
     *
     * @param bi source image
     * @return image
     */
    public BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * magic thresholding
     *
     * @return image
     */
    public BufferedImage magicThresholding_Core(BufferedImage SourceImage,
            int ExcludeGrayRange_Min, int ExcludeGrayRange_Max) {


        short[][] SrcPixels, OutPixels;
        int SrcBitmapWidth = SourceImage.getWidth();
        int SrcBitmapHeight = SourceImage.getHeight();


        SrcPixels = convert_Image_To_GrayShortArray(SourceImage);
        OutPixels = new short[SrcBitmapWidth][SrcBitmapHeight];


        _FloodFilledDoneMap = new boolean[SrcBitmapWidth][SrcBitmapHeight];
        int ToleranceOfExclusion = 20;

        for (int y = 0; y < SrcBitmapHeight; y++) {

            for (int x = 0; x < SrcBitmapWidth; x++) {


                if (_FloodFilledDoneMap[x][y] == false) {
                    if (SrcPixels[x][y] >= ExcludeGrayRange_Min
                            && SrcPixels[x][y] <= ExcludeGrayRange_Max) {

                        floodFill_Core(SrcPixels, x, y,
                                (short) 255, ToleranceOfExclusion);
                    }
                }

            }
        }


        for (int y = 0; y < SrcBitmapHeight; y++) {

            for (int x = 0; x < SrcBitmapWidth; x++) {
                if (_FloodFilledDoneMap[x][y]) {
                    OutPixels[x][y] = 0;
                } else {
                    OutPixels[x][y] = 255;
                }

            }

        }

        return convert_GrayShortArray_To_Image(OutPixels);
    }

    /**
     * Create integral image
     *
     * @param SrcGrayArrayMap source gray array
     * @return array
     */
    public long[][] build_IntegralGrayArrayMap(short[][] SrcGrayArrayMap) {
        int SrcBitmapWidth = SrcGrayArrayMap.length;
        int SrcBitmapHeight = SrcGrayArrayMap[0].length;


        long[][] IntegralGrayArrayMap = new long[SrcBitmapWidth][SrcBitmapHeight];

        int x1 = 0;
        int x2 = SrcBitmapWidth - 1;
        int y1 = 0;
        int y2 = SrcBitmapHeight - 1;



        IntegralGrayArrayMap[x1][y1] = SrcGrayArrayMap[x1][y1];
        for (int x = x1 + 1; x <= x2; x++) {
            IntegralGrayArrayMap[x][y1] = IntegralGrayArrayMap[x - 1][y1]
                    + SrcGrayArrayMap[x1][y1];
        }
        //Computing integral sum of the first column
        for (int y = y1 + 1; y <= y2; y++) {
            IntegralGrayArrayMap[x1][y] = IntegralGrayArrayMap[x1][y - 1]
                    + SrcGrayArrayMap[x1][y1];
        }

        //Computing integral sum for the rest of points
        for (int y = y1 + 1; y <= y2; y++) {
            for (int x = x1 + 1; x <= x2; x++) {
                IntegralGrayArrayMap[x][y] = SrcGrayArrayMap[x][y]
                        + IntegralGrayArrayMap[x - 1][y]
                        + IntegralGrayArrayMap[x][y - 1]
                        - IntegralGrayArrayMap[x - 1][y - 1];

            }
        }

        return IntegralGrayArrayMap;
    }

    /**
     * Calculate average gray value for given selection region
     *
     * @param IntegralGrayImageMap integral gray map
     * @param LocalBoxSize box size for processing
     * @param CenterX center of selection box
     * @param CenterY center of selection box
     * @return optimal threshold value
     */
    private int calculate_LocalGrayAvgValueUsingIntegralMap(long[][] IntegralGrayImageMap,
            int LocalBoxSize, int CenterX, int CenterY) {
        long LocalBoxSum = 0;
        int LocalBoxAvg = 0;
        int LocalBoxX1 = 0;
        int LocalBoxX2 = 0;
        int LocalBoxY1 = 0;
        int LocalBoxY2 = 0;
        int LocalBoxHalfSize = (int) ((LocalBoxSize - 1) / 2.0);
        int ImageWidth = 0;
        int ImageHeight = 0;

        ImageWidth = IntegralGrayImageMap.length - 1 + 1;
        ImageHeight = IntegralGrayImageMap[0].length - 1 + 1;

        LocalBoxX1 = Math.max(CenterX - LocalBoxHalfSize, 0);
        LocalBoxY1 = Math.max(CenterY - LocalBoxHalfSize, 0);

        LocalBoxX2 = Math.min(CenterX + LocalBoxHalfSize, ImageWidth - 1);
        LocalBoxY2 = Math.min(CenterY + LocalBoxHalfSize, ImageHeight - 1);


        LocalBoxSum = IntegralGrayImageMap[LocalBoxX2][LocalBoxY2];
        if (LocalBoxY1 - 1 >= 0) {
            LocalBoxSum -= IntegralGrayImageMap[LocalBoxX2][LocalBoxY1 - 1];
        }
        if (LocalBoxX1 - 1 >= 0) {
            LocalBoxSum -= IntegralGrayImageMap[LocalBoxX1 - 1][LocalBoxY2];
        }
        if (LocalBoxX1 - 1 >= 0 && LocalBoxY1 - 1 >= 0) {
            LocalBoxSum += IntegralGrayImageMap[LocalBoxX1 - 1][LocalBoxY1 - 1];
        }


        LocalBoxAvg = (int) (LocalBoxSum / (double) ((LocalBoxX2 - LocalBoxX1 + 1) * (LocalBoxY2 - LocalBoxY1 + 1)));

        return LocalBoxAvg;
    }

    /**
     * Conduct adaptive thresholding
     *
     * @param SrcPixels source gray image
     * @param boxSize box size for processing
     * @param tpercent t percent value for comparison between gray value and
     * average gray value of selected box
     * @param GrayLimit pixels whose gray value is higher than GrayLimit will be
     * excluded
     * @return optimal threshold value
     */
    public BufferedImage adaptiveThresholding_Core(short[][] SrcPixels,
            int boxSize, float tpercent,
            int GrayLimit) {


        short[][] OutPixels;
        int SrcBitmapWidth = SrcPixels.length;
        int SrcBitmapHeight = SrcPixels[0].length;

        OutPixels = new short[SrcBitmapWidth][SrcBitmapHeight];

        long[][] temp_IntegralGrayMap;
        int LocalGrayAvg = 0;
        float rPercentValue = (float) (1 - tpercent);

        temp_IntegralGrayMap = build_IntegralGrayArrayMap(SrcPixels);


        for (int y = 0; y < SrcBitmapHeight; y++) {
            for (int x = 0; x < SrcBitmapWidth; x++) {

                LocalGrayAvg = calculate_LocalGrayAvgValueUsingIntegralMap(
                        temp_IntegralGrayMap, boxSize, x, y);
                if (SrcPixels[x][y] < LocalGrayAvg * rPercentValue && SrcPixels[x][y] < GrayLimit) {
                    OutPixels[x][y] = 255;
                } else {
                    OutPixels[x][y] = 0;
                }

            }
        }
        AdaptiveThreshold_BWPixelArray = OutPixels;
        return convert_GrayShortArray_To_Image(OutPixels);
    }

    /**
     * Find optimal threshold value for magic thresholding Optimal threshold
     * value will be one of 100, 75, and 50.
     *
     * @param SourceGrayImage source gray image
     * @return optimal threshold value
     */
    public int find_OptimalThreshold_For_MagicThresholding(BufferedImage SourceGrayImage) {
        BufferedImage BWImage;
        int TestValue;

        float[][] BasicAnalysis;
        int NumbValidBlob;
        boolean IsGoodValueFound = false;

        BWImage = null;

        //Find optimal threshold valueL maximum three values will be tested (100,75, 50)
        for (TestValue = 100; TestValue >= 50; TestValue -= 25) {
            //Run magic threshold
            BWImage = magicThresholding_Core(SourceGrayImage, TestValue, 255);

            //conduct region lanbeling
            regionExtract_RasterScanning(BWImage, 0);

            //Conduct basic analysis of detected regions such as center x and y, size
            BasicAnalysis = regionExtract_BasicAnalysis(RegionLabeling_LabelIDMap,
                    RegionLabeling_ColorTable);


            //Count number of blobs whose size is higher than 20 pixels
            NumbValidBlob = 0;
            if (BasicAnalysis.length > 0) {
                for (int tt = 1; tt <= BasicAnalysis.length - 1; tt++) {
                    if (BasicAnalysis[tt][2] > 20) {
                        NumbValidBlob += 1;
                    }
                }
            }


            if (NumbValidBlob < 100) {
                MagicThreshold_BWImage = BWImage;
                return TestValue;
            }
        }


        if (IsGoodValueFound == false) {
            MagicThreshold_BWImage = BWImage;
            return 50;
        }

        return 50;
    }

    /**
     * Find optimal threshold value for adaptive thresholding Optimal threshold
     * value will be between 150 to 50 with step size -10.
     *
     * @param SourceGrayImage source gray image
     * @return optimal threshold value
     */
    public int find_OptimalThreshold_For_AdaptiveThresholding(BufferedImage SourceGrayImage) {
        short[][] SrcPixels = convert_Image_To_GrayShortArray(SourceGrayImage);
        int returnValue = find_OptimalThreshold_For_AdaptiveThresholding(SrcPixels);

        return returnValue;
    }

    /**
     * Find optimal threshold value for adaptive thresholding Optimal threshold
     * value will be between 150 to 50 with step size -10.
     *
     * @param SrcGrayPixelArray source gray array
     * @return optimal threshold value
     */
    public int find_OptimalThreshold_For_AdaptiveThresholding(short[][] SrcGrayPixelArray) {
        BufferedImage BWImage = null;

        int TestValue;
        float[][] BasicAnalysis;
        int NumbValidBlob;
        boolean IsGoodValueFound = false;


        //Find optimal threshold value
        for (TestValue = 150; TestValue >= 50; TestValue -= 25) {
            //Run magic threshold
            BWImage = adaptiveThresholding_Core(SrcGrayPixelArray, 15,
                    AdaptiveThreshold_tPercent, TestValue);

            //conduct region lanbeling
            regionExtract_RasterScanning(BWImage, 0);

            //Conduct basic analysis of detected regions such as center x and y, size
            BasicAnalysis = regionExtract_BasicAnalysis(RegionLabeling_LabelIDMap,
                    RegionLabeling_ColorTable);


            //Count number of blobs whose size is higher than 20 pixels
            NumbValidBlob = 0;
            if (BasicAnalysis.length > 0) {
                for (int tt = 1; tt <= BasicAnalysis.length - 1; tt++) {
                    if (BasicAnalysis[tt][2] > 20) {
                        NumbValidBlob += 1;
                    }
                }
            }


            if (NumbValidBlob < 100) {
                AdaptiveThreshold_BWImage = BWImage;
                return TestValue;
            }
        }


        if (IsGoodValueFound == false) {
            AdaptiveThreshold_BWImage = BWImage;
            return 50;
        }

        return 50;
    }

    /**
     * Analyze detected objects
     *
     * @param Src_IDMap Source IDMap
     * @param Src_ColorTable Source color table
     * @return 2D array
     */
    public float[][] regionExtract_BasicAnalysis(short[][] Src_IDMap, short[][] Src_ColorTable) {
        //Returning array of float[region ID][analysis value]
        // region ID: index starts from 1        
        // if analysis value=0,   center X
        //    analysis value=1,   center Y
        //    analysis value=2,   pixel count
        //    anlaysis value=3,   min X
        //    analysis value=4,   max X
        //    anlaysis value=5,   min Y
        //    analysis value=6,   max Y



        int RegionCount = Src_ColorTable.length - 1;
        float[][] ReturnArray = new float[RegionCount + 1][7];
        int ImageWidthUpperBound = Src_IDMap.length - 1;
        int ImageHeightUpperBound = Src_IDMap[0].length - 1;



        for (int y = 0; y <= ImageHeightUpperBound; y++) {
            for (int x = 0; x <= ImageWidthUpperBound; x++) {
                ReturnArray[Src_IDMap[x][y]][0] += x;
                ReturnArray[Src_IDMap[x][y]][1] += y;
                ReturnArray[Src_IDMap[x][y]][2] += 1;

                if (ReturnArray[Src_IDMap[x][y]][3] > x) {
                    ReturnArray[Src_IDMap[x][y]][3] = x;
                }
                if (ReturnArray[Src_IDMap[x][y]][5] > y) {
                    ReturnArray[Src_IDMap[x][y]][5] = y;
                }
                if (ReturnArray[Src_IDMap[x][y]][4] < x) {
                    ReturnArray[Src_IDMap[x][y]][4] = x;
                }
                if (ReturnArray[Src_IDMap[x][y]][6] < y) {
                    ReturnArray[Src_IDMap[x][y]][6] = y;
                }
            }
        }



        //Calculating average value
        for (int y = 1; y <= RegionCount; y++) {

            if (ReturnArray[y][2] > 0) {
                ReturnArray[y][0] = (int) (ReturnArray[y][0] / ReturnArray[y][2]);
                ReturnArray[y][1] = (int) (ReturnArray[y][1] / ReturnArray[y][2]);
            }
        }

        return ReturnArray;
    }

    /**
     * Conduct magic thresholding
     *
     * @param IsConductBlurring not used. Keep this parameter for future
     * implementation
     * @param IsFindThresholdValue set true to find optimal threshold value
     * @param DefaultThresholdValue if IsFindThresholdValue is false, set this
     * value.
     * @return Black and white image
     */
    public final BufferedImage do_MagicThresholding(BufferedImage SourceGrayImage,
            boolean IsConductBlurring, boolean IsFindThresholdValue, int DefaultThresholdValue) {

        BufferedImage BWImage = null;

        if (IsFindThresholdValue) {
            MagicThreshold_OptimalThresholdValue = find_OptimalThreshold_For_MagicThresholding(
                    SourceGrayImage);
            BWImage = deepCopy(MagicThreshold_BWImage);
        } else {
            BWImage = magicThresholding_Core(SourceGrayImage, DefaultThresholdValue, 255);
        }


        return BWImage;

    }

    /**
     * Modified version of adaptive thresholding for bigimage
     *
     * @param SourceGrayImage source gray image
     * @param SliceWidth width of slice
     * @param SliceHeight height of slice
     * @return array in RGB
     */
    public BufferedImage do_AdaptiveThresholding_For_BigImage(BufferedImage SourceGrayImage,
            int SliceWidth, int SliceHeight) {

        int srcImageWidth = SourceGrayImage.getWidth();
        int srcImageHeight = SourceGrayImage.getHeight();
        short[][] srcImagePixelArray = convert_Image_To_GrayShortArray(SourceGrayImage);
        short[][] cropImagePixelArray = new short[SliceWidth][SliceHeight];
        BufferedImage outputImage;

        for (int i = 0; i < srcImageWidth; i = i + 640) {
            for (int j = 0; j < srcImageHeight; j = j + 480) {

                //Cropping big image to get slice image
                for (int x = 0; x < 640; x++) {
                    for (int y = 0; y < 480; y++) {
                        cropImagePixelArray[x][y] = srcImagePixelArray[i + x][j + y];
                    }
                }

                //Run adaptive thresholding
                AdaptiveThreshold_OptimalThresholdValue =
                        find_OptimalThreshold_For_AdaptiveThresholding(
                        cropImagePixelArray);

                //Copying slice image to big image
                for (int x = 0; x < 640; x++) {
                    for (int y = 0; y < 480; y++) {
                        srcImagePixelArray[i + x][j + y] = AdaptiveThreshold_BWPixelArray[x][y];

                    }
                }
            }
        }

        outputImage = convert_GrayShortArray_To_Image(srcImagePixelArray);

        return outputImage;
    }

    /**
     * Conduct adaptive thresholding
     *
     * @param SourceGrayImage source gray image
     * @param IsFindOptimalThresholdValue set true to find optimal threshold
     * value
     * @param DefaultThresholdValue if IsFindThresholdValue is false, set this
     * @return Black and white image
     */
    public BufferedImage do_AdaptiveThresholding(BufferedImage SourceGrayImage,
            boolean IsFindOptimalThresholdValue, int DefaultThresholdValue) {

        BufferedImage BWImage = null;

        if (IsFindOptimalThresholdValue) {
            AdaptiveThreshold_OptimalThresholdValue = find_OptimalThreshold_For_AdaptiveThresholding(
                    SourceGrayImage);
            BWImage = deepCopy(AdaptiveThreshold_BWImage);
        } else {
            short[][] SrcPixels = convert_Image_To_GrayShortArray(SourceGrayImage);

            BWImage = adaptiveThresholding_Core(SrcPixels, 15, AdaptiveThreshold_tPercent, DefaultThresholdValue);
        }


        return BWImage;

    }

    /**
     * Evaluate two images to determine whether alignment is needed
     *
     * @param srcImage1 gray source image 1
     * @param srcImage2 gray source image 2
     * @param maxShift extent of shift
     * @param samplingInterval interval of sample
     * @return BufferedImage if alignment is needed, otherwise return null
     */
    public BufferedImage alignImage(BufferedImage srcImage1, BufferedImage srcImage2,
            int maxShift, int samplingInterval) {

        short[][] grayImage1 = convert_GrayImage_To_GrayShortArray(srcImage1);
        short[][] grayImage2 = convert_GrayImage_To_GrayShortArray(srcImage2);


        int width = grayImage1.length;
        int height = grayImage1[0].length;
        int arrayMax = (int) Math.pow(maxShift * 2 + 1, 2);

        int[] arrayShiftInX = new int[arrayMax];
        int[] arrayShiftInY = new int[arrayMax];
        long[] arrayDiffCount = new long[arrayMax];
        int arrayCount = 0;


        for (int shiftX = -maxShift; shiftX <= maxShift; shiftX++) {
            for (int shiftY = -maxShift; shiftY <= maxShift; shiftY++) {
                arrayShiftInX[arrayCount] = shiftX;
                arrayShiftInY[arrayCount] = shiftY;

                //Counting  difference of array
                arrayDiffCount[arrayCount] = 0;
                for (int x = maxShift; x < width - maxShift; x = x + samplingInterval) {
                    for (int y = maxShift; y < height - maxShift; y = y + samplingInterval) {
                        arrayDiffCount[arrayCount] = arrayDiffCount[arrayCount]
                                + Math.abs(grayImage1[x + shiftX][y + shiftY] - grayImage2[x][y]);

                    }
                }

                arrayCount++;
            }
        }


        //Find minimum difference count and array index
        long curMinDiffCount = arrayDiffCount[0];
        int curArrayIndex = 0;
        for (int q = 1; q < arrayCount; q++) {
            if (arrayDiffCount[q] < curMinDiffCount) {
                curArrayIndex = q;
                curMinDiffCount = arrayDiffCount[q];
            }
        }




        if (arrayShiftInX[curArrayIndex] == 0
                && arrayShiftInY[curArrayIndex] == 0) {
            return null;
        } else {

            //Add padding to the image1 and save to image2
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if ((x + arrayShiftInX[curArrayIndex] >= 0)
                            && (x + arrayShiftInX[curArrayIndex] < width)
                            && (y + arrayShiftInY[curArrayIndex] >= 0)
                            && (y + arrayShiftInY[curArrayIndex] < height)) {
                        grayImage2[x][y] = grayImage1[x + arrayShiftInX[curArrayIndex]][y + arrayShiftInY[curArrayIndex]];
                    } else {
                        grayImage2[x][y] = 255;
                    }
                }
            }
            BufferedImage outputImage = convert_GrayShortArray_To_GrayImage(grayImage2);

            return outputImage;
        }

    }

    /**
     * Convert gray image to gray array
     *
     * @param SrcImage source image
     * @return gray array
     */
    public short[][] convert_GrayImage_To_GrayShortArray(BufferedImage SrcImage) {
        int x_upperBound = SrcImage.getWidth();
        int y_upperBound = SrcImage.getHeight();
        int rgb, gray;

        short[][] outShortArray = new short[x_upperBound][y_upperBound];

        for (int y = 0; y < y_upperBound; y++) {
            for (int x = 0; x < x_upperBound; x++) {
                rgb = SrcImage.getRGB(x, y);
                gray = (rgb >> 16) & 0x000000FF;
                //green = (rgb >> 8) & 0x000000FF;
                //blue = (rgb) & 0x000000FF;

                outShortArray[x][y] = (short) gray;
            }
        }
        return outShortArray;
    }

    /**
     * Convert gray array to gray image
     *
     * @param SrcGrayShortArray gray array
     * @return gray image
     */
    public BufferedImage convert_GrayShortArray_To_GrayImage(short[][] SrcGrayShortArray) {
        int x_upperBound = SrcGrayShortArray.length - 1;
        int y_upperBound = SrcGrayShortArray[0].length - 1;
        int rgb;
        short grayValue;

        BufferedImage outImage = new BufferedImage(x_upperBound + 1,
                y_upperBound + 1, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                grayValue = SrcGrayShortArray[x][y];
                if (grayValue > 255) {
                    grayValue = 255;
                } else {
                    if (grayValue < 0) {
                        grayValue = 0;
                    }
                }

                rgb = new Color(grayValue, grayValue,
                        grayValue).getRGB();
                outImage.setRGB(x, y, rgb);
            }
        }
        return outImage;
    }

    /**
     * Search tails of skeleton
     *
     * @param SrcGrayShortArray source gray array
     * @return image
     */
    public Point[] search_SkeletonTail(short[][] SrcGrayShortArray, int CenterX, int CenterY,
            short FillColorG, int ToleranceG, int MaxTrackingCount) {


        int SrcImageWidth = SrcGrayShortArray.length;
        int SrcImageHeight = SrcGrayShortArray[0].length;
        int curTrackingCount = 0;

        Point Cur_Location = new Point();
        Point[] neighborhoodPixelLocation = new Point[MaxTrackingCount];
        Point[] foundPixelLocation = new Point[MaxTrackingCount];

        int CenterColor_G;

        boolean[][] QueueMap = new boolean[SrcImageWidth][SrcImageHeight];

        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);
        _FloodFilledDoneMap = new boolean[SrcImageWidth][SrcImageHeight];

        FloodFillQue.clear();
        Cur_Location.x = CenterX;
        Cur_Location.y = CenterY;
        FloodFillQue.add(Cur_Location);
        QueueMap[Cur_Location.x][Cur_Location.y] = true;
        _FloodFilledDoneMap[Cur_Location.x][Cur_Location.y] = true;

        CenterColor_G = SrcGrayShortArray[Cur_Location.x][Cur_Location.y];



        do {
            Cur_Location.x = FloodFillQue.peek().x;
            Cur_Location.y = FloodFillQue.peek().y;

            outPixels[Cur_Location.x][Cur_Location.y] = FillColorG;
            _FloodFilledDoneMap[Cur_Location.x][Cur_Location.y] = true;


            curTrackingCount++;
            foundPixelLocation[curTrackingCount - 1] = new Point(Cur_Location);
            if (curTrackingCount == MaxTrackingCount) {
                break;
            }


            FloodFillQue.poll();
            QueueMap[Cur_Location.x][Cur_Location.y] = false;


            //Initializing pixel locations
            neighborhoodPixelLocation[1] = new Point(Cur_Location.x - 1, Cur_Location.y - 1);
            neighborhoodPixelLocation[2] = new Point(Cur_Location.x, Cur_Location.y - 1);
            neighborhoodPixelLocation[3] = new Point(Cur_Location.x + 1, Cur_Location.y - 1);
            neighborhoodPixelLocation[4] = new Point(Cur_Location.x - 1, Cur_Location.y);
            neighborhoodPixelLocation[5] = new Point(-1, -1);
            neighborhoodPixelLocation[6] = new Point(Cur_Location.x + 1, Cur_Location.y);
            neighborhoodPixelLocation[7] = new Point(Cur_Location.x - 1, Cur_Location.y + 1);
            neighborhoodPixelLocation[8] = new Point(Cur_Location.x, Cur_Location.y + 1);
            neighborhoodPixelLocation[9] = new Point(Cur_Location.x + 1, Cur_Location.y + 1);



            //Excluding invalid pixel locations
            if (Cur_Location.y == 0) {
                neighborhoodPixelLocation[1].x = -1;
                neighborhoodPixelLocation[2].x = -1;
                neighborhoodPixelLocation[3].x = -1;
            }

            if (Cur_Location.y == SrcImageHeight - 1) {
                neighborhoodPixelLocation[7].x = -1;
                neighborhoodPixelLocation[8].x = -1;
                neighborhoodPixelLocation[9].x = -1;
            }

            if (Cur_Location.x == 0) {
                neighborhoodPixelLocation[1].x = -1;
                neighborhoodPixelLocation[4].x = -1;
                neighborhoodPixelLocation[7].x = -1;
            }

            if (Cur_Location.x == SrcImageWidth - 1) {
                neighborhoodPixelLocation[3].x = -1;
                neighborhoodPixelLocation[6].x = -1;
                neighborhoodPixelLocation[9].x = -1;
            }


            //Processing for valid pixel locations
            for (int i = 1; i < 10; i++) {

                if (neighborhoodPixelLocation[i].x != -1) {
                    if (QueueMap[neighborhoodPixelLocation[i].x][neighborhoodPixelLocation[i].y] == false) {
                        if (_FloodFilledDoneMap[neighborhoodPixelLocation[i].x][neighborhoodPixelLocation[i].y] == false
                                && Math.abs(CenterColor_G
                                - SrcGrayShortArray[neighborhoodPixelLocation[i].x][neighborhoodPixelLocation[i].y]) <= ToleranceG) {
                            FloodFillQue.add(neighborhoodPixelLocation[i]);
                            QueueMap[neighborhoodPixelLocation[i].x][neighborhoodPixelLocation[i].y] = true;
                        }
                    }
                }
            }

        } while (FloodFillQue.size() != 0);



        if (curTrackingCount < MaxTrackingCount) {
            return null;
        } else {
            return foundPixelLocation;
        }
    }

    /**
     * Conduct adaptive thresholding
     *
     * @param SrcPixels source gray image
     * @param boxSize box size for processing
     * @param tpercent t percent value for comparison between gray value and
     * average gray value of selected box
     * @param GrayLimit pixels whose gray value is higher than GrayLimit will be
     * excluded
     * @return optimal threshold value
     */
    public BufferedImage adaptiveThresholding_Core_To_Gray(short[][] SrcPixels,
            int boxSize, float tpercent,
            int GrayLimit) {


        short[][] OutPixels;
        int SrcBitmapWidth = SrcPixels.length;
        int SrcBitmapHeight = SrcPixels[0].length;

        OutPixels = new short[SrcBitmapWidth][SrcBitmapHeight];

        long[][] temp_IntegralGrayMap;
        int LocalGrayAvg = 0;
        float rPercentValue = (float) (1 - tpercent);

        temp_IntegralGrayMap = build_IntegralGrayArrayMap(SrcPixels);


        for (int y = 0; y < SrcBitmapHeight; y++) {
            for (int x = 0; x < SrcBitmapWidth; x++) {

                LocalGrayAvg = calculate_LocalGrayAvgValueUsingIntegralMap(
                        temp_IntegralGrayMap, boxSize, x, y);
                if (SrcPixels[x][y] < LocalGrayAvg * rPercentValue && SrcPixels[x][y] < GrayLimit) {
                    OutPixels[x][y] = 255;
                } else {
                    OutPixels[x][y] = 0;
                }

            }
        }
        AdaptiveThreshold_BWPixelArray = OutPixels;
        return convert_GrayShortArray_To_GrayImage(OutPixels);
    }

    
    //Count the total number pixels in circle
    public long countPixelInCircle(short[][] SrcGrayShortArray,
            int centerX, int centerY,
            int circleRadius, int circleWidth,
            short pixelValue) {

        int srcImageWidth = SrcGrayShortArray.length;
        int srcImageHeight = SrcGrayShortArray[0].length;

        double curX, curY;
        double prevX = 0;
        double prevY = 0;
        double oneCycleRad = 2 * Math.PI;
        double circleStepIncrease = oneCycleRad / 360;

        long pixelCountSum = 0;

        for (int curCircleRadius = circleRadius - circleWidth / 2;
                curCircleRadius < circleRadius + circleWidth / 2;
                curCircleRadius++) {

            for (double radAngle = 0; radAngle < oneCycleRad;
                    radAngle = radAngle + circleStepIncrease) {

                curX = Math.sin(radAngle) * curCircleRadius + centerX;
                curY = Math.cos(radAngle) * curCircleRadius + centerY;

                if ((int) curX < 0
                        || (int) curY < 0
                        || (int) curX >= srcImageWidth
                        || (int) curY >= srcImageHeight) {
                    //if the point is out of range, skip it
                    continue;
                }

                if ((int) curX == prevX && (int) curY == prevY) {
                    //if the point is the same as the previous one, skip it
                    continue;
                }

                if (SrcGrayShortArray[(int) curX][(int) curY] == pixelValue) {
                    pixelCountSum++;
                }

                prevX = (int) curX;
                prevY = (int) curY;

            }
        }

        return pixelCountSum;

    }

    //Rotate image
    public BufferedImage rotateImage(BufferedImage src, int centerX,
            int centerY, double degrees, boolean isAntialiasingOn,
            Color bkgroundColor) {

        double radians = Math.toRadians(degrees);

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        BufferedImage result = new BufferedImage(srcWidth, srcHeight,
                src.getType());
        
        Graphics2D g = result.createGraphics();
        if (isAntialiasingOn) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.setBackground(bkgroundColor);  
        g.fillRect(0,0,srcWidth,srcHeight);  
        g.rotate(radians, centerX, centerY);
        g.drawRenderedImage(src, null);

        return result;
    }

    //Shift image
    public BufferedImage shiftImage(BufferedImage src, int stepX,
            int stepY, boolean isAntialiasingOn,
            Color bkgroundColor) {


        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        BufferedImage result = new BufferedImage(srcWidth, srcHeight,
                src.getType());
        
        Graphics2D g = result.createGraphics();
        
        if (isAntialiasingOn) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.setBackground(bkgroundColor);  
        g.fillRect(0,0,srcWidth,srcHeight);  
        g.translate(stepX, stepY);
        g.drawRenderedImage(src, null);

        return result;
    }

   //Rotate and shift image 
   public BufferedImage rotateAndShiftImage(BufferedImage src, int centerX,
            int centerY, double degrees, boolean isAntialiasingOn,
            Color bkgroundColor) {

       BufferedImage outImage = 
                        rotateImage(src, centerX, centerY, 
                        degrees,isAntialiasingOn, bkgroundColor );
       outImage =  shiftImage(outImage, Math.round(src.getWidth()/2 - centerX),
                        Math.round(src.getHeight()/2 - centerY), 
                        isAntialiasingOn, bkgroundColor);
       
        return outImage;
    }
   
   
    //Crop image
    public BufferedImage cropImage(BufferedImage srcImage, int x, int y,
                                    int width, int height) {
       
        BufferedImage dest = new BufferedImage(width, height,
                srcImage.getType());
        
        Graphics2D g2 = dest.createGraphics();
        
        g2.drawImage(srcImage.getSubimage(x, y, width, height), 0, 0, null);
        
        return dest;
    } 
}
