package cqx.LiveReconstruction.utilities;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;

public class CalibData {
    private MatOfKeyPoint leftPoints;
    private MatOfKeyPoint rightPoints;
    private MatOfDMatch matches;
    private Mat fm;
    public static CalibData newInstance(MatOfKeyPoint leftPoints, MatOfKeyPoint rightPoints, MatOfDMatch matches, Mat fm){
        CalibData calibData = new CalibData();
        calibData.leftPoints = leftPoints;
        calibData.rightPoints = rightPoints;
        calibData.matches = matches;
        calibData.fm = fm;
        return calibData;
    }
    public MatOfKeyPoint getLeftPoints(){return this.leftPoints;}
    public MatOfKeyPoint getRightPoints(){return this.rightPoints;}
    public MatOfDMatch getMatches(){return this.matches;}
    public Mat getFM(){return this.fm;}
}
