package com.stonkscalc;

import com.stonkscalc.api.ApiServer;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(env("PORT", args.length > 0 ? args[0] : "8080"));
        Path staticDir = findStaticDir();
        ApiServer server = new ApiServer(port, staticDir);
        server.start();
        System.out.println("""

                 ____  _              _         ____      _
                / ___|| |_ ___  _ __ | | _____ / ___|__ _| | ___
                \\___ \\| __/ _ \\| '_ \\| |/ / __| |   / _` | |/ __|
                 ___) | || (_) | | | |   <\\__ \\ |__| (_| | | (__
                |____/ \\__\\___/|_| |_|_|\\_\\___/\\____\\__,_|_|\\___|   3000 📈
                """);
        System.out.println("  API rodando em  http://localhost:" + port + "/api/health");
        if (staticDir != null) {
            System.out.println("  Interface em    http://localhost:" + port + "  (servindo " + staticDir + ")");
        } else {
            System.out.println("  Interface: rode 'npm run dev' na pasta frontend (http://localhost:5173)");
        }
        System.out.println("  Ctrl+C para parar.\n");
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? def : v;
    }

    private static Path findStaticDir() {
        String configured = System.getenv("STATIC_DIR");
        Path[] candidates = configured != null
                ? new Path[]{Path.of(configured)}
                : new Path[]{Path.of("frontend", "dist"), Path.of("..", "frontend", "dist")};
        for (Path p : candidates) {
            if (Files.isRegularFile(p.resolve("index.html"))) return p.toAbsolutePath().normalize();
        }
        return null;
    }
}
