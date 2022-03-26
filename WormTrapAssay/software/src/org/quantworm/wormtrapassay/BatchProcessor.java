/**
 * Filename: BatchProcessor.java
 */
package org.quantworm.wormtrapassay;

import java.io.File;
import java.io.PrintStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Performs processing of videos in batch
 */
public class BatchProcessor {

    private final JFrame parentFrame;
    private static final PrintStream out = System.out;

    /**
     * Default constructor: loads the default tracking settings and default
     * analysis settings
     *
     * @param parent
     */
    public BatchProcessor(JFrame parent) {
        parentFrame = parent;
    }

    /**
     * Runs the batch tracking process
     *
     * @param fileChooser
     */
    public void go(JFileChooser fileChooser) {

        int returnValue = fileChooser.showOpenDialog(parentFrame);
        if (returnValue != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        File[] videoFiles = Utilities.getAllFiles(file, ".avi");
        if (videoFiles.length == 0) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Did not find any .avi video files." + "\n" + "Nothing to do!",
                    "Did not find any .avi video files.", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // need to know whether something has been processed (i.e., there exists a .file file)
        int dotFileCount = 0;

        // also do steps that may require user-input to get it done before processing everything
        for (int index = 0; index < videoFiles.length; index++) {
            File video = videoFiles[ index];

            File dotFile = new File(video.getAbsoluteFile()
                    + VideoAnalyzer.CHEMO_VIDEO_RESULT_FILE);
            if (dotFile.exists() == true) {
                dotFileCount++;
            }
        }

        //Ask whether to re-do processing of videos
        boolean redoVideoProcessingFlag = false;
        if (dotFileCount > 0) {
            String[] options = new String[]{"<html>Redo processing of the videos</html>",
                "<html>Skip the videos already processed</html>",
                "<html>Cancel,<br>do nothing at all</html>"};
            int response = JOptionPane.showOptionDialog(parentFrame, dotFileCount
                    + " of " + videoFiles.length + " videos have been processed already.\n"
                    + "What would you like to do?",
                    dotFileCount + " of " + videoFiles.length
                    + " videos have been processed already",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[ 1]);
            if (response == -1 || response == 2) {
                return;
            }
            if (response == 0) {
                redoVideoProcessingFlag = true;
            }
        }

        // from now on, no user-input needed, but we continue to have a few verifications just in case 
        for (int index = 0; index < videoFiles.length; index++) {
            File video = videoFiles[index];

            // does the .ChemoVideo_Result.txt exist?
            File dotFile = new File(video.getAbsoluteFile()
                    + VideoAnalyzer.CHEMO_VIDEO_RESULT_FILE);

            if (dotFile.exists() == false || redoVideoProcessingFlag == true) {

                // process the video to get the worm-tracks
                try {
                    VideoAnalyzer videoAnalyzer = new VideoAnalyzer(video);
                    videoAnalyzer.run();
                    videoAnalyzer = null;
                } catch (Exception ex) {
                    System.out.println("Processing failed:" + video.getAbsolutePath());
                    System.out.println("Reason: " + ex.getMessage());
                    continue;
                }

            }
            video = null;
            dotFile = null;

        }

        JOptionPane.showMessageDialog(parentFrame,
                "Batch processing completed",
                "Finished. ", JOptionPane.INFORMATION_MESSAGE);

    }

}
