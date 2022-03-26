/**
 * Filename: VideoFileFilter.java
 */
package org.quantworm.wormtrapassay;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 * Basic selection of video files (e.g., .avi)
 */
public class VideoFileFilter extends FileFilter {

    @Override
    public boolean accept(File file) {
        if (file.isDirectory() == true) {
            return true;
        }
        if (file.getName().toLowerCase().endsWith(".avi") == true) {
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Video Files ( *.avi )";
    }

}
