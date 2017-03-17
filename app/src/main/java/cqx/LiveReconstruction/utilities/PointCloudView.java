package cqx.LiveReconstruction.utilities;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class PointCloudView extends GLSurfaceView {
    private PointCloudRenderer pointCloudRenderer;
    private Mat pc;
    boolean isUpdating = false;
    public PointCloudView(Context context){
        super(context);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        this.setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        pointCloudRenderer = new PointCloudRenderer();
        setRenderer(pointCloudRenderer);
        Mat init = new Mat(3,1, CvType.CV_32F);
        float[] test = new float[]{0,0,0};
        init.put(0,0,test);
        setPc(init);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        new Thread(new Task()).start();
    }
    public void setPc(Mat pc){
        isUpdating = true;
        this.pc = pc.clone();
        isUpdating = false;
    }
    class Task implements Runnable{
        @Override
        public void run(){
            while(true){
                if (!isUpdating){
                    pointCloudRenderer.pc = pc.clone();
                    requestRender();
                }

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
