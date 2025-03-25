package com.fabriciofkt157.encoder;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

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
    public static void logarMap(Map<Uri, byte[]> uriMap) {
        if (uriMap.isEmpty()) {
            Log.d("UriStorage", "O mapa está vazio.");
            return;
        }

        for (Map.Entry<Uri, byte[]> entry : uriMap.entrySet()) {
            String uriString = entry.getKey().toString();
            String chaveHex = bytesToHex(entry.getValue());

            Log.d("UriStorage", "URI: " + uriString + " | Chave: " + chaveHex);
        }
    }



    public static void deleteFileFromUri(Context context, Uri fileUri) {
        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, fileUri);
            if (documentFile != null && documentFile.exists()) {
                if (documentFile.delete()) {
                    Log.d("DELETE_FILE", "Arquivo deletado com sucesso: " + fileUri);
                } else {
                    Log.e("DELETE_FILE", "Falha ao deletar o arquivo: " + fileUri);
                }
            } else {
                Log.e("DELETE_FILE", "Arquivo não encontrado: " + fileUri);
            }
        } catch (Exception e) {
            Log.e("DELETE_FILE", "Erro ao deletar arquivo: " + fileUri, e);
        }
    }


    private static final String FILE_NAME = "uri_armazenado.txt";

    //salvar o mapa no arquivo
    public static void salvarMap(Context context, Map<String, byte[]> mapaDeChavesSalvas) {
        try (FileOutputStream fos = context.openFileOutput("uri_armazenado.txt", Context.MODE_PRIVATE);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {

            JSONArray jsonArray = new JSONArray();
            for (Map.Entry<String, byte[]> entry : mapaDeChavesSalvas.entrySet()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("nome", entry.getKey());
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
    public static Map<String, byte[]> carregarMap(Context context) {
        File file = new File(context.getFilesDir(), "uri_armazenado.txt");

        if (!file.exists()) {
            Log.e("UriStorage", "Arquivo uri_armazenado.txt não encontrado, criando novo.");
            return new HashMap<>();
        }

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }

            JSONArray jsonArray = new JSONArray(jsonString.toString());
            Map<String, byte[]> uriMap = new HashMap<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String nomeArquivo = String.valueOf(jsonObject.getString("nome"));
                byte[] chave = hexToBytes(jsonObject.getString("chave"));
                uriMap.put(nomeArquivo, chave);
            }

            return uriMap;

        } catch (Exception e) {
            Log.e("UriStorage", "Erro ao carregar o Map", e);
            Map<String, byte[]> mapGerado= new HashMap<>();
            mapGerado.put("none", null);
            salvarMap(context, mapGerado);
            return mapGerado;
        }
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

