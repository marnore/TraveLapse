package lt.marius.travelapse.utils;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;

public class CameraStuff {



    public interface BitmapCallback {
		void onBitmap(Bitmap bmp);
	}
	
	private Camera mCamera;
	private boolean cameraActive;
	private CameraPreview mPreview;
	private Context context;
	private int cameraIndex;

	private View.OnClickListener clickListener;

    public interface CameraStuffCallback {
        void onInitialized(CameraStuff cameraStuff);
    }

	public CameraStuff(Context context, int index) {
		this.context = context;
		cameraIndex = index;
	}

	public void startPreview(final CameraStuffCallback callback, boolean visible) {

		safeCameraOpen(cameraIndex);
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		mPreview = new CameraPreview(context, mCamera, true, windowManager);
        if (callback != null) {
            mPreview.setPreviewCallback(new CameraPreview.CameraPreviewCallback() {
                @Override
                public void onInitialized() {
                    callback.onInitialized(CameraStuff.this);
                }
            });
        }


		int previewWidth = UIUtils.dpToPx(100, context);
		int previewHeight = UIUtils.dpToPx(75, context);

		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		WindowManager.LayoutParams pp = new WindowManager.LayoutParams(previewWidth, previewHeight,
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				/*WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH*/0, PixelFormat.OPAQUE);
		pp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        pp.verticalMargin = 0.05f;


		System.out.println("Adding preview to the window manager");
		wm.addView(mPreview, pp);
		mPreview.setZOrderOnTop(true);

        if (!visible) {
//        mPreview.getHolder().setFormat(PixelFormat.TRANSLUCENT);
//        mPreview.setBackgroundColor(0);
//        mPreview.setAlpha(0);
            mPreview.setX(450);
        }
		cameraActive = true;
	}

	public void setOnClickListener(View.OnClickListener l) {
		mPreview.setOnClickListener(l);
	}

	public void stopPreview() {
        releaseCamera();
		if (mPreview != null) {
			WindowManager wm = (WindowManager) mPreview.getContext().getSystemService(Context.WINDOW_SERVICE);
			wm.removeViewImmediate(mPreview);
            mPreview = null;
		}
	}

	public void focusCamera() {
		if (mCamera != null) mCamera.autoFocus(null);
	}
	
	private BitmapCallback pendingCallback = null;
    private Integer lastWidth;
    private Integer lastHeight;

	public void takePicture(final BitmapCallback callback, final Integer width, final Integer height) {
		if (mCamera == null) callback.onBitmap(null);
		pendingCallback = callback;
		mCamera.autoFocus(new AutoFocusCallback() {
			
			@Override
			public void onAutoFocus(boolean success, Camera camera) {
                lastHeight = height;
                lastWidth = width;
				mCamera.takePicture(null, pictureTakenCallback, pictureTakenCallback);
			}
		});
	}
	
//	private ExecutorService exec = Executors.newSingleThreadExecutor();
	Thread tt = null;
	private int[] tempFrame = null;
	private int previewWidth, previewHeight;
	
	public void startCapture(final BitmapCallback callback) {
		Parameters parameters = mCamera.getParameters();
		for (Size sz : parameters.getSupportedPreviewSizes()){
			System.out.println(sz.width + "x" + sz.height);
		}
		System.out.println("Current " + parameters.getPreviewSize().width + "x" + parameters.getPreviewSize().height);
		Size s = parameters.getSupportedPreviewSizes().get(1);
		previewWidth = s.width;
		previewHeight = s.height;
		tempFrame = new int[previewHeight * previewWidth];
		parameters.setPreviewSize(previewWidth, previewHeight);
		parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		mCamera.stopPreview();
		mCamera.startPreview();
		
		mCamera.setPreviewCallback(new PreviewCallback() {
			private long time = -1;
			@Override
			public void onPreviewFrame(final byte[] data, Camera camera) {
				if (tt == null || !tt.isAlive()) {
					tt = new Thread() {
						public void run() {
							time = System.currentTimeMillis();
							final Bitmap b = callbackData(decodeYUV420SP(tempFrame, data, previewWidth, previewHeight));
							callback.onBitmap(b);
							System.out.println(1.0 / ((System.currentTimeMillis() - time) / 1000.d) + " FPS");
							time = System.currentTimeMillis();
							tt = null;
						}
					};
					tt.start();
				}
			}
		});
	}


	public void stopCapture() {
		if (mCamera != null) {
			mCamera.stopPreview();
		}
	}


	static public int[] decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
	    final int frameSize = width * height;

	    for (int j = 0, yp = 0; j < height; j++) {
	        int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
	        for (int i = 0; i < width; i++, yp++) {
	            int y = (0xff & ((int) yuv420sp[yp])) - 16;
	            if (y < 0) y = 0;
	            if ((i & 1) == 0) {
	                v = (0xff & yuv420sp[uvp++]) - 128;
	                u = (0xff & yuv420sp[uvp++]) - 128;
	            }
	            int y1192 = 1192 * y;
	            int r = (y1192 + 1634 * v);
	            int g = (y1192 - 833 * v - 400 * u);
	            int b = (y1192 + 2066 * u);

	            if (r < 0) r = 0; else if (r > 262143) r = 262143;
	            if (g < 0) g = 0; else if (g > 262143) g = 262143;
	            if (b < 0) b = 0; else if (b > 262143) b = 262143;

	            rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
//	            rgb[yp * 4 + 0] = (byte) 0xff;
//	            rgb[yp * 4 + 1] = (byte) (((r << 6) & 0xff0000) >> 4);
//	            rgb[yp * 4 + 2] = (byte) (((g >> 2) & 0xff00) >> 2);
//	            rgb[yp * 4 + 3] = (byte) ((b >> 10) & 0xff);
	        }
	    }
	    return rgb;
	}

    private PictureCallback pictureTakenCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            if (data == null || pendingCallback == null) {
                System.err.println("picture Data is null");
            } else {
                new Thread() {
                    public void run() {
//						pendingCallback.onBitmap(callbackData(data));
                        Options opts = new Options();
                        //opts.inSampleSize = 2;
                        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, opts);

                        if (mPreview.getRotationDegrees() != 0) {
                            Matrix m = new Matrix();
                            m.preRotate(mPreview.getRotationDegrees());
                            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                        }

                        if (lastHeight != null && lastWidth != null) {
                            int w, h;
                            int dw = bmp.getWidth() - lastWidth;
                            int dh = bmp.getHeight() - lastHeight;

                            if (dw < 0 && dh < 0) {
                                if (dw < dh) {
                                    w = bmp.getWidth();
                                    h = (int)((double)(w * lastHeight) / (double)lastWidth);
                                } else {
                                    h = bmp.getHeight();
                                    w = (int)((double)(h * lastWidth) / (double)lastHeight);
                                }
                            } else if (dw < 0) {
                                w = bmp.getWidth();
                                h = (int)((double)(w * lastHeight) / (double)lastWidth);
                            } else if (dh < 0) {
                                h = bmp.getHeight();
                                w = (int)((double)(h * lastWidth) / (double)lastHeight);
                            } else {
                                if (dw < dh) {
                                    w = bmp.getWidth();
                                    h = (int)((double)(w * lastHeight) / (double)lastWidth);
                                } else {
                                    h = bmp.getHeight();
                                    w = (int)((double)(h * lastWidth) / (double)lastHeight);
                                }
                            }



                            int top = Math.max((bmp.getHeight() - h) / 2, 0);
                            int left = Math.max((bmp.getWidth() - w) / 2, 0);
                            bmp = Bitmap.createBitmap(bmp, left, top, w, h);

                            if (w != lastWidth || h != lastHeight) {
                                bmp = Bitmap.createScaledBitmap(bmp, lastWidth, lastHeight, true);
                            }
                        }

                        pendingCallback.onBitmap(bmp);
                        System.out.println("Picture taken successfully");
                        pendingCallback = null;
                    }
                }.start();

            }
            mCamera.startPreview();
        }
    };
	
	private Bitmap callbackData(int[] data) {
		int rotation = mPreview.getRotationDegrees();
		Options opts = new Options();
		opts.inSampleSize = 4;
		Bitmap bmp = Bitmap.createBitmap(data, previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
//		Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
		if (bmp == null) {
			System.err.println("cannot decode input bitmap");
			return null;
		}
//		if (true) return bmp;
		System.out.println(bmp.getWidth() + " " + bmp.getHeight());
		Matrix m = new Matrix();
		m.preRotate(rotation);
		
		bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);

		return bmp;
	}
	
	private boolean safeCameraOpen(int id) {
	    try {
	        releaseCamera();
	        mCamera = Camera.open(id);
	        return true;
	    } catch (Exception e) {
	        Log.e("error", "failed to open Camera");
	        e.printStackTrace();
	    }

	    return false;
	}

	public void releaseCamera() {
	    if (mCamera != null) {
            mCamera.stopPreview();
	        mCamera.release();
	        mCamera = null;
	    }
	}

	public void onResume() {
		// TODO Auto-generated method stub
		if (!cameraActive) {
			mCamera.startPreview();
		}
		cameraActive = true;
	}
	
	public void onPause() {
		if (cameraActive && mCamera != null) {
			mCamera.stopPreview();
		}
		cameraActive = false;
	}
	
    public void onDestroy() {
    	UIUtils.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if (mPreview != null) {
					WindowManager wm = (WindowManager) mPreview.getContext().getSystemService(Context.WINDOW_SERVICE);
					wm.removeViewImmediate(mPreview);
				} else {
					System.err.println("MPreview is null");
				}
				
			}
		});
    	
    }

//	public void resume() {
//		mCamera.startPreview();
//	}
//	
//	public void pause() {
//		mCamera.stopPreview();
//	}
	
}
