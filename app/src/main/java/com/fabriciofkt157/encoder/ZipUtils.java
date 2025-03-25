package com.fabriciofkt157.encoder;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static byte[] zipFile(Context context, List<Uri> folderUris, List<Uri> fileUris) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

            // Adicionar arquivos das pastas ao ZIP
            for (Uri folderUri : folderUris) {
                DocumentFile folder = DocumentFile.fromTreeUri(context, folderUri);
                if (folder != null && folder.isDirectory()) {
                    addFolderToZip(context, zipOutputStream, folder, folder.getName() + "/");
                }
            }

            // Adicionar arquivos soltos ao ZIP
            for (Uri fileUri : fileUris) {
                addFileToZip(context, zipOutputStream, fileUri, "");
            }

            zipOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            Log.e("ZipUtils", "Erro ao criar ZIP", e);
            return null;
        }
    }

    private static void addFolderToZip(Context context, ZipOutputStream zipOutputStream, DocumentFile folder, String folderPath) {
        try {
            // Criar a entrada da pasta no ZIP
            zipOutputStream.putNextEntry(new ZipEntry(folderPath));
            zipOutputStream.closeEntry();

            // Percorrer os arquivos dentro da pasta
            for (DocumentFile file : folder.listFiles()) {
                if (file.isDirectory()) {
                    addFolderToZip(context, zipOutputStream, file, folderPath + file.getName() + "/");
                } else {
                    addFileToZip(context, zipOutputStream, file.getUri(), folderPath);
                }
            }
        } catch (Exception e) {
            Log.e("ZipUtils", "Erro ao adicionar pasta ao ZIP", e);
        }
    }

    private static void addFileToZip(Context context, ZipOutputStream zipOutputStream, Uri fileUri, String parentPath) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) return;

            String fileName = getFileNameFromUri(context, fileUri);
            zipOutputStream.putNextEntry(new ZipEntry(parentPath + fileName));

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }

            zipOutputStream.closeEntry();
            inputStream.close();
        } catch (Exception e) {
            Log.e("ZipUtils", "Erro ao adicionar arquivo ao ZIP", e);
        }
    }

    private static String getFileNameFromUri(Context context, Uri uri) {
        String path = uri.getPath();
        return path != null ? path.substring(path.lastIndexOf('/') + 1) : "arquivo";
    }
}

