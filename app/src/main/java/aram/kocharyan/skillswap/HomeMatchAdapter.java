package aram.kocharyan.skillswap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HomeMatchAdapter extends RecyclerView.Adapter<HomeMatchAdapter.MatchViewHolder> {

    private final List<AppUser> matchList;
    private final OnMatchClickListener listener;

    public HomeMatchAdapter(List<AppUser> matchList, OnMatchClickListener listener) {
        this.matchList = matchList;
        this.listener = listener;
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

        holder.tvName.setText(user.name + " " + user.surname);

        String location = (user.country != null && user.city != null) ?
                user.country + ", " + user.city : "Online";
        holder.tvLocation.setText(location);

        String skills = "Teach: " + user.skillTeach + "\nLearn: " + user.skillStudy;
        holder.tvSkills.setText(skills);

        holder.itemView.setOnClickListener(v -> listener.onMatchClick(user));
    }

    @Override
    public int getItemCount() {
        return matchList.size();
    }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLocation, tvSkills;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvSkills = itemView.findViewById(R.id.tvSkills);
        }
    }

    public interface OnMatchClickListener {
        void onMatchClick(AppUser user);
    }
}