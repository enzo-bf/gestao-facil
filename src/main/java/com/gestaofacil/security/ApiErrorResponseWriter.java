package com.gestaofacil.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

final class ApiErrorResponseWriter {

    private ApiErrorResponseWriter() {
    }

    static void write(
            HttpServletResponse response,
            int status,
            String error,
            String message,
            String path
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"timestamp\":\"" + escape(Instant.now().toString())
                + "\",\"status\":" + status
                + ",\"error\":\"" + escape(error)
                + "\",\"message\":\"" + escape(message)
                + "\",\"path\":\"" + escape(path)
                + "\",\"fieldErrors\":{}}");
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
