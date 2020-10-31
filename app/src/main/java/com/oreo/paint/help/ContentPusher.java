package com.oreo.paint.help;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.oreo.paint.Paper;

public class ContentPusher {
    static final String TAG = "-=-=";
    // to local server
    public static final String SERVER_POST_URL = "http://192.168.68.112:3000/send";

    Paper paper;
    Canvas canvas;
    Bitmap bitmap;

    HttpURLConnection urlConnection;

    static final int SIGCONN = 1;
    static final int SIGDISC = 2;
    static int SIGNAL = -1;
    static final Object LOCK = new Object();
    static Runnable SUCCEED;
    static Runnable FAIL;

    class NetworkThread extends Thread {
        @Override
        public void run() {
            int paperState = -1;
            while (true) {
                if (SIGNAL == SIGDISC) {
                    // just don't connect
                    CONNECTED = false;
                    if (FAIL != null) {
                        FAIL.run();
                    }
                    synchronized (LOCK) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (SIGNAL == SIGCONN) {
                    if (paperState != paper.getState()) {
                        paperState = paper.getState();
                        startConnection();
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }

        /**
         * more like send
         */
        void startConnection() {
            Log.d(TAG, "startConnection");
            if (canvas == null) {
                FAIL.run();
                Log.d(TAG, "startConnection: canvas is null");
                return;
            }

            // not connected
            try {
                urlConnection = (HttpURLConnection) new URL(SERVER_POST_URL).openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

                paper.draw(canvas);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                Buffer ok = ByteBuffer.allocate(bitmap.getByteCount());
//                bitmap.copyPixelsToBuffer(ok);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                byte[] b = new byte[16];
                while ((in.read(b) + 1 & -2) != 0) {
                    Log.d(TAG, "startConnection: received " + new String(b));
                }
                in.close();
//                out.flush();
            } catch (IOException e) {
                Log.e(TAG, "startConnection: url error", e);
//                e.printStackTrace();
                ContentPusher.this.disconnect();
                return;
            } finally {
                urlConnection.disconnect();
            }
            Log.d(TAG, "startConnection: communication finished");
            if (!CONNECTED) {
                SUCCEED.run();
            }
            CONNECTED = true;

        }

    }

    static boolean CONNECTED;

    NetworkThread networkThread;
    public ContentPusher(Paper paper) {

        networkThread = new NetworkThread();
        networkThread.start();

        this.paper = paper;

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(200);
                    if (paper.getWidth() != 0) {
                        bitmap = Bitmap.createBitmap(paper.getWidth(), paper.getHeight(), Bitmap.Config.ARGB_8888);
                        canvas = new Canvas(bitmap);
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    public void connect(Runnable succeed, Runnable fail) {
        SUCCEED = succeed;
        FAIL = fail;
        SIGNAL = SIGCONN;
        synchronized (LOCK) {
            LOCK.notify();
        }
    }

    public void disconnect() {
        SIGNAL = SIGDISC;
        synchronized (LOCK) {
            LOCK.notify();
        }
    }

}
