package com.example.rungirlrun.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rungirlrun.R;
import com.example.rungirlrun.models.SafetyTip;

import java.util.ArrayList;
import java.util.List;


public class SafetyTipAdapter extends RecyclerView.Adapter<SafetyTipAdapter.TipViewHolder> {


    private List<SafetyTip> tips = new ArrayList<>();

    private OnTipActionListener listener;

    private String currentUserId;


    public interface OnTipActionListener {

        void onEdit(SafetyTip tip);

        void onDelete(SafetyTip tip);

    }


    public SafetyTipAdapter(String currentUserId, OnTipActionListener listener) {

        this.currentUserId = currentUserId;

        this.listener = listener;

    }


    public void setTips(List<SafetyTip> newTips) {

        this.tips = new ArrayList<>(newTips);

        notifyDataSetChanged();

    }



    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_safety_tip, parent, false);

        return new TipViewHolder(view);

    }



    @Override
    public void onBindViewHolder(@NonNull TipViewHolder holder, int position) {


        SafetyTip tip = tips.get(position);



        holder.tvTipText.setText(tip.getTipText());


        holder.tvAuthorName.setText(
                tip.isOfficial()
                        ? "RunGirlRun Team"
                        : tip.getAuthorName()
        );


        holder.tvOfficialBadge.setVisibility(
                tip.isOfficial()
                        ? View.VISIBLE
                        : View.GONE
        );


        holder.tvCategory.setText(
                "Category: " + tip.getCategory()
        );



        // Show buttons only for current user's own tips
        if (!tip.isOfficial()
                && tip.getAuthorId() != null
                && tip.getAuthorId().equals(currentUserId)) {


            holder.btnEditTip.setVisibility(View.VISIBLE);

            holder.btnDeleteTip.setVisibility(View.VISIBLE);


        } else {

            holder.btnEditTip.setVisibility(View.GONE);
            holder.btnDeleteTip.setVisibility(View.GONE);

            holder.btnEditTip.setOnClickListener(null);
            holder.btnDeleteTip.setOnClickListener(null);



        }




        holder.btnEditTip.setOnClickListener(v -> {

            if(listener != null){

                listener.onEdit(tip);

            }

        });



        holder.btnDeleteTip.setOnClickListener(v -> {

            if(listener != null){

                listener.onDelete(tip);

            }

        });


    }




    @Override
    public int getItemCount() {

        return tips.size();

    }





    static class TipViewHolder extends RecyclerView.ViewHolder {


        TextView tvTipText;
        TextView tvAuthorName;
        TextView tvOfficialBadge;
        TextView tvCategory;

        Button btnEditTip;
        Button btnDeleteTip;



        TipViewHolder(View itemView) {

            super(itemView);


            tvTipText = itemView.findViewById(R.id.tvTipText);

            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);

            tvOfficialBadge = itemView.findViewById(R.id.tvOfficialBadge);

            tvCategory = itemView.findViewById(R.id.tvCategory);


            btnEditTip = itemView.findViewById(R.id.btnEditTip);

            btnDeleteTip = itemView.findViewById(R.id.btnDeleteTip);

        }

    }

}