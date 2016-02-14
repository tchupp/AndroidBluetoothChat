package edu.msu.team15.androidbluetoothchat;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class Cloud {

    private static class DeviceInfo {
        public String name = "";
        public String address = "";
    }

    public static class AvailableDeviceAdapter extends BaseAdapter {

        private List<DeviceInfo> devices = new ArrayList<>();

        public AvailableDeviceAdapter(final View view) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<DeviceInfo> newAvailableDevices = getAvailableDevices();
                    if (newAvailableDevices != null) {
                        devices = newAvailableDevices;
                    }
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

            // TODO Scan for devices here

            return list;
        }
    }

}
