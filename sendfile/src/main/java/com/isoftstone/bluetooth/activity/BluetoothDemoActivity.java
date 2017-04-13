package com.isoftstone.bluetooth.activity;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.isoftstone.bluetooth.BluetoothApplication;
import com.isoftstone.bluetooth.R;
import com.isoftstone.bluetooth.adapter.AdapterManager;
import com.isoftstone.bluetooth.entity.MyMenuItem;
import com.isoftstone.bluetooth.entity.TouchObject;
import com.isoftstone.bluetooth.listener.DeviceListCCMenuListener;
import com.isoftstone.bluetooth.listener.SearchDeviceBtnClickListener;
import com.isoftstone.bluetooth.listener.SelectFileBtnClickListener;
import com.isoftstone.bluetooth.listener.SetVisibleBtnClickListener;
import com.isoftstone.bluetooth.receiver.PairStateChangeReceiver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BluetoothDemoActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    public static final String SEND_FILE_NAME = "sendFileName";
    public static final int RESULT_CODE = 1000;    //选择文件   请求码
    public static final int REQUEST_ENABLE = 10000;   //打开蓝牙    请求码
//	private static final String CONNECT_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private BluetoothApplication mApplication;
    private AdapterManager mAdapterManager;     //Adapter管理器
    private TouchObject mTouchObject;       //当前操作对象

    private PairStateChangeReceiver mPairStateChangeReceiver;   //配对状态改变广播接收器
    private BluetoothSocket socket;       //蓝牙连接socket
    private Handler mOthHandler;    //其它线程Handler
    private SearchDeviceBtnClickListener mSearchDeviceBtnClickListener;  //搜索设备按钮监听器

    ListView mDeviceListView;
    TextView mSendFileNameTV;
    Button mSetVisibleBtn;
    Button mSearchDeviceBtn;
    Button mSelectFileBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mDeviceListView = (ListView) findViewById(R.id.deviceListView);
        mSetVisibleBtn = (Button) findViewById(R.id.setDeviceVisibleBtn);
        mSearchDeviceBtn = (Button) findViewById(R.id.searchDeviceBtn);
        mSelectFileBtn = (Button) findViewById(R.id.cancelSearchBtn);
        mSendFileNameTV = (TextView) findViewById(R.id.sendFileTV);

        mApplication = BluetoothApplication.getInstance();
        mTouchObject = mApplication.getTouchObject();
        //实例化Adapter管理器并设置到Application
        mAdapterManager = new AdapterManager(this);
        mApplication.setAdapterManager(mAdapterManager);

        //
        mDeviceListView.setAdapter(mAdapterManager.getDeviceListAdapter());

        mSearchDeviceBtnClickListener = new SearchDeviceBtnClickListener(this);
        //添加监听器
        mDeviceListView.setOnCreateContextMenuListener(new DeviceListCCMenuListener(mDeviceListView));
        mSetVisibleBtn.setOnClickListener(new SetVisibleBtnClickListener(this));
        mSearchDeviceBtn.setOnClickListener(mSearchDeviceBtnClickListener);
        mSelectFileBtn.setOnClickListener(new SelectFileBtnClickListener(this));
        findViewById(R.id.sendFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doPair();
            }
        });
        findViewById(R.id.sendFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSendFileByBluetooth();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE) {
            //请求为 "打开蓝牙"
            if (resultCode == RESULT_OK) {
                //打开蓝牙成功
                mSearchDeviceBtnClickListener.beginDiscovery();
            } else {
                //打开蓝牙失败
                Toast.makeText(this, "打开蓝牙失败！", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == RESULT_CODE) {
            //请求为 "选择文件"
            try {
                //取得选择的文件名
                String sendFileName = data.getStringExtra(SEND_FILE_NAME);
                mSendFileNameTV.setText(sendFileName);
            } catch (Exception e) {

            }
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == MyMenuItem.MENU_GROUP_DEVICE) {
            switch (item.getItemId()) {
                case MyMenuItem.MENU_ITEM_PAIR_ID:     //配对
                    doPair();
                    break;

                case MyMenuItem.MENU_ITEM_SEND_ID:   //发送文件
                    doSendFileByBluetooth();
                    break;

                default:
                    break;
            }
        }
        return true;
    }

    /**
     * 配对
     */
    private void doPair() {
        if (mTouchObject.bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
            //未与该设备配对
            if (null == mPairStateChangeReceiver) {
                mPairStateChangeReceiver = new PairStateChangeReceiver(this);
            }
            //注册设备配对状态改变监听器
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            this.registerReceiver(mPairStateChangeReceiver, intentFilter);
            if (null == mOthHandler) {
                HandlerThread handlerThread = new HandlerThread("other_thread");
                handlerThread.start();
                mOthHandler = new Handler(handlerThread.getLooper());
            }
            mOthHandler.post(new Runnable() {

                @Override
                public void run() {
                    initSocket();   //取得socket
                    try {
                        socket.connect();   //请求配对
//						mAdapterManager.updateDeviceAdapter();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            //已经与该设备配对
            Toast.makeText(BluetoothDemoActivity.this, "该设备已配对，无需重复操作！", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 通过蓝牙发送文件
     */
    private void doSendFileByBluetooth() {
        //取得文件全路径
        String filePath = mSendFileNameTV.getText().toString().trim();
        if (!filePath.equals("null")) {
            if (null == mOthHandler) {
                HandlerThread handlerThread = new HandlerThread("other_thread");
                handlerThread.start();
                mOthHandler = new Handler(handlerThread.getLooper());
            }
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("*/*");
            sharingIntent.setComponent(new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity"));
//            sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
            sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));

            startActivity(sharingIntent);
            try {
                if (null != socket) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


//			mOthHandler.post(new Runnable() {
//
//				@Override
//				public void run() {
//					//调用系统程序发送文件
//					ContentValues cv = new ContentValues();
//					String uri = "file://" + mSendFileNameTV.getText().toString().trim();
//					cv.put("uri", uri);
//					cv.put("destination", mTouchObject.bluetoothDevice.getAddress());
//					cv.put("direction", 0);
//					Long ts = System.currentTimeMillis();
//					cv.put("timestamp", ts);
//					getContentResolver().insert(Uri.parse("content://com.android.bluetooth.opp/btopp"), cv);
//					try {
//						if(null != socket){
//							socket.close();
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//
//				Intent intent = new Intent();
//				intent.setAction(Intent.ACTION_SEND);
//				String filePath = "file:///sdcard/test.jpg";
//				String extension = filePath.substring(filePath.lastIndexOf(".")+1);
//				String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
//				Log.i("BluetoothDemo", "extension" + extension + "type:" + type);
//				intent.setType("image/jpg");
//				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
//				startActivity(intent);
//				}
//			});
//		}else {
//			Toast.makeText(BluetoothDemoActivity.this, "请选择要发送的文件!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 取得BluetoothSocket
     */

    private void initSocket() {
        BluetoothSocket temp = null;
        try {
//			temp = mTouchObject.bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(CONNECT_UUID));
            //以上取得socket方法不能正常使用， 用以下方法代替
            Method m = mTouchObject.bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            temp = (BluetoothSocket) m.invoke(mTouchObject.bluetoothDevice, 1);
            //怪异错误： 直接赋值给socket,对socket操作可能出现异常，  要通过中间变量temp赋值给socket
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        socket = temp;
    }

    /**
     * 改变按钮显示文字
     */
    public void changeSearchBtnText() {
        mSearchDeviceBtn.setText("重新搜索");
    }

}