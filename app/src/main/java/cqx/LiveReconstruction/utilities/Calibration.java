package cqx.LiveReconstruction.utilities;


import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.android.Utils;
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
import org.opencv.features2d.Features2d;
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
    public Calibration(ArrayList<Uri> uri, Activity ma){
        this.uriList = uri;
        this.mActivity = ma;
    }
    public ArrayList<Mat> computeK(){
        ArrayList<Mat> fm = new ArrayList<>();
        for(int i=0;i<uriList.size()-1;i++){
            CalibData calibData = detectCorrespondence(uriList.get(i),uriList.get(i+1));
            fm.add(calibData.getFM());
        }
        return fm;
    }
    public CalibData detectCorrespondence(Uri left_path, Uri right_path){
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
            return CalibData.newInstance(keypoints1,keypoints2,gm, F);
        }catch (IOException e){
            Log.e(TAG,e.toString());
            return null;
        }
    }
}
