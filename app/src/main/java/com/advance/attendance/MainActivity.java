        package com.advance.attendance;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Duty point: current registered location, Wanadongri, Nagpur
    private static final double DUTY_POINT_LAT = 21.0926149;
    private static final double DUTY_POINT_LNG = 78.9714733;
    private static final float GEOFENCE_RADIUS_METERS = 50f;
    private static final int GRACE_PERIOD_MINUTES = 15;
    private static final String PREFS_NAME = "attendance_records";

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
        Button historyButton = findViewById(R.id.historyButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        punchButton.setOnClickListener(v -> handlePunch());
        historyButton.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

        showLastRecord();
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

                    ShiftInfo shiftInfo = detectShiftAndPunctuality();
                    String payrollPeriod = detectPayrollPeriod();
                    String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

                    if (distanceMeters <= GEOFENCE_RADIUS_METERS) {
                        String message = String.format(Locale.getDefault(),
                                "Punch recorded at duty point (%.0fm away).\n%s | %s\nPayroll period: %s",
                                distanceMeters, shiftInfo.shiftLabel, shiftInfo.punctualityLabel, payrollPeriod);
                        statusText.setText(message);
                        saveRecord(timestamp, shiftInfo, "VALID", distanceMeters, payrollPeriod);
                        startLocationService();
                    } else {
                        String message = String.format(Locale.getDefault(),
                                "Punch rejected: you are %.0fm away from the duty point (limit %.0fm).",
                                distanceMeters, GEOFENCE_RADIUS_METERS);
                        statusText.setText(message);
                        saveRecord(timestamp, shiftInfo, "REJECTED_LOCATION", distanceMeters, payrollPeriod);
                    }
                })
                .addOnFailureListener(e -> statusText.setText("Location error: " + e.getMessage()));
    }

    private static class ShiftInfo {
        String shiftLabel;
        String punctualityLabel;
    }

    private ShiftInfo detectShiftAndPunctuality() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int minutesNow = hour * 60 + minute;

        int shiftStartMinutes;
        String shiftName;

        if (hour >= 7 && hour < 15) {
            shiftName = "Shift 1 (07:00-15:00)";
            shiftStartMinutes = 7 * 60;
        } else if (hour >= 15 && hour < 23) {
            shiftName = "Shift 2 (15:00-23:00)";
            shiftStartMinutes = 15 * 60;
        } else {
            shiftName = "Shift 3 (23:00-07:00)";
            shiftStartMinutes = 23 * 60;
            if (hour < 7) {
                minutesNow += 24 * 60;
            }
        }

        int lateMinutes = minutesNow - shiftStartMinutes;

        ShiftInfo info = new ShiftInfo();
        info.shiftLabel = shiftName;

        if (lateMinutes <= GRACE_PERIOD_MINUTES) {
            info.punctualityLabel = "ON TIME";
        } else {
            info.punctualityLabel = "LATE by " + (lateMinutes - GRACE_PERIOD_MINUTES) + " min (grace " + GRACE_PERIOD_MINUTES + " min used)";
        }

        return info;
    }

    private String detectPayrollPeriod() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();

        if (day >= 21) {
            startCal.set(year, month, 21);
            endCal.set(year, month + 1, 20);
        } else {
            startCal.set(year, month - 1, 21);
            endCal.set(year, month, 20);
        }

        SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        return fmt.format(startCal.getTime()) + " to " + fmt.format(endCal.getTime());
    }

    private void saveRecord(String timestamp, ShiftInfo shiftInfo, String status, float distanceMeters, String payrollPeriod) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int count = prefs.getInt("record_count", 0);

        String record = timestamp + " | " + shiftInfo.shiftLabel + " | " + shiftInfo.punctualityLabel +
                " | " + status + String.format(Locale.getDefault(), " | %.0fm", distanceMeters) +
                " | Payroll: " + payrollPeriod;

        prefs.edit()
                .putString("record_" + count, record)
                .putInt("record_count", count + 1)
                .putString("last_record", record)
                .apply();
    }

    private void showLastRecord() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastRecord = prefs.getString("last_record", null);
        if (lastRecord != null) {
            statusText.setText("Last punch: " + lastRecord);
        }
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
