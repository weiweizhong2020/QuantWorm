/*
 * Filename: DetectionCondition.java
 */
package org.quantworm.wormcounter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains important condition to detect worms.
 *
 * @author SangKyu Jung
 */
public class DetectionCondition {

    public String presetTitle;
    public int min_WormSize;                 //Pixel unit
    public int max_WormSize;                 //Pixel unit
  

    // Common size adult worm detection
    public DetectionCondition() {
        this.presetTitle = "Default";
        this.min_WormSize = 50;
        this.max_WormSize = 6000;

    }

    public static String[] get_DetectionConditionPreset_Titles(String fileName) {
        File file = new File(fileName);
        if (file.exists() == false) {
		System.out.println( "did not find it, " + fileName );
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

    public  DetectionCondition set_DetectionConditionPreset(String fileName, String profileTitle) {
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
                if (each.startsWith("min_WormSize") == true) {
                    String[] splitStr = each.split("=");
                    this.min_WormSize = Utilities.getInteger(splitStr[1].trim());
                }
                if (each.startsWith("max_WormSize") == true) {
                    String[] splitStr = each.split("=");
                    this.max_WormSize = Utilities.getInteger(splitStr[1].trim());
                }
              
                if (each.startsWith("}") == true) {
                    break;
                }
            }

        }

        return this;
    }
}

