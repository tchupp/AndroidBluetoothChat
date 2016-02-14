package edu.msu.team15.androidbluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Cloud {

    private static class DeviceInfo {
        public String name;
        public String address;
        public boolean connected;

        public DeviceInfo(String address, String name, boolean connected) {
            this.address = address;
            this.name = name;
            this.connected = connected;
        }
    }

    public static class AvailableDeviceAdapter extends BaseAdapter {

        private List<DeviceInfo> devices = new ArrayList<>();

        public AvailableDeviceAdapter(final View view) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    devices = getAvailableDevices();
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
            // TODO draw the available device list
            return view;
        }

        private List<DeviceInfo> getAvailableDevices() {
            ArrayList<DeviceInfo> list = new ArrayList<>();

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

            for (BluetoothDevice device : bondedDevices) {
                list.add(new DeviceInfo(device.getName(), device.getAddress(), false));
            }

            return list;
        }
    }

}
