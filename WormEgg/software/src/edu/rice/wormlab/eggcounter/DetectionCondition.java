/*
 * Filename: DetectionCondition.java
 * This class contains worm detection parameters.
 */
package edu.rice.wormlab.eggcounter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class DetectionCondition {

    public String presetTitle;
    
    //Minimum bounding box size (in the pixel unit) in the CannyEdge_RegionExt image
    public int min_BoundingSize_In_CannyEdgeRegion = 3;
    
    //Maximum bounding box size (in the pixel unit) in the CannyEdge_RegionExt image
    public int max_BoundingSize_In_CannyEdgeRegion = 20;
    
    //Minimum egg area (in the pixel unit) in the CannyEdge_RegionExt image
    public int min_ObjectSize_In_CannyEdgeRegion = 30;
    
    //Maximum egg area (in the pixel unit) in the CannyEdge_RegionExt image
    public int max_ObjectSize_In_CannyEdgeRegion = 100;
    
    //Minimum  circularity (sqrt of length from center) in the CannyEdge_RegionExt image
    public double min_SqrtDiffRadius_In_CannyEdgeRegion = 4;
    
    //Minimum  circularity (sqrt of length from center) in the Canny Edge gap-filled image
    public double min_SqrtDiffRadius_In_FilledRegion = 2;
    
    //Minimum radius (in the pixel unit) of egg in the Binarized image
    public double min_AvgRadius_In_ThresholdRegion = 20;
    
    //Minimum bounding box size (in the pixel unit) of egg in the Binarized image
    public int min_BoundingSize_In_ThresholdRegion = 5;
    
    //Maximum bounding box size (in the pixel unit) of egg in the Binarized image
    public int max_BoundingSize_In_ThresholdRegion = 40;
    
    //Minimum egg area (in the pixel unit) in the Binarized image
    public int min_ObjectSize_In_ThresholdRegion = 400;
    
    //Maximum distance (in the pixel unit) of objects to be duplicate findings
    public double max_Distance_For_DuplicateFinding = 3;

    
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
                if (each.startsWith("min_BoundingSize_In_CannyEdgeRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.min_BoundingSize_In_CannyEdgeRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("max_BoundingSize_In_CannyEdgeRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.max_BoundingSize_In_CannyEdgeRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_ObjectSize_In_CannyEdgeRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.min_ObjectSize_In_CannyEdgeRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("max_ObjectSize_In_CannyEdgeRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.max_ObjectSize_In_CannyEdgeRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_SqrtDiffRadius_In_CannyEdgeRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.min_SqrtDiffRadius_In_CannyEdgeRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_SqrtDiffRadius_In_FilledRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.min_SqrtDiffRadius_In_FilledRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_AvgRadius_In_ThresholdRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.min_AvgRadius_In_ThresholdRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_BoundingSize_In_ThresholdRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.min_BoundingSize_In_ThresholdRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("max_BoundingSize_In_ThresholdRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.max_BoundingSize_In_ThresholdRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("min_ObjectSize_In_ThresholdRegion") == true) {
                    String[] splitStr = each.split("=");
                    this.min_ObjectSize_In_ThresholdRegion = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("max_Distance_For_DuplicateFinding") == true) {
                    String[] splitStr = each.split("=");
                    this.max_Distance_For_DuplicateFinding = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("}") == true) {
                    break;
                }
            }

        }

        return this;
    }
}
