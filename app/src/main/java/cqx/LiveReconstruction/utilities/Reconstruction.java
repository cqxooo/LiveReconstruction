package cqx.LiveReconstruction.utilities;

import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.LinkedList;

import static org.opencv.calib3d.Calib3d.findEssentialMat;

public class Reconstruction {
    private static final String TAG = "Reconstruction";
    private ArrayList<ImgData> imgList;
    private Mat cameraMat = new Mat(3,3, CvType.CV_64F);
    public Reconstruction(ArrayList<ImgData> imgList, double[] K){
        this.imgList = imgList;
        this.cameraMat.put(0,0,new double[]{K[0],0,K[2],0,K[1],K[3],0,0,1});
    }
    public Mat InitPointCloud(){
        MatOfKeyPoint leftPoint = imgList.get(0).getLeftPoints();
        MatOfKeyPoint rightPoint = imgList.get(0).getRightPoints();
        MatOfDMatch gm = imgList.get(0).getMatches();
        Mat em;
        Mat rot2 = new Mat(3,3,CvType.CV_64F);
        Mat t2 = new Mat(3,3,CvType.CV_64F);
        LinkedList<Point> ptlist1 = new LinkedList<>();
        LinkedList<Point> ptlist2 = new LinkedList<>();
        MatOfPoint2f kp1 = new MatOfPoint2f();
        MatOfPoint2f kp2 = new MatOfPoint2f();
        for(int i=0;i<gm.toList().size();i++){
            ptlist1.addLast(leftPoint.toList().get(gm.toList().get(i).queryIdx).pt);
            ptlist2.addLast(rightPoint.toList().get(gm.toList().get(i).trainIdx).pt);
        }
        kp1.fromList(ptlist1);
        kp2.fromList(ptlist2);
        em = findEssentialMat(kp1, kp2, cameraMat);
        Calib3d.recoverPose(em,kp1,kp2,cameraMat,rot2,t2);
        Mat rot1 = Mat.eye(3,3,CvType.CV_64F);
        Mat t1 = Mat.zeros(3,1,CvType.CV_64F);
        Mat P1 = computeProjMat(cameraMat, rot1, t1);
        Mat P2 = computeProjMat(cameraMat, rot2, t2);
        Mat pc_raw = new Mat();
        Calib3d.triangulatePoints(P1,P2,kp1,kp2,pc_raw);
        Mat pc = divideLast(pc_raw);
        return pc;
    }
    public Mat computeProjMat(Mat K, Mat R, Mat T){
        Mat Proj = new Mat(3,4,CvType.CV_64F);
        Mat RT = new Mat();
        RT.push_back(R.t());
        RT.push_back(T.t());
        Core.gemm(K,RT.t(),1, new Mat(), 0, Proj, 0);
        double test[] = new double[12];
        Proj.get(0,0,test);
        return Proj;
    }
    public Mat divideLast(Mat raw){
        Mat pc = new Mat();
        for(int i = 0;i<raw.cols();i++){
            Mat col = new Mat(4,1,CvType.CV_32F);
            Core.divide(raw.col(i),new Scalar(raw.col(i).get(3,0)),col);
            pc.push_back(col.t());
        }
        return pc.colRange(0,3);
    }
}
