package com.example.rungirlrun.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import com.example.rungirlrun.R;
import com.example.rungirlrun.models.PoliceStation;

import java.util.List;
import java.util.Locale;

public class PoliceStationAdapter extends RecyclerView.Adapter<PoliceStationAdapter.ViewHolder> {

    private final List<PoliceStation> stations;

    public PoliceStationAdapter(List<PoliceStation> stations) {
        this.stations = stations;
    }

    public void updateStations(List<PoliceStation> newStations) {
        stations.clear();
        stations.addAll(newStations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_police_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PoliceStation station = stations.get(position);

        holder.name.setText(station.getName());

        float km = station.getDistanceMeters() / 1000f;
        holder.distance.setText(String.format(Locale.getDefault(), "%.1f km away", km));

        boolean notified = "notified".equals(station.getStatus());

        holder.status.setText(notified ? "Sent \u2713" : "Notifying...");
        holder.status.setTextColor(
                ContextCompat.getColor(
                        holder.itemView.getContext(),
                        notified ? R.color.lavender_deep : R.color.sparkle_pink
                )
        );

        holder.icon.setImageResource(notified ? R.drawable.ic_shield : R.drawable.ic_sparkle);
        holder.icon.setColorFilter(
                ContextCompat.getColor(
                        holder.itemView.getContext(),
                        notified ? R.color.lavender_deep : R.color.lavender_primary
                )
        );
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView name;
        TextView distance;
        TextView status;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.statusIcon);
            name = itemView.findViewById(R.id.stationName);
            distance = itemView.findViewById(R.id.stationDistance);
            status = itemView.findViewById(R.id.stationStatus);
        }
    }
}