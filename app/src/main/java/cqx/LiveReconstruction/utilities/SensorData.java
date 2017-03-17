package cqx.LiveReconstruction.utilities;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import cqx.LiveReconstruction.activities.CameraActivity;

public class SensorData {
    private float gravity[] = new float[3];
    private float noise[] = new float[3];
    private float linear_acceleration[] = new float[3];
    private long time_prev = System.currentTimeMillis();
    private final SensorManager sm;
    private final CameraActivity cameraActivity;
    private Sensor rSensor;
    private Sensor gSensor;
    private Sensor aSensor;
    private Sensor wSensor;
    private boolean init = false;
    float[] rotpred = new float[9];
    float[] rot = new float[9];
    float[] wData = new float[3];
    private float[] velocity = {0,0,0};
    private float[] shift = {0,0,0};
    private float[] last_accel = {0,0,0};
    private float last_rot;
    public SensorData(SensorManager sm, CameraActivity cameraActivity){
        this.sm = sm;
        this.cameraActivity = cameraActivity;
        aSensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        //wSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sm.registerListener(Listener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(Listener, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(Listener, rSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //sm.registerListener(wListener, wSensor, SensorManager.SENSOR_DELAY_GAME);
    }
    public void onPause() {
        sm.unregisterListener(Listener);
    }

    public void onResume() {
        sm.registerListener(Listener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(Listener, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(Listener, rSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    private SensorEventListener Listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            final float alpha = 0.8f;
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
                    double angle[] = getAngle(gravity);
                    if(angle[0]>0 && angle[0]<20){
                        cameraActivity.setReconMode(true);
                    }
                    else{
                        cameraActivity.setReconMode(false);
                    }
                    //Log.d("Angle","X: "+angle[0]+", Y: "+angle[1]+", Z: "+angle[2]);
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    long time_curr = System.currentTimeMillis();
                    float dt = (time_curr - time_prev);
                    final float scale = 1.0f/1000000.0f;
                    if(dt>170) {
                        time_prev = time_curr;
                        float[] accel = new float[3];
                        accel[0] = event.values[0] - last_accel[0];
                        accel[1] = event.values[1] - last_accel[1];
                        accel[2] = event.values[2] - last_accel[2];
                        last_accel = event.values.clone();
                        if (event.values[1]<0 && accel[1] > 0.15 && cameraActivity.getReconMode()) {
                            cameraActivity.setMoving(true);
                        }
                        if (Math.abs(accel[1]) > 0.03 && cameraActivity.getMoving()) {
                            velocity[0] += accel[0] * dt * scale;
                            velocity[1] += accel[1] * dt * scale;
                            velocity[2] += accel[2] * dt * scale;
                            shift[0] += velocity[0] * dt * scale;
                            shift[1] += velocity[1] * dt * scale;
                            shift[2] += velocity[2] * dt * scale;
                        }
                        else if(Math.abs(accel[1]) < 0.03 && cameraActivity.getMoving()){
                            cameraActivity.setMoving(false);
                            cameraActivity.setTaking(true);
                            velocity[0] = 0;
                            velocity[1] = 0;
                            velocity[2] = 0;
                            shift[0] = 0;
                            shift[1] = 0;
                            shift[2] = 0;
                        }
                        //Log.d("Accel","X: "+accel[0]+", Y: "+accel[1]+", Z: "+accel[2]);
                        //Log.d("v","X: "+velocity[0]+", Y: "+velocity[1]+", Z: "+velocity[2]);
                        //Log.d("shift","X: "+shift[0]+", Y: "+shift[1]+", Z: "+shift[2]);
                        //Log.d("time",""+dt);
                    }
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    float[] rot= event.values.clone();
                    //Log.d("rot",""+rot[0]+", "+rot[1]+", "+rot[2]+", "+rot[3]+"angle: "+Math.acos(rot[3])*360/Math.PI);


                    break;
                default:
                    return;
            }

        }
    };
    private SensorEventListener rListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ROTATION_VECTOR:
                    if(!init){
                        SensorManager.getRotationMatrixFromVector(rotpred, event.values);
                        init = true;

                    }
                    else{
                        SensorManager.getRotationMatrixFromVector(rot, event.values);
                    }
                    break;
                default:
                    return;
            }
            Log.d("rotation",""+System.currentTimeMillis());
        }
    };

    private SensorEventListener wListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    wData = event.values.clone();
                    float[] wdt = expMat(skew(wData));
                    Mat wMat = new Mat(3, 3, CvType.CV_32F);
                    Mat rMat = new Mat(3, 3, CvType.CV_32F);
                    Mat rpredMat = new Mat(3, 3, CvType.CV_32F);
                    wMat.put(0,0,wdt);
                    rpredMat.put(0,0,rotpred);
                    Core.gemm(wMat, rpredMat, 1, new Mat(), 0, rMat, 0);
                    rMat.get(0,0,rotpred);
                    break;
                default:
                    return;
            }
        }
    };
    private double[] getAngle(float[] gravity){
        double angle[] = new double[3];
        for(int i=0;i<gravity.length;i++){
            double ratio;
            ratio = gravity[i]/SensorManager.GRAVITY_EARTH;
            if(ratio > 1.0)
                ratio = 1.0;
            if(ratio < -1.0)
                ratio = -1.0;
            angle[i] = Math.toDegrees(Math.acos(ratio));
        }
        return angle.clone();
    }

    public float[] skew(float[] w){
        return new float[]{0,-w[2],w[1],w[2],0,-w[0],-w[1],w[0],0};
    }
    public float[] expMat(float[] w){
        float[] v = new float[w.length];
        for (int i=0;i<w.length;i++){
            v[i] =(float) Math.exp(w[i]);
        }
        return v;
    }
    public void Integrate(float[] la){


    }
}
