package com.se1605.util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

public class FileEncrypterDecrypter {
    private SecretKey secretKey;

    private Cipher cipher;

    public FileEncrypterDecrypter(String secretKeyString) {
        // Create a message digest object using the SHA-256 algorithm
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // Generate a byte array from the secret key string
        byte[] keyBytes = secretKeyString.getBytes();

        // Hash the byte array using the message digest object
        byte[] hashedBytes = sha256.digest(keyBytes);

        // Use the hashed byte array to create a new SecretKeySpec object
        secretKey = new SecretKeySpec(hashedBytes, "AES");

        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public void encrypt(InputStream inputStream, String filePath) {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        try (
                FileOutputStream fileOut = new FileOutputStream(filePath);
                CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)
        ) {
            fileOut.write(iv);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cipherOut.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void decrypt(String inputFilePath, String outputFilePath) throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        try (FileInputStream fileIn = new FileInputStream(inputFilePath)) {
            byte[] fileIv = new byte[16];
            fileIn.read(fileIv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(fileIv));

            try (
                    CipherInputStream cipherIn = new CipherInputStream(fileIn, cipher);
                    FileOutputStream fileOut = new FileOutputStream(outputFilePath)
            ) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = cipherIn.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
