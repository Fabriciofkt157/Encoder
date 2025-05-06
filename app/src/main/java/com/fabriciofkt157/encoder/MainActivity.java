package com.fabriciofkt157.encoder;

import static android.widget.Toast.makeText;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {


    TextView btn_selecionar_criptografar, btn_selecionar_descriptografar, btn_selecionar_base64, btn_selecionar_sha256, btn_selecionar_comparador;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Esconde a action bar e altera a cor da barra de status
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.cinza_background));

        File reg_version = new File(getFilesDir(), "reg_version");
        String versao = "0.15.1";
        StringBuilder builder = new StringBuilder();
        String linha;
        if(reg_version.exists()){
            try (FileInputStream fis = new FileInputStream(reg_version);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {


                while ((linha = reader.readLine()) != null) {
                    builder.append(linha).append("\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(!builder.toString().equals(versao)){
                try (FileOutputStream fos = new FileOutputStream(reg_version);
                     OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                     BufferedWriter writer = new BufferedWriter(osw)) {
                    writer.write("0.15.1");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            try (FileOutputStream fos = new FileOutputStream(reg_version);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {
                writer.write("0.15.1");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        btn_selecionar_criptografar = findViewById(R.id.btn_selecionar_criptografar);
        btn_selecionar_descriptografar = findViewById(R.id.btn_selecionar_descriptografar);
        btn_selecionar_base64 = findViewById(R.id.btn_selecionar_base64);
        btn_selecionar_comparador = findViewById(R.id.btn_selecionar_comparador);
        btn_selecionar_sha256 = findViewById(R.id.btn_selecionar_sha256);

        btn_selecionar_criptografar.setOnClickListener(v -> {
            botaoPressionado(btn_selecionar_criptografar);
            Intent intent = new Intent(this, Criptografar.class);
            startActivity(intent);
            finish();
        });
        btn_selecionar_descriptografar.setOnClickListener(v -> {
            botaoPressionado(btn_selecionar_descriptografar);
            Intent intent = new Intent(this, Descriptografar.class);
            startActivity(intent);
            finish();
        });
        btn_selecionar_base64.setOnClickListener(v -> {
            botaoPressionado(btn_selecionar_base64);
            Intent intent = new Intent(this, Base64Tradutor.class);
            startActivity(intent);
            finish();
        });
        btn_selecionar_sha256.setOnClickListener(v -> {
            botaoPressionado(btn_selecionar_sha256);
            Intent intent = new Intent(this, SHA256.class);
            startActivity(intent);
            finish();
        });
        btn_selecionar_comparador.setOnClickListener(v -> {
            botaoPressionado(btn_selecionar_comparador);
            Intent intent = new Intent(this, ComparadorFn.class);
            startActivity(intent);
            finish();
        });
    }

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, 16);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateRunnable); // Inicia a atualização
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable); // Para a atualização quando sair da tela
    }

    @SuppressLint("ClickableViewAccessibility")
    private void botaoPressionado(TextView btn){
        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    btn.setAlpha(0.5f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    btn.setAlpha(1.0f);
                    break;
                default:
                    break;
            }
            return false;
        });
    }
}
