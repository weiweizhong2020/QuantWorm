/*
 * Filename: EggCounter.java
 * This class contains main modules for egg counting
 */

package edu.rice.wormlab.eggcounter;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
        
//Initializing parameters for screening condition
class ScreeningConditionDef {

    int Threshold_High = 0;
    int Threshold_Low = 0;
    int Threshold_StepSize = 0;
    int AvgGrayOfEgg = 0;
    int AvgPixelCount = 0;
    int Default_AvgGrayOfEgg = 0;
    int Default_AvgPixelCount = 0;
    int Default_CutOffProbability = 0;
}

//Initializing variables of detected objects (eggs)
class ID_InfoDef {

    public double PixelCount = 0;
    public double AvgCenter_X = 0;
    public double AvgCenter_Y = 0;
    public double X_Min = 0;
    public double X_Max = 0;
    public double Y_Min = 0;
    public double Y_Max = 0;
    public double AvgRadius = 0;
    public double SqrtDiffRadius_CannyEdge = 0;
    public double SqrtDiffRadius_FilledRegion = 0;
    public boolean IsExcludeThisID = false;
    public double AvgGrayOfEggBox = 0;
    public double CannyEdgePointCount = 0;
    public double AvgGrayOfMaskedObject = 0;
    public double CubicLenth = 0;
    public int DuplicateCount = 0;
    public int EstimatedEggCount = 0;
    public String Method = "";
    public double ProbablityPercent = 0;
}

public class EggCounter implements Runnable {

    ID_InfoDef ID_Info[] = new ID_InfoDef[1];
    ID_InfoDef ID_Info_Accumu[] = new ID_InfoDef[100000];
    public int ID_Info_Accumu_Count = 0;
    ID_InfoDef ID_Info_Accumu_Ref[];
    public int ID_Info_Accumu_Ref_Count = 0;
    ID_InfoDef DetectedEggInfo[] = new ID_InfoDef[1001];
    public int DetectedEggInfoCount;
    public int DetectedEggCount;
    ScreeningConditionDef myScreeningCondition = new ScreeningConditionDef();
    public int EggCount;
    public int ReferenceEggCount;
    public final static String Method_AutomatedCounting = "Automated counting";
    public final static String Method_ManualInspection = "Human-inspection";
    public final static String softwareTitleVersion = "Progress Widnow";
    public short GrayImageMap[][];
    public short BlackWhiteMap[][];
    public short CannyEedgeMap[][];
    public short GapFloodFilledCannyEdgeMap[][];
    public int IntegralGrayImageMap[][];
    public short FoundRegionColorTable[][];
    public short LabelIDMap[][];
    public int LabelID_Count;
    public short LabelID_Info[][];
    public int ImageWidth;
    public int ImageHeight;
    public int ImageWidthUpperBound;
    public int ImageHeightUpperBound;
    public BufferedImage _Image_Source;
    public BufferedImage _Image_NegativeGray;
    public BufferedImage _Image_BlackWhite;
    public BufferedImage _Image_CannyEdgeDetection;
    public BufferedImage _Image_GapFloodFilledCannyEdgeDetection;
    public BufferedImage _Image_FinalOverLapOfCannyEdgeDetection;
    public BufferedImage _Image_FinalOverLapWithoutLabels;
    public BufferedImage _Image_FinalOverLap;
    public BufferedImage _Image_Debug;
    //Temporary image
    public BufferedImage _Image_RegionExtraction;
    public BufferedImage _Image_RegionExtractionOverlap;
    public BufferedImage _Image_EggDetection;
    private NativeImgProcessing ImgProc = new NativeImgProcessing();
    private CannyEdgeDetector CannyDet = new CannyEdgeDetector();
    public int numberOfPiecesInX;
    public int numberOfPiecesInY;
    public int[] imageScanOrder;
    public static final String EggCountResultFileName = "result-egg.txt";
    public String currentFolder = "";
    public boolean _UserStopped_AllPieceAnalysis = false;
    public boolean _UserStopped_BatchAnalysis = false;
    public boolean _IsRunUnderCommandLineArgs = false;
    public String _CommandLineArg_FullPath;
    public int subFolderCount = -1;
    //UI for Image Viewer
    public JFrame outframe = new JFrame("Current image");
    public JImagePanel imgPanel;
    //UI for Batch window
    public JFrame frameBatch = new JFrame(softwareTitleVersion);
    public JProgressBar progbarCurImage = new JProgressBar(0, 100);
    public JProgressBar progbarOverall = new JProgressBar(0, 100);
    public JList listFolders = new JList();
    public DefaultListModel listmodelFolders = new DefaultListModel();
    public JLabel labelCurImageFileName = new JLabel("filename.jpeg");
    public JLabel labelOverallProg = new JLabel("Overall progress ");
    public JLabel labelCurImage = new JLabel("Current folder");
    public JButton buttonStop = new JButton("Stop");
    public JButton buttonClose = new JButton("Close");
    public JScrollPane scrollFolders;
    public JButton buttonImageVisible = new JButton("View image");
    
    public static DetectionCondition detectionCondition = new DetectionCondition();
    
    
    public void run() {
               int i, foldersMaxIndex, ReturnValue;
        foldersMaxIndex = listmodelFolders.size() - 1;

        progbarOverall.setMaximum(foldersMaxIndex + 1);
        progbarOverall.setValue(0);


        for (i = 0; i <= foldersMaxIndex; i++) {
            listFolders.setSelectedIndex(i);
            
            frameBatch.setVisible(true);

        
            currentFolder = listmodelFolders.elementAt(i).toString();
            ReturnValue = analyze_AllPieces(currentFolder, "piece_");
            if (ReturnValue == -1) {
                break;
            }

            progbarOverall.setValue(i + 1);
        }


        buttonStop.setEnabled(false);
        buttonClose.setEnabled(true);

    }
    
    //Set up UI
    public void setUI() {

        //Setting Image Viewer Window
        outframe.setBounds(150, 150, 660, 530);
        outframe.setLocation(300, 180);
        outframe.setMinimumSize(new Dimension(660,530));
        imgPanel = new JImagePanel(null, 0, 0);
        outframe.add(imgPanel);
        outframe.setVisible(false);


        //Setting Batch Window
        frameBatch.setLayout(null);
        frameBatch.setBounds(110, 170, 500, 345);
        frameBatch.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        frameBatch.setResizable(false);

        progbarOverall.setLocation(123, 11);
        progbarCurImage.setLocation(123, 45);
        progbarOverall.setSize(346, 22);
        progbarCurImage.setSize(205, 23);
        frameBatch.add(progbarOverall);
        frameBatch.add(progbarCurImage);

        labelOverallProg.setLocation(15, 16);
        labelCurImage.setLocation(15, 49);
        labelCurImageFileName.setLocation(334, 49);
        labelOverallProg.setSize(100, 13);
        labelCurImage.setSize(100, 13);
        labelCurImageFileName.setSize(220, 13);
        frameBatch.add(labelCurImageFileName);
        frameBatch.add(labelOverallProg);
        frameBatch.add(labelCurImage);

        listFolders = new JList(listmodelFolders);
        listFolders.setSelectedIndex(0);
        listFolders.setEnabled(false);

        scrollFolders = new JScrollPane(listFolders);
        scrollFolders.setSize(467, 160);
        scrollFolders.setLocation(12, 90);
        frameBatch.add(scrollFolders);


        buttonStop.setSize(89, 34);
        buttonClose.setSize(89, 34);

        buttonStop.setLocation(100, 271);
        buttonClose.setLocation(235, 271);


        buttonStop.setEnabled(true);
        buttonClose.setEnabled(false);

        frameBatch.add(buttonStop);
        frameBatch.add(buttonClose);




        buttonStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _UserStopped_BatchAnalysis = true;
                _UserStopped_AllPieceAnalysis = true;
                buttonStop.setEnabled(false);
                buttonClose.setEnabled(true);
            }
        });


        buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	frameBatch.setVisible( false );
                outframe.setVisible(false);
            }
        });



        buttonImageVisible.setLocation(360, 271);
        buttonImageVisible.setSize(100, 34);
        buttonImageVisible.setVisible(true);
        buttonImageVisible.setSelected(false);
        frameBatch.add(buttonImageVisible);

        buttonImageVisible.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (outframe.isShowing() == false) {
                    outframe.setVisible(true);
                }
            }
        });

        
        frameBatch.add(new JLabel(""));
        
        frameBatch.setVisible(true);
       
    }


    //add a new folder to the listbox   
    public void addFolders(String fullPathFolder) {
        listmodelFolders.addElement(fullPathFolder);
    }

    //add a list of folders to the listbox
    public void addFolders(String fullPathFolders[]) {
        for (String singleFile : fullPathFolders) {
            listmodelFolders.addElement(singleFile);
        }
    }

    //Display current image
    public void displayImage(JFrame frame, BufferedImage srcImage) {
        imgPanel.image = srcImage;
        imgPanel.updateUI();
    }

    //Write image file
    public void saveImage(BufferedImage img, String fullPathFileName) {
        try {
            String format = (fullPathFileName.endsWith(".png")) ? "png" : "jpg";
            ImageIO.write(img, format, new File(fullPathFileName));
        } catch (IOException e) {
            //do nothing
        }

    }

    public class JImagePanel extends JPanel {

        // serial version ID
		private static final long serialVersionUID = -1378923884725093934L;
		
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

    public static void main(String[] args) {
        EggCounter ia = new EggCounter();
        DirectoryReader dr = new DirectoryReader();


        if (args.length > 0) {
            ia.initialize();
            String allSubFolders[] = dr.scanAllSubFolders(args[0]);
            ia.addFolders(allSubFolders);

            ia.run();
        }
    }

    public void initialize() {
        initialize_ScreeningCondition();
        setUI();
    }

    public void initialize_ScreeningCondition() {
        myScreeningCondition.AvgGrayOfEgg = 70;
        myScreeningCondition.AvgPixelCount = 100;
        myScreeningCondition.Threshold_High =
                myScreeningCondition.AvgGrayOfEgg + 40;
        myScreeningCondition.Threshold_Low =
                myScreeningCondition.AvgGrayOfEgg - 30;
        myScreeningCondition.Threshold_StepSize = 10;

        myScreeningCondition.Default_AvgGrayOfEgg = 70;
        myScreeningCondition.Default_AvgPixelCount = 100;
        myScreeningCondition.Default_CutOffProbability = 100;
    }

    public void initialize_LabelID_Info() {
        for (int w = 0; w < ID_Info.length; w++) {
            ID_Info[w] = new ID_InfoDef();
            ID_Info[w].PixelCount = 0;
            ID_Info[w].AvgCenter_X = 0;
            ID_Info[w].AvgCenter_Y = 0;
            ID_Info[w].X_Min = 10000;
            ID_Info[w].X_Max = -1;
            ID_Info[w].Y_Min = 10000;
            ID_Info[w].Y_Max = -1;
            ID_Info[w].AvgRadius = 0;
            ID_Info[w].SqrtDiffRadius_CannyEdge = 0;
            ID_Info[w].SqrtDiffRadius_FilledRegion = 0;
            ID_Info[w].AvgGrayOfEggBox = 0;
            ID_Info[w].CannyEdgePointCount = 0;
            ID_Info[w].CubicLenth = 0;
            ID_Info[w].DuplicateCount = 0;
            ID_Info[w].AvgGrayOfMaskedObject = 0;
            ID_Info[w].IsExcludeThisID = false;
            ID_Info[w].EstimatedEggCount = 0;
            ID_Info[w].Method = Method_AutomatedCounting;
            ID_Info[w].ProbablityPercent = 0;
        }

    }

    //Estimate egg count of detected object
    private void estimate_NumberOfEggs() {

        for (int q = 1; q <= ID_Info_Accumu_Count; q++) {
            if (ID_Info_Accumu[q].IsExcludeThisID == false) {

                if ((int) Math.rint(ID_Info_Accumu[q].PixelCount / 
                        myScreeningCondition.AvgPixelCount * 0.8) >= 1) {
                    ID_Info_Accumu[q].EstimatedEggCount = 
                            (int) Math.rint(ID_Info_Accumu[q].PixelCount
                            / myScreeningCondition.AvgPixelCount * 0.8);
                } else if ((int) Math.rint(ID_Info_Accumu[q].PixelCount / 
                        myScreeningCondition.AvgPixelCount + 0.3) >= 1) {
                    ID_Info_Accumu[q].EstimatedEggCount = 1;
                    ID_Info_Accumu[q].ProbablityPercent = 
                            ID_Info_Accumu[q].ProbablityPercent * 0.5;
                }

            }

        }

    }

    
    //Return Number of detected objects
    private int determine_ScreeningCondition() {
        
        float[] ValidIDArray;
        int ValidIDArrayCount;

        ValidIDArrayCount = ReferenceEggCount;

        switch (ValidIDArrayCount) {
            case 0:
                myScreeningCondition.AvgGrayOfEgg = myScreeningCondition.Default_AvgGrayOfEgg;
                myScreeningCondition.AvgPixelCount = myScreeningCondition.Default_AvgPixelCount;
                myScreeningCondition.Threshold_High = myScreeningCondition.AvgGrayOfEgg + 70;
                myScreeningCondition.Threshold_Low = myScreeningCondition.AvgGrayOfEgg;
                return ValidIDArrayCount;

            case 1:
                myScreeningCondition.AvgGrayOfEgg = 
                        (int) Math.rint((ID_Info_Accumu_Ref[1].AvgGrayOfMaskedObject
                        + myScreeningCondition.Default_AvgGrayOfEgg) / 2);
                myScreeningCondition.AvgPixelCount = 
                        (int) Math.rint((ID_Info_Accumu_Ref[1].PixelCount
                        + myScreeningCondition.Default_AvgPixelCount) / 2);
                myScreeningCondition.Threshold_High = myScreeningCondition.AvgGrayOfEgg + 70;
                myScreeningCondition.Threshold_Low = myScreeningCondition.AvgGrayOfEgg;
                return ValidIDArrayCount;

            case 2:
                myScreeningCondition.AvgGrayOfEgg = 
                        (int) Math.rint((ID_Info_Accumu_Ref[1].AvgGrayOfMaskedObject
                        + ID_Info_Accumu_Ref[2].AvgGrayOfMaskedObject
                        + myScreeningCondition.Default_AvgGrayOfEgg) / 3);

                myScreeningCondition.AvgPixelCount =
                        (int) Math.rint((ID_Info_Accumu_Ref[1].PixelCount
                        + ID_Info_Accumu_Ref[2].PixelCount
                        + myScreeningCondition.Default_AvgPixelCount) / 3);
                myScreeningCondition.Threshold_High = myScreeningCondition.AvgGrayOfEgg + 70;
                myScreeningCondition.Threshold_Low = myScreeningCondition.AvgGrayOfEgg;
                return ValidIDArrayCount;

            default:

                break;
        }


        //Calculating median value of AvgGrayOfMaskedObject
        ValidIDArray = new float[ValidIDArrayCount];
        ValidIDArrayCount = 0;
        for (int q = 1; q <= ID_Info_Accumu_Ref_Count; q++) {
            if (ID_Info_Accumu_Ref[q].IsExcludeThisID == false) {
                ValidIDArray[ValidIDArrayCount] = 
                        (float) (ID_Info_Accumu_Ref[q].AvgGrayOfMaskedObject);
                ValidIDArrayCount += 1;
            }
        }
        myScreeningCondition.AvgGrayOfEgg = (int) Math.rint(calculate_Median(ValidIDArray));



        //Calculating median value of PixelCount
        ValidIDArrayCount = 0;
        for (int q = 1; q <= ID_Info_Accumu_Ref_Count; q++) {
            if (ID_Info_Accumu_Ref[q].IsExcludeThisID == false) {
                ValidIDArray[ValidIDArrayCount] = (float) (ID_Info_Accumu_Ref[q].PixelCount);
                ValidIDArrayCount += 1;
            }
        }
        myScreeningCondition.AvgPixelCount = (int) Math.rint(calculate_Median(ValidIDArray));
        myScreeningCondition.Threshold_High = myScreeningCondition.AvgGrayOfEgg + 70;
        myScreeningCondition.Threshold_Low = myScreeningCondition.AvgGrayOfEgg;

        return ValidIDArrayCount;
    }

    //Calculate median value
    public static double calculate_Median(float[] SrcNumericArray) {
        float[] NumericArray = SrcNumericArray.clone();
        Arrays.sort(NumericArray);

        int middle = NumericArray.length / 2;

        if (NumericArray.length % 2 == 1) {
            return NumericArray[middle];
        } else {
            return (NumericArray[middle - 1] + NumericArray[middle]) / 2.0;
        }
    }

    public final void check_AvgGrayProfile() {
        float[] AvgGrayProfile;
        float HighestAvgGray;
        float LowestAvgGray;


        for (int q = 1; q <= ID_Info_Accumu_Count; q++) {
            if (ID_Info_Accumu[q].IsExcludeThisID == false) {

                AvgGrayProfile = calculate_AvgGrayProfile(GrayImageMap,
                        (int) ID_Info_Accumu[q].X_Min,
                        (int) ID_Info_Accumu[q].Y_Min,
                        (int) ID_Info_Accumu[q].X_Max,
                        (int) ID_Info_Accumu[q].Y_Max);

                HighestAvgGray = (AvgGrayProfile[0]
                        + AvgGrayProfile[AvgGrayProfile.length - 1]) / 2;

                LowestAvgGray = min(AvgGrayProfile);

                if ((HighestAvgGray - LowestAvgGray) / HighestAvgGray < 0.2 * (255 - LowestAvgGray) / 118) {
                    ID_Info_Accumu[q].IsExcludeThisID = true;
                }
            }
        }
    }

    public static float min(float[] array) {
        // Validates input
        if (array == null) {
            throw new IllegalArgumentException("The Array must not be null");
        } else if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }

        // Finds and returns min
        float min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }

        return min;
    }

    public final float[] calculate_AvgGrayProfile(
            short[][] GrayMap, int X1, int Y1, int X2, int Y2) {

        X1 = Math.max(0, X1 - 4);
        Y1 = Math.max(0, Y1 - 4);
        X2 = Math.min(GrayMap.length - 1, X2 + 4);
        Y2 = Math.min(GrayMap[0].length - 1, Y2 + 4);


        float[] AvgLineProfile = new float[X2 - X1 + 1];
        int x;
        int y;
        float GraySum;


        for (x = X1; x <= X2; x++) {
            GraySum = 0F;

            for (y = Y1; y <= Y2; y++) {
                GraySum += GrayMap[x][y];
            }

            AvgLineProfile[x - X1] = GraySum / (X2 - X1 + 1);
        }


        return AvgLineProfile;
    }

    public final void check_CannyEdgePoints() {
        int q;

        for (q = 1; q <= ID_Info_Accumu_Count; q++) {

            if (ID_Info_Accumu[q].IsExcludeThisID == false) {

                if (ID_Info_Accumu[q].CannyEdgePointCount <= 5) {
                    ID_Info_Accumu[q].ProbablityPercent = 
                            ID_Info_Accumu[q].ProbablityPercent * 0.6;
                    if (ID_Info_Accumu[q].ProbablityPercent < 
                            myScreeningCondition.Default_CutOffProbability) {
                        ID_Info_Accumu[q].IsExcludeThisID = true;
                    }
                } else if (ID_Info_Accumu[q].CannyEdgePointCount <= 10) {
                    ID_Info_Accumu[q].ProbablityPercent = 
                            ID_Info_Accumu[q].ProbablityPercent * 0.8;
                    if (ID_Info_Accumu[q].ProbablityPercent <
                            myScreeningCondition.Default_CutOffProbability) {
                        ID_Info_Accumu[q].IsExcludeThisID = true;
                    }
                }

            }
        }


    }

    public final void check_Circularity_CannyEdge() {
        int q;


        for (q = 1; q <= ID_Info_Accumu_Count; q++) {


            if (ID_Info_Accumu[q].IsExcludeThisID == false) {


                if (ID_Info_Accumu[q].EstimatedEggCount == 1) {
                    if (ID_Info_Accumu[q].SqrtDiffRadius_CannyEdge >= 20) {
                        ID_Info_Accumu[q].ProbablityPercent = 0;
                        ID_Info_Accumu[q].IsExcludeThisID = true;

                    } else if (ID_Info_Accumu[q].SqrtDiffRadius_CannyEdge >= 10) {
                        ID_Info_Accumu[q].ProbablityPercent =
                                ID_Info_Accumu[q].ProbablityPercent * 0.3;
                    } else if (ID_Info_Accumu[q].SqrtDiffRadius_CannyEdge >= 5) {
                        ID_Info_Accumu[q].ProbablityPercent =
                                ID_Info_Accumu[q].ProbablityPercent * 0.4;
                    }


                } else if (ID_Info_Accumu[q].EstimatedEggCount == 2) {
                    if (ID_Info_Accumu[q].SqrtDiffRadius_CannyEdge >= 13) {
                        ID_Info_Accumu[q].ProbablityPercent =
                                ID_Info_Accumu[q].ProbablityPercent * 0.4;
                    } else if (ID_Info_Accumu[q].SqrtDiffRadius_CannyEdge >= 8) {
                        ID_Info_Accumu[q].ProbablityPercent = 
                                ID_Info_Accumu[q].ProbablityPercent * 0.5;
                    }

                } else if (ID_Info_Accumu[q].EstimatedEggCount == 3) {
                    if (ID_Info_Accumu[q].SqrtDiffRadius_CannyEdge >= 15) {
                        ID_Info_Accumu[q].ProbablityPercent = 
                                ID_Info_Accumu[q].ProbablityPercent * 0.4;
                    }
                } else if (ID_Info_Accumu[q].EstimatedEggCount > 4) {
                    if (ID_Info_Accumu[q].SqrtDiffRadius_CannyEdge >= 50) {
                        ID_Info_Accumu[q].ProbablityPercent = 
                                ID_Info_Accumu[q].ProbablityPercent * 0.7;
                    }
                }

                if (ID_Info_Accumu[q].ProbablityPercent < 
                        myScreeningCondition.Default_CutOffProbability) {
                    ID_Info_Accumu[q].IsExcludeThisID = true;
                }
            }
        }



    }

    public void check_Circularity_FilledRegion() {
        for (int q = 1; q <= ID_Info_Accumu_Count; q++) {

            if (ID_Info_Accumu[q].IsExcludeThisID == false) {

                if (ID_Info_Accumu[q].EstimatedEggCount == 1) {
                    if (ID_Info_Accumu[q].SqrtDiffRadius_FilledRegion >= 2) {
                        ID_Info_Accumu[q].ProbablityPercent =
                                ID_Info_Accumu[q].ProbablityPercent * 0.7;
                    }
                } else if (ID_Info_Accumu[q].EstimatedEggCount == 2) {
                    if (ID_Info_Accumu[q].SqrtDiffRadius_FilledRegion >= 10) {
                        ID_Info_Accumu[q].ProbablityPercent =
                                ID_Info_Accumu[q].ProbablityPercent * 0.7;
                    }
                } else if (ID_Info_Accumu[q].EstimatedEggCount == 3) {
                    if (ID_Info_Accumu[q].SqrtDiffRadius_FilledRegion >= 20) {
                        ID_Info_Accumu[q].ProbablityPercent =
                                ID_Info_Accumu[q].ProbablityPercent * 0.7;
                    }
                } else if (ID_Info_Accumu[q].EstimatedEggCount > 4) {
                    if (ID_Info_Accumu[q].SqrtDiffRadius_FilledRegion >= 20) {
                        ID_Info_Accumu[q].ProbablityPercent = 
                                ID_Info_Accumu[q].ProbablityPercent * 0.7;
                    }
                }

                if (ID_Info_Accumu[q].ProbablityPercent <
                        myScreeningCondition.Default_CutOffProbability) {
                    ID_Info_Accumu[q].IsExcludeThisID = true;
                }
            }
        }


    }

    public final short[][] check_LabelID_From_CannyEdge_RegionExt(
            short[][] Src_LabelIDMap, short[][] Src_ColorTable) {

        short[][] Temp_New_IDMap;
        int x;
        int y;


        Temp_New_IDMap = ImgProc.multiArrayCopy(Src_LabelIDMap);
        LabelID_Count = (short) Src_ColorTable.length - 1;

        ID_InfoDef[] temp = new ID_InfoDef[LabelID_Count + 1];
        ID_Info = temp.clone();

        initialize_LabelID_Info();

        //Adding .AvgCenter_X (or _Y) of each pixel
        //Finding Min,Max
        for (y = 0; y <= ImageHeightUpperBound; y++) {
            for (x = 0; x <= ImageWidthUpperBound; x++) {
                ID_Info[Src_LabelIDMap[x][y]].PixelCount += 1;
                ID_Info[Src_LabelIDMap[x][y]].AvgCenter_X += x;
                ID_Info[Src_LabelIDMap[x][y]].AvgCenter_Y += y;

                if (ID_Info[Src_LabelIDMap[x][y]].X_Min > x) {
                    ID_Info[Src_LabelIDMap[x][y]].X_Min = x;
                }
                if (ID_Info[Src_LabelIDMap[x][y]].Y_Min > y) {
                    ID_Info[Src_LabelIDMap[x][y]].Y_Min = y;
                }
                if (ID_Info[Src_LabelIDMap[x][y]].X_Max < x) {
                    ID_Info[Src_LabelIDMap[x][y]].X_Max = x;
                }
                if (ID_Info[Src_LabelIDMap[x][y]].Y_Max < y) {
                    ID_Info[Src_LabelIDMap[x][y]].Y_Max = y;
                }
            }
        }


        //Calculating average value
        for (y = 1; y <= LabelID_Count; y++) {
            if (ID_Info[y].PixelCount > 0) {
                ID_Info[y].AvgCenter_X = (int) Math.rint(ID_Info[y].AvgCenter_X /
                        ID_Info[y].PixelCount);
                ID_Info[y].AvgCenter_Y = (int) Math.rint(ID_Info[y].AvgCenter_Y /
                        ID_Info[y].PixelCount);
            }
        }



        //Adding .avgRadius of each pixel
        for (y = 0; y <= ImageHeightUpperBound; y++) {
            for (x = 0; x <= ImageWidthUpperBound; x++) {
                ID_Info[Src_LabelIDMap[x][y]].AvgRadius += Math.sqrt((Math.pow(
                        (x - ID_Info[Src_LabelIDMap[x][y]].AvgCenter_X), 2))
                        + (Math.pow((y - ID_Info[Src_LabelIDMap[x][y]].AvgCenter_Y), 2)));
            }
        }


        //Calculating avgRadius
        for (y = 1; y <= LabelID_Count; y++) {
            ID_Info[y].AvgRadius = ID_Info[y].AvgRadius / ID_Info[y].PixelCount;
        }




        for (y = 1; y <= LabelID_Count; y++) {

            if (ID_Info[y].PixelCount > detectionCondition.min_ObjectSize_In_CannyEdgeRegion
                    && ID_Info[y].PixelCount < detectionCondition.max_ObjectSize_In_CannyEdgeRegion
                    && (ID_Info[y].X_Max - ID_Info[y].X_Min) > detectionCondition.min_BoundingSize_In_CannyEdgeRegion
                    && (ID_Info[y].Y_Max - ID_Info[y].Y_Min) > detectionCondition.min_BoundingSize_In_CannyEdgeRegion
                    && (ID_Info[y].X_Max - ID_Info[y].X_Min) < detectionCondition.max_BoundingSize_In_CannyEdgeRegion
                    && (ID_Info[y].Y_Max - ID_Info[y].Y_Min) < detectionCondition.max_BoundingSize_In_CannyEdgeRegion) {

                ID_Info[y].IsExcludeThisID = false;
            } else {
                ID_Info[y].IsExcludeThisID = true;
            }
        }



        for (int q = 1; q <= LabelID_Count; q++) {

            if (ID_Info[q].IsExcludeThisID == false) {
                long GrayValueSum;
                long MaskedPixelCount;
                int Cur_X;
                int Cur_Y;

                GrayValueSum = 0;
                MaskedPixelCount = 0;
                for (Cur_Y = (int) Math.rint(ID_Info[q].Y_Min); 
                        Cur_Y <= (int) Math.rint(ID_Info[q].Y_Max); Cur_Y++) {
                    for (Cur_X = (int) Math.rint(ID_Info[q].X_Min); 
                            Cur_X <= (int) Math.rint(ID_Info[q].X_Max); Cur_X++) {
                        if (Src_LabelIDMap[Cur_X][ Cur_Y] == q) {
                            GrayValueSum += GrayImageMap[Cur_X][Cur_Y];
                            MaskedPixelCount += 1;
                        }
                    }
                }

                ID_Info[q].AvgGrayOfMaskedObject = (int) Math.rint(GrayValueSum / 
                        (double) MaskedPixelCount);
            }

        }



        //Calculating Circularity_CannyEdge
        //Calculating CannyEdgePointCount and SqrtDiffRadius_CannyEdge
        int CannyEdgePointCount;

        for (int w = 1; w <= LabelID_Count; w++) {

            if (ID_Info[w].IsExcludeThisID == false) {

                CannyEdgePointCount = 0;
                for (y = (int) Math.rint(ID_Info[w].Y_Min); 
                        y <= (int) Math.rint(ID_Info[w].Y_Max); y++) {
                    for (x = (int) Math.rint(ID_Info[w].X_Min);
                            x <= (int) Math.rint(ID_Info[w].X_Max); x++) {
                        if (CannyEedgeMap[x][ y] == 255) {
                            CannyEdgePointCount += 1;

                            ID_Info[w].SqrtDiffRadius_CannyEdge =
                                    ID_Info[w].SqrtDiffRadius_CannyEdge
                                    + (Math.pow((Math.sqrt((Math.pow((x - ID_Info[w].AvgCenter_X), 2))
                                    + (Math.pow((y - ID_Info[w].AvgCenter_Y), 2)))
                                    - ID_Info[w].AvgRadius), 2));
                        }
                    }
                }


                ID_Info[w].CannyEdgePointCount = CannyEdgePointCount;
                if (ID_Info[w].CannyEdgePointCount > 1) {
                    ID_Info[w].SqrtDiffRadius_CannyEdge = ID_Info[w].SqrtDiffRadius_CannyEdge
                            / CannyEdgePointCount;

                    if (ID_Info[w].SqrtDiffRadius_CannyEdge > detectionCondition.min_SqrtDiffRadius_In_CannyEdgeRegion) {
                        ID_Info[w].IsExcludeThisID = true;
                    }
                } else {
                    ID_Info[w].IsExcludeThisID = true;
                }
            }
        }



        //Calculating Circularity_FilledRegion
        //Adding SqrtDiffRadius
        for (y = 0; y <= ImageHeightUpperBound; y++) {
            for (x = 0; x <= ImageWidthUpperBound; x++) {
                if (ID_Info[Src_LabelIDMap[x][ y]].IsExcludeThisID == false &&
                        Src_LabelIDMap[x][ y] != 0) {

                    ID_Info[Src_LabelIDMap[x][ y]].SqrtDiffRadius_FilledRegion +=
                            (Math.pow((Math.sqrt((Math.pow((x - 
                            ID_Info[Src_LabelIDMap[x][ y]].AvgCenter_X), 2))
                            + (Math.pow((y - ID_Info[Src_LabelIDMap[x][ y]].AvgCenter_Y), 2)))
                            - ID_Info[Src_LabelIDMap[x][ y]].AvgRadius), 2));

                }
            }
        }



        //Calculating SqrtDiffRadius
        for (y = 1; y <= LabelID_Count; y++) {
            if (ID_Info[y].IsExcludeThisID == false) {
                ID_Info[y].SqrtDiffRadius_FilledRegion = ID_Info[y].SqrtDiffRadius_FilledRegion
                        / ID_Info[y].PixelCount;

                if (ID_Info[y].SqrtDiffRadius_FilledRegion > detectionCondition.min_SqrtDiffRadius_In_FilledRegion) {
                    ID_Info[y].IsExcludeThisID = true;
                }
            }
        }


        //Removing pixels on IDMap
        for (y = 0; y <= ImageHeightUpperBound; y++) {
            for (x = 0; x <= ImageWidthUpperBound; x++) {
                if (ID_Info[Src_LabelIDMap[x][y]].IsExcludeThisID) {
                    Temp_New_IDMap[x][ y] = 0;
                } else {
                    Temp_New_IDMap[x][ y] = Src_LabelIDMap[x][ y];
                }
            }
        }


        return Temp_New_IDMap;

    }

    //Process (update and delete) detected objects
    //utilizing region labeled binary image
    public final short[][] check_LabelID_From_Threshold_RegionExt(
            short[][] Src_IDMap, short[][] Src_ColorTable) {

        short[][] Temp_New_IDMap;

        int w;
        int x;
        int y;


        Temp_New_IDMap = ImgProc.multiArrayCopy(Src_IDMap);
        LabelID_Count = (short) Src_ColorTable.length - 1;

        ID_InfoDef[] temp = new ID_InfoDef[LabelID_Count + 1];
        ID_Info = temp.clone();


        //Initializing
        initialize_LabelID_Info();



        //Adding .AvgCenter_X (or _Y) of each pixel
        //Finding Min,Max
        for (y = 0; y <= ImageHeightUpperBound; y++) {
            for (x = 0; x <= ImageWidthUpperBound; x++) {

                ID_Info[Src_IDMap[x][y]].PixelCount += 1;
                ID_Info[Src_IDMap[x][y]].AvgCenter_X += x;
                ID_Info[Src_IDMap[x][y]].AvgCenter_Y += y;

                if (ID_Info[Src_IDMap[x][y]].X_Min > x) {
                    ID_Info[Src_IDMap[x][y]].X_Min = x;
                }
                if (ID_Info[Src_IDMap[x][y]].Y_Min > y) {
                    ID_Info[Src_IDMap[x][y]].Y_Min = y;
                }
                if (ID_Info[Src_IDMap[x][y]].X_Max < x) {
                    ID_Info[Src_IDMap[x][y]].X_Max = x;
                }
                if (ID_Info[Src_IDMap[x][y]].Y_Max < y) {
                    ID_Info[Src_IDMap[x][y]].Y_Max = y;
                }
            }
        }



        //Calculating average value
        for (y = 1; y <= LabelID_Count; y++) {

            if (ID_Info[y].PixelCount > 0) {
                ID_Info[y].AvgCenter_X = (int) Math.rint(ID_Info[y].AvgCenter_X / 
                        ID_Info[y].PixelCount);
                ID_Info[y].AvgCenter_Y = (int) Math.rint(ID_Info[y].AvgCenter_Y / 
                        ID_Info[y].PixelCount);
            }
        }


        //Adding .avgRadius of each pixel
        for (y = 0; y <= ImageHeightUpperBound; y++) {
            for (x = 0; x <= ImageWidthUpperBound; x++) {
                ID_Info[Src_IDMap[x][y]].AvgRadius +=
                        Math.sqrt((Math.pow((x - ID_Info[Src_IDMap[x][y]].AvgCenter_X), 2))
                        + (Math.pow((y - ID_Info[Src_IDMap[x][y]].AvgCenter_Y), 2)));
            }
        }



        //Calculating avgRadius
        for (y = 1; y <= LabelID_Count; y++) {
            ID_Info[y].AvgRadius = ID_Info[y].AvgRadius / ID_Info[y].PixelCount;
            ID_Info[y].CubicLenth = (int) Math.rint(Math.pow(ID_Info[y].PixelCount, 0.5));
        }



        for (y = 1; y <= LabelID_Count; y++) {
            if (ID_Info[y].AvgRadius < detectionCondition.min_AvgRadius_In_ThresholdRegion
                    && (ID_Info[y].X_Max - ID_Info[y].X_Min) >= detectionCondition.min_BoundingSize_In_ThresholdRegion
                    && (ID_Info[y].Y_Max - ID_Info[y].Y_Min) >= detectionCondition.min_BoundingSize_In_ThresholdRegion
                    && (ID_Info[y].X_Max - ID_Info[y].X_Min) < detectionCondition.max_BoundingSize_In_ThresholdRegion
                    && (ID_Info[y].Y_Max - ID_Info[y].Y_Min) < detectionCondition.max_BoundingSize_In_ThresholdRegion
                    && ID_Info[y].PixelCount <= detectionCondition.min_ObjectSize_In_ThresholdRegion) {

                ID_Info[y].IsExcludeThisID = false;
                ID_Info[y].ProbablityPercent = 100;
            } else {
                ID_Info[y].IsExcludeThisID = true;
                ID_Info[y].ProbablityPercent = 0;
            }




            if (ID_Info[y].IsExcludeThisID == false) {
                if (ID_Info[y].PixelCount < myScreeningCondition.AvgPixelCount * 0.6) {
                    if (ID_Info[y].PixelCount >= myScreeningCondition.AvgPixelCount * 0.3) {
                        ID_Info[y].ProbablityPercent = ID_Info[y].ProbablityPercent * 0.7;
                    } else {
                        ID_Info[y].ProbablityPercent = 0;
                        ID_Info[y].IsExcludeThisID = true;
                    }
                }
            }



            if (ID_Info[y].IsExcludeThisID == false) {
                long GrayValueSum;
                long MaskedPixelCount;
                int Cur_X;
                int Cur_Y;

                GrayValueSum = 0;
                MaskedPixelCount = 0;
                for (Cur_Y = (int) Math.rint(ID_Info[y].Y_Min); 
                        Cur_Y <= (int) Math.rint(ID_Info[y].Y_Max); Cur_Y++) {
                    for (Cur_X = (int) Math.rint(ID_Info[y].X_Min); 
                            Cur_X <= (int) Math.rint(ID_Info[y].X_Max); Cur_X++) {
                        if (Src_IDMap[Cur_X][ Cur_Y] == y) {
                            GrayValueSum += GrayImageMap[Cur_X][ Cur_Y];
                            MaskedPixelCount++;
                        }
                    }
                }

                if (MaskedPixelCount > 1) {

                    ID_Info[y].AvgGrayOfMaskedObject = (int) Math.rint(GrayValueSum /
                            (double) MaskedPixelCount);


                    if (ID_Info[y].AvgGrayOfMaskedObject <= myScreeningCondition.AvgGrayOfEgg - 30
                            | ID_Info[y].AvgGrayOfMaskedObject >= myScreeningCondition.AvgGrayOfEgg + 30) {

                        ID_Info[y].ProbablityPercent = ID_Info[y].ProbablityPercent * 0.5;

                    } else if (ID_Info[y].AvgGrayOfMaskedObject <= myScreeningCondition.AvgGrayOfEgg - 40
                            | ID_Info[y].AvgGrayOfMaskedObject >= myScreeningCondition.AvgGrayOfEgg + 40) {

                        ID_Info[y].ProbablityPercent = ID_Info[y].ProbablityPercent * 0.4;

                    }
                }
            }


        }


        //Calculating CannyEdgePointCount and SqrtDiffRadius_CannyEdge
        int CannyEdgePointCount;

        for (w = 1; w <= LabelID_Count; w++) {
            if (ID_Info[w].IsExcludeThisID == false) {

                CannyEdgePointCount = 0;
                for (y = (int) Math.rint(ID_Info[w].Y_Min); 
                        y <= (int) Math.rint(ID_Info[w].Y_Max); y++) {
                    for (x = (int) Math.rint(ID_Info[w].X_Min); 
                            x <= (int) Math.rint(ID_Info[w].X_Max); x++) {
                        if (CannyEedgeMap[x][y] == 255) {
                            CannyEdgePointCount++;

                            ID_Info[w].SqrtDiffRadius_CannyEdge +=
                                    (Math.pow((Math.sqrt((Math.pow((x - ID_Info[w].AvgCenter_X), 2))
                                    + (Math.pow((y - ID_Info[w].AvgCenter_Y), 2)))
                                    - ID_Info[w].AvgRadius), 2));
                        }
                    }
                }


                ID_Info[w].CannyEdgePointCount = CannyEdgePointCount;


                if (ID_Info[w].CannyEdgePointCount > 1) {
                    ID_Info[w].SqrtDiffRadius_CannyEdge = ID_Info[w].SqrtDiffRadius_CannyEdge
                            / CannyEdgePointCount;
                } else {
                    ID_Info[w].IsExcludeThisID = true;
                }
            }
        }



        //Calculating Circularity_FilledRegion
        //Adding SqrtDiffRadius
        for (y = 0; y <= ImageHeightUpperBound; y++) {
            for (x = 0; x <= ImageWidthUpperBound; x++) {
                if (ID_Info[Src_IDMap[x][ y]].IsExcludeThisID == false && Src_IDMap[x][ y] != 0) {

                    ID_Info[Src_IDMap[x][ y]].SqrtDiffRadius_FilledRegion +=
                            (Math.pow((Math.sqrt((Math.pow((x - 
                            ID_Info[Src_IDMap[x][ y]].AvgCenter_X), 2))
                            + (Math.pow((y - ID_Info[Src_IDMap[x][ y]].AvgCenter_Y), 2)))
                            - ID_Info[Src_IDMap[x][ y]].AvgRadius), 2));

                }
            }
        }
        //Calculating SqrtDiffRadius
        for (y = 1; y <= LabelID_Count; y++) {
            if (ID_Info[y].IsExcludeThisID == false) {
                ID_Info[y].SqrtDiffRadius_FilledRegion = ID_Info[y].SqrtDiffRadius_FilledRegion
                        / ID_Info[y].PixelCount;
            }
        }



        //Removing pixels on IDMap
        for (y = 0; y <= ImageHeightUpperBound; y++) {
            for (x = 0; x <= ImageWidthUpperBound; x++) {
                if (ID_Info[Src_IDMap[x][ y]].IsExcludeThisID) {
                    Temp_New_IDMap[x][ y] = 0;
                } else {
                    Temp_New_IDMap[x][ y] = Src_IDMap[x][y];
                }
            }
        }


        return Temp_New_IDMap;

    }

    //Count pixel count in the Canny edge image
    public int count_CannyEdgePoints(int X1, int Y1, int X2, int Y2) {
        int PointCount;
        int x;
        int y;

        PointCount = 0;
        for (y = Y1; y <= Y2; y++) {
            for (x = X1; x <= X2; x++) {
                if (CannyEedgeMap[x][y] == 255) {
                    PointCount += 1;
                }
            }
        }


        return PointCount;
    }

    //Reorder egg objects
    public final void reorder_IDInfo_Accumu() {
        int ValidIDCount = 0;

        for (int w = 1; w <= ID_Info_Accumu_Count; w++) {

            if (ID_Info_Accumu[w].IsExcludeThisID == false) {
                ValidIDCount += 1;

                ID_Info_Accumu[ValidIDCount] = ID_Info_Accumu[w];
            }
        }


        ID_Info_Accumu_Count = ValidIDCount;
    }

    
    //Remove duplicate findings
    public final void removeDuplicateLabelID_Of_IDInfo_Accumu() {

        for (int w = 1; w <= ID_Info_Accumu_Count; w++) {
            if (ID_Info_Accumu[w].IsExcludeThisID == false) {
                for (int s = 1; s < w; s++) {
                    if (ID_Info_Accumu[s].IsExcludeThisID == false) {
                        if (Math.abs(ID_Info_Accumu[s].X_Min - ID_Info_Accumu[w].X_Min) <= 
                                    detectionCondition.max_Distance_For_DuplicateFinding
                                && Math.abs(ID_Info_Accumu[s].Y_Min - ID_Info_Accumu[w].Y_Min) <= 
                                    detectionCondition.max_Distance_For_DuplicateFinding
                                && Math.abs(ID_Info_Accumu[s].X_Max - ID_Info_Accumu[w].X_Max) <=
                                    detectionCondition.max_Distance_For_DuplicateFinding
                                && Math.abs(ID_Info_Accumu[s].Y_Max - ID_Info_Accumu[w].Y_Max) <=
                                    detectionCondition.max_Distance_For_DuplicateFinding) {

                            if (ID_Info_Accumu[s].IsExcludeThisID == false
                                    && ID_Info_Accumu[w].IsExcludeThisID == false) {
                                removeDuplicateLabelID_Of_IDInfo_Accumu_UpdateDim(s, w);
                                ID_Info_Accumu[s].DuplicateCount++;
                                ID_Info_Accumu[w].IsExcludeThisID = true;

                                break;
                            }
                        }

                    }
                }
            }

        }


        for (int w = 1; w <= ID_Info_Accumu_Count; w++) {

            if (ID_Info_Accumu[w].IsExcludeThisID == false) {

                for (int s = 1; s < w; s++) {
                    if (ID_Info_Accumu[s].IsExcludeThisID == false) {
                        if (ID_Info_Accumu[s].X_Min >= ID_Info_Accumu[w].X_Min - 
                                detectionCondition.max_Distance_For_DuplicateFinding
                                && ID_Info_Accumu[s].Y_Min >= ID_Info_Accumu[w].Y_Min - 
                                detectionCondition.max_Distance_For_DuplicateFinding
                                && ID_Info_Accumu[w].X_Max + detectionCondition.max_Distance_For_DuplicateFinding >= 
                                ID_Info_Accumu[s].X_Max
                                && ID_Info_Accumu[w].Y_Max + detectionCondition.max_Distance_For_DuplicateFinding >=
                                ID_Info_Accumu[s].Y_Max) {

                            if (ID_Info_Accumu[s].IsExcludeThisID == false && 
                                    ID_Info_Accumu[w].IsExcludeThisID == false) {

                                if (ID_Info_Accumu[s].DuplicateCount > 
                                        ID_Info_Accumu[w].DuplicateCount) {
                                    ID_Info_Accumu[w].IsExcludeThisID = true;
                                    break;
                                } else {
                                    ID_Info_Accumu[w].IsExcludeThisID = true;
                                }

                            }

                        } else if (ID_Info_Accumu[s].X_Min < (ID_Info_Accumu[w].X_Min + 
                                    detectionCondition.max_Distance_For_DuplicateFinding)
                                && ID_Info_Accumu[s].Y_Min < (ID_Info_Accumu[w].Y_Min + 
                                    detectionCondition.max_Distance_For_DuplicateFinding)
                                && (ID_Info_Accumu[w].X_Max - detectionCondition.max_Distance_For_DuplicateFinding) < 
                                ID_Info_Accumu[s].X_Max
                                && (ID_Info_Accumu[w].Y_Max - detectionCondition.max_Distance_For_DuplicateFinding) < 
                                ID_Info_Accumu[s].Y_Max) {

                            if (ID_Info_Accumu[s].DuplicateCount > ID_Info_Accumu[w].DuplicateCount) {
                                ID_Info_Accumu[w].IsExcludeThisID = true;
                                break;
                            } else {
                                ID_Info_Accumu[s].IsExcludeThisID = true;
                            }

                        }

                    }

                }

            }

        }

    }

    public final void removeDuplicateLabelID_Of_IDInfo_Accumu_UpdateDim(int S, int W) {

        ID_Info_Accumu[S].X_Max = (ID_Info_Accumu[S].X_Max
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].X_Max)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].X_Min = (ID_Info_Accumu[S].X_Min
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].X_Min)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].Y_Max = (ID_Info_Accumu[S].Y_Max
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].Y_Max)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].Y_Min = (ID_Info_Accumu[S].Y_Min
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].Y_Min)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].AvgCenter_X = (ID_Info_Accumu[S].AvgCenter_X
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].AvgCenter_X)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].AvgCenter_Y = (ID_Info_Accumu[S].AvgCenter_Y
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].AvgCenter_Y)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].AvgGrayOfEggBox = (ID_Info_Accumu[S].AvgGrayOfEggBox
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].AvgGrayOfEggBox)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].AvgGrayOfMaskedObject = (ID_Info_Accumu[S].AvgGrayOfMaskedObject
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].AvgGrayOfMaskedObject)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].AvgRadius = (ID_Info_Accumu[S].AvgRadius
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].AvgRadius)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].CubicLenth = (ID_Info_Accumu[S].CubicLenth
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].CubicLenth)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].PixelCount = (ID_Info_Accumu[S].PixelCount
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].PixelCount)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].SqrtDiffRadius_CannyEdge = (ID_Info_Accumu[S].SqrtDiffRadius_CannyEdge
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].SqrtDiffRadius_CannyEdge)
                / (ID_Info_Accumu[S].DuplicateCount + 1);
        ID_Info_Accumu[S].SqrtDiffRadius_FilledRegion = (ID_Info_Accumu[S].SqrtDiffRadius_FilledRegion
                * ID_Info_Accumu[S].DuplicateCount
                + ID_Info_Accumu[W].SqrtDiffRadius_FilledRegion)
                / (ID_Info_Accumu[S].DuplicateCount + 1);

        if (((ID_Info_Accumu[S].DuplicateCount + 1) == 1)
                || ((ID_Info_Accumu[S].DuplicateCount + 1) == 2)) {
            ID_Info_Accumu[S].ProbablityPercent = (ID_Info_Accumu[S].ProbablityPercent
                    * ID_Info_Accumu[S].DuplicateCount
                    + ID_Info_Accumu[W].ProbablityPercent)
                    / (ID_Info_Accumu[S].DuplicateCount + 1);
        } else if ((ID_Info_Accumu[S].DuplicateCount + 1) >= 3) {
            ID_Info_Accumu[S].ProbablityPercent = 100;
        } else {
        }

    }

    
    //Return Egg count
    public final int search_ReferenceEggs(BufferedImage SourceGrayImage) {

        _Image_Source = ImgProc.deepCopy(SourceGrayImage);

        create_BaseImagesAndMaps();

        do_RegionExtraction_From_GapFloodFilledCannyEdgeDetection();

        _Image_FinalOverLap = ImgProc.deepCopy(_Image_FinalOverLapOfCannyEdgeDetection);

        determine_ScreeningCondition();
        return ReferenceEggCount;
    }

    
    //Region extraction
    public final void do_RegionExtraction_From_GapFloodFilledCannyEdgeDetection() {


        _Image_RegionExtraction =
                ImgProc.regionExtract_RasterScanning(_Image_GapFloodFilledCannyEdgeDetection, 0);
        LabelIDMap = ImgProc.RegionLabeling_LabelIDMap;
        FoundRegionColorTable = ImgProc.RegionLabeling_ColorTable;


        _Image_RegionExtractionOverlap = regionExtract_RasterScanning_Update_LabelIDMap_To_Image(
                _Image_Source, LabelIDMap, true, 1);


        LabelIDMap = check_LabelID_From_CannyEdge_RegionExt(LabelIDMap, FoundRegionColorTable);


        _Image_FinalOverLapOfCannyEdgeDetection = 
                
                regionExtract_RasterScanning_Update_LabelIDMap_To_Image(
                _Image_Source, LabelIDMap, true, 1);


        ID_InfoDef[] temp = new ID_InfoDef[ID_Info.length + 1];
        ID_Info_Accumu_Ref = temp.clone();

        ID_Info_Accumu_Ref_Count = 0;
        if (ID_Info.length > 1) {

            for (int w = 1; w <= ID_Info.length - 1; w++) {

                if (ID_Info[w].IsExcludeThisID == false) {

                    ID_Info_Accumu_Ref_Count += 1;
                    ID_Info_Accumu_Ref[ID_Info_Accumu_Ref_Count] = new ID_InfoDef();
                    ID_Info_Accumu_Ref[ID_Info_Accumu_Ref_Count] = ID_Info[w];

                }
            }

        }

        ReferenceEggCount = ID_Info_Accumu_Ref_Count;
    }

    
    public BufferedImage regionExtract_RasterScanning_Update_LabelIDMap_To_Image(
            BufferedImage SrcImage, short[][] LabelIDMap,
            boolean IsOverlap, int BkColor) {

        int BitmapHeightUpper;
        int BitmapWidthUpper;
        int x, y;
        short[][][] OutPixels;
        short[][] Src_GrayShortArray = ImgProc.convert_Image_To_GrayShortArray(SrcImage);


        BitmapWidthUpper = Src_GrayShortArray.length;
        BitmapHeightUpper = Src_GrayShortArray[0].length;
        OutPixels = new short[Src_GrayShortArray.length][Src_GrayShortArray[0].length][3];


        if (IsOverlap) {
            //Draw final image
            for (y = 0; y <= BitmapHeightUpper - 1; y++) {
                for (x = 0; x <= BitmapWidthUpper - 1; x++) {
                    if (LabelIDMap[x][y] != 0) {
                        OutPixels[x][y][0] = FoundRegionColorTable[LabelIDMap[x][y]][0];
                        OutPixels[x][y][1] = FoundRegionColorTable[LabelIDMap[x][y]][1];
                        OutPixels[x][y][2] = FoundRegionColorTable[LabelIDMap[x][y]][2];
                    } else {
                        OutPixels[x][y][0] = Src_GrayShortArray[x][y];
                        OutPixels[x][y][1] = Src_GrayShortArray[x][y];
                        OutPixels[x][y][2] = Src_GrayShortArray[x][y];
                    }
                }
            }
        } else {

            for (y = 0; y <= BitmapHeightUpper; y++) {
                for (x = 0; x <= BitmapWidthUpper; x++) {
                    if (LabelIDMap[x][y] != 0) {
                        OutPixels[x][y][0] = FoundRegionColorTable[LabelIDMap[x][y]][0];
                        OutPixels[x][y][1] = FoundRegionColorTable[LabelIDMap[x][y]][1];
                        OutPixels[x][y][2] = FoundRegionColorTable[LabelIDMap[x][y]][2];

                    } else {
                        OutPixels[x][y][0] = 0;
                        OutPixels[x][y][1] = 0;
                        OutPixels[x][y][2] = 0;
                    }

                }
            }
        }

        return ImgProc.convert_RGBShortArray_To_Image(OutPixels);
    }

    
    public byte[] convert_Image_To_2DByteArray(BufferedImage SrcImage) {
        if (SrcImage.getType() == BufferedImage.TYPE_INT_RGB) {
            byte[] a = ((DataBufferByte) SrcImage.getRaster().getDataBuffer()).getData();
            return a;
        }

        return null;
    }

    
    //Process orignal image to create a few different images
    public final void create_BaseImagesAndMaps() {

        //Set information
        ImageWidth = _Image_Source.getWidth(null);
        ImageHeight = _Image_Source.getHeight(null);

        ImageWidthUpperBound = ImageWidth - 1;
        ImageHeightUpperBound = ImageHeight - 1;



        //Generate images and maps
        GrayImageMap = ImgProc.convert_Image_To_GrayShortArray(_Image_Source);


        _Image_CannyEdgeDetection =
                CannyDet.detectCannyEdges(_Image_Source, 100, 10, 3, 1);


        CannyEedgeMap = ImgProc.convert_Image_To_GrayShortArray(_Image_CannyEdgeDetection);

        GapFloodFilledCannyEdgeMap =
                ImgProc.gapFilling(CannyEedgeMap);
        GapFloodFilledCannyEdgeMap = 
                ImgProc.floodFill(GapFloodFilledCannyEdgeMap, 1, 1, (short) 255, 1);
        GapFloodFilledCannyEdgeMap = 
                ImgProc.negative_UsingGrayShortArray(GapFloodFilledCannyEdgeMap);



        _Image_GapFloodFilledCannyEdgeDetection =
                ImgProc.convert_GrayShortArray_To_Image(GapFloodFilledCannyEdgeMap);

    }

    
    //Return Egg count
    //Return -1 if user stop analysis
    public final int detectEggs(boolean IsShowRectangle, boolean IsShowLabel) {

        do_RegionExtraction_From_ThresholdImage(IsShowRectangle);


        if (IsShowLabel) {
            _Image_FinalOverLap = draw_Labels_Of_DetectedObjects(
                    _Image_FinalOverLapWithoutLabels, ID_Info_Accumu, ID_Info_Accumu_Count, 1);
        } else {
            _Image_FinalOverLap = ImgProc.deepCopy(_Image_FinalOverLapWithoutLabels);
        }


        return EggCount;

    }

    public final void do_RegionExtraction_From_ThresholdImage(boolean IsShowRectangle) {
        int StartThresholdValue;
        int EndTrhesholdValue;
        int ThresholdStep;
        int q;


        StartThresholdValue = Math.min(myScreeningCondition.Threshold_High, 230);

        EndTrhesholdValue = Math.max(myScreeningCondition.Threshold_Low, 30);

        ThresholdStep = -myScreeningCondition.Threshold_StepSize;


        ID_Info_Accumu_Count = 0;


        for (q = StartThresholdValue; q >= EndTrhesholdValue; q += ThresholdStep) {

            _Image_BlackWhite = ImgProc.bwLeveling(_Image_Source, q, true);

            _Image_RegionExtraction = ImgProc.regionExtract_RasterScanning(_Image_BlackWhite, 0);

            LabelIDMap = ImgProc.RegionLabeling_LabelIDMap;
            FoundRegionColorTable = ImgProc.RegionLabeling_ColorTable;


            LabelIDMap = check_LabelID_From_Threshold_RegionExt(LabelIDMap, FoundRegionColorTable);


            _Image_Debug = regionExtract_RasterScanning_Update_LabelIDMap_To_Image(
                    _Image_Source, LabelIDMap, true, 1);


            if (ID_Info.length - 1 > 1) {

                for (int w = 1; w <= ID_Info.length - 1; w++) {

                    if (ID_Info[w].IsExcludeThisID == false) {

                        ID_Info_Accumu_Count += 1;
                        ID_Info_Accumu[ID_Info_Accumu_Count] = new ID_InfoDef();
                        ID_Info_Accumu[ID_Info_Accumu_Count].AvgCenter_X = ID_Info[w].AvgCenter_X;
                        ID_Info_Accumu[ID_Info_Accumu_Count].AvgCenter_Y = ID_Info[w].AvgCenter_Y;
                        ID_Info_Accumu[ID_Info_Accumu_Count].AvgGrayOfEggBox = ID_Info[w].AvgGrayOfEggBox;
                        ID_Info_Accumu[ID_Info_Accumu_Count].AvgGrayOfMaskedObject = ID_Info[w].AvgGrayOfMaskedObject;
                        ID_Info_Accumu[ID_Info_Accumu_Count].AvgRadius = ID_Info[w].AvgRadius;
                        ID_Info_Accumu[ID_Info_Accumu_Count].CannyEdgePointCount = ID_Info[w].CannyEdgePointCount;
                        ID_Info_Accumu[ID_Info_Accumu_Count].CubicLenth = ID_Info[w].CubicLenth;
                        ID_Info_Accumu[ID_Info_Accumu_Count].DuplicateCount = ID_Info[w].DuplicateCount;
                        ID_Info_Accumu[ID_Info_Accumu_Count].EstimatedEggCount = ID_Info[w].EstimatedEggCount;
                        ID_Info_Accumu[ID_Info_Accumu_Count].IsExcludeThisID = ID_Info[w].IsExcludeThisID;
                        ID_Info_Accumu[ID_Info_Accumu_Count].Method = ID_Info[w].Method;
                        ID_Info_Accumu[ID_Info_Accumu_Count].PixelCount = ID_Info[w].PixelCount;
                        ID_Info_Accumu[ID_Info_Accumu_Count].ProbablityPercent = ID_Info[w].ProbablityPercent;
                        ID_Info_Accumu[ID_Info_Accumu_Count].SqrtDiffRadius_CannyEdge = ID_Info[w].SqrtDiffRadius_CannyEdge;
                        ID_Info_Accumu[ID_Info_Accumu_Count].SqrtDiffRadius_FilledRegion = ID_Info[w].SqrtDiffRadius_FilledRegion;
                        ID_Info_Accumu[ID_Info_Accumu_Count].X_Max = ID_Info[w].X_Max;
                        ID_Info_Accumu[ID_Info_Accumu_Count].X_Min = ID_Info[w].X_Min;
                        ID_Info_Accumu[ID_Info_Accumu_Count].Y_Max = ID_Info[w].Y_Max;
                        ID_Info_Accumu[ID_Info_Accumu_Count].Y_Min = ID_Info[w].Y_Min;
                    }
                }

            }

        }


        check_CannyEdgePoints();

        estimate_NumberOfEggs();

        check_Circularity_CannyEdge();
        check_Circularity_FilledRegion();

        removeDuplicateLabelID_Of_IDInfo_Accumu();


        estimate_NumberOfEggs();


        reorder_IDInfo_Accumu();


        EggCount = count_DetectedEggs();




        if (IsShowRectangle) {

            _Image_FinalOverLapWithoutLabels = draw_Rectangles_Of_DetectedObjects(
                    _Image_Source, ID_Info_Accumu, ID_Info_Accumu_Count, 1);
        } else {
            _Image_FinalOverLapWithoutLabels = ImgProc.deepCopy(_Image_Source);
        }

    }

    public final int count_DetectedEggs() {
        int tempEggCount;

        tempEggCount = 0;
        for (int w = 1; w <= ID_Info_Accumu_Count; w++) {

            if (ID_Info_Accumu[w].IsExcludeThisID == false) {

                tempEggCount += ID_Info_Accumu[w].EstimatedEggCount;

            }

        }

        return tempEggCount;
    }

    //Draw bounding box of detected objects
    BufferedImage draw_Rectangles_Of_DetectedObjects(BufferedImage Source_Image,
            ID_InfoDef[] Src_IDInfo_Accumu, int Src_IDIno_Accumu_Count,
            float Magnification) {

        BufferedImage Src_Image = ImgProc.deepCopy(Source_Image);
        Graphics2D GraphBox = Src_Image.createGraphics();

        for (int w = 1; w <= Src_IDIno_Accumu_Count; w++) {

            if (Src_IDInfo_Accumu[w].IsExcludeThisID == false) {
                if ((Src_IDInfo_Accumu[w].ProbablityPercent) >= 
                        (myScreeningCondition.Default_CutOffProbability)) {
                    if (Src_IDInfo_Accumu[w].Method.equals(Method_AutomatedCounting)) {
                        if (Src_IDInfo_Accumu[w].EstimatedEggCount >= 1) {
                            GraphBox.setColor(Color.red);
                            GraphBox.drawRect((int) Math.rint(Src_IDInfo_Accumu[w].X_Min
                                    * Magnification) - 3,
                                    (int) Math.rint(Src_IDInfo_Accumu[w].Y_Min
                                    * Magnification) - 3, 
                                    (int) Math.rint(Src_IDInfo_Accumu[w].X_Max
                                    - (int) Math.rint(Src_IDInfo_Accumu[w].X_Min *
                                    Magnification) + 6),
                                    (int) Math.rint((Src_IDInfo_Accumu[w].Y_Max
                                    - Src_IDInfo_Accumu[w].Y_Min * Magnification) + 6));

                        }
                    } else {
                        GraphBox.setColor(Color.blue);
                        GraphBox.drawRect((int) Math.rint(Src_IDInfo_Accumu[w].X_Min *
                                Magnification) - 3,
                                (int) Math.rint(Src_IDInfo_Accumu[w].Y_Min * Magnification) - 3,
                                (int) Math.rint((Src_IDInfo_Accumu[w].X_Max
                                - Src_IDInfo_Accumu[w].X_Min * Magnification) + 6),
                                (int) Math.rint((Src_IDInfo_Accumu[w].Y_Max
                                - Src_IDInfo_Accumu[w].Y_Min * Magnification) + 6));
                    }
                }
            }

        }

        return Src_Image;
    }


    public void get_NumberOfPiecesInXandY(String FullPathLogFileName) {

        String lineStr;
        String[] ItemArray;
        String singleItem;
        HashMap Hash_LocationOfPieceInX = new HashMap();
        HashMap Hash_LocationOfPieceInY = new HashMap();


            try  {
                FileReader fr = new FileReader(FullPathLogFileName);
                BufferedReader br = new BufferedReader(fr);
                br.readLine();
                br.readLine();
                br.readLine();

                while ((lineStr = br.readLine()) != null) {

                    ItemArray = lineStr.split("\t");

                    if (ItemArray.length == 3) {
                        singleItem = ItemArray[0].substring(0, 6);

                        if (singleItem.equals("piece_")) {

                            if (Hash_LocationOfPieceInX.containsValue(ItemArray[1]) == false) {
                                Hash_LocationOfPieceInX.put(Hash_LocationOfPieceInX.size() + 1,
                                        ItemArray[1]);
                            }

                            if (Hash_LocationOfPieceInY.containsValue(ItemArray[2]) == false) {
                                Hash_LocationOfPieceInY.put(Hash_LocationOfPieceInY.size() + 1, 
                                        ItemArray[2]);
                            }

                        }
                    }

                }
                
            numberOfPiecesInX = Hash_LocationOfPieceInX.size();
            numberOfPiecesInY = Hash_LocationOfPieceInY.size();
        
        } catch (Exception ex) {
            numberOfPiecesInX = 0;
            numberOfPiecesInY = 0;
        }

    }

    
    //Draw label strings
    BufferedImage draw_Labels_Of_DetectedObjects(BufferedImage Source_Image,
            ID_InfoDef[] Src_IDInfo_Accumu, int Src_IDIno_Accumu_Count, float Magnification) {
        BufferedImage Src_Image = ImgProc.deepCopy(Source_Image);
        Graphics2D GraphBox = Src_Image.createGraphics();

        int x;
        int y;

        String DrawString;



        for (int q = 1; q <= Src_IDIno_Accumu_Count; q++) {

            if (Src_IDInfo_Accumu[q].IsExcludeThisID == false) {

                if (Src_IDInfo_Accumu[q].ProbablityPercent >= 
                        myScreeningCondition.Default_CutOffProbability) {

                    if (Src_IDInfo_Accumu[q].Method.equals(Method_AutomatedCounting)) {

                        if (Src_IDInfo_Accumu[q].EstimatedEggCount > 1) {
                            y = (int) Math.rint(Src_IDInfo_Accumu[q].Y_Min * Magnification + 8);
                            x = (int) Math.rint(Src_IDInfo_Accumu[q].X_Max * Magnification + 4);

                            DrawString = 
                                    new Integer((int) Math.rint(Src_IDInfo_Accumu[q].EstimatedEggCount)).toString();
                            GraphBox.setColor(Color.white);
                            GraphBox.drawString(DrawString, x - 1, y - 1);
                            GraphBox.drawString(DrawString, x - 1, y + 1);
                            GraphBox.drawString(DrawString, x + 1, y + 1);
                            GraphBox.drawString(DrawString, x + 1, y - 1);

                            GraphBox.setColor(Color.blue);
                            GraphBox.drawString(DrawString, x, y);
                        }

                    } else {

                        y = (int) Math.rint(Src_IDInfo_Accumu[q].Y_Min * Magnification + 8);
                        x = (int) Math.rint(Src_IDInfo_Accumu[q].X_Max * Magnification + 4);


                        DrawString = 
                                new Integer((int) Math.rint(Src_IDInfo_Accumu[q].EstimatedEggCount)).toString();

                        GraphBox.setColor(Color.white);

                        GraphBox.drawString(DrawString, x - 1, y - 1);
                        GraphBox.drawString(DrawString, x - 1, y + 1);
                        GraphBox.drawString(DrawString, x + 1, y + 1);
                        GraphBox.drawString(DrawString, x + 1, y - 1);

                        GraphBox.setColor(Color.blue);
                        GraphBox.drawString(DrawString, x, y);

                    }

                }
            }
        }


        return Src_Image;
    }

    
    //Set the order of processing piece image for detecting single eggs
    public void initialize_ImageScanOrder(int ImageCount_InX, int ImageCount_InY) {

        int CenterX;
        int CenterY;
        int CenterImgNumb;

        CenterX = (int) Math.rint((int) Math.floor((double) (ImageCount_InX / 2.0))) + 1;
        CenterY = (int) Math.rint((int) Math.floor((double) (ImageCount_InY / 2.0))) + 1;
        CenterImgNumb = (CenterY - 1) * ImageCount_InX + CenterX;



        imageScanOrder = new int[5];

        imageScanOrder[0] = CenterImgNumb;
        imageScanOrder[1] = CenterImgNumb - 1;
        imageScanOrder[2] = CenterImgNumb + 1;
        imageScanOrder[3] = CenterImgNumb - ImageCount_InX;
        imageScanOrder[4] = CenterImgNumb + ImageCount_InX;

    }

    
    
    //Return eggcount of all images
    //Return -1 if user stop analysis
    //Return -2 if reference egg is not enough (number<=2)
    //Return -3 if thelog.txt not found
    public int analyze_AllPieces(String Source_FullPath, String Source_BaseOnlyFileName) {


        String FullPathFileName;
        int ImageNumber;
        int RefereceEggCount;
        int LocalEggCount;
        int RefereceEggCountMax = 0;
        BufferedImage WorkImage;
        int x;
        int y;
        int w;


        File f = new File(Source_FullPath + "\\" + "thelog.txt");
        if (f.exists()) {
            get_NumberOfPiecesInXandY(Source_FullPath + "\\" + "thelog.txt");
        } else {
            return -3;
        }


        initialize_ImageScanOrder(numberOfPiecesInX, numberOfPiecesInY);

        labelCurImageFileName.setText("Preparing analysis");
        _UserStopped_AllPieceAnalysis = false;
        progbarCurImage.setValue(0);


        for (w = 0; w <= imageScanOrder.length - 1; w++) {

            FullPathFileName = Source_FullPath + "\\" + Source_BaseOnlyFileName
                    + Integer.toString(imageScanOrder[w]) + ".jpeg";
            WorkImage = ImgProc.loadImage(FullPathFileName);


            if (_UserStopped_AllPieceAnalysis) {
                return -1;
            }


            if (w == 0) {
                displayImage(outframe, WorkImage);
            }


            RefereceEggCount = search_ReferenceEggs(WorkImage);
            
            displayImage(outframe, _Image_FinalOverLap);
            
            
            if (RefereceEggCountMax < RefereceEggCount) {
                RefereceEggCountMax = RefereceEggCount;
            }



            if ((RefereceEggCount == 0) || (RefereceEggCount == 1) || (RefereceEggCount == 2)) {
            } else if (RefereceEggCount == 3) {
                if (RefereceEggCountMax < 3) {
                    myScreeningCondition.Default_AvgGrayOfEgg = myScreeningCondition.AvgGrayOfEgg;
                    myScreeningCondition.Default_AvgPixelCount = myScreeningCondition.AvgPixelCount;
                }
            } else if (RefereceEggCount > 3) {
                myScreeningCondition.Default_AvgGrayOfEgg = myScreeningCondition.AvgGrayOfEgg;
                myScreeningCondition.Default_AvgPixelCount = myScreeningCondition.AvgPixelCount;

                break;
            }


        }


        //If there a very few eggs, the software doesn't analyze image
        if (RefereceEggCountMax < 3) {
            DetectedEggCount = 0;
            DetectedEggInfoCount = 0;
        } else {
            //Scanning all files
            ImageNumber = 0;

            DetectedEggCount = 0;
            DetectedEggInfoCount = 0;

            progbarCurImage.setMaximum(numberOfPiecesInX * numberOfPiecesInY);
            progbarCurImage.setValue(0);


            for (y = 1; y <= numberOfPiecesInY; y++) {
                for (x = 1; x <= numberOfPiecesInX; x++) {
                    ImageNumber += 1;

                    progbarCurImage.setValue(ImageNumber);



                    FullPathFileName = Source_FullPath + "\\"
                            + Source_BaseOnlyFileName
                            + (new Integer(ImageNumber)).toString() + ".jpeg";

                    WorkImage = ImgProc.loadImage(FullPathFileName);

                    if (_UserStopped_AllPieceAnalysis) {
                        return -1;
                    }



                    RefereceEggCount = search_ReferenceEggs(WorkImage);

                    if (_UserStopped_AllPieceAnalysis) {
                        return -1;
                    }



                    if (RefereceEggCount < 4) {
                        myScreeningCondition.AvgGrayOfEgg =
                                myScreeningCondition.Default_AvgGrayOfEgg;
                        myScreeningCondition.AvgPixelCount = 
                                myScreeningCondition.Default_AvgPixelCount;
                        myScreeningCondition.Threshold_High = 
                                myScreeningCondition.AvgGrayOfEgg + 70;
                        myScreeningCondition.Threshold_Low = 
                                myScreeningCondition.AvgGrayOfEgg;
                    }


                    labelCurImageFileName.setText(Source_BaseOnlyFileName
                            + (new Integer(ImageNumber)).toString() + ".jpeg  [Analyzing]");

                    LocalEggCount = detectEggs(true, true);
                    if (LocalEggCount == -1) {
                        //user stop analysis
                        return -1;
                    }




                    DetectedEggCount += LocalEggCount;

                    adding_NewResult_To_DetectedEggInfo(x, y,
                            WorkImage.getWidth(), WorkImage.getHeight());

                    displayImage(outframe, _Image_FinalOverLap);
                    labelCurImageFileName.setText(Source_BaseOnlyFileName
                            + (new Integer(ImageNumber)).toString() + ".jpeg  [Done]");

                    if (_UserStopped_AllPieceAnalysis) {
                        return -1;
                    }


                }
            }
        }


        if (_UserStopped_AllPieceAnalysis) {
            return -1;
        }


        export_Result(Source_FullPath + "\\" + EggCountResultFileName, false);


        return 0;
    }

    
    //Update EggInfo
    public final void adding_NewResult_To_DetectedEggInfo(int PieceImageIndex_InX,
            int PieceImageIndex_InY, int PieceImageWidth, int PieceImageheight) {
        int ShiftX;
        int ShiftY;

        for (int w = 1; w <= ID_Info_Accumu_Count; w++) {
            DetectedEggInfoCount += 1;

            DetectedEggInfo[DetectedEggInfoCount] = ID_Info_Accumu[w];


            ShiftX = (PieceImageIndex_InX - 1) * PieceImageWidth;
            ShiftY = (PieceImageIndex_InY - 1) * PieceImageheight;

            DetectedEggInfo[DetectedEggInfoCount].AvgCenter_X = 
                    ID_Info_Accumu[w].AvgCenter_X + ShiftX;
            DetectedEggInfo[DetectedEggInfoCount].AvgCenter_Y = 
                    ID_Info_Accumu[w].AvgCenter_Y + ShiftY;
            DetectedEggInfo[DetectedEggInfoCount].X_Max = 
                    ID_Info_Accumu[w].X_Max + ShiftX;
            DetectedEggInfo[DetectedEggInfoCount].X_Min = 
                    ID_Info_Accumu[w].X_Min + ShiftX;
            DetectedEggInfo[DetectedEggInfoCount].Y_Max = 
                    ID_Info_Accumu[w].Y_Max + ShiftY;
            DetectedEggInfo[DetectedEggInfoCount].Y_Min = 
                    ID_Info_Accumu[w].Y_Min + ShiftY;
        }
    }
    
    
    //Return True if OK
    //Return False if failed
    public final boolean export_Result(String OutputFullPathFileName,
            boolean IsManualInspection) {

        PrintWriter pw = null;

        try {
            pw = new PrintWriter(new FileWriter(OutputFullPathFileName));
            pw.println("# File saved at:" + "\t" + new java.util.Date().toString());
            pw.println("# Egg count:" + "\t" + Integer.toString(DetectedEggCount));
            pw.println("# Automated counting:" + "\t" + "Done");
            pw.println("# File saved at:" + "\t" + new java.util.Date().toString());


            if (IsManualInspection == false) {
                pw.println("# Manual inspection:" + "\t" + "Not yet");
            } else {
                pw.println("# Manual inspection:" + "\t" + "Done by myEggCounter");
            }

            pw.println("# pX" + "\t" + "pY" + "\t" + "width"
                    + "\t" + "height" + "\t" + "nEgg" + "\t"
                    + "areaInPixel" + "\t" + "method");


            for (int q = 1; q <= DetectedEggInfoCount; q++) {

                if (DetectedEggInfo[q].IsExcludeThisID == false && 
                        DetectedEggInfo[q].EstimatedEggCount != 0) {
                    pw.println((new Integer(new Integer((int) DetectedEggInfo[q].X_Min))).toString()
                            + "\t" + (new Integer(new Integer((int) DetectedEggInfo[q].Y_Min))).toString()
                            + "\t" + (new Integer(new Integer((int) (DetectedEggInfo[q].X_Max
                            - DetectedEggInfo[q].X_Min + 1)))).toString() + "\t"
                            + (new Integer(new Integer((int) (DetectedEggInfo[q].Y_Max
                            - DetectedEggInfo[q].Y_Min + 1)))).toString() + "\t"
                            + (new Integer(new Integer((int) DetectedEggInfo[q].EstimatedEggCount))).toString()
                            + "\t"
                            + (new Integer(new Integer((int) DetectedEggInfo[q].PixelCount))).toString()
                            + "\t" + DetectedEggInfo[q].Method);
                }


            }
            pw.flush();

        } catch (IOException ex) {
            //do nothing
        } finally {

            if (pw != null) {
                pw.close();
                return false;
            }

            return true;
        }


    }
}
