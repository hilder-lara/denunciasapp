package com.lara.denunciasapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lara.denunciasapp.R;
import com.lara.denunciasapp.model.Denuncia;
import com.lara.denunciasapp.network.ApiClient;

public class DetailActivity extends BaseActivity {
    public static final String EXTRA_ID = "denuncia_id";
    private int id;
    private Denuncia current;
    private boolean loading;
    private boolean changed;
    private View progress, content;
    private TextView status;
    private MaterialButton attend, retry;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_detail);
        setupScreen("Detalle de denuncia", true);
        id = getIntent().getIntExtra(EXTRA_ID, -1);
        progress = findViewById(R.id.progress);
        content = findViewById(R.id.detail_content);
        status = findViewById(R.id.status);
        attend = findViewById(R.id.attend_button);
        retry = findViewById(R.id.retry_button);
        retry.setOnClickListener(v -> load());
        attend.setOnClickListener(v -> confirmAttention());
        changed = state != null && state.getBoolean("changed");
        if (changed) setResult(RESULT_OK);
        if (id <= 0) {
            progress.setVisibility(View.GONE);
            status.setText("No se recibió un código de denuncia válido.");
        } else {
            load();
        }
    }

    private void load() {
        if (loading) return;
        setLoading(true);
        status.setText("Consultando denuncia…");
        request(ApiClient.get().obtener(id), denuncia -> {
            render(denuncia);
            setLoading(false);
            status.setText("Información actualizada desde la API.");
        }, this::showError);
    }

    private void render(Denuncia denuncia) {
        current = denuncia;
        content.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.detail_title)).setText("#" + denuncia.getId() + " · " + denuncia.getTipo());
        TextView state = findViewById(R.id.detail_state);
        state.setText(denuncia.getEstadoTexto() + " (estado " + denuncia.getEstado() + ")");
        state.setTextColor(getColor(denuncia.getEstado() == 1 ? R.color.pending : R.color.attended));
        ((TextView) findViewById(R.id.detail_dni)).setText(denuncia.getDni());
        ((TextView) findViewById(R.id.detail_date)).setText(DenunciaAdapter.formatDate(denuncia.getFecha()));
        ((TextView) findViewById(R.id.detail_address)).setText(denuncia.getDireccion());
        ((TextView) findViewById(R.id.detail_description)).setText(denuncia.getDescripcion());
        attend.setText(denuncia.getEstado() == 1 ? "Marcar como atendida" : "Denuncia atendida");
        attend.setEnabled(!loading && denuncia.getEstado() == 1);
    }

    private void confirmAttention() {
        if (loading || current == null || current.getEstado() == 0) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Atender denuncia #" + id)
                .setMessage("Confirma que el serenazgo atendió esta denuncia.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar atención", (dialog, which) -> attend())
                .show();
    }

    private void attend() {
        if (loading) return;
        setLoading(true);
        status.setText("Actualizando la atención…");
        request(ApiClient.get().atender(id), denuncia -> {
            render(denuncia);
            setLoading(false);
            changed = true;
            setResult(RESULT_OK);
            status.setText("Denuncia atendida. El nuevo estado se guardó en la base de datos.");
        }, this::showError);
    }

    private void setLoading(boolean value) {
        loading = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        attend.setEnabled(!value && current != null && current.getEstado() == 1);
        if (value) retry.setVisibility(View.GONE);
    }

    private void showError(String message) {
        setLoading(false);
        status.setText(message + " Pulsa Reintentar para consultar el estado guardado.");
        retry.setVisibility(View.VISIBLE);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("changed", changed);
        super.onSaveInstanceState(state);
    }
}
