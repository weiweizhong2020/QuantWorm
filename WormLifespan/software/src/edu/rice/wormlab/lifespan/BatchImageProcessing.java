/*
 * Filename: BatchImageProcessing.java
 * This class contains functions for batch processing.
 */
package edu.rice.wormlab.lifespan;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BatchImageProcessing {

    public static DetectionCondition detectionCondition = new DetectionCondition();
    public static MotionDetection motionDetection = new MotionDetection(null);

    /**
     * Recursively performs image-processing operation in all sub-folders
     *
     * @param  directory  the directory to start with
     * @return  outcome-map containing folders processed with message (such as error or null)
     */
    public static Map<String, String> recursiveImageProcessing(File directory) {
        if (directory == null) {
            return null;
        }; // if
        Map<String, String> ret = new TreeMap<String, String>();
        recursiveImageProcessing(directory, ret);
        return ret;
    }

    /*
     * Recursively performs image-processing operation in all sub-folders (private)
     * @param  directory  the directory to start with
     * @param  outcomeMap  Map of folders to message (either error or null)
     */
    private static void recursiveImageProcessing(File folder, Map<String, String> outcomeMap) {
        if (folder == null) {
            return;
        }; // if

        // does this folder contain images?
        int imageFilesCount = Utilities.countPiecesFiles(folder.getAbsolutePath());
        File theFile = new File(folder.getAbsolutePath() + File.separator + Utilities.ASSEMBLED_JPEG);
        boolean existingAssembleOrgFileFlag = theFile.exists();
        
        if (imageFilesCount > 0 || existingAssembleOrgFileFlag) {
            String error = imageProcessing(folder);
            outcomeMap.put(folder.getAbsolutePath(), error);
        }; // if

        // get the sub-directories
        List<File> subdirectoriesList = new ArrayList<File>();
        File[] subfolders = folder.listFiles();
        for (File eachFolder : subfolders) {
            if (eachFolder.isDirectory() == true) {
                subdirectoriesList.add(eachFolder);
            }; // if
        }; // for

        // recursion happens here
        for (File each : subdirectoriesList) {
            recursiveImageProcessing(each, outcomeMap);
        }; // for
    }

    /**
     * Performs image-processing on a directory: assemble the image
     *
     * @param directory the directory
     * @return null when things go OK, otherwise an error message
     */
    public static String imageProcessing(File directory) {
        ScannerLog scannerLog = ScannerLog.readLog(directory);
        String error = scannerLog.getErrors();
        if (error != null) {
            return error;
        }; // if
        
        if (directory.getName().endsWith(App.UNDERSCORE_UNDERSCORE_ONE) == false) {
            return null;
        }
        
        File resultsFile = new File(directory.getAbsoluteFile() 
                            + File.separator + MotionDetection.N_LIVE_RESULTS_TXT);
        if (resultsFile.exists() == true) {
            resultsFile.delete();
        }
        
        motionDetection.setFolders(directory, directory);

        String errors = motionDetection.do_imageProcessing();
        if( errors != null ) {
			  return errors;
		  }; // if
        MotionDetection.saveInspectionResults( false, null, motionDetection.getWormsList(), motionDetection.getFolder(), motionDetection.getAssembledImage() );

        return null;
    }
}
