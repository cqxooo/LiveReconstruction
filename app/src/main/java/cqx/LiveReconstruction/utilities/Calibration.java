package cqx.LiveReconstruction.utilities;


import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.utils.Converters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.opencv.calib3d.Calib3d.FM_RANSAC;
import static org.opencv.calib3d.Calib3d.findFundamentalMat;

public class Calibration {
    private static final String TAG = "Calibration";
    private ArrayList<Uri> uriList = new ArrayList<>();
    public Activity mActivity;
    private double u0;
    private double v0;
    public Calibration(ArrayList<Uri> uri, Activity ma){
        this.uriList = uri;
        this.mActivity = ma;
    }
    public ArrayList<double[]> computeK(){
        ArrayList<double[]> K = new ArrayList<>();
        for(int i=0;i<uriList.size()-1;i++){
            ImgData imgData = detectCorrespondence(uriList.get(i),uriList.get(i+1));
            double focal[] = getFocal(imgData.getFM());
            K.add(new double[]{focal[0],focal[1],u0,v0});
        }
        return K;
    }
    public ImgData detectCorrespondence(Uri left_path, Uri right_path){
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        try{
            Mat descriptors1 = new Mat();
            Mat descriptors2 = new Mat();
            MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
            MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
            MatOfDMatch matches = new MatOfDMatch();
            Bitmap image1 = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), left_path);
            Bitmap image2 = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), right_path);
            u0 = image1.getWidth()/2;
            v0 = image1.getHeight()/2;
            Mat im1 = new Mat(image1.getHeight(),image1.getWidth(), CvType.CV_8UC3);
            Mat im2 = new Mat(image2.getHeight(),image2.getWidth(),CvType.CV_8UC3);
            Utils.bitmapToMat(image1,im1);
            Utils.bitmapToMat(image2,im2);
            detector.detect(im1,keypoints1);
            extractor.compute(im1, keypoints1, descriptors1);
            detector.detect(im2,keypoints2);
            extractor.compute(im2, keypoints2, descriptors2);
            matcher.match(descriptors1,descriptors2,matches);
            List<DMatch> matchesList = matches.toList();
            List<KeyPoint> kpList1 = keypoints1.toList();
            List<KeyPoint> kpList2 = keypoints2.toList();
            LinkedList<Point> points1 = new LinkedList<>();
            LinkedList<Point> points2 = new LinkedList<>();
            for (int i=0;i<matchesList.size();i++){
                points1.addLast(kpList1.get(matchesList.get(i).queryIdx).pt);
                points2.addLast(kpList2.get(matchesList.get(i).trainIdx).pt);
            }
            MatOfPoint2f kp1 = new MatOfPoint2f();
            MatOfPoint2f kp2 = new MatOfPoint2f();
            kp1.fromList(points1);
            kp2.fromList(points2);
            Mat inliner = new Mat();
            Mat F = findFundamentalMat(kp1,kp2,FM_RANSAC,1,0.99, inliner);
            List<Byte> isInliner = new ArrayList<>();
            Converters.Mat_to_vector_uchar(inliner,isInliner);
            LinkedList<DMatch> good_matches = new LinkedList<>();
            MatOfDMatch gm = new MatOfDMatch();
            for (int i=0;i<isInliner.size();i++){
                if(isInliner.get(i)!=0){
                    good_matches.addLast(matchesList.get(i));
                }
            }
            gm.fromList(good_matches);
            return ImgData.newInstance(keypoints1,keypoints2,gm, F);
        }catch (IOException e){
            Log.e(TAG,e.toString());
            return null;
        }
    }
    public double[] getFocal(Mat fm){
        double focal[] = new double[2];
        double a1, b1, c1, d1, e1, f1;
        double a2, b2, c2, d2, e2, f2;
        double h1, i1, j1, k1, l1;
        double h2, i2, j2, k2, l2;
        double a, b, c, d, e;
        /*double F[] = {0.000000216061742, -0.000001923123233,  -0.000326418955152,
                      0.000001157076688,  0.000002578109281,   0.002080082887732,
                      0.001619337592209, -0.002820407533338,   0.999992494884871};*/
        Mat u = new Mat(3,3,CvType.CV_64F);
        Mat vt = new Mat(3,3,CvType.CV_64F);
        Mat w = new Mat(3,1,CvType.CV_64F);
        //fm.put(0,0,F);
        Core.SVDecomp(fm,w,u,vt);
        double[] m = new double[3];
        double[] n = new double[3];
        double[] o = new double[3];
        double[] p = new double[3];
        double[] dd = new double[3];
        u.t().get(0,0,m);
        u.t().get(1,0,n);
        vt.get(0,0,o);
        vt.get(1,0,p);
        w.get(0,0,dd);
        a1 = -dd[1]*dd[1]*m[0]*n[0]*p[0]*p[0]-dd[0]*dd[1]*m[0]*m[0]*o[0]*p[0];
        b1 = -dd[1]*dd[1]*m[1]*n[1]*p[1]*p[1]-dd[0]*dd[1]*m[1]*m[1]*o[1]*p[1];
        c1 = -dd[1]*dd[1]*(m[0]*n[0]*p[1]*p[1]+m[1]*n[1]*p[0]*p[0])-dd[0]*dd[1]*(m[0]*m[0]*o[1]*p[1]+m[1]*m[1]*o[0]*p[0]);
        d1 = -dd[1]*dd[1]*(getB(p,p,u0,v0)*m[0]*n[0]+getB(m,n,u0,v0)*p[0]*p[0])-dd[0]*dd[1]*(getB(o,p,u0,v0)*m[0]*m[0]+getB(m,m,u0,v0)*o[0]*p[0]);
        e1 = -dd[1]*dd[1]*(getB(p,p,u0,v0)*m[1]*n[1]+getB(m,n,u0,v0)*p[1]*p[1])-dd[0]*dd[1]*(getB(o,p,u0,v0)*m[1]*m[1]+getB(m,m,u0,v0)*o[1]*p[1]);
        f1 = -dd[1]*dd[1]*getB(m,n,u0,v0)*getB(p,p,u0,v0)-dd[0]*dd[1]*getB(m,m,u0,v0)*getB(o,p,u0,v0);

        a2 = dd[0]*dd[0]*m[0]*m[0]*o[0]*o[0]-dd[1]*dd[1]*n[0]*n[0]*p[0]*p[0];
        b2 = dd[0]*dd[0]*m[1]*m[1]*o[1]*o[1]-dd[1]*dd[1]*n[1]*n[1]*p[1]*p[1];
        c2 = dd[0]*dd[0]*(m[0]*m[0]*o[1]*o[1]+m[1]*m[1]*o[0]*o[0])-dd[1]*dd[1]*(n[0]*n[0]*p[1]*p[1]+n[1]*n[1]*p[0]*p[0]);
        d2 = dd[0]*dd[0]*(getB(o,o,u0,v0)*m[0]*m[0]+getB(m,m,u0,v0)*o[0]*o[0])-dd[1]*dd[1]*(getB(p,p,u0,v0)*n[0]*n[0]+getB(n,n,u0,v0)*p[0]*p[0]);
        e2 = dd[0]*dd[0]*(getB(o,o,u0,v0)*m[1]*m[1]+getB(m,m,u0,v0)*o[1]*o[1])-dd[1]*dd[1]*(getB(p,p,u0,v0)*n[1]*n[1]+getB(n,n,u0,v0)*p[1]*p[1]);
        f2 = dd[0]*dd[0]*getB(m,m,u0,v0)*getB(o,o,u0,v0)-dd[1]*dd[1]*getB(n,n,u0,v0)*getB(p,p,u0,v0);

        h2 = b1*a2-a1*b2;
        i2 = a2*c1-a1*c2;
        j2 = a2*d1-a1*d2;
        k2 = a2*e1-a1*e2;
        l2 = a2*f1-a1*f2;

        h1 = b2*a1-a2*b1;
        i1 = b2*c1-b1*c2;
        j1 = b2*d1-b1*d2;
        k1 = b2*e1-b1*e2;
        l1 = b2*f1-b1*f2;

        a = h2*h1*h1-i1*h1*i2;
        b = (2*h2*h1*j1-i1*j1*i2-h1*k1*i2+i1*i1*j2-i1*h1*k2)/a;
        c = (2*h1*h2*l1+h2*j1*j1-l1*i1*i2-i2*j1*k1+2*i1*j2*k1-i1*k2*j1-h1*k1*k2+i1*i1*l2)/a;
        d = (2*l1*j1*h2-i2*l1*k1+k1*k1*j2-l1*i1*k2-j1*k1*k2+2*i1*k1*l2)/a;
        e = (h2*l1*l1-l1*k1*k2+l2*k1*k1)/a;
        Complex result[];
        double coef[] = {e,d,c,b,1};
        LaguerreSolver solver = new LaguerreSolver();
        result = solver.solveAllComplex(coef, 0.0);
        for(int i=0;i<result.length;i++){
            if(result[i].getReal()>0 && result[i].getImaginary()==0){
                focal[0] = result[i].getReal();
                focal[1] = -(h1*focal[0]*focal[0]+j1*focal[0]+l1)/(i1*focal[0]+k1);
                focal[0] = Math.sqrt(focal[0]);
                focal[1] = Math.sqrt(focal[1]);
            }
        }
        return focal;

    }
    public double getB(double[] m, double[] n, double u0, double v0){
        double b;
        b = m[0]*n[0]*u0*u0+m[1]*n[1]*v0*v0+m[1]*n[0]*u0*v0+m[0]*n[1]*u0*v0+m[2]*n[0]*u0+m[0]*n[2]*u0+m[2]*n[1]*v0+m[1]*n[2]*v0+m[2]*n[2];
        return b;
    }
}
