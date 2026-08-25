package com.advance.attendance;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HistoryActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "attendance_records";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        TextView historyList = findViewById(R.id.historyList);
        historyList.setText(buildHistoryText());
    }

    private String buildHistoryText() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int count = prefs.getInt("record_count", 0);

        if (count == 0) {
            return "No punch records yet.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = count - 1; i >= 0; i--) {
            String record = prefs.getString("record_" + i, null);
            if (record != null) {
                builder.append(record).append("\n\n");
            }
        }
        return builder.toString();
    }
}
