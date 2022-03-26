/*
 * Filename: TestUnit.java
 */

package edu.rice.wormlab.lifespan;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TestUnit {
    
    public JFrame outframe = new JFrame("Current image");
    public JImagePanel imgPanel;
    public boolean IsShowImage = true;
    
    
    
     public void displayImage(JFrame frame, BufferedImage srcImage) {
        imgPanel.image = srcImage;
        imgPanel.updateUI();
        frame.setVisible(IsShowImage);
    }

    public void saveImage(BufferedImage img, String fullPathFileName) {
        try {
            String format = (fullPathFileName.endsWith(".png")) ? "png" : "jpg";
            ImageIO.write(img, format, new File(fullPathFileName));
        } catch (IOException e) {
            //do nothing
        }

    }

    public class JImagePanel extends JPanel {

        // default serial-version-ID
		private static final long serialVersionUID = 1L;
		
		private BufferedImage image;
        int x, y;

        public JImagePanel(BufferedImage image, int x, int y) {
            super();
            this.image = image;
            this.x = x;
            this.y = y;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, x, y, null);
        }
    }

    
    public void setUI() {

        //Setting Image Viewer Window
        outframe.setBounds(150, 150, 660, 530);
        outframe.setLocation(150, 150);
        
        imgPanel = new JImagePanel(null, 0, 0);
        outframe.add(imgPanel);
        outframe.setVisible(IsShowImage);

    }
    
    
    
    public static void main(String[] args) {
        TestUnit tu = new TestUnit();
        
        //Declare instance of NativeImgProcessing
        NativeImgProcessing imgProc = new NativeImgProcessing();
        //Initialize class

                
        BufferedImage srcImg;
        
        
        //Setup window and show original image
        tu.setUI();
        srcImg = imgProc.loadImage("C:/TestAutoThresholdingImage/Test image Set 3/Test2.jpeg");
        
       
        tu.displayImage(tu.outframe, srcImg);
        tu.outframe.setVisible(true);
        
        
        //Run do_AdaptiveThresholding
        short[][] srcPixelArray;
        srcPixelArray = imgProc.convert_Image_To_GrayShortArray(srcImg);

        srcImg =imgProc.adaptiveThresholding_Core(srcPixelArray, 15, 0.2f, 200);
        imgProc.saveImage(srcImg, "bmp", "C:/TestAutoThresholdingImage/Test image Set 3/Test2adaptive.bmp");
        //Show window with result bw image
        tu.displayImage(tu.outframe, srcImg);
        tu.outframe.setVisible(true);
    }
}
