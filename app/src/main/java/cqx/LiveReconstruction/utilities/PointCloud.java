package cqx.LiveReconstruction.utilities;


import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class PointCloud {
    private FloatBuffer vertexsBuffer;
    private FloatBuffer colorsBuffer;
    private float points[];
    private int height;
    private int width;
    public void setPoints(Mat pointCloud){
        //Mat pc = new Mat();
       // Core.normalize(pointCloud,pc,1,0,Core.NORM_L2);
        points = new float[pointCloud.cols()*pointCloud.rows()];
        pointCloud.get(0,0,points);
        ByteBuffer vbb= ByteBuffer.allocateDirect(points.length*4);
        vbb.order(ByteOrder.nativeOrder());
        vertexsBuffer=vbb.asFloatBuffer();
        vertexsBuffer.put(points);
        vertexsBuffer.position(0);

    }
    public void setResolution(int width, int height){
        this.width = width;
        this.height = height;
    }
    public void draw(GL10 gl){
        gl.glViewport(0, 0, width, height);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3,GL10.GL_FLOAT,0,vertexsBuffer);
        //gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        //gl.glColorPointer(4,GL10.GL_FLOAT,0,colorsBuffer);
        gl.glDrawArrays(GL10.GL_POINTS,0,points.length/3);
        gl.glPointSize(8f);
        //gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
