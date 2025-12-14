package cl.yotta.yotta;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SniAdapter extends RecyclerView.Adapter<SniAdapter.SniViewHolder> {
    private final List<String> sniList;

    public SniAdapter(List<String> sniList) {
        this.sniList = sniList;
    }

    @Override
    public SniViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sni, parent, false);
        return new SniViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SniViewHolder holder, int position) {
        holder.textView.setText(sniList.get(position));
    }

    @Override
    public int getItemCount() {
        return sniList.size();
    }

    static class SniViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        SniViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textSni);
        }
    }
}

