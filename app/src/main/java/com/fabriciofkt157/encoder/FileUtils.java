package com.fabriciofkt157.encoder;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileUtils {
    public static Uri salvarArquivo(Context context, boolean descompilar, Uri uriPasta, String nomeArquivo, byte[] dados) {
        try {
            // Cria o documento dentro da pasta selecionada
            Uri uriDiretorio;
            if(!descompilar) {
                uriDiretorio = DocumentsContract.buildDocumentUriUsingTree(uriPasta,
                        DocumentsContract.getTreeDocumentId(uriPasta));
            } else {
                uriDiretorio = uriPasta;
            }

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

    public static Uri criarPasta(Context context, Uri uriPai, String nomePasta) {
        try {
            // Obter o ID correto do documento para manipulação
            String documentoId = DocumentsContract.getTreeDocumentId(uriPai);
            Uri uriDiretorio = DocumentsContract.buildChildDocumentsUriUsingTree(uriPai, documentoId);

            // Verificar se a pasta já existe
            Uri uriExistente = verificarPastaExistente(context, uriDiretorio, nomePasta);
            if (uriExistente != null) {
                // A pasta já existe, retornar o URI dela
                Log.d("CriarPasta", "Pasta já existe: " + uriExistente);
                return uriExistente;
            }

            // Criar a nova pasta
            Uri uriNovaPasta = DocumentsContract.createDocument(
                    context.getContentResolver(),
                    uriDiretorio,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    nomePasta
            );

            if (uriNovaPasta != null) {
                Log.d("CriarPasta", "Pasta criada com sucesso: " + uriNovaPasta);
            } else {
                Log.e("CriarPasta", "Falha ao criar a pasta: " + nomePasta);
            }

            return uriNovaPasta; // Retorna o URI da nova pasta
        } catch (Exception e) {
            Log.e("CriarPasta", "Erro ao criar pasta", e);
            return null;
        }
    }

    private static Uri verificarPastaExistente(Context context, Uri uriPai, String nomePasta) {
        try {
            // Obtém o ID do documento do URI pai
            String documentId = DocumentsContract.getDocumentId(uriPai);
            // Constrói o URI que lista os documentos filhos
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uriPai, documentId);

            Cursor cursor = context.getContentResolver().query(
                    childrenUri,
                    null, // Todas as colunas
                    null, // Sem filtro
                    null, // Sem argumentos de filtro
                    null  // Sem ordenação
            );
            if (cursor != null) {
                int displayNameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                int documentIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                if (displayNameIndex == -1 || documentIdIndex == -1) {
                    Log.e("VerificarPastaExistente", "Colunas necessárias não foram encontradas.");
                    cursor.close();
                    return null;
                }
                while (cursor.moveToNext()) {
                    String nomeDocumento = cursor.getString(displayNameIndex);
                    if (nomeDocumento.equals(nomePasta)) {
                        String docId = cursor.getString(documentIdIndex);
                        Uri uriDocumento = DocumentsContract.buildDocumentUriUsingTree(uriPai, docId);
                        cursor.close();
                        return uriDocumento;
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("VerificarPastaExistente", "Erro ao verificar se a pasta existe", e);
        }
        return null;
    }

    public static boolean temPermissaoParaEscrever(Context context, Uri uri) {
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        context.getContentResolver().takePersistableUriPermission(uri, flags);

        return (context.getContentResolver().getPersistedUriPermissions().stream()
                .anyMatch(p -> p.getUri().equals(uri) && p.isWritePermission()));
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

            Log.d("SalvarArquivo", "Dados salvos com sucesso no Uri: " + uriDestino);
        } catch (IOException e) {
            Log.e("SalvarArquivo", "Erro ao salvar no Uri", e);
        }
    }

    public static Uri criarEstruturaDePastas(Context context, Uri uriRaiz, String caminho) throws FileNotFoundException {
        String[] pastas = caminho.split("/");
        Log.i("pastas", Arrays.toString(pastas));

        // Se a primeira entrada for vazia, removê-la
        if (pastas.length > 0 && pastas[0].isEmpty()) {
            pastas = Arrays.copyOfRange(pastas, 1, pastas.length);
        }

        Uri uriAtual = uriRaiz;
        Log.i("estrutura pastas, uriRaiz", uriRaiz.toString());
        Log.i("pastas", Arrays.toString(pastas));

        for (String pasta : pastas) {
            Log.i("pasta", pasta);
            Log.i("Uri atual", uriAtual.toString());

            Uri novaPasta = verificarPastaExistente(context, uriAtual, pasta);
            if (novaPasta == null) {
                novaPasta = DocumentsContract.createDocument(
                        context.getContentResolver(),
                        uriAtual,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        pasta
                );
            }

            if (novaPasta != null) {
                uriAtual = novaPasta;
            } else {
                Log.e("CriarPastas", "Erro ao criar ou encontrar a pasta: " + pasta);
                return uriRaiz;
            }
        }
        return uriAtual;
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
                String nomeArquivo = jsonObject.getString("nome");
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
    public static void limparTemp(Context context){
        File pastaTemp = new File(context.getFilesDir(), "temp");
        limparRecursivamente(context, pastaTemp);
    }
    public static void limparRecursivamente(Context context, File arquivo_pasta){
        if(arquivo_pasta.isDirectory()){
            for(File pastaFilha : Objects.requireNonNull(arquivo_pasta.listFiles())){
                limparRecursivamente(context, pastaFilha);
            }
        }
        arquivo_pasta.delete();
    }

    // Converter byte[] para String hexadecimal
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // Converter String hexadecimal para byte[]
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}

