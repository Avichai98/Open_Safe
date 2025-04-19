package com.opensafe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.opensafe.databinding.ActivityMainBinding;
import com.opensafe.utils.BluetoothUnlocker;
import com.opensafe.utils.ContactUnlocker;
import com.opensafe.utils.ImageUnlocker;
import com.opensafe.utils.NfcUnlocker;
import com.opensafe.utils.SensorUnlocker;

import java.util.Locale;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity implements SensorUnlocker.MagneticFieldChangeListener {

    private ActivityMainBinding binding;

    private BluetoothUnlocker bluetoothUnlocker;
    private ImageUnlocker imageUnlocker;
    private ContactUnlocker contactUnlocker;
    private SensorUnlocker sensorUnlocker;
    private NfcUnlocker nfcUnlocker;

    private final String BLUETOOTH_PERMISSION = Manifest.permission.BLUETOOTH_CONNECT;
    private final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private final String GALLERY_PERMISSION = Manifest.permission.READ_MEDIA_IMAGES;
    private final String[] CONTACTS_PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
    };

    //private ActivityResultLauncher<Intent> bluetoothLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> addContactLauncher;
    private ActivityResultLauncher<String[]> permissionsLauncher;

    private String pendingAction = null; // Stores the action to perform after permission is granted

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupLaunchers(); // Initialize ActivityResultLaunchers

        // Initialize unlocker classes
        bluetoothUnlocker = new BluetoothUnlocker(this);
        contactUnlocker = new ContactUnlocker(addContactLauncher);
        imageUnlocker = new ImageUnlocker(this);
        sensorUnlocker = new SensorUnlocker(this);
        sensorUnlocker.setMagneticFieldChangeListener(this); // Set the listener for magnetic field changes
        nfcUnlocker = new NfcUnlocker(this);

        if (getIntent() != null) {
            handleNfcIntent(getIntent());
        }

        binding.btnBluetooth.setOnClickListener(v -> {
            pendingAction = "bluetooth";
            // Check if Bluetooth permission is granted
            if (ContextCompat.checkSelfPermission(this, BLUETOOTH_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                permissionsLauncher.launch(new String[]{BLUETOOTH_PERMISSION}); // Request Bluetooth permission
            } else {
                startBluetoothUnlock();
                openBluetoothSettings();
            }
        });

        binding.btnCamera.setOnClickListener(v -> requestCameraOrGalleryPermission("camera"));
        binding.btnContact.setOnClickListener(v -> requestContactsPermissionAndStart());
        binding.btnMagnitude.setOnClickListener(v -> {
            // Check if the current magnetic field magnitude is above the threshold
            if (sensorUnlocker.getCurrentMagneticFieldMagnitude() >= 19) {
                binding.btnMagnitude.setBackgroundResource(R.drawable.opened_lock); // Unlock UI
            } else {
                updateMagnitude();
            }
        });
        binding.btnNfc.setOnClickListener(v -> {
            if (!nfcUnlocker.isNfcEnabled && nfcUnlocker.nfcAdapter != null) {
                Toast.makeText(this, "NFC is not enabled. Please enable it in settings.", Toast.LENGTH_LONG).show();
                Intent enableNfcIntent = new Intent(Settings.ACTION_NFC_SETTINGS);
                this.startActivity(enableNfcIntent);
            }
            Toast.makeText(this, "Please scan an NFC tag.", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNfcIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothUnlocker.stopListening();
        sensorUnlocker.stopListening(); // Stop listening for sensors when activity is destroyed
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap photo = (Bitmap) Objects.requireNonNull(result.getData().getExtras()).get("data");
                        imageUnlocker.analyzeImage(photo);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                            imageUnlocker.analyzeImage(bitmap);
                        } catch (Exception e) {
                            Log.e("MainActivity", "Error loading image", e);
                        }
                    }
                });

        addContactLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (contactUnlocker.contactExists(this, contactUnlocker.getPhoneNumber()))
                        binding.btnContact.setBackgroundResource(R.drawable.opened_lock);
                }
        );

        permissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!Boolean.TRUE.equals(granted)) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        switch (pendingAction) {
                            case "camera":
                                launchCamera();
                                break;
                            case "gallery":
                                launchGallery();
                                break;
                            case "contacts":
                                checkAndHandleContact();
                                break;
                            case "bluetooth":
                                startBluetoothUnlock();
                                openBluetoothSettings();
                                break;
                            default:
                        }
                    } else {
                        showSettingsDialog(); // Show dialog if permissions are not granted
                    }
                    pendingAction = null; // Reset pending action
                });
    }

    private void handleNfcIntent(Intent intent) {
        if (nfcUnlocker.handleNfcIntent(intent))
            runOnUiThread(() -> binding.btnNfc.setBackgroundResource(R.drawable.opened_lock));
    }

    private void updateMagnitude() {
        // Update the text view with the current magnetic field strength
        binding.magnitudeText.setText(String.format(Locale.US, "%.2f µT", sensorUnlocker.getCurrentMagneticFieldMagnitude()));
        sensorUnlocker.startListening(); // Start listening for magnetic field changes
    }

    @Override
    public void onMagneticFieldChanged(float magnitude) {
        runOnUiThread(() -> {
            // Update the text view with the current magnetic field strength
            binding.magnitudeText.setText(String.format(Locale.US, "%.2f µT", sensorUnlocker.getCurrentMagneticFieldMagnitude()));
            // Check if the magnetic field magnitude is above the unlock threshold
            if (magnitude >= 60) { // You might need to adjust this threshold based on your magnet
                binding.btnMagnitude.setBackgroundResource(R.drawable.opened_lock); // Unlock UI
                sensorUnlocker.stopListening(); // Stop listening for further changes
            }
        });
    }

    private void startBluetoothUnlock() {
        bluetoothUnlocker.startListening(() ->
                runOnUiThread(() ->
                        binding.btnBluetooth.setBackgroundResource(R.drawable.opened_lock)
                ));
    }

    private void openBluetoothSettings() {
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    private void requestCameraOrGalleryPermission(String action) {
        pendingAction = action;
        // Check if camera or gallery permissions are granted
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, GALLERY_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            permissionsLauncher.launch(new String[]{CAMERA_PERMISSION, GALLERY_PERMISSION}); // Request permissions
        } else {
            if ("camera".equals(action)) {
                launchCamera();
            } else {
                launchGallery();
            }
        }
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void launchGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void requestContactsPermissionAndStart() {
        pendingAction = "contacts";
        // Check if read and write contacts permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsLauncher.launch(CONTACTS_PERMISSIONS); // Request contacts permissions
        } else {
            checkAndHandleContact();
        }
    }

    private void checkAndHandleContact() {
        String phoneNumber = "0500000000";
        if (contactUnlocker.contactExists(this, phoneNumber)) {
            binding.btnContact.setBackgroundResource(R.drawable.opened_lock);
        } else {
            contactUnlocker.startUnlockFlow();
        }
    }

    private void showSettingsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Missing Permissions")
                .setMessage("Please allow permissions in settings")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}