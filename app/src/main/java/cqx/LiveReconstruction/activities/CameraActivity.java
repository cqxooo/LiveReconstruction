package cqx.LiveReconstruction.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import cqx.LiveReconstruction.R;

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
    private Mat mRgba, mBgr;
    private String TAG = "MyCameraView";
    private ArrayList<Uri> uriList = new ArrayList<>();
    public void onResume()
    {
        super.onResume();
        mCamera.enableView();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera);
        mCamera = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mCamera.setCvCameraViewListener(this);
        mCamera.enableView();
        mCamera.setOnClickListener(takephoto);
        Intent intent = getIntent();
        uriList = intent.getParcelableArrayListExtra("uriList");
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
    public void onDestroy() {
        super.onDestroy();
        if(mCamera!=null){
            mCamera.disableView();
        }
    }
    @Override
    public void onPause() {
        if (mCamera != null) {
            mCamera.disableView();
        }
        super.onPause();
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mBgr = new Mat(height, width, CvType.CV_8UC3);
    }
    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mBgr.release();
    }
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        return mRgba;
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra("uriList",uriList);
        setResult(1000,intent);
        super.onBackPressed();
    }



}
