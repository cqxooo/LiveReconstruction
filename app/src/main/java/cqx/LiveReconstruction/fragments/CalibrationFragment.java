package cqx.LiveReconstruction.fragments;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.opencv.core.Mat;

import java.util.ArrayList;

import cqx.LiveReconstruction.R;
import cqx.LiveReconstruction.utilities.Calibration;

public class CalibrationFragment extends Fragment {
    private static final String TAG = "CameraCalibration";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View calibLayout = inflater.inflate(R.layout.calibration, container, false);
        Bundle bundle = getArguments();
        ArrayList<Uri> uriList = bundle.getParcelableArrayList("uriList");
        Calibration calib = new Calibration(uriList, getActivity());
        ArrayList<Mat> fm = calib.computeK();
        String info = "";
        for (int i=0;i<fm.size();i++){
            double[] test = new double[9];
            fm.get(i).get(0,0,test);
            info = info + "FM"+i+":"+test[0]+","+ test[1]+","+ test[2]+","+ test[3]+","+ test[4]+","+ test[5]+","+ test[6]+","+ test[7]+","+ test[8]+"\n\n";
        }
        EditText output = (EditText)calibLayout.findViewById(R.id.editText);
        output.setText(info);
        return calibLayout;

    }
}
