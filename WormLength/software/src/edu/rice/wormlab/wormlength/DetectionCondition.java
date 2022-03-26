/*
 * Filename: DetectionCondition.java
 * This class contains important condition to detect worms
 */
package edu.rice.wormlab.wormlength;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DetectionCondition {

    public String presetTitle;
    
    //minimum area (in the pixel unit) to be a valid worm
    public int minArea;
    
    //maximum area (in the pixel unit) to be a valid worm
    public int maxArea;
    
    //minimum bounding size (in the pixel unit) to be a valid worm
    public int minBoundingSize;
    
    //maximum bounding size (in the pixel unit) to be a valid worm
    public int maxBoundingSize;
    
    //Threshold of spur
    //This is used to remove spur in skeleton curves
    public int spurTh;
    
    //Minimum worm thickness to be a valid worm
    //MeanWormFat = (Area in pixel)/(WormLength in pixel)
    public double minMeanWormFat;
    
    //Maximum worm thickness to be a valid worm
    public double maxMeanWormFat;
    
    //Minimum true length (in the micrometer unit) to be a valid worm
    public double minTrueLength;
    
    //Maximum true length (in the micrometer unit) to be a valid worm
    public double maxTrueLength;

    // Common size adult worm detection
    public DetectionCondition() {
        this.presetTitle = "Default";
        this.minArea = 350;
        this.maxArea = 8000;
        this.minBoundingSize = 45;
        this.maxBoundingSize = 250;
        this.spurTh = 6;
        this.minMeanWormFat = 8;
        this.maxMeanWormFat = 45;
        this.minTrueLength = 250;
        this.maxTrueLength = 2000;
    }

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
                if (each.startsWith("minArea") == true) {
                    String[] splitStr = each.split("=");
                    this.minArea = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("maxArea") == true) {
                    String[] splitStr = each.split("=");
                    this.maxArea = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("minBoundingSize") == true) {
                    String[] splitStr = each.split("=");
                    this.minBoundingSize = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("maxBoundingSize") == true) {
                    String[] splitStr = each.split("=");
                    this.maxBoundingSize = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("spurTh") == true) {
                    String[] splitStr = each.split("=");
                    this.spurTh = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("minMeanWormFat") == true) {
                    String[] splitStr = each.split("=");
                    this.minMeanWormFat = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("maxMeanWormFat") == true) {
                    String[] splitStr = each.split("=");
                    this.maxMeanWormFat = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("minTrueLength") == true) {
                    String[] splitStr = each.split("=");
                    this.minTrueLength = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("maxTrueLength") == true) {
                    String[] splitStr = each.split("=");
                    this.maxTrueLength = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("}") == true) {
                    break;
                }
            }

        }

        return this;
    }
}

