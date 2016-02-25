package edu.msu.team15.androidbluetoothchat;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static int REQUEST_BLUETOOTH = 1;

    public String bluetoothStatus;
    public String deviceName;
    public String bluetoothAddress;
    public String messageSent;
    private EditText messageBox;

    private Cloud.ChatService chatService;

    public String getMessageReceived() {
        return messageReceived;
    }

    public String messageReceived;

    public String getMessageSent() {
        return messageSent;
    }

    public void setBluetoothStatus(String bluetoothStatus) {
        this.bluetoothStatus = bluetoothStatus;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_BLUETOOTH);
            }
        }
        this.messageBox = (EditText) findViewById(R.id.editMessage);

        this.chatService = new Cloud.ChatService(this, bluetoothAdapter);
        this.chatService.start();
    }

    public void onScanForDevices(View view) {
        AvailableDevicesDialog availableDevicesDialog = new AvailableDevicesDialog();
        availableDevicesDialog.show(getFragmentManager(), "loadingDevices");
    }

    public void onSendMessage(View view) {
        String message = this.messageBox.getText().toString();
        this.chatService.sendMessage(message);
    }

    public void connect(BluetoothDevice device) {
        this.chatService.connect(device);
    }

    public void update(int state) {
        //TODO update UI
    }

    public void toast(final String message) {
        this.messageBox.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(messageBox.getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
