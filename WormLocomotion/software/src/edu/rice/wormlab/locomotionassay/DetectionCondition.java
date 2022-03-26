/*
 * This class contains important condition to detect worms.
 */
package edu.rice.wormlab.locomotionassay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializing worm detection parameters
 */
public class DetectionCondition {

    public String presetTitle;
    //minimum frame count to be an valid track
    public int min_FrameCount_Of_ActiveTrack = 15;
    //minimum path length (in the pixel unit) of smootheded track to be an valid track
    public int min_PathLength_Of_SmoothedTrack = 12;
    //minimum bounding size (in the pixel unit) to be a valid track
    public int min_BoundingSize_Of_ActiveTrack = 10;
    //minimum worm size (in the pixel unit) to be a valid track
    public int min_WormSize = 100;
    //maximum worm size (in the pixel unit) to be a valid track
    public int max_WormSize = 300;
    //minimum distance (in the pixel unit) between centroids between frame
    //to be identified as the same worm
    public int min_Distance_Of_SameWorm = 10;
    //maximum allowable area percent changes of worm
    public int max_PercentChange_In_WormSize = 20;
    //bin size for statistical analysis
    public double max_BinSize_For_Histogram = 0.5;
    public double binSpacingSize_For_Histogram = 0.01;

    /**
     * To obtain a list of preset titles
     *
     * @param fileName absolute file name
     * @return array of string
     */
    public static String[] get_DetectionConditionPreset_Titles(String fileName) {
        File file = new File(fileName);
        if (file.exists() == false) {
            return null;
        }
        List<String> settingTitle = new ArrayList();
        List<String> linesList = Utilities.getLinesFromFile(file);

        for (String each : linesList) {
            if (each.startsWith("#") == true) {
                settingTitle.add(each.substring(1));
            }
        }

        String[] strArray = new String[settingTitle.size()];
        settingTitle.toArray(strArray);

        return strArray;
    }

    /**
     * To load source preset
     *
     * @param fileName absolute file name
     * @param profileTitle source preset title
     * @return
     */
    public DetectionCondition set_DetectionConditionPreset(String fileName, String profileTitle) {
        File file = new File(fileName);
        if (file.exists() == false) {
            return null;
        }

        List<String> linesList = Utilities.getLinesFromFile(file);
        this.presetTitle = profileTitle;
        boolean isReadParameterValue = false;

        for (String each : linesList) {
            if (each.startsWith("#" + profileTitle) == true) {
                isReadParameterValue = true;
            }

            if (isReadParameterValue == true) {
                if (each.startsWith("min_FrameCount_Of_ActiveTrack") == true) {
                    String[] splitStr = each.split("=");
                    this.min_FrameCount_Of_ActiveTrack = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_PathLength_Of_SmoothedTrack") == true) {
                    String[] splitStr = each.split("=");
                    this.min_PathLength_Of_SmoothedTrack = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_BoundingSize_Of_ActiveTrack") == true) {
                    String[] splitStr = each.split("=");
                    this.min_BoundingSize_Of_ActiveTrack = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_WormSize") == true) {
                    String[] splitStr = each.split("=");
                    this.min_WormSize = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("max_WormSize") == true) {
                    String[] splitStr = each.split("=");
                    this.max_WormSize = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_Distance_Of_SameWorm") == true) {
                    String[] splitStr = each.split("=");
                    this.min_Distance_Of_SameWorm = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("max_PercentChange_In_WormSize") == true) {
                    String[] splitStr = each.split("=");
                    this.max_PercentChange_In_WormSize = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("max_BinSize_For_Histogram") == true) {
                    String[] splitStr = each.split("=");
                    this.max_BinSize_For_Histogram = Utilities.getDouble(splitStr[1].trim());
                }
                if (each.startsWith("binSpacingSize_For_Histogram") == true) {
                    String[] splitStr = each.split("=");
                    this.binSpacingSize_For_Histogram = Utilities.getDouble(splitStr[1].trim());
                }
                if (each.startsWith("}") == true) {
                    break;
                }
            }

        }

        return this;
    }
}