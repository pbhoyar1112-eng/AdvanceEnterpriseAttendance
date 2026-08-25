# Advance Enterprise Attendance – Android App

This starter project implements the main requirements from the supplied PRD:
- Payroll cycle: 21st–20th
- Shift 1: 07:00–15:00
- Shift 2: 15:00–23:00
- Shift 3: 23:00–07:00 next day
- Camera/face-punch entry point
- GPS capture on punch
- Live-location foreground service starter
- Full / Half-Day / Absent rule
- 15-minute grace / late-mark concept
- Duty-point geofence target of about 50 m
- Attendance dashboard

## Build
Open this folder in Android Studio and let Gradle sync. Then:
Build > Build APK(s)

The project uses Android Gradle Plugin 8.6.1 and compileSdk 35.

## Production work still required
1. Add approved duty-point latitude/longitude and server-side geofence validation.
2. Integrate a real liveness/face verification SDK or backend (Firebase ML Kit/AWS Rekognition as approved).
3. Add PostgreSQL/MySQL/Node.js/FastAPI backend and secure login.
4. Implement server cron for the 21st–20th payroll cycle and the exact 15-minute late/penalty policy.
5. Add employee/admin roles, reports, monthly attendance export, audit logs and encrypted API authentication.
6. Test background-location behaviour against current Android permission policies before production release.

\n## Build APK yourself on GitHub (phone se)
1. GitHub par account/login karein.
2. New repository banayein.
3. Is ZIP ke andar ke sabhi files repository me upload karein (ZIP ko repository me sirf ek file ke roop me upload na karein).
4. Actions tab kholen.
5. "Build Android APK" workflow select karke "Run workflow" dabayein.
6. Build complete hone ke baad workflow run ke bottom me **Artifacts** me `AdvanceEnterpriseAttendance-debug-apk` milega.
7. Artifact download karke ZIP extract karein; andar `app-debug.apk` milega.

Note: Debug APK testing ke liye hai. Play Store release ke liye signed AAB/APK alag se banana hoga.
