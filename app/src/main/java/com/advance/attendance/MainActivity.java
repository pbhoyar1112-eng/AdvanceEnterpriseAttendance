package com.advance.attendance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Duty point: Shalinitai Meghe Hospital & Research Centre, Wanadongri, Hingna, Nagpur
    private static final double DUTY_POINT_LAT = 21.0943;
    private static final double DUTY_POINT_LNG = 78.97464;
    private static final float GEOFENCE_RADIUS_METERS = 50f;

    private TextView statusText;
    private ImageView punchPhoto;
    private Uri photoUri;
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && photoUri != null) {
                    punchPhoto.setImageURI(photoUri);
                    checkLocationAndConfirmPunch();
                } else {
                    statusText.setText("Photo capture cancelled.");
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean locationGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                if (cameraGranted && locationGranted) {
                    launchCamera();
                } else {
                    statusText.setText("Camera and location permissions are required to punch.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        punchPhoto = findViewById(R.id.punchPhoto);
        Button punchButton = findViewById(R.id.punchButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        punchButton.setOnClickListener(v -> handlePunch());
    }

    private void handlePunch() {
        if (hasRequiredPermissions()) {
            launchCamera();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }

    private boolean hasRequiredPermissions() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean locationGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return cameraGranted && locationGranted;
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(
                    this,
                    "com.advance.attendance.fileprovider",
                    photoFile);
            takePictureLauncher.launch(photoUri);
        } catch (IOException e) {
            statusText.setText("Could not create photo file.");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "PUNCH_" + timeStamp;
        File storageDir = getExternalFilesDir("Pictures");
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private void checkLocationAndConfirmPunch() {
        statusText.setText("Checking location...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            statusText.setText("Location permission missing.");
            return;
        }

        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        statusText.setText("Could not get location. Enable GPS and try again.");
                        return;
                    }

                    float[] results = new float[1];
                    Location.distanceBetween(
                            location.getLatitude(), location.getLongitude(),
                            DUTY_POINT_LAT, DUTY_POINT_LNG,
                            results);
                    float distanceMeters = results[0];

                    if (distanceMeters <= GEOFENCE_RADIUS_METERS) {
                        statusText.setText(String.format(Locale.getDefault(),
                                "Punch recorded at duty point (%.0fm away). Location service started.",
                                distanceMeters));
                        startLocationService();
                    } else {
                        statusText.setText(String.format(Locale.getDefault(),
                                "Punch rejected: you are %.0fm away from the duty point (limit %.0fm).",
                                distanceMeters, GEOFENCE_RADIUS_METERS));
                    }
                })
                .addOnFailureListener(e -> statusText.setText("Location error: " + e.getMessage()));
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
