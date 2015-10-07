package org.hyrax.multicamera.server.app.activity;

import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.ramimartin.multibluetooth.activity.BluetoothActivity;
import org.hyrax.multicamera.server.app.R;
import org.hyrax.multicamera.server.app.ui.ResultItem;
import org.hyrax.multicamera.server.app.ui.ResultListAdapter;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BluetoothActivity
{

    private ResultListAdapter mResultAdapter;
    private ArrayAdapter<String> mLogAdapter;
    private ArrayList<String> mListLog;
    private ListView mListView;
    private Timer mExpTimer;
    private static final String RESULT_KEY = "r";
    private static final String ID_KEY = "id";
    private long expCounter = 0;
    private long repliesCounter;
    private int devicesConnected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        mListView = (ListView) findViewById(R.id.client_list);
        mListLog = new ArrayList<>();
        mLogAdapter = new ArrayAdapter<>(this, R.layout.item_console, mListLog);
        mListView.setAdapter(mLogAdapter);

        ListView mResultView = (ListView) findViewById(R.id.result_list);
        mResultAdapter = new ResultListAdapter(this, new ArrayList<ResultItem>());
        mResultView.setAdapter(mResultAdapter);

    }


    @Override
    public void onBluetoothCommunicator(String messageReceive, String dev)
    {
        setLogText("===> receive msg from: " + dev);
        try
        {
            JSONObject json = new JSONObject(messageReceive);
            String img = json.getString(RESULT_KEY);
            mResultAdapter.add(dev, StringToBitmap(img));
            setLogText("===> Images clicked: " + ++repliesCounter);
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private Bitmap StringToBitmap(String img)
    {
        byte[] bytes = Base64.decode(img, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }


    public void onConnect(View view)
    {
        setLogText("===> Start Server ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
        selectServerMode();
    }

    public void doExperiment(View view)
    {
        setLogText("===> Starting Experiment ");
        expCounter = 0;
        repliesCounter = 0;
        mExpTimer = new Timer();
        mExpTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        expCounter++;
                        setLogText("Server Click Number:"+expCounter);
                        click();
                    }
                });
            }
        }, 0l, 5000l);
    }


    public void stopExperiment(View view)
    {
        mExpTimer.cancel();
        mExpTimer.purge();
    }

    public void doSnap(View view)
    {
        click();
    }

    private void click()
    {
        JSONObject json = new JSONObject();
        try
        {
            json.put(ID_KEY, System.currentTimeMillis());
            sendMessage(json.toString());
            setLogText("===> Snap!");
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int myNbrClientMax()
    {
        return 6;
    }

    @Override
    public void onBluetoothStartDiscovery()
    {
        setLogText("===> Start discovering ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
    }

    @Override
    public void onBluetoothDeviceFound(BluetoothDevice device)
    {
        setLogText("===> Device detected : " + device.getAddress());
    }

    @Override
    public void onClientConnectionSuccess()
    {
    }

    @Override
    public void onClientConnectionFail()
    {
    }

    @Override
    public void onServeurConnectionSuccess()
    {
        devicesConnected++;
        setLogText("===> Serveur Connexion success ! Total devices:"+devicesConnected);
    }

    @Override
    public void onServeurConnectionFail()
    {
        devicesConnected--;
        setLogText("===> Serveur Connexion fail ! Total devices:"+devicesConnected);
    }


    @Override
    public void onBluetoothNotAviable()
    {
        setLogText("===> Bluetooth not aviable on this device");
    }

    /**
     * Add text to the view log
     *
     * @param text
     */
    public void setLogText(String text)
    {
        mListLog.add(text);
        mLogAdapter.notifyDataSetChanged();
        mListView.setSelection(mListView.getCount() - 1);
    }

}
