package com.opensafe.activities;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.opensafe.R;
import com.opensafe.databinding.ActivityMainBinding;
import com.opensafe.utils.BluetoothUnlocker;
import com.opensafe.utils.ContactUnlocker;
import com.opensafe.utils.ImageUnlocker;
import com.opensafe.utils.NfcUnlocker;
import com.opensafe.utils.SensorUnlocker;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity implements SensorUnlocker.MagneticFieldChangeListener, ContactUnlocker.UnlockCallback {

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

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String[]> permissionsLauncher;

    private String pendingAction = null; // Stores the action to perform after permission is granted

    private boolean bluetoothUnlocked;
    private boolean cameraUnlocked;
    private boolean contactUnlocked;
    private boolean magnitudeUnlocked;
    private boolean nfcUnlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupLaunchers(); // Initialize ActivityResultLaunchers

        // Initialize unlocker classes
        bluetoothUnlocker = new BluetoothUnlocker(this);
        contactUnlocker = new ContactUnlocker(this);

        imageUnlocker = new ImageUnlocker(this, detectedObject -> runOnUiThread(() -> {
            Toast.makeText(this, detectedObject + " detected! Unlocking...", Toast.LENGTH_SHORT).show();
            binding.btnCamera.setBackgroundResource(R.drawable.opened_lock); // Change UI when food is detected
            this.cameraUnlocked = true;
            if (checksAllLocks()) {
                openNewActivity();
            }

        }));

        sensorUnlocker = new SensorUnlocker(this);
        sensorUnlocker.setMagneticFieldChangeListener(this); // Set the listener for magnetic field changes
        nfcUnlocker = new NfcUnlocker(this);

        if (getIntent() != null) {
            handleNfcIntent(getIntent());
        }

        binding.btnBluetooth.setOnClickListener(v -> requestPermission("bluetooth"));

        binding.btnCamera.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Choose Image Source") // Title for the selection dialog
                .setItems(new String[]{"Camera", "Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        requestPermission("camera"); // Calls camera permission check
                    } else {
                        requestPermission("gallery"); // Calls gallery permission check
                    }
                })
                .show());

        binding.btnContact.setOnClickListener(v -> requestPermission("contacts"));
        binding.btnMagnitude.setOnClickListener(v -> {
            // Check if the current magnetic field magnitude is above the threshold
            if (sensorUnlocker.getCurrentMagneticFieldMagnitude() >= 60) {
                binding.btnMagnitude.setBackgroundResource(R.drawable.opened_lock); // Unlock UI
                this.magnitudeUnlocked = true;
                if (checksAllLocks()) {
                    openNewActivity();
                }
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
        setIntent(intent);
        handleNfcIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcUnlocker.enableForegroundDispatch(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcUnlocker.disableForegroundDispatch(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothUnlocker.stopListening();
        sensorUnlocker.stopListening(); // Stop listening for sensors when activity is destroyed
        contactUnlocker.unregisterObserver();
    }

    private void setupLaunchers() {
        // Camera activity result
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        assert result.getData() != null;
                        Bitmap imageBitmap = (Bitmap) Objects.requireNonNull(result.getData().getExtras()).get("data");
                        if (imageBitmap != null) {
                            imageUnlocker.analyzeImage(imageBitmap);
                        }
                    }
                });

        // Gallery activity result
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        assert result.getData() != null;
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            imageUnlocker.analyzeImage(imageBitmap);
                        } catch (IOException e) {
                            Log.e("MainActivity", "Error processing image", e);
                        }
                    }
                });

        permissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean allGranted = true;
                    boolean shouldShowRationale = false;

                    for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                        String permission = entry.getKey();
                        Boolean granted = entry.getValue();

                        if (!Boolean.TRUE.equals(granted)) {
                            allGranted = false;

                            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                                shouldShowRationale = true;
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
                                contactUnlocker.startUnlockFlow();
                                break;
                            case "bluetooth":
                                startBluetoothUnlock();
                                openBluetoothSettings();
                                break;
                        }
                    } else if (shouldShowRationale) {
                        showPermissionRationaleDialog(); // Shows explanation, then triggers the permission request again
                    } else {
                        showSettingsDialog(); // Open app settings
                    }

                    pendingAction = null;
                });
    }

    private void handleNfcIntent(Intent intent) {
        if (nfcUnlocker.handleNfcIntent(intent)) {
            runOnUiThread(() -> binding.btnNfc.setBackgroundResource(R.drawable.opened_lock));
            this.nfcUnlocked = true;
            if (checksAllLocks()) {
                openNewActivity();
            }
        }
    }

    @Override
    public void onUnlockDetected() {
        runOnUiThread(() -> {
            Log.d("MainActivity", "Contact modified or added! Unlocking...");
            binding.btnContact.setBackgroundResource(R.drawable.opened_lock);
            contactUnlocked = true;
            Toast.makeText(this, "Contact updated! Lock opened!", Toast.LENGTH_SHORT).show();

            if (checksAllLocks()) {
                openNewActivity();
            }
        });
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
                this.magnitudeUnlocked = true;
                if (checksAllLocks()) {
                    openNewActivity();
                }
                sensorUnlocker.stopListening(); // Stop listening for further changes
            }
        });
    }

    private void startBluetoothUnlock() {
        bluetoothUnlocker.startListening(() ->
                runOnUiThread(() -> {
                    binding.btnBluetooth.setBackgroundResource(R.drawable.opened_lock);
                    bluetoothUnlocked = true;
                    if (checksAllLocks()) {
                        openNewActivity();
                    }
                }));
    }


    private void openBluetoothSettings() {
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    private void requestPermission(String action) {
        pendingAction = action;

        switch (action) {
            case "camera":
                if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsLauncher.launch(new String[]{CAMERA_PERMISSION}); // Request camera permission
                } else {
                    launchCamera();
                }
                break;
            case "gallery":
                if (ContextCompat.checkSelfPermission(this, GALLERY_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsLauncher.launch(new String[]{GALLERY_PERMISSION}); // Request gallery permissions
                } else {
                    launchGallery();
                }
                break;
            case "contacts":
                if (ContextCompat.checkSelfPermission(this, CONTACTS_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, CONTACTS_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED) {
                    permissionsLauncher.launch(CONTACTS_PERMISSIONS); // Request contacts permissions
                } else {
                    contactUnlocker.startUnlockFlow();
                }
                break;
            case "bluetooth":
                if (ContextCompat.checkSelfPermission(this, BLUETOOTH_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsLauncher.launch(new String[]{BLUETOOTH_PERMISSION}); // Request Bluetooth permission
                } else {
                    startBluetoothUnlock();
                    openBluetoothSettings();
                }
            default:
                Log.e("MainActivity", "Invalid action: " + action);
                break;
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

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This permission is necessary for the feature to work properly.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Do nothing, just close the dialog
                })
                .setCancelable(false)
                .show();
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

    private boolean checksAllLocks() {
        return bluetoothUnlocked && cameraUnlocked && contactUnlocked && magnitudeUnlocked && nfcUnlocked;
    }

    private void openNewActivity() {
        Intent intent = new Intent(this, SafeUnlockActivity.class);
        startActivity(intent);
        finish();
    }
}