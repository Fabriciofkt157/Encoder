package com.fabriciofkt157.encoder;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    public static void copiarParaInterno (List<Uri> pastas, List<Uri> arquivos, Context context) {
        File pastaTemp = new File(context.getFilesDir(), "temp");
        if(!pastaTemp.exists()){
            if(!pastaTemp.mkdirs()) Log.e("ERRO", "pasta temp não pode ser criada");
        }

        int totalTasks = pastas.size() + arquivos.size();
        CountDownLatch latch = new CountDownLatch(totalTasks);

        for(Uri pastaUri : pastas){
            String documentoId = DocumentsContract.getTreeDocumentId(pastaUri);
            documentoId = documentoId.replace("primary:", "");
            if(documentoId.contains("/")){
                String[] parts = documentoId.split("/");
                documentoId = parts[parts.length - 1];
            }
            Log.i("teste uri", documentoId);
            Log.i("pastaPaiString", documentoId);
            File pastaPai = new File(context.getFilesDir(), "temp/" + documentoId);
            Log.i("pastaPai", String.valueOf(pastaPai));
            if(!pastaPai.exists()){
                if(!pastaPai.mkdirs()) Log.e("ERRO", "pasta pai não pode ser criada");
            }
            DocumentFile pasta = DocumentFile.fromTreeUri(context, pastaUri);
            String caminhoRelativo = "temp/" + documentoId + "/";
            if(pasta != null && pasta.isDirectory()){
                copiarPastaRecursivamente(pasta, caminhoRelativo , context);
                latch.countDown();
            }

        }
        for(Uri arquivoUri : arquivos) {
            copiarArquivosParaInterno(arquivoUri, pastaTemp, context);
            latch.countDown();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static void copiarArquivosParaInterno(Uri arquivo, File pastaTemp, Context context) {
        ContentResolver resolver = context.getContentResolver();

        String nomeArquivo = "nao_identificado";

        Cursor cursor = context.getContentResolver().query(arquivo, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) nomeArquivo = cursor.getString(nameIndex);
            cursor.close();
        }

        File arquivoDestino = new File(pastaTemp, nomeArquivo);

        try(InputStream input = resolver.openInputStream(arquivo);
            OutputStream output = new FileOutputStream(arquivoDestino)){

            byte[] buffer = new byte[8192];
            int bytesLidos;
            while((bytesLidos = Objects.requireNonNull(input).read(buffer)) != -1){
                output.write(buffer, 0, bytesLidos);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void copiarPastaRecursivamente(DocumentFile pasta, String caminhoRelativo, Context context){
        for(DocumentFile file : pasta.listFiles()) {
            if(file.isDirectory()) {
                copiarPastaRecursivamente(file,  caminhoRelativo + file.getName() + "/", context);
            }
            else {
                copiarDocument(file, caminhoRelativo, context);
            }
        }
    }

    public static void copiarDocument(DocumentFile file, String caminhoRelativo, Context context){
        ContentResolver resolver = context.getContentResolver();
        File pastaAnterior = new File(context.getFilesDir() + "/" + caminhoRelativo);
        if(!pastaAnterior.exists()){
            if(!pastaAnterior.mkdirs()) Log.e("ERRO", "pasta anterior não pode ser criada");
        }
        File arquivo = new File(context.getFilesDir() + "/" + caminhoRelativo, Objects.requireNonNull(file.getName()));
        try(InputStream input = resolver.openInputStream(file.getUri());
            OutputStream output = new FileOutputStream(arquivo)){

            byte[] buffer = new byte[8192];
            int bytesLidos;
            while((bytesLidos = Objects.requireNonNull(input).read(buffer)) != -1){
                output.write(buffer, 0, bytesLidos);
            }
        }
        catch (IOException e){
            Log.e("Erro ao copiar Document", String.valueOf(e));
        }
    }

    public static void zip(File pasta, File arquivoZip){
        try (FileOutputStream fos = new FileOutputStream(arquivoZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            ziparPasta(zos, pasta, pasta.getName());
            Log.d("Executor", "Zip finalizado");
        } catch (IOException e) {
            Log.e("ERRO", String.valueOf(e));
        }
    }

    public static void ziparPasta(ZipOutputStream zos, File pasta, String pastaParente){

        for(File arquivo : Objects.requireNonNull(pasta.listFiles())){
            if(arquivo.isDirectory()){
                ziparPasta(zos, arquivo, pastaParente + "/" + arquivo.getName());
            }
            else {
                try(FileInputStream fis = new FileInputStream(arquivo)){
                    ZipEntry zipEntry = new ZipEntry(pastaParente + "/" + arquivo.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[8192];
                    int bytesLidos;

                    while((bytesLidos = fis.read(buffer)) != -1){
                        zos.write(buffer, 0, bytesLidos);
                    }
                    zos.closeEntry();

                } catch (IOException e){
                    Log.e("ERRO ao processar a pasta", String.valueOf(e));
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void unzip(Context context, DocumentFile arquivoZip, DocumentFile pastaDestino) {
        byte[] buffer = new byte[8192];

        try (InputStream fis = context.getContentResolver().openInputStream(arquivoZip.getUri());
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entradaZip;
            while ((entradaZip = zis.getNextEntry()) != null) {
                String nomeEntrada = entradaZip.getName();

                DocumentFile arquivoDestino = criarHierarquia(pastaDestino, nomeEntrada, entradaZip.isDirectory());

                if (entradaZip.isDirectory()) {
                    continue;
                }

                try (OutputStream os = context.getContentResolver().openOutputStream(arquivoDestino.getUri())) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        Objects.requireNonNull(os).write(buffer, 0, len);
                    }
                }
            }

            Log.d("ZipUtils", "Unzip finalizado");
        } catch (IOException e) {
            Log.e("ZipUtils", "Erro ao descompactar", e);
        }
    }
    private static DocumentFile criarHierarquia(DocumentFile pastaRaiz, String caminhoRelativo, boolean isDiretorio) {
        String[] partes = caminhoRelativo.split("/");
        DocumentFile atual = pastaRaiz;

        for (int i = 0; i < partes.length; i++) {
            String nome = partes[i];
            boolean ultimaParte = (i == partes.length - 1);

            DocumentFile existente = Objects.requireNonNull(atual).findFile(nome);
            if (existente != null) {
                atual = existente;
            } else {
                if (ultimaParte && !isDiretorio) {
                    atual = atual.createFile("application/octet-stream", nome);
                } else {
                    atual = atual.createDirectory(nome);
                }
            }
        }

        return atual;
    }
}
