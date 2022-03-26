/*
 * Filename: CannyEdgeDetector.java
 * This class contains Canny edge detection algorithm
 */

package edu.rice.wormlab.eggcounter;

/**
 * Perform Canny edge detection
 */
import java.awt.image.BufferedImage;


public class CannyEdgeDetector {

    private float[][] DerivativeX;
    private float[][] DerivativeY;
    private short[][] EdgeMap;
    private short[][] EdgePoints;
    public short[][] FilteredImage;
    private short[][] GaussianKernel;
    private float[][] GNH;
    private float[][] GNL;
    private float[][] Gradient;
    private short[][] GreyImage;
    private int KernelSize;
    private int KernelWeight;
    private float MaxHysteresisThresh;
    private float MinHysteresisThresh;
    private float[][] NonMax;
    private short[][] PostHysteresis;
    private float Sigma;
    private short[][] VisitedMap;
    public int Height;
    public int Width;
    public int HeightUpperIndex; 
    public int WidthUpperIndex; 
    private float Phi = 3.1415926535897931F;
    private NativeImgProcessing ImgProc = new NativeImgProcessing();

    public final BufferedImage detectCannyEdges(BufferedImage InputImage, float Th, float Tl,
            int GaussianMaskSize, float SigmaforGaussianKernel) {

        MaxHysteresisThresh = Th;
        MinHysteresisThresh = Tl;
        KernelSize = GaussianMaskSize;
        Sigma = SigmaforGaussianKernel;
        Width = InputImage.getWidth();
        Height = InputImage.getHeight();
        WidthUpperIndex = Width - 1;
        HeightUpperIndex = Height - 1;

        EdgeMap = new short[WidthUpperIndex + 1][HeightUpperIndex + 1];
        VisitedMap = new short[WidthUpperIndex + 1][HeightUpperIndex + 1];


        GreyImage = ImgProc.convert_Image_To_GrayShortArray(InputImage);


        Gradient = new float[WidthUpperIndex + 1][HeightUpperIndex + 1];
        NonMax = new float[WidthUpperIndex + 1][HeightUpperIndex + 1];
        PostHysteresis = new short[WidthUpperIndex + 1][HeightUpperIndex + 1];
        DerivativeX = new float[WidthUpperIndex + 1][HeightUpperIndex + 1];
        DerivativeY = new float[WidthUpperIndex + 1][HeightUpperIndex + 1];

        FilteredImage = GaussianFilter(GreyImage);


        short[][] Dx = new short[][]{{1, 0, -1}, {1, 0, -1}, {1, 0, -1}};
        short[][] Dy = new short[][]{{1, 1, 1}, {0, 0, 0}, {-1, -1, -1}};
        DerivativeX = differentiate(FilteredImage, Dx);
        DerivativeY = differentiate(FilteredImage, Dy);


        calculate_Gradient();
        do_NonMaxSuppression();
        do_PostHysteresis();
        do_GNHandGHL();
        hysterisisThresholding(EdgePoints);


        for (int i = 0; i <= WidthUpperIndex; i++) {
            for (int j = 0; j <= HeightUpperIndex; j++) {
                EdgeMap[i][j] = (short) (EdgeMap[i][j] * 0xFF);
            }
        }


        return ImgProc.convert_GrayShortArray_To_Image(EdgeMap);
    }

    private float[][] differentiate(short[][] Data, short[][] Filter) {
        int Fw = Filter.length;
        int Fh = Filter[0].length;
        float FwHalf = (float) (Fw / 2.0);
        int FwHalfInt = (int) (Math.rint(Fw / 2.0));
        float FhHalf = (float) (Fh / 2.0);
        int FhHalfInt = (int) (Math.rint(Fh / 2.0));


        float sum;
        float[][] Output = new float[WidthUpperIndex + 1][HeightUpperIndex + 1];
        int i = FwHalfInt;
        while (i <= ((Width - FwHalf) - 1)) {
            int j = FhHalfInt;
            while (j <= ((Height - FhHalf) - 1)) {
                sum = 0.0F;
                int k = -FwHalfInt;
                while (k <= FwHalf) {
                    int l = -FhHalfInt;
                    while (l <= FhHalf) {
                        sum = sum + (Data[(i + k)][(j + l)]
                                * Filter[(int) (Math.rint(FwHalf + k))][(int) (Math.rint(FhHalf + l))]);
                        l ++;
                    }
                    k ++;
                }
                Output[i][j] = sum;
                j ++;
            }
            i ++;
        }
        return Output;
    }

    
    //Create Gaussian filter
    private short[][] GaussianFilter(short[][] Data) {
        KernelWeight = generateGaussianKernel(KernelSize, Sigma, KernelWeight);


        short[][] Output;
        int Limit = (int) Math.rint((int) (KernelSize / 2));
        float Sum;
        Output = Data;
        int i = Limit;
        while (i <= ((WidthUpperIndex) - Limit)) {
            int j = Limit;
            while (j <= ((Height - 1) - Limit)) {
                Sum = 0.0F;
                int k = -Limit;
                while (k <= Limit) {
                    int l = -Limit;
                    while (l <= Limit) {
                        Sum = Sum + (Data[(i + k)][(j + l)]
                                * GaussianKernel[Limit + k][Limit + l]);
                        l ++;
                    }
                    k ++;
                }
                Output[i][j] = (short) Math.round((double) (Sum / (float) KernelWeight));
                j ++;
            }
            i ++;
        }
        return Output;
    }

    //Create Gaussian kernel
    private int generateGaussianKernel(int N, float Sigma, int Weight) {
        int i;
        int j;
        float pi = 3.141593F;
        int SizeofKernel = N;
        float[][] Kernel = new float[N][N];
        GaussianKernel = new short[N][N];
        float D1 = 1.0F / (((2.0F * pi) * Sigma) * Sigma);
        float D2 = (2.0F * Sigma) * Sigma;
        float min = 1000.0F;

        double SizeofKernel_Half;
        int SizeofKernel_i_HalfPlus1;
        int SizeofKernel_j_HalfPlus1;

        SizeofKernel_Half = SizeofKernel / 2.0;
        i = (int) Math.rint(-SizeofKernel_Half);

        while (i <= SizeofKernel_Half) {
            j = (int) Math.rint(-SizeofKernel_Half);

            while (j <= SizeofKernel_Half) {
                SizeofKernel_i_HalfPlus1 = (int) Math.rint(SizeofKernel_Half + i);
                SizeofKernel_j_HalfPlus1 = (int) Math.rint(SizeofKernel_Half + j);

                Kernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1] = (1.0F / D1) *
                        (float) Math.exp((double) (-((i * i) + (j * j))) / D2);

                if (Kernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1] < min) {
                    min = Kernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1];
                }
                j ++;
            }
            i ++;
        }


        int mult = (int) Math.rint(1.0F / min);
        int sum = 0;
        if ((min > 0.0F) && (min < 1.0F)) {
            i = (int) Math.rint(-SizeofKernel_Half);
            while (i <= SizeofKernel_Half) {
                j = (int) Math.rint(-SizeofKernel_Half);
                while (j <= SizeofKernel_Half) {

                    SizeofKernel_i_HalfPlus1 = (int) Math.rint(SizeofKernel_Half + i);
                    SizeofKernel_j_HalfPlus1 = (int) Math.rint(SizeofKernel_Half + j);


                    Kernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1] =
                            (float) (Math.round(((double) (Kernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1] * mult))
                            * Math.pow(10, 0)) / Math.pow(10, 0));
                    GaussianKernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1] =
                            (short) Math.rint(Kernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1]);
                    sum += GaussianKernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1];
                    j ++;
                }
                i ++;
            }
        } else {
            sum = 0;
            i = (int) Math.rint(-SizeofKernel_Half);
            while (i <= SizeofKernel_Half) {
                j = (int) Math.rint(-SizeofKernel_Half);
                while (j <= SizeofKernel_Half) {

                    SizeofKernel_i_HalfPlus1 = (int) Math.rint(SizeofKernel_Half + i);
                    SizeofKernel_j_HalfPlus1 = (int) Math.rint(SizeofKernel_Half + j);

                    Kernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1] =
                            (float) (Math.round(((double) Kernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1])
                            * Math.pow(10, 0)) / Math.pow(10, 0));
                    GaussianKernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1] =
                            (short) Math.rint(Kernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1]);
                    sum += GaussianKernel[SizeofKernel_i_HalfPlus1][SizeofKernel_j_HalfPlus1];
                    j ++;
                }
                i ++;
            }
        }
        Weight = sum;

        return Weight;
    }

    private void hysterisisThresholding(short[][] Edges) {
        int i;
        int j;
        int Limit = (int) Math.rint(KernelSize / 2.0);
        i = Limit;
        while (i <= ((WidthUpperIndex) - Limit)) {
            j = Limit;
            while (j <= ((Height - 1) - Limit)) {
                if (Edges[i][j] == 1) {
                    EdgeMap[i][j] = 1;
                }
                j ++;
            }
            i ++;
        }


        i = Limit;
        while (i <= ((WidthUpperIndex) - Limit)) {
            j = Limit;
            while (j <= ((Height - 1) - Limit)) {
                if (Edges[i][j] == 1) {
                    EdgeMap[i][j] = 1;
                    travers(i, j);
                    VisitedMap[i][j] = 1;
                }
                j ++;
            }
            i ++;
        }
    }

    private void travers(int X, int Y) {

        if (VisitedMap[X][Y] != 1) {
            if (EdgePoints[X + 1][Y] == 2) {
                EdgeMap[X + 1][Y] = 1;
                VisitedMap[X + 1][Y] = 1;
                travers(X + 1, Y);
            } else if (EdgePoints[X + 1][Y - 1] == 2) {
                EdgeMap[X + 1][Y - 1] = 1;
                VisitedMap[X + 1][Y - 1] = 1;
                travers(X + 1, Y - 1);
            } else if (EdgePoints[X][Y - 1] == 2) {
                EdgeMap[X][Y - 1] = 1;
                VisitedMap[X][Y - 1] = 1;
                travers(X, (Y - 1));
            } else if (EdgePoints[X - 1][Y - 1] == 2) {
                EdgeMap[X - 1][Y - 1] = 1;
                VisitedMap[X - 1][Y - 1] = 1;
                travers((X - 1), (Y - 1));
            } else if (EdgePoints[X - 1][Y] == 2) {
                EdgeMap[X - 1][Y] = 1;
                VisitedMap[X - 1][Y] = 1;
                travers((X - 1), Y);
            } else if (EdgePoints[X - 1][Y + 1] == 2) {
                EdgeMap[X - 1][Y + 1] = 1;
                VisitedMap[X - 1][Y + 1] = 1;
                travers((X - 1), (Y + 1));
            } else if (EdgePoints[X][Y + 1] == 2) {
                EdgeMap[X][Y + 1] = 1;
                VisitedMap[X][Y + 1] = 1;
                travers(X, (Y + 1));
            } else if (EdgePoints[X + 1][Y + 1] == 2) {
                EdgeMap[X + 1][Y + 1] = 1;
                VisitedMap[X + 1][Y + 1] = 1;
                travers((X + 1), (Y + 1));
            }

        }
    }

    private void do_NonMaxSuppression() {
        int i;
        int j;
        int Limit = (int) Math.rint((float) KernelSize / 2.0);


        i = Limit;
        while (i <= ((Width - Limit) - 1)) {
            j = Limit;
            while (j <= ((Height - Limit) - 1)) {
                float Tangent;
                if (DerivativeX[i][j] == 0.0F) {
                    Tangent = 90.0F;
                } else {
                    Tangent = (float) ((Math.atan((double) (DerivativeY[i][j] /
                            DerivativeX[i][j])) * 180.0) / Phi);
                }




                //Angle 0
                if ((((-22.5 < Tangent) && (Tangent <= 22.5F)) || ((157.5 < Tangent)
                        && (Tangent <= -157.5F))) && ((Gradient[i][j] < Gradient[i][j + 1])
                        || (Gradient[i][j] < Gradient[i][j - 1]))) {
                    NonMax[i][j] = 0.0F;
                }


                //Angle 45
                if ((((-157.5 < Tangent) && (Tangent <= -112.5F)) || ((22.5 < Tangent)
                        && (Tangent <= 67.5F))) && ((Gradient[i][j] < Gradient[i + 1][j + 1])
                        || (Gradient[i][j] < Gradient[i - 1][j - 1]))) {
                    NonMax[i][j] = 0.0F;
                }



                //Angle 90
                if ((((-112.5 < Tangent) && (Tangent <= -67.5F)) || ((67.5 < Tangent)
                        && (Tangent <= 112.5F))) && ((Gradient[i][j] < Gradient[i + 1][j])
                        || (Gradient[i][j] < Gradient[i - 1][ j]))) {
                    NonMax[i][j] = 0.0F;
                }


                //Angle 135
                if ((((-67.5 < Tangent) && (Tangent <= -22.5F)) || ((112.5 < Tangent)
                        && (Tangent <= 157.5F))) && ((Gradient[i][ j] < Gradient[i + 1][j - 1])
                        || (Gradient[i][j] < Gradient[i - 1][j + 1]))) {
                    NonMax[i][j] = 0.0F;
                }

                j ++;
            }
            i ++;
        }

    }

    private void do_PostHysteresis() {
        int r;
        int c;
        float min = 100F;
        float max = 0F;
        int Limit = (int) Math.rint(KernelSize / 2.0);


        r = Limit;
        while (r <= ((Width - Limit) - 1)) {
            c = Limit;
            while (c <= ((Height - Limit) - 1)) {
                PostHysteresis[r][c] = (short) Math.rint(NonMax[r][c]);
                c ++;
            }
            r ++;
        }


        while (r <= ((Width - Limit) - 1)) {
            c = Limit;
            while (c <= ((Height - Limit) - 1)) {
                if (PostHysteresis[r][c] > max) {
                    max = PostHysteresis[r][c];
                }
                if ((PostHysteresis[r][c] < min) && (PostHysteresis[r][c] > 0)) {
                    min = PostHysteresis[r][c];
                }
                c ++;
            }
            r ++;
        }
    }

    private void do_GNHandGHL() {
        int r;
        int c;


        int Limit = (int) Math.rint(KernelSize / 2.0);

        GNH = new float[WidthUpperIndex + 1][HeightUpperIndex + 1];
        GNL = new float[WidthUpperIndex + 1][HeightUpperIndex + 1];
        EdgePoints = new short[WidthUpperIndex + 1][HeightUpperIndex + 1];

        r = Limit;
        while (r <= ((Width - Limit) - 1)) {
            c = Limit;
            while (c <= ((Height - Limit) - 1)) {
                if (PostHysteresis[r][c] >= MaxHysteresisThresh) {
                    EdgePoints[r][c] = 1;
                    GNH[r][c] = 255.0F;
                }

                if ((PostHysteresis[r][c] < MaxHysteresisThresh)
                        && (PostHysteresis[r][c] >= MinHysteresisThresh)) {
                    EdgePoints[r][c] = 2;
                    GNL[r][c] = 255.0F;
                }
                c ++;
            }
            r ++;
        }

    }

    private void calculate_Gradient() {
        for (int i = 0; i <= WidthUpperIndex; i++) {
            for (int j = 0; j <= HeightUpperIndex; j++) {
                Gradient[i][j] = (float) Math.sqrt((double) ((DerivativeX[i][j]
                        * DerivativeX[i][j]) + (DerivativeY[i][j] * DerivativeY[i][j])));
                NonMax[i][j] = Gradient[i][j];
            }
        }
    }
}

