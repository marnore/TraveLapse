package lt.marius.travelapse;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;

import lt.marius.travelapse.utils.UIUtils;

/**
 * Created by marius-pc on 9/7/15.
 */
public class MovieRenderer {

    private FFmpeg ffmpeg;
    private boolean loaded = false;

    private Handler handler;

    private RendererCallback listener;

    private static final int LOAD = 1;
    private static final int RENDER = 2;

    interface RendererCallback {
        void onOutput(String outputLine);
        void onStart();
        void onFinish();
    }

    public MovieRenderer(Context c, RendererCallback listener) {
        ffmpeg = FFmpeg.getInstance(c);
        load(); //lets just hope it loads in time
        this.listener = listener;
    }

    private void load() {

        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onFailure() {}

                @Override
                public void onSuccess() {
                    loaded = true;
                }

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
            e.printStackTrace();
        }
    }

    public void render(File dir, int fps, File movieName) throws FFmpegCommandAlreadyRunningException {

            String command;

            command = "-f image2 -framerate " + fps + " -start_number 1 -i " +
                    dir.getAbsolutePath() + "/capture%4d.jpg " + movieName.getPath();
            //command = "-version";
            ffmpeg.execute(command, new FFmpegExecuteResponseHandler() {
                @Override
                public void onSuccess(String s) {
                    if (listener != null) listener.onOutput(s);
                }

                @Override
                public void onProgress(final String s) {
                    UIUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) listener.onOutput(s);
                        }
                    });
                }

                @Override
                public void onFailure(final String s) {
                    UIUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) listener.onOutput(s);
                        }
                    });
                }

                @Override
                public void onStart() {
                    if (listener != null) listener.onStart();
                }

                @Override
                public void onFinish() {
                    if (listener != null) listener.onFinish();
                }
            });

    }
}
