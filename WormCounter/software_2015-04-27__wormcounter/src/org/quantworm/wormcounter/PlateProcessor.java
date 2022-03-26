/*
 * Filename: PlateProcessor.java
 */

package org.quantworm.wormcounter;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.io.FileSaver;
import ij.plugin.MontageMaker;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ByteProcessor;
import ij.process.ByteStatistics;
import ij.process.FloodFiller;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.PolygonFiller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/** 
 * Assembles the plate image and counts worms 
 */

public class PlateProcessor {
	int[][] componentLabel;
	int nLabel;
        
	public static DetectionCondition detectionCondition = new DetectionCondition();
   public NativeImgProcessing imgProc = new NativeImgProcessing();
   public LineMath lineMath = new LineMath();
	
	/**
	 * Constructor
	 * @param  maskFileName  the mask file name
	 */
	public PlateProcessor( String maskFileName ) { 
		loadMask( maskFileName );
	}



	/**
	 * Recursively processes a directory and all its subdirectories
	 * @param  directory  the directory
	 */
	public void recursivelyProcessDirectory( String directory ) {
		File dir = new File( directory );
		if( dir.exists() == false ) {
			// Directory does not exist
			return;
		}; // if
		
		if( dir.isDirectory() == false ) {
			// This is not a directory
			return;
		}; // if

		// get the sub-directories
		List<String> subdirectoriesList = new ArrayList<String>();
		File[] folders = dir.listFiles();
		for( File eachFolder : folders ) {
			if( eachFolder.isDirectory() == false ) {
				continue;
			}; // if
			subdirectoriesList.add( eachFolder.getAbsolutePath() );
		}; // for

		String error = avoidanceAssay( dir.getAbsolutePath() );
		if( error == null ) {
			// Done with dir
		}
		else {
			if( subdirectoriesList.isEmpty() == true ) {
				// Skipping  + dir
			}
			else {
				// Skipping dir but will look into its subdirectories
			}; // if
		}; // if

		// recursion happens here
		for( String subdirectory : subdirectoriesList ) {
			recursivelyProcessDirectory( subdirectory );
		}; // for
	}

    
	/**
	 * Computes the avoidance assay values
	 * @param  dirName  the name of the directory containing images
	 * @return  null when things went OK; 
	 * otherwise a description of error
	 */
	public String avoidanceAssay( String dirName ) {
		ImagePlus imp;
                
		//load plate image
		ByteProcessor plate = assembleAndAlignImages(dirName);

		// see whether we can continue
		if( plate == null ) {
			return "no images";
		}; // if

		
		//binarize plate image
		plate=binarize(plate);
                
                
		//match valid area on mask
		plate=matchMask(plate);
		
        //count worms
		int wormCount=0; 
		int[] wormArea=new int[1000]; //assume there are fewer than 1000 worms	
		LinkedList<WormInfo> worms=new LinkedList<WormInfo>();
    	Wand wand = new Wand(plate);
		for (int y=0; y<plate.getHeight(); y++) {
			for (int x=0; x<plate.getWidth(); x++){
				if (plate.getPixelValue(x, y)!=0) {
					continue; //skip white pixels
				}
			       		
			   	//mark particle as ROI and get its boundary.
			    wand.autoOutline(x,y); 
		        if (wand.npoints==0){
					  IJ.log("wand error: "+x+" "+y); 
					  continue;
					}
		        Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
		        Rectangle r = roi.getBounds();    
		        plate.setRoi(roi); 
		        PolygonFiller pf=new PolygonFiller();
				PolygonRoi proi = (PolygonRoi)roi;
				pf.setPolygon(proi.getXCoordinates(), proi.getYCoordinates(), proi.getNCoordinates());
				plate.setMask(pf.getMask(r.width, r.height));
				ImageProcessor mask0 = plate.getMask();
						
				ImageStatistics stats = new ByteStatistics(plate);	

				//if a particle is too small, consider it background
				if (stats.pixelCount<detectionCondition.min_WormSize) {
					plate.setValue(255);
					plate.fill(mask0);
					continue;
				}		
					
				//if a particle is too big and close to the edge of the plate, consider it background
				if (stats.pixelCount>detectionCondition.max_WormSize){
					double r2=(plate.getHeight()/2-y)*(plate.getHeight()/2-y)+(plate.getWidth()/2-x)*(plate.getWidth()/2-x);
					double r_border=plate.getHeight()/2*3/4;
					if (r2>r_border*r_border) {
						plate.setValue(255);
						plate.fill(mask0);
						continue;
					}
				}
					
				//record worm particle
				WormInfo worm=new WormInfo();
				worm.pX=r.x; 
				worm.pY=r.y;
				worm.width=r.width; 
				worm.height=r.height;
				worm.area=stats.pixelCount;
				worm.label=componentLabel[x][y];
				worms.add(worm);
		        
				wormArea[wormCount]=worm.area;
				wormCount++;
				
				//mark off particle
				plate.setValue(10);
				plate.fill(mask0);
			}
		}
		plate.resetRoi();		
		
		//find medium area as single worm area
		int singleWorm;
		int[] wormArea1=new int[wormCount];
		for (int i=0; i<wormCount; i++){
			wormArea1[i]=wormArea[i];
		}
		Arrays.sort(wormArea1);
		singleWorm=wormArea1[(wormCount-1)/2];
		
		//print out worm stats
		try{
			BufferedWriter out=new BufferedWriter(new FileWriter(dirName + File.separator + App.RESULT_TXT ));
			out.write("# Particle Count:\t"+wormCount+"\n");
			out.write("# Single Worm Area:\t"+singleWorm+"\n");
			out.write("# Component Count:\t"+nLabel+"\n");
			out.write("# pX\tpY\twidth\theight\tnWorm\tarea\tlabel No\n");
			
			ListIterator<WormInfo> it=worms.listIterator();
			while(it.hasNext()){
				WormInfo worm=(WormInfo)it.next();
				int nWorm=(int)((double)(worm.area)/(double)singleWorm+0.5);
				if (nWorm<1 && worm.area*3>=singleWorm) {
					nWorm++;
				}
				if (nWorm<1) {
					continue;
				}
				out.write(worm.pX+"\t"+worm.pY+"\t"+worm.width+"\t"+worm.height+"\t"+nWorm+"\t"+worm.area+"\t"+worm.label+"\n");
			}
			out.close();
		}
		catch( IOException e ){
			e.printStackTrace();
			System.out.println(e);
		}
		return null;
	} 
    
    public void loadMask(String fileName){
    	//load image, convert to 8-bit grayscale
		ImagePlus imp = new ImagePlus(fileName);
		ImageConverter imgcvt=new ImageConverter(imp);
		imgcvt.convertToGray8();
		ByteProcessor bp=(ByteProcessor)imp.getProcessor();
		
		//binarize
		bp.threshold(bp.getAutoThreshold());
		
		//label each pixel which component it belongs to
		this.componentLabel=new int[bp.getWidth()][bp.getHeight()];
		int label=1;
		FloodFiller ff=new FloodFiller(bp);
		
		for (int y=0; y<bp.getHeight(); y++) {
			for (int x=0; x<bp.getWidth(); x++){
				if (bp.getPixelValue(x, y)!=0) {
					continue; //skip white pixels
				}
				
				bp.setValue(label);
				ff.fill8(x,y);
				label++;
			}
		}
		
		//convert the image into an array
		for (int y=0; y<bp.getHeight(); y++) {
			for (int x=0; x<bp.getWidth(); x++){
				if (bp.getPixelValue(x, y)==255) {
					componentLabel[x][y]=0;
					continue; 
				}
				componentLabel[x][y]=(int)bp.getPixelValue(x, y);
			}
		}
		this.nLabel=label-1;
    }

	/**
    * Assemble and align images
	 * @param  dirName  the name of directory containing the images
	 * @return  the assembled image; 
	 *          otherwise null when unable to create the assembled image
	 */
    public ByteProcessor assembleAndAlignImages(String dirName) {
        ScannerLog scannerLog = ScannerLog.readLog(dirName);
		  String errors = scannerLog.getErrors();
		  if( errors != null ) { 
			  // there were errors, we leave
			  return null;
		  }; // if
        if (scannerLog == null) {
            return null;
        }
        //Assemble original image
        BufferedImage originalImage = Utilities.assembleImage_CreateNewly(
                dirName, false, scannerLog).getBufferedImage();
        //Do not delete for debugging
        //BufferedImage originalImage = Utilities.assembleImage_TestUnit(
        //        dirName, false, scannerLog).getBufferedImage();
        if (originalImage == null) {
            return null;
        }

        //Save original image
        imgProc.saveImage(originalImage, "jpeg",
                dirName + File.separator + "assembled_org.jpeg");


        //Conduct background subtraction
        ByteProcessor plate = do_LocalBackgroundSubtraction(originalImage,
                640, 480);

        //Do not delete for debugging                
        //outputImage(plate, dirName + File.separator +
        //            "assembled_org_bksubtract.jpeg"); 


        //Conduct alignment. Image is not aligned yet.
        //double[] resultIntArray = Calculate_CenterAndAngle(originalImage);
		  // (Aleman) I'm skipping alignment because we need a quick fix. 4/27/2015
        double[] resultIntArray = null;


        //Do not conduct alignment if no central line is found
        if (resultIntArray == null) {
            imgProc.saveImage(originalImage, "jpeg",
                    dirName + File.separator + "assembled.jpeg");
            return plate;
        }


        //Align images
        //Rotate and shift image, and then save images
        int centerX = (int) resultIntArray[0];
        int centerY = (int) resultIntArray[1];
        double angleInDegree = resultIntArray[2];

        originalImage = imgProc.rotateAndShiftImage(originalImage,
                centerX, centerY, angleInDegree, true, Color.white);
        imgProc.saveImage(originalImage, "jpeg",
                dirName + File.separator + "assembled.jpeg");


        ImagePlus bkSubtractedImgPls = new ImagePlus(null, plate);
        bkSubtractedImgPls.setImage(imgProc.rotateAndShiftImage(
                bkSubtractedImgPls.getBufferedImage(),
                centerX, centerY, angleInDegree, true, Color.white));
        plate = (ByteProcessor) bkSubtractedImgPls.getProcessor();

        //Do not delete for debugging
        //imgProc.saveImage(bkSubtractedImgPls.getBufferedImage(), "jpeg",
        //        dirName + File.separator + "assembled_org_bksubtracted_aligned.jpeg");


        return plate;
    }

    //Conduct Hough transform and get finding
    //Return as integer array
    //double[0]: center x
    //double[1]: center y
    //double[2]: angle
    public double[] Calculate_CenterAndAngle(BufferedImage srcBufferedImage) {
        double[] resultIntArray = new double[3];

        //Alignment will be done using small size image
        final float resizingPercent = 25;
        final int resizedImageWidth =
                (int) (srcBufferedImage.getWidth() * resizingPercent / 100);
        final int resizedImageHeight =
                (int) (srcBufferedImage.getHeight() * resizingPercent / 100);

        //Assign region to detect the center of a plate
        final int detectingRegionX1 =
                (int) (resizedImageWidth * 0.3);
        final int detectingRegionY1 =
                (int) (resizedImageHeight * 0.3);
        final int detectingRegionX2 =
                (int) (resizedImageWidth * 0.7);
        final int detectingRegionY2 =
                (int) (resizedImageHeight * 0.7);


        //Create small size image
        BufferedImage resized = imgProc.resizeImage(
                srcBufferedImage,
                BufferedImage.TYPE_BYTE_GRAY,
                resizedImageWidth, resizedImageHeight);


        //Conduct resizing and adaptive thresholding to create binary image
        short[][] srcGrayShortArray =
                imgProc.convert_GrayImage_To_GrayShortArray(resized);
        resized = imgProc.adaptiveThresholding_Core(srcGrayShortArray, 25, 0.2f, 255);
        srcGrayShortArray = imgProc.convert_GrayImage_To_GrayShortArray(resized);


        //Conduct Hough transform for line detection
        LineHough lineHough = new LineHough();
        int[][] accumulator = lineHough.process(srcGrayShortArray, detectingRegionX1,
                detectingRegionY1, detectingRegionX2, detectingRegionY2);


        //Find a single central line
        int resultArray[] = lineHough.findMaxima(accumulator);

        //Do not conduct alignment if no distinct central line is found
        if (resultArray[2] < 10) {
            return null;
        }


        //Calculate line parameter
        int r = resultArray[0];
        int theta = resultArray[1];
        int x1 = 0;
        int y1 = lineHough.getY_from_XrThetaLine(x1, r, theta);
        int x2 = resized.getWidth();
        int y2 = lineHough.getY_from_XrThetaLine(x2, r, theta);


        //Find center point of a plate image
        Point2D centerPoint =
                lineMath.find_CenterLocation(
                new Point2D.Double(x1, y1),
                new Point2D.Double(x2, y2),
                new Point2D.Double(detectingRegionX1, detectingRegionY1),
                new Point2D.Double(detectingRegionX2, detectingRegionY2),
                3,
                srcGrayShortArray);

        //Do not conduct alignment if no center point is found
        if (centerPoint == null) {
            return null;
        }

        //Calculate angle
        double angleInRad = lineMath.get_AngleInRadian_Of_TwoPoints(
                new Point2D.Double(x1, y1),
                new Point2D.Double(x2, y2));


        //Do not delete for debugging
        //Draw central line and center point to resized image
        /*
         Graphics resizedGraphics = resized.getGraphics();
         resizedGraphics.setColor(Color.red);
         resizedGraphics.drawLine(x1,y1,x2,y2);
         resizedGraphics.setColor(Color.red);
         resizedGraphics.fillOval((int) (centerPoint.getX()-4),
         (int) (centerPoint.getY()-4), 
         8, 8);
         imgProc.saveImage(resized, "jpeg", "C:/assembled_centerline.jpeg");

         resized = imgProc.rotateAndShiftImage(resized, 
         (int) centerPoint.getX(),
         (int) centerPoint.getY(), 
         angleInRad/Math.PI*180-90,
         false, Color.black);
         
         imgProc.saveImage(resized, "jpeg", "C:/assembled_aligned.jpeg");
        */ 


        resultIntArray[0] = centerPoint.getX() * 100 / resizingPercent;
        resultIntArray[1] = centerPoint.getY() * 100 / resizingPercent;
        resultIntArray[2] = angleInRad / Math.PI * 180 - 90;

        return resultIntArray;
    }

    //Do local background subtraction
    //This is equivalent of 'assembleImages_By_BackSubtract'
    public ByteProcessor do_LocalBackgroundSubtraction(BufferedImage srcBufferedImage,
            int pieceWidth, int pieceHeight) {
        int srcImageWidth = srcBufferedImage.getWidth();
        int srcImageHeight = srcBufferedImage.getHeight();


        BufferedImage srcImage = new BufferedImage(srcImageWidth,
                srcImageHeight, srcBufferedImage.getType());
        Graphics2D g = srcImage.createGraphics();


        for (int y = 0; y < srcImageHeight; y = y + pieceHeight) {
            for (int x = 0; x < srcImageWidth; x = x + pieceWidth) {
                BufferedImage subImage = imgProc.cropImage(srcBufferedImage, x, y,
                        pieceWidth, pieceHeight);

                ImagePlus imp = new ImagePlus(null, subImage);
                ImageConverter imgcvt = new ImageConverter(imp);
                imgcvt.convertToGray8();

                ByteProcessor bp = (ByteProcessor) imp.getProcessor();
                BackgroundSubtracter bgSub = new BackgroundSubtracter();

                bgSub.subtractBackround(bp, 50);


                g.drawImage(imp.getBufferedImage(), x, y, pieceWidth, pieceHeight,
                        null);
            }


        }

        ImagePlus outImgPlus = new ImagePlus();
        outImgPlus.setImage(srcImage);

        return (ByteProcessor) outImgPlus.getProcessor();
    }


    //binarize
	public ByteProcessor binarize(ByteProcessor image){		
		//get histogram
		int[] histogram=image.getHistogram();
		
		//find most common pixel value (other than 255, 255 is always the most abundent);
		int max=0; 
		int pmax=0;
		for (int i=0; i<histogram.length-1; i++){
			if (histogram[i]<=max) {
				continue;
			}
			max=histogram[i];
			pmax=i;
		}
		
		int threshold;
      if (pmax == 254) {
          threshold = 220; //----Hardwired threshold 
      } else {
          threshold = 200;
      }
		
		//binarize 
		image.threshold(threshold); 
			
		//optimize
		image.dilate(); 
		image.erode(); 
		
		return image;
	}
	
	public ByteProcessor matchMask(ByteProcessor plate){
		FloodFiller ff=new FloodFiller(plate);
		
		for (int y=0; y<plate.getHeight(); y++) {
			for (int x=0; x<plate.getWidth(); x++){
            if (plate.getPixelValue(x, y) != 0) {
                continue; //skip white pixels
            }
				if (x>=componentLabel.length||y>=componentLabel[0].length||componentLabel[x][y]==0){
					plate.setValue(255);
					ff.fill8(x,y);
				}
			}
		}
		return plate;
	}
	    
    /**
     * writes an image
     */
	public void outputImage(ImageProcessor ip, String filename){
		ImagePlus imgPls=new ImagePlus(null, ip);
		FileSaver fs=new FileSaver(imgPls);
      if (filename.indexOf(".jpeg") < 0 && filename.indexOf(".JPEG") < 0) {
			filename=filename+".jpeg";
		}
		fs.saveAsJpeg(filename);
	}
	
}
