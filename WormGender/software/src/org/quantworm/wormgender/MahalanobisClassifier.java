/*
 * This class is for Mahalanobis distance classifier
 * It includes default training set and can load new training set
 * This class finally conduct classification for a new data
 */
package org.quantworm.wormgender;

import Jama.Matrix;
import Jama.QRDecomposition;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class MahalanobisClassifier {

    public int dim = 4;
    public int genre = 3;
    public double[] averMale;
    public double[] averHerma;
    public double[] averY;
    Matrix R;
    double[][] trSetMale;
    double[][] trSetHerma;
    double[][] trSetY;

    MahalanobisClassifier() {
    }

    /**
     * Apply new training set
     *
     * @param ntrSetMale training set of males
     * @param ntrSetHerma training set of hermas
     * @param ntrSetY training set of larva
     */
    public void loadSet(double[][] ntrSetMale, double[][] ntrSetHerma, double[][] ntrSetY) {

        trSetMale = NativeImgProcessing.multiArrayCopy(ntrSetMale);
        trSetHerma = NativeImgProcessing.multiArrayCopy(ntrSetHerma);
        trSetY = NativeImgProcessing.multiArrayCopy(ntrSetY);

        averMale = new double[dim];
        averHerma = new double[dim];
        averY = new double[dim];

        //compute the average vector of each sets
        for (int i = 0; i < dim; i++) {
            double sumM = 0;
            double sumH = 0;
            double sumY = 0;
            for (double[] ntrSetMale1 : ntrSetMale) {
                sumM += ntrSetMale1[i];
                if (i == 2) {
                    sumM -= 2;
                }
            }
            averMale[i] = sumM / (double) ntrSetMale.length;

            for (double[] ntrSetHerma1 : ntrSetHerma) {
                sumH += ntrSetHerma1[i];
                if (i == 2) {
                    sumH -= 2;
                }
            }
            averHerma[i] = sumH / (double) ntrSetHerma.length;

            for (double[] ntrSetY1 : ntrSetY) {
                sumY += ntrSetY1[i];
                if (i == 2) {
                    sumY -= 2;
                }
            }
            averY[i] = sumY / (double) ntrSetY.length;
        }

        double[][] pool = new double[(ntrSetMale.length + ntrSetHerma.length + ntrSetY.length)][dim];
        for (int i = 0; i < ntrSetMale.length; i++) {
            for (int j = 0; j < dim; j++) {
                pool[i][j] = ntrSetMale[i][j] - averMale[j];
            }
        }
        for (int i = 0; i < ntrSetHerma.length; i++) {
            for (int j = 0; j < dim; j++) {
                pool[ntrSetMale.length + i][j] = ntrSetHerma[i][j] - averHerma[j];
            }
        }
        for (int i = 0; i < ntrSetY.length; i++) {
            for (int j = 0; j < dim; j++) {
                pool[ntrSetMale.length + ntrSetHerma.length + i][j] = ntrSetY[i][j] - averY[j];
            }
        }
        QRDecomposition qr = new QRDecomposition(new Matrix(pool));
        R = qr.getR();
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                R.set(i, j, R.get(i, j) / Math.sqrt(ntrSetMale.length
                        + ntrSetHerma.length + ntrSetY.length - genre));
            }
        }

    }

    /**
     * classify the worm into 2 genders using generalized Mahalanobis distance
     * similar to linear classify function in MatLab
     *
     * @param info To accept the judgment
     * @param trueLen The length of worm:computed by treating diagonal arranged
     * pixel as sqrt(2) pixels
     * @param e The array containing the ratio of two ends
     * @param f The fatness of the body
     * @return
     */
    public int classifyWorm(WormInfo info, double trueLen, double[] e, double f) {

        double e1;
        double e2;
        if (e[0] <= e[1]) {
            e1 = e[0];
            e2 = e[1];
        } else {
            e1 = e[1];
            e2 = e[0];
        }
        info.e[0] = e1;
        info.e[1] = e2;

        Matrix DvMale;
        Matrix DvHerma;
        Matrix DvY;

        if (dim == 2) {
            double dvMale[][] = {{e1}, {e2}};
            double dvHerma[][] = {{e1}, {e2}};
            double dvY[][] = {{e1}, {e2}};

            for (int i = 0; i < dim; i++) {
                dvMale[i][0] -= averMale[i];
                dvHerma[i][0] -= averHerma[i];
                dvY[i][0] -= averY[i];
            }

            DvMale = new Matrix(dvMale);
            DvHerma = new Matrix(dvHerma);
            DvY = new Matrix(dvY);
        } else if (dim == 3) {
            double dvMale[][] = {{e1}, {e2}, {f}};
            double dvHerma[][] = {{e1}, {e2}, {f}};
            double dvY[][] = {{e1}, {e2}, {f}};

            for (int i = 0; i < dim; i++) {
                dvMale[i][0] -= averMale[i];
                dvHerma[i][0] -= averHerma[i];
                dvY[i][0] -= averY[i];
            }

            DvMale = new Matrix(dvMale);
            DvHerma = new Matrix(dvHerma);
            DvY = new Matrix(dvY);
        } else {
            double dvMale[][] = {{e1}, {e2}, {trueLen}, {f}};
            double dvHerma[][] = {{e1}, {e2}, {trueLen}, {f}};
            double dvY[][] = {{e1}, {e2}, {trueLen}, {f}};

            for (int i = 0; i < dim; i++) {
                dvMale[i][0] -= averMale[i];
                dvHerma[i][0] -= averHerma[i];
                dvY[i][0] -= averY[i];
            }

            DvMale = new Matrix(dvMale);
            DvHerma = new Matrix(dvHerma);
            DvY = new Matrix(dvY);
        }

        Matrix A = DvMale.transpose().times(R.inverse());
        A.arrayTimesEquals(A);

        Matrix B = DvHerma.transpose().times(R.inverse());
        B.arrayTimesEquals(B);

        Matrix C = DvY.transpose().times(R.inverse());
        C.arrayTimesEquals(C);

        double kMale = 0;
        double kHerma = 0;
        double kY = 0;
        for (int i = 0; i < dim; i++) {
            kMale += A.get(0, i);
            kHerma += B.get(0, i);
            kY += C.get(0, i);
        }

        double[] dist = {kMale, kHerma, kY};
        double min = dist[0];
        int index = 0;
        for (int i = 0; i < dist.length; i++) {
            if (dist[i] < min) {
                index = i;
                min = dist[i];
            }
        }
        if (index == 0) {
            info.nMale = 1;
        } else if (index == 1) {
            info.nHerma = 1;
        } else if (index == 2) {
            info.nY = 1;
            return -1;
        }

        return 0;
    }

    /**
     * Load training set from file
     *
     * @param TraningTxtFile
     */
    public void import_TrainingSet(File TraningTxtFile) {
        LinkedList<double[]> trSetMaleList = new LinkedList<double[]>();
        LinkedList<double[]> trSetHermList = new LinkedList<double[]>();
        LinkedList<double[]> trSetYList = new LinkedList<double[]>();

        List<String> srcTrainingTxtList
                = Utilities.getLinesFromFile(TraningTxtFile);

        for (int q = 1; q < srcTrainingTxtList.size(); q++) {
            String[] subItems = srcTrainingTxtList.get(q).split("\t");

            double[] numbArray = {Utilities.getDouble(subItems[2]),
                Utilities.getDouble(subItems[3]),
                Utilities.getDouble(subItems[1]),
                Utilities.getDouble(subItems[4])};

            if (subItems[0].toLowerCase().indexOf("\\male") > 0
                    || subItems[0].toLowerCase().indexOf(File.separator + "male") > 0) {
                trSetMaleList.add(numbArray);
            }
            if (subItems[0].toLowerCase().indexOf("\\herm") > 0
                    || subItems[0].toLowerCase().indexOf(File.separator + "herm") > 0) {
                trSetHermList.add(numbArray);
            }
            if (subItems[0].toLowerCase().indexOf("\\l3l4") > 0
                    || subItems[0].toLowerCase().indexOf(File.separator + "l3l4") > 0) {
                trSetYList.add(numbArray);
            }
        }

        trSetMale
                = Utilities.convert_ListOfDouble1D_To_ArrayOfDouble2D(trSetMaleList);
        trSetHerma
                = Utilities.convert_ListOfDouble1D_To_ArrayOfDouble2D(trSetHermList);
        trSetY
                = Utilities.convert_ListOfDouble1D_To_ArrayOfDouble2D(trSetYList);

        loadSet(trSetMale, trSetHerma, trSetY);
    }
}
