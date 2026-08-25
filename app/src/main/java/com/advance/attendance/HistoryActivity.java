package com.advance.attendance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "attendance_records";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        TextView summaryText = findViewById(R.id.summaryText);
        TextView historyList = findViewById(R.id.historyList);
        Button shareButton = findViewById(R.id.shareButton);

        List<String> rawRecords = loadRawRecords();

        summaryText.setText(AttendanceHelper.buildMonthlySummary(rawRecords) + "\n\n" +
                "Daily breakdown:\n" + AttendanceHelper.buildDailySummary(rawRecords));

        historyList.setText(buildHistoryText(rawRecords));

        shareButton.setOnClickListener(v -> shareRecords(rawRecords));
    }

    private List<String> loadRawRecords() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int count = prefs.getInt("record_count", 0);
        List<String> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String record = prefs.getString("record_" + i, null);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private String buildHistoryText(List<String> rawRecords) {
        if (rawRecords.isEmpty()) {
            return "No punch records yet.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = rawRecords.size() - 1; i >= 0; i--) {
            builder.append(rawRecords.get(i)).append("\n\n");
        }
        return builder.toString();
    }

    private void shareRecords(List<String> rawRecords) {
        StringBuilder builder = new StringBuilder();
        builder.append("Advance Enterprise Attendance - Export\n\n");
        builder.append(AttendanceHelper.buildMonthlySummary(rawRecords)).append("\n\n");
        builder.append("All records:\n");
        for (String record : rawRecords) {
            builder.append(record).append("\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Records Export");
        shareIntent.putExtra(Intent.EXTRA_TEXT, builder.toString());
        startActivity(Intent.createChooser(shareIntent, "Share attendance records via"));
    }
}
