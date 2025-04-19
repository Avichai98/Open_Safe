package com.opensafe.utils;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class BluetoothUnlocker {

    private final Context context;
    private boolean isUnlocked = false;
    private UnlockListener listener;

    public interface UnlockListener {
        void onUnlock();
    }

    public BluetoothUnlocker(Context context) {
        this.context = context;
    }

    public void startListening(UnlockListener listener) {
        this.listener = listener;

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);

        context.registerReceiver(broadcastReceiver, filter);
    }

    public void stopListening() {
        try {
            context.unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                if (!isUnlocked && device != null) {
                    Log.d("BluetoothUnlocker", "מכשיר התחבר: " + device.getName());
                    isUnlocked = true;
                    if (listener != null) {
                        listener.onUnlock();
                    }
                }
            }
        }
    };
}
