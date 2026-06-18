package com.fatima.vientos;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Devuelve TODO el estado del juego en un único JSON.
 * Las pantallas de los 5 roles consultan este endpoint y cada una usa
 * la parte que le corresponde mostrar.
 *
 * Estrategia: lo que cambia por acciones de jugadoras vive en BD;
 * lo que se mueve solo (viento, picos, alertas) se calcula al vuelo
 * con JuegoTiempo a partir del momento de inicio. Así los 5 navegadores
 * ven exactamente lo mismo sin sincronizar nada.
 */
@WebServlet("/EstadoServlet")
public class EstadoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try (Connection conn = ConectorBD.getConnection()) {

            // ----- 1) leer partida -----
            JuegoTiempo.EstadoPartida e = new JuegoTiempo.EstadoPartida();
            String estadoPartida = "esperando";
            int energiaTotal = 0;
            double bateria = 0;
            int puntaje = 0;
            int txQuemados = 0;
            int generadoresRestantes = 3;
            String gesto1Dir = null, gesto2Dir = null;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT estado, inicio, modelo_tx_activo, tx_configurado, subestacion_on, " +
                    "ml_conectado, energia_total_kw, bateria, puntaje, transformadores_quemados, " +
                    "generadores_restantes, gesto1_direccion, gesto2_direccion FROM partida WHERE id=1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    estadoPartida   = rs.getString("estado");
                    Timestamp t     = rs.getTimestamp("inicio");
                    e.inicio        = (t != null) ? t.toLocalDateTime() : null;
                    e.modeloTxActivo = rs.getString("modelo_tx_activo");
                    e.txConfigurado = rs.getInt("tx_configurado") == 1;
                    e.subestacionOn = rs.getInt("subestacion_on") == 1;
                    e.mlConectado   = rs.getInt("ml_conectado") == 1;
                    energiaTotal    = rs.getInt("energia_total_kw");
                    bateria         = rs.getDouble("bateria");
                    puntaje         = rs.getInt("puntaje");
                    txQuemados      = rs.getInt("transformadores_quemados");
                    generadoresRestantes = rs.getInt("generadores_restantes");
                    gesto1Dir       = rs.getString("gesto1_direccion");
                    gesto2Dir       = rs.getString("gesto2_direccion");
                }
            }

            // ----- 2) leer datos del transformador activo -----
            int potenciaMva = 0;
            String grupoVect = "", enfriamiento = "", conversion = "", fabricante = "";
            if (e.modeloTxActivo != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT potencia_mva, conversion_alta, conversion_baja, " +
                        "grupo_vectorial, enfriamiento, fabricante FROM transformador WHERE modelo=?")) {
                    ps.setString(1, e.modeloTxActivo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            potenciaMva = rs.getInt("potencia_mva");
                            int ca = rs.getInt("conversion_alta");
                            int cb = rs.getInt("conversion_baja");
                            conversion   = ca + "/" + cb + " kV";
                            grupoVect    = rs.getString("grupo_vectorial");
                            enfriamiento = rs.getString("enfriamiento");
                            fabricante   = rs.getString("fabricante");
                        }
                    }
                }
            }

            // ----- 3) calcular cosas derivadas del tiempo -----
            String viento = JuegoTiempo.direccionViento(e.inicio);
            // El aerogenerador empieza apuntando al N; en el simulador completo,
            // el rol Aerogenerador lo cambia y guardaríamos ese estado.
            // Por ahora hardcodeamos N para que el Panel pueda calcular eficiencia.
            String dirAero = "N";
            int eficiencia = JuegoTiempo.eficienciaAerogenerador(viento, dirAero);
            boolean picoAhora = JuegoTiempo.picoActivo(e.inicio, e.inicio); // simplificado
            int kw = JuegoTiempo.kwActuales(e.txConfigurado, e.subestacionOn, e.mlConectado, eficiencia, picoAhora);
            e.cargaActual = JuegoTiempo.porcentajeCarga(kw, potenciaMva);
            e.txQuemado = false; // luego: detectar quemado por carga + tiempo
            e.subestacionDesde = e.inicio;

            List<JuegoTiempo.Alerta> alertas = JuegoTiempo.alertasActivas(e);

            // ----- 4) leer zonas -----
            // Antes de leer, apagar generadores que ya pasaron sus 30 seg (manual sec 4.7.5)
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE zona SET generador_activo=0, generador_inicio=NULL " +
                    "WHERE generador_activo=1 AND generador_inicio IS NOT NULL " +
                    "AND TIMESTAMPDIFF(SECOND, generador_inicio, NOW()) >= 30")) {
                ps.executeUpdate();
            }

            List<String> zonasJson = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT nombre, demanda_base, multiplicador, tiene_evento, slider_pct, " +
                    "cobertura_pct, generador_activo FROM zona");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    zonasJson.add(
                        "{\"nombre\":\""    + rs.getString("nombre") + "\"," +
                        "\"demandaBase\":"  + rs.getInt("demanda_base") + "," +
                        "\"multiplicador\":" + rs.getDouble("multiplicador") + "," +
                        "\"tieneEvento\":"  + (rs.getInt("tiene_evento") == 1) + "," +
                        "\"slider\":"       + rs.getInt("slider_pct") + "," +
                        "\"cobertura\":"    + rs.getDouble("cobertura_pct") + "," +
                        "\"genActivo\":"    + (rs.getInt("generador_activo") == 1) + "}");
                }
            }

            // ----- 5) armar JSON final -----
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"estado\":\"").append(estadoPartida).append("\",");
            json.append("\"tiempoRestante\":\"").append(JuegoTiempo.tiempoRestante(e.inicio)).append("\",");
            json.append("\"puntaje\":").append(puntaje).append(",");

            // Bloque del Panel de Control
            json.append("\"panel\":{");
            json.append("\"viento\":\"").append(viento).append("\",");
            json.append("\"energiaActual\":").append(kw).append(",");
            json.append("\"energiaTotal\":").append(energiaTotal).append(",");
            json.append("\"cargaActual\":").append(e.cargaActual).append(",");
            json.append("\"subestacionOn\":").append(e.subestacionOn).append(",");
            json.append("\"transformador\":{");
            json.append("\"modelo\":\"").append(safe(e.modeloTxActivo)).append("\",");
            json.append("\"quemado\":").append(e.txQuemado);
            json.append("}");
            json.append("},");

            // Bloque del Técnico (datos completos del transformador)
            json.append("\"tecnico\":{");
            json.append("\"transformadorActivo\":{");
            json.append("\"modelo\":\"").append(safe(e.modeloTxActivo)).append("\",");
            json.append("\"potenciaMva\":").append(potenciaMva).append(",");
            json.append("\"conversion\":\"").append(conversion).append("\",");
            json.append("\"grupoVectorial\":\"").append(grupoVect).append("\",");
            json.append("\"enfriamiento\":\"").append(enfriamiento).append("\",");
            json.append("\"fabricante\":\"").append(fabricante).append("\"");
            json.append("}");
            json.append("},");

            // Bloque del Programador
            json.append("\"programador\":{");
            json.append("\"txConfigurado\":").append(e.txConfigurado).append(",");
            json.append("\"subestacionOn\":").append(e.subestacionOn).append(",");
            json.append("\"mlConectado\":").append(e.mlConectado).append(",");
            json.append("\"gesto1Direccion\":\"").append(safe(gesto1Dir)).append("\",");
            json.append("\"gesto2Direccion\":\"").append(safe(gesto2Dir)).append("\"");
            json.append("},");

            // Bloque del Distribuidor
            json.append("\"distribuidor\":{");
            json.append("\"bateria\":").append(bateria).append(",");
            json.append("\"generadoresRestantes\":").append(generadoresRestantes).append(",");
            json.append("\"zonas\":[");
            for (int i = 0; i < zonasJson.size(); i++) {
                if (i > 0) json.append(",");
                json.append(zonasJson.get(i));
            }
            json.append("]");
            json.append("},");

            // Alertas (visibles al Panel, decodificadas por el Técnico)
            json.append("\"alertas\":[");
            for (int i = 0; i < alertas.size(); i++) {
                JuegoTiempo.Alerta a = alertas.get(i);
                if (i > 0) json.append(",");
                json.append("{\"codigo\":\"").append(a.codigo)
                    .append("\",\"tipo\":\"").append(a.tipo)
                    .append("\",\"motivo\":\"").append(a.motivo).append("\"}");
            }
            json.append("]");

            json.append("}");

            out.print(json.toString());

        } catch (SQLException ex) {
            resp.setStatus(500);
            out.print("{\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}