package com.lara.denunciasapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.lara.denunciasapp.R;
import com.lara.denunciasapp.model.Denuncia;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DenunciaAdapter extends RecyclerView.Adapter<DenunciaAdapter.Holder> {
    private final List<Denuncia> rows = new ArrayList<>();
    private final Consumer<Denuncia> onClick;

    public DenunciaAdapter(Consumer<Denuncia> onClick) {
        this.onClick = onClick;
        setHasStableIds(true);
    }

    public void replace(List<Denuncia> items) {
        rows.clear();
        rows.addAll(items);
        notifyDataSetChanged();
    }

    public void append(List<Denuncia> items) {
        int start = rows.size();
        rows.addAll(items);
        notifyItemRangeInserted(start, items.size());
    }

    @Override public long getItemId(int position) { return rows.get(position).getId(); }
    @Override public int getItemCount() { return rows.size(); }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_denuncia, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Denuncia denuncia = rows.get(position);
        holder.title.setText("#" + denuncia.getId() + " · " + denuncia.getTipo());
        holder.state.setText(denuncia.getEstadoTexto());
        holder.state.setTextColor(holder.itemView.getContext().getColor(
                denuncia.getEstado() == 1 ? R.color.pending : R.color.attended));
        holder.address.setText(denuncia.getDireccion());
        holder.meta.setText("DNI " + denuncia.getDni() + " · " + formatDate(denuncia.getFecha()));
        holder.itemView.setOnClickListener(v -> onClick.accept(denuncia));
    }

    public static String formatDate(String value) {
        if (value == null) return "";
        try {
            return LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception ignored) {
            return value;
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title, state, address, meta;
        Holder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.row_title);
            state = itemView.findViewById(R.id.row_state);
            address = itemView.findViewById(R.id.row_address);
            meta = itemView.findViewById(R.id.row_meta);
        }
    }
}
