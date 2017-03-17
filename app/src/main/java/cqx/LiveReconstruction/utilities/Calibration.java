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
import org.opencv.imgproc.Imgproc;
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
    public Calibration(double u0, double v0){
        this.u0 = u0;
        this.v0 = v0;
    }
    public Calibration(ArrayList<Uri> uri, Activity ma){
        this.uriList = uri;
        this.mActivity = ma;
    }
    public ArrayList<double[]> computeKs(){
        ArrayList<double[]> K = new ArrayList<>();
        return K;
    }
    public double[] computeK(Mat F){
        double K[] = new double[4];
        double focal[] = getFocal(F);
        if(focal[0]>0 && focal[1]>0){
            K = new double[]{focal[0],focal[1],u0,v0};
        }
        return K;
    }
    public double[] computeK(){
        double K[] = new double[4];
        return K;
    }
    public ImgData detectFeature(Mat mRgba){
        Mat img = new Mat(mRgba.height(), mRgba.width(), CvType.CV_8UC3);;
        Imgproc.cvtColor(mRgba, img, Imgproc.COLOR_RGBA2BGR, 3);
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();
        detector.detect(img, keypoints);
        extractor.compute(img, keypoints, descriptors);
        return ImgData.newInstance(keypoints,descriptors);
    }
    public MatchInfo detectCorrespondence(ImgData left, ImgData right){
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(left.getDescriptors(),right.getDescriptors(),matches);
        List<DMatch> matchesList = matches.toList();
        List<KeyPoint> kpList1 = left.getKeyPoint().toList();
        List<KeyPoint> kpList2 = right.getKeyPoint().toList();
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
        Mat F = findFundamentalMat(kp1,kp2,FM_RANSAC,3,0.99, inliner);
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
        return MatchInfo.newInstance(gm, F);
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
            return null;
            //return ImgData.newInstance(keypoints1,keypoints2,gm, F);
        }catch (IOException e){
            Log.e(TAG,e.toString());
            return null;
        }
    }
    public double[] getFocal(Mat fm){
        //u0 = 320;
        //v0 = 240;
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
        Complex x[];
        Complex y[] = new Complex[4];
        double coef[] = {e,d,c,b,1};
        LaguerreSolver solver = new LaguerreSolver();
        x = solver.solveAllComplex(coef, 0.0);
        for(int i=0;i<x.length;i++){
            //compute y: y = -(h1 * x^2 + j1 * x + l1)/(i1 * x + k1)
            //s = h1 * x^2
            //q = j1 * x;
            //r = i1 * x;
            Complex s = x[i].multiply(x[i]).multiply(h1);
            Complex q = x[i].multiply(j1);
            Complex r = x[i].multiply(i1);
            y[i] = s.add(q).add(l1).divide(r.add(k1)).multiply(-1);
        }
        for(int i=0;i<x.length;i++){
            if(x[i].getReal()>0 & y[i].getReal()>0){
                focal[0] = Math.sqrt(x[i].getReal());
                focal[1] = Math.sqrt(y[i].getReal());
                break;
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
