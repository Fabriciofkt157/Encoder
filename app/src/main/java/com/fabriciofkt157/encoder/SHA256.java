package com.fabriciofkt157.encoder;

import static android.widget.Toast.makeText;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SHA256 extends BaseActivity{
    ImageButton btnCenter, btnSelecionarArquivos, btnSelecionarModo, btn_ok, btn_copiar, btn_lixeira;
    TextView tv_nome_arquivo;
    FrameLayout frame_hash;
    List<Uri> urisArquivosSelecionados = new ArrayList<>(), urisPastasSelecionadas = new ArrayList<>();
    Map<String, String> mapsHash256 = new HashMap<>();
    TextView tv_hash;
    boolean arquivos = false;
    ActivityManager activityManager;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sha256);

        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        btnCenter = findViewById(R.id.btn_center);
        tv_nome_arquivo = findViewById(R.id.nome_arquivo);
        btnSelecionarArquivos = findViewById(R.id.btn_select_files);
        btnSelecionarModo = findViewById(R.id.selecionar_modo);
        frame_hash = findViewById(R.id.frame_hash);
        tv_hash = findViewById(R.id.tv_hashes);
        btn_ok = findViewById(R.id.btn_ok);
        btn_copiar = findViewById(R.id.btn_copiar);
        btn_lixeira = findViewById(R.id.lixeira);

        setupMenu();
        renderMenu(
                new ImageButton[]{ btnSelecionarArquivos, btnSelecionarModo, btnCenter, btn_ok, btn_lixeira },
                new TextView[]{ encoder, encoderM, tv_nome_arquivo },
                new FrameLayout[]{ frame_hash },
                null
        );
        btnSelecionarArquivos.setOnClickListener(v -> {
            botaoPressionado(btnSelecionarArquivos);

            if(modo == 0) {
                selecionarArquivos(uris -> {
                    urisArquivosSelecionados.addAll(uris);
                    atualizarEstadoArquivos(urisArquivosSelecionados, urisPastasSelecionadas, tv_nome_arquivo);
                    arquivos = true;
                });
            }
            else {
                makeText(this, "Selecione a pasta que deseja gerar o hash", Toast.LENGTH_SHORT).show();
                selecionarPasta(false, uri -> {
                    urisPastasSelecionadas.add(uri);
                    atualizarEstadoArquivos(urisArquivosSelecionados, urisPastasSelecionadas, tv_nome_arquivo);
                    arquivos = true;
                });
            }
        });
        btnSelecionarModo.setOnClickListener(v -> {
            botaoPressionado(btnSelecionarModo);
            if (modo == 1) {
                btnSelecionarModo.setBackgroundResource(R.drawable.arquivo);
                modo = 0;
            } else {
                btnSelecionarModo.setBackgroundResource(R.drawable.pasta);
                modo = 1;
            }
        });
        if(!arquivos) btnCenter.setAlpha(0.7f);
        else btnCenter.setAlpha(1f);
        btnCenter.setOnClickListener(v -> {
            botaoPressionado(btnCenter);
            gerarSHA();
        });

        btn_lixeira.setOnClickListener(v -> {
            arquivos = false;
            urisArquivosSelecionados.clear();
            urisPastasSelecionadas.clear();
            tv_nome_arquivo.setText("nenhum arquivo carregado");
            makeText(this, "Os arquivos selecionados foram removidos.", Toast.LENGTH_SHORT).show();
        });

        btn_ok.setOnClickListener(v -> {
            btnCenter.setVisibility(View.VISIBLE);
            btn_ok.setVisibility(View.INVISIBLE);
            btnSelecionarArquivos.setEnabled(true);
            btnSelecionarArquivos.setAlpha(1f);
            btnSelecionarModo.setEnabled(true);
            btnSelecionarModo.setAlpha(1f);
            btn_lixeira.setEnabled(true);
            btn_lixeira.setAlpha(1f);
            frame_hash.setVisibility(View.INVISIBLE);
            mapsHash256 = new HashMap<>();
        });
        btn_copiar.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Hashes", tv_hash.getText());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "Códigos hashes copiados para área de transferência", Toast.LENGTH_SHORT).show();
        });
    }

    @SuppressLint("SetTextI18n")
    public void gerarSHA(){
        try{
            Map<String, byte[]> caminho_dados = new HashMap<>();
            for(Uri uri : urisArquivosSelecionados){
                caminho_dados.put(nomeArquivoPorUri(uri), obterBytesDeUri(uri));
            }
            for(Uri uri : urisPastasSelecionadas){
                caminho_dados.putAll(capturarEstruturaPasta(uri));
            }
            for(Map.Entry<String, byte[]> entry : caminho_dados.entrySet()){
                String caminho = entry.getKey();
                byte[] dados = entry.getValue();
                try {
                    mapsHash256.put(caminho, calcularSHA256(dados));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
            StringBuilder hashes = new StringBuilder();

            for (Map.Entry<String, String> entry : mapsHash256.entrySet()) {
                hashes.append(entry.getKey()).append(":\n\n").append(entry.getValue()).append("\n\n");
            }
            tv_hash.setText(hashes.toString());
            btnCenter.setVisibility(View.INVISIBLE);
            btn_ok.setVisibility(View.VISIBLE);
            btnSelecionarArquivos.setEnabled(false);
            btnSelecionarArquivos.setAlpha(0.25f);
            btnSelecionarModo.setEnabled(false);
            btnSelecionarModo.setAlpha(0.25f);
            btn_lixeira.setEnabled(false);
            btn_lixeira.setAlpha(0.25f);
            frame_hash.setVisibility(View.VISIBLE);
        } catch (OutOfMemoryError e){
            urisArquivosSelecionados.clear();
            urisPastasSelecionadas.clear();
            tv_nome_arquivo.setText("nenhum arquivo carregado");
            new AlertDialog.Builder(this)
                    .setTitle("Erro: alocação de memória")
                    .setMessage(
                            "Seus arquivos são grandes demais para o processamento. \n" +
                                    "Atualmente, o limite é " + activityManager.getLargeMemoryClass() + "MB. Estou trabalhando para contornar esse problema. \n" +
                                    "A seleção de arquivos foi limpa. Tente um de cada vez, caso tenha selecionado mais de um arquivo.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNeutralButton("Ok", null)
                    .show();
        }


    }


    public String calcularSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(data);

        byte[] hashBytes = digest.digest();

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println(arquivos);
            if (!arquivos) {
                btn_lixeira.setAlpha(0.25f);
                btn_lixeira.setEnabled(false);

                if (btn_menu_mode == 0) {
                    btnCenter.setAlpha(0.65f);
                }
            } else {
                btn_lixeira.setAlpha(1f);
                btn_lixeira.setEnabled(true);

                if (btn_menu_mode == 1) {
                    btnCenter.setAlpha(0.25f);
                } else if (btn_menu_mode == 0) {
                    btnCenter.setAlpha(1f);
                }
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
