package com.example.rungirlrun.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rungirlrun.R;
import com.example.rungirlrun.models.SafetyReport;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private final List<SafetyReport> reports;
    private final Map<String, String> roadNamesById; // roadId -> display name

    public ReportAdapter(List<SafetyReport> reports, Map<String, String> roadNamesById) {
        this.reports = reports;
        this.roadNamesById = roadNamesById;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        SafetyReport report = reports.get(position);

        holder.reportTypeText.setText(formatReportType(report.getReportType().name()));

        String displayLocation;

        if (report.getLocationName() != null
                && !report.getLocationName().isEmpty()) {

            displayLocation =
                    report.getLocationName();

        } else {

            String roadName =
                    roadNamesById.get(
                            report.getRoadId()
                    );

            displayLocation =
                    roadName != null
                            ? roadName
                            : "Unknown location";
        }

        holder.roadNameText.setText(
                displayLocation
        );

        holder.timeAgoText.setText(formatTimeAgo(report.getTimestamp()));

        holder.nightBadge.setVisibility(report.isNight() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    private String formatReportType(String rawType) {
        String[] words = rawType.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(word.charAt(0))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        }
        return result.toString().trim();
    }

    private String formatTimeAgo(long timestamp) {
        long diffMillis = System.currentTimeMillis() - timestamp;

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else {
            return days + (days == 1 ? " day ago" : " days ago");
        }
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView reportTypeText;
        TextView roadNameText;
        TextView timeAgoText;
        TextView nightBadge;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            reportTypeText = itemView.findViewById(R.id.reportTypeText);
            roadNameText = itemView.findViewById(R.id.roadNameText);
            timeAgoText = itemView.findViewById(R.id.timeAgoText);
            nightBadge = itemView.findViewById(R.id.nightBadge);
        }
    }
}