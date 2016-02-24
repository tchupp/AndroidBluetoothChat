package edu.msu.team15.androidbluetoothchat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class AvailableDevicesDialog extends DialogFragment {
    private Cloud.AvailableDeviceReceiver availableDeviceReceiver;

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        getActivity().unregisterReceiver(availableDeviceReceiver);
        Log.d("ABC", "Unregistered Receiver from activity");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final Cloud cloud = new Cloud();
        final Cloud.ConnectThread connectThread = new Cloud.ConnectThread();


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Scanning for devices");
        builder.setCancelable(false);

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.devices_catalog, null);
        builder.setView(view);

        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        bluetoothAdapter.cancelDiscovery();
                        Log.d("ABC", "Bluetooth is discovering: " + bluetoothAdapter.isDiscovering());
                    }
                });

        AlertDialog dialog = builder.create();

        ListView list = (ListView) view.findViewById(R.id.listDevices);

        Cloud.AvailableDeviceAdapter adapter = new Cloud.AvailableDeviceAdapter(list, bluetoothAdapter);
        availableDeviceReceiver = new Cloud.AvailableDeviceReceiver(adapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(availableDeviceReceiver, filter);
        Log.d("ABC", "Registered Receiver to activity");
        Log.d("ABC", "Bluetooth is enabled: " + bluetoothAdapter.isEnabled());
        Log.d("ABC", "Bluetooth is discovering: " + bluetoothAdapter.isDiscovering());

        list.setAdapter(adapter);

        list.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();

                // TODO close dialog
                // TODO connect to device



                connectThread.connect();

            }
        });

        return dialog;
    }
}
