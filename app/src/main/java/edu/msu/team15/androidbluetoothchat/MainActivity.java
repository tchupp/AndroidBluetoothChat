package edu.msu.team15.androidbluetoothchat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    public String bluetoothStatus;
    public String deviceName;
    public String bluetootherAddress;
    public String messageSent;

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

    public void setBluetootherAddress(String bluetootherAddress) {
        this.bluetootherAddress = bluetootherAddress;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button scanButton = (Button) findViewById(R.id.scanButton);

        //b committed, b b committed
    }



}
