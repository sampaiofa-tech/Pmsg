package com.example.data.network

/**
 * Centralized multiplatform endpoint manager for Cloud Functions and Firebase Auth.
 *
 * ANTI-SPOOFING SECURITY GUARANTEE:
 * - RELEASE builds: URLs and Project ID are immutable compile-time constants.
 *   Environment variables are strictly ignored to prevent runtime endpoint redirection / MITM attacks.
 * - DEBUG builds: Allows local development using Firebase Emulators or environment variable overrides.
 *
 * Note: Baseline production URLs will be updated with actual deployed endpoints from 'firebase deploy' in Etapa B.
 */
object AppEndpoints {

    // Real deployed project configuration from Firebase deploy
    const val DEFAULT_PROJECT_ID: String = "gen-lang-client-0858445711"
    const val REGION: String = "us-central1"

    // Production endpoints (immutable in Release)
    const val PROD_PROXY_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/geminiProxy"
    const val PROD_STORE_KEY_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/storeMessageKey"
    const val PROD_GET_KEY_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/getMessageKey"
    const val PROD_RESOLVE_FINGERPRINT_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/resolveFingerprint"
    const val PROD_CREATE_INVITE_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/createInvite"
    const val PROD_ACCEPT_INVITE_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/acceptInvite"
    const val PROD_UPDATE_IDENTITY_ROUTING_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/updateIdentityRouting"
    const val DEFAULT_WEB_API_KEY: String = "AIzaSyDpVykwTzWXMqYQFpKGebyBW979nlcaL1Y"
    const val PROD_REPORT_ABUSE_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/reportAbuse"
    const val PROD_IDENTITY_TOOLKIT_URL: String = "https://identitytoolkit.googleapis.com/v1"
    const val PROD_SECURE_TOKEN_URL: String = "https://securetoken.googleapis.com/v1"
    const val PROD_FIRESTORE_URL: String = "https://firestore.googleapis.com/v1"

    val isDebug: Boolean
        get() = PlatformEnvironment.isDebug

    val isEmulator: Boolean
        get() = isDebug && (PlatformEnvironment.getEnv("PMSG_USE_EMULATOR") == "true")

    val projectId: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_PROJECT_ID") ?: DEFAULT_PROJECT_ID
        } else {
            DEFAULT_PROJECT_ID
        }

    val geminiProxyUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_PROXY_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/geminiProxy"
            } else {
                PROD_PROXY_URL
            }
        } else {
            PROD_PROXY_URL
        }

    val storeMessageKeyUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_STORE_KEY_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/storeMessageKey"
            } else {
                PROD_STORE_KEY_URL
            }
        } else {
            PROD_STORE_KEY_URL
        }

    val getMessageKeyUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_GET_KEY_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/getMessageKey"
            } else {
                PROD_GET_KEY_URL
            }
        } else {
            PROD_GET_KEY_URL
        }

    val resolveFingerprintUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_RESOLVE_FP_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/resolveFingerprint"
            } else {
                PROD_RESOLVE_FINGERPRINT_URL
            }
        } else {
            PROD_RESOLVE_FINGERPRINT_URL
        }

    val createInviteUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_CREATE_INVITE_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/createInvite"
            } else {
                PROD_CREATE_INVITE_URL
            }
        } else {
            PROD_CREATE_INVITE_URL
        }

    val acceptInviteUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_ACCEPT_INVITE_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/acceptInvite"
            } else {
                PROD_ACCEPT_INVITE_URL
            }
        } else {
            PROD_ACCEPT_INVITE_URL
        }

    val updateIdentityRoutingUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_UPDATE_ROUTING_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/updateIdentityRouting"
            } else {
                PROD_UPDATE_IDENTITY_ROUTING_URL
            }
        } else {
            PROD_UPDATE_IDENTITY_ROUTING_URL
        }

    val reportAbuseUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_REPORT_ABUSE_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/reportAbuse"
            } else {
                PROD_REPORT_ABUSE_URL
            }
        } else {
            PROD_REPORT_ABUSE_URL
        }

    val identityToolkitBaseUrl: String
        get() = if (isDebug && isEmulator) {
            "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1"
        } else {
            PROD_IDENTITY_TOOLKIT_URL
        }

    val secureTokenBaseUrl: String
        get() = if (isDebug && isEmulator) {
            "http://127.0.0.1:9099/securetoken.googleapis.com/v1"
        } else {
            PROD_SECURE_TOKEN_URL
        }

    val firestoreBaseUrl: String
        get() = if (isDebug && isEmulator) {
            "http://127.0.0.1:8080/v1/projects/$projectId/databases/(default)/documents"
        } else {
            "$PROD_FIRESTORE_URL/projects/$projectId/databases/(default)/documents"
        }

    val webApiKey: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("FIREBASE_WEB_API_KEY") ?: DEFAULT_WEB_API_KEY
        } else {
            DEFAULT_WEB_API_KEY
        }
}
