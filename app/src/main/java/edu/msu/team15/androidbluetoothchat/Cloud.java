package edu.msu.team15.androidbluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    public static class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket;

        public boolean connect(BluetoothDevice bluetoothDevice, UUID uuid) {
            BluetoothSocket temp = null;

            try {
                temp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.d("CONNECTTHREAD", "Could not create RFCOMM socket:" + e.toString());
                return false;
            }
            try {
                bluetoothSocket.connect();
            } catch (IOException e) {
                Log.d("CONNECTTHREAD", "Could not connect: " + e.toString());
                try {
                    bluetoothSocket.close();
                } catch (IOException close) {
                    Log.d("CONNECTTHREAD", "Could not close connection:" + e.toString());
                    return false;
                }
            }
            return true;
        }

        public boolean cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException close) {
                Log.d("CONNECTTHREAD","Could not close connection:" + close.toString());
                return false;
            }
            return true;
        }
    }

    public static class ServerConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket;

        public ServerConnectThread() { }

        public void acceptConnect(BluetoothAdapter bluetoothAdapter, UUID uuid) {
            BluetoothServerSocket temp = null;

            try {
                temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Service_Name", uuid);
            } catch (IOException e) {
                Log.d("SERVERCONNECT", "Could not get a BluetoothServerSocket:" + e.toString());
            }
            while (true) {
                try {
                    if (temp != null) {
                        bluetoothSocket = temp.accept();
                    }
                    else {
                        Log.d("SERVERCONNECT", "temp is null");
                    }
                } catch (IOException e) {
                    Log.d("SERVERCONNECT", "Could not accept an incoming connection.");
                    break;
                }
                if (bluetoothSocket != null) {
                    try {
                        temp.close();
                    } catch (IOException e) {
                        Log.d("SERVERCONNECT", "Could not close ServerSocket:" + e.toString());
                    }
                    break;
                }
            }
        }

        public void closeConnect() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {

            }
        }
    }

    public static class ManageConnectThread extends Thread {
        public ManageConnectThread() { }

        public void sendData(BluetoothSocket bluetoothSocket, int data) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(4);
            output.write(data);
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(output.toByteArray());
        }

        public int receiveData(BluetoothSocket bluetoothSocket) throws IOException{
            byte[] buffer = new byte[4];
            ByteArrayInputStream input = new ByteArrayInputStream(buffer);
            InputStream inputStream = bluetoothSocket.getInputStream();
            inputStream.read(buffer);
            return input.read();
        }
    }
}
