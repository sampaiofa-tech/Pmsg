import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";

/**
 * Retenção legal obrigatória de registros de conexão para provedores de aplicações de internet:
 * Marco Civil da Internet (Lei nº 12.965/2014, Art. 15):
 * O provedor de aplicações de internet deve manter os respectivos registros de acesso a aplicações
 * de internet, sob sigilo, em ambiente controlado e de segurança, pelo prazo de 6 meses (180 dias).
 */
export const CONNECTION_LOG_RETENTION_DAYS = 180;

export interface ConnectionLogData {
  ip: string;
  porta: number | null;
  functionName: string;
  timestampUtc: any;
  expiresAt: Date;
}

/**
 * Registra dados estritos de conexão (IP, porta lógica de origem, timestamp UTC, nome do endpoint e expiração de 180 dias)
 * em conformidade com o Art. 15 do Marco Civil da Internet.
 * 
 * Executa em modo non-blocking e fail-safe para não interromper a operação criptográfica do cliente.
 */
export async function recordConnectionLog(
  request: any,
  functionName: string
): Promise<void> {
  try {
    const rawReq = request?.rawRequest || request;
    if (!rawReq) return;

    // 1. Extração do endereço IP de origem (respeitando cabeçalhos de proxy reverso Cloudflare / GCP)
    const xForwardedFor = rawReq.headers?.["x-forwarded-for"];
    let clientIp = "unknown";
    if (typeof xForwardedFor === "string" && xForwardedFor.trim().length > 0) {
      clientIp = xForwardedFor.split(",")[0].trim();
    } else if (typeof rawReq.ip === "string" && rawReq.ip.trim().length > 0) {
      clientIp = rawReq.ip.trim();
    } else if (rawReq.socket?.remoteAddress) {
      clientIp = rawReq.socket.remoteAddress;
    }

    // 2. Extração da porta lógica de origem (se disponibilizada pelo proxy/socket)
    const xForwardedPort = rawReq.headers?.["x-forwarded-port"];
    let clientPort: number | null = null;
    if (typeof xForwardedPort === "string" && xForwardedPort.trim().length > 0) {
      const parsed = parseInt(xForwardedPort.trim(), 10);
      if (!isNaN(parsed) && parsed > 0 && parsed <= 65535) {
        clientPort = parsed;
      }
    } else if (typeof rawReq.socket?.remotePort === "number") {
      clientPort = rawReq.socket.remotePort;
    }

    // 3. TTL estrito de 180 dias (Marco Civil Art. 15)
    const now = Date.now();
    const expiresAt = new Date(now + CONNECTION_LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000);

    const timestampUtc = typeof FieldValue?.serverTimestamp === "function" 
      ? FieldValue.serverTimestamp() 
      : new Date();

    const logEntry: ConnectionLogData = {
      ip: clientIp,
      porta: clientPort,
      functionName,
      timestampUtc,
      expiresAt,
    };

    const db = admin.firestore();
    if (db && typeof db.collection === "function") {
      const coll = db.collection("connectionLogs");
      if (coll && typeof coll.add === "function") {
        await coll.add(logEntry);
      }
    }
  } catch (err: any) {
    logger.warn(`recordConnectionLog: Falha não-bloqueante ao registrar log para ${functionName}:`, err?.message || err);
  }
}
