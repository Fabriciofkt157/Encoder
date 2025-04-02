package com.fabriciofkt157.encoder;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Tradutor extends BaseActivity{
    FrameLayout frame_entrada, frame_saida;
    EditText edit_entrada;
    ImageButton btn_ok, btn_alternar;
    TextView tv_saida;
    String entrada, saida;
    ImageView tela_entrada;
    int modoBase = 1;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base64);
        frame_entrada = findViewById(R.id.frame_entrada);
        edit_entrada = findViewById(R.id.edit_entrada);
        btn_ok = findViewById(R.id.btn_ok);
        btn_alternar = findViewById(R.id.btn_alternar);
        frame_saida = findViewById(R.id.frame_saida);
        tv_saida = findViewById(R.id.tv_saida);
        tela_entrada = findViewById(R.id.tela_entrada);
        setupMenu();
        renderMenu(
                new ImageButton[]{ btn_ok },
                new TextView[]{ encoder, encoderM, tv_saida },
                new FrameLayout[]{ frame_entrada, frame_saida },
                new EditText[]{ edit_entrada }
        );
        btn_ok.setOnClickListener(v -> {
            botaoPressionado(btn_ok);
            entrada = edit_entrada.getText().toString();
            if(entrada.isEmpty()) Toast.makeText(this, "Digite algo primeiro!", Toast.LENGTH_SHORT).show();
            else {
                frame_saida.setAlpha(1f);
                if(modoBase == 1){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        tv_saida.setEnabled(true);
                        saida = Base64.getEncoder().encodeToString(entrada.getBytes(StandardCharsets.UTF_8));
                        String saidaFinal = saida;
                        if (saida.length() > 15){
                            saidaFinal = saida.substring(0, 35) + "...";
                        }
                        tv_saida.setText(saidaFinal);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (entrada == null || entrada.trim().isEmpty()) {
                            tv_saida.setText("Entrada vazia ou inválida!");
                        }
                        try {
                            byte[] decodedBytes = Base64.getDecoder().decode(entrada);
                            tv_saida.setText(new String(decodedBytes, StandardCharsets.UTF_8));
                        } catch (IllegalArgumentException e) {
                            entrada = null;
                            tv_saida.setText("Entrada inválida");
                        }
                    }
                }
            }
        });
        btn_alternar.setOnClickListener(v -> {
            botaoPressionado(btn_alternar);
            modoBase *= -1;
            tv_saida.setText("digite algo, primeiro ...");
            edit_entrada.setText("");
            tv_saida.setEnabled(false);
            frame_saida.setAlpha(0.65f);
            if(modoBase == 1) tela_entrada.setImageResource(R.drawable.base64_entrada);
            else tela_entrada.setImageResource(R.drawable.base64_traduzir);
        });
        tv_saida.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Texto em Base64", saida);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "Texto copiado para a área de transferência", Toast.LENGTH_SHORT).show();
        });
    }

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if(edit_entrada.getText().toString().isEmpty() && !menuAtivado) btn_ok.setAlpha(0.65f);
            else if(!menuAtivado) btn_ok.setAlpha(1f);

            if ((entrada == null || entrada.trim().isEmpty()) && !menuAtivado) {
                frame_saida.setAlpha(0.65f);
                tv_saida.setEnabled(false);
            }

            handler.postDelayed(this, 16);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }
}
