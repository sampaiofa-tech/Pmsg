import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";

const RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 minutes window
const MAX_REPORTS_PER_WINDOW = 5; // Maximum 5 reports per 10 minutes per user
const ABUSE_FLAG_THRESHOLD = 3; // 3 distinct reporters flag fingerprint for abuse review

export const VALID_ABUSE_TYPES = ["SPAM", "HARASSMENT", "ILLEGAL_CONTENT", "OTHER"] as const;
export type AbuseType = (typeof VALID_ABUSE_TYPES)[number];

/**
 * HTTPS Callable Cloud Function: reportAbuse (v1.3)
 *
 * Receives behavioral abuse reports without ANY message content (Zero-Knowledge).
 *
 * SPECIFICATIONS:
 * 1. Requires authenticated caller (Anonymous or Firebase Auth).
 * 2. Sliding window rate limiting using Firestore collection 'userRateLimits'.
 * 3. Strictly behavioral: REJECTS any payload containing message text, contents, or bodies.
 * 4. Stores report in 'abuseReports' collection (client read/write disabled).
 * 5. Updates server-side metrics in 'abuseMetrics/{fingerprint}' tracking distinct reporters
 *    and setting an abuse flag when multiple independent reports occur.
 */
export const reportAbuse = onCall(async (request) => {
  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("reportAbuse: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para reportar abuso."
    );
  }

  const reporterUid = request.auth.uid;
  const data = request.data as Record<string, any>;

  if (!data || typeof data !== "object") {
    throw new HttpsError("invalid-argument", "Payload da requisição inválido.");
  }

  // 2. Strict ALLOW-LIST enforcement: exclusively accept reportedFingerprint, abuseType, and inviteId
  const ALLOWED_FIELDS = new Set(["reportedFingerprint", "abuseType", "inviteId"]);
  const payloadKeys = Object.keys(data);
  for (const key of payloadKeys) {
    if (!ALLOWED_FIELDS.has(key)) {
      logger.warn(`reportAbuse: Rejected unauthorized field '${key}' in payload.`);
      throw new HttpsError(
        "invalid-argument",
        `Campo não permitido: '${key}'. Denúncias aceitam exclusivamente 'reportedFingerprint', 'abuseType' e 'inviteId'.`
      );
    }
  }

  // 3. Validate required fields
  const { reportedFingerprint, abuseType, inviteId } = data;

  if (!reportedFingerprint || typeof reportedFingerprint !== "string") {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'reportedFingerprint' é obrigatório."
    );
  }

  const fingerprint = reportedFingerprint.trim().toLowerCase();
  if (fingerprint.length !== 64 || !/^[0-9a-f]{64}$/.test(fingerprint)) {
    throw new HttpsError(
      "invalid-argument",
      "Fingerprint deve conter exatamente 64 caracteres hexadecimais (256 bits)."
    );
  }

  if (!abuseType || typeof abuseType !== "string") {
    throw new HttpsError("invalid-argument", "Campo 'abuseType' é obrigatório.");
  }

  const normalizedAbuseType = abuseType.trim().toUpperCase() as AbuseType;
  if (!VALID_ABUSE_TYPES.includes(normalizedAbuseType)) {
    throw new HttpsError(
      "invalid-argument",
      `Tipo de abuso inválido. Valores aceitos: ${VALID_ABUSE_TYPES.join(", ")}.`
    );
  }

  let cleanInviteId: string | null = null;
  if (inviteId !== undefined && inviteId !== null) {
    if (typeof inviteId !== "string" || inviteId.trim().length === 0 || inviteId.length > 128) {
      throw new HttpsError(
        "invalid-argument",
        "Campo 'inviteId' deve ser uma string de até 128 caracteres."
      );
    }
    cleanInviteId = inviteId.trim();
  }

  const db = admin.firestore();

  // 4. Rate Limiting via Firestore userRateLimits (max 5 reports per 10 minutes)
  const rateLimitRef = db.collection("userRateLimits").doc(reporterUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const limitData = (doc && typeof doc.data === "function" && doc.data()) || {
        count: 0,
        windowStart: now,
      };
      const windowStart = typeof limitData.windowStart === "number" ? limitData.windowStart : now;
      const currentCount = typeof limitData.count === "number" ? limitData.count : 0;

      if (now - windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now, lastReportAt: now }, { merge: true });
      } else if (currentCount >= MAX_REPORTS_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de denúncias excedido (máximo 5 a cada 10 minutos)."
        );
      } else {
        t.set(rateLimitRef, { count: currentCount + 1, lastReportAt: now }, { merge: true });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed in reportAbuse:", err);
    throw new HttpsError("internal", "Falha ao processar limite de taxa.");
  }

  // 5. Atomic recording in abuseReports + server-side behavioral metrics in abuseMetrics
  const reportRef = db.collection("abuseReports").doc();
  const metricRef = db.collection("abuseMetrics").doc(fingerprint);

  await db.runTransaction(async (t) => {
    const metricDoc = await t.get(metricRef);
    const metricData = (metricDoc && typeof metricDoc.data === "function" && metricDoc.data()) || {};
    const existingReporters: string[] = Array.isArray(metricData.reporters)
      ? metricData.reporters
      : [];

    let updatedReporters = existingReporters;
    if (!existingReporters.includes(reporterUid)) {
      updatedReporters = [...existingReporters, reporterUid];
    }

    const reportCount = updatedReporters.length;
    const isAbuseFlagged = reportCount >= ABUSE_FLAG_THRESHOLD;

    t.set(reportRef, {
      reportId: reportRef.id,
      reportedFingerprint: fingerprint,
      reporterUid,
      abuseType: normalizedAbuseType,
      inviteId: cleanInviteId,
      createdAt: FieldValue.serverTimestamp(),
    });

    t.set(
      metricRef,
      {
        reportedFingerprint: fingerprint,
        reportCount,
        reporters: updatedReporters,
        abuseFlag: isAbuseFlagged,
        lastReportedAt: FieldValue.serverTimestamp(),
        flaggedAt: isAbuseFlagged ? (metricData.flaggedAt || FieldValue.serverTimestamp()) : null,
      },
      { merge: true }
    );
  });

  logger.info(
    `reportAbuse: Successfully registered abuse report for ${fingerprint.substring(0, 8)}... (type: ${normalizedAbuseType}) by ${reporterUid}`
  );

  return {
    success: true,
    reportId: reportRef.id,
    timestamp: Date.now(),
  };
});
