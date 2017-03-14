package cqx.LiveReconstruction.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import cqx.LiveReconstruction.R;
import cqx.LiveReconstruction.utilities.Calibration;
import cqx.LiveReconstruction.utilities.ImgData;
import cqx.LiveReconstruction.utilities.MatchInfo;
import cqx.LiveReconstruction.utilities.SensorData;

import android.app.Activity;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
    private CameraBridgeViewBase mCamera;
    private SensorData sensorData;
    private Mat mRgba;
    private boolean ReconMode = false;
    private boolean isMoving = false;
    private boolean isTaking = false;
    private int count;
    private double cameraMat[] = new double[4];
    private Calibration calib;
    private ArrayList<MatchInfo> matchList = new ArrayList<>();
    private ArrayList<ImgData> dataList = new ArrayList<>();
    private String TAG = "MyCameraView";
    //private ArrayList<Uri> uriList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera);
        initViews();
        //mCamera.setOnClickListener(takephoto);
        //Intent intent = getIntent();
        //uriList = intent.getParcelableArrayListExtra("uriList");
    }
    private void initViews(){
        mCamera = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mCamera.setCvCameraViewListener(this);
        mCamera.enableView();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorData = new SensorData(sensorManager, CameraActivity.this);
    }

    private View.OnClickListener takephoto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };
    private void saveImage(Mat mRgba){
        Mat mBgr = new Mat(mRgba.height(), mRgba.width(), CvType.CV_8UC3);
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
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            //uriList.add(uri);
            //Log.d("uriTest",""+uri);
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
    public void onResume()
    {
        super.onResume();
        mCamera.enableView();
        sensorData.onResume();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mCamera!=null){
            mCamera.disableView();
        }
        sensorData.onPause();
    }
    @Override
    public void onPause() {
        if (mCamera != null) {
            mCamera.disableView();
        }
        super.onPause();
        sensorData.onPause();
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        calib = new Calibration(width/2,height/2);
    }
    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        if(count==0 && ReconMode){
            //saveImage(mRgba);
            dataList.add(calib.detectFeature(mRgba));
            count++;
        }
        else if(count>0 && ReconMode) {
            if (!isMoving && isTaking) {
                saveImage(mRgba);
                dataList.add(calib.detectFeature(mRgba));
                matchList.add(calib.detectCorrespondence(dataList.get(count - 1), dataList.get(count)));
                if (cameraMat[0] == 0) {
                    cameraMat = calib.computeK(matchList.get(count - 1).getFM());
                    Log.d("calib", "" + cameraMat[0] + ", " + cameraMat[1] + ", " + cameraMat[2] + ", " + cameraMat[3]);
                }
                count++;
                isTaking = false;
            }
        }
        return mRgba;
    }

    /*
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra("uriList",uriList);
        setResult(1000,intent);
        super.onBackPressed();
    }
    */
    public void setReconMode(boolean mode){
        this.ReconMode = mode;
    }
    public void setMoving(boolean isMoving){
        this.isMoving = isMoving;
    }
    public void setTaking(boolean isTaking){
        this.isTaking = isTaking;
    }
    public boolean getMoving(){
        return this.isMoving;
    }
    public boolean getReconMode(){
        return this.ReconMode;
    }


}
