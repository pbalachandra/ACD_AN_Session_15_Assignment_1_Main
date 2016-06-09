package com.acadgild.balu.acd_an_session_15_assignment_1_main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    EditText editText_URL;
    TextView textView_filePath;
    Button button_downloadImage;
    ProgressBar progressBar;
    ImageView imageView;
    private Bitmap bitmap = null;
    URL mUrl;

    private int mDelay = 300;
    private final static int SET_PROGRESS_BAR_VISIBILITY = 0;
    private final static int PROGRESS_UPDATE = 1;
    private final static int SET_BITMAP = 2;
    private final static int SET_TEXT = 3;

    static class UIHandler extends Handler {
        WeakReference<MainActivity> mParent;

        public UIHandler(WeakReference<MainActivity> parent) {
            mParent = parent;
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity parent = mParent.get();
            if (null != parent) {
                switch (msg.what) {
                    case SET_PROGRESS_BAR_VISIBILITY: {
                        parent.getProgressBar().setVisibility((Integer) msg.obj);
                        break;
                    }
                    case PROGRESS_UPDATE: {
                        parent.getProgressBar().setProgress((Integer) msg.obj);
                        break;
                    }
                    case SET_BITMAP: {
                        parent.getImageView().setImageBitmap((Bitmap) msg.obj);
                        break;
                    }
                    case SET_TEXT: {
                        parent.getTextView().setText((CharSequence) msg.obj);
                        break;
                    }
                }
            }
        }
    }

    public ImageView getImageView() {
        return imageView;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public TextView getTextView() {
        return textView_filePath;
    }

    Handler handler = new UIHandler(new WeakReference<MainActivity>(this));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText_URL = (EditText) findViewById(R.id.editText_URL);
        button_downloadImage = (Button) findViewById(R.id.button_downloadImage);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        imageView = (ImageView) findViewById(R.id.imageView);
        textView_filePath = (TextView) findViewById(R.id.textView_filePath);

        button_downloadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mUrl = new URL(editText_URL.getText().toString());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                new Thread(new DownloadImage(mUrl, handler))
                        .start();
            }
        });
    }

    private class DownloadImage implements Runnable {
        private final URL imageURL;
        private final Handler handler;

        public DownloadImage(URL imageURL, Handler handler) {
            this.imageURL = imageURL;
            this.handler = handler;
        }

        @Override
        public void run() {
            Message msg = handler.obtainMessage(SET_PROGRESS_BAR_VISIBILITY, ProgressBar.VISIBLE);
            handler.sendMessage(msg);

            File file = null;
            try {
                bitmap = showImage(imageURL);
                String filename = Environment.getExternalStorageDirectory().toString();
                file = new File(filename, "newImage.png");
                file.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);

            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int i = 1; i < 11; i++) {
                sleep();
                msg = handler.obtainMessage(PROGRESS_UPDATE, i * 10);
                handler.sendMessage(msg);
            }

            msg = handler.obtainMessage(SET_BITMAP, bitmap);
            handler.sendMessage(msg);

            msg = handler.obtainMessage(SET_TEXT,
                    String.format(getResources().getString(R.string.file_path), file.toString()));
            handler.sendMessage(msg);

            msg = handler.obtainMessage(SET_PROGRESS_BAR_VISIBILITY, ProgressBar.INVISIBLE);
            handler.sendMessage(msg);
        }

        private void sleep() {
            try {
                Thread.sleep(mDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    static private Bitmap showImage(URL url) throws IOException
    {
        HttpUriRequest request = new HttpGet(url.toString());
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(request);

        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            byte[] bytes = EntityUtils.toByteArray(entity);

            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            return bitmap;
        }
        else
        {
            throw new IOException(statusCode + " - " + statusLine.getReasonPhrase());
        }
    }
}
