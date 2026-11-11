package com.nvllz.piggsy.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class BackupCrypto {

    private static final String TAG = "BackupCrypto";
    private static final String CRYPTO_PREFS = "backup_crypto";
    private static final String KEYSTORE_ALIAS = "piggsy_backup_chain";

    public static final int PBKDF2_ITERATIONS = 600_000;
    public static final int MAX_IMPORT_BYTES = 25_000_000;

    private static final byte[] MAGIC = new byte[]{'P', 'I', 'G', 'G', 'S', 'Y', 'E', 'N', 'C'};
    private static final int VERSION = 2;
    private static final int KDF_ID_PBKDF2_SHA256 = 1;
    private static final int DEK_ALGO_ID_AES_GCM = 1;
    private static final int MIN_PBKDF2_ITERATIONS = 100_000;
    private static final int MAX_PBKDF2_ITERATIONS = 2_000_000;
    private static final int MAX_HEADER_BYTES = 1024;
    private static final int SALT_SIZE = 16;
    private static final int IV_SIZE = 12;
    private static final int KEY_SIZE_BYTES = 32;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_TAG_BYTES = GCM_TAG_BITS / 8;
    private static final int ENCODED_DEK_SIZE = IV_SIZE + KEY_SIZE_BYTES + GCM_TAG_BYTES;
    private static final int HEADER_BYTES = 2 + SALT_SIZE + 1 + 4 + 1 + 2 + ENCODED_DEK_SIZE;

    private static final byte[] AAD_DEK_WRAP = "piggsyenc/dek-wrap/pw".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] AAD_KEK_WRAP = "piggsyenc/dek-wrap/keystore".getBytes(StandardCharsets.US_ASCII);

    private BackupCrypto() {
    }

    public static class InvalidEncryptionConfigException extends IllegalStateException {
        InvalidEncryptionConfigException(String message) {
            super(message);
        }

        InvalidEncryptionConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class CryptoConfig {
        final byte[] salt;
        final int iterations;
        final byte[] encDek;
        final byte[] wrappedKek;

        CryptoConfig(byte[] salt, int iterations, byte[] encDek, byte[] wrappedKek) {
            this.salt = salt;
            this.iterations = iterations;
            this.encDek = encDek;
            this.wrappedKek = wrappedKek;
        }
    }

    public static void setBackupPassword(Context context, char[] password) throws Exception {
        byte[] salt = random(SALT_SIZE);
        byte[] dek = random(KEY_SIZE_BYTES);
        SecretKey kek = pbkdf2(password, salt, PBKDF2_ITERATIONS);
        byte[] kekBytes = kek.getEncoded();
        try {
            byte[] encDek = gcmEncryptSoftware(kek, dek, AAD_DEK_WRAP);
            byte[] wrappedKek = gcmEncryptKeystore(createOrGetKeystoreKey(), kekBytes, AAD_KEK_WRAP);

            boolean written = cryptoPrefs(context).edit()
                    .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString("iterations", String.valueOf(PBKDF2_ITERATIONS))
                    .putString("enc_dek", Base64.encodeToString(encDek, Base64.NO_WRAP))
                    .putString("wrapped_kek", Base64.encodeToString(wrappedKek, Base64.NO_WRAP))
                    .commit();
            if (!written) throw new IllegalStateException("Failed to persist backup encryption configuration");
        } finally {
            zero(kekBytes);
            zero(dek);
        }

        new PreferenceUtil(context).setBackupEncrypted(true);
    }

    public static void clearBackupPassword(Context context) {
        cryptoPrefs(context).edit().clear().apply();
        PreferenceUtil legacy = new PreferenceUtil(context);
        legacy.setBackupSalt(null);
        legacy.setBackupEncDek(null);
        legacy.setBackupWrappedKek(null);
        deleteKeystoreKey();
        legacy.setBackupEncrypted(false);
    }

    public static boolean isLocalConfigHealthy(Context context) {
        try {
            CryptoConfig config = loadConfig(context);
            byte[] kek = gcmUnwrapKek(context, config.wrappedKek);
            zero(kek);
            return true;
        } catch (Exception unusable) {
            return false;
        }
    }

    public static boolean repairWithPassword(Context context, char[] password) {
        CryptoConfig config;
        try {
            config = loadConfig(context);
        } catch (InvalidEncryptionConfigException broken) {
            return false;
        }

        try {
            byte[] kek = gcmUnwrapKek(context, config.wrappedKek);
            zero(kek);
            return true;
        } catch (Exception unusable) {
        }

        byte[] dekBytes = null;
        try {
            SecretKey kek = pbkdf2(password, config.salt, config.iterations);
            byte[] kekBytes = kek.getEncoded();
            try {
                dekBytes = gcmDecrypt(new SecretKeySpec(kekBytes, "AES"), config.encDek, AAD_DEK_WRAP);
            } finally {
                zero(kekBytes);
            }

            byte[] newSalt = random(SALT_SIZE);
            SecretKey newKek = pbkdf2(password, newSalt, PBKDF2_ITERATIONS);
            byte[] newKekBytes = newKek.getEncoded();
            byte[] newEncDek = null;
            byte[] newWrappedKek = null;
            try {
                newEncDek = gcmEncryptSoftware(newKek, dekBytes, AAD_DEK_WRAP);
                newWrappedKek = gcmEncryptKeystore(recreatedKeystoreKey(), newKekBytes, AAD_KEK_WRAP);

                boolean written = cryptoPrefs(context).edit()
                        .putString("salt", Base64.encodeToString(newSalt, Base64.NO_WRAP))
                        .putString("iterations", String.valueOf(PBKDF2_ITERATIONS))
                        .putString("enc_dek", Base64.encodeToString(newEncDek, Base64.NO_WRAP))
                        .putString("wrapped_kek", Base64.encodeToString(newWrappedKek, Base64.NO_WRAP))
                        .commit();
                if (!written) throw new IllegalStateException("Failed to persist backup encryption configuration");
                return true;
            } finally {
                zero(newKekBytes);
                zero(newEncDek);
                zero(newWrappedKek);
            }
        } catch (Exception failure) {
            Log.e(TAG, "Local backup encryption recovery failed", failure);
            return false;
        } finally {
            zero(dekBytes);
        }
    }

    public static boolean isEncryptedBackup(byte[] file) {
        if (file == null || file.length < MAGIC.length + 1) return false;
        for (int i = 0; i < MAGIC.length; i++) {
            if (file[i] != MAGIC[i]) return false;
        }
        return true;
    }

    public static byte[] encryptBackup(Context context, byte[] json) throws Exception {
        if (!new PreferenceUtil(context).isBackupEncrypted()) return json;

        CryptoConfig config = loadConfig(context);
        byte[] kekBytes = null;
        byte[] dekBytes = null;
        try {
            kekBytes = gcmUnwrapKek(context, config.wrappedKek);
            dekBytes = gcmDecrypt(new SecretKeySpec(kekBytes, "AES"), config.encDek, AAD_DEK_WRAP);

            ByteBuffer header = ByteBuffer.allocate(2 + SALT_SIZE + 1 + 4 + 1 + 2 + ENCODED_DEK_SIZE);
            header.putShort((short) SALT_SIZE);
            header.put(config.salt);
            header.put((byte) KDF_ID_PBKDF2_SHA256);
            header.putInt(config.iterations);
            header.put((byte) DEK_ALGO_ID_AES_GCM);
            header.putShort((short) ENCODED_DEK_SIZE);
            header.put(config.encDek);

            byte[] headerBytes = header.array();
            byte[] headerPrefix = new byte[MAGIC.length + 1 + 4];
            ByteBuffer prefix = ByteBuffer.wrap(headerPrefix);
            prefix.put(MAGIC);
            prefix.put((byte) VERSION);
            prefix.putInt(headerBytes.length);

            byte[] aad = new byte[headerPrefix.length + headerBytes.length];
            System.arraycopy(headerPrefix, 0, aad, 0, headerPrefix.length);
            System.arraycopy(headerBytes, 0, aad, headerPrefix.length, headerBytes.length);

            byte[] payloadIv = random(IV_SIZE);
            Cipher payloadCipher = Cipher.getInstance("AES/GCM/NoPadding");
            payloadCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dekBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, payloadIv));
            payloadCipher.updateAAD(aad);
            byte[] payloadCt = payloadCipher.doFinal(json);

            ByteBuffer file = ByteBuffer.allocate(headerPrefix.length + headerBytes.length
                    + IV_SIZE + 4 + payloadCt.length);
            file.put(headerPrefix);
            file.put(headerBytes);
            file.put(payloadIv);
            file.putInt(payloadCt.length);
            file.put(payloadCt);
            return file.array();
        } finally {
            zero(kekBytes);
            zero(dekBytes);
        }
    }

    public static String decryptBackup(byte[] file, char[] password) throws Exception {
        if (!isEncryptedBackup(file)) {
            throw new IllegalArgumentException("Not an encrypted backup");
        }

        ByteBuffer buffer = ByteBuffer.wrap(file);
        buffer.position(MAGIC.length);
        int version = buffer.get();

        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported backup format version " + version);
        }
        return decryptBackupV2(buffer, password);
    }

    private static String decryptBackupV2(ByteBuffer buffer, char[] password) throws Exception {
        if (buffer.remaining() < 4) throw new IllegalArgumentException("Truncated backup header length");
        int headerLength = buffer.getInt();
        if (headerLength <= 0 || headerLength > MAX_HEADER_BYTES) {
            throw new IllegalArgumentException("Invalid backup header length");
        }
        if (headerLength > buffer.remaining()) throw new IllegalArgumentException("Backup header length exceeds file size");
        if (headerLength != HEADER_BYTES) throw new IllegalArgumentException("Invalid backup header length");

        byte[] header = new byte[headerLength];
        buffer.get(header);

        ByteBuffer aad = ByteBuffer.allocate(MAGIC.length + 1 + 4 + headerLength);
        aad.put(MAGIC);
        aad.put((byte) VERSION);
        aad.putInt(headerLength);
        aad.put(header);

        ByteBuffer fields = ByteBuffer.wrap(header);
        int saltLength = fields.getShort() & 0xFFFF;
        if (saltLength != SALT_SIZE) throw new IllegalArgumentException("Invalid backup salt length");
        if (fields.remaining() < saltLength + 1 + 4 + 1 + 2 + ENCODED_DEK_SIZE) {
            throw new IllegalArgumentException("Truncated backup header");
        }

        byte[] salt = new byte[saltLength];
        fields.get(salt);

        int kdfId = fields.get();
        if (kdfId != KDF_ID_PBKDF2_SHA256) throw new IllegalArgumentException("Unsupported backup KDF");
        int iterations = fields.getInt();
        if (iterations < MIN_PBKDF2_ITERATIONS || iterations > MAX_PBKDF2_ITERATIONS) {
            throw new IllegalArgumentException("Invalid backup KDF parameters");
        }
        if (fields.get() != DEK_ALGO_ID_AES_GCM) throw new IllegalArgumentException("Unsupported backup DEK algorithm");
        int encDekLength = fields.getShort() & 0xFFFF;
        if (encDekLength != ENCODED_DEK_SIZE) throw new IllegalArgumentException("Invalid backup encrypted DEK length");
        byte[] encDek = new byte[encDekLength];
        fields.get(encDek);
        if (fields.remaining() != 0) throw new IllegalArgumentException("Unexpected bytes in backup header");

        if (buffer.remaining() < IV_SIZE + 4 + GCM_TAG_BYTES) {
            throw new IllegalArgumentException("Truncated backup payload header");
        }
        byte[] payloadIv = new byte[IV_SIZE];
        buffer.get(payloadIv);
        int payloadLength = buffer.getInt();
        if (payloadLength < GCM_TAG_BYTES) throw new IllegalArgumentException("Invalid backup payload length");
        if (payloadLength > MAX_IMPORT_BYTES) throw new IllegalArgumentException("Backup payload too large");
        if (payloadLength > buffer.remaining()) throw new IllegalArgumentException("Backup payload length exceeds file size");
        if (buffer.remaining() != payloadLength) throw new IllegalArgumentException("Unexpected trailing bytes in backup");
        byte[] payloadCt = new byte[payloadLength];
        buffer.get(payloadCt);

        SecretKey kek = pbkdf2(password, salt, iterations);
        byte[] dekBytes = gcmDecrypt(kek, encDek, AAD_DEK_WRAP);
        try {
            byte[] json = gcmDecrypt(new SecretKeySpec(dekBytes, "AES"), payloadCt, payloadIv, aad.array());
            return new String(json, StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException corrupted) {
            throw new IllegalArgumentException("Backup is corrupted or has been modified", corrupted);
        } finally {
            zero(dekBytes);
        }
    }

    private static SharedPreferences cryptoPrefs(Context context) {
        return context.getSharedPreferences(CRYPTO_PREFS, Context.MODE_PRIVATE);
    }

    private static CryptoConfig loadConfig(Context context) {
        Map<String, ?> stored = cryptoPrefs(context).getAll();
        String salt = storedString(stored, "salt");
        String iterations = storedString(stored, "iterations");
        String encDek = storedString(stored, "enc_dek");
        String wrappedKek = storedString(stored, "wrapped_kek");

        if (salt == null || encDek == null) {
            throw new InvalidEncryptionConfigException("Backup encryption configuration is missing");
        }
        if (wrappedKek == null) {
            throw new InvalidEncryptionConfigException("Local Keystore wrap is missing");
        }
        if (iterations == null) {
            throw new InvalidEncryptionConfigException("Backup KDF iterations are missing");
        }

        int iterationsValue;
        try {
            iterationsValue = Integer.parseInt(iterations);
        } catch (NumberFormatException failure) {
            throw new InvalidEncryptionConfigException("Invalid backup KDF iterations", failure);
        }

        byte[] saltBytes = decode(salt);
        byte[] encDekBytes = decode(encDek);
        byte[] wrappedKekBytes = decode(wrappedKek);
        if (saltBytes.length != SALT_SIZE || encDekBytes.length != ENCODED_DEK_SIZE
                || iterationsValue < MIN_PBKDF2_ITERATIONS || iterationsValue > MAX_PBKDF2_ITERATIONS) {
            throw new InvalidEncryptionConfigException("Invalid backup encryption configuration");
        }
        return new CryptoConfig(saltBytes, iterationsValue, encDekBytes, wrappedKekBytes);
    }

    private static String storedString(Map<String, ?> stored, String key) {
        Object value = stored.get(key);
        return value instanceof String ? (String) value : null;
    }

    private static byte[] decode(String value) {
        if (value == null || value.isEmpty()) {
            throw new InvalidEncryptionConfigException("Missing backup encryption value");
        }
        try {
            return Base64.decode(value, Base64.NO_WRAP);
        } catch (IllegalArgumentException failure) {
            throw new InvalidEncryptionConfigException("Corrupt backup encryption value", failure);
        }
    }

    private static byte[] gcmUnwrapKek(Context context, byte[] wrapped) throws Exception {
        try {
            return gcmDecrypt(keystoreKey(context), wrapped, AAD_KEK_WRAP);
        } catch (javax.crypto.AEADBadTagException tampered) {
            throw new InvalidEncryptionConfigException("Local Keystore key cannot unwrap the stored KEK", tampered);
        }
    }

    private static SecretKey keystoreKey(Context context) throws Exception {
        KeyStore keyStore = androidKeyStore();
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            throw new InvalidEncryptionConfigException("Local Keystore key is missing");
        }
        SecretKey key = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        if (key == null) {
            throw new InvalidEncryptionConfigException("Local Keystore key is unreadable");
        }
        return key;
    }

    private static SecretKey createOrGetKeystoreKey() throws Exception {
        KeyStore keyStore = androidKeyStore();
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            SecretKey existing = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
            if (existing != null) {
                return existing;
            }
        }
        return generateKeystoreKey();
    }

    private static SecretKey recreatedKeystoreKey() throws Exception {
        KeyStore keyStore = androidKeyStore();
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            keyStore.deleteEntry(KEYSTORE_ALIAS);
        }
        return generateKeystoreKey();
    }

    private static SecretKey generateKeystoreKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BYTES * 8)
                .build());
        generator.generateKey();
        KeyStore keyStore = androidKeyStore();
        SecretKey key = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        if (key == null) {
            throw new InvalidEncryptionConfigException("Local Keystore key generation failed");
        }
        return key;
    }

    private static void deleteKeystoreKey() {
        try {
            KeyStore keyStore = androidKeyStore();
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete Keystore key", e);
        }
    }

    private static KeyStore androidKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return keyStore;
    }

    private static SecretKey pbkdf2(char[] password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_SIZE_BYTES * 8);
        try {
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
            return new SecretKeySpec(encoded, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] random(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static byte[] gcmEncryptSoftware(SecretKey key, byte[] plaintext, byte[] aad) throws Exception {
        return gcmEncryptSoftware(key, plaintext, random(IV_SIZE), aad);
    }

    private static byte[] gcmEncryptSoftware(SecretKey key, byte[] plaintext, byte[] iv, byte[] aad) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        if (aad != null) cipher.updateAAD(aad);
        byte[] ciphertext = cipher.doFinal(plaintext);

        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        return buffer.array();
    }

    private static byte[] gcmEncryptKeystore(SecretKey key, byte[] plaintext, byte[] aad) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        if (aad != null) cipher.updateAAD(aad);
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext);

        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        return buffer.array();
    }

    private static byte[] gcmDecrypt(SecretKey key, byte[] ivAndCiphertext, byte[] aad) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(ivAndCiphertext);
        if (buffer.remaining() < IV_SIZE + GCM_TAG_BYTES) {
            throw new IllegalArgumentException("Encrypted blob too short");
        }
        byte[] iv = new byte[IV_SIZE];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        return gcmDecrypt(key, ciphertext, iv, aad);
    }

    private static byte[] gcmDecrypt(SecretKey key, byte[] ciphertext, byte[] iv, byte[] aad) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        if (aad != null) cipher.updateAAD(aad);
        return cipher.doFinal(ciphertext);
    }

    private static void zero(byte[] buffer) {
        if (buffer != null) Arrays.fill(buffer, (byte) 0);
    }
}
