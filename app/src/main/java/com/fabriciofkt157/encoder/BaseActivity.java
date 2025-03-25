package com.fabriciofkt157.encoder;

import static android.widget.Toast.makeText;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BaseActivity extends AppCompatActivity {
    TextView[] btnsMenu;
    ImageButton btnMenu;
    TextView encoder, encoderM, btn_selecionar_comparador, btn_selecionar_sha256, btn_selecionar_criptografar, btn_selecionar_descriptografar, btn_selecionar_base64;
    boolean menuAtivado = false;
    FrameLayout frame_selecionar_operacao;
    int btn_menu_mode = 0, op_modo, modo = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.cinza_background));

    }
    public void setupMenu(){
        encoder = findViewById(R.id.tv_encoder);
        encoderM = findViewById(R.id.tv_encoder_modo);
        btn_selecionar_criptografar = findViewById(R.id.btn_selecionar_criptografar);
        btn_selecionar_descriptografar = findViewById(R.id.btn_selecionar_descriptografar);
        btn_selecionar_base64 = findViewById(R.id.btn_selecionar_base64);
        btn_selecionar_comparador = findViewById(R.id.btn_selecionar_comparador);
        btn_selecionar_sha256 = findViewById(R.id.btn_selecionar_sha256);
        btnsMenu = new TextView[]{
                btn_selecionar_criptografar,
                btn_selecionar_descriptografar,
                btn_selecionar_base64,
                btn_selecionar_sha256,
                btn_selecionar_comparador
        };
        btnMenu = findViewById(R.id.btn_menu);
        frame_selecionar_operacao = findViewById(R.id.frame_selecionar_funcao);
    }
    public void renderMenu(ImageButton[] imgBtns, TextView[] txtViews, FrameLayout[] frameLayouts, EditText[] editTexts){
        btnMenu.setOnClickListener(v -> {
            Ferramentas.botaoPressionado(btnMenu);
            menuAtivado = true;
            if(btn_menu_mode == 0){
                btn_menu_mode = 1;
                frame_selecionar_operacao.setVisibility(View.VISIBLE);
                btnMenu.setBackgroundResource(R.drawable.menu_alternar);

                if(imgBtns != null){
                    for (ImageButton imgBtn : imgBtns) {
                        imgBtn.setEnabled(false);
                        imgBtn.setAlpha(0.25f);
                    }
                }
                if(txtViews != null){
                    for (TextView txtBtn : txtViews) {
                        txtBtn.setEnabled(false);
                        txtBtn.setAlpha(0.25f);
                    }
                }
                if(frameLayouts != null){
                    for (FrameLayout frames : frameLayouts) {
                        frames.setEnabled(false);
                        frames.setAlpha(0.25f);
                    }
                }
                if(editTexts != null){
                    for (EditText txts : editTexts) {
                        txts.setEnabled(false);
                        txts.setAlpha(0.25f);
                    }
                }

            } else {
                menuAtivado = false;
                btn_menu_mode = 0;
                frame_selecionar_operacao.setVisibility(View.INVISIBLE);
                btnMenu.setBackgroundResource(R.drawable.menu);

                if(imgBtns != null){
                    for (ImageButton imgBtn : imgBtns) {
                        imgBtn.setEnabled(true);
                        imgBtn.setAlpha(1f);
                    }
                }
                if(txtViews != null){
                    for (TextView txtBtn : txtViews) {
                        txtBtn.setEnabled(true);
                        txtBtn.setAlpha(1f);
                    }
                }
                if(frameLayouts != null){
                    for (FrameLayout frames : frameLayouts) {
                        frames.setEnabled(true);
                        frames.setAlpha(1);
                    }
                }
                if(editTexts != null){
                    for (EditText txts : editTexts) {
                        txts.setEnabled(true);
                        txts.setAlpha(1f);
                    }
                }
            }
        });
        for(int i = 0; i < btnsMenu.length; i++){
            final int index = i;
            btnsMenu[i].setOnClickListener(v -> {
                Ferramentas.limparBg(null, btnsMenu, R.color.background_cinza_claro);
                btnsMenu[index].setBackgroundResource(R.color.cinza_azulado_claro);
                op_modo = index;
                if(index == 0){
                    Intent intent = new Intent(this, Criptografar.class);
                    startActivity(intent);
                    finish();
                } else if(index == 1){
                    Intent intent = new Intent(this, Descriptografar.class);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    public void atualizarEstadoArquivos(List<Uri> uriArquivosSelecionados, List<Uri> uriPastasSelecionadas, TextView tv_nome_arquivo) {
        StringBuilder nomesArquivos = new StringBuilder("Os seguintes arquivos foram selecionados:\n");
        for (Uri uri : uriArquivosSelecionados) {
            String nomeArquivo = nomeArquivoPorUri(uri);
            nomesArquivos.append("-").append(nomeArquivo).append("\n");
        }
        if(!uriPastasSelecionadas.isEmpty()) {
            nomesArquivos.append("As seguintes pastas foram selecionadas:\n");
            for (Uri uri : uriPastasSelecionadas) {
                String nomePasta = nomePastaPorUri(uri);
                nomesArquivos.append("-").append(nomePasta).append("\n");
            }
        }
        nomesArquivos.append("Note que ainda é possível selecionar mais arquivos :)");
        tv_nome_arquivo.setText(nomesArquivos.toString());
        if(uriArquivosSelecionados.size() + uriPastasSelecionadas.size() > 1) {
            makeText(this, "Você seleciou múltiplos arquivos,", Toast.LENGTH_LONG).show();
            makeText(this, "eles serão compactados em apenas um que depois será criptografado.", Toast.LENGTH_SHORT).show();
        }
    }


    public String nomeArquivoPorUri(Uri uri) {
        String nomeArquivo = "Não foi possível identificar o nome do arquivo ...";

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) nomeArquivo = cursor.getString(nameIndex);
            cursor.close();
        }
        return nomeArquivo;
    }
    public String nomePastaPorUri(Uri uri) {
        String nomePasta = "Não foi possível identificar o nome da pasta ...";

        if (DocumentsContract.isTreeUri(uri)) {
            List<String> pathSegments = uri.getPathSegments();
            if (!pathSegments.isEmpty()) {
                String folderId = pathSegments.get(pathSegments.size() - 1);
                nomePasta = folderId.split(":")[1];
            }
        }
        return nomePasta;
    }

    public byte[] obterBytesDeUri(Uri fileUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = Objects.requireNonNull(inputStream).read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            Log.e("ErroLeitura", "Erro ao ler arquivo: " + fileUri, e);
            return null;
        }
    }

    private OnArquivosSelecionadosListener callbackSelecionarUriArquivos;
    public void selecionarArquivos(OnArquivosSelecionadosListener callback) {
        this.callbackSelecionarUriArquivos = callback;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        selecionarArquivosLauncher.launch(intent);
    }

    private OnArquivoSelecionadoListener callbackSelecionarUriArquivoUnico;

    public void selecionarArquivoUnico(OnArquivoSelecionadoListener callback) {
        this.callbackSelecionarUriArquivoUnico = callback;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        selecionarArquivosLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> selecionarArquivosLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        List<Uri> listaUris = new ArrayList<>();

                        if (data.getClipData() != null) {
                            // Se o usuário selecionou múltiplos arquivos
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri fileUri = data.getClipData().getItemAt(i).getUri();
                                listaUris.add(fileUri);
                            }
                        } else if (data.getData() != null) {
                            // Se o usuário selecionou apenas um arquivo
                            listaUris.add(data.getData());
                        }

                        // Retorna a lista com todos os arquivos selecionados
                        if (callbackSelecionarUriArquivos != null) {
                            callbackSelecionarUriArquivos.onArquivosSelecionados(listaUris);
                        } else if(callbackSelecionarUriArquivoUnico != null){
                            callbackSelecionarUriArquivoUnico.onArquivoSelecionado(listaUris.get(0));
                        }
                    }
                }
            });

    private OnPastaSelecionadaListener callbackSelecionarPasta;
    private final ActivityResultLauncher<Intent> selecionarPastaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode() == RESULT_OK){
                    Intent data = result.getData();
                    if(data != null && callbackSelecionarPasta != null){
                        callbackSelecionarPasta.onPastaSelecionada(data.getData());
                    }
                }
    });
    public void selecionarPasta(OnPastaSelecionadaListener callback){
        this.callbackSelecionarPasta = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        selecionarPastaLauncher.launch(intent);
    }
    public interface OnArquivosSelecionadosListener {
        void onArquivosSelecionados(List<Uri> uris);

    }
    public interface OnArquivoSelecionadoListener {
        void onArquivoSelecionado(Uri uri);

    }

    public interface OnPastaSelecionadaListener {
        void onPastaSelecionada(Uri uri);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void botaoPressionado(ImageButton btn){
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



    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
