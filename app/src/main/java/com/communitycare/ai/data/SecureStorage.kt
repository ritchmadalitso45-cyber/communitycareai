package com.communitycare.ai.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Private app files encrypted with a non-exportable Android Keystore key. */
class SecureStorage(context: Context) {
    private val directory = File(context.noBackupFilesDir, "secure").apply { mkdirs() }
    private val key: SecretKey
        get() {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey("communitycare-data-v1", null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("communitycare-data-v1",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
        }

    @Synchronized fun read(name: String): String? {
        val file = AtomicFile(File(directory, name))
        if (!file.baseFile.exists() && !File(directory, "$name.bak").exists()) return null
        try {
            val bytes = file.readFully()
            if (bytes.size < 29 || bytes[0] != 1.toByte()) throw IOException("Invalid storage format")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
            return cipher.doFinal(bytes.copyOfRange(13, bytes.size)).toString(Charsets.UTF_8)
        } catch (error: Exception) {
            // Do not replace unreadable files: they may contain unsent records.
            throw IOException("Cannot open encrypted app data. Do not clear app storage; contact your administrator.", error)
        }
    }

    @Synchronized fun write(name: String, value: String?) {
        val file = AtomicFile(File(directory, name))
        if (value == null) { file.delete(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        val encrypted = byteArrayOf(1) + cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val stream = file.startWrite()
        try { stream.write(encrypted); file.finishWrite(stream) }
        catch (error: Exception) { file.failWrite(stream); throw IOException("Could not save encrypted data", error) }
    }
}
