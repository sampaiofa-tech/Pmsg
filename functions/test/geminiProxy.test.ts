import { geminiProxy } from "../src/geminiProxy";
import * as admin from "firebase-admin";

function createMockResponse() {
  let statusCode = 200;
  let jsonResult: any = null;

  const res: any = {
    statusCode: 200,
    on: jest.fn(),
    getHeader: jest.fn(),
    setHeader: jest.fn(),
    status: (code: number) => {
      statusCode = code;
      res.statusCode = code;
      return res;
    },
    json: (data: any) => {
      jsonResult = data;
      return res;
    },
    getStatusCode: () => statusCode,
    getJsonResult: () => jsonResult,
  };

  return res;
}

describe("geminiProxy Cloud Function Test Suite", () => {
  let mockVerifyIdToken: jest.Mock;

  beforeAll(() => {
    if (!admin.apps.length) {
      admin.initializeApp({ projectId: "test-pmsg" });
    }
  });

  beforeEach(() => {
    mockVerifyIdToken = jest.fn().mockResolvedValue({
      uid: "authenticated_desktop_user",
      email: "user@pmsg.internal",
    });

    jest.spyOn(admin, "auth").mockReturnValue({
      verifyIdToken: mockVerifyIdToken,
    } as any);

    // Default: rate limiter allows requests
    jest.spyOn(admin, "firestore").mockReturnValue({
      collection: () => ({
        doc: () => ({}),
      }),
      runTransaction: jest.fn().mockImplementation(async (callback) => {
        const mockDoc = { exists: false, data: () => ({}) };
        const mockTx = { get: jest.fn().mockResolvedValue(mockDoc), set: jest.fn(), update: jest.fn() };
        return await callback(mockTx);
      }),
    } as any);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should reject non-POST requests with 405", async () => {
    const req: any = {
      method: "GET",
      headers: {},
    };

    const res = createMockResponse();
    await (geminiProxy as any)(req, res);

    expect(res.getStatusCode()).toBe(405);
    expect(res.getJsonResult().error).toContain("Method Not Allowed");
  });

  it("should reject requests without Bearer token with 401", async () => {
    const req: any = {
      method: "POST",
      headers: {},
      body: { prompt: "Teste de nota efêmera" },
    };

    const res = createMockResponse();
    await (geminiProxy as any)(req, res);

    expect(res.getStatusCode()).toBe(401);
    expect(res.getJsonResult().error).toContain("Unauthorized");
  });

  it("CRITICAL RULE: should reject invalid or expired Firebase ID token with 401", async () => {
    mockVerifyIdToken.mockRejectedValue(new Error("Firebase ID token has expired."));

    const req: any = {
      method: "POST",
      headers: {
        authorization: "Bearer expired_or_malformed_token",
      },
      body: { prompt: "Mensagem secreta" },
    };

    const res = createMockResponse();
    await (geminiProxy as any)(req, res);

    expect(res.getStatusCode()).toBe(401);
    expect(res.getJsonResult().error).toContain("Invalid or expired Firebase ID token");
  });

  it("should reject requests with missing prompt with 400", async () => {
    const req: any = {
      method: "POST",
      headers: {
        authorization: "Bearer valid_client_session_token_123",
      },
      body: {},
    };

    const res = createMockResponse();
    await (geminiProxy as any)(req, res);

    expect(res.getStatusCode()).toBe(400);
    expect(res.getJsonResult().error).toContain("Missing or invalid 'prompt'");
  });

  it("CRITICAL RULE: should reject requests with 429 when rate limit is exceeded", async () => {
    // Mock runTransaction to simulate exceeded rate limit (count = 5)
    jest.spyOn(admin, "firestore").mockReturnValue({
      collection: () => ({
        doc: () => ({}),
      }),
      runTransaction: jest.fn().mockImplementation(async (callback) => {
        // Mock transaction returning false (limit exceeded)
        const mockDoc = {
          exists: true,
          data: () => ({
            windowStart: Date.now() - 10000,
            count: 5, // Already reached 5
          }),
        };
        const mockTransaction = {
          get: jest.fn().mockResolvedValue(mockDoc),
          update: jest.fn(),
          set: jest.fn(),
        };
        return await callback(mockTransaction);
      }),
    } as any);

    const req: any = {
      method: "POST",
      headers: {
        authorization: "Bearer authenticated_desktop_token",
      },
      body: {
        prompt: "Tentativa de ultrapassar rate limit",
      },
    };

    const res = createMockResponse();
    await (geminiProxy as any)(req, res);

    expect(res.getStatusCode()).toBe(429);
    expect(res.getJsonResult().error).toContain("Too Many Requests");
  });

  it("SECURITY GUARANTEE: Response must NEVER contain GEMINI_API_KEY", async () => {
    const mockApiKey = "AIzaSyFakeSecretKeyThatMustNeverLeak123456";
    process.env.GEMINI_API_KEY = mockApiKey;

    // Mock global fetch to simulate successful Gemini API response
    const originalFetch = global.fetch;
    global.fetch = jest.fn().mockImplementation(async () => {
      return {
        ok: true,
        json: async () => ({
          candidates: [
            {
              content: {
                parts: [{ text: "Nota efêmera gerada com segurança máxima" }],
              },
            },
          ],
        }),
      } as any;
    });

    try {
      const req: any = {
        method: "POST",
        headers: {
          authorization: "Bearer authenticated_desktop_token",
        },
        body: {
          prompt: "Lembrete de segurança para queimar em 1h",
          model: "gemini-2.0-flash",
        },
      };

      const res = createMockResponse();
      await (geminiProxy as any)(req, res);

      expect(res.getStatusCode()).toBe(200);
      expect(res.getJsonResult().note).toBe("Nota efêmera gerada com segurança máxima");
      expect(res.getJsonResult().model).toBe("gemini-2.0-flash");

      // Check serialized response string: MUST NOT leak API key
      const responseStr = JSON.stringify(res.getJsonResult());
      expect(responseStr).not.toContain(mockApiKey);
      expect(responseStr).not.toContain("AIzaSy");
    } finally {
      global.fetch = originalFetch;
      delete process.env.GEMINI_API_KEY;
    }
  });
});
