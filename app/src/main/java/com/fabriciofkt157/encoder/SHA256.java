package com.fabriciofkt157.encoder;

import static android.widget.Toast.makeText;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SHA256 extends BaseActivity{
    ImageButton btnCenter, btnSelecionarArquivos, btnSelecionarModo, btn_ok, btn_copiar, btn_lixeira;
    TextView tv_nome_arquivo;
    FrameLayout frame_hash, frame_aguarde;
    ProgressBar progressBar;
    List<Uri> urisArquivosSelecionados = new ArrayList<>(), urisPastasSelecionadas = new ArrayList<>();
    Map<String, String> mapsHash256 = new HashMap<>();
    TextView tv_hash;
    boolean arquivos = false, calculando = false;
    StringBuilder hashes;
    ActivityManager activityManager;
    String origem;
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
        frame_aguarde = findViewById(R.id.frame_aguarde);
        progressBar = findViewById(R.id.progressBar);

        setupMenu();
        renderMenu(
                new ImageButton[]{ btnSelecionarArquivos, btnSelecionarModo, btnCenter, btn_ok, btn_lixeira },
                new TextView[]{ encoder, encoderM, tv_nome_arquivo },
                new FrameLayout[]{ frame_hash, frame_aguarde },
                null
        );
        origem = getIntent().getStringExtra("origem");
        if (origem == null) {
            origem = "";
        }
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
            frame_aguarde.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            ExecutorService executor = Executors.newFixedThreadPool(1);
            executor.execute(() -> {
                if (origem.equals("Comparador")) {
                    ZipUtils.copiarParaInterno(urisPastasSelecionadas, urisArquivosSelecionados, SHA256.this);
                    File pastaTemp = new File(this.getFilesDir(), "temp");
                    File arquivoZip = new File(this.getFilesDir(), "arquivos.zip");
                    ZipUtils.zip(pastaTemp, arquivoZip);
                    try (InputStream is = getContentResolver().openInputStream(Uri.fromFile(arquivoZip))) {
                        String hash = calcularSHA256(Objects.requireNonNull(is));
                        Intent intent = new Intent();
                        intent.putExtra("hash", hash);
                        if(!arquivoZip.delete()){
                            Log.e("ERRO", "arquivo zip não pode ser deletado");
                        }
                        FileUtils.limparTemp(SHA256.this);
                        setResult(RESULT_OK, intent);
                        finish();
                    } catch (IOException | NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    gerarSHA();
                    runOnUiThread(() -> {
                        frame_aguarde.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.GONE);
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
                        tv_nome_arquivo.setAlpha(0.25f);
                        calculando = false;
                    });
                }
            });

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
            tv_nome_arquivo.setAlpha(1f);
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
        calculando = true;
        try{
            Map<String, Uri> caminho_dados = new HashMap<>();
            for(Uri uri : urisArquivosSelecionados){
                caminho_dados.put(nomeArquivoPorUri(uri), uri);
            }
            for(Uri uri : urisPastasSelecionadas){
                caminho_dados.putAll(capturarEstruturaPasta(uri));
            }
            for(Map.Entry<String, Uri> entry : caminho_dados.entrySet()){
                String caminho = entry.getKey();
                Uri uri = entry.getValue();
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    String hash = calcularSHA256(Objects.requireNonNull(is));
                    mapsHash256.put(caminho, hash);
                } catch (Exception e) {
                    Log.e("SHA", "Erro ao calcular SHA de " + caminho, e);
                }
            }
            hashes = new StringBuilder();

            for (Map.Entry<String, String> entry : mapsHash256.entrySet()) {
                hashes.append(entry.getKey()).append(":\n\n").append(entry.getValue()).append("\n\n");
            }
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
            calculando = false;
        }
    }
    public String calcularSHA256(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }

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
            if (!arquivos || calculando) {
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
