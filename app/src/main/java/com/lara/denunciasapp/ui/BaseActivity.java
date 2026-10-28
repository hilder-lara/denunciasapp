package com.lara.denunciasapp.ui;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.lara.denunciasapp.R;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Utilidades compartidas de las tres pantallas: margenes del sistema y llamadas asincronas. */
public abstract class BaseActivity extends AppCompatActivity {
    private final List<Call<?>> calls = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(false);
    }

    protected void setupScreen(String title, boolean back) {
        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(back);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    protected <T> void request(Call<T> call, Consumer<T> success, Consumer<String> error) {
        calls.add(call);
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> current, Response<T> response) {
                calls.remove(current);
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    success.accept(response.body());
                } else {
                    String message = "El servidor respondio HTTP " + response.code() + ".";
                    try {
                        if (response.errorBody() != null) {
                            JSONObject json = new JSONObject(response.errorBody().string());
                            message = json.optString("detail",
                                    json.optString("message", json.optString("mensaje", message)));
                            JSONObject errors = json.optJSONObject("errors");
                            if (errors != null) {
                                StringBuilder fields = new StringBuilder(message);
                                Iterator<String> keys = errors.keys();
                                while (keys.hasNext()) {
                                    String field = keys.next();
                                    fields.append("\n").append(field).append(": ").append(errors.optString(field));
                                }
                                message = fields.toString();
                            }
                        }
                    } catch (Exception ignored) { /* Conserva un error legible ante una respuesta HTML. */ }
                    error.accept(message);
                }
            }

            @Override public void onFailure(Call<T> current, Throwable failure) {
                calls.remove(current);
                if (current.isCanceled() || isFinishing() || isDestroyed()) return;
                error.accept("No se pudo conectar. Revisa tu conexion y la URL de la API. "
                        + "Render puede tardar cerca de un minuto en iniciar.");
            }
        });
    }

    @Override protected void onDestroy() {
        for (Call<?> call : calls) call.cancel();
        calls.clear();
        super.onDestroy();
    }
}
