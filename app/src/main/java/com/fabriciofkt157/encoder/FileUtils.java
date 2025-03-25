package com.fabriciofkt157.encoder;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileUtils {

    /*public static File getFileFromUri(Context context, Uri uri, Uri pastaDestino) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        String fileName = "temp_" + System.currentTimeMillis();

        File tempFile = new File(Objects.requireNonNull(context.getContentResolver().openFileDescriptor(pastaDestino, "rw")).getFileDescriptor().toString(), fileName);

        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = Objects.requireNonNull(inputStream).read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        outputStream.close();

        return tempFile;
    }*/


    public static Uri salvarArquivo(Context context, Uri uriPasta, String nomeArquivo, byte[] dados) {
        try {
            // Cria o documento dentro da pasta selecionada
            Uri uriDiretorio = DocumentsContract.buildDocumentUriUsingTree(uriPasta,
                    DocumentsContract.getTreeDocumentId(uriPasta));

            Uri arquivoUri = DocumentsContract.createDocument(
                    context.getContentResolver(),
                    uriDiretorio,
                    "application/octet-stream",
                    nomeArquivo
            );

            if (arquivoUri != null && dados != null) {
                try (OutputStream outputStream = context.getContentResolver().openOutputStream(arquivoUri)) {
                    if (outputStream != null) {
                        outputStream.write(dados);
                        outputStream.flush();
                    }
                }
            }

            return arquivoUri;
        } catch (Exception e) {
            Log.e("SalvarArquivo", "Erro ao salvar arquivo", e);
            Toast.makeText(context, "Erro ao salvar arquivo...", Toast.LENGTH_SHORT).show();
        }
        return null;
    }
    public static void salvarListaEmArquivo(Context context, Uri uriDestino, List<Map<String, byte[]>> lista) {
        try {
            // Obtém um OutputStream do Uri usando ContentResolver
            OutputStream outputStream = context.getContentResolver().openOutputStream(uriDestino);
            if (outputStream == null) {
                throw new IOException("Não foi possível abrir OutputStream para o Uri fornecido.");
            }

            // Usa ObjectOutputStream para serializar e escrever no arquivo
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(lista);

            oos.close();
            outputStream.close();

            Log.d("SalvarArquivo", "Dados salvos com sucesso no Uri: " + uriDestino.toString());
        } catch (IOException e) {
            Log.e("SalvarArquivo", "Erro ao salvar no Uri", e);
        }
    }


    public static void deleteFileFromUri(Context context, Uri fileUri) {
        try {
            int deletedRows = context.getContentResolver().delete(fileUri, null, null);
            if (deletedRows > 0) {
                Log.d("DELETE_FILE", "Arquivo deletado com sucesso: " + fileUri);
            } else {
                Log.e("DELETE_FILE", "Não foi possível deletar o arquivo: " + fileUri);
            }
        } catch (Exception e) {
            Log.e("DELETE_FILE", "Erro ao deletar arquivo: " + fileUri, e);
        }
    }

    private static final String FILE_NAME = "uri_armazenado.txt";

    //salvar o mapa no arquivo
    public static void salvarMap(Context context, Map<Uri, byte[]> uriMap) {
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {

            JSONArray jsonArray = new JSONArray();
            for (Map.Entry<Uri, byte[]> entry : uriMap.entrySet()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uri", entry.getKey().toString());
                jsonObject.put("chave", bytesToHex(entry.getValue()));
                jsonArray.put(jsonObject);
            }

            writer.write(jsonArray.toString());
            writer.flush();
        } catch (Exception e) {
            Log.e("UriStorage", "Erro ao salvar o Map", e);
        }
    }

    //ler o mapa do arquivo
    public static Map<Uri, byte[]> carregarMap(Context context) {
        Map<Uri, byte[]> uriMap = new HashMap<>();

        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }

            JSONArray jsonArray = new JSONArray(jsonString.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Uri uri = Uri.parse(jsonObject.getString("uri"));
                byte[] chave = hexToBytes(jsonObject.getString("chave"));
                uriMap.put(uri, chave);
            }
        } catch (Exception e) {
            Log.e("UriStorage", "Erro ao carregar o Map", e);
        }

        return uriMap;
    }

    // Converter byte[] para String hexadecimal
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // Converter String hexadecimal para byte[]
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}

