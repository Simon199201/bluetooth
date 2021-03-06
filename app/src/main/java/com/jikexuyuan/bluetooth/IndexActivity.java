package com.jikexuyuan.bluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.isoftstone.bluetooth.activity.BluetoothDemoActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class IndexActivity extends AppCompatActivity {
    @BindView(R.id.search_and_create_socket)
    Button searchAndCreateSocket;
    @BindView(R.id.pair)
    Button pair;
    @BindView(R.id.status)
    Button status;
    @BindView(R.id.transfer)
    Button transfer;
    @BindView(R.id.transfer_by_socket)
    Button transferBySocket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.search_and_create_socket, R.id.pair, R.id.status, R.id.transfer, R.id.transfer_by_socket})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.search_and_create_socket:
                startActivity(new Intent(this, MainActivity.class));
                break;
            case R.id.pair:
                startActivity(new Intent(this, PairActivity.class));
                break;
            case R.id.status:
                startActivity(new Intent(this, StatusSettingActivity.class));
                break;
            case R.id.transfer:
                startActivity(new Intent(this, BluetoothDemoActivity.class));
                break;
            case R.id.transfer_by_socket:
                startActivity(new Intent(this, SendFileBySocketActivity.class));
                break;
        }
    }
}
