package cqx.LiveReconstruction.fragments;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import cqx.LiveReconstruction.R;

public class CameraCalibration extends Fragment {
    private static final String TAG = "CameraCalibration";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View calibLayout = inflater.inflate(R.layout.calibration, container, false);
        Bundle bundle = getArguments();
        ArrayList<Uri> uriList = bundle.getParcelableArrayList("uriList");
        for (int i=0;i<uriList.size();i++){
            Log.d(TAG,""+uriList.get(i));
        }
        return calibLayout;

    }
}
