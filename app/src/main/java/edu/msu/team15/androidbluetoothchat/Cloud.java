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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Cloud {

    private static String ABC_NAME = "AndroidBluetoothChat";
    private static UUID ABC_UUID = new UUID(1625435424, 547981287);

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
        private List<DeviceInfo> bluetoothDeviceInfo = new ArrayList<>();
        private Map<String, BluetoothDevice> bluetoothDeviceMap = new HashMap<>();


        public AvailableDeviceAdapter(final View view, BluetoothAdapter bluetoothAdapter) {
            this.view = view;
            this.bluetoothAdapter = bluetoothAdapter;
            this.bluetoothAdapter.startDiscovery();

            new Thread(new Runnable() {

                @Override
                public void run() {
                    Set<BluetoothDevice> bondedDevices = AvailableDeviceAdapter.this.bluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice device : bondedDevices) {
                        AvailableDeviceAdapter.this.bluetoothDeviceInfo.add(new DeviceInfo(device.getName(), device.getAddress()));
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
            return this.bluetoothDeviceInfo.size();
        }

        @Override
        public Object getItem(int position) {
            return this.bluetoothDeviceInfo.get(position);
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
            textDeviceName.setText(bluetoothDeviceInfo.get(position).name);
            textDeviceAddress.setText(bluetoothDeviceInfo.get(position).address);

            return view;
        }

        public BluetoothDevice getDevice(int position) {
            String address = this.bluetoothDeviceInfo.get(position).address;
            return this.bluetoothDeviceMap.get(address);
        }

        public void addDevice(BluetoothDevice device) {
            this.bluetoothDeviceInfo.add(new DeviceInfo(device.getName(), device.getAddress()));
            this.bluetoothDeviceMap.put(device.getAddress(), device);

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
                this.adapter.addDevice(device);
            }
        }
    }

    public static class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket;

        public ConnectThread(BluetoothDevice bluetoothDevice) {
            try {
                this.bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(Cloud.ABC_UUID);
            } catch (IOException e) {
                // TODO remove print stack
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                this.bluetoothSocket.connect();
            } catch (IOException e) {
                // TODO remove print stack
                e.printStackTrace();

                try {
                    this.bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO remove print stack
                    e1.printStackTrace();
                }
            }
        }

        public void cancel() {
            try {
                this.bluetoothSocket.close();
            } catch (IOException e) {
                // TODO remove print stack
                e.printStackTrace();
            }
        }
    }

    public static class AcceptThread extends Thread {
        private BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread(BluetoothAdapter bluetoothAdapter) {
            try {
                this.bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(Cloud.ABC_NAME, Cloud.ABC_UUID);
            } catch (IOException e) {
                // TODO remove print stack
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            BluetoothSocket bluetoothSocket;

            while (true) {
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    // TODO remove print stack
                    e.printStackTrace();
                    break;
                }
                Log.i("Accept Thread", "" + String.valueOf(bluetoothSocket.isConnected()));
                Log.i("Accept Thread", "" + String.valueOf(bluetoothSocket.getRemoteDevice()));
            }
        }

        public void cancel() {
            try {
                this.bluetoothServerSocket.close();
            } catch (IOException e) {
                // TODO remove print stack
                e.printStackTrace();
            }
        }
    }

    public static class ConnectedThread extends Thread {
        private BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ConnectedThread(BluetoothSocket bluetoothSocket) {
            this.bluetoothSocket = bluetoothSocket;

            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                // TODO remove print stack
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Log.i("ABC", "ConnectedThread started");
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = this.inputStream.read(buffer);
                    Log.i("RECEIVED MESSAGE LENGTH", String.valueOf(bytes));
                    Log.i("RECEIVED MESSAGE IS", Arrays.toString(buffer));
                } catch (IOException e) {
                    // TODO remove print stack
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] message) {
            try {
                this.outputStream.write(message);
            } catch (IOException e) {
                // TODO remove print stack
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                this.bluetoothSocket.close();
            } catch (IOException e) {
                // TODO remove print stack
                e.printStackTrace();
            }
        }
    }
}
