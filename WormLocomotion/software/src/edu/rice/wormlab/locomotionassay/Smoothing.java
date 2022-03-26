/*
 * This class contains Bezier smoothing algorithm
 */
package edu.rice.wormlab.locomotionassay;

import java.util.List;


public class Smoothing {
    
    
        public static double[] doBezierSmoothing(double[] numbArray, int curveOrder) {
        if (numbArray == null) {
            return null;
        }

        double[] smoothedXArray = new double[numbArray.length + curveOrder];
        double[] smoothedYArray = new double[numbArray.length + curveOrder];
        double[] avgXArray = new double[numbArray.length + curveOrder];
        double[] avgYArray = new double[numbArray.length + curveOrder];
        double[] reconstructedSmoothedXArray = new double[numbArray.length];
        double[] reconstructedSmoothedYArray;

        for (int i = 0; i < numbArray.length; i++) {
            smoothedXArray[i] = i;
            smoothedYArray[i] = numbArray[i];
        }

        
        for (int j = 0; j < curveOrder; j++) {
            avgXArray[0] = smoothedXArray[0];
            avgXArray[numbArray.length + j] = smoothedXArray[numbArray.length + j - 1];
            avgYArray[0] = smoothedYArray[0];
            avgYArray[numbArray.length + j] = smoothedYArray[numbArray.length + j - 1];
            
            for (int i = 0; i < numbArray.length + j - 1; i++) {
                avgXArray[i + 1] = (smoothedXArray[i] + smoothedXArray[i + 1]) / 2;
                avgYArray[i + 1] = (smoothedYArray[i] + smoothedYArray[i + 1]) / 2;
            }
            System.arraycopy(avgXArray, 0, smoothedXArray, 0, numbArray.length + 1 + j);
            System.arraycopy(avgYArray, 0, smoothedYArray, 0, numbArray.length + 1 + j);
        }

        
        for (int i = 0; i < numbArray.length; i++) {
            reconstructedSmoothedXArray[i] = i;
        }
        reconstructedSmoothedYArray = get_Interpolated_yArray_From_ArrayPoints(
                                smoothedXArray, smoothedYArray,reconstructedSmoothedXArray);
        
        return reconstructedSmoothedYArray;
    }

    
        
    public static double[] get_Interpolated_yArray_From_ArrayPoints(double[] x0, double[] y0,
                                                           double[] x) {
        double[] y = new double[x.length];
        
        for (int i=0; i<x.length;i++) {
            y[i] = get_Interpolated_y_From_ArrayPoints(x0, y0, x[i]);
        }
        
        return y;
    }
    
    
    public static double get_Interpolated_y_From_ArrayPoints(double[] x0, double[] y0,
                                                           double x) {
        for (int i=0; i<x0.length;i++) {
            if (x0[i] >= x) {
                if (x0[i] == x) {
                    return y0[i];
                } else {
                    return get_Interpolated_y_From_TwoPoints(x0[i-1],y0[i-1],
                                                   x0[i], y0[i], x);
                }
            }
        }
        return 0;
    }
    
    
    public static double get_Interpolated_y_From_TwoPoints(double x0, double y0,
                                double x1, double y1, double x) {
        if (x0 == x1) {
            return y0;
        } else {
            return y0 + (y1-y0) *(x-x0)/(x1-x0);
        }
    }
    
    
    public static double[] doNeigborhoodAvgSmoothing(double[] numbArray, int NeighborRange) {
        if (numbArray == null) {
            return null;
        }

        double[] avgArray = new double[numbArray.length];
        int validNeightborCount;

        for (int i = 0; i < numbArray.length; i++) {
            validNeightborCount = 0;
            avgArray[i] = 0;
            for (int j = -NeighborRange; j <= NeighborRange; j++) {
                if (i + j >= 0 && i + j < numbArray.length) {
                    validNeightborCount++;
                    avgArray[i] = avgArray[i] + numbArray[i + j];
                }
            }
            avgArray[i] = avgArray[i] / validNeightborCount;
        }

        return avgArray;
    }

    
    
      /**
     * Computer path length of given TrackList
     */
    public static double get_PathLength(double[] x, double[] y) {
        if (x == null || y == null) {
            return 0;
        }
 

        double pathLength = 0;

        for (int i = 1; i < x.length; i++) {
            pathLength = pathLength + Math.sqrt(Math.pow(x[i-1]-x[i],2) +
                                                Math.pow(y[i-1]-y[i],2));
        }

        return pathLength;
    }
   
    
      public static double[] getX(List<double[]> valuesList) {
        if (valuesList != null) {
            double[] X = new double[valuesList.size()];
            for (int i = 0; i < valuesList.size(); i++) {
                double[] unit = valuesList.get(i);
                X[ i] = unit[ 0];
            }
            return X;
        }
        return null;
    }

    public static double[] getY(List<double[]> valuesList) {
        if (valuesList != null) {
            double[] Y = new double[valuesList.size()];
            for (int i = 0; i < valuesList.size(); i++) {
                double[] unit = valuesList.get(i);
                Y[ i] = unit[ 1];
            }
            return Y;
        }
        return null;
    }
}
