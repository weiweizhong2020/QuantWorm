package org.quantworm.wormgender;

import ij.*;
import ij.process.*;

/**
 *
 * @author
 * G.Landini---http://www.dentistry.bham.ac.uk/landinig/software/software.html
 */
public class imClearBorder {

    /**
     * Ask for parameters and then execute
     *
     * @param imp.
     * @return
     */
    public static ImagePlus imclearborder(ImagePlus imp) {
        ImagePlus result = null;
        // 1 - Obtain the currently active image if necessary:

        if (null == imp) {
            return null;
        }

        int stackSize = imp.getStackSize();
        if (imp.getBitDepth() != 8) {
            return null;
        }
        ImageStatistics stats = imp.getStatistics();
        if (stats.histogram[0] + stats.histogram[255] != stats.pixelCount) {
            return null;
        }

        // 2 - Ask for parameters:
        // 3 - Retrieve parameters from the dialog
        boolean doIstack = false;
        boolean killTop = true;
        boolean killRight = true;
        boolean killBottom = true;
        boolean killLeft = true;
        boolean whiteParticles = true;
        boolean connect4 = false;

        if (stackSize > 1) {
            doIstack = true;
        }

        // 4 - Execute!
        if (stackSize > 1 && doIstack) {
            for (int j = 1; j <= stackSize; j++) {
                imp.setSlice(j);
                result = exec(imp, killTop, killRight, killBottom, killLeft, whiteParticles, connect4);
            }
            imp.setSlice(1);
        } else {
            result = exec(imp, killTop, killRight, killBottom, killLeft, whiteParticles, connect4);
        }
        // 5 - If all went well, show the image:
        imp.updateAndDraw();
        return result;
    }

    /**
     * Execute the plugin functionality: duplicate and scale the given image.
     *
     * @param imp
     * @param killTop
     * @param killLeft
     * @param killRight
     * @param whiteParticles
     * @param killBottom
     * @param connect4
     * @return an Object[] array with the name and the scaled ImagePlus. Does
     * NOT show the new, image; just returns it.
     */
    public static ImagePlus exec(ImagePlus imp, boolean killTop,
            boolean killRight, boolean killBottom, boolean killLeft,
            boolean whiteParticles, boolean connect4) {

        // 0 - Check validity of parameters
        if (null == imp) {
            return null;
        }

        int width = imp.getWidth();
        int height = imp.getHeight();
        int xem1 = width - 1;
        int yem1 = height - 1;
        int i, offset;
        int foreground = 255, background = 0;
        ImageProcessor ip;

        ip = imp.getProcessor();

        ip.snapshot(); //undo
        Undo.setup(Undo.FILTER, imp);

        byte[] pixels = (byte[]) ip.getPixels();

        // 1 - Perform the magic
        if (!whiteParticles) {
            foreground = 0;
            background = 255;
        }

        FloodFiller ff = new FloodFiller(ip);
        ip.setColor(background);

        if (connect4) {
            if (killTop) {
                for (i = 0; i < width; i++) {
                    if ((int) (pixels[i] & 0xff) == foreground) {
                        ff.fill(i, 0);
                    }
                }
            }
            if (killRight) {
                for (i = 0; i < height; i++) {
                    if ((int) (pixels[xem1 + i * width] & 0xff) == foreground) {
                        ff.fill(xem1, i);
                    }
                }
            }
            if (killBottom) {
                offset = yem1 * width;
                for (i = 0; i < width; i++) {
                    if ((int) (pixels[offset + i] & 0xff) == foreground) {
                        ff.fill(i, yem1);
                    }
                }
            }
            if (killLeft) {
                for (i = 0; i < height; i++) {
                    if ((int) (pixels[i * width] & 0xff) == foreground) {
                        ff.fill(0, i);
                    }
                }
            }
        } else {
            if (killTop) {
                for (i = 0; i < width; i++) {
                    if ((int) (pixels[i] & 0xff) == foreground) {
                        ff.fill8(i, 0);
                    }
                }
            }
            if (killRight) {
                for (i = 0; i < height; i++) {
                    if ((int) (pixels[xem1 + i * width] & 0xff) == foreground) {
                        ff.fill8(xem1, i);
                    }
                }
            }
            if (killBottom) {
                offset = yem1 * width;
                for (i = 0; i < width; i++) {
                    if ((int) (pixels[offset + i] & 0xff) == foreground) {
                        ff.fill8(i, yem1);
                    }
                }
            }
            if (killLeft) {
                for (i = 0; i < height; i++) {
                    if ((int) (pixels[i * width] & 0xff) == foreground) {
                        ff.fill8(0, i);
                    }
                }
            }
        }
        return imp;
    }

}
