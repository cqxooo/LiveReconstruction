package cqx.LiveReconstruction.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

import cqx.LiveReconstruction.R;

import static android.app.Activity.RESULT_OK;
import static org.opencv.calib3d.Calib3d.FM_RANSAC;
import static org.opencv.calib3d.Calib3d.findFundamentalMat;

public class ShowCorrespondences extends Fragment {
    private ImageView limg = null;
    private ImageView rimg = null;
    private ImageView rcorr = null;
    private ImageView cor = null;
    private Uri left_path = null;
    private Uri right_path = null;
    private MatOfKeyPoint keypoints1 = null;
    private MatOfKeyPoint keypoints2 = null;
    private MatOfDMatch matches = null;
    private static final String TAG = "ShowCorrespondences";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View corrLayout = inflater.inflate(R.layout.correspondence, container, false);
        limg = (ImageView)corrLayout.findViewById(R.id.left_image);
        rimg = (ImageView)corrLayout.findViewById(R.id.right_image);
        rcorr = (ImageView)corrLayout.findViewById(R.id.raw_corr);
        cor = (ImageView)corrLayout.findViewById(R.id.corr);
        limg.setOnClickListener(LoadImage);
        rimg.setOnClickListener(LoadImage);
        rcorr.setOnClickListener(LoadImage);
        cor.setOnClickListener(LoadImage);
        return corrLayout;

    }
    private View.OnClickListener LoadImage = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            switch (v.getId()){
                case R.id.left_image:
                    startActivityForResult(intent, 1);
                    break;
                case R.id.right_image:
                    startActivityForResult(intent, 2);
                    break;
                case R.id.raw_corr:
                    detectCorrespondence();
                    break;
                case R.id.corr:
                    corrFilter();
                default:
                    break;
            }
        }
    };
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null,null);
            if (cursor != null && cursor.moveToFirst()) {
                try{
                    if (requestCode == 1){
                        left_path = data.getData();
                        limg.setImageBitmap(MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), left_path));
                    }
                    if (requestCode == 2){
                        right_path = data.getData();
                        rimg.setImageBitmap(MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), right_path));
                    }
                    cursor.close();
                }catch (IOException e) {
                    Log.e(TAG,e.toString());
                }
            }
        }
    }
    private void detectCorrespondence(){
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        try{
            Mat descriptors1 = new Mat();
            Mat descriptors2 = new Mat();
            Mat outimg = new Mat();
            keypoints1 = new MatOfKeyPoint();
            keypoints2 = new MatOfKeyPoint();
            matches = new MatOfDMatch();
            Bitmap image1 = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), left_path);
            Bitmap image2 = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), right_path);
            Mat im1 = new Mat(image1.getHeight(),image1.getWidth(), CvType.CV_8UC3);
            Mat im2 = new Mat(image2.getHeight(),image2.getWidth(),CvType.CV_8UC3);
            Utils.bitmapToMat(image1,im1);
            Utils.bitmapToMat(image2,im2);
            detector.detect(im1,keypoints1);
            extractor.compute(im1, keypoints1, descriptors1);
            detector.detect(im2,keypoints2);
            extractor.compute(im2, keypoints2, descriptors2);
            matcher.match(descriptors1,descriptors2,matches);
            Imgproc.cvtColor(im1,im1,Imgproc.COLOR_RGBA2RGB);
            Imgproc.cvtColor(im2,im2,Imgproc.COLOR_RGBA2RGB);
            Features2d.drawMatches(im1,keypoints1,im2,keypoints2,matches,outimg);
            Bitmap output = Bitmap.createBitmap(outimg.cols(),outimg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(outimg,output);
            rcorr.setImageBitmap(output);
        }catch (IOException e){
            Log.e(TAG,e.toString());
        }
    }
    private void corrFilter(){
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
        LinkedList<KeyPoint> ptlist1 = new LinkedList<>();
        LinkedList<KeyPoint> ptlist2 = new LinkedList<>();
        MatOfDMatch gm = new MatOfDMatch();
        MatOfKeyPoint newkp1 = new MatOfKeyPoint();
        MatOfKeyPoint newkp2 = new MatOfKeyPoint();
        for (int i=0;i<isInliner.size();i++){
            if(isInliner.get(i)!=0){
                good_matches.addLast(matchesList.get(i));
                ptlist1.addLast(kpList1.get(matchesList.get(i).queryIdx));
                ptlist2.addLast(kpList2.get(matchesList.get(i).trainIdx));
            }
        }
        gm.fromList(good_matches);
        newkp1.fromList(ptlist1);
        newkp2.fromList(ptlist2);
        try{
            Bitmap image1 = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), left_path);
            Bitmap image2 = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), right_path);
            Mat im1 = new Mat(image1.getHeight(),image1.getWidth(), CvType.CV_8UC3);
            Mat im2 = new Mat(image2.getHeight(),image2.getWidth(),CvType.CV_8UC3);
            Utils.bitmapToMat(image1,im1);
            Utils.bitmapToMat(image2,im2);
            Imgproc.cvtColor(im1,im1,Imgproc.COLOR_RGBA2RGB);
            Imgproc.cvtColor(im2,im2,Imgproc.COLOR_RGBA2RGB);
            Mat outimg = new Mat();
            Features2d.drawMatches(im1,keypoints1,im2,keypoints2,gm,outimg);
            Bitmap output = Bitmap.createBitmap(outimg.cols(),outimg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(outimg,output);
            cor.setImageBitmap(output);
        }catch (IOException e){
            Log.e(TAG,e.toString());
        }

    }
}
