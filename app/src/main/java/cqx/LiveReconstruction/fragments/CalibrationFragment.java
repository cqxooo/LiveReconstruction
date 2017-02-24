package cqx.LiveReconstruction.fragments;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
        String info = "";
        ArrayList<double[]> K = calib.computeKs();
        for (int i=0;i<K.size();i++){
            double[] test = K.get(i);
           info = info + "K"+i+":"+test[0]+","+ test[1]+","+ test[2]+","+ test[3]+"\n\n";
        }
        EditText output = (EditText)calibLayout.findViewById(R.id.editText);
        output.setText(info);
        return calibLayout;

    }
}
