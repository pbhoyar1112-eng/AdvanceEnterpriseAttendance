package com.advanceenterprise.attendance;

import android.app.*;
import android.content.*;
import android.os.*;
import android.location.*;
import android.content.pm.ServiceInfo;

public class LocationService extends Service {
    @Override public void onCreate(){
        super.onCreate();
        String ch="attendance_location";
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel nc=new NotificationChannel(ch,"Attendance Location",NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(nc);
        }
        Notification n=new Notification.Builder(this, ch)
            .setContentTitle("Advance Enterprise Attendance")
            .setContentText("Live location tracking is active")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation).build();
        if(Build.VERSION.SDK_INT>=29) startForeground(7,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        else startForeground(7,n);
    }
    @Override public int onStartCommand(Intent i,int f,int s){ return START_STICKY; }
    @Override public android.os.IBinder onBind(Intent i){ return null; }
}
