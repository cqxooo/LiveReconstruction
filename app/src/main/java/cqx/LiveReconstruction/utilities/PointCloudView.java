package cqx.LiveReconstruction.utilities;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

public class PointCloudView extends GLSurfaceView {
    private PointCloudRenderer pointCloudRenderer;
    public PointCloudView(Context context){
        super(context);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        this.setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        pointCloudRenderer = new PointCloudRenderer(context);
    }

}
