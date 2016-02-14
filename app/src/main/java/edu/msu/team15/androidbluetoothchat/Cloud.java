package edu.msu.team15.androidbluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Cloud {

    private static class DeviceInfo {
        public String name;
        public String address;
        public boolean connected = false;

        public DeviceInfo(String name, String address) {
            this.address = address;
            this.name = name;
        }
    }

    public static class AvailableDeviceAdapter extends BaseAdapter {
        private final View view;
        private final BluetoothAdapter bluetoothAdapter;
        private List<DeviceInfo> devices = new ArrayList<>();

        public AvailableDeviceAdapter(final View view, BluetoothAdapter bluetoothAdapter) {
            this.view = view;
            this.bluetoothAdapter = bluetoothAdapter;

            this.bluetoothAdapter.startDiscovery();

            new Thread(new Runnable() {

                @Override
                public void run() {
                    Set<BluetoothDevice> bondedDevices = AvailableDeviceAdapter.this.bluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice device : bondedDevices) {
                        AvailableDeviceAdapter.this.devices.add(new DeviceInfo(device.getName(), device.getAddress()));
                    }

                    view.post(new Runnable() {

                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }

        @Override
        public int getCount() {
            return this.devices.size();
        }

        @Override
        public Object getItem(int position) {
            return this.devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
            }

            TextView textDeviceName = (TextView) view.findViewById(R.id.textDeviceName);
            TextView textDeviceAddress = (TextView) view.findViewById(R.id.textDeviceAddress);
            textDeviceName.setText(devices.get(position).name);
            textDeviceAddress.setText(devices.get(position).address);

            return view;
        }

        public void addDevice(DeviceInfo deviceInfo) {
            this.devices.add(deviceInfo);

            this.view.post(new Runnable() {

                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    public static class AvailableDeviceReceiver extends BroadcastReceiver {

        private AvailableDeviceAdapter adapter;

        public AvailableDeviceReceiver(AvailableDeviceAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("ABC", "action received");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("ABC", "found device: " + device.getAddress());
                this.adapter.addDevice(new DeviceInfo(device.getName(), device.getAddress()));
            }
        }
    }
}
