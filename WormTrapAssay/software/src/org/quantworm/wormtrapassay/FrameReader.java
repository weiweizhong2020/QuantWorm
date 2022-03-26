/* 
 * Filename: FrameReader.java
 */
package org.quantworm.wormtrapassay;

import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.media.Buffer;
import javax.media.CannotRealizeException;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Duration;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;

/**
 * Reads a video for the purpose of taking a frame image out of it
 */
public class FrameReader implements ControllerListener {

    private boolean stateTransitionOK = true;
    private Object waitSync = new Object();
    private boolean error = false;
    private boolean failedInitialization = false;
    private Player player = null;
    private FramePositioningControl framePositioningControl = null;
    private FrameGrabbingControl frameGrabbingControl = null;
    private double videoDurationInSec;
    private long videoFrameCount;

    public FrameReader(String filename) {
        MediaLocator mediaLocator = new MediaLocator(filename);
        Manager.setHint(Manager.PLUGIN_PLAYER, true);

        try {
            player = Manager.createRealizedPlayer(mediaLocator);

        } catch (IOException ioe) {
            failedInitialization = true;
        } catch (NoPlayerException npe) {
            failedInitialization = true;
        } catch (CannotRealizeException cre) {
            failedInitialization = true;
        }

        if (failedInitialization == true) {
            System.out.println("unable to create realized player!");
            return;
        }

        player.addControllerListener(this);

        try {
            framePositioningControl = (FramePositioningControl) player.getControl(
                    "javax.media.control.FramePositioningControl");
        } catch (Exception e) {
            failedInitialization = true;
        }

        if (framePositioningControl == null) {
            failedInitialization = true;
        }

        FrameGrabbingControl frameGrabbingControl = (FrameGrabbingControl) player.getControl(
                "javax.media.control.FrameGrabbingControl");
        if (frameGrabbingControl == null) {
            failedInitialization = true;
        }

        Time duration = player.getDuration();
        videoDurationInSec = duration.getSeconds();
        if (duration != Duration.DURATION_UNKNOWN) {
            videoFrameCount = framePositioningControl.mapTimeToFrame(duration);
            if (videoFrameCount != FramePositioningControl.FRAME_UNKNOWN) {
                //System.out.println( "\t" + totalFrames + " frames" );
            }
        }

        try {
            player.prefetch();
        } catch (Exception e) {
            failedInitialization = true;
            e.printStackTrace();
        }

        if (!waitForState(Player.Prefetched)) {
            failedInitialization = true;
        }
    }

    /**
     * @return the VideoDurationInSec
     */
    public double getVideoDurationInSec() {
        return videoDurationInSec;
    }

    /**
     * @return the VideoFrameCount
     */
    public long getVideoFrameCount() {
        return videoFrameCount;
    }

    /**
     * @return the current video time in sec
     */
    public double getCurrentVideoTimeInSec() {
        return player.getMediaTime().getSeconds();
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Grab image frame at a specific time
     *
     * @param seekTimeInNanoSec
     * @return
     */
    public BufferedImage grabImage_At_TimeInNanoSec(long seekTimeInNanoSec) {

        frameGrabbingControl
                = (FrameGrabbingControl) player.getControl(
                        "javax.media.control.FrameGrabbingControl");

        Time seekTime = new Time(seekTimeInNanoSec);

        int seekFrame = framePositioningControl.mapTimeToFrame(seekTime);

        Buffer buffer = null;
        VideoFormat videoFormat = null;
        BufferToImage bufferToImage = null;
        int acturalFrame = framePositioningControl.seek(seekFrame);
        if (acturalFrame != seekFrame) {
            return null;
        }

        if (frameGrabbingControl == null) {
            return null;
        }

        buffer = frameGrabbingControl.grabFrame();

        if (buffer == null) {
            System.out.println("empty buffer");
            return null;
        }
        videoFormat = (VideoFormat) buffer.getFormat();
        bufferToImage = new BufferToImage(videoFormat);
        return (BufferedImage) bufferToImage.createImage(buffer);
    }

    /**
     * Grab frame image at certain location
     *
     * @param frame
     * @return
     */
    public BufferedImage grabImage_At_Frame(int frame) {
        if (failedInitialization) {
            return null;
        }

        frameGrabbingControl = (FrameGrabbingControl) player.getControl("javax.media.control.FrameGrabbingControl");

        Buffer buffer = null;
        VideoFormat videoFormat = null;
        BufferToImage bufferToImage = null;
        int acturalFrame = framePositioningControl.seek(frame);
        if (acturalFrame != frame) {
            return null;
        }

        if (frameGrabbingControl == null) {
            return null;
        }

        buffer = frameGrabbingControl.grabFrame();

        if (buffer == null) {
            System.out.println("empty buffer");
            return null;
        }
        videoFormat = (VideoFormat) buffer.getFormat();
        bufferToImage = new BufferToImage(videoFormat);
        return (BufferedImage) bufferToImage.createImage(buffer);
    }

    boolean waitForState(int state) {
        synchronized (waitSync) {
            try {
                while (player.getState() != state && stateTransitionOK) {
                    waitSync.wait();
                }
            } catch (InterruptedException ie) {
            } catch (Exception e) {
            }
        }
        return stateTransitionOK;
    }

    @Override
    public void controllerUpdate(ControllerEvent controllerEvent) {

        if (error == true) {
            return;
        }
        try {
            if (controllerEvent instanceof javax.media.ControllerErrorEvent) {
                throw new Exception("ControllerErrorEvent, sorry, cannot process this video, bye");
            }
            if (controllerEvent instanceof PrefetchCompleteEvent) {
                synchronized (waitSync) {
                    stateTransitionOK = true;
                    waitSync.notifyAll();
                }
            }
            if (controllerEvent instanceof EndOfMediaEvent) {
                player.stop();
                player.close();
            }
        } catch (Exception e) {
            error = true;
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        }
    }
}
