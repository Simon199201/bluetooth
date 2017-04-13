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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.isoftstone.bluetooth.activity.SelectFileActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.isoftstone.bluetooth.activity.BluetoothDemoActivity.RESULT_CODE;
import static com.isoftstone.bluetooth.activity.BluetoothDemoActivity.SEND_FILE_NAME;

public class SendFileBySocketActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private static final String TAG = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private ListView lvDevices;//用来展示扫描结果的ListView
    private TextView mTextView;//用来展示扫描结果的TextView
    private Button mSendButton;
    private EditText mEditText;
    private Button mSearchButton;//扫描

    private List<String> blueToothDevices = new ArrayList<>();//存放蓝牙设备的List

    private ArrayAdapter<String> mAdapter;

    private final UUID MY_UUID = UUID.fromString("db764ac8-4b08-7f25-aafe-59d03c27bae3");//uuid 相当于端口号
    private final String NAME = "bluetooth_socket";//serverSocket name

    private BluetoothSocket mBluetoothSocket;//蓝牙客户端
    private BluetoothDevice mBluetoothDevice;//蓝牙设备

    private OutputStream mOutputStream;//客户端
    private ProgressBar progress;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.Message.FILE_NOT_EXIST:
                    Toast.makeText(SendFileBySocketActivity.this, getString(R.string.not_exist), Toast.LENGTH_SHORT).show();
                    break;
            }
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
                mSearchButton.setText(getString(R.string.search_blue_device));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                setTitle(getString(R.string.start_search));
                progress.setVisibility(View.VISIBLE);
            }
        }
    };
    private String address;

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
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    private void initReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        this.registerReceiver(receiver, intentFilter);
    }

    private void initOnClick() {
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Onclick_search();
            }
        });
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSendButton.getText().equals(getString(R.string.send))) {
                    Onclick_send();
                } else {
                    Intent intent = new Intent(SendFileBySocketActivity.this, SelectFileActivity.class);
                    SendFileBySocketActivity.this.startActivityForResult(intent, RESULT_CODE);
                }
            }
        });
    }

    private void initAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == mBluetoothAdapter) {
            Toast.makeText(this.getApplicationContext(), R.string.not_suppotr_bluetooth, Toast.LENGTH_SHORT).show();
            mSearchButton.setEnabled(false);
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
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
        lvDevices.setOnItemLongClickListener(this);
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private void initView() {
        mTextView = (TextView) findViewById(R.id.textView_devices);
        mSearchButton = (Button) findViewById(R.id.btn_search_blueDevice);
        lvDevices = (ListView) findViewById(R.id.listView);
        progress = (ProgressBar) findViewById(R.id.progress);
        mSendButton = (Button) findViewById(R.id.send);
        mSendButton.setText(getString(R.string.choose_file));
        mEditText = (EditText) findViewById(R.id.editText_send);
    }

    private void Onclick_search() {
        mSearchButton.setText(getString(R.string.search_blue_device_cancel));
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private void Onclick_send() {
        if (address == null) {
            Toast.makeText(this, getString(R.string.please_select_bluetooth_address), Toast.LENGTH_SHORT).show();
            return;
        }
        if (mEditText.getText().toString().equals("")) {
            Toast.makeText(this, R.string.not_null, Toast.LENGTH_LONG).show();
        }
        sendMessage(address, mEditText.getText().toString());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String s = mAdapter.getItem(position);
        if (null == s) {
            return;
        }
        address = s.substring(s.indexOf(":") + 1).trim();

//        sendMessage(address, (mBluetoothAdapter.getName() + ":" + getString(R.string.send_message_to_other_device)));
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

    /**
     * 发送信息到另一个蓝牙设备
     *
     * @param address  蓝牙设备地址
     * @param filePath 信息
     */
    private void sendMessage(final String address, final String filePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    Log.e(TAG, "sendMessage: 1");
                    if (null == mBluetoothDevice) {
                        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
                    }
                    Log.e(TAG, "sendMessage: 2");

                    if (mBluetoothSocket == null) {
                        mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        mBluetoothSocket.connect();
                        mOutputStream = mBluetoothSocket.getOutputStream();
                    }
                    Log.e(TAG, "sendMessage: 3");

                    if (!mBluetoothSocket.isConnected()) {
                        resetSocket();
                    }

                    Log.e(TAG, "sendMessage: 4");
                    File file = new File(filePath);
                    if (!file.exists()) {
                        mHandler.sendEmptyMessage(Constant.Message.FILE_NOT_EXIST);
                    }
                    if (mOutputStream != null) {
                        FileInputStream mInputStream = null;
                        try {
                            mInputStream = new FileInputStream(file);
                            int count = 2 * 1024 * 1024;
                            byte[] buffer = new byte[count];
                            int read;
                            while ((read = mInputStream.read(buffer)) != -1) {
                                mOutputStream.write(buffer, 0, read);
                                Log.e(TAG, "run: " + read);
                            }
                            Log.e(TAG, "onItemClick: " + mBluetoothAdapter.getName() + ":" + filePath + "\tclose");
                        } catch (Exception e) {
                            resetSocket();
                            sendMessage(address, filePath);
                        } finally {
                            if (mInputStream != null) {
                                mInputStream.close();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    private class AcceptThread extends Thread {
        private BluetoothServerSocket mBluetoothServerSocket;
        private BluetoothSocket bluetoothSocket;
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
            while (true) {
                FileOutputStream fo = null;
                try {
                    if (mBluetoothServerSocket == null) {
                        return;
                    }
                    bluetoothSocket = mBluetoothServerSocket.accept();
                    File file = new File("/mnt/sdcard/test.png");
                    fo = new FileOutputStream(file);
                    Log.e(TAG, "run: accept");
                    is = bluetoothSocket.getInputStream();
                    os = bluetoothSocket.getOutputStream();
                    isContinue = true;
                    while (isContinue) {
                        byte[] buffer = new byte[128];
                        int count = is.read(buffer);
                        if (count != -1) {
                            fo.write(buffer, 0, count);
                        }
//                        Message message = new Message();
//                        message.obj = new String(buffer, 0, count, "utf-8");
//                        mHandler.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    isContinue = false;
                } finally {
                    try {
                        if (bluetoothSocket != null) {
                            bluetoothSocket.close();
                        }
                        if (fo != null) {
                            fo.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    private void resetSocket() {
        try {
            mBluetoothSocket.close();
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
            mBluetoothSocket.connect();
            mOutputStream = mBluetoothSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_CODE) {
            //请求为 "选择文件"
            try {
                //取得选择的文件名
                String sendFileName = data.getStringExtra(SEND_FILE_NAME);
                mEditText.setText(sendFileName);
                mSendButton.setText(getString(R.string.send));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
