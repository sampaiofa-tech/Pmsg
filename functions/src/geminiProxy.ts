import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import * as admin from "firebase-admin";

const geminiApiKey = defineSecret("GEMINI_API_KEY");

/**
 * HTTPS Cloud Function: geminiProxy
 *
 * Receives ephemeral AI burner note requests from Desktop and Web clients.
 * Authenticates client session token, attaches GEMINI_API_KEY from server-side
 * secret store, queries Google Gemini API (gemini-2.0-flash), and returns
 * the generated note without ever leaking the API key to the client.
 */
export const geminiProxy = onRequest(
  {
    secrets: [geminiApiKey],
    cors: true,
    invoker: "public",
  },
  async (req, res) => {
    // 1. Enforce POST method
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method Not Allowed. Use POST." });
      return;
    }

    // 2. Validate Authorization header with real verifyIdToken check
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      logger.warn("geminiProxy: Unauthorized request (missing or invalid Bearer header).");
      res.status(401).json({ error: "Unauthorized. Valid Bearer session token required." });
      return;
    }

    const sessionToken = authHeader.substring("Bearer ".length).trim();
    if (!sessionToken) {
      res.status(401).json({ error: "Unauthorized. Empty session token." });
      return;
    }

    // Cryptographic validation of Firebase Auth ID token
    try {
      await admin.auth().verifyIdToken(sessionToken);
    } catch (authError: any) {
      logger.warn("geminiProxy: Invalid or expired Firebase ID token.", { message: authError?.message });
      res.status(401).json({ error: "Unauthorized. Invalid or expired Firebase ID token." });
      return;
    }

    // 3. Parse request payload
    let body = req.body;
    if (typeof body === "string") {
      try {
        body = JSON.parse(body);
      } catch {
        res.status(400).json({ error: "Invalid JSON body." });
        return;
      }
    }

    const prompt = body?.prompt;
    if (!prompt || typeof prompt !== "string") {
      res.status(400).json({ error: "Missing or invalid 'prompt' parameter." });
      return;
    }

    const model = body?.model || "gemini-2.0-flash";

    // 4. Retrieve GEMINI_API_KEY from Secret Manager or environment (Emulator fallback)
    const apiKey = process.env.GEMINI_API_KEY || geminiApiKey.value();
    if (!apiKey) {
      logger.error("geminiProxy: GEMINI_API_KEY secret is not configured.");
      res.status(500).json({ error: "Server AI configuration unavailable." });
      return;
    }

    // 5. Call Gemini API via HTTPS REST endpoint
    try {
      const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

      const geminiPayload = {
        contents: [
          {
            parts: [
              {
                text: `Gere uma nota efêmera e concisa para mensageria segura baseada no seguinte pedido: ${prompt}`,
              },
            ],
          },
        ],
        generationConfig: {
          maxOutputTokens: 256,
          temperature: 0.7,
        },
      };

      const response = await fetch(geminiUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(geminiPayload),
      });

      if (!response.ok) {
        const errText = await response.text();
        logger.error(`geminiProxy: Upstream Gemini API error (${response.status}):`, errText);
        res.status(502).json({ error: "Upstream AI service error." });
        return;
      }

      const responseData = (await response.json()) as any;
      const generatedText =
        responseData?.candidates?.[0]?.content?.parts?.[0]?.text ||
        "Nota efêmera gerada com sucesso.";

      res.status(200).json({
        note: generatedText.trim(),
        model: model,
        timestamp: Date.now(),
      });
    } catch (err: any) {
      logger.error("geminiProxy: Exception calling Gemini API:", err);
      res.status(500).json({ error: "Internal proxy error executing AI task." });
    }
  }
);
