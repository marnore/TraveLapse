package lt.marius.travelapse.utils;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

public class CameraView extends ViewGroup implements SurfaceHolder.Callback {

	public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CameraView(Context context) {
		super(context);
		init(context);
	}

	SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    private Camera mCamera;
	private CameraStuff mPreview;
	private List<Size> mSupportedPreviewSizes;
	private Size mPreviewSize;

    
    private void init(Context context) {
    	 mSurfaceView = new SurfaceView(context);
         addView(mSurfaceView);

         // Install a SurfaceHolder.Callback so we get notified when the
         // underlying surface is created and destroyed.
         mHolder = mSurfaceView.getHolder();
         mHolder.addCallback(this); 
    }
    
    public void setCamera(Camera camera) {
	    if (mCamera == camera) { return; }
	    
	    stopPreviewAndFreeCamera();
	    
	    mCamera = camera;
	    
	    if (mCamera != null) {
	        List<Size> localSizes = mCamera.getParameters().getSupportedPreviewSizes();
	        mSupportedPreviewSizes = localSizes;
	        mPreviewSize = localSizes.get(0);
	        requestLayout();
	      
	        try {
	            mCamera.setPreviewDisplay(mHolder);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	      
	        // Important: Call startPreview() to start updating the preview
	        // surface. Preview must be started before you can take a picture.
	        mCamera.startPreview();
	    }
	}

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();
        mCamera.setParameters(parameters);

        // Important: Call startPreview() to start updating the preview surface.
        // Preview must be started before you can take a picture.
        mCamera.startPreview();
    }

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
	    // Surface will be destroyed when we return, so stop the preview.
	    if (mCamera != null) {
	        // Call stopPreview() to stop updating the preview surface.
	        mCamera.stopPreview();
	    }
	}

	/**
	 * When this function returns, mCamera will be null.
	 */
	private void stopPreviewAndFreeCamera() {

	    if (mCamera != null) {
	        // Call stopPreview() to stop updating the preview surface.
	        mCamera.stopPreview();
	    
	        // Important: Call release() to release the camera for use by other
	        // applications. Applications should release the camera immediately
	        // during onPause() and re-open() it during onResume()).
	        mCamera.release();
	    
	        mCamera = null;
	    }
	}

	@Override
	protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
		// TODO Auto-generated method stub
		
	}
	
}
