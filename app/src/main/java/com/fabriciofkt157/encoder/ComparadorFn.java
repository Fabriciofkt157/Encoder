package com.fabriciofkt157.encoder;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class ComparadorFn extends BaseActivity{
    ActivityManager activityManager;

    ImageButton btn_ok, btn_host, btn_receptor;
    TextView text_selecione_fn, text_host_info, text_receptor_info, text_ip, text_porta, text_aguardando, text_aguardando_outro;
    FrameLayout frame_conectar_host, frame_conectar_receptor;
    EditText edit_ip, edit_porta;
    ActivityResultLauncher<Intent> launcherHost;
    ActivityResultLauncher<Intent> launcherReceptor;
    ImageView resultado;
    volatile String hasheObtido;
    String IP, porta_receptor = "5000";
    ProgressBar progressBar;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comparador_fn);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        setupMenu();

        btn_ok = findViewById(R.id.btn_ok);
        text_selecione_fn = findViewById(R.id.text_selecione_fn);
        btn_host = findViewById(R.id.btn_host);
        btn_receptor = findViewById(R.id.btn_receptor);
        text_host_info = findViewById(R.id.text_host_info);
        text_receptor_info = findViewById(R.id.text_recpetor_info);
        frame_conectar_host = findViewById(R.id.frame_conectar_host);
        text_ip = findViewById(R.id.text_ip);
        text_porta = findViewById(R.id.text_porta);
        frame_conectar_receptor = findViewById(R.id.frame_conectar_receptor);
        edit_ip = findViewById(R.id.edit_ip);
        edit_porta = findViewById(R.id.edit_porta);
        text_aguardando = findViewById(R.id.text_aguardando);
        text_aguardando_outro = findViewById(R.id.text_aguardando_outro);
        progressBar = findViewById(R.id.progressBar);
        resultado = findViewById(R.id.resultado);

        renderMenu(
                new ImageButton[]{ btn_ok, btn_host, btn_receptor },
                new TextView[]{ encoder, encoderM, text_selecione_fn, text_host_info, text_receptor_info, text_ip, text_porta, text_aguardando, text_aguardando_outro },
                new FrameLayout[]{ frame_conectar_host, frame_conectar_receptor },
                new EditText[]{ edit_ip, edit_porta }
        );

        btn_host.setOnClickListener(v -> {
            if (isWifiEnabled(this)) {
                new AlertDialog.Builder(this)
                        .setTitle("Wi-Fi desligado")
                        .setMessage("Ative o Wi-Fi para que a sincronização funcione corretamente.")
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                String IP_host = getIP_host();
                if(IP_host != null){
                    text_ip.setText(IP_host);
                    text_porta.setText("5000");
                }
                desativarBtns();
                frame_conectar_host.setVisibility(TextView.VISIBLE);
                host();
            }
        });
        btn_receptor.setOnClickListener(v -> {
            if (isWifiEnabled(this)) {
                new AlertDialog.Builder(this)
                        .setTitle("Wi-Fi desligado")
                        .setMessage("Ative o Wi-Fi para que a sincronização funcione corretamente.")
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                desativarBtns();
                frame_conectar_receptor.setVisibility(TextView.VISIBLE);
                btn_ok.setVisibility(View.VISIBLE);
            }
        });
        edit_ip.setOnClickListener(v -> {
            edit_ip.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(edit_ip, InputMethodManager.SHOW_IMPLICIT);
        });
        edit_porta.setOnClickListener(v -> {
            edit_porta.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(edit_porta, InputMethodManager.SHOW_IMPLICIT);
        });
        edit_ip.setOnFocusChangeListener((v, hasFocus) -> edit_ip.setCursorVisible(hasFocus));
        edit_porta.setOnFocusChangeListener((v, hasFocus) -> edit_porta.setCursorVisible(hasFocus));
        btn_ok.setOnClickListener(v -> {
            IP = String.valueOf(edit_ip.getText());
            porta_receptor = String.valueOf(edit_porta.getText());
            frame_conectar_receptor.setEnabled(false);
            frame_conectar_receptor.setVisibility(View.INVISIBLE);
            btn_ok.setVisibility(View.INVISIBLE);
            receptor();
        });


        launcherHost = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            hasheObtido = data.getStringExtra("hash");
                            Log.i("info", hasheObtido);
                        }
                    }
                }
        );

        launcherReceptor = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            hasheObtido = data.getStringExtra("hash");
                            Log.i("info", hasheObtido);
                        }
                    }
                }
        );

        resultado.setOnClickListener(v -> {
            ativarBtns();
            resultado.setVisibility(View.INVISIBLE);
        });
    }
    public boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager == null || !wifiManager.isWifiEnabled();
    }
    public String getIP_host() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @SuppressLint("SetTextI18n")
    public void host() {
        new Thread(() -> {
            Socket socket = null;
            ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(5000);
                socket = serverSocket.accept();
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                runOnUiThread(() -> {
                    text_aguardando.setText("CONEXÃO ESTABELECIDA");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    text_aguardando.setText("REDIRECIONANDO PARA SHA-256");
                    Toast.makeText(ComparadorFn.this, "selecione os arquivos que deseja comparar.", Toast.LENGTH_SHORT).show();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    Intent intent = new Intent(this, SHA256.class);
                    intent.putExtra("origem", "Comparador");
                    launcherHost.launch(intent);
                });

                while(hasheObtido == null) {
                    Thread.sleep(100);
                }

                runOnUiThread(() -> {
                    Log.i("info", "rodando UI");
                    text_selecione_fn.setVisibility(View.INVISIBLE);
                    text_host_info.setVisibility(View.INVISIBLE);
                    text_receptor_info.setVisibility(View.INVISIBLE);
                    frame_conectar_host.setVisibility(View.INVISIBLE);
                    btn_receptor.setVisibility(View.INVISIBLE);
                    btn_host.setVisibility(View.INVISIBLE);
                    text_aguardando_outro.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                });

                String hashesRecebidos = reader.readLine();

                runOnUiThread(() -> {
                    text_aguardando_outro.setText("INFORMAÇÕES RECEBIDAS. PROCESSANDO ...");
                });
                Thread.sleep(1000);

                runOnUiThread(() -> {
                    text_aguardando_outro.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    resultado.setVisibility(View.VISIBLE);
                });
                if(!hashesRecebidos.equals(hasheObtido)){
                    runOnUiThread(() -> resultado.setImageResource(R.drawable.reprovado));
                    System.out.println("reprovado");
                    writer.println("reprovado\n");
                } else {
                    runOnUiThread(() -> resultado.setImageResource(R.drawable.aprovado));
                    writer.println("aprovado\n");
                    System.out.println("aprovado");
                    System.out.println(hashesRecebidos);
                    System.out.println(hasheObtido);
                }

                socket.close();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e("SocketError", "Erro ao fechar socket: " + e.getMessage());
                    }
                }
            }
        }).start();
    }
    public void receptor() {
        new Thread(() -> {
            Socket socket = null;
            try {
                socket = new Socket(IP, 5000);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                runOnUiThread(() -> {
                    Intent intent = new Intent(ComparadorFn.this, SHA256.class);
                    intent.putExtra("origem", "Comparador");
                    launcherReceptor.launch(intent);
                });
                while(hasheObtido == null){
                    Thread.sleep(100);
                }
                runOnUiThread(() -> {
                    text_selecione_fn.setVisibility(View.INVISIBLE);
                    text_host_info.setVisibility(View.INVISIBLE);
                    text_receptor_info.setVisibility(View.INVISIBLE);
                    frame_conectar_host.setVisibility(View.INVISIBLE);
                    frame_conectar_receptor.setVisibility(View.INVISIBLE);
                    btn_ok.setEnabled(false);
                    btn_ok.setVisibility(View.INVISIBLE);
                    btn_receptor.setVisibility(View.INVISIBLE);
                    btn_host.setVisibility(View.INVISIBLE);
                });
                writer.println(hasheObtido + "\n");
                
                String result = reader.readLine();
                runOnUiThread(() -> {
                    text_aguardando_outro.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    resultado.setVisibility(View.VISIBLE);
                });
                if(result.equals("aprovado")){
                    runOnUiThread(() -> resultado.setImageResource(R.drawable.aprovado));
                } else {
                    runOnUiThread(() -> resultado.setImageResource(R.drawable.reprovado));
                }
                socket.close();

            } catch (UnknownHostException e) {
                Log.e("SocketError", "Host desconhecido: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ComparadorFn.this, "IP inválido ou host não encontrado", Toast.LENGTH_SHORT).show());

            } catch (SocketTimeoutException e) {
                Log.e("SocketError", "Tempo de conexão excedido: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ComparadorFn.this, "Tempo de conexão excedido", Toast.LENGTH_SHORT).show());

            } catch (IOException e) {
                Log.e("SocketError", "Erro de I/O: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ComparadorFn.this, "Erro ao conectar: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e("SocketError", "Erro ao fechar socket: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    public void desativarBtns(){
        text_receptor_info.setAlpha(0.25f);
        text_selecione_fn.setAlpha(0.25f);
        text_host_info.setAlpha(0.25f);
        btn_receptor.setAlpha(0.25f);
        btn_host.setAlpha(0.25f);
        text_receptor_info.setEnabled(false);
        text_host_info.setEnabled(false);
        btn_receptor.setEnabled(false);
        btn_host.setEnabled(false);
        text_selecione_fn.setEnabled(false);
    }
    public void ativarBtns(){
        text_selecione_fn.setAlpha(1f);
        text_receptor_info.setAlpha(1f);
        text_host_info.setAlpha(1f);
        btn_receptor.setAlpha(1f);
        btn_host.setAlpha(1f);
        text_receptor_info.setEnabled(true);
        text_host_info.setEnabled(true);
        btn_receptor.setEnabled(true);
        btn_host.setEnabled(true);
        text_selecione_fn.setEnabled(true);
    }

}
