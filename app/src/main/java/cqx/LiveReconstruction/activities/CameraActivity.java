package cqx.LiveReconstruction.activities;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import cqx.LiveReconstruction.R;
import cqx.LiveReconstruction.utilities.PointCloudView;

import android.app.Activity;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


import static org.opencv.imgcodecs.Imgcodecs.imwrite;

public class CameraActivity extends Activity implements CvCameraViewListener2{
    public static float[] gravity = new float[3];
    public static float[] linear_acceleration = new float[3];
    private long time_prev, time_curr;
    private float[] aData;
    private SensorManager sm;
    private Sensor aSensor;
    private CameraBridgeViewBase mCamera;
    private RelativeLayout renderView;
    private PointCloudView pointCloudView;
    private Mat mRgba, mBgr, curr, prev;
    private boolean first = true;
    private String TAG = "MyCameraView";
    private ArrayList<Uri> uriList = new ArrayList<>();
     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera);
        initView();
        Intent intent = getIntent();
        uriList = intent.getParcelableArrayListExtra("uriList");
    }
    private void initView(){
        renderView = (RelativeLayout)findViewById(R.id.render_view);
        mCamera = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mCamera.setCvCameraViewListener(this);
        mCamera.enableView();
        mCamera.setOnClickListener(takephoto);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sm.registerListener(aListener, aSensor, SensorManager.SENSOR_DELAY_GAME);
    }
    private View.OnClickListener takephoto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
                Log.e(TAG, "Error writing to file");
            }
            final Date date = new Date(System.currentTimeMillis());
            final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            final String appName = getString(R.string.app_name);
            final String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            final String albumPath = galleryPath + File.separator + appName;
            final String photoPath = albumPath + File.separator + format.format(date) + ".jpg";
            final ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, photoPath);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put(MediaStore.Images.Media.TITLE, appName);
            values.put(MediaStore.Images.Media.DESCRIPTION, appName);
            values.put(MediaStore.Images.Media.DATE_TAKEN, format.format(date));
            File album = new File(albumPath);
            if (!album.isDirectory() && !album.mkdirs()) {
                Log.e(TAG, "Failed to create album directory at " + albumPath);
                onTakePhotoFailed();
                return;
            }
            Imgproc.cvtColor(mRgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
            if (!imwrite(photoPath, mBgr)) {
                Log.e(TAG, "Failed to save photo to " + photoPath);
                onTakePhotoFailed();
            }
            Log.d(TAG, "Photo saved successfully to " + photoPath);
            try {
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                uriList.add(uri);
                //Log.d("uriTest",""+uri);
            } catch (final Exception e) {
                Log.e(TAG, "Failed to insert photo into MediaStore");
                e.printStackTrace();
                File photo = new File(photoPath);
                if (!photo.delete()) {
                    Log.e(TAG, "Failed to delete non-inserted photo");
                }
                onTakePhotoFailed();
                return;
            }
        }
    };
    private void onTakePhotoFailed() {
        final String errorMessage = getString(R.string.photo_error_message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onResume(){
        super.onResume();
        mCamera.enableView();
        sm.registerListener(aListener, aSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mCamera!=null){
            mCamera.disableView();
        }
        sm.unregisterListener(aListener);
    }
    @Override
    public void onPause() {
        if (mCamera != null) {
            mCamera.disableView();
        }
        sm.unregisterListener(aListener);
        super.onPause();
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mBgr = new Mat(height, width, CvType.CV_8UC3);
        curr = new Mat(height, width, CvType.CV_8UC4);
        prev = new Mat(height, width, CvType.CV_8UC4);
    }
    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mBgr.release();
    }
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mRgba.copyTo(curr);
        if(first){
            mRgba.copyTo(prev);
            first = false;
        }
        if(!first && aData[1]<0 && Math.abs(aData[1])>0.1){
            final Date date = new Date(System.currentTimeMillis());
            final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            final String appName = getString(R.string.app_name);
            final String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            final String albumPath = galleryPath + File.separator + appName;
            final String photoPath = albumPath + File.separator +  format.format(date)+".jpg";
            final ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, photoPath);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put(MediaStore.Images.Media.TITLE, appName);
            values.put(MediaStore.Images.Media.DESCRIPTION, appName);
            File album = new File(albumPath);
            if (!album.isDirectory() && !album.mkdirs()) {
                Log.e(TAG, "Failed to create album directory at " + albumPath);
                onTakePhotoFailed();

            }
            Imgproc.cvtColor(curr, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
            if (!imwrite(photoPath, mBgr)) {
                Log.e(TAG, "Failed to save photo to " + photoPath);
                onTakePhotoFailed();
            }
            Log.d(TAG, "Photo saved successfully to " + photoPath);
            try {
                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } catch (final Exception e) {
                Log.e(TAG, "Failed to insert photo into MediaStore");
                e.printStackTrace();
                File photo = new File(photoPath);
                if (!photo.delete()) {
                    Log.e(TAG, "Failed to delete non-inserted photo");
                }
                onTakePhotoFailed();
            }

        }

        return mRgba;
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra("uriList",uriList);
        setResult(1000,intent);
        super.onBackPressed();
    }
    private SensorEventListener aListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    aData = event.values.clone();
                    final float alpha = 0.8f;
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                    linear_acceleration[0] = event.values[0] - gravity[0];
                    linear_acceleration[1] = event.values[1] - gravity[1];
                    linear_acceleration[2] = event.values[2] - gravity[2];

                    double angle[] = getAngle(gravity);
                    Log.d("Angle","X: "+angle[0]+", Y: "+angle[1]+", Z: "+angle[2]);
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
}
