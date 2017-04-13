package com.isoftstone.bluetooth.listener;

import com.isoftstone.bluetooth.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * 设置蓝牙可见按钮监听器
 * @author 210001001427
 *
 */
public class SetVisibleBtnClickListener implements OnClickListener {
	private static final String[] arr = {"60", "120", "180", "240", "300"};
	
	private Activity mActivity;
	
	private String visibleTime;   //蓝牙可见时间
	Spinner mSelectTimeSpinner;
	Button mSelectTimeEnsureBtn;
	Button mSelectTimeCancelBtn;
	AlertDialog mSelectTimeDialog;  //选择时间dialog
	
	public SetVisibleBtnClickListener(Activity activity){
		this.mActivity = activity;
	}

	@Override
	public void onClick(View v) {
		if(null == mSelectTimeDialog){
			View view = LayoutInflater.from(mActivity).inflate(R.layout.select_time_dialog, null);
			mSelectTimeSpinner = (Spinner) view.findViewById(R.id.selectTimeSpinner);
			mSelectTimeEnsureBtn = (Button) view.findViewById(R.id.selectTimeEnsureBtn);
			mSelectTimeCancelBtn = (Button) view.findViewById(R.id.selectTimeCancelBtn);
			
			ArrayAdapter adapter = new ArrayAdapter(mActivity, android.R.layout.simple_spinner_item, arr);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSelectTimeSpinner.setAdapter(adapter);
			mSelectTimeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent,
						View view, int position, long id) {
					visibleTime = arr[position];
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					
				}
			});
			
			mSelectTimeEnsureBtn.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//发送请求，设置蓝牙可见时间
					Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
					intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, visibleTime);
					mActivity.startActivity(intent);
					mSelectTimeDialog.dismiss();
				}
			});
			mSelectTimeCancelBtn.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mSelectTimeDialog.dismiss();
				}
			});
			mSelectTimeDialog = new AlertDialog.Builder(mActivity)
										.setTitle("设置蓝牙可见")
										.setView(view).create();
		}
		mSelectTimeDialog.show();
	}

}
