package com.fabriciofkt157.encoder;


import android.annotation.SuppressLint;
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
import static android.widget.Toast.makeText;

import androidx.documentfile.provider.DocumentFile;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Descriptografar extends BaseActivity {
    ImageButton btnCenter, btn_ok_senha, btn_ok_chave, btnSelecionarArquivos, btn_lixeira;
    FrameLayout senha, chave, frame_aguarde;
    TextView tv_nome_arquivo;
    EditText edit_senha, edit_chave;
    Uri arquivoSelecionado, pastaDestino;
    int estado = 0;
    ProgressBar progressBar;
    boolean descriptografando = false;

    byte[] chaveAES;
    String nomeArquivoCarregado, chaveInserida;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.descriptografar);
        btnCenter = findViewById(R.id.btn_center);
        btn_ok_senha = findViewById(R.id.btn_ok_senha);
        btn_ok_chave = findViewById(R.id.btn_ok_chave);
        edit_senha = findViewById(R.id.edit_senha);
        edit_chave = findViewById(R.id.edit_chave);
        senha = findViewById(R.id.frame_senha);
        chave = findViewById(R.id.frame_chave);
        tv_nome_arquivo = findViewById(R.id.nome_arquivo);
        btnSelecionarArquivos = findViewById(R.id.btn_select_files);
        btn_lixeira = findViewById(R.id.lixeira);
        frame_aguarde = findViewById(R.id.frame_aguarde);
        progressBar = findViewById(R.id.progressBar);
        setupMenu();
        renderMenu(
                new ImageButton[]{ btnSelecionarArquivos, btnCenter, btn_ok_senha, btn_ok_chave, btn_lixeira },
                new TextView[]{ encoder, encoderM, tv_nome_arquivo },
                new FrameLayout[]{ senha, chave },
                new EditText[]{ edit_senha, edit_chave }
        );

        btnSelecionarArquivos.setOnClickListener(v -> {
            botaoPressionado(btnSelecionarArquivos);
            if(arquivoSelecionado != null) makeText(this, "Apenas um arquivo por vez ...", Toast.LENGTH_SHORT).show();
            else{
                makeText(this, "Selecione umm arquivo criptografado (.aes)", Toast.LENGTH_SHORT).show();
                selecionarArquivoUnico(uri -> {
                    arquivoSelecionado = uri;
                    nomeArquivoCarregado = nomeArquivoPorUri(uri);
                    String extensao;
                    if(nomeArquivoCarregado.length() >= 4){
                        extensao = nomeArquivoCarregado.substring(nomeArquivoCarregado.length() - 4);
                        if(!extensao.equals(".aes")){
                            limparArquivoCarregado();
                        } else tv_nome_arquivo.setText("O arquivo " + nomeArquivoCarregado + " foi selecionado");
                    } else limparArquivoCarregado();
                });
            }
        });
        btnCenter.setOnClickListener(v -> {
            botaoPressionado(btnSelecionarArquivos);
            if(arquivoSelecionado == null) makeText(this, "Selecione um arquivo primeiro.", Toast.LENGTH_SHORT).show();
            else {
                descriptografando = true;
                estado = 1;
                boolean arquivoReconhecido = false;
                Map<String, byte[]> dadosSalvos = FileUtils.carregarMap(this);
                //FileUtils.logarMap(dadosSalvos);
                for(Map.Entry<String, byte[]> chavesSalvas : dadosSalvos.entrySet()){
                    String nomeArquivoSalvo = chavesSalvas.getKey();
                    if(nomeArquivoSalvo.equals(nomeArquivoCarregado)){
                        makeText(this, "Boas notícias! Encontramos a chave de criptografia no armazenamento do aplicativo", Toast.LENGTH_SHORT).show();
                        makeText(this, "Vamos descriptografar seu arquivo.", Toast.LENGTH_SHORT).show();
                        arquivoReconhecido = true;
                        chaveAES = chavesSalvas.getValue();
                        descriptografia();
                    }
                }
                if(!arquivoReconhecido){
                    chave.setVisibility(View.VISIBLE);
                    edit_chave.setVisibility(View.VISIBLE);
                    btn_ok_chave.setVisibility(View.VISIBLE);
                    desativarBtns();

                    edit_chave.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(edit_chave, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        btn_lixeira.setOnClickListener(v -> {
            arquivoSelecionado = null;
            tv_nome_arquivo.setText("nenhum arquivo carregado");
            makeText(this, "O arquivo selecionado foi removido.", Toast.LENGTH_SHORT).show();
        });

        edit_senha.setOnFocusChangeListener((v, hasFocus) -> edit_senha.setCursorVisible(hasFocus));
        edit_chave.setOnFocusChangeListener((v, hasFocus) -> edit_chave.setCursorVisible(hasFocus));

        btn_ok_chave.setOnClickListener(v -> {
            chaveInserida = edit_chave.getText().toString();
            if(chaveInserida.isEmpty()){
                chave.setVisibility(View.INVISIBLE);
                edit_chave.setVisibility(View.INVISIBLE);
                btn_ok_chave.setVisibility(View.INVISIBLE);

                senha.setVisibility(View.VISIBLE);
                edit_senha.setVisibility(View.VISIBLE);
                btn_ok_senha.setVisibility(View.VISIBLE);
            } else {
                if(chaveInserida.length() != 32 && chaveInserida.length() != 48 && chaveInserida.length() != 64){
                    makeText(this, "formato de chave inadequado. Apenas chaves hexadecimais", Toast.LENGTH_SHORT).show();
                    makeText(this, "Deixe em branco para usar a senha, ou verifique a digitação..", Toast.LENGTH_SHORT).show();
                } else {
                    chaveAES = FileUtils.hexToBytes(chaveInserida);
                    descriptografia();
                }

            }
        });

        btn_ok_senha.setOnClickListener(v -> {
            chaveInserida = edit_senha.getText().toString();
            if(chaveInserida.isEmpty()){
                makeText(this, "O campo senha não pode estar em branco", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    chaveAES = Crypt.gerarChaveAES(chaveInserida, 128);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                selecionarPasta(true, uri -> {
                    Uri arquivoTemp = FileUtils.salvarArquivo(this, false, uri, "temp_" + System.currentTimeMillis(), null);
                    pastaDestino = uri;
                    if(Crypt.descriptografarArquivo(this, arquivoSelecionado, arquivoTemp, chaveAES)){
                        sucessoSenha(arquivoTemp);
                    } else {
                        try {
                            chaveAES = Crypt.gerarChaveAES(chaveInserida, 196);
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        }
                        if(Crypt.descriptografarArquivo(this, arquivoSelecionado, arquivoTemp, chaveAES)){
                            sucessoSenha(arquivoTemp);
                            makeText(this, "Salvamos a nova chave no armazenamento do aplicativo.", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                chaveAES = Crypt.gerarChaveAES(chaveInserida, 256);
                            } catch (NoSuchAlgorithmException e) {
                                throw new RuntimeException(e);
                            }
                            if(Crypt.descriptografarArquivo(this, arquivoSelecionado, arquivoTemp, chaveAES)){
                                sucessoSenha(arquivoTemp);
                                makeText(this, "Salvamos a nova chave no armazenamento do aplicativo.", Toast.LENGTH_SHORT).show();
                            } else {
                                makeText(this, "Senha incorreta ou possível erro de digitação", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        });
    }

    public void sucessoSenha(Uri arquivoTemp){
        makeText(this, "O arquivo foi descriptografado com sucesso", Toast.LENGTH_SHORT).show();
        descompilar(arquivoTemp);
        Map<String, byte[]> mapaDeChaves = FileUtils.carregarMap(this);
        mapaDeChaves.put(nomeArquivoCarregado, chaveAES);
        FileUtils.salvarMap(this, mapaDeChaves);
        makeText(this, "Salvamos a nova chave no armazenamento do aplicativo.", Toast.LENGTH_SHORT).show();
    }

    public void ativarBtns(){
        btnCenter.setEnabled(true);
        btnCenter.setAlpha(1f);
        btnSelecionarArquivos.setEnabled(true);
        btnSelecionarArquivos.setAlpha(1f);
        tv_nome_arquivo.setAlpha(1f);
        btn_lixeira.setAlpha(1f);
        btn_lixeira.setEnabled(true);
    }
    public void desativarBtns(){
        btnCenter.setEnabled(false);
        btnCenter.setAlpha(0.25f);
        btnSelecionarArquivos.setEnabled(false);
        btnSelecionarArquivos.setAlpha(0.25f);
        tv_nome_arquivo.setAlpha(0.25f);
        btn_lixeira.setAlpha(0.25f);
        btn_lixeira.setEnabled(false);
    }
    public void descriptografia(){
        btnCenter.setEnabled(false);
        btnCenter.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        frame_aguarde.setVisibility(View.VISIBLE);
        desativarBtns();
        selecionarPasta(true, uri -> {
            pastaDestino = uri;
            Uri arquivoTemp = FileUtils.salvarArquivo(this, false, uri, "temp_" + System.currentTimeMillis() + ".zip", null);
            if(Crypt.descriptografarArquivo(this, arquivoSelecionado, arquivoTemp, chaveAES)){
                descompilar(arquivoTemp);
            } else {
                makeText(this, "Chave incorreta ou mal inserida.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void limparArquivoCarregado(){
        makeText(this, "Arquivo inválido ... Apenas arquivos '.aes' são permitidos", Toast.LENGTH_SHORT).show();
        arquivoSelecionado = null;
        nomeArquivoCarregado = null;
    }

    public void descompilar(Uri arquivoTemp){
        if(FileUtils.temPermissaoParaEscrever(this, pastaDestino)){
            DocumentFile pasta = DocumentFile.fromTreeUri(this, pastaDestino);
            DocumentFile arquivoZip = DocumentFile.fromSingleUri(this, arquivoTemp);
            ZipUtils.unzip(this, arquivoZip, pasta);

            progressBar.setVisibility(View.VISIBLE);
            frame_aguarde.setVisibility(View.VISIBLE);

            ExecutorService executor = Executors.newFixedThreadPool(1);
            executor.execute(() -> {
                try {
                    ZipUtils.unzip(Descriptografar.this, arquivoZip, pasta);
                    runOnUiThread(() -> {
                        DocumentFile pastaTemp = Objects.requireNonNull(pasta).findFile("temp");
                        if (pastaTemp != null && pastaTemp.isDirectory()) {
                            pastaTemp.renameTo("arquivos descriptografados");
                        }
                        progressBar.setVisibility(View.GONE);
                        frame_aguarde.setVisibility(View.INVISIBLE);
                        ativarBtns();
                        descriptografando = false;
                        btnCenter.setEnabled(true);
                        btnCenter.setVisibility(View.VISIBLE);
                        Toast.makeText(Descriptografar.this, "Arquivo descriptografado com sucesso!", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e("ERRO_EXECUTOR", "Erro durante processo de descriptografia", e);
                    runOnUiThread(() -> Toast.makeText(Descriptografar.this, "Erro ao descriptografar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });

        } else makeText(this, "Sem permissão de escrita para a pasta destino", Toast.LENGTH_SHORT).show();
        FileUtils.deleteFileFromUri(this, arquivoTemp);

        ativarBtns();
        senha.setVisibility(View.INVISIBLE);
        edit_senha.setVisibility(View.INVISIBLE);
        btn_ok_senha.setVisibility(View.INVISIBLE);
        chave.setVisibility(View.INVISIBLE);
        edit_chave.setVisibility(View.INVISIBLE);
        btn_ok_chave.setVisibility(View.INVISIBLE);
    }

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (arquivoSelecionado == null && btn_menu_mode == 0) btnCenter.setAlpha(0.65f);
            if (arquivoSelecionado == null || descriptografando){
                btn_lixeira.setAlpha(0.25f);
                btn_lixeira.setEnabled(false);
            } else {
                btn_lixeira.setAlpha(1f);
                btn_lixeira.setEnabled(true);
            }
            if (arquivoSelecionado == null){
                if(btn_menu_mode == 0) {
                    btnCenter.setAlpha(0.65f);
                    if(estado == 0) btnCenter.setAlpha(1f);
                }
                else if (btn_menu_mode == 1) btnCenter.setAlpha(0.25f);
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