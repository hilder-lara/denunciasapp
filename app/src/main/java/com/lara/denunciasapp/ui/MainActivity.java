package com.lara.denunciasapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.lara.denunciasapp.BuildConfig;
import com.lara.denunciasapp.R;
import com.lara.denunciasapp.network.ApiClient;
import java.util.Collections;

public class MainActivity extends BaseActivity {
    private static final int PAGE_SIZE = 5;
    private DenunciaAdapter adapter;
    private EditText searchDni;
    private TextView status, listTitle;
    private MaterialButton searchButton, refreshButton, allButton, moreButton, retryButton;
    private View progress;
    private boolean loading;
    private boolean pendingReload;
    private boolean canLoadMore = true;
    private String activeDni;
    private Runnable retryAction;

    private final ActivityResultLauncher<Intent> details = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> reload());

    private final ActivityResultLauncher<Intent> registration = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Abre la denuncia creada aunque no aparezca entre las cinco primeras.
                    if (result.getData() != null) {
                        int id = result.getData().getIntExtra(DetailActivity.EXTRA_ID, -1);
                        if (id > 0) {
                            openDetail(id);
                            return;
                        }
                    }
                    reload();
                }
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        setupScreen("Serenazgo · Lara", false);
        searchDni = findViewById(R.id.search_dni);
        status = findViewById(R.id.status);
        listTitle = findViewById(R.id.list_title);
        progress = findViewById(R.id.progress);
        searchButton = findViewById(R.id.search_button);
        refreshButton = findViewById(R.id.refresh_button);
        allButton = findViewById(R.id.all_button);
        moreButton = findViewById(R.id.load_more);
        retryButton = findViewById(R.id.retry_button);
        ((TextView) findViewById(R.id.api_url)).setText("Servicio: " + BuildConfig.API_BASE_URL);

        RecyclerView recycler = findViewById(R.id.recycler);
        adapter = new DenunciaAdapter(denuncia -> { if (!loading) openDetail(denuncia.getId()); });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        searchButton.setOnClickListener(v -> search());
        searchDni.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });
        refreshButton.setOnClickListener(v -> reload());
        moreButton.setOnClickListener(v -> loadPage(false));
        retryButton.setOnClickListener(v -> { if (retryAction != null) retryAction.run(); });
        allButton.setOnClickListener(v -> {
            activeDni = null;
            searchDni.setText("");
            adapter.replace(Collections.emptyList());
            reload();
        });
        findViewById(R.id.new_button).setOnClickListener(v ->
                registration.launch(new Intent(this, RegisterActivity.class)));
        if (state != null) activeDni = state.getString("activeDni");
        reload();
    }

    private void openDetail(int id) {
        details.launch(new Intent(this, DetailActivity.class).putExtra(DetailActivity.EXTRA_ID, id));
    }

    private void search() {
        if (loading) return;
        String dni = searchDni.getText().toString().trim();
        if (!dni.matches("[0-9]{8}")) {
            searchDni.setError("Ingresa exactamente 8 dígitos.");
            return;
        }
        activeDni = dni;
        adapter.replace(Collections.emptyList());
        reload();
    }

    private void reload() {
        if (loading) {
            // Si el detalle vuelve mientras hay una consulta anterior, no se pierde el refresco.
            pendingReload = true;
            return;
        }
        pendingReload = false;
        if (activeDni == null) {
            loadPage(true);
            return;
        }
        retryAction = this::reload;
        setLoading(true);
        request(ApiClient.get().buscarPorDni(activeDni), rows -> {
            adapter.replace(rows);
            setLoading(false);
            status.setText(rows.isEmpty() ? "No hay denuncias para este DNI."
                    : rows.size() + " denuncia(s) del ciudadano. Toca una para ver el detalle.");
            runPendingReload();
        }, this::showError);
    }

    private void loadPage(boolean reset) {
        if (loading || activeDni != null || (!reset && !canLoadMore)) return;
        // offset cuenta registros omitidos: 0, 5, 10...; no es un numero de pagina.
        int offset = reset ? 0 : adapter.getItemCount();
        retryAction = () -> loadPage(reset);
        setLoading(true);
        request(ApiClient.get().listar(offset, PAGE_SIZE), rows -> {
            if (reset) adapter.replace(rows); else adapter.append(rows);
            canLoadMore = rows.size() == PAGE_SIZE;
            setLoading(false);
            if (adapter.getItemCount() == 0) {
                status.setText("Aún no hay denuncias. Registra la primera.");
            } else {
                status.setText(adapter.getItemCount() + " denuncia(s) cargada(s). Toca una para ver el detalle."
                        + (canLoadMore ? "" : " Llegaste al final."));
            }
            runPendingReload();
        }, this::showError);
    }

    private void setLoading(boolean value) {
        loading = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!value);
        findViewById(R.id.new_button).setEnabled(!value);
        refreshButton.setEnabled(!value);
        allButton.setEnabled(!value);
        searchDni.setEnabled(!value);
        moreButton.setVisibility(activeDni == null ? View.VISIBLE : View.GONE);
        moreButton.setEnabled(!value && canLoadMore);
        moreButton.setText(canLoadMore ? "Cargar 5 siguientes" : "Todas las denuncias cargadas");
        allButton.setVisibility(activeDni == null ? View.GONE : View.VISIBLE);
        listTitle.setText(activeDni == null ? "Todas las denuncias" : "Denuncias del DNI " + activeDni);
        if (value) {
            retryButton.setVisibility(View.GONE);
            status.setText("Consultando denuncias… La primera conexión con Render puede tardar un minuto.");
        }
    }

    private void showError(String message) {
        setLoading(false);
        status.setText(message);
        retryButton.setVisibility(View.VISIBLE);
        runPendingReload();
    }

    private void runPendingReload() {
        if (pendingReload && !loading) reload();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("activeDni", activeDni);
        super.onSaveInstanceState(state);
    }
}
