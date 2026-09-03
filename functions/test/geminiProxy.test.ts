import { geminiProxy } from "../src/geminiProxy";

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
