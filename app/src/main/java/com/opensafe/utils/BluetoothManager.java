package com.opensafe.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Button;

import androidx.core.app.ActivityCompat;

import com.opensafe.R;

public class BluetoothManager {
        private Context context;
        private BluetoothAdapter bluetoothAdapter;
        private BluetoothDevice connectedDevice;

        public BluetoothManager(Context context) {
            this.context = context;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        public void enableBluetooth() {
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                ((Activity) context).startActivityForResult(enableBtIntent, 1);
            }
        }

        public void connectToDevice(BluetoothDevice device) {
            // לוגיקה לחיבור למכשיר
            connectedDevice = device; // סימון המכשיר המחובר
            updateBluetoothButton(true);
        }

        public boolean isDeviceConnected() {
            return connectedDevice != null;
        }

        private void updateBluetoothButton(boolean isConnected) {
            Button bluetoothButton = ((Activity) context).findViewById(R.id.btn_bluetooth);
            if (isConnected) {
                bluetoothButton.setText("מכשיר מחובר");
                bluetoothButton.setEnabled(false); // לדוגמה, הפוך אותו ללא פעיל
            } else {
                bluetoothButton.setText("חיבור Bluetooth");
                bluetoothButton.setEnabled(true);
            }
        }
    }
