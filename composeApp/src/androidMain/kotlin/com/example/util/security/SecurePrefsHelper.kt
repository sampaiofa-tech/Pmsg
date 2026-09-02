package com.example.util.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

sealed class PinValidationResult {
    object Success : PinValidationResult()
    object DuressTriggered : PinValidationResult()
    data class InvalidPin(val failedAttempts: Int, val attemptsUntilLockout: Int) : PinValidationResult()
    data class LockedOut(val remainingSeconds: Int) : PinValidationResult()
}

/**
 * Helper to securely manage user authentication credentials (PIN)
 * with individual cryptographic salt and key-stretched PBKDF2WithHmacSHA256 hashing.
 * Prevents offline dictionary attacks and includes progressive cooldown lockout against brute-force.
 * Also supports Duress PIN (PIN de Coação) for silent defense under threat.
 */
object SecurePrefsHelper {

    private const val PREFS_NAME = "pmsg_vault_security_prefs"
    private const val KEY_PIN_HASH = "sec_pin_hash_v3_pbkdf2"
    private const val KEY_PIN_SALT = "sec_pin_salt_v3"
    private const val KEY_PIN_HASH_LEGACY = "sec_pin_hash_v2"
    private const val KEY_PIN_SALT_LEGACY = "sec_pin_salt_v2"
    private const val KEY_DURESS_PIN_HASH = "sec_duress_pin_hash"
    private const val KEY_DURESS_PIN_SALT = "sec_duress_pin_salt"
    private const val KEY_FAILED_PIN_ATTEMPTS = "sec_failed_pin_attempts"
    private const val KEY_LOCKOUT_UNTIL_TS = "sec_lockout_until_ts"
    private const val KEY_SCREEN_PROTECTION = "sec_screen_protection"
    private const val KEY_SCREENSHOT_DETECTION = "sec_screenshot_detection"
    private const val KEY_BLOCK_SENSITIVE_SCREENSHOT = "sec_block_sensitive_screenshot"
    private const val KEY_BIOMETRIC_LOCK = "sec_biometric_lock"
    private const val KEY_AUTO_LOCK = "sec_auto_lock"
    private const val KEY_AUTO_LOCK_TIMEOUT = "sec_auto_lock_timeout"
    private const val KEY_READ_RECEIPTS = "sec_read_receipts"
    private const val KEY_VANISH_AFTER_READ = "sec_vanish_after_read"
    private const val KEY_SHAKE_TO_CLEAR = "sec_shake_to_clear"
    private const val KEY_SHAKE_SENSITIVITY = "sec_shake_sensitivity"
    private const val KEY_SHAKE_CONFIRMATION = "sec_shake_confirmation"
    private const val KEY_CLIPBOARD_AUTO_CLEAR = "sec_clipboard_auto_clear"
    private const val KEY_PRIVACY_CURTAIN = "sec_privacy_curtain"

    private const val DEFAULT_PIN = "1234"
    private const val PBKDF2_ITERATIONS = 10000
    private const val PBKDF2_KEY_LENGTH = 256

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Initializes default PIN hash if none exists.
     */
    fun ensurePinInitialized(context: Context) {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_PIN_HASH) && !prefs.contains(KEY_PIN_HASH_LEGACY)) {
            setPin(context, DEFAULT_PIN)
        }
    }

    /**
     * Sets a new 4-digit PIN, generating a fresh cryptographically random 16-byte salt
     * and deriving a PBKDF2WithHmacSHA256 key-stretched hash.
     */
    fun setPin(context: Context, newPin: String) {
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hashBase64 = computePbkdf2Hash(newPin, saltBytes)

        getPrefs(context).edit()
            .putString(KEY_PIN_SALT, saltBase64)
            .putString(KEY_PIN_HASH, hashBase64)
            .remove(KEY_PIN_HASH_LEGACY)
            .remove(KEY_PIN_SALT_LEGACY)
            .putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_TS, 0L)
            .apply()
    }

    /**
     * Sets or updates a Duress PIN (PIN de Coação).
     * If entered on the lock screen under duress, the app will execute silent protection.
     */
    fun setDuressPin(context: Context, duressPin: String) {
        if (duressPin.length != 4 || !duressPin.all { it.isDigit() }) return
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hashBase64 = computePbkdf2Hash(duressPin, saltBytes)

        getPrefs(context).edit()
            .putString(KEY_DURESS_PIN_SALT, saltBase64)
            .putString(KEY_DURESS_PIN_HASH, hashBase64)
            .apply()
    }

    fun clearDuressPin(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_DURESS_PIN_SALT)
            .remove(KEY_DURESS_PIN_HASH)
            .apply()
    }

    fun isDuressPinConfigured(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.contains(KEY_DURESS_PIN_HASH) && prefs.contains(KEY_DURESS_PIN_SALT)
    }

    private fun checkDuressPin(context: Context, inputPin: String): Boolean {
        val prefs = getPrefs(context)
        val saltBase64 = prefs.getString(KEY_DURESS_PIN_SALT, null) ?: return false
        val expectedHash = prefs.getString(KEY_DURESS_PIN_HASH, null) ?: return false
        return try {
            val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
            val computedHash = computePbkdf2Hash(inputPin, saltBytes)
            MessageDigest.isEqual(
                computedHash.toByteArray(Charsets.UTF_8),
                expectedHash.toByteArray(Charsets.UTF_8)
            )
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns remaining lockout seconds if currently under cooldown.
     */
    fun getLockoutRemainingSeconds(context: Context): Int {
        val prefs = getPrefs(context)
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL_TS, 0L)
        val now = System.currentTimeMillis()
        if (now >= lockoutUntil) return 0
        return ((lockoutUntil - now + 999) / 1000).toInt().coerceAtLeast(1)
    }

    fun isLockedOut(context: Context): Boolean {
        return getLockoutRemainingSeconds(context) > 0
    }

    /**
     * Verifies if the provided PIN matches:
     * 1. Duress PIN -> triggers DuressTriggered
     * 2. Real PIN -> triggers Success with rate-limit counter reset
     * 3. Mismatch -> progressive cooldown lockout
     */
    fun verifyPinWithRateLimit(context: Context, inputPin: String): PinValidationResult {
        ensurePinInitialized(context)
        val remainingLockout = getLockoutRemainingSeconds(context)
        if (remainingLockout > 0) {
            return PinValidationResult.LockedOut(remainingLockout)
        }

        // 1. Check Duress PIN first
        if (checkDuressPin(context, inputPin)) {
            return PinValidationResult.DuressTriggered
        }

        // 2. Check Standard PIN (PBKDF2 with SHA-256 legacy migration)
        val prefs = getPrefs(context)
        var isValid = false

        if (prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)) {
            val saltBase64 = prefs.getString(KEY_PIN_SALT, null)
            val expectedHash = prefs.getString(KEY_PIN_HASH, null)
            if (saltBase64 != null && expectedHash != null) {
                val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
                val computedHash = computePbkdf2Hash(inputPin, saltBytes)
                isValid = MessageDigest.isEqual(
                    computedHash.toByteArray(Charsets.UTF_8),
                    expectedHash.toByteArray(Charsets.UTF_8)
                )
            }
        } else if (prefs.contains(KEY_PIN_HASH_LEGACY) && prefs.contains(KEY_PIN_SALT_LEGACY)) {
            val saltBase64 = prefs.getString(KEY_PIN_SALT_LEGACY, null)
            val expectedHash = prefs.getString(KEY_PIN_HASH_LEGACY, null)
            if (saltBase64 != null && expectedHash != null) {
                val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
                val computedLegacy = computeLegacySha256(inputPin, saltBytes)
                isValid = MessageDigest.isEqual(
                    computedLegacy.toByteArray(Charsets.UTF_8),
                    expectedHash.toByteArray(Charsets.UTF_8)
                )
                if (isValid) {
                    // Transparently upgrade to PBKDF2
                    setPin(context, inputPin)
                }
            }
        } else {
            isValid = (inputPin == DEFAULT_PIN)
            if (isValid) {
                setPin(context, DEFAULT_PIN)
            }
        }

        return if (isValid) {
            // Reset failure counter on correct PIN
            prefs.edit()
                .putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL_TS, 0L)
                .apply()
            PinValidationResult.Success
        } else {
            val currentFailures = prefs.getInt(KEY_FAILED_PIN_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_PIN_ATTEMPTS, currentFailures)

            val lockoutSeconds = when {
                currentFailures >= 8 -> 60
                currentFailures >= 5 -> 30
                currentFailures >= 3 -> 15
                else -> 0
            }

            if (lockoutSeconds > 0) {
                val lockoutUntil = System.currentTimeMillis() + (lockoutSeconds * 1000L)
                editor.putLong(KEY_LOCKOUT_UNTIL_TS, lockoutUntil)
                editor.apply()
                PinValidationResult.LockedOut(lockoutSeconds)
            } else {
                editor.apply()
                val attemptsUntilLockout = (3 - currentFailures).coerceAtLeast(1)
                PinValidationResult.InvalidPin(currentFailures, attemptsUntilLockout)
            }
        }
    }

    /**
     * Standard boolean verification (backwards compatible).
     */
    fun verifyPin(context: Context, inputPin: String): Boolean {
        return verifyPinWithRateLimit(context, inputPin) is PinValidationResult.Success
    }

    private fun computePbkdf2Hash(pin: String, salt: ByteArray): String {
        return try {
            val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } catch (_: Throwable) {
            // Fallback to SHA-256 if PBKDF2 provider is unavailable
            computeLegacySha256(pin, salt)
        }
    }

    private fun computeLegacySha256(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    /**
     * Panic Wipe: Completely wipes all stored preferences, hashes, salts and reset to defaults.
     */
    fun panicWipeAllPrefs(context: Context) {
        getPrefs(context).edit().clear().apply()
        ensurePinInitialized(context)
    }

    // --- SECURE PREFERENCE ACCESSORS ---

    fun isScreenshotDetectionEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SCREENSHOT_DETECTION, true)

    fun setScreenshotDetectionEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_SCREENSHOT_DETECTION, enabled).apply()

    fun isBlockSensitiveOnScreenshotEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_BLOCK_SENSITIVE_SCREENSHOT, true)

    fun setBlockSensitiveOnScreenshotEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_BLOCK_SENSITIVE_SCREENSHOT, enabled).apply()

    fun isBiometricLockEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_BIOMETRIC_LOCK, false)

    fun setBiometricLockEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()

    fun isAutoLockEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AUTO_LOCK, true)

    fun setAutoLockEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_AUTO_LOCK, enabled).apply()

    fun getAutoLockTimeoutMinutes(context: Context): Int =
        getPrefs(context).getInt(KEY_AUTO_LOCK_TIMEOUT, 5)

    fun setAutoLockTimeoutMinutes(context: Context, minutes: Int) =
        getPrefs(context).edit().putInt(KEY_AUTO_LOCK_TIMEOUT, minutes).apply()

    fun isReadReceiptsEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_READ_RECEIPTS, true)

    fun setReadReceiptsEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_READ_RECEIPTS, enabled).apply()

    fun getVanishAfterReadSeconds(context: Context): Int =
        getPrefs(context).getInt(KEY_VANISH_AFTER_READ, 0)

    fun setVanishAfterReadSeconds(context: Context, seconds: Int) =
        getPrefs(context).edit().putInt(KEY_VANISH_AFTER_READ, seconds).apply()

    fun isShakeToClearEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SHAKE_TO_CLEAR, true)

    fun setShakeToClearEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_SHAKE_TO_CLEAR, enabled).apply()

    fun getShakeSensitivity(context: Context): String =
        getPrefs(context).getString(KEY_SHAKE_SENSITIVITY, "NORMAL") ?: "NORMAL"

    fun setShakeSensitivity(context: Context, sensitivity: String) =
        getPrefs(context).edit().putString(KEY_SHAKE_SENSITIVITY, sensitivity).apply()

    fun isShakeRequiresConfirmation(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SHAKE_CONFIRMATION, false)

    fun setShakeRequiresConfirmation(context: Context, requiresConfirmation: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_SHAKE_CONFIRMATION, requiresConfirmation).apply()

    fun getClipboardAutoClearSeconds(context: Context): Int =
        getPrefs(context).getInt(KEY_CLIPBOARD_AUTO_CLEAR, 30)

    fun setClipboardAutoClearSeconds(context: Context, seconds: Int) =
        getPrefs(context).edit().putInt(KEY_CLIPBOARD_AUTO_CLEAR, seconds).apply()

    fun getClipboardClearSeconds(context: Context): Int = getClipboardAutoClearSeconds(context)

    fun setClipboardClearSeconds(context: Context, seconds: Int) = setClipboardAutoClearSeconds(context, seconds)

    fun isPrivacyCurtainEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_PRIVACY_CURTAIN, true)

    fun setPrivacyCurtainEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_PRIVACY_CURTAIN, enabled).apply()
}
