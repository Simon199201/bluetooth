package com.isoftstone.bluetooth.listener;

import com.isoftstone.bluetooth.BluetoothApplication;
import com.isoftstone.bluetooth.activity.BluetoothDemoActivity;
import com.isoftstone.bluetooth.adapter.AdapterManager;
import com.isoftstone.bluetooth.receiver.ScanBluetoothReceiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * 搜索设备按钮监听器
 * @author 210001001427
 *
 */
public class SearchDeviceBtnClickListener implements OnClickListener {
	private Activity mActivity;
	private AdapterManager mAdapterManager;
	
	private BluetoothAdapter mBluetoothAdapter;
	private ScanBluetoothReceiver mScanBluetoothReceiver;  //蓝牙扫描监听器
	private AlertDialog mAlertDialog;   //确定打开蓝牙 dialog
	private ProgressDialog mProgressDialog;
	
	public SearchDeviceBtnClickListener(Activity activity){
		this.mActivity = activity;
		this.mAdapterManager = BluetoothApplication.getInstance().getAdapterManager();
	}

	@Override
	public void onClick(View v) {
		//清空蓝牙设备列表
		mAdapterManager.clearDevice();
		mAdapterManager.updateDeviceAdapter();
		if(null == mBluetoothAdapter){
			//取得蓝牙适配器
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}
		if(!mBluetoothAdapter.isEnabled()){
			//蓝牙未打开, 打开蓝牙
			if(null == mAlertDialog){
				mAlertDialog = new AlertDialog.Builder(mActivity)
										.setTitle("打开蓝牙")
										.setPositiveButton("确定", new Dialog.OnClickListener(){

											@Override
											public void onClick(DialogInterface dialog,
													int which) {
												//发送请求，打开蓝牙
												Intent startBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
												mActivity.startActivityForResult(startBluetoothIntent, BluetoothDemoActivity.REQUEST_ENABLE);
											}
				
										})
										.setNeutralButton("取消", new Dialog.OnClickListener(){

											@Override
											public void onClick(DialogInterface dialog,
													int which) {
												mAlertDialog.dismiss();
											}
				
										}).create();
			}
			mAlertDialog.setMessage("蓝牙未打开，是否打开？");
			mAlertDialog.show();
		}else {
			//蓝牙已打开， 开始搜索设备
			beginDiscovery();
			Log.i("BluetoothDemo", "begin");
		}
	}
	
	/**
	 * 开始搜索设备...
	 */
	public void beginDiscovery() {
		if(null == mProgressDialog){
			mProgressDialog = new ProgressDialog(mActivity);
			mProgressDialog.setMessage("搜索设备中...");
		}
		mProgressDialog.show();
		//注册蓝牙扫描监听器
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		if(null == mScanBluetoothReceiver){
			mScanBluetoothReceiver = new ScanBluetoothReceiver(mActivity, mAdapterManager, mProgressDialog);
		}
		mActivity.registerReceiver(mScanBluetoothReceiver, intentFilter);
		mBluetoothAdapter.startDiscovery();
	}

}
