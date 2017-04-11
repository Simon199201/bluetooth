package com.jikexuyuan.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StatusSettingActivity extends AppCompatActivity {
    @BindView(R.id.switch1)
    Switch switch1;
    private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//获取本地蓝牙设备
    private String bluetoothStatus = "off";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        ButterKnife.bind(this);

        Context mContext = getApplicationContext();
        mContext.registerReceiver(mReceiver, makeFilter());

        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mBluetoothAdapter.enable();//打开蓝牙
                } else {
                    mBluetoothAdapter.disable();// 关闭蓝牙
                }
            }
        });

    }

    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("TAG", "onReceive---------");
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.e("TAG", "onReceive---------STATE_TURNING_ON");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            bluetoothStatus = "on";
                            Log.e("TAG", "onReceive---------STATE_ON");
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.e("TAG", "onReceive---------STATE_TURNING_OFF");
                            //Ble.toReset(mContext);
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            bluetoothStatus = "off";
                            Log.e("TAG", "onReceive---------STATE_OFF");
                            //Ble.toReset(mContext);
                            break;
                    }
                    break;
            }
        }
    };

    @OnClick({R.id.button})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                Toast.makeText(StatusSettingActivity.this, "bluetooth is " + bluetoothStatus,
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
