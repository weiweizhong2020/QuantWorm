/*
 * Filename: DetectionCondition.java
 * This class contains worm detection parameters.
 */
package edu.rice.wormlab.lifespan;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializing worm detection parameters
 */
public class DetectionCondition {

    public String presetTitle;
    
    /** minimum white pixel count (in binarized differential image) within worm */
    public int min_DiffPixelCount_Of_Worm_In_DiffBinaryImage = 10;
    
    /** minimum area size of worms in pixel unit */
    public int min_WormSize = 40;
    
    /** maximum area size of worms in pixel unit */
    public int max_WormSize = 800;
    
    /** minimum area size of worms detected in differential image */
    public int min_MissingWormSize_In_DiffImage = 50;
    
    /** minimum value of color difference to binarize differential image*/
    public int min_GrayDiff_In_DiffImage = 30;
    
    /** minimum white pixel count in binarized differential image to be detected as valid worm */
    public int min_WhitePixelCount_In_DiffImage = 4;

    /**
     * To obtain a list of preset titles
     *
     * @param fileName absolute file name
     * @return array of string
     */
    public String[] get_DetectionConditionPreset_Titles(String fileName) {
        File file = new File(fileName);
        if (file.exists() == false) {
            return null;
        }
        List<String> settingTitle = new ArrayList<String>();
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
                if (each.startsWith("min_DiffPixelCount_Of_Worm_In_DiffBinaryImage") == true) {
                    String[] splitStr = each.split("=");
                    this.min_DiffPixelCount_Of_Worm_In_DiffBinaryImage = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_WormSize") == true) {
                    String[] splitStr = each.split("=");
                    this.min_WormSize = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("max_WormSize") == true) {
                    String[] splitStr = each.split("=");
                    this.max_WormSize = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_MissingWormSize_In_DiffImage") == true) {
                    String[] splitStr = each.split("=");
                    this.min_MissingWormSize_In_DiffImage = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_GrayDiff_In_DiffImage") == true) {
                    String[] splitStr = each.split("=");
                    this.min_GrayDiff_In_DiffImage = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_WhitePixelCount_In_DiffImage") == true) {
                    String[] splitStr = each.split("=");
                    this.min_WhitePixelCount_In_DiffImage = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("}") == true) {
                    break;
                }
            }

        }

        return this;
    }
}
