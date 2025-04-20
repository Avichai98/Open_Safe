package com.opensafe.utils;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactUnlocker {
    private final Context context;
    private final ContactsObserver contactsObserver;

    private class ContactsObserver extends ContentObserver {
        public ContactsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d("ContactUnlocker", "Contact list changed! Unlocking...");
            onContactChanged();
        }
    }

    public ContactUnlocker(Context context) {
        this.context = context;
        this.contactsObserver = new ContactsObserver(new Handler());

        context.getContentResolver().registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contactsObserver
        );
    }

    public void startUnlockFlow() {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        context.startActivity(intent);
    }

    public void unregisterObserver() {
        context.getContentResolver().unregisterContentObserver(contactsObserver);
    }

    public void onContactChanged() {
        if (context instanceof UnlockCallback) {
            ((UnlockCallback) context).onUnlockDetected();
        }
    }

    public interface UnlockCallback {
        void onUnlockDetected();
    }
}