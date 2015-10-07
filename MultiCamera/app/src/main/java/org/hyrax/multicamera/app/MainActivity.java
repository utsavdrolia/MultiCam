package org.hyrax.multicamera.app;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.ramimartin.multibluetooth.activity.BluetoothActivity;
import org.hyrax.multicamera.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends BluetoothActivity implements SurfaceHolder.Callback
{
    private SurfaceView sv;
    //a surface holder
    private SurfaceHolder sHolder;

    private ArrayAdapter<String> mLogAdapter;
    private ArrayList<String> mListLog;
    private ListView mListView;

    private static final String TAG = "MultiCamera";
    private static final String IMAGE_FOLDER = "/sdcard/DCIM/Camera/MultiCam";
    private static final String IMAGE_PATH = IMAGE_FOLDER + "/img_%d.jpg";
    private static final boolean D = true;
    private static final String RESULT_KEY = "r";
    private static final String ID_KEY = "id";

    private Camera mCamera;
    public int mCameraOrientation;
    private static int currentCamera = Camera.CameraInfo.CAMERA_FACING_BACK;

    @Override
    protected void onDestroy()
    {
        if (D) Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        if (D) Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File folder = new File(IMAGE_FOLDER);
        if (!folder.exists())
            folder.mkdirs();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCamera = Camera.open(currentCamera);

        sv = (SurfaceView) findViewById(R.id.surfaceView);

        //Get a surface
        sHolder = sv.getHolder();

        //add the callback interface methods defined below as the Surface View callbacks
        sHolder.addCallback(this);

        //tells Android that this surface will have its data constantly replaced
        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mListView = (ListView) findViewById(R.id.client_list);
        mListLog = new ArrayList<>();
        mLogAdapter = new ArrayAdapter<>(this, R.layout.item_console, mListLog);
        mListView.setAdapter(mLogAdapter);

    }

    public void setCameraDisplayOrientation()
    {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(currentCamera, info);
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int resultA = 0, resultB = 0;
        if (currentCamera == Camera.CameraInfo.CAMERA_FACING_BACK)
        {
            resultA = (info.orientation - degrees + 360) % 360;
            resultB = (info.orientation - degrees + 360) % 360;
            mCamera.setDisplayOrientation(resultA);
        } else
        {
            resultA = (360 + 360 - info.orientation - degrees) % 360;
            resultB = (info.orientation + degrees) % 360;
            mCamera.setDisplayOrientation(resultA);
        }
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(resultB);
        mCamera.setParameters(params);
        mCameraOrientation = resultB;
    }


    public void doSnap(final long snapID)
    {
        if (mCamera == null)
        {
            if (D) Log.d(TAG, "tried to snap when camera was inactive");
            return;
        }

        Camera.PictureCallback jpegCallback = new Camera.PictureCallback()
        {
            public void onPictureTaken(byte[] data, Camera camera)
            {
                BufferedOutputStream outStream = null;
                try
                {
                    String filename = String.format(IMAGE_PATH, snapID);
                    outStream = new BufferedOutputStream(new FileOutputStream(filename));
                    outStream.write(data);
                    outStream.flush();
                    outStream.close();
                    if (D) Log.d(TAG, "wrote bytes: " + data.length);
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = 8;
                    Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                    int smallWidth, smallHeight;
                    int dimension = 70;
                    if (bmp.getWidth() > bmp.getHeight())
                    {
                        smallWidth = dimension;
                        smallHeight = dimension * bmp.getHeight() / bmp.getWidth();
                    } else
                    {
                        smallHeight = dimension;
                        smallWidth = dimension * bmp.getWidth() / bmp.getHeight();
                    }
                    Bitmap bmpSmall = Bitmap.createScaledBitmap(bmp, smallWidth, smallHeight, false);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmpSmall.compress(Bitmap.CompressFormat.WEBP, 60, baos);
                    sendToServer(RESULT_KEY, baos.toByteArray(), null);
                    if (D) Log.d(TAG, "wrote bytes: " + baos.size());
                    mCamera.startPreview();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        };
        mCamera.startPreview();
        mCamera.takePicture(null, null, jpegCallback);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "onResume");
        super.onResume();
    }


    private void sendToServer(String key, byte[] bytes, Object o)
    {
        JSONObject json = new JSONObject();
        try
        {
            json.put(key, Base64.encodeToString(bytes, Base64.DEFAULT));
            sendMessage(json.toString());
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void receiveFromServer(String s)
    {
        try
        {
            JSONObject json = new JSONObject(s);
            doSnap(json.getLong(ID_KEY));
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int myNbrClientMax()
    {
        return 0;
    }

    @Override
    public void onBluetoothStartDiscovery() {
        setLogText("===> Start discovering ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
    }

    @Override
    public void onBluetoothCommunicator(String msg, String dev)
    {
        setLogText("===> receive msg from: " + dev);
        receiveFromServer(msg);
    }

    @Override
    public void onBluetoothDeviceFound(BluetoothDevice device) {
        setLogText("===> Device detected : "+ device.getAddress());
    }

    @Override
    public void onClientConnectionSuccess()
    {
        setLogText("===> Client Connexion success !");
    }

    @Override
    public void onClientConnectionFail()
    {
        setLogText("===> Client Connexion Failed !");
    }

    @Override
    public void onServeurConnectionSuccess()
    {
    }

    @Override
    public void onServeurConnectionFail()
    {
    }



    @Override
    public void onBluetoothNotAviable()
    {
        setLogText("===> Bluetooth not available on this device");
    }

    /**
     * Add text to the view log
     * @param text
     */
    public void setLogText(String text) {
        mListLog.add(text);
        mLogAdapter.notifyDataSetChanged();
        mListView.setSelection(mListView.getCount() - 1);
    }

    /**
     * This is called immediately after the surface is first created.
     * Implementations of this should start up whatever rendering code
     * they desire.  Note that only one thread can ever draw into
     * a {@link Surface}, so you should not draw into the Surface here
     * if your normal rendering will be in another thread.
     *
     * @param holder The SurfaceHolder whose surface is being created.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        try {
            mCamera.setPreviewDisplay(holder);
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            Camera.Size size = sizes.get(0);
            for (Camera.Size size1 : sizes)
            {
                if (size1.width > size.width)
                    size = size1;
            }
            params.setPictureSize(size.width, size.height);
            mCamera.setParameters(params);

        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * This is called immediately after any structural changes (format or
     * size) have been made to the surface.  You should at this point update
     * the imagery in the surface.  This method is always called at least
     * once, after {@link #surfaceCreated}.
     *
     * @param holder The SurfaceHolder whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width  The new width of the surface.
     * @param height The new height of the surface.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

    }

    /**
     * This is called immediately before a surface is being destroyed. After
     * returning from this call, you should no longer try to access this
     * surface.  If you have a rendering thread that directly accesses
     * the surface, you must ensure that thread is no longer touching the
     * Surface before returning from this function.
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        if (mCamera != null)
        {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
        }
    }

    public void doConnect(View view)
    {
        selectClientMode();
    }
}
