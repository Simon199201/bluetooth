package com.jikexuyuan.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jikexuyuan.ndk.searchbluetoothdevice.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private BluetoothAdapter mBluetoothAdapter;
    private ListView lvDevices;
    private TextView mTextView;
    private Button mButton;

    private List<String> blueToothDevices = new ArrayList<>();

    private ArrayAdapter<String> mAdapter;

    private final UUID MY_UUID = UUID.fromString("db764ac8-4b08-7f25-aafe-59d03c27bae3");
    private final String NAME = "bluetooth_socket";

    private BluetoothSocket mBluetootoSocket;
    private BluetoothDevice mBluetoothDevice;

    private OutputStream mOutputStream;
    private ProgressBar progress;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Toast.makeText(MainActivity.this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
            return false;
        }
    });

    private AcceptThread acceptThread;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mTextView.append(device.getName() + "\t" + device.getAddress() + "\n");
                    if (!blueToothDevices.contains(device.getName() + ":" + device.getAddress())) {
                        blueToothDevices.add(device.getName() + ":" + device.getAddress());
                        mAdapter.notifyDataSetChanged();
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                setTitle(getString(R.string.search_finished));
                progress.setVisibility(View.GONE);
                mButton.setEnabled(true);
                mButton.setText(getString(R.string.search_blue_device));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                setTitle(getString(R.string.start_search));
                progress.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initAdapter();
        initOnClick();
        initReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (acceptThread != null && !acceptThread.isInterrupted()) {
            acceptThread.interrupt();
        }
        acceptThread = null;
    }

    private void initReceiver() {
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, intentFilter);
        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, intentFilter);
        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        this.registerReceiver(receiver, intentFilter);
    }

    private void initOnClick() {
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Onclick_search();
            }
        });
    }

    private void initAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == mBluetoothAdapter) {
            Toast.makeText(this.getApplicationContext(), R.string.not_suppotr_bluetooth, Toast.LENGTH_SHORT).show();
            mButton.setEnabled(false);
            return;
        }
        Set<BluetoothDevice> bluetoothDevices = mBluetoothAdapter.getBondedDevices();
        if (bluetoothDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                mTextView.append(bluetoothDevice.getName() + "\t" + bluetoothDevice.getAddress() + "\n");
                blueToothDevices.add(bluetoothDevice.getName() + ":" + bluetoothDevice.getAddress());
            }
        }
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, blueToothDevices);
        lvDevices.setAdapter(mAdapter);
        lvDevices.setOnItemClickListener(this);
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private void initView() {
        mTextView = (TextView) findViewById(R.id.textView_devices);
        mButton = (Button) findViewById(R.id.btn_search_blueDevice);
        lvDevices = (ListView) findViewById(R.id.listView);
        progress = (ProgressBar) findViewById(R.id.progress);
    }

    private void Onclick_search() {
//        mButton.setEnabled(false);
        mButton.setText(getString(R.string.search_blue_device_cancel));
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String s = mAdapter.getItem(position);
        if (null == s) {
            return;
        }
        String address = s.substring(s.indexOf(":") + 1).trim();

        try {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            if (null == mBluetoothDevice) {
                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
            }
            if (mBluetootoSocket == null) {
                mBluetootoSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mBluetootoSocket.connect();
                mOutputStream = mBluetootoSocket.getOutputStream();

            }
            if (mOutputStream != null) {
                mOutputStream.write("发送信息到其他蓝牙设备".getBytes("utf-8"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mBluetoothServerSocket;
        private BluetoothSocket mBluetoothSocket;
        private InputStream is;
        private OutputStream os;
        private boolean isContinue;

        AcceptThread() {
            try {
                mBluetoothServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                mBluetoothSocket = mBluetoothServerSocket.accept();
                is = mBluetoothSocket.getInputStream();
                os = mBluetoothSocket.getOutputStream();
                isContinue = true;
                while (isContinue) {
                    byte[] buffer = new byte[128];
                    int count = is.read(buffer);
                    Message message = new Message();
                    message.obj = new String(buffer, 0, count, "utf-8");
                    mHandler.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
                isContinue = false;
            }
        }
    }
}
