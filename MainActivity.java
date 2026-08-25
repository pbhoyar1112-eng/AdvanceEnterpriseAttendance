package com.advanceenterprise.attendance;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    TextView dateText, status, times, locationText;
    Spinner shiftSpinner;
    long punchIn = 0L, punchOut = 0L;
    String[] shifts = {"Shift 1: 07:00–15:00 (Morning)", "Shift 2: 15:00–23:00 (Evening)", "Shift 3: 23:00–07:00 (Night / Cross-Over)"};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        dateText=findViewById(R.id.dateText); status=findViewById(R.id.status);
        times=findViewById(R.id.times); locationText=findViewById(R.id.locationText);
        shiftSpinner=findViewById(R.id.shiftSpinner);
        shiftSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, shifts));
        dateText.setText("Today: " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date()));

        findViewById(R.id.cameraBtn).setOnClickListener(v -> openCamera());
        findViewById(R.id.punchInBtn).setOnClickListener(v -> punch(true));
        findViewById(R.id.punchOutBtn).setOnClickListener(v -> punch(false));
        findViewById(R.id.locationBtn).setOnClickListener(v -> getLocation());
        findViewById(R.id.liveBtn).setOnClickListener(v -> toggleLive());
    }

    void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 10);
            Toast.makeText(this,"Camera permission requested.",Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) != null) startActivityForResult(i, 20);
    }

    void punch(boolean in) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 11);
            Toast.makeText(this,"Location permission required for punch.",Toast.LENGTH_LONG).show();
            return;
        }
        long now=System.currentTimeMillis();
        if(in){ punchIn=now; status.setText("Status: Punched IN • " + fmt(now)); }
        else {
            if(punchIn==0){ Toast.makeText(this,"Please Punch IN first.",Toast.LENGTH_SHORT).show(); return; }
            punchOut=now; status.setText("Status: Punched OUT • " + fmt(now));
            long hrs=(punchOut-punchIn)/3600000L;
            String day = hrs>=8 ? "FULL PRESENT" : (hrs>=4 ? "HALF-DAY" : "ABSENT");
            times.setText("In: "+fmt(punchIn)+"\nOut: "+fmt(punchOut)+"\nWorking: "+hrs+" hr\nAttendance: "+day);
        }
        getLocation();
    }

    String fmt(long t){return new SimpleDateFormat("hh:mm a",Locale.getDefault()).format(new Date(t));}

    void getLocation(){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},11); return;
        }
        LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
        Location l=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(l==null) l=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(l!=null) locationText.setText(String.format(Locale.US,"Current GPS: %.6f, %.6f\nGeofence target: configure approved Duty Point • radius ~50 m",l.getLatitude(),l.getLongitude()));
        else locationText.setText("GPS fix unavailable. Please enable Location and try again.");
    }

    void toggleLive(){
        Intent i=new Intent(this,LocationService.class);
        if(android.os.Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
        Toast.makeText(this,"Live location tracking service started.",Toast.LENGTH_LONG).show();
    }
}
