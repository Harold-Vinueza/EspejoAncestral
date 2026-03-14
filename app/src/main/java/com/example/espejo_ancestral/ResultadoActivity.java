package com.example.espejo_ancestral;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultadoActivity extends AppCompatActivity {

    TextView txtPuebloGanador, txtTop3, txtRasgos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultado);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtPuebloGanador = findViewById(R.id.txtPuebloGanador);
        txtTop3 = findViewById(R.id.txtTop3);
        txtRasgos = findViewById(R.id.txtRasgos);

        String resultado = getIntent().getStringExtra("resultado");

        if (resultado != null) {

            String[] lineas = resultado.split("\n");

            StringBuilder ganador = new StringBuilder();
            StringBuilder top3 = new StringBuilder();
            StringBuilder rasgos = new StringBuilder();

            boolean seccionTop3 = false;
            boolean seccionRasgos = false;

            for (String linea : lineas) {

                if (linea.contains("Otros perfiles posibles")) {
                    seccionTop3 = true;
                    seccionRasgos = false;
                }

                if (linea.contains("Rostro") || linea.contains("Ojos") || linea.contains("Simetría")) {
                    seccionTop3 = false;
                    seccionRasgos = true;
                }

                if (!seccionTop3 && !seccionRasgos) {
                    ganador.append(linea).append("\n");
                } else if (seccionTop3) {
                    top3.append(linea).append("\n");
                } else if (seccionRasgos) {
                    rasgos.append(linea).append("\n");
                }
            }

            txtPuebloGanador.setText(ganador.toString());
            txtTop3.setText(top3.toString());
            txtRasgos.setText(rasgos.toString());
        }


    }

    public void volverPantallaPrincipal(View view) {
        finish();
    }
}