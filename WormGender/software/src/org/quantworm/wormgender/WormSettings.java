/*
 * This class defines important parameters
 * for image processing and worm detection
 */
package org.quantworm.wormgender;

public class WormSettings {

    //minimum area size in pixel unit
    public int minArea;

    //maximum area size in pixel unit
    public int maxArea;

    //minimum true length in um unit
    public double minTrueLen;

    //maximum true length in um unit    
    public double maxTrueLen;

    //minimum fatness (fatness = area in pixel/length in pixl)
    public double minFatness;

    //maximum fatness
    public double maxFatness;

    //the number of partitions in a worm we can divide into
    public double partitionCount;

    //+- range of the end of the first partition
    public int rangeOfEndOfFirstPartition;

    //threshold parameter used in processing spur dots
    public int spurTh;

    //minimum variation of stdev of gray color values
    public int minVar;

    //analysisMethod
    //"absolute": um from the end of skeleton curve
    //"percentage": % from the end of skeleton curve relative to the true worm length in um
    public String analysisMethod;  //"absolute"  or "percentage"
    public double curveLocation1;
    public double curveLocation2;

    public WormSettings() {
        this.minArea = 400;      //pixel unit
        this.maxArea = 20000;    //pixel unit
        this.minTrueLen = 600;   //um unit
        this.maxTrueLen = 1700;  //um unit
        this.minFatness = 5;     //area in pixel / length in pixel
        this.maxFatness = 200;
        this.spurTh = 26;
        this.minVar = 48;

        this.partitionCount = 3;
        this.rangeOfEndOfFirstPartition = 6;

        //Analysis will be done by the 'absolute' length method
        this.analysisMethod = "absolute";
        this.curveLocation1 = 20;   //distance (um) from the end of a worm
        this.curveLocation2 = 120;  //distance (um) from the end of a worm
    }
}
