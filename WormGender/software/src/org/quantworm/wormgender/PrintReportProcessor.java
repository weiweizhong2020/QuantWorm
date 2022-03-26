/*
 * Print report function
 */
package org.quantworm.wormgender;

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
    private int nMale;
    private int nHerm;
    private String statusMsg;
    private int inspectedCount;
    public static final String WORMGENDER_REPORT_FILE = "report-gender.txt";

    public PrintReportProcessor(JFrame parent) {
        parentFrame = parent;
        inspectedCount = 0;
    }

    /**
     * Runs the batch process
     *
     * @param fileChooser
     * @return
     */
    public String go(JFileChooser fileChooser) {

        File file = fileChooser.getSelectedFile();
        File[] resultFiles
                = DirectoryReader.getAllFilesInAllSubfolders(file,
                        ResultsGender.RESULTS_TXT);

        if (resultFiles.length == 0) {
            return "Did not find any " + ResultsGender.RESULTS_TXT;
        }
        int folderCount = 0;
        inspectedCount = 0;

        try {
            FileWriter fileWriter = new FileWriter(
                    fileChooser.getSelectedFile().getAbsolutePath()
                    + File.separator + WORMGENDER_REPORT_FILE);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter printWriter = new PrintWriter(bufferedWriter);

            //Print head
            printWriter.println("File" + "\tnHerm\tnMale\tstatus");

            for (File resultsFile : resultFiles) {
                if (ResultsGender.RESULTS_TXT.equalsIgnoreCase(resultsFile.getName()) == false) {
                    continue;
                }

                File dotFile = new File(resultsFile.getAbsoluteFile().toString());

                if (dotFile.exists()) {
                    read_ResultTxt(dotFile.getAbsoluteFile().toString());

                    //if invalid result file is found, then skip it
                    if (nHerm == -1 || nMale == -1 || statusMsg.equals("")) {
                        continue;
                    }

                    String fileName;
                    String line;
                    fileName = dotFile.getAbsoluteFile().toString();
                    fileName = fileName.substring(0, fileName.length()
                            - ResultsGender.RESULTS_TXT.length());

                    line = fileName + "\t"
                            + nHerm + "\t"
                            + nMale + "\t"
                            + statusMsg;

                    printWriter.println(line);
                    folderCount++;
                }
            }
            printWriter.close();

        } catch (IOException ioe) {
            return ioe.getMessage();
        }

        statusMsg = folderCount + " folders.\n" + inspectedCount + " folders inspected.";

        return null;
    }

    /**
     *
     * @return
     */
    public String getStatus() {
        return statusMsg;
    }

    /**
     * Read result file
     *
     * @param resultFileName
     */
    public void read_ResultTxt(String resultFileName) {
        //Initialize
        nHerm = -1;
        nMale = -1;
        statusMsg = "";

        //Check if file exists
        File resultFile = new File(resultFileName);
        if (resultFile.exists() == false) {
            return;
        }

        //Read text file
        List<String> linesList = Utilities.getLinesFromFile(resultFile);
        if (linesList == null) {
            return;
        }

        //Read value
        for (String each : linesList) {
            String[] items = each.split("\t");
            if (each.startsWith("# Herm count")) {
                nHerm = Utilities.getInteger(items[1]);
            }
            if (each.startsWith("# Male count")) {
                nMale = Utilities.getInteger(items[1]);
            }
            if (each.startsWith("# Status")) {
                statusMsg = items[1];
                if (ResultsGender.INSPECTED.equalsIgnoreCase(items[ 1]) == true) {
                    inspectedCount++;
                }
                break;
            }

        }
    }
}
