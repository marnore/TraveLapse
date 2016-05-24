package lt.marius.travelapse;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import lt.marius.travelapse.utils.CameraStuff;
import lt.marius.travelapse.utils.UIUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private ImageView iv;
    private TextView textView;
    private Spinner resolutionSpinner;
    private ProgressBar progress;
    private ScrollView scrollView;
    private CheckBox showPreviewCheckBox;

    public MainActivityFragment() {
    }

    static class ResolutionItem {
        ResolutionItem(String text, int w, int h) {
            this.text = text;
            width = w;
            height = h;
        }

        String text;
        int width;
        int height;

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getText() {
            return text;
        }
    }

    private List<ResolutionItem> resolutions;
    private ResolutionItem selectedResolution;
    private MovieRenderer renderer;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.fragment_main, container, false);

        scrollView = (ScrollView) vg.findViewById(R.id.scroll_view);
        iv = (ImageView) vg.findViewById(R.id.image_preview);
        textView = (TextView) vg.findViewById(R.id.text_info);

        vg.findViewById(R.id.button_start).setOnClickListener(startClick);
        vg.findViewById(R.id.button_stop).setOnClickListener(stopClick);

        vg.findViewById(R.id.button3).setOnClickListener(renderClick);

        resolutionSpinner = (Spinner) vg.findViewById(R.id.resolution_spinner);

        progress = (ProgressBar) vg.findViewById(R.id.progressBar);

        resolutions = new ArrayList<>();
        resolutions.add(new ResolutionItem("1080p", 1920, 1080));
        resolutions.add(new ResolutionItem("720p", 1280, 720));

        selectedResolution = resolutions.get(0);

        resolutionSpinner.setAdapter(new SpinnerAdapter(getContext(), resolutions));
        resolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedResolution = (ResolutionItem) parent.getAdapter().getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        showPreviewCheckBox = (CheckBox) vg.findViewById(R.id.checkBox);

        renderer = new MovieRenderer(getContext(), rendererCallback);

        return vg;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (CaptureService.isRunning()) {
            Intent serviceIntent = new Intent(context, CaptureService.class);
            context.bindService(serviceIntent, mConnection, Activity.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onDetach() {
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
        super.onDetach();
    }

    static class SpinnerAdapter extends BaseAdapter {

        private List<ResolutionItem> resItems;
        private Context context;

        public SpinnerAdapter(Context c, List<ResolutionItem> resolutionItems) {
            resItems = resolutionItems;
            context = c;
        }

        public Context getContext() {
            return context;
        }

        @Override
        public int getCount() {
            return resItems.size();
        }

        @Override
        public ResolutionItem getItem(int position) {
            return resItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_resolution, parent, false);

            }
            ViewGroup vg = (ViewGroup) convertView;
            TextView tv = (TextView)vg.findViewById(R.id.resolution_item_text);
            tv.setText(getItem(position).getText());

            tv = (TextView)vg.findViewById(R.id.resolution_item_subtitle);
            tv.setText(getItem(position).getWidth() + " x " + getItem(position).getHeight());

            return convertView;
        }
    }

    private CaptureService service;
    private boolean mBound;
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {

            service = ((CaptureService.CaptureServiceBinder) binder).getService();
            mBound = true;
            checkStartCapture();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void checkStartCapture() {
        if (!CaptureService.isRunning()) {
            textView.setText("");
            service.setNoPreview(!showPreviewCheckBox.isChecked());
            service.start(5000, selectedResolution.getWidth(), selectedResolution.getHeight());
            service.setStatusListener(statusListener);
        } else {
            service.setStatusListener(statusListener);
        }
    }

    private CaptureService.StatusListener statusListener = new CaptureService.StatusListener() {
        @Override
        public void onPicCountIncreased(int newCount) {
            textView.setText("Pictures taken " + newCount);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent serviceIntent = new Intent(getActivity(), CaptureService.class);
            getActivity().startService(serviceIntent);

            if (!getActivity().bindService(serviceIntent, mConnection, Activity.BIND_AUTO_CREATE)) {
                System.err.println("Cannot bind to service");
            }

        }
    };

    private View.OnClickListener stopClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mBound) {
                service.stop();
                getActivity().stopService(new Intent(getActivity(), CaptureService.class));
                getActivity().unbindService(mConnection);
                mBound = false;
            }
        }
    };

    @Override
    public void onDestroy() {
//        camera.release();
        super.onDestroy();
    }

    private View.OnClickListener renderClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            File dir = CaptureService.getCaptureDir();

            UIUtils.showFilesDialog(getActivity(), dir, new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            }, new UIUtils.FileDialogListener() {
                @Override
                public void onSelected(File file) {
                    File movieFile = new File(file, "movie12.mp4");
                    try {
                        renderer.render(file, 12, movieFile);
                    } catch (FFmpegCommandAlreadyRunningException e) {
                        e.printStackTrace();
                        textView.setText("Movie rendering failed");
                    }
                }
            });


        }
    };

    private MovieRenderer.RendererCallback rendererCallback = new MovieRenderer.RendererCallback() {
        @Override
        public void onOutput(String outputLine) {
            if (textView.getLineCount() > 100) textView.setText("");
            textView.setText(textView.getText() + "\n" + outputLine);
            scrollToBottom();
        }

        @Override
        public void onStart() {
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onFinish() {
            progress.setVisibility(View.GONE);
            textView.setText(textView.getText() + "\n" + "Finished");
            scrollToBottom();
        }

        private void scrollToBottom() {
            scrollView.post(new Runnable() {
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    };

    private void renameFiles() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Captures");
        int count = 0;
        for (File f : dir.listFiles()) {
            if (f.getPath().endsWith(".jpg")) {
                count++;
            }
        }

        int digits = 1;
        while (count > 10) {
            count = count / 10;
            digits++;
        }

        int i = 1;

        for (File f : dir.listFiles()) {
            if (f.getPath().endsWith(".jpg")) {
                f.renameTo(new File(f.getParentFile(), String.format("image%0" + digits + "d.jpg", i)));
                i++;
            }
        }
    }



}
