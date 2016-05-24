package lt.marius.travelapse;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import lt.marius.travelapse.utils.CameraStuff;
import lt.marius.travelapse.utils.UIUtils;

public class CaptureService extends Service implements  Runnable{

    public static final String SAVE_DIR = "Captures";

    private static boolean running = false;

    public CaptureService() {
    }

    public static File getCaptureDir() {
        return new File(Environment.getExternalStorageDirectory(), SAVE_DIR);
    }


    public class CaptureServiceBinder extends Binder {
        CaptureService getService() {
            return CaptureService.this;
        }
    }

    public interface StatusListener {
        void onPicCountIncreased(int newCount);
    }

    private Notification notification;

    private CameraStuff cameraStuff;
    private PicsHandler picsHandler;

    private LocationProvider locationProvider;
    private int pictureTakingRate = 5000;

    private int takenPictures;

    private StatusListener listener;

    private File saveDirectory;

    private boolean noPreview = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notification = buildForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new CaptureServiceBinder();
    }

    public void stop() {
        picsHandler.sendEmptyMessage(PicsHandler.STOP);
        cameraStuff.stopPreview();
        running = false;
        locationProvider.stopListening();
        stopSelf(); //bye bye service
    }

    public void setNoPreview(boolean noPreview) {
        this.noPreview = noPreview;
    }

    public void start() {
        start(pictureTakingRate, 1920, 1080);
    }

    public void start(final int pictureTakingRate, final int width, final int height) {
        cameraStuff = new CameraStuff(this, 0);

        picsHandler = new PicsHandler(cameraStuff, callback, width, height);
        picsHandler.setNoPreview(noPreview);
        takenPictures = 0;

        locationProvider = new LocationProvider(this, null);

        //create dir
        File dir = new File(Environment.getExternalStorageDirectory(), SAVE_DIR);
        saveDirectory = new File(dir, sdf.format(new Date()));
        saveDirectory.mkdirs();

        locationProvider.startListening(pictureTakingRate);
        cameraStuff.startPreview(new CameraStuff.CameraStuffCallback() {
            @Override
            public void onInitialized(CameraStuff cameraStuff) {
                cameraStuff.setOnClickListener(previewClickListener);
                running = true;
                cameraStuff.takePicture(callback, width, height);
                picsHandler.setRate(pictureTakingRate);
                picsHandler.sendEmptyMessageDelayed(PicsHandler.START, pictureTakingRate);
            }
        }, !noPreview);
        //cameraStuff.setOnClickListener(previewClickListener);
    }


    public void setRate(int rateInMillis) {
        pictureTakingRate = rateInMillis;
        locationProvider.setRefreshRate(pictureTakingRate);
        picsHandler.setRate(pictureTakingRate);
    }

    private View.OnClickListener previewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(CaptureService.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    };

    private static final int NOTIFICATION_ID = 121584611;
    private Notification buildForegroundNotification() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);

        b.setOngoing(true);
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        PendingIntent intent = PendingIntent.getActivity(this, 0, i, 0);

        b.setContentTitle("Taking pictures")
                .setContentText("Press to go to the activity")
                .setSmallIcon(android.R.drawable.btn_radio)
                .setContentIntent(intent)
                .setTicker("Taking pictures");

        return b.build();
    }

    @Override
    public void run() {

    }


    public static boolean isRunning() {
        return running;
    }


    public void setStatusListener(StatusListener l) {
        listener = l;
    }

    static class PicsHandler extends Handler {

        public static final int START = 0;
        public static final int STOP = 1;

        private int rate = 5000;
        private CameraStuff cs;
        private CameraStuff.BitmapCallback callback;
        private int width, height;
        private boolean noPreview = false;

        PicsHandler(CameraStuff cs, CameraStuff.BitmapCallback callback, int width, int height) {
            this.cs = cs;
            this.callback = callback;
            this.width = width;
            this.height = height;
        }

        void setRate(int rate) {
            this.rate = rate;
        }

        void setNoPreview(boolean noPreview) {
            this.noPreview = noPreview;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == START) {

                if (noPreview) {
                    cs.startPreview(new CameraStuff.CameraStuffCallback() {
                        @Override
                        public void onInitialized(CameraStuff cameraStuff) {
                            cs.takePicture(callback, width, height);
                            PicsHandler.this.sendEmptyMessageDelayed(0, rate);
                        }
                    }, !noPreview);
                } else {
                    cs.takePicture(callback, width, height);
                    this.sendEmptyMessageDelayed(0, rate);
                }
            } else if (msg.what == STOP) {
                this.removeMessages(START);
            }
        }


    }

    private CameraStuff.BitmapCallback callback = new CameraStuff.BitmapCallback() {
        @Override
        public void onBitmap(Bitmap bmp) {
            File saved = saveBitmap(bmp);
            if (noPreview) {
                UIUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cameraStuff.stopPreview();
                    }
                });
            }
            if (saved != null) {
                takenPictures++;
                if (listener != null) {
                    UIUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onPicCountIncreased(takenPictures);
                        }
                    });
                }
                Location loc = locationProvider.getCurrentBest();
                if (loc != null) {
                    try {
                        writeGPSInfoToJpeg(saved, loc);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    //http://stackoverflow.com/questions/11644873/android-write-exif-gps-latitude-and-longitude-onto-jpeg-failed
    private void writeGPSInfoToJpeg(File file, Location location) throws IOException {
        ExifInterface exif = new ExifInterface(file.getPath());
        //String latitudeStr = "90/1,12/1,30/1";
        double lat = location.getLatitude();
        double alat = Math.abs(lat);
        String dms = Location.convert(alat, Location.FORMAT_SECONDS);
        String[] splits = dms.split(":");
        String[] secnds = (splits[2]).split("\\.");
        String seconds;
        if (secnds.length == 0) {
            seconds = splits[2];
        } else {
            seconds = secnds[0];
        }

        String latitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitudeStr);

        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, lat > 0 ? "N" : "S");

        double lon = location.getLongitude();
        double alon = Math.abs(lon);


        dms = Location.convert(alon, Location.FORMAT_SECONDS);
        splits = dms.split(":");
        secnds = (splits[2]).split("\\.");

        if (secnds.length == 0) {
            seconds = splits[2];
        } else {
            seconds = secnds[0];
        }
        String longitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";


        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitudeStr);
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lon > 0 ? "E" : "W");

        exif.saveAttributes();

    }

    private Date getLastSavedDate() {
        File dir = new File(Environment.getExternalStorageDirectory(), SAVE_DIR);
        String[] dirs = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return new File(dir, filename).isDirectory();
            }
        });
        if (dirs.length == 0) return null;
        Arrays.sort(dirs);
        String last = dirs[dirs.length - 1];
        throw new UnsupportedOperationException();
        //return new Date();
    }

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.US);
    private File saveBitmap(Bitmap bmp) {
        if (bmp == null) return null;
        String path = saveDirectory.getPath();
        OutputStream fOutputStream = null;
        File file = new File(path, String.format("capture%04d.jpg", (takenPictures + 1)));

        try {
            if (!file.exists()) {
                File dir = file.getParentFile();
                if (!dir.exists() && !dir.mkdirs() ) {
                    throw new IOException("Cannot create dirs " + dir.getPath());
                };
            }
            fOutputStream = new FileOutputStream(file);

            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOutputStream);

            fOutputStream.flush();
            fOutputStream.close();
            //makes another image...
            //MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), "TraveLapse photo");

            return file;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



}
