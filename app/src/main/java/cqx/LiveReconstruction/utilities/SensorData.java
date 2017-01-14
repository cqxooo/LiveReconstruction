package cqx.LiveReconstruction.utilities;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import cqx.LiveReconstruction.activities.MainActivity;

public class SensorData {
    private final SensorManager sm;
    private final MainActivity ma;
    private Sensor rSensor;
    private Sensor gSensor;
    private Sensor aSensor;
    private Sensor wSensor;
    private boolean init = false;
    float[] rotpred = new float[9];
    float[] rot = new float[9];
    float[] wData = new float[3];
    public SensorData(SensorManager sm, MainActivity ma){
        this.sm = sm;
        this.ma = ma;
        aSensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        rSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        gSensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        wSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sm.registerListener(rListener, rSensor, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(aListener, aSensor, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(gListener, gSensor, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(wListener, wSensor, SensorManager.SENSOR_DELAY_GAME);
    }
    public void onPause() {
        sm.unregisterListener(rListener);
        sm.unregisterListener(gListener);
        sm.unregisterListener(aListener);
        sm.unregisterListener(wListener);
    }

    public void onResume() {
        sm.registerListener(rListener, rSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(gListener, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(aListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(wListener, wSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
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
    private SensorEventListener gListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] gw = new float[3];
            float[] gData;
            Mat gwMat = new Mat(3, 1, CvType.CV_32F);
            switch (event.sensor.getType()) {
                case Sensor.TYPE_GRAVITY:
                    gData = event.values.clone();
                    break;
                default:
                    return;
            }
            Mat rotMat = new Mat(3, 3, CvType.CV_32F);
            Mat gMat = new Mat(3, 1, CvType.CV_32F);
            rotMat.put(0, 0, rot);
            gMat.put(0, 0, gData);
            Core.gemm(rotMat, gMat, 1, new Mat(), 0, gwMat, 0);
            gwMat.get(0, 0, gw);

        }
    };
    private SensorEventListener aListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] aData;
            switch (event.sensor.getType()) {
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    aData = event.values.clone();
                    break;
                default:
                    return;
            }

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
}
