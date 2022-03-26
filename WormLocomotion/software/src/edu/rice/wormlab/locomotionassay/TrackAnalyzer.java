/*
 * Filename: TrackAnalyzer.java
 * This class is used to calculate worm speed
 */
package edu.rice.wormlab.locomotionassay;

import ij.gui.Plot;
import java.awt.Color;
import java.util.LinkedList;

public class TrackAnalyzer {

    //Calculate worm speed for all tracklists
    public static double[] analyze_AllTrackLists(LinkedList<LinkedList<double[]>> tracksList,
            int videoFrameCount, Double videoDurationInSec,
            Double microMeterPerPixel) {
        if (tracksList == null) {
            return null;
        }; // if


        int numPoints = 0;
        for (int i = 0; i < tracksList.size(); i++) {
            numPoints += (tracksList.get(i)).size();
        }

        double[] ret = new double[numPoints];
        int copyStart = 0;
        for (int i = 0; i < tracksList.size(); i++) {
            LinkedList<double[]> trackList = tracksList.get(i);
            double[] X = Smoothing.getX(trackList);
            double[] Y = Smoothing.getY(trackList);
            double[] XSmoothed = Smoothing.doBezierSmoothing(X,
                    X.length);
            double[] YSmoothed = Smoothing.doBezierSmoothing(Y,
                    Y.length);
            double[] speed = calcSpeed(XSmoothed, YSmoothed, videoFrameCount,
                    videoDurationInSec, microMeterPerPixel);
            System.arraycopy(speed, 0, ret, copyStart, speed.length);
            copyStart += speed.length;
        }; // for
        return ret;
    }
   
    
    
    
    //Calculate worm speed only for the designated tracklist
    public static double[] analyze_OnlyTargetTrackList(LinkedList<LinkedList<double[]>> tracksList, int frames,
            Double duration, Double microMeterPerPixel, int targetTrackIndex) {
        if (tracksList == null) {
            return null;
        }; // if


        int numPoints = tracksList.get(targetTrackIndex).size();


        double[] ret = new double[numPoints];
        int copyStart = 0;

        LinkedList<double[]> trackList = tracksList.get(targetTrackIndex);
        double[] X = Smoothing.getX(trackList);
        double[] Y = Smoothing.getY(trackList);
        double[] XSmoothed = Smoothing.doBezierSmoothing(X,
                X.length);
        double[] YSmoothed = Smoothing.doBezierSmoothing(Y,
                Y.length);
        double[] speed = calcSpeed(XSmoothed, YSmoothed, frames,
                duration, microMeterPerPixel);
        System.arraycopy(speed, 0, ret, copyStart, speed.length);
        return ret;
    }

    

    public static double[] calcSpeed(double[] X, double[] Y, int frames,
            Double duration, Double microMeterPerPixel) {
        double actualFrameRate = frames / duration;
        if (X == null || Y == null || X.length != Y.length) {
            return null;
        }; // if

        double[] speed = new double[X.length - 1];
        for (int i = 0; i < X.length - 1; i++) {
            speed[ i] = Math.sqrt(
                    Math.pow(X[ i] - X[ i + 1], 2)
                    + Math.pow(Y[ i] - Y[ i + 1], 2)) * actualFrameRate * microMeterPerPixel / 1000;

        }; // for

        return speed;
    }

    public static double[] calcHistogram(double[] values, Double maxBin, Double binSpacing) {
        if (values == null || values.length == 0) {
            return null;
        }; // if
        int nBins = (int) (maxBin / binSpacing);
        double[] hist = new double[nBins];
        for (int i = 0; i < values.length; i++) {
            if ((int) (Math.floor(values[ i] / binSpacing)) >= nBins) {
                hist[ nBins - 1]++;
            } else {
                hist[ (int) (Math.floor(values[ i] / binSpacing))]++;
            }; // if
        }
        // divide by sum
        for (int i = 0; i < nBins; i++) {
            hist[ i] /= values.length;
        }; // for
        return hist;
    }

    public static double[] calcCDF(double[] values) {
        if (values == null || values.length == 0) {
            return null;
        }; // if
        double[] cdf = new double[values.length + 1];
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[ i];
            cdf[ i + 1] = sum;
        }; // for
        return cdf;
    }

    public static void drawCDF(double[] cdf, String name, double binSpacing, double maxBin) {
        double[] bins = new double[(int) (maxBin / binSpacing) + 1];
        bins[ 0] = 0;
        for (int i = 0; i < (maxBin / binSpacing); i++) {
            bins[ i + 1] = binSpacing * i + binSpacing / 2.0;
        }; // for
        Plot myplot = new Plot("CDF plot " + name, "speed", "cumulative probability", bins, cdf);
        myplot.setSize(500, 300);
        myplot.setLimits(0, maxBin, 0, 1.1);
        myplot.setColor(Color.blue);
        myplot.show();
    }

    public static void drawHistogram(double[] hist, String name, double binSpacing, double maxBin) {
        double[] bins = new double[(int) (maxBin / binSpacing)];
        double max = 0;
        for (int i = 0; i < (maxBin / binSpacing); i++) {
            bins[ i] = binSpacing * i + binSpacing / 2.0;
            if (hist[ i] >= max) {
                max = hist[ i];
            }; // if
        }; // for
        //Plot myplot = new Plot("histogram " + name, "speed", "percantage", bins, hist);
        Plot myplot = new Plot("histogram " + name, "speed", "percentage", new double[0], new double[0]);
        myplot.setSize(300, 300);
        myplot.setLimits(-0.01, maxBin, 0, Math.ceil(max / 0.02) * 0.02);
        myplot.setColor(Color.blue);
        myplot.addPoints(bins, hist, Plot.LINE);
        //myplot.setLineWidth((int)(300*0.7/(maxBin/binSpacing)));
        for (int i = 0; i < (maxBin / binSpacing); i++) {
            if (hist[ i] > 0) {
                myplot.drawLine(binSpacing * i, 0, binSpacing * i, hist[ i]);
            }; // if
        }; // for
        myplot.show();
    }
}
