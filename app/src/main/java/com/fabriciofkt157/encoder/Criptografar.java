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
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Criptografar extends BaseActivity{
    ImageButton btnAes128, btnAes192, btnAes256, btnCenter, btn_ok, btnSelecionarArquivos, btnSelecionarModo, btn_lixeira;
    FrameLayout senha, frame_aguarde;
    EditText edit_senha;
    TextView text_selecione_cripto, tv_nome_arquivo;
    int nivelDeSeguranca = 0, estado = 0;

    byte[] chaveGerada;
    private boolean arquivos = false, criptografando = false;
    private String senhaUsuario;
    Uri pastaFinal, uriArquivoAes;
    List<Uri> urisArquivosSelecionados = new ArrayList<>(), urisPastasSelecionadas = new ArrayList<>();
    ActivityManager activityManager;
    ProgressBar progressBar;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.criptografar);
        fn_op = 0;

        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        tv_nome_arquivo = findViewById(R.id.nome_arquivo);
        btnSelecionarArquivos = findViewById(R.id.btn_select_files);
        btnSelecionarModo = findViewById(R.id.selecionar_modo);
        btn_lixeira = findViewById(R.id.lixeira);

        btnCenter = findViewById(R.id.btn_center);
        btn_ok = findViewById(R.id.btn_ok);
        edit_senha = findViewById(R.id.edit_senha);
        senha = findViewById(R.id.frame_senha);
        progressBar = findViewById(R.id.progressBar);
        frame_aguarde = findViewById(R.id.frame_aguarde);

        btnAes128 = findViewById(R.id.btn_aes_128);
        btnAes192 = findViewById(R.id.btn_aes_192);
        btnAes256 = findViewById(R.id.btn_aes_256);
        text_selecione_cripto = findViewById(R.id.text_selecione_cripto);
        android.graphics.Typeface typeface = ResourcesCompat.getFont(this, R.font.jersey10_regular);
        text_selecione_cripto.setTypeface(typeface);

        setupMenu();
        renderMenu(
                new ImageButton[]{ btnSelecionarArquivos, btnSelecionarModo, btnCenter, btn_ok, btnAes128, btnAes192, btnAes256, btn_lixeira },
                new TextView[]{ encoder, encoderM, text_selecione_cripto, tv_nome_arquivo },
                new FrameLayout[]{ senha, frame_aguarde },
                new EditText[]{ edit_senha }
        );

        switch(nivelDeSeguranca){
            case 128:
                btnAes128.setAlpha(0.75f);
                break;
            case 192:
                btnAes192.setAlpha(0.75f);
                break;
            case 256:
                btnAes256.setAlpha(0.75f);
                break;
            default:
                break;
        }

        btnAes128.setOnClickListener(v -> {
            btnAes256.setAlpha(1f);
            btnAes192.setAlpha(1f);
            btnAes128.setAlpha(0.75f);
            if(nivelDeSeguranca != 128) makeText(this, "Chave para 128 bits selecionada.", Toast.LENGTH_SHORT).show(); nivelDeSeguranca = 128;
        });

        btnAes192.setOnClickListener(v -> {
            btnAes256.setAlpha(1f);
            btnAes192.setAlpha(0.75f);
            btnAes128.setAlpha(1f);
            if(nivelDeSeguranca != 192) makeText(this, "Chave para 192 bits selecionada.", Toast.LENGTH_SHORT).show(); nivelDeSeguranca = 192;
        });

        btnAes256.setOnClickListener(v -> {
            btnAes256.setAlpha(0.75f);
            btnAes192.setAlpha(1f);
            btnAes128.setAlpha(1f);
            if(nivelDeSeguranca != 256) makeText(this, "Chave para 256 bits selecionada.", Toast.LENGTH_SHORT).show(); nivelDeSeguranca = 256;
        });

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
                makeText(this, "Selecione a pasta que deseja criptografar", Toast.LENGTH_SHORT).show();
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

        btn_lixeira.setOnClickListener(v -> {
            urisArquivosSelecionados.clear();
            urisPastasSelecionadas.clear();
            tv_nome_arquivo.setText("nenhum arquivo carregado");
            makeText(this, "Os arquivos selecionados foram removidos.", Toast.LENGTH_SHORT).show();
            arquivos = false;
        });

        btnCenter.setOnClickListener(v -> {
            if(urisPastasSelecionadas.isEmpty() && urisArquivosSelecionados.isEmpty()) makeText(this, "Nenhum arquivo selecionado.", Toast.LENGTH_SHORT).show();
            else if(nivelDeSeguranca == 0) makeText(this, "Nenhum tipo de criptografia foi selecionado.", Toast.LENGTH_SHORT).show();
            else {
                criptografando = true;
                estado = 1;
                botaoPressionado(btnCenter);
                senha.setVisibility(View.VISIBLE);
                btn_ok.setVisibility(View.VISIBLE);

                desativarBtns();

                edit_senha.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(edit_senha, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        edit_senha.setOnFocusChangeListener((v, hasFocus) -> edit_senha.setCursorVisible(hasFocus));
        btn_ok.setOnClickListener(v -> {
            botaoPressionado(btn_ok);
            senhaUsuario = edit_senha.getText().toString();
            if(!senhaUsuario.isEmpty()) {
                senha.setVisibility(View.INVISIBLE);
                btn_ok.setVisibility(View.INVISIBLE);
                hideKeyboard(edit_senha);
                edit_senha.setText("");

                try {
                    chaveGerada = Crypt.gerarChaveAES(senhaUsuario, nivelDeSeguranca);
                    ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Chave AES gerada", FileUtils.bytesToHex(chaveGerada));
                    clipboard.setPrimaryClip(clip);
                    new AlertDialog.Builder(this)
                            .setTitle("Chave AES")
                            .setMessage("A chave de criptografia foi copiada para a área de transferência.\n\n" +
                                    "Guarde-a em um local seguro para evitar a perda de acesso aos seus dados.\n\n" +
                                    "Ainda podemos tentar recuperá-la, mas não garantimos 100% de sucesso.")
                            .setPositiveButton("Entendi", (dialog, which) -> dialog.dismiss())
                            .show();

                    criptografia();
                } catch (NoSuchAlgorithmException e) {
                    makeText(this, "Houve um problema ao processar sua senha... Vamos prosseguir com uma chave aleatória.", Toast.LENGTH_SHORT).show();
                    try {
                        chaveGerada = Crypt.gerarChaveAES(null, nivelDeSeguranca);
                        criptografia();

                    } catch (NoSuchAlgorithmException ex) {
                        makeText(this, "Um erro lendário foi encontrado: ", Toast.LENGTH_SHORT).show();
                        makeText(this, "Não conseguimos processar a chave ... ", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                hideKeyboard(edit_senha);
                AlertDialog.Builder builder = getBuilder(senha, btn_ok, edit_senha);
                builder.show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void criptografia(){

        File pastaTemp = new File(this.getFilesDir(), "temp");
        File arquivoZip = new File(this.getFilesDir(), "arquivos.zip");
        if(!arquivoZip.exists()){
            try {
                if(!arquivoZip.createNewFile()){
                    Log.e("ERRO", "arquivo zip não pode ser criado.");
                }
            } catch (IOException e) {
                Log.e("ERRO", String.valueOf(e));
                throw new RuntimeException(e);
            }
        }

        String nomeArquivoAes = "arquivos_criptografados_" + System.currentTimeMillis() + ".aes";
        Uri uriArquivoZip = Uri.fromFile(arquivoZip);

        selecionarPasta(false, uriPastaDestino -> {
            pastaFinal = uriPastaDestino;
            uriArquivoAes = FileUtils.salvarArquivo(Criptografar.this, false, pastaFinal, nomeArquivoAes, null);

            progressBar.setVisibility(View.VISIBLE);
            frame_aguarde.setVisibility(View.VISIBLE);
            desativarBtns();
            btnCenter.setEnabled(false);
            btnCenter.setVisibility(View.INVISIBLE);

            ExecutorService executor = Executors.newFixedThreadPool(1);
            executor.execute(() -> {
                try {
                    Log.i("etapa", "copiando");
                    ZipUtils.copiarParaInterno(urisPastasSelecionadas, urisArquivosSelecionados, Criptografar.this);
                    Log.i("etapa", "zip");
                    ZipUtils.zip(pastaTemp, arquivoZip);
                    Log.i("tamanho arquivo zip", String.valueOf(arquivoZip.length()));
                    Log.i("etapa", "criptografia");
                    Crypt.criptografarArquivo(Criptografar.this, uriArquivoZip, uriArquivoAes, chaveGerada);

                    runOnUiThread(() -> {
                        if(!arquivoZip.delete()){
                            Log.e("ERRO", "arquivo zip não pode ser deletado");
                        }
                        FileUtils.limparTemp(Criptografar.this);

                        progressBar.setVisibility(View.GONE);
                        frame_aguarde.setVisibility(View.INVISIBLE);
                        Log.i("etapa", "UI");
                        Toast.makeText(Criptografar.this, "Os dados foram salvos em: " + nomeArquivoAes, Toast.LENGTH_SHORT).show();
                        Map<String, byte[]> mapaDeChaves = FileUtils.carregarMap(this);
                        mapaDeChaves.put(nomeArquivoAes, chaveGerada);
                        FileUtils.salvarMap(this, mapaDeChaves);
                        new AlertDialog.Builder(Criptografar.this)
                                .setTitle("Chave AES")
                                .setMessage("Salvamos a chave AES no armazenamento do aplicativo.\n\n" +
                                        "Observação: não altere o nome do arquivo, caso contrário, terá que inserir a chave manualmente.\n\n" +
                                        "Ainda estamos trabalhando num gerenciador de chaves ... aguarde :).")
                                .setPositiveButton("Entendi", (dialog, which) -> {
                                    dialog.dismiss();
                                    ativarBtns();
                                    criptografando = false;
                                    estado = 0;
                                    urisArquivosSelecionados.clear();
                                    urisPastasSelecionadas.clear();
                                    tv_nome_arquivo.setText("nenhum arquivo carregado");
                                    btnCenter.setEnabled(true);
                                    btnCenter.setVisibility(View.VISIBLE);
                                })
                                .show();
                    });
                } catch (Exception e) {
                    Log.e("ERRO_EXECUTOR", "Erro durante processo de criptografia", e);
                    runOnUiThread(() -> Toast.makeText(Criptografar.this, "Erro ao criptografar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        });
    }


    public void ativarBtns(){
        btnCenter.setEnabled(true);
        btnSelecionarArquivos.setEnabled(true);
        btnAes128.setEnabled(true);
        btnAes192.setEnabled(true);
        btnAes256.setEnabled(true);
        btnSelecionarModo.setEnabled(true);
        btn_lixeira.setEnabled(true);

        btnCenter.setAlpha(1f);
        btnSelecionarArquivos.setAlpha(1f);
        btnAes256.setAlpha(1f);
        btnAes192.setAlpha(1f);
        btnAes128.setAlpha(1f);
        btnSelecionarModo.setAlpha(1f);
        tv_nome_arquivo.setAlpha(1f);
        text_selecione_cripto.setAlpha(1f);
        btn_lixeira.setAlpha(1f);
    }
    public void desativarBtns(){
        btnCenter.setEnabled(false);
        btnSelecionarArquivos.setEnabled(false);
        btnAes128.setEnabled(false);
        btnAes192.setEnabled(false);
        btnAes256.setEnabled(false);
        btnSelecionarModo.setEnabled(false);
        btn_lixeira.setEnabled(true);

        btnCenter.setAlpha(0.25f);
        btnSelecionarArquivos.setAlpha(0.25f);
        btnAes256.setAlpha(0.25f);
        btnAes192.setAlpha(0.25f);
        btnAes128.setAlpha(0.25f);
        btnSelecionarModo.setAlpha(0.25f);
        tv_nome_arquivo.setAlpha(0.25f);
        text_selecione_cripto.setAlpha(0.25f);
        btn_lixeira.setAlpha(0.25f);
    }

    private AlertDialog.Builder getBuilder(FrameLayout senha, ImageButton btn_ok, EditText edit_senha) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nenhum texto foi inserido.");
        builder.setMessage("Deseja continuar a partir de uma chave aleatória?");
        builder.setPositiveButton("Sim", (dialog, which) -> {
            try {
                chaveGerada = Crypt.gerarChaveAES(null, nivelDeSeguranca);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            makeText(this, "Prosseguindo com a criptografia.", Toast.LENGTH_SHORT).show();
            senha.setVisibility(View.INVISIBLE);
            btn_ok.setVisibility(View.INVISIBLE);
            hideKeyboard(edit_senha);
            criptografia();
            makeText(this, "Arquivo criptografado com sucesso!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Inserir senha", (dialog, which) -> {
            dialog.dismiss();
            edit_senha.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(edit_senha, InputMethodManager.SHOW_IMPLICIT);
        });
        return builder;
    }

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if ((!arquivos || nivelDeSeguranca == 0) && btn_menu_mode == 0) btnCenter.setAlpha(0.65f);
            else if (btn_menu_mode == 1 || estado == 1) btnCenter.setAlpha(0.25f);
            else if (btn_menu_mode == 0) btnCenter.setAlpha(1f);

            if((urisPastasSelecionadas.isEmpty() && urisArquivosSelecionados.isEmpty()) || criptografando) {
                btn_lixeira.setEnabled(false);
                btn_lixeira.setAlpha(0.25f);
            } else {
                btn_lixeira.setAlpha(1f);
                btn_lixeira.setEnabled(true);
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