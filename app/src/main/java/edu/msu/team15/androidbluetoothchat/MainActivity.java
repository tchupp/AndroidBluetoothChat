package edu.msu.team15.androidbluetoothchat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onScanForDevices(View view) {
        LoadingDevicesDialog loadingDevicesDialog = new LoadingDevicesDialog();
        loadingDevicesDialog.show(getFragmentManager(), "loadingDevices");
    }
}
