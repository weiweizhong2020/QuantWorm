/**
 * Filename: BatchTracking.java
 */
package edu.rice.wormlab.locomotionassay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Performs processing of videos in batch
 */
public class BatchTracking {

    /**
     * file name for tracking-results
     */

    // utilized to show dialog messages on the frame that invoked batch-tracking
    private final JFrame parentFrame;
    private static final PrintStream out = System.out;

    /**
     * Default constructor: loads the default tracking settings and default
     * analysis settings
     */
    public BatchTracking(JFrame parent) {
        parentFrame = parent;
    }

    /**
     * Runs the batch tracking process
     *
     * @param fileChooser file-chooser object to use
     */
    public void go(JFileChooser fileChooser) {

        int returnValue = fileChooser.showOpenDialog(parentFrame);
        if (returnValue != JFileChooser.APPROVE_OPTION) {
            return;
        }; // if

        File file = fileChooser.getSelectedFile();
        File[] videoFiles = Utilities.getAllFiles(file, ".avi");
        if (videoFiles.length == 0) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Did not find any .avi video files." + "\n" + "Nothing to do!",
                    "Did not find any .avi video files.", JOptionPane.INFORMATION_MESSAGE);
            return;
        }; // if



        // need to know whether something has been processed (i.e., there exists a .file file)
        int dotFileCount = 0;
        // also do steps that may require user-input to get it done before processing everything
        for (int index = 0; index < videoFiles.length; index++) {
            File video = videoFiles[ index];

            File dotFile = new File(video.getAbsoluteFile() + ".file");
            if (dotFile.exists() == true) {
                dotFileCount++;
            }; // if
        }; // for


        // ask whether to re-do processing of videos
        boolean redoVideoProcessingFlag = false;
        if (dotFileCount > 0) {
            String[] options = new String[]{"<html>Redo processing of the videos</html>",
                "<html>Skip the videos already processed.<br>Data of all videos data will be included in the report</html>",
                "<html>Cancel,<br>do nothing at all</html>"};
            int response = JOptionPane.showOptionDialog(parentFrame, dotFileCount + " of " + videoFiles.length + " videos have been processed already.\n"
                    + "What would you like to do?",
                    dotFileCount + " of " + videoFiles.length + " videos have been processed already", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[ 1]);
            if (response == -1 || response == 2) {
                return;
            }; // if
            if (response == 0) {
                redoVideoProcessingFlag = true;
            }; // if
        }; // if



        List<String> resultsList = new ArrayList<String>();
        List<String> resultsList_DetailedInfo = new ArrayList<String>();
        

        Tracker tracker = null;
        resultsList.add(Tracker.get_SummaryOutput_Header(false));
        resultsList_DetailedInfo.add(Tracker.get_SummaryOutput_Detailed_Header(false));
        
        
        // from now on, no user-input needed, but we continue to have a few verifications just in case 
        for (int index = 0; index < videoFiles.length; index++) {
            File video = videoFiles[index];
            // read the tracking-settings of the video 


            //For debugging
            if (false) {
                if (video.getParent().toString().lastIndexOf("01plate") > -1
                        && (video.getParent().toString().lastIndexOf("video_4") > -1
                        || video.getParent().toString().lastIndexOf("video_5") > -1
                        || video.getParent().toString().lastIndexOf("video_6") > -1)) {
                } else {
                    continue;
                }


            }


            LinkedList<LinkedList<double[]>> tracksList = null;
            // does the .file exist?
            File dotFile = new File(video.getAbsoluteFile() + ".file");
            
            if (dotFile.exists() == false || redoVideoProcessingFlag == true) {

                // process the video to get the worm-tracks
                try {
                    tracker = new Tracker(video, false, false);
                    tracker.run();
                } catch (Exception ex) {
                    System.out.println("Processing failed:" + video.getAbsolutePath());
                    System.out.println("Reason: " + ex.getMessage());
                    continue;
                }

            } else {
                // retrieve the worm-tracks from .file file
                tracker = new Tracker(video, true, false);
                String error = tracker.readPrecomputedTracks();
                if (error != null) {
                    out.println("\terror: " + error);
                    continue;
                }; // if
            }; // if
            tracksList = tracker.getActiveList();


            if (tracksList == null) {
                continue;
            }



            //Calculate speed info for all tracklist
             resultsList.add(tracker.get_SummaryOutput(
                    video.getAbsolutePath(),
                    tracksList, tracker.getFrameCount(),
                    tracker.getVideoDurationInSec(),
                    tracker.getMicroMeterPerPixels()));
                         


            //Calculate individual spped info
             resultsList_DetailedInfo.add(tracker.get_SummaryOutput_Detailed(
                    video.getAbsolutePath(),
                    tracksList, tracker.getFrameCount(),
                    tracker.getVideoDurationInSec(),
                    tracker.getMicroMeterPerPixels()));

        }


        //Writing summary result
        
        String anyError = writeTrackingResults(file.getAbsolutePath(),
                resultsList,tracker.TRACKING_RESULTS);
        if (anyError != null) {
            JOptionPane.showMessageDialog(parentFrame, 
                    "Unable to write the results.\nDirectory: " + file.getAbsolutePath()
                    + "\nFile: " + tracker.TRACKING_RESULTS,
                    "Unable to write the results.", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(parentFrame,
                "Finished.\nResults were written to:\n" + file.getAbsolutePath() 
                + File.separator + tracker.TRACKING_RESULTS,
                "Finished. ", JOptionPane.INFORMATION_MESSAGE);
        
        
        //Writing detailed result
        writeTrackingResults(file.getAbsolutePath(),
                resultsList_DetailedInfo,tracker.TRACKING_RESULTS_DETAIL);
    }

    /**
     * Writes results (TRACKING_RESULTS) file
     *
     * @param folder the folder into which write the results file
     * @param list contains lines of results to be written
     * @return null when things go OK; otherwise it returns an error message
     */
    public static String writeTrackingResults(String folder, List<String> list, 
            String fileName) {
        if (folder == null) {
            return "Invalid folder (null)";
        }; // if
        if (folder.endsWith(File.separator) == false) {
            folder += File.separator;
        }; // if
        try {
            FileWriter fileWriter = new FileWriter(folder + fileName);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter printWriter = new PrintWriter(bufferedWriter);
            printWriter.println("# File saved at:\t" + new Date());
            printWriter.println("# units: um/sec");
            printWriter.println("# values show 4 decimals (rounded up)");
            for (String each : list) {
                printWriter.println(each);
            }; // for
            printWriter.flush();
            printWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return ex.getMessage();
        }; // try
        return null;
    }
}
