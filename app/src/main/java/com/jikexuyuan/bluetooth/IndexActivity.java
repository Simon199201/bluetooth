package com.jikexuyuan.bluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class IndexActivity extends AppCompatActivity {
    @BindView(R.id.search_and_create_socket)
    Button searchAndCreateSocket;
    @BindView(R.id.pair)
    Button pair;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.search_and_create_socket, R.id.pair})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.search_and_create_socket:
                startActivity(new Intent(this, MainActivity.class));
                break;
            case R.id.pair:
                startActivity(new Intent(this, PairActivity.class));
                break;
        }
    }
}
