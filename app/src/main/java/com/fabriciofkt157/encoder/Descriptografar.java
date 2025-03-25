package com.fabriciofkt157.encoder;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import static android.widget.Toast.makeText;

import java.util.Map;

public class Descriptografar extends BaseActivity {
    ImageButton btnCenter, btn_ok_senha, btn_ok_chave, btnSelecionarArquivos;
    FrameLayout senha, chave;
    TextView tv_nome_arquivo;
    EditText edit_senha, edit_chave;
    Uri arquivoSelecionado;
    int estado = 0;

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
        setupMenu();
        renderMenu(
                new ImageButton[]{ btnSelecionarArquivos, btnCenter, btn_ok_senha, btn_ok_chave },
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
                estado = 1;
                boolean arquivoReconhecido = false;
                Map<Uri, byte[]> dadosSalvos = FileUtils.carregarMap(this);
                for(Map.Entry<Uri, byte[]> urisSalvos : dadosSalvos.entrySet()){
                    Uri uriSalvo = urisSalvos.getKey();
                    if(uriSalvo == arquivoSelecionado){
                        makeText(this, "Boas notícias! Encontramos a chave de criptografia no armazenamento do aplicativo", Toast.LENGTH_SHORT).show();
                        makeText(this, "Vamos descriptografar seu arquivo.", Toast.LENGTH_SHORT).show();
                        arquivoReconhecido = true;
                        chaveAES = urisSalvos.getValue();
                        descriptografia();
                    }
                }
                if(!arquivoReconhecido){
                    chave.setVisibility(View.VISIBLE);
                    edit_chave.setVisibility(View.VISIBLE);
                    btn_ok_chave.setVisibility(View.VISIBLE);
                    btnCenter.setEnabled(false);
                    btnCenter.setAlpha(0.25f);
                    btnSelecionarArquivos.setEnabled(false);
                    btnSelecionarArquivos.setAlpha(0.25f);
                    tv_nome_arquivo.setAlpha(0.25f);

                    edit_chave.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(edit_chave, InputMethodManager.SHOW_IMPLICIT);
                }
            }
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
                chaveAES = chaveInserida.getBytes();
                descriptografia();
            }
        });
    }

    public void descriptografia(){
        selecionarPasta(uri -> {
            Uri arquivoTemp = FileUtils.salvarArquivo(this, uri, "temp_" + System.currentTimeMillis(), null);
            Crypt.descriptografarArquivo(this, arquivoSelecionado, arquivoTemp, chaveAES);
        });
    }
    public void limparArquivoCarregado(){
        makeText(this, "Arquivo inválido ... Apenas .aes são permitidos", Toast.LENGTH_SHORT).show();
        arquivoSelecionado = null;
        nomeArquivoCarregado = null;
    }
    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (arquivoSelecionado == null && btn_menu_mode == 0) btnCenter.setAlpha(0.65f);
            else if (btn_menu_mode == 1) btnCenter.setAlpha(0.25f);
            else if (btn_menu_mode == 0 && estado == 0) btnCenter.setAlpha(1f);

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
