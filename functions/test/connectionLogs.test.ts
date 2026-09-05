import { recordConnectionLog, CONNECTION_LOG_RETENTION_DAYS } from "../src/connectionLogs";
import * as admin from "firebase-admin";

describe("v1.4: connectionLogs Helper (Marco Civil da Internet Art. 15)", () => {
  let mockAdd: jest.Mock;

  beforeEach(() => {
    mockAdd = jest.fn().mockResolvedValue({ id: "log_123" });

    jest.spyOn(admin, "firestore").mockReturnValue({
      collection: (name: string) => {
        if (name === "connectionLogs") {
          return { add: mockAdd };
        }
        return { doc: jest.fn() };
      },
    } as any);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should extract client IP and port from headers and store with 180-day TTL", async () => {
    const mockRequest: any = {
      rawRequest: {
        headers: {
          "x-forwarded-for": "177.18.29.30, 10.0.0.1",
          "x-forwarded-port": "49152",
        },
        ip: "10.0.0.1",
      },
    };

    await recordConnectionLog(mockRequest, "testCallable");

    expect(mockAdd).toHaveBeenCalledTimes(1);
    const loggedData = mockAdd.mock.calls[0][0];

    expect(loggedData.ip).toBe("177.18.29.30");
    expect(loggedData.porta).toBe(49152);
    expect(loggedData.functionName).toBe("testCallable");
    expect(loggedData.expiresAt).toBeInstanceOf(Date);

    // Verify TTL is approximately 180 days in future
    const diffDays = Math.round((loggedData.expiresAt.getTime() - Date.now()) / (1000 * 60 * 60 * 24));
    expect(diffDays).toBe(CONNECTION_LOG_RETENTION_DAYS);
  });

  it("should fallback to socket address and port when x-forwarded headers are absent", async () => {
    const mockRequest: any = {
      rawRequest: {
        headers: {},
        socket: {
          remoteAddress: "200.100.50.25",
          remotePort: 54321,
        },
      },
    };

    await recordConnectionLog(mockRequest, "storeMessageKey");

    expect(mockAdd).toHaveBeenCalledTimes(1);
    const loggedData = mockAdd.mock.calls[0][0];

    expect(loggedData.ip).toBe("200.100.50.25");
    expect(loggedData.porta).toBe(54321);
    expect(loggedData.functionName).toBe("storeMessageKey");
  });

  it("should fail safe without throwing if firestore add fails", async () => {
    mockAdd.mockRejectedValueOnce(new Error("Firestore write unavailable"));

    const mockRequest: any = {
      rawRequest: {
        headers: { "x-forwarded-for": "1.2.3.4" },
      },
    };

    await expect(recordConnectionLog(mockRequest, "safeOp")).resolves.not.toThrow();
  });
});
