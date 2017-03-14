package cqx.LiveReconstruction.fragments;

import android.app.Fragment;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.opencv.core.Mat;

import java.util.ArrayList;

import cqx.LiveReconstruction.R;
import cqx.LiveReconstruction.utilities.Calibration;
import cqx.LiveReconstruction.utilities.ImgData;
import cqx.LiveReconstruction.utilities.PointCloudRenderer;
import cqx.LiveReconstruction.utilities.Reconstruction;


public class ReconFragment extends Fragment {
    private static final String TAG = "Reconstruction";
    private GLSurfaceView mSurfaceView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View reconLayout = inflater.inflate(R.layout.reconstruction, container, false);
        mSurfaceView = (GLSurfaceView)reconLayout.findViewById(R.id.recon_layout);
        Bundle bundle = getArguments();
        ArrayList<Uri> uriList = bundle.getParcelableArrayList("uriList");
        Calibration calib = new Calibration(uriList, getActivity());
        double K[] = calib.computeK();
        Log.d(TAG,""+K[0]+" "+K[1]+" "+K[2]+" "+K[3]);
        //final Reconstruction recon = new Reconstruction(imgList, K);
        //Mat pointCloud = recon.InitPointCloud();
        //mSurfaceView.setRenderer(new PointCloudRenderer(pointCloud));
        return reconLayout;
    }
    @Override
    public void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

}
