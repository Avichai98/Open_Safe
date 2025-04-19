package com.opensafe.utils;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;


public class NfcUnlocker {
    private NfcManager nfcManager;
    public NfcAdapter nfcAdapter;
    public boolean isNfcEnabled = false;
    private Context context;

    public NfcUnlocker(Context context) {
        this.context = context;
        nfcManager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        nfcAdapter = nfcManager.getDefaultAdapter();
        isNfcEnabled = nfcAdapter != null && nfcAdapter.isEnabled();
    }

    public boolean handleNfcIntent(Intent intent) {
        return NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction());
    }
}