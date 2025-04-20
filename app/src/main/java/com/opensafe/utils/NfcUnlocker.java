package com.opensafe.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;

public class NfcUnlocker {
    private NfcManager nfcManager;
    public NfcAdapter nfcAdapter;
    public boolean isNfcEnabled;
    private Context context;

    public NfcUnlocker(Context context) {
        this.context = context;
        nfcManager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        nfcAdapter = nfcManager != null ? nfcManager.getDefaultAdapter() : null;
        isNfcEnabled = nfcAdapter != null && nfcAdapter.isEnabled();
    }

    // Handle NFC intent actions
    public boolean handleNfcIntent(Intent intent) {
        return NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction());
    }

    // Enable NFC foreground dispatch to prevent activity restart
    public void enableForegroundDispatch(Activity activity) {
        if (nfcAdapter == null) return;

        Intent intent = new Intent(activity.getApplicationContext(), activity.getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                activity.getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, null);
    }

    // Disable NFC foreground dispatch when activity is paused
    public void disableForegroundDispatch(Activity activity) {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(activity);
        }
    }
}
