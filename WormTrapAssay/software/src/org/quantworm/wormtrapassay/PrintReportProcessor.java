/*
 * Print report function
 */
package org.quantworm.wormtrapassay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class PrintReportProcessor {

    private final JFrame parentFrame;
    private String normalized_Area_Control;
    private String normalized_Area_Experimental;
    private String escape_Time_Control;
    private String escape_Time_Experimental;
    public static final String CHEMO_VIDEO_REPORT_FILE = "Report-WormTrapAssay.txt";

    public PrintReportProcessor(JFrame parent) {
        parentFrame = parent;
    }

    /**
     * Runs the batch process
     *
     * @param fileChooser
     * @return
     */
    public String go(JFileChooser fileChooser) {

        File file = fileChooser.getSelectedFile();
        File[] videoResultFiles = Utilities.getAllFiles(file,
                VideoAnalyzer.CHEMO_VIDEO_RESULT_FILE);

        if (videoResultFiles.length == 0) {
            return "Did not find any " + VideoAnalyzer.CHEMO_VIDEO_RESULT_FILE;
        }

        try {
            FileWriter fileWriter = new FileWriter(
                    fileChooser.getSelectedFile().getAbsolutePath()
                    + File.separator + CHEMO_VIDEO_REPORT_FILE);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter printWriter = new PrintWriter(bufferedWriter);

            //Print head
            printWriter.println("File" + "\tAvg trapped time in Control (sec)\t"
                    + "Avg trapped time in Experimental (sec)\t"
                    + "Escape time in Control (sec)\tEscape time in Experimental (sec)");

            for (int index = 0; index < videoResultFiles.length; index++) {
                File video = videoResultFiles[index];

                // does the .ChemoVideo_Result.txt exist?
                File dotFile = new File(video.getAbsoluteFile().toString());

                if (dotFile.exists()) {
                    read_VideoResultTxt(dotFile.getAbsoluteFile().toString());

                    String fileName;
                    String line;
                    fileName = dotFile.getAbsoluteFile().toString();
                    fileName = fileName.substring(0, fileName.length()
                            - VideoAnalyzer.CHEMO_VIDEO_RESULT_FILE.length());

                    line = fileName + "\t"
                            + normalized_Area_Control + "\t"
                            + normalized_Area_Experimental + "\t"
                            + escape_Time_Control + "\t"
                            + escape_Time_Experimental;
                    printWriter.println(line);

                }

            }

            printWriter.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
            return ioe.getMessage().toString();
        }

        return null;
    }

    /**
     * Read result file
     *
     * @param videoResultFileName
     */
    public void read_VideoResultTxt(String videoResultFileName) {
        //Initialize
        normalized_Area_Control = "";
        normalized_Area_Experimental = "";
        escape_Time_Control = "";
        escape_Time_Experimental = "";

        //Check if file exists
        File resultFile = new File(videoResultFileName);
        if (resultFile.exists() == false) {
            return;
        }

        //Read text file
        List<String> linesList = Utilities.getLinesFromFileWithEndMark(
                resultFile, "#Time");
        if (linesList == null) {
            return;
        }

        //Read value
        for (String each : linesList) {
            String[] items = each.split("\t");
            if (each.startsWith("#Avg trapped time in Control")) {
                normalized_Area_Control = items[1];
            }
            if (each.startsWith("#Avg trapped time in Experimental")) {
                normalized_Area_Experimental = items[1];
            }
            if (each.startsWith("#Escape time in Control")) {
                escape_Time_Control = items[1];
            }
            if (each.startsWith("#Escape time in Experimental")) {
                escape_Time_Experimental = items[1];
            }

            if (each.startsWith("#Time")) {
                break;
            }
        }
    }
}
