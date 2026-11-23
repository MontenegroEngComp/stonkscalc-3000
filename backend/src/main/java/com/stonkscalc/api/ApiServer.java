package com.stonkscalc.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP usando só o JDK. Rotas /api/* respondem JSON; o resto serve o
 * frontend já compilado (frontend/dist), se ele existir.
 */
public final class ApiServer {

    private static final int MAX_BODY = 64 * 1024;

    private final HttpServer server;
    private final Path staticDir;

    public ApiServer(int port, Path staticDir) throws IOException {
        this.staticDir = staticDir;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void handleApi(HttpExchange ex) throws IOException {
        try (ex) {
            routeApi(ex);
        }
    }

    private void routeApi(HttpExchange ex) throws IOException {
        try {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            String method = ex.getRequestMethod();
            if (method.equals("OPTIONS")) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            String path = ex.getRequestURI().getPath();
            Map<String, Object> body = method.equals("POST") ? readBody(ex) : Map.of();
            Map<String, Object> resp = CalcService.route(path, body);
            if (resp == null) {
                sendJson(ex, 404, Map.of("ok", false, "error", "404: endpoint não encontrado (igual meu boleto pago)"));
            } else {
                sendJson(ex, 200, resp);
            }
        } catch (Json.JsonException | ClassCastException e) {
            sendJson(ex, 400, Map.of("ok", false, "error", "JSON inválido: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, Map.of("ok", false, "error", "Erro interno. This is fine 🔥🐶☕"));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] bytes = in.readNBytes(MAX_BODY + 1);
            if (bytes.length > MAX_BODY) throw new Json.JsonException("corpo grande demais");
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.isBlank()) return Map.of();
            Object parsed = Json.parse(text);
            if (!(parsed instanceof Map)) throw new Json.JsonException("esperava um objeto");
            return (Map<String, Object>) parsed;
        }
    }

    private static void sendJson(HttpExchange ex, int status, Object data) throws IOException {
        byte[] bytes = Json.stringify(data).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void handleStatic(HttpExchange ex) throws IOException {
        try (ex) {
            if (staticDir == null || !Files.isDirectory(staticDir)) {
                byte[] msg = ("StonksCalc backend rodando 📈\nAPI em /api/*. Para a interface, rode o frontend "
                        + "(cd frontend && npm run dev) ou faça o build dele.").getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                ex.sendResponseHeaders(200, msg.length);
                ex.getResponseBody().write(msg);
                return;
            }
            String rel = ex.getRequestURI().getPath().replaceFirst("^/+", "");
            Path root = staticDir.toAbsolutePath().normalize();
            Path file = root.resolve(rel).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                file = root.resolve("index.html"); // fallback de SPA
            }
            byte[] bytes = Files.readAllBytes(file);
            ex.getResponseHeaders().set("Content-Type", contentType(file));
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".ico")) return "image/x-icon";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }
}
