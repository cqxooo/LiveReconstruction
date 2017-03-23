package cqx.LiveReconstruction.utilities;


import android.opengl.GLU;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class PointCloud {
    private FloatBuffer vertexsBuffer;
    private FloatBuffer colorsBuffer;
    private float[] points;
    private int height;
    private int width;
    public void setPoints(Mat pointCloud, float[] color){
        //Mat pc = new Mat();
        //Core.normalize(pointCloud,pc,1,0,Core.NORM_L2);
        points = new float[pointCloud.cols()*pointCloud.rows()];
        //pc.get(0,0,points);
        pointCloud.get(0,0,points);
        ByteBuffer vbb= ByteBuffer.allocateDirect(points.length*4);
        vbb.order(ByteOrder.nativeOrder());
        vertexsBuffer=vbb.asFloatBuffer();
        vertexsBuffer.put(points);
        vertexsBuffer.position(0);

        ByteBuffer cbb= ByteBuffer.allocateDirect(color.length*4);
        cbb.order(ByteOrder.nativeOrder());
        colorsBuffer=cbb.asFloatBuffer();
        colorsBuffer.put(color);
        colorsBuffer.position(0);
    }
    public void setResolution(int width, int height){
        this.width = width;
        this.height = height;
    }
    public void draw(GL10 gl){
        float[] margin = getMargin();
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(margin[0], margin[1], margin[2], margin[3], margin[4], margin[5]);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        GLU.gluLookAt(gl, 0.0f,0.0f,margin[4],  0.0f,0.0f,0.0f,  0.0f,1.0f,0.0f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3,GL10.GL_FLOAT,0,vertexsBuffer);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glColorPointer(4,GL10.GL_FLOAT,0,colorsBuffer);
        gl.glDrawArrays(GL10.GL_POINTS,0,points.length/3);
        gl.glPointSize(8f);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
    public float[] getMargin(){
        float x_min, x_max, y_min, y_max, z_min, z_max;
        x_min = y_min = z_min = 1000.0f;
        x_max = y_max = z_max = 0.0f;
        if(points != null && points.length>6){
            for(int i=0;i<points.length/3;i++){
                if(x_min>points[i]){
                    x_min = points[i];
                }
                if(x_max<points[i]){
                    x_max = points[i];
                }
                if(y_min>points[i+1]){
                    y_min = points[i+1];
                }
                if(y_max<points[i+1]){
                    y_max = points[i+1];
                }
                if(z_min>points[i+2]){
                    z_min = points[i+2];
                }
                if(z_max<points[i+2]){
                    z_max = points[i+2];
                }
            }
        }
        return new float[]{x_min, x_max, y_min, y_max, z_min, z_max};
    }
}

