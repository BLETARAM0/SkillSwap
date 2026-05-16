package aram.kocharyan.skillswap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import java.util.List;

public class HomeMatchAdapter extends RecyclerView.Adapter<HomeMatchAdapter.MatchViewHolder> {

    private final List<AppUser> matchList;
    private final OnMatchClickListener listener;

    public HomeMatchAdapter(List<AppUser> matchList, OnMatchClickListener listener) {
        this.matchList = matchList;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        AppUser user = matchList.get(position);

        // Имя
        holder.tvName.setText(user.name + " " + user.surname);

        // Локация
        if ("offline".equals(user.mode)
                && user.city != null && !user.city.isEmpty()
                && user.country != null && !user.country.isEmpty()) {
            holder.tvLocation.setText("📍 " + user.city + ", " + user.country);
        } else {
            holder.tvLocation.setText("🌐 Online");
        }

        // Язык
        if (user.language != null && !user.language.isEmpty()) {
            holder.tvLanguage.setText("🗣 " + user.language);
            holder.tvLanguage.setVisibility(View.VISIBLE);
        } else {
            holder.tvLanguage.setVisibility(View.GONE);
        }

        // Скиллы — чипы
        holder.tvTeach.setText("Teaches: " + shortSkill(user.skillTeach));
        holder.tvStudy.setText("Learns: " + shortSkill(user.skillStudy));

        // Аватарка
        if (user.avatarUrl != null && !user.avatarUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.avatarUrl)
                    .transform(new CircleCrop())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        holder.itemView.setOnClickListener(v -> listener.onMatchClick(user));
    }

    /** "Mathematics Grade 10" → "Math G10" */
    private String shortSkill(String skill) {
        if (skill == null) return "";
        String[] parts = skill.split(" ");
        if (parts.length >= 3) {
            String subj = parts[0].length() > 4 ? parts[0].substring(0, 4) : parts[0];
            return subj + " " + parts[2];
        }
        return skill.length() > 12 ? skill.substring(0, 12) : skill;
    }

    @Override
    public int getItemCount() { return matchList.size(); }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView  tvName, tvLocation, tvLanguage, tvTeach, tvStudy;
        ImageView ivAvatar;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName     = itemView.findViewById(R.id.tvName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvLanguage = itemView.findViewById(R.id.tvLanguage);
            tvTeach    = itemView.findViewById(R.id.tvTeach);
            tvStudy    = itemView.findViewById(R.id.tvStudy);
            ivAvatar   = itemView.findViewById(R.id.ivAvatar);
        }
    }

    public interface OnMatchClickListener {
        void onMatchClick(AppUser user);
    }
}