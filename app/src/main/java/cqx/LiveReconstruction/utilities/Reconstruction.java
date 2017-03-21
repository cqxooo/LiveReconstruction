package cqx.LiveReconstruction.utilities;

import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static org.opencv.calib3d.Calib3d.Rodrigues;
import static org.opencv.calib3d.Calib3d.findEssentialMat;
import static org.opencv.calib3d.Calib3d.solvePnPRansac;

public class Reconstruction {
    private static final String TAG = "Reconstruction";
    private Mat cameraMat = new Mat(3,3, CvType.CV_64F);
    private Mat pointCloud;
    private Mat color;
    private ArrayList<int[]> correspondence_idx = new ArrayList<>();
    private Mat LastP;
    private final float scale = 1.0f/256.0f;
    public Reconstruction(double[] K){
        this.cameraMat.put(0,0,new double[]{K[0],0,K[2],0,K[1],K[3],0,0,1});
    }
    public Mat InitPointCloud(ImgData left, ImgData right, MatOfDMatch gm, Mat img){
        color = new Mat(gm.toList().size(), 1, CvType.CV_32FC4);
        MatOfKeyPoint leftPoint = left.getKeyPoint();
        MatOfKeyPoint rightPoint = right.getKeyPoint();
        Mat em;
        Mat rot2 = new Mat(3,3,CvType.CV_64F);
        Mat t2 = new Mat(3,1,CvType.CV_64F);
        LinkedList<Point> ptlist1 = new LinkedList<>();
        LinkedList<Point> ptlist2 = new LinkedList<>();
        MatOfPoint2f kp1 = new MatOfPoint2f();
        MatOfPoint2f kp2 = new MatOfPoint2f();
        int[] left_idx = new int[leftPoint.height()];
        int[] right_idx = new int[rightPoint.height()];
        Arrays.fill(left_idx,-1);
        Arrays.fill(right_idx,-1);
        for(int i=0;i<gm.toList().size();i++){
            ptlist1.addLast(leftPoint.toList().get(gm.toList().get(i).queryIdx).pt);
            ptlist2.addLast(rightPoint.toList().get(gm.toList().get(i).trainIdx).pt);
            left_idx[gm.toList().get(i).queryIdx] = i;
            right_idx[gm.toList().get(i).trainIdx] = i;
            int y = (int) rightPoint.toList().get(gm.toList().get(i).trainIdx).pt.y;
            int x = (int) rightPoint.toList().get(gm.toList().get(i).trainIdx).pt.x;
            double[] tmp = img.get(y, x);
            tmp[0] *= scale;
            tmp[1] *= scale;
            tmp[2] *= scale;
            tmp[3] *= scale;
            color.put(i, 0, tmp);
        }
        correspondence_idx.add(left_idx);
        correspondence_idx.add(right_idx);
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
        pointCloud = divideLast(pc_raw);
        LastP = P2.clone();
        return pointCloud.clone();
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
    public Mat addImage(ImgData left, ImgData right, MatOfDMatch gm, Mat img){
        MatOfPoint3f pc3f = MatToPoint3f(pointCloud);
        MatOfKeyPoint leftPoint = left.getKeyPoint();
        MatOfKeyPoint rightPoint = right.getKeyPoint();
        LinkedList<Point3> pclist = new LinkedList<>();
        LinkedList<Point> right_inPC = new LinkedList<>();
        LinkedList<Point> leftlist = new LinkedList<>();
        LinkedList<Point> rightist = new LinkedList<>();
        MatOfPoint3f pc = new MatOfPoint3f();
        MatOfPoint2f kp1 = new MatOfPoint2f();
        MatOfPoint2f kp2 = new MatOfPoint2f();
        int count = pointCloud.height();
        int[] left_idx = correspondence_idx.get(correspondence_idx.size()-1);
        int[] right_idx = new int[rightPoint.height()];
        Arrays.fill(right_idx,-1);
        for(int i=0;i<gm.toList().size();i++){
            if(left_idx[gm.toList().get(i).queryIdx] >=0 ){
                pclist.addLast(pc3f.toList().get(left_idx[gm.toList().get(i).queryIdx]));
                right_inPC.addLast(rightPoint.toList().get(gm.toList().get(i).trainIdx).pt);
                right_idx[gm.toList().get(i).trainIdx] = left_idx[gm.toList().get(i).queryIdx];
            }
            else{
                leftlist.addLast(leftPoint.toList().get(gm.toList().get(i).queryIdx).pt);
                rightist.addLast(rightPoint.toList().get(gm.toList().get(i).trainIdx).pt);
                left_idx[gm.toList().get(i).queryIdx] = count;
                right_idx[gm.toList().get(i).trainIdx] = count;
                int y = (int) rightPoint.toList().get(gm.toList().get(i).trainIdx).pt.y;
                int x = (int) rightPoint.toList().get(gm.toList().get(i).trainIdx).pt.x;
                double[] tmp = img.get(y, x);
                tmp[0] *= scale;
                tmp[1] *= scale;
                tmp[2] *= scale;
                tmp[3] *= scale;
                Mat dummy = new Mat(1,1,CvType.CV_32FC4);
                color.push_back(dummy);
                count++;
            }
        }
        pc.fromList(pclist);
        kp2.fromList(right_inPC);
        Mat rotvec = new Mat(3,1,CvType.CV_64F);
        Mat rot = new Mat(3,3,CvType.CV_64F);
        Mat t = new Mat(3,1,CvType.CV_64F);
        solvePnPRansac(pc,kp2,cameraMat,new MatOfDouble(),rotvec,t);
        kp1.fromList(leftlist);
        kp2.fromList(rightist);
        Rodrigues(rotvec,rot);
        Mat P = computeProjMat(cameraMat, rot, t);
        Mat pc_raw = new Mat();
        Calib3d.triangulatePoints(LastP,P,kp1,kp2,pc_raw);
        Mat new_PC = divideLast(pc_raw);
        pointCloud.push_back(new_PC);
        LastP = P.clone();
        return pointCloud.clone();
    }
    public MatOfPoint3f MatToPoint3f(Mat m){
        MatOfPoint3f points = new MatOfPoint3f();
        LinkedList<Point3> ptlist = new LinkedList<>();
        Point3 pt = new Point3();
        Mat doubleM = new Mat();
        m.convertTo(doubleM,CvType.CV_64F);
        for(int i=0;i<m.height();i++){
            double[] tmp = new double[3];
            doubleM.get(0,0,tmp);
            pt.set(tmp);
            ptlist.add(pt);
        }
        points.fromList(ptlist);
        return points;
    }
    public float[] getColor(){
        float[] c = new float[color.height()*4];
        for(int i=0;i<color.height();i++){
            double[] tmp = color.get(i,0);
            c[i] = (float) tmp[0];
            c[i+1] = (float) tmp[1];
            c[i+2] = (float) tmp[2];
            c[i+3] = (float) tmp[3];
        }
        return c;
    }
}
