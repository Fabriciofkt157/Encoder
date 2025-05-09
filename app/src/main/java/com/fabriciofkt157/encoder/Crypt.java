package com.fabriciofkt157.encoder;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    public static void criptografarArquivo(Context context, Uri uriEntrada, Uri uriSaida, byte[] chaveAES){
        try (
                InputStream fis = context.getContentResolver().openInputStream(uriEntrada);
                OutputStream fos = context.getContentResolver().openOutputStream(uriSaida)
        ) {
            if (fis == null || fos == null) {
                Log.e("AESCrypt", "Erro ao abrir arquivos de entrada ou saída.");
                return;
            }

            // Criar IV aleatório (16 bytes)
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Criar chave AES
            SecretKey secretKey = new SecretKeySpec(chaveAES, "AES");

            // Inicializar o Cipher no modo ENCRYPT_MODE
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            // Escrever IV no início do arquivo
            fos.write(iv);

            // Criar buffer para leitura e criptografia
            byte[] buffer = new byte[1024*1024*10];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] dadosCriptografados = cipher.update(buffer, 0, bytesRead);
                if (dadosCriptografados != null) {
                    fos.write(dadosCriptografados);
                }
            }

            // Finalizar a criptografia
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
                fos.write(finalBytes);
            }
            Log.d("AESCrypt", "Arquivo criptografado com sucesso!");
        } catch (Exception e) {
            Log.e("AESCrypt", "Erro na criptografia: " + e.getMessage());
        }
    }

    public static boolean descriptografarArquivo(Context context, Uri uriEntrada, Uri uriSaida, byte[] chaveAES) {
        try (
                InputStream fis = context.getContentResolver().openInputStream(uriEntrada);
                OutputStream fos = context.getContentResolver().openOutputStream(uriSaida)
        ) {
            if (fis == null || fos == null) {
                Log.e("AESCrypt", "Erro ao abrir arquivos de entrada ou saída.");
                return false;
            }

            // Ler o IV (16 bytes) do início do arquivo
            byte[] iv = new byte[16];
            if (fis.read(iv) != 16) {
                Log.e("AESCrypt", "Erro ao ler o IV.");
                return false;
            }
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Criar chave AES
            SecretKey secretKey = new SecretKeySpec(chaveAES, "AES");

            // Inicializar o Cipher no modo DECRYPT_MODE
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // Criar buffer para leitura e descriptografia
            byte[] buffer = new byte[1024*1024*10];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] dadosDescriptografados = cipher.update(buffer, 0, bytesRead);
                if (dadosDescriptografados != null) {
                    fos.write(dadosDescriptografados);
                }
            }

            // Finalizar a descriptografia
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
                fos.write(finalBytes);
            }
            Log.d("AESCrypt", "Arquivo descriptografado com sucesso!");
            return true;
        } catch (Exception e) {
            Log.e("AESCrypt", "Erro na descriptografia: " + e.getMessage());
            //Toast.makeText(context, "Erro ao descriptografar o arquivo" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }



    public static byte[] gerarChaveAES(String key, int keySize) throws NoSuchAlgorithmException {
        if(key != null){
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes());
            return Arrays.copyOf(hash, keySize / 8);
        }
        else {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(keySize, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return secretKey.getEncoded();
        }
    }
}