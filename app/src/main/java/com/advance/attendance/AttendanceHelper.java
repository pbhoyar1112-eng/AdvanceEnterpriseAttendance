package com.advance.attendance;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceHelper {

    public static class PunchRecord {
        Date dateTime;
        String dateKey;
        String shift;
        String punctuality;
        String status;
        float distanceMeters;
        String payrollPeriod;
    }

    private static final SimpleDateFormat FULL_FORMAT =
            new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DATE_ONLY_FORMAT =
            new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    public static PunchRecord parse(String line) {
        if (line == null) return null;
        String[] parts = line.split("\\|");
        if (parts.length < 6) return null;

        try {
            PunchRecord record = new PunchRecord();
            record.dateTime = FULL_FORMAT.parse(parts[0].trim());
            record.dateKey = DATE_ONLY_FORMAT.format(record.dateTime);
            record.shift = parts[1].trim();
            record.punctuality = parts[2].trim();
            record.status = parts[3].trim();
            record.distanceMeters = Float.parseFloat(parts[4].trim().replace("m", ""));
            record.payrollPeriod = parts[5].replace("Payroll:", "").trim();
            return record;
        } catch (ParseException | NumberFormatException e) {
            return null;
        }
    }

    public static Map<String, List<PunchRecord>> groupByDate(List<String> rawRecords) {
        Map<String, List<PunchRecord>> grouped = new LinkedHashMap<>();
        for (String raw : rawRecords) {
            PunchRecord record = parse(raw);
            if (record == null) continue;
            grouped.computeIfAbsent(record.dateKey, k -> new ArrayList<>()).add(record);
        }
        return grouped;
    }

    public static String dayStatus(List<PunchRecord> dayRecords) {
        List<PunchRecord> validPunches = new ArrayList<>();
        for (PunchRecord r : dayRecords) {
            if ("VALID".equals(r.status)) {
                validPunches.add(r);
            }
        }

        if (validPunches.isEmpty()) {
            return "ABSENT";
        }

        Collections.sort(validPunches, Comparator.comparing(r -> r.dateTime));

        if (validPunches.size() == 1) {
            return "HALF DAY (single punch)";
        }

        Date first = validPunches.get(0).dateTime;
        Date last = validPunches.get(validPunches.size() - 1).dateTime;
        double hours = (last.getTime() - first.getTime()) / (1000.0 * 60 * 60);

        if (hours >= 6) {
            return String.format(Locale.getDefault(), "FULL DAY (%.1f hrs)", hours);
        } else if (hours >= 3) {
            return String.format(Locale.getDefault(), "HALF DAY (%.1f hrs)", hours);
        } else {
            return String.format(Locale.getDefault(), "HALF DAY (%.1f hrs, short)", hours);
        }
    }

    public static String buildDailySummary(List<String> rawRecords) {
        Map<String, List<PunchRecord>> grouped = groupByDate(rawRecords);
        if (grouped.isEmpty()) {
            return "No records to summarize.";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<PunchRecord>> entry : grouped.entrySet()) {
            String status = dayStatus(entry.getValue());
            int lateCount = 0;
            for (PunchRecord r : entry.getValue()) {
                if (r.punctuality != null && r.punctuality.startsWith("LATE")) {
                    lateCount++;
                }
            }
            sb.append(entry.getKey()).append(" -> ").append(status);
            if (lateCount > 0) {
                sb.append(" (").append(lateCount).append(" late punch(es))");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String buildMonthlySummary(List<String> rawRecords) {
        Map<String, List<PunchRecord>> grouped = groupByDate(rawRecords);
        if (grouped.isEmpty()) {
            return "No records for this period.";
        }

        int fullDays = 0;
        int halfDays = 0;
        int absentDays = 0;
        int lateCount = 0;
        String payrollPeriod = null;

        for (Map.Entry<String, List<PunchRecord>> entry : grouped.entrySet()) {
            String status = dayStatus(entry.getValue());
            if (status.startsWith("FULL")) {
                fullDays++;
            } else if (status.startsWith("HALF")) {
                halfDays++;
            } else {
                absentDays++;
            }
            for (PunchRecord r : entry.getValue()) {
                if (r.punctuality != null && r.punctuality.startsWith("LATE")) {
                    lateCount++;
                }
                if (payrollPeriod == null) {
                    payrollPeriod = r.payrollPeriod;
                }
            }
        }

        return "Payroll period: " + payrollPeriod + "\n" +
                "Full days: " + fullDays + "\n" +
                "Half days: " + halfDays + "\n" +
                "Absent/incomplete days: " + absentDays + "\n" +
                "Late punches: " + lateCount + "\n" +
                "Total days with activity: " + grouped.size();
    }
}
