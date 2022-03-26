/*
 * Filename: DirectoryReader.java
 * This class is to scan sub-folders and return all folder names
 */
package org.quantworm.wormgender;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DirectoryReader {

    /**
     * Finds all sub folders of a given folder
     *
     * @param folder the folder to start with
     * @return a list of all sub-folders
     */
    public static List<String> getAllSubFolders(String folder) {
        List<String> retList = new ArrayList<String>();
        File aFile = new File(folder);
        getAllSubFolders_aux(aFile, retList);
        return retList;
    }

    /**
     * (helper method) Recursively finds all sub folders of a given folder
     *
     * @param folder a folder
     * @param list the list into which to put the folders
     */
    protected static void getAllSubFolders_aux(File folder, List<String> list) {
        if (folder.isDirectory() == true) {
            list.add(folder.getPath());
            File[] files = folder.listFiles();
            if (files != null) {
                for (File each : files) {
                    getAllSubFolders_aux(each, list);
                }
            }
        }
    }

    /**
     * Get all files in all subfolders such that they end with the specified
     * suffix
     *
     * @param directory the directory to start at
     * @param suffix the suffix
     * @return an array of File objects
     */
    public static File[] getAllFilesInAllSubfolders(File directory, String suffix) {
        List<File> retList = new ArrayList<File>();
        LinkedList<File> list = new LinkedList<File>();
        File[] insideFiles = directory.listFiles();
        list.addAll(Arrays.asList(insideFiles));
        while (list.isEmpty() == false) {
            File tmp = list.removeFirst();
            if (tmp.isDirectory() == true) {
                File[] insideFiles2 = tmp.listFiles();
                if (insideFiles2 != null) {
                    list.addAll(Arrays.asList(insideFiles2));
                }
            } else {
                if (tmp.getName().endsWith(suffix)) {
                    retList.add(tmp);
                }
            }
        }
        return retList.toArray(new File[0]);
    }
}
