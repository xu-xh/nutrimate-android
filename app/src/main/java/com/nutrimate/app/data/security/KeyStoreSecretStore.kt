package com.nutrimate.app.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts the user's BYOK API key with AES-GCM backed by the Android Keystore.
 * The plaintext key never touches disk; the Keystore key is non-exportable.
 *
 * Adversary model: at-rest confidentiality on a non-rooted device.
 * If the Keystore alias is gone (reinstall / device transfer), [read] returns
 * null and the UI must prompt the user to re-enter their key.
 */
@Singleton
class KeyStoreSecretStore @Inject constructor() {

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "nutrimate_api_key"
        const val IV_LENGTH = 12
        const val TAG_LENGTH = 128
        const val AES_GCM = "AES/GCM/NoPadding"
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    /** Encrypt [plaintext]; the payload is Base64(iv || ciphertext). */
    fun write(plaintext: String): String {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    /** Decrypt a Base64(iv || ciphertext) payload; null when unreadable. */
    fun read(payload: String): String? = try {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        require(bytes.size > IV_LENGTH)
        val iv = bytes.copyOfRange(0, IV_LENGTH)
        val ciphertext = bytes.copyOfRange(IV_LENGTH, bytes.size)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }
}