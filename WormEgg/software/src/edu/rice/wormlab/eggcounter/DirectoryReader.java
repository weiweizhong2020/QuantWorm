/*
 * Filename: DirectoryReader.java
 * This class is to scan sub-folders and return all folder names
 */

package edu.rice.wormlab.eggcounter;


import java.io.File;
import java.util.Arrays;

public class DirectoryReader {

    public final int FOLDER_ARRAY_LIMIT = 1000;
    static int spc_count = -1;
    public int folderCountIndex = -1;
    public String folderArray[] = new String[FOLDER_ARRAY_LIMIT];

    //Recursive process
    private void process(File aFile) {
        spc_count++;

        if (aFile.isDirectory()) {

            folderCountIndex++;
            folderArray[folderCountIndex] = aFile.getPath();

            if (folderCountIndex < FOLDER_ARRAY_LIMIT) {
                File[] listOfFiles = aFile.listFiles();
                if (listOfFiles != null) {
                    for (int i = 0; i < listOfFiles.length; i++) {
                        process(listOfFiles[i]);
                    }
                }
            }

        }

        spc_count--;
    }

    //Return a list of all sub-folders
    public String[] scanAllSubFolders(String nam) {
        File aFile = new File(nam);
        process(aFile);

        String[] tempArray= Arrays.copyOf(folderArray, folderCountIndex + 1);

        return tempArray;
    }
}
