package edu.msu.team15.androidbluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Cloud {

    private static String ABC_NAME = "AndroidBluetoothChat";
    private static UUID ABC_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private static class DeviceInfo {
        public String name;
        public String address;
        public boolean paired = false;

        public DeviceInfo(String name, String address, boolean paired) {
            this.address = address;
            this.name = name;
            this.paired = paired;
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

            new Thread(new Runnable() {

                @Override
                public void run() {
                    Set<BluetoothDevice> bondedDevices = AvailableDeviceAdapter.this.bluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice device : bondedDevices) {
                        AvailableDeviceAdapter.this.addPairedDevice(device);
                    }

                    notifyDataChanged();
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

        public void addUnpairedDevice(BluetoothDevice device) {
            addDevice(device, false);
        }

        public void addPairedDevice(BluetoothDevice device) {
            addDevice(device, true);
        }

        private void addDevice(BluetoothDevice device, boolean paired) {
            if (this.bluetoothDeviceMap.get(device.getAddress()) == null) {
                this.bluetoothDeviceMap.put(device.getAddress(), device);
                this.bluetoothDeviceInfo.add(new DeviceInfo(device.getName(), device.getAddress(), paired));

                notifyDataChanged();
            }
        }

        private void notifyDataChanged() {
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

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("ABC", "found device: " + device.getAddress());
                this.adapter.addUnpairedDevice(device);
            }
        }
    }

    public static class ChatService {
        public static final int STATE_NONE = 0;
        public static final int STATE_LISTEN = 1;
        public static final int STATE_CONNECTING = 2;
        public static final int STATE_CONNECTED = 3;

        private MainActivity context;
        private BluetoothAdapter bluetoothAdapter;
        private int currentState;

        private ConnectThread connectThread;
        private ConnectedThread connectedThread;
        private AcceptThread acceptThread;

        public ChatService(MainActivity context, BluetoothAdapter bluetoothAdapter) {
            this.context = context;
            this.bluetoothAdapter = bluetoothAdapter;
            this.currentState = STATE_NONE;
        }

        public synchronized void start() {
            closeConnectThread();
            closeConnectedThread();

            setState(STATE_LISTEN, "Unknown", "Unknown");

            if (this.acceptThread == null) {
                this.acceptThread = new AcceptThread(this.bluetoothAdapter, this);
                this.acceptThread.start();
            }
        }

        public synchronized void connect(BluetoothDevice bluetoothDevice) {
            Log.i("ABC", "Connect to " + bluetoothDevice.getAddress());

            if (this.currentState == STATE_CONNECTING) {
                closeConnectThread();
            }

            closeConnectedThread();

            this.connectThread = new ConnectThread(bluetoothDevice, this);
            this.connectThread.start();
            setState(STATE_CONNECTING, bluetoothDevice.getName(), bluetoothDevice.getAddress());
        }

        public synchronized void connected(BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice) {
            Log.d("ABC", "connected to " + bluetoothDevice.getAddress());

            closeConnectedThread();
            closeAcceptThread();

            this.connectedThread = new ConnectedThread(bluetoothSocket, this);
            this.connectedThread.start();

            this.context.update(this.currentState, bluetoothDevice.getName(), bluetoothDevice.getAddress());
            this.context.toast("Connected to " + bluetoothDevice.getAddress());

            setState(STATE_CONNECTED, bluetoothDevice.getName(), bluetoothDevice.getAddress());
        }

        public void messageReceived(String message) {
            this.context.toast(message);
        }

        public void connectionFailed() {
            this.context.toast("Connection Failed");
            start();
        }

        public void connectionLost() {
            this.context.toast("Connection Lost");
            start();
        }

        public void sendMessage(String message) {
            ConnectedThread temp;

            synchronized (this) {
                if (this.currentState != STATE_CONNECTED) {
                    return;
                }
                temp = this.connectedThread;
            }
            temp.write(message.getBytes());
        }

        public synchronized int getState() {
            return this.currentState;
        }

        private synchronized void setState(int state, String deviceName, String deviceAddress) {
            Log.i("ABC", "set state to " + state);

            this.currentState = state;
            this.context.update(this.currentState, deviceName, deviceAddress);
        }

        private void closeConnectThread() {
            if (this.connectThread != null) {
                this.connectThread.cancel();
                this.connectThread = null;
            }
        }

        private void closeConnectedThread() {
            if (this.connectedThread != null) {
                this.connectedThread.cancel();
                this.connectedThread = null;
            }
        }

        private void closeAcceptThread() {
            if (this.acceptThread != null) {
                this.acceptThread.cancel();
                this.acceptThread = null;
            }
        }
    }

    public static class ConnectThread extends Thread {
        private BluetoothDevice bluetoothDevice;
        private ChatService chatService;
        private BluetoothSocket bluetoothSocket;

        public ConnectThread(BluetoothDevice bluetoothDevice, ChatService chatService) {
            this.bluetoothDevice = bluetoothDevice;
            this.chatService = chatService;
            ParcelUuid[] uuids = this.bluetoothDevice.getUuids();
            Log.d("ABC", "Remote UUIDs length" + Arrays.toString(uuids));

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
                this.chatService.connectionFailed();
                return;
            }

            this.chatService.connected(this.bluetoothSocket, this.bluetoothDevice);
            Log.d("ABC", "Connect Thread ended");
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
        private final ChatService chatService;
        private BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread(BluetoothAdapter bluetoothAdapter, ChatService chatService) {
            this.chatService = chatService;
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

            while (this.chatService.getState() != ChatService.STATE_CONNECTED) {
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    // TODO remove print stack
                    e.printStackTrace();
                    Log.i("ABC", "THIS IS EXPECTED IF CLOSING ACCEPT THREAD");
                    break;
                }

                if (bluetoothSocket != null) {
                    synchronized (this.chatService) {
                        switch (this.chatService.getState()) {
                            case ChatService.STATE_LISTEN:
                            case ChatService.STATE_CONNECTING:
                                this.chatService.connected(bluetoothSocket, bluetoothSocket.getRemoteDevice());
                                break;
                            case ChatService.STATE_CONNECTED:
                            case ChatService.STATE_NONE:
                                try {
                                    bluetoothSocket.close();
                                } catch (IOException e) {
                                    // TODO remove print stack
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                    Log.i("Accept Thread", "Connected:" + String.valueOf(bluetoothSocket.isConnected()));
                    Log.i("Accept Thread", "Remote:" + String.valueOf(bluetoothSocket.getRemoteDevice()));
                }
            }
            Log.i("ABC", "Accept Thread Ended");
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
        private ChatService chatService;

        public ConnectedThread(BluetoothSocket bluetoothSocket, ChatService chatService) {
            this.bluetoothSocket = bluetoothSocket;
            this.chatService = chatService;
        }

        @Override
        public void run() {
            Log.i("ABC", "ConnectedThread started");
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    Log.d("ABC", "Socket connected: " + this.bluetoothSocket.isConnected());

                    this.inputStream = this.bluetoothSocket.getInputStream();
                    bytes = this.inputStream.read(buffer);

                    this.chatService.messageReceived(new String(buffer, 0, bytes, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    // TODO remove print stack
                    e.printStackTrace();
                    Log.e("ABC", "ConnectedThread disconnected");

                    this.chatService.connectionLost();
                    break;
                }
            }

            try {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
            } catch (IOException e) {
                // TODO remove print stack
                e.printStackTrace();
            }
            Log.i("ABC", "ConnectedThread ended");
        }

        public void write(byte[] message) {
            try {
                this.outputStream = this.bluetoothSocket.getOutputStream();
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
