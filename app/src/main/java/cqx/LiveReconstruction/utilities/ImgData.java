package cqx.LiveReconstruction.utilities;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;

public class ImgData {
    private MatOfKeyPoint keyPoint;
    private Mat descriptors;
    public static ImgData newInstance(MatOfKeyPoint keyPoint, Mat descriptors){
        ImgData imgData = new ImgData();
        imgData.keyPoint = keyPoint;
        imgData.descriptors = descriptors;
        return imgData;
    }
    public MatOfKeyPoint getKeyPoint(){return this.keyPoint;}
    public Mat getDescriptors(){return this.descriptors;}
}
