package com.isoftstone.bluetooth.adapter;

import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.isoftstone.bluetooth.R;
import com.isoftstone.bluetooth.adapter.util.Device;

public class DeviceListAdapter extends BaseAdapter {
	private List<BluetoothDevice> mDeviceList;
	private LayoutInflater mLayoutInflater;
	private int mLayoutId;
	
	private String pairInfo;
	
	
	public DeviceListAdapter(Context context, List<BluetoothDevice> deviceList, int layoutId){
		this.mDeviceList = deviceList;
		this.mLayoutInflater = LayoutInflater.from(context);
		this.mLayoutId = layoutId;
	}

	@Override
	public int getCount() {
		return mDeviceList.size();
	}

	@Override
	public Object getItem(int position) {
		return mDeviceList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mDeviceList.get(position).hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		BluetoothDevice bluetoothDevice = mDeviceList.get(position);
		Device device = null;
		if(null == convertView){
			device = new Device();
			convertView = mLayoutInflater.inflate(mLayoutId, null);
			device.mDeviceNameTV = (TextView) convertView.findViewById(R.id.deviceNameTV);
			device.mIsPairTV = (TextView) convertView.findViewById(R.id.isPairTV);
			device.mMacAddressTV = (TextView) convertView.findViewById(R.id.marAddressTV);
			
			convertView.setTag(device);
		}else {
			device = (Device) convertView.getTag();
		}
		try {
			device.mDeviceNameTV.setText(bluetoothDevice.getName());
		} catch (Exception e) {
			device.mDeviceNameTV.setText("null");
		}
		if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED){
			pairInfo = "已配对";
		}else {
			pairInfo = "未配对";
		}
		device.mIsPairTV.setText(pairInfo);
		device.mMacAddressTV.setText(bluetoothDevice.getAddress());
		
		return convertView;
	}

}
