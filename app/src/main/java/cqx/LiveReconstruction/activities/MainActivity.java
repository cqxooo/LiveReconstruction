package cqx.LiveReconstruction.activities;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import cqx.LiveReconstruction.R;
import cqx.LiveReconstruction.fragments.ShowCorrespondences;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Reconstruction";
    private FragmentManager fragmentManager;
    private View corrLayout;
    private View cameraLayout;
    private ShowCorrespondences showCorrespondences;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //sd = new SensorData(sm, this);
        initViews();
        fragmentManager = getFragmentManager();
        setTabSelection(0);

    }
    private void initViews() {
        corrLayout = findViewById(R.id.corr_layout);
        cameraLayout = findViewById(R.id.camera_layout);
        corrLayout.setOnClickListener(this);
        cameraLayout.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.corr_layout:
                setTabSelection(0);
                break;
            case R.id.camera_layout:
                setTabSelection(1);
                break;
            default:
                break;
        }
    }
    private void setTabSelection(int index){
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        switch (index){
            case 0:
                if (showCorrespondences == null) {
                    showCorrespondences = new ShowCorrespondences();
                    transaction.replace(R.id.content, showCorrespondences);
                    //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                else{
                    transaction.show(showCorrespondences);
                }
                break;
            case 1:
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
        transaction.commit();
    }


    @Override
    protected void onPause() {
        super.onPause();
        //sd.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        //sd.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        View bar = findViewById(R.id.functions);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            bar.setVisibility(View.GONE);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(attrs);
            bar.setVisibility(View.VISIBLE);

        }
    }
}
