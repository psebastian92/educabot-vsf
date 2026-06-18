package com.fatima.vientos;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

@WebServlet("/ProxyServlet")
public class ProxyServlet extends HttpServlet {

	private static final String MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions";
	private static final String MISTRAL_KEY = "YZOD1grpfXKmsiUdRZtCTXhM15PrTyGB";
	private static final String MODELO      = "mistral-small-latest";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // Verificar sesión
        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("nombre") == null) {
            resp.setStatus(401);
            return;
        }

        // Leer body del request
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        JSONObject body = new JSONObject(sb.toString());

        // Armar request para Groq
        JSONObject groqBody = new JSONObject();
        groqBody.put("model", MODELO);
        groqBody.put("max_tokens", 1000);
        groqBody.put("temperature", 0.7);

        // System prompt viene del cliente
        String systemPrompt = body.getString("system");
        JSONArray messages = body.getJSONArray("messages");

        // Insertar system como primer mensaje
        JSONArray groqMessages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        groqMessages.put(systemMsg);

        for (int i = 0; i < messages.length(); i++) {
            groqMessages.put(messages.getJSONObject(i));
        }
        groqBody.put("messages", groqMessages);

        // Llamar a Mistrarl
        URL url = new URL(MISTRAL_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + MISTRAL_KEY);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(groqBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        // Leer respuesta de Groq
        int status = conn.getResponseCode();
        InputStream is = status == 200 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder respBody = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) respBody.append(line);
        }

        // Extraer solo el texto y devolverlo
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (status == 200) {
            JSONObject groqResp = new JSONObject(respBody.toString());
            String texto = groqResp
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

            JSONObject resultado = new JSONObject();
            resultado.put("texto", texto);
            resp.getWriter().write(resultado.toString());
        } else {
            resp.setStatus(500);
            resp.getWriter().write(respBody.toString());
        }
    }
}