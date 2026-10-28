package com.lara.denunciasapp.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.lara.denunciasapp.R;
import com.lara.denunciasapp.model.DenunciaRequest;
import com.lara.denunciasapp.network.ApiClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RegisterActivity extends BaseActivity {
    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private EditText dni, date, address, description;
    private Spinner type;
    private TextView status;
    private MaterialButton save;
    private View progress;
    private LocalDateTime selectedDate;
    private boolean saving;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_register);
        setupScreen("Nueva denuncia", true);
        dni = findViewById(R.id.input_dni);
        date = findViewById(R.id.input_date);
        address = findViewById(R.id.input_address);
        description = findViewById(R.id.input_description);
        type = findViewById(R.id.input_type);
        status = findViewById(R.id.status);
        save = findViewById(R.id.save_button);
        progress = findViewById(R.id.progress);
        selectedDate = state != null && state.containsKey("date")
                ? LocalDateTime.parse(state.getString("date")) : LocalDateTime.now().withSecond(0).withNano(0);
        updateDate();
        date.setOnClickListener(v -> chooseDate());
        save.setOnClickListener(v -> save());
        if (state != null && state.getBoolean("wasSaving")) {
            status.setText("Se interrumpió la espera de la respuesta. Antes de volver a registrar, "
                    + "regresa a la lista y busca tu DNI para comprobar si la denuncia ya se guardó.");
        }
    }

    private void chooseDate() {
        if (saving) return;
        new DatePickerDialog(this, (picker, year, month, day) ->
                new TimePickerDialog(this, (time, hour, minute) -> {
                    selectedDate = LocalDateTime.of(year, month + 1, day, hour, minute);
                    updateDate();
                }, selectedDate.getHour(), selectedDate.getMinute(), true).show(),
                selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth()).show();
    }

    private void updateDate() {
        date.setText(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }

    private void save() {
        if (saving) return;
        String dniValue = dni.getText().toString().trim();
        String addressValue = address.getText().toString().trim();
        String descriptionValue = description.getText().toString().trim();
        boolean valid = true;
        if (!dniValue.matches("[0-9]{8}")) {
            dni.setError("El DNI debe contener exactamente 8 dígitos.");
            valid = false;
        }
        if (addressValue.isEmpty() || addressValue.length() > 200) {
            address.setError("Ingresa una dirección de hasta 200 caracteres.");
            valid = false;
        }
        if (descriptionValue.isEmpty() || descriptionValue.length() > 255) {
            description.setError("Ingresa una descripción de hasta 255 caracteres.");
            valid = false;
        }
        if (!valid) return;

        DenunciaRequest payload = new DenunciaRequest(dniValue, selectedDate.format(API_DATE),
                type.getSelectedItem().toString(), addressValue, descriptionValue);
        setSaving(true);
        request(ApiClient.get().registrar(payload), denuncia -> {
            setSaving(false);
            Toast.makeText(this, "Denuncia #" + denuncia.getId() + " registrada como pendiente.", Toast.LENGTH_LONG).show();
            setResult(RESULT_OK, new Intent().putExtra(DetailActivity.EXTRA_ID, denuncia.getId().intValue()));
            finish();
        }, message -> {
            setSaving(false);
            status.setText(message + " Si se interrumpió la conexión, consulta primero tu DNI en la lista "
                    + "para evitar registrar dos veces la misma denuncia.");
        });
    }

    private void setSaving(boolean value) {
        saving = value;
        dni.setEnabled(!value);
        date.setEnabled(!value);
        address.setEnabled(!value);
        description.setEnabled(!value);
        type.setEnabled(!value);
        save.setEnabled(!value);
        save.setText(value ? "Registrando…" : "Registrar denuncia");
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        if (value) status.setText("Enviando denuncia. Espera la confirmación del servidor.");
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("date", selectedDate.format(API_DATE));
        state.putBoolean("wasSaving", saving);
        super.onSaveInstanceState(state);
    }
}
