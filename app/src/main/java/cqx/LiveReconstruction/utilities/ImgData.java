package cqx.LiveReconstruction.utilities;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;

public class ImgData {
    private MatOfKeyPoint leftPoints;
    private MatOfKeyPoint rightPoints;
    private MatOfDMatch matches;
    private Mat fm;
    public static ImgData newInstance(MatOfKeyPoint leftPoints, MatOfKeyPoint rightPoints, MatOfDMatch matches, Mat fm){
        ImgData imgData = new ImgData();
        imgData.leftPoints = leftPoints;
        imgData.rightPoints = rightPoints;
        imgData.matches = matches;
        imgData.fm = fm;
        return imgData;
    }
    public MatOfKeyPoint getLeftPoints(){return this.leftPoints;}
    public MatOfKeyPoint getRightPoints(){return this.rightPoints;}
    public MatOfDMatch getMatches(){return this.matches;}
    public Mat getFM(){return this.fm;}
}
