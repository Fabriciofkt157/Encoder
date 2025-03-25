package com.fabriciofkt157.encoder;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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


    public static boolean deleteFileFromUri(Context context, Uri fileUri) {
        try {
            int deletedRows = context.getContentResolver().delete(fileUri, null, null);
            if (deletedRows > 0) {
                Log.d("DELETE_FILE", "Arquivo deletado com sucesso: " + fileUri);
                return true;
            } else {
                Log.e("DELETE_FILE", "Não foi possível deletar o arquivo: " + fileUri);
                return false;
            }
        } catch (Exception e) {
            Log.e("DELETE_FILE", "Erro ao deletar arquivo: " + fileUri, e);
            return false;
        }
    }

    public static void deleteTempFiles(Uri pastaDestino) {
        File dir = new File(Objects.requireNonNull(pastaDestino.getPath()));
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("temp")) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            Log.d("DELETE_TEMP", "Arquivo temporário deletado: " + file.getName());
                        } else {
                            Log.e("DELETE_TEMP", "Não foi possível deletar o arquivo: " + file.getName());
                        }
                    }
                }
            }
        }
    }
}

