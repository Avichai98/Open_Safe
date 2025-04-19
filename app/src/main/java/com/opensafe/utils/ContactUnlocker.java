package com.opensafe.utils;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.activity.result.ActivityResultLauncher;

public class ContactUnlocker {

    private final ActivityResultLauncher<Intent> addContactLauncher;
    private final String phoneNumber;

    public ContactUnlocker(ActivityResultLauncher<Intent> launcher) {
        this.addContactLauncher = launcher;
        phoneNumber = "0500000000";
    }

    public void startUnlockFlow() {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        addContactLauncher.launch(intent);
    }

    public boolean contactExists(Activity activity, String number) {
        if (number != null) {
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            String[] mPhoneNumberProjection = { ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER, ContactsContract.PhoneLookup.DISPLAY_NAME };
            try (Cursor cur = activity.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null)) {
                assert cur != null;
                if (cur.moveToFirst()) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }// contactExists

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
