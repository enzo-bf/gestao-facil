import { AxiosError, AxiosHeaders, type AxiosAdapter } from "axios";
import { afterEach, describe, expect, it } from "vitest";
import { api } from "./api";

const originalAdapter = api.defaults.adapter;

afterEach(() => {
  api.defaults.adapter = originalAdapter;
});

describe("API CSRF retry", () => {
  it("refreshes a rejected CSRF token and retries a state-changing request only once", async () => {
    let csrfRequests = 0;
    let clientRequests = 0;
    const adapter: AxiosAdapter = async (config) => {
      if (config.url?.endsWith("/api/v1/auth/csrf")) {
        csrfRequests += 1;
        const response = {
          config,
          data: { token: `csrf-${csrfRequests}` },
          headers: new AxiosHeaders(),
          status: 200,
          statusText: "OK",
        };
        return response;
      }

      clientRequests += 1;
      if (clientRequests === 1) {
        const response = {
          config,
          data: { message: "Você não tem permissão para acessar este recurso." },
          headers: new AxiosHeaders({ "X-CSRF-Error": "true" }),
          status: 403,
          statusText: "Forbidden",
        };
        throw new AxiosError("Forbidden", "ERR_BAD_REQUEST", config, undefined, response);
      }

      return {
        config,
        data: { id: "client-1" },
        headers: new AxiosHeaders(),
        status: 201,
        statusText: "Created",
      };
    };
    api.defaults.adapter = adapter;

    await expect(api.post("/api/v1/clients", { name: "Cliente" })).resolves.toMatchObject({
      status: 201,
      data: { id: "client-1" },
    });

    expect(csrfRequests).toBe(2);
    expect(clientRequests).toBe(2);
  });

  it("does not retry a permission rejection that is not identified as a CSRF failure", async () => {
    let clientRequests = 0;
    api.defaults.adapter = async (config) => {
      if (config.url?.endsWith("/api/v1/auth/csrf")) {
        return {
          config,
          data: { token: "csrf-token" },
          headers: new AxiosHeaders(),
          status: 200,
          statusText: "OK",
        };
      }
      clientRequests += 1;
      const response = {
        config,
        data: { message: "Você não tem permissão para acessar este recurso." },
        headers: new AxiosHeaders(),
        status: 403,
        statusText: "Forbidden",
      };
      throw new AxiosError("Forbidden", "ERR_BAD_REQUEST", config, undefined, response);
    };

    await expect(api.post("/api/v1/clients", { name: "Cliente" })).rejects.toMatchObject({
      response: { status: 403 },
    });
    expect(clientRequests).toBe(1);
  });
});
