package com.stonkscalc.web;

import com.stonkscalc.api.CalcService;
import com.stonkscalc.api.Json;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import java.util.Map;

/**
 * Ponto de entrada da versão web. O TeaVM compila este código (e todo o motor Java)
 * para JavaScript, e o navegador chama {@code stonkscalcHandle(rota, json)}, que
 * responde exatamente como a API HTTP do servidor.
 */
public final class WebMain {

    @JSFunctor
    public interface Handler extends JSObject {
        String handle(String path, String body);
    }

    private WebMain() {
    }

    @JSBody(params = "handler", script = "globalThis.stonkscalcHandle = handler;")
    private static native void install(Handler handler);

    public static void main(String[] args) {
        install(WebMain::handle);
    }

    @SuppressWarnings("unchecked")
    static String handle(String path, String body) {
        try {
            Map<String, Object> request = body == null || body.isBlank()
                    ? Map.of()
                    : (Map<String, Object>) Json.parse(body);
            Map<String, Object> response = CalcService.route(path, request);
            if (response == null) {
                response = Map.of("ok", false, "error", "404: endpoint não encontrado");
            }
            return Json.stringify(response);
        } catch (RuntimeException e) {
            return Json.stringify(Map.of("ok", false, "error", "Erro interno: " + e.getMessage()));
        }
    }
}
