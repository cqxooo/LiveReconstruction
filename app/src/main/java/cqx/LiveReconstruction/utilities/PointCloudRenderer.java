package cqx.LiveReconstruction.utilities;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import org.opencv.core.Mat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PointCloudRenderer implements GLSurfaceView.Renderer {
    private PointCloud pointCloud;
    private int height;
    private int width;
    public volatile Mat pc;
    public float[] color;
    public float[] margin;
    public PointCloudRenderer(){
        this.pointCloud = new PointCloud();
    }
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glDisable(GL10.GL_DITHER);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        float[] margin = pointCloud.getMargin();
        this.height = height;
        this.width = width;
        if (height == 0) {
            height = 1;
        }
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(margin[0], margin[1], margin[2], margin[3], margin[4], margin[5]);
        //GLU.gluPerspective(gl, 45, (float) height * 2.0f/(float)width, 0.1f, 100.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        GLU.gluLookAt(gl, 0.0f,0.0f,margin[4],  0.0f,0.0f,0.0f,  0.0f,1.0f,0.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        //gl.glTranslatef(0.0f, 0.0f, -3.0f);
        //GLU.gluLookAt(gl, 0.0f,0.0f,5.0f,  0.0f,0.0f,0.0f,  0.0f,1.0f,0.0f);
        //gl.glRotatef(45,1.0f,1.0f,1.0f);
        this.pointCloud.setResolution(width,height);
        this.pointCloud.setPoints(pc, color);
        pointCloud.draw(gl);
    }
}